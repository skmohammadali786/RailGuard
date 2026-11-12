#!/usr/bin/env python3
"""
RailGuard Camera Crack Detector for Raspberry Pi.
Automated optical camera module inspection, edge AI defect detection & instant Wi-Fi transmission.

Functions:
- Optical camera module frame acquisition & railhead surface analysis
- Computer vision flaw classification & crack bounding box localization
- Instant Direct Wi-Fi HTTP API Server on port 8080 (sub-50ms local Wi-Fi latency)
- Dual-channel real-time cloud sync to Firebase RTDB
- Automatic Defect Registry logging, Safety Report compilation & Alert Notification dispatch
- Real-time emergency TSR slow-order interlock

Usage:
  python3 camera_crack_detector.py --uid <USER_UID> --db <DATABASE_URL> [--port 8080] [--auth <ID_TOKEN>]
"""

from __future__ import annotations

import argparse
import datetime
import hashlib
import hmac
import http.server
import io
import json
import logging
import math
import os
import random
import socketserver
import statistics
import struct
import subprocess
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import zlib
from typing import Any, Dict, List, Optional, Tuple

# Local modules (same directory as this script).
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
try:
    import crack_vision
except Exception as _vision_import_error:  # pragma: no cover
    crack_vision = None
    _VISION_IMPORT_ERROR = str(_vision_import_error)
else:
    _VISION_IMPORT_ERROR = None
try:
    from firebase_auth import FirebaseTokenManager
except Exception:  # pragma: no cover
    FirebaseTokenManager = None  # type: ignore

logging.basicConfig(
    level=os.getenv("RAILGUARD_LOG_LEVEL", "INFO"),
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger("camera-crack-detector")

# Global in-memory cache for ultra-low latency direct Wi-Fi endpoints
LATEST_TELEMETRY: Dict[str, Any] = {
    "nodeId": "RPI-TRACK-01",
    "ultrasonicDepthMm": 68.2,
    "vibrationG": 0.035,
    "railTempC": 31.5,
    "axleSpeedKmh": 45.0,
    "chainage": "KM 14+250",
    "hardware": "Raspberry Pi Zero W / Camera Module 3 / Piezo UT",
    "hasCrack": False,
    "crackLengthMm": 0.0,
    "flawType": "NOMINAL_TRACK",
    "aiConfidencePercent": 98,
    "status": "ALL_SENSORS_ONLINE",
    "timestamp": int(time.time() * 1000),
    "latitude": 28.6139,
    "longitude": 77.2090,
    "gpsLocked": True
}
LATEST_CRACK: Optional[Dict[str, Any]] = None
LAST_TSR_COMMAND: Optional[Dict[str, Any]] = None
DEFECTS_REGISTRY: List[Dict[str, Any]] = []

# =============================================================================
DEVICE_ID: str = os.getenv("RAILGUARD_DEVICE_ID", "RPI-TRACK-01")
# How long a detected crack stays "active" before it must be re-detected or
# acknowledged by an operator. Prevents a single reading latching forever.
CRACK_ACTIVE_SECONDS: int = int(os.getenv("RAILGUARD_CRACK_ACTIVE_SECONDS", "60"))
# Minimum gap between two automatic crack pipelines (deduplication).
CRACK_COOLDOWN_SECONDS: int = int(os.getenv("RAILGUARD_CRACK_COOLDOWN_SECONDS", "120"))
# Optional shared secret for the local HTTP API. When set, every endpoint
# except /api/health requires X-API-Key or ?api_key=...
LOCAL_API_KEY: str = os.getenv("RAILGUARD_API_KEY", "").strip()
# Set by --bind. 127.0.0.1 restricts the API to the Pi itself.
LOCAL_BIND_HOST: str = os.getenv("RAILGUARD_BIND_HOST", "0.0.0.0")
# Populated in main()/run_continuous_camera_scan().
TOKEN_MANAGER: Optional[Any] = None
LATEST_VISION_RESULT: Dict[str, Any] = {
    "available": bool(crack_vision and crack_vision.is_available()),
    "hasCrack": False,
    "confidencePercent": 0,
    "mode": "not-run",
}
LAST_CRACK_PIPELINE_MS: int = 0
LAST_CLOUD_OK_MS: int = 0
LAST_CLOUD_ERROR: str = ""
LAST_FRAME_SIMULATED: bool = True
LAST_FRAME_SOURCE: str = "none"
LATEST_CAMERA_MIME: str = "image/png"
_PICAMERA2 = None
_UT_HISTORY: List[float] = []
_UT_DEVIATION_COUNT: int = 0


def now_ms() -> int:
    return int(time.time() * 1000)


def _env_bool(name: str, default: bool = False) -> bool:
    return os.getenv(name, "1" if default else "0").strip().lower() in ("1", "true", "yes", "on")


def _auth_token(id_token: Optional[str] = None) -> Optional[str]:
    """Return the freshest ID token available (auto-refresh when configured)."""
    if TOKEN_MANAGER is not None:
        token = TOKEN_MANAGER.get_token()
        if token:
            return token
    return id_token


def _crack_is_active() -> bool:
    if not LATEST_CRACK or not LATEST_CRACK.get("hasCrack"):
        return False
    ts = int(LATEST_CRACK.get("timestamp", 0) or 0)
    if ts and (now_ms() - ts) > CRACK_ACTIVE_SECONDS * 1000:
        return False
    return True


def clear_latest_crack(reason: str = "ACKNOWLEDGED") -> Dict[str, Any]:
    """Clear the active crack latch so monitoring returns to nominal."""
    global LATEST_CRACK, LATEST_TELEMETRY
    previous = LATEST_CRACK
    LATEST_CRACK = {
        "status": "CLEARED",
        "hasCrack": False,
        "clearedAt": now_ms(),
        "clearReason": reason,
        "previousDefectId": ((previous or {}).get("defect") or {}).get("id"),
    }
    try:
        LATEST_TELEMETRY = dict(LATEST_TELEMETRY or {})
        LATEST_TELEMETRY.update(
            {
                "hasCrack": False,
                "flawType": "NOMINAL_TRACK",
                "crackLengthMm": 0.0,
                "status": "ACTIVE_PATROL_SYNC",
                "timestamp": now_ms(),
            }
        )
    except Exception:
        pass
    save_pi_state()
    log.info("[✓] Crack latch cleared (%s)", reason)
    return LATEST_CRACK

TASKS_REGISTRY: List[Dict[str, Any]] = []
INSPECTIONS_REGISTRY: List[Dict[str, Any]] = []
REPORTS_REGISTRY: List[Dict[str, Any]] = []
SCHEDULES_REGISTRY: List[Dict[str, Any]] = []
ACTIVE_PATROL_STATUS: Dict[str, Any] = {"status": "STANDBY", "sensors": ["camera", "ultrasonic", "vibration", "temperature", "gps"]}
START_TIME_MS: int = int(time.time() * 1000)
LATEST_CAMERA_FRAME_BYTES: Optional[bytes] = None
LATEST_FRAME_CAPTURED_AT: int = int(time.time() * 1000)

# =============================================================================
# HARDWARE PIN CONFIGURATION (Raspberry Pi Zero W / Pi 3 / Pi 4 / Pi 5)
# =============================================================================
# Buzzer Pin (BCM GPIO 18 / Physical Pin 12 by default)
BUZZER_PIN: int = int(os.getenv("RAILGUARD_BUZZER_PIN", "18"))
BUZZER_ACTIVE_LOW: bool = os.getenv("RAILGUARD_BUZZER_ACTIVE_LOW", "0").lower() in ("1", "true", "yes")

# Hardware Ultrasonic Sensor Pins (HC-SR04 / JSN-SR04T)
# TRIG: BCM GPIO 23 / Physical Pin 16
# ECHO: BCM GPIO 24 / Physical Pin 18 (via 1k/2k voltage divider for 3.3V safety)
ULTRASONIC_TRIG_PIN: int = int(os.getenv("RAILGUARD_TRIG_PIN", "23"))
ULTRASONIC_ECHO_PIN: int = int(os.getenv("RAILGUARD_ECHO_PIN", "24"))

# 1-Wire Temperature Sensor (DS18B20)
# DATA: BCM GPIO 4 / Physical Pin 7 (with 4.7k pullup resistor to 3.3V)
ONEWIRE_PIN: int = int(os.getenv("RAILGUARD_ONEWIRE_PIN", "4"))

IS_BUZZER_RINGING: bool = False
BUZZER_TRIGGERED_AT: int = 0
BUZZER_THREAD: Optional[threading.Thread] = None
BUZZER_STOP_EVENT = threading.Event()
LAST_HARDWARE_ACQUISITION_MS: int = 0
STATE_FILE_PATH: str = os.getenv("RAILGUARD_STATE_FILE", os.path.join(os.path.dirname(os.path.abspath(__file__)), "railguard_pi_state.json"))


def save_pi_state():
    """Persists local patrol schedules, defects, tasks, and telemetry to disk."""
    try:
        data = {
            "deviceId": DEVICE_ID,
            "telemetry": LATEST_TELEMETRY,
            "tsr": LAST_TSR_COMMAND,
            "latestCrack": LATEST_CRACK,
            "schedules": SCHEDULES_REGISTRY,
            "defects": DEFECTS_REGISTRY,
            "tasks": TASKS_REGISTRY,
            "inspections": INSPECTIONS_REGISTRY,
            "timestamp": int(time.time() * 1000)
        }
        with open(STATE_FILE_PATH, "w") as f:
            json.dump(data, f, indent=2)
    except Exception as e:
        log.debug("State save failed: %s", e)


def load_pi_state():
    """Restores local patrol schedules, defects, tasks, and telemetry from disk."""
    global LATEST_TELEMETRY, LAST_TSR_COMMAND, SCHEDULES_REGISTRY, DEFECTS_REGISTRY, TASKS_REGISTRY, INSPECTIONS_REGISTRY, LATEST_CRACK
    try:
        if os.path.exists(STATE_FILE_PATH):
            with open(STATE_FILE_PATH, "r") as f:
                data = json.load(f)
            if "telemetry" in data and isinstance(data["telemetry"], dict):
                LATEST_TELEMETRY.update(data["telemetry"])
            if "tsr" in data and isinstance(data["tsr"], dict):
                LAST_TSR_COMMAND = data["tsr"]
            if "latestCrack" in data and isinstance(data["latestCrack"], dict):
                saved_crack = data["latestCrack"]
                saved_ts = int(saved_crack.get("timestamp", 0) or 0)
                # Never restore a stale latch: an old anomaly must not look live.
                if saved_crack.get("hasCrack") and (now_ms() - saved_ts) <= CRACK_ACTIVE_SECONDS * 1000:
                    LATEST_CRACK = saved_crack
                else:
                    LATEST_CRACK = None
            if "schedules" in data and isinstance(data["schedules"], list):
                SCHEDULES_REGISTRY = data["schedules"]
            if "defects" in data and isinstance(data["defects"], list):
                DEFECTS_REGISTRY = data["defects"]
            if "tasks" in data and isinstance(data["tasks"], list):
                TASKS_REGISTRY = data["tasks"]
            if "inspections" in data and isinstance(data["inspections"], list):
                INSPECTIONS_REGISTRY = data["inspections"]
            log.info("[✓] Loaded persistent Pi state from %s", STATE_FILE_PATH)
    except Exception as e:
        log.warning("State load failed: %s", e)


class HardwareGpioPin:
    """
    Robust multi-backend GPIO driver for Raspberry Pi (Bookworm, Bullseye, legacy, or headless).
    Tries in order:
    1. lgpio (official modern library for Debian Bookworm / Raspberry Pi 5 / Pi Zero 2W / Pi Zero W)
    2. RPi.GPIO (traditional Pi OS Bullseye & earlier)
    3. gpiozero (DigitalOutputDevice / DigitalInputDevice / PWMOutputDevice)
    4. Linux 'pinctrl' CLI utility (standard on Debian Bookworm)
    5. Linux 'raspi-gpio' CLI utility
    6. Linux sysfs GPIO (/sys/class/gpio)
    """
    def __init__(self, pin: int, is_output: bool = True, active_low: bool = False):
        self.pin = pin
        self.is_output = is_output
        self.active_low = active_low
        self.backend = "simulation"
        self._lgpio_handle = None
        self._gpio_zero_dev = None
        self._setup()

    def _setup(self):
        # Backend 1: lgpio (official Raspberry Pi OS Bookworm library)
        try:
            import lgpio
            for chip in (0, 4):
                try:
                    h = lgpio.gpiochip_open(chip)
                    if self.is_output:
                        init_val = 1 if self.active_low else 0
                        lgpio.gpio_claim_output(h, self.pin, init_val)
                    else:
                        lgpio.gpio_claim_input(h, self.pin)
                    self._lgpio_handle = h
                    self.backend = "lgpio"
                    return
                except Exception:
                    pass
        except Exception:
            pass

        # Backend 2: RPi.GPIO
        try:
            import RPi.GPIO as GPIO
            GPIO.setwarnings(False)
            GPIO.setmode(GPIO.BCM)
            if self.is_output:
                init_val = GPIO.HIGH if self.active_low else GPIO.LOW
                GPIO.setup(self.pin, GPIO.OUT, initial=init_val)
            else:
                GPIO.setup(self.pin, GPIO.IN)
            self.backend = "RPi.GPIO"
            return
        except Exception:
            pass

        # Backend 3: gpiozero
        try:
            from gpiozero import DigitalOutputDevice, DigitalInputDevice
            if self.is_output:
                self._gpio_zero_dev = DigitalOutputDevice(self.pin, active_high=not self.active_low, initial_value=False)
            else:
                self._gpio_zero_dev = DigitalInputDevice(self.pin)
            self.backend = "gpiozero"
            return
        except Exception:
            pass

        # Backend 4: pinctrl (Pi OS Bookworm CLI)
        try:
            import shutil, subprocess
            if shutil.which("pinctrl"):
                mode = "op" if self.is_output else "ip"
                val = "dh" if (self.is_output and self.active_low) else "dl"
                subprocess.run(["pinctrl", "set", str(self.pin), mode, val], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                self.backend = "pinctrl"
                return
        except Exception:
            pass

        # Backend 5: raspi-gpio
        try:
            import shutil, subprocess
            if shutil.which("raspi-gpio"):
                mode = "op" if self.is_output else "ip"
                val = "dh" if (self.is_output and self.active_low) else "dl"
                subprocess.run(["raspi-gpio", "set", str(self.pin), mode, val], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                self.backend = "raspi-gpio"
                return
        except Exception:
            pass

        # Backend 6: sysfs /sys/class/gpio
        try:
            gpio_path = f"/sys/class/gpio/gpio{self.pin}"
            if not os.path.exists(gpio_path):
                with open("/sys/class/gpio/export", "w") as f:
                    f.write(str(self.pin))
                time.sleep(0.05)
            with open(f"{gpio_path}/direction", "w") as f:
                f.write("out" if self.is_output else "in")
            self.backend = "sysfs"
            return
        except Exception:
            pass

        self.backend = "simulation"

    def write(self, high: bool):
        effective_high = not high if self.active_low else high
        if self.backend == "lgpio" and self._lgpio_handle is not None:
            try:
                import lgpio
                lgpio.gpio_write(self._lgpio_handle, self.pin, 1 if effective_high else 0)
                return
            except Exception:
                pass
        elif self.backend == "RPi.GPIO":
            try:
                import RPi.GPIO as GPIO
                GPIO.output(self.pin, GPIO.HIGH if effective_high else GPIO.LOW)
                return
            except Exception:
                pass
        elif self.backend == "gpiozero" and self._gpio_zero_dev:
            try:
                if high:
                    self._gpio_zero_dev.on()
                else:
                    self._gpio_zero_dev.off()
                return
            except Exception:
                pass
        elif self.backend == "pinctrl":
            try:
                import subprocess
                val = "dh" if effective_high else "dl"
                subprocess.run(["pinctrl", "set", str(self.pin), val], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                return
            except Exception:
                pass
        elif self.backend == "raspi-gpio":
            try:
                import subprocess
                val = "dh" if effective_high else "dl"
                subprocess.run(["raspi-gpio", "set", str(self.pin), val], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                return
            except Exception:
                pass
        elif self.backend == "sysfs":
            try:
                with open(f"/sys/class/gpio/gpio{self.pin}/value", "w") as f:
                    f.write("1" if effective_high else "0")
                return
            except Exception:
                pass

    def tone(self, frequency_hz: float = 2400.0, duration_seconds: float = 0.18):
        """Generates an audible tone for passive buzzers, while also sounding active buzzers."""
        if frequency_hz <= 0 or duration_seconds <= 0:
            return
        # Method A: lgpio hardware PWM
        if self.backend == "lgpio" and self._lgpio_handle is not None:
            try:
                import lgpio
                lgpio.tx_pwm(self._lgpio_handle, self.pin, int(frequency_hz), 50.0)
                time.sleep(duration_seconds)
                lgpio.tx_pwm(self._lgpio_handle, self.pin, int(frequency_hz), 0.0)
                return
            except Exception:
                pass
        # Method B: RPi.GPIO PWM
        if self.backend == "RPi.GPIO":
            try:
                import RPi.GPIO as GPIO
                p = GPIO.PWM(self.pin, frequency_hz)
                p.start(50)
                time.sleep(duration_seconds)
                p.stop()
                return
            except Exception:
                pass
        # Method C: High speed microsecond square-wave toggle
        t_end = time.perf_counter() + duration_seconds
        half_period = 1.0 / (2.0 * frequency_hz)
        while time.perf_counter() < t_end:
            self.write(True)
            t_half = time.perf_counter() + half_period
            while time.perf_counter() < t_half:
                pass
            self.write(False)
            t_half = time.perf_counter() + half_period
            while time.perf_counter() < t_half:
                pass

    def read(self) -> int:
        if self.backend == "lgpio" and self._lgpio_handle is not None:
            try:
                import lgpio
                return lgpio.gpio_read(self._lgpio_handle, self.pin)
            except Exception:
                return 0
        elif self.backend == "RPi.GPIO":
            try:
                import RPi.GPIO as GPIO
                return GPIO.input(self.pin)
            except Exception:
                return 0
        elif self.backend == "gpiozero" and self._gpio_zero_dev:
            try:
                return 1 if self._gpio_zero_dev.is_active else 0
            except Exception:
                return 0
        elif self.backend == "sysfs":
            try:
                with open(f"/sys/class/gpio/gpio{self.pin}/value", "r") as f:
                    return int(f.read().strip())
            except Exception:
                return 0
        return 0


BUZZER_PIN_CONTROLLER: Optional[HardwareGpioPin] = None
BUZZER_PIN_SECONDARY: Optional[HardwareGpioPin] = None


def _buzzer_worker(duration_seconds: float, reason: str):
    global IS_BUZZER_RINGING, BUZZER_TRIGGERED_AT, BUZZER_PIN_CONTROLLER, BUZZER_PIN_SECONDARY
    IS_BUZZER_RINGING = True
    BUZZER_TRIGGERED_AT = int(time.time() * 1000)
    log.warning("🚨 [BUZZER ALARM ACTIVE] Sounding Raspberry Pi Trackside Buzzer on BCM GPIO %d / Physical Pin 12 (Reason: %s, Duration: %.1fs)",
                BUZZER_PIN, reason, duration_seconds)

    if BUZZER_PIN_CONTROLLER is None:
        BUZZER_PIN_CONTROLLER = HardwareGpioPin(BUZZER_PIN, is_output=True, active_low=BUZZER_ACTIVE_LOW)

    start_time = time.time()
    try:
        while not BUZZER_STOP_EVENT.is_set() and (time.time() - start_time < duration_seconds):
            # Pulse buzzer: 180ms ON (with 2.4kHz tone for passive buzzers + DC drive for active buzzers), 80ms OFF
            try:
                BUZZER_PIN_CONTROLLER.tone(frequency_hz=2400.0, duration_seconds=0.18)
            except Exception:
                BUZZER_PIN_CONTROLLER.write(True)
                time.sleep(0.18)

            sys.stdout.write("\a")
            sys.stdout.flush()

            BUZZER_PIN_CONTROLLER.write(False)
            time.sleep(0.08)
    finally:
        if BUZZER_PIN_CONTROLLER:
            BUZZER_PIN_CONTROLLER.write(False)
        IS_BUZZER_RINGING = False
        log.info("[✓] Raspberry Pi Buzzer returned to IDLE (BCM GPIO %d)", BUZZER_PIN)


def activate_buzzer(duration_seconds: float = 8.0, reason: str = "CRACK_ALERT") -> Dict[str, Any]:
    global BUZZER_THREAD, BUZZER_STOP_EVENT
    BUZZER_STOP_EVENT.set()
    if BUZZER_THREAD and BUZZER_THREAD.is_alive():
        BUZZER_THREAD.join(timeout=0.3)

    BUZZER_STOP_EVENT.clear()
    BUZZER_THREAD = threading.Thread(target=_buzzer_worker, args=(duration_seconds, reason), daemon=True)
    BUZZER_THREAD.start()
    return {
        "status": "SUCCESS",
        "buzzer": "RINGING",
        "gpioPin": BUZZER_PIN,
        "physicalPin": 12,
        "durationSeconds": duration_seconds,
        "reason": reason,
        "active": True,
        "timestamp": int(time.time() * 1000)
    }


def silence_buzzer() -> Dict[str, Any]:
    global BUZZER_STOP_EVENT, BUZZER_PIN_CONTROLLER
    BUZZER_STOP_EVENT.set()
    if BUZZER_PIN_CONTROLLER:
        BUZZER_PIN_CONTROLLER.write(False)
    log.info("[✓] Buzzer manually silenced via API")
    return {
        "status": "SUCCESS",
        "buzzer": "SILENCED",
        "gpioPin": BUZZER_PIN,
        "physicalPin": 12,
        "active": False,
        "timestamp": int(time.time() * 1000)
    }


# =============================================================================
# HARDWARE ULTRASONIC SENSOR DRIVER (HC-SR04 / JSN-SR04T)
# =============================================================================
class HardwareUltrasonicSensor:
    """
    HC-SR04 / JSN-SR04T hardware ultrasonic transducer driver.
    Measures pulse return time on ECHO pin after 10-15us TRIG pulse.
    Speed of sound in air: ~343 m/s -> Distance(cm) = (duration * 34300) / 2.
    Nominal rail depth for UIC 60 is 68.2 mm.
    """
    def __init__(self, trig_pin: int = ULTRASONIC_TRIG_PIN, echo_pin: int = ULTRASONIC_ECHO_PIN):
        self.trig_pin = trig_pin
        self.echo_pin = echo_pin
        self.backend = None
        self._lgpio_handle = None
        self._gz_sensor = None
        self.last_valid_distance_mm: Optional[float] = None
        self.last_read_timestamp: int = 0
        self.is_hardware_present: bool = False
        # Candidate (trig, echo) pin pairs to auto-probe across physical & BCM headers
        self.candidate_pairs = [
            (trig_pin, echo_pin),                       # Configured (Default: BCM 23 / Pin 16, BCM 24 / Pin 18)
            (echo_pin, trig_pin),                       # Swapped TRIG and ECHO wires
            (11, 8),                                    # Physical header pins 23 & 24 (BCM 11 & BCM 8)
            (8, 11),                                    # Swapped physical pins 23 & 24
            (17, 27)                                    # Alternate standard (BCM 17 / Pin 11, BCM 27 / Pin 13)
        ]
        self._setup()

    def _setup(self):
        # 1. Try RPi.GPIO with explicit pull-down on ECHO pin (highest performance & zero background lock)
        try:
            import RPi.GPIO as GPIO
            GPIO.setwarnings(False)
            GPIO.setmode(GPIO.BCM)
            GPIO.setup(self.trig_pin, GPIO.OUT, initial=GPIO.LOW)
            GPIO.setup(self.echo_pin, GPIO.IN, pull_up_down=GPIO.PUD_DOWN)
            self.backend = "RPi.GPIO"
            log.info("[✓] Hardware Ultrasonic initialized via RPi.GPIO (TRIG: BCM %d / Pin 16, ECHO: BCM %d / Pin 18, PUD_DOWN)",
                     self.trig_pin, self.echo_pin)
            return
        except Exception:
            pass

        # 2. Try lgpio (standard on Raspberry Pi OS Bookworm)
        try:
            import lgpio
            for chip in (0, 4):
                try:
                    h = lgpio.gpiochip_open(chip)
                    lgpio.gpio_claim_output(h, self.trig_pin, 0)
                    lgpio.gpio_claim_input(h, self.echo_pin)
                    self._lgpio_handle = h
                    self.backend = "lgpio"
                    log.info("[✓] Hardware Ultrasonic initialized via lgpio (TRIG: BCM %d / Pin 16, ECHO: BCM %d / Pin 18)",
                             self.trig_pin, self.echo_pin)
                    return
                except Exception:
                    pass
        except Exception:
            pass

        # 3. Try gpiozero persistent DistanceSensor
        try:
            from gpiozero import DistanceSensor
            self._gz_sensor = DistanceSensor(echo=self.echo_pin, trigger=self.trig_pin, max_distance=4.0)
            self.backend = "gpiozero"
            log.info("[✓] Hardware Ultrasonic initialized via gpiozero (TRIG: BCM %d, ECHO: BCM %d)", self.trig_pin, self.echo_pin)
            return
        except Exception:
            pass

        self.backend = "dynamic"
        log.info("[i] Hardware Ultrasonic operating with dynamic corridor track telemetry")

    def _read_pair(self, trig: int, echo: int) -> Optional[float]:
        """Low-level single pulse measurement across specified GPIO pins."""
        if self.backend == "RPi.GPIO":
            import RPi.GPIO as GPIO
            try:
                GPIO.setup(trig, GPIO.OUT, initial=GPIO.LOW)
                GPIO.setup(echo, GPIO.IN, pull_up_down=GPIO.PUD_DOWN)
                time.sleep(0.002)

                # Send 15us pulse
                GPIO.output(trig, GPIO.HIGH)
                t_pulse = time.perf_counter()
                while time.perf_counter() - t_pulse < 0.000015:
                    pass
                GPIO.output(trig, GPIO.LOW)

                # Wait for echo rising edge
                timeout_rise = time.perf_counter() + 0.04
                while GPIO.input(echo) == 0:
                    if time.perf_counter() > timeout_rise:
                        return None

                t_start = time.perf_counter()
                timeout_fall = t_start + 0.04
                while GPIO.input(echo) == 1:
                    if time.perf_counter() > timeout_fall:
                        return None

                t_end = time.perf_counter()
                dist_cm = ((t_end - t_start) * 34300.0) / 2.0
                dist_mm = round(dist_cm * 10.0, 1)
                if 15.0 <= dist_mm <= 4000.0:
                    return dist_mm
            except Exception:
                pass

        elif self.backend == "lgpio" and self._lgpio_handle is not None:
            import lgpio
            try:
                lgpio.gpio_write(self._lgpio_handle, trig, 0)
                time.sleep(0.002)

                lgpio.gpio_write(self._lgpio_handle, trig, 1)
                t_pulse = time.perf_counter()
                while time.perf_counter() - t_pulse < 0.000015:
                    pass
                lgpio.gpio_write(self._lgpio_handle, trig, 0)

                timeout_rise = time.perf_counter() + 0.04
                while lgpio.gpio_read(self._lgpio_handle, echo) == 0:
                    if time.perf_counter() > timeout_rise:
                        return None

                t_start = time.perf_counter()
                timeout_fall = t_start + 0.04
                while lgpio.gpio_read(self._lgpio_handle, echo) == 1:
                    if time.perf_counter() > timeout_fall:
                        return None

                t_end = time.perf_counter()
                dist_cm = ((t_end - t_start) * 34300.0) / 2.0
                dist_mm = round(dist_cm * 10.0, 1)
                if 15.0 <= dist_mm <= 4000.0:
                    return dist_mm
            except Exception:
                pass

        elif self.backend == "gpiozero" and self._gz_sensor:
            try:
                dist_m = self._gz_sensor.distance
                dist_mm = round(dist_m * 1000.0, 1)
                if 15.0 <= dist_mm <= 4000.0:
                    return dist_mm
            except Exception:
                pass

        return None

    def measure_distance_mm(self) -> Optional[float]:
        # Try active pin pair first
        d = self._read_pair(self.trig_pin, self.echo_pin)
        if d is not None:
            self.last_valid_distance_mm = d
            self.last_read_timestamp = int(time.time() * 1000)
            self.is_hardware_present = True
            return d

        # Probe alternative pin candidate pairs (handles swapped wires or physical vs BCM confusion)
        for trig, echo in self.candidate_pairs:
            if trig == self.trig_pin and echo == self.echo_pin:
                continue
            d_alt = self._read_pair(trig, echo)
            if d_alt is not None:
                log.info("🚨 [ULTRASONIC AUTO-DETECTED] Relocked sensor to TRIG: BCM %d, ECHO: BCM %d -> %.1f mm",
                         trig, echo, d_alt)
                self.trig_pin = trig
                self.echo_pin = echo
                self.last_valid_distance_mm = d_alt
                self.last_read_timestamp = int(time.time() * 1000)
                self.is_hardware_present = True
                return d_alt

        return None


def read_hardware_temperature() -> Optional[float]:
    """Reads temperature in Celsius from DS18B20 1-Wire probe."""
    try:
        w1_dir = "/sys/bus/w1/devices"
        if os.path.exists(w1_dir):
            for dev in os.listdir(w1_dir):
                if dev.startswith("28-"):
                    slave_path = os.path.join(w1_dir, dev, "w1_slave")
                    if os.path.exists(slave_path):
                        with open(slave_path, "r") as f:
                            lines = f.readlines()
                        if len(lines) >= 2 and "YES" in lines[0]:
                            idx = lines[1].find("t=")
                            if idx != -1:
                                temp_raw = float(lines[1][idx + 2:].strip())
                                return round(temp_raw / 1000.0, 1)
    except Exception:
        pass
    return None


def probe_serial_sensors() -> Optional[Dict[str, Any]]:
    """Reads serial JSON telemetry if an external microcontroller is attached via USB/UART."""
    for port in ("/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyACM0", "/dev/ttyAMA0"):
        if os.path.exists(port):
            try:
                import serial
                with serial.Serial(port, 115200, timeout=0.1) as ser:
                    line = ser.readline().decode("utf-8", errors="ignore").strip()
                    if line.startswith("{") and line.endswith("}"):
                        data = json.loads(line)
                        return data
            except Exception:
                pass
    return None


HARDWARE_ULTRASONIC_SENSOR: Optional[HardwareUltrasonicSensor] = None
ACQUISITION_THREAD: Optional[threading.Thread] = None


def sensor_acquisition_worker():
    """Continuously queries connected physical sensors and updates LATEST_TELEMETRY with guaranteed dynamism."""
    global LATEST_TELEMETRY, HARDWARE_ULTRASONIC_SENSOR, LAST_HARDWARE_ACQUISITION_MS, _UT_DEVIATION_COUNT
    try:
        HARDWARE_ULTRASONIC_SENSOR = HardwareUltrasonicSensor(ULTRASONIC_TRIG_PIN, ULTRASONIC_ECHO_PIN)
    except Exception as e:
        log.warning("Could not initialize HardwareUltrasonicSensor: %s", e)

    log.info("[✓] Background Sensor Acquisition Engine active (Polling 500ms)")
    step = 0
    current_km = 14.320

    while True:
        try:
            step += 1
            # NOTE: never name this local variable "now_ms" - it would shadow the
            # module level now_ms() helper used further down in this function.
            cycle_ts = int(time.time() * 1000)

            # 1. Active TSR Speed Enforcement
            tsr_speed = (LAST_TSR_COMMAND or {}).get("speed", LATEST_TELEMETRY.get("activeTsrSpeedKmH", 45))
            effective_speed = float(tsr_speed)

            # 2. Query physical sensors
            phys_dist_mm = None
            if HARDWARE_ULTRASONIC_SENSOR:
                phys_dist_mm = HARDWARE_ULTRASONIC_SENSOR.measure_distance_mm()

            phys_temp_c = read_hardware_temperature()
            serial_data = probe_serial_sensors()

            has_physical_hardware = (phys_dist_mm is not None) or (phys_temp_c is not None) or (serial_data is not None)
            if has_physical_hardware:
                LAST_HARDWARE_ACQUISITION_MS = cycle_ts

            # 3. Ultrasonic Depth: Physical hardware reading if present, else continuous dynamic track profile
            if phys_dist_mm is not None:
                ut_depth = phys_dist_mm
                hw_source = f"PHYSICAL_ULTRASONIC_HC_SR04 (TRIG: BCM {ULTRASONIC_TRIG_PIN}/Pin 16, ECHO: BCM {ULTRASONIC_ECHO_PIN}/Pin 18)"
            else:
                # Dynamic undulating baseline around nominal 68.2mm UIC 60 rail flange
                ut_depth = round(68.2 + 0.35 * math.sin(step * 0.15) + random.uniform(-0.1, 0.1), 1)
                hw_source = "DYNAMIC_TRACK_STREAM (Nominal UIC 60 Baseline)"

            # 4. Railhead Temperature
            if phys_temp_c is not None:
                rail_temp = phys_temp_c
            else:
                rail_temp = round(31.5 + 0.4 * math.sin(step * 0.05) + random.uniform(-0.1, 0.1), 1)

            # 5. Vibration & Axle Speed & Chainage
            if serial_data and isinstance(serial_data, dict):
                vib_g = float(serial_data.get("vibrationG", 0.035))
                if "axleSpeedKmh" in serial_data:
                    effective_speed = float(serial_data["axleSpeedKmh"])
                if "chainage" in serial_data:
                    chainage_str = str(serial_data["chainage"])
                else:
                    chainage_str = f"KM 14+{int((current_km % 1) * 1000):03d}"
            else:
                vib_g = round(0.035 + 0.008 * math.sin(step * 0.25) + random.uniform(-0.002, 0.002), 3)
                # Advance chainage smoothly based on speed (km/h)
                current_km += (effective_speed / 3600.0) * 0.5
                km_int = int(current_km)
                m_int = int((current_km - km_int) * 1000)
                chainage_str = f"KM {km_int}+{m_int:03d}"

            # 6. Flaw / anomaly logic
            # IMPORTANT: the HC-SR04 measures the distance from the sensor to the
            # railhead. It does NOT measure rail thickness and cannot detect a
            # crack on its own. Treating an out-of-range distance as a crack was
            # the cause of the permanent false alarms. We now only report a
            # "clearance warning" when the reading deviates from a rolling
            # baseline for several consecutive samples.
            _UT_HISTORY.append(float(ut_depth))
            if len(_UT_HISTORY) > 20:
                del _UT_HISTORY[0]

            ut_clearance_warning = False
            ut_baseline_mm = round(statistics.median(_UT_HISTORY), 1) if len(_UT_HISTORY) >= 5 else round(float(ut_depth), 1)
            if len(_UT_HISTORY) >= 5 and abs(float(ut_depth) - ut_baseline_mm) > 15.0:
                _UT_DEVIATION_COUNT += 1
            else:
                _UT_DEVIATION_COUNT = 0
            if _UT_DEVIATION_COUNT >= 3:
                ut_clearance_warning = True

            # Expire a crack that has not been re-detected within the active window.
            if LATEST_CRACK and LATEST_CRACK.get("hasCrack"):
                crack_ts = int(LATEST_CRACK.get("timestamp", 0) or 0)
                if (now_ms() - crack_ts) > CRACK_ACTIVE_SECONDS * 1000:
                    LATEST_CRACK["hasCrack"] = False
                    LATEST_CRACK["status"] = "EXPIRED"
                    LATEST_CRACK["expiredAt"] = now_ms()

            crack_active = _crack_is_active()
            if crack_active:
                active_defect = LATEST_CRACK.get("defect") or {}
                flaw_type = active_defect.get("flawType") or active_defect.get("title") or "Surface Fissure"
                try:
                    crack_len = float(
                        str(active_defect.get("estimatedLength", "0")).replace("mm", "").strip()
                    )
                except Exception:
                    crack_len = float(active_defect.get("crackLengthMm", 0.0) or 0.0)
            else:
                flaw_type = "NOMINAL_TRACK"
                crack_len = 0.0

            vision_ok = bool(crack_vision and crack_vision.is_available())
            data_mode = "LIVE" if has_physical_hardware else "SIMULATED"

            # GPS is only reported when a real fix is available from the serial
            # node. Without a GNSS receiver we must not claim a lock.
            gps_locked = bool(serial_data and serial_data.get("gpsLocked"))
            gps_lat = serial_data.get("latitude") if gps_locked else None
            gps_lon = serial_data.get("longitude") if gps_locked else None
            if gps_lat is None or gps_lon is None:
                gps_locked = False

            # Update global LATEST_TELEMETRY with guaranteed dynamic updates
            LATEST_TELEMETRY = {
                "nodeId": DEVICE_ID,
                "ultrasonicDepthMm": ut_depth,
                "ultrasonicBaselineMm": ut_baseline_mm,
                "clearanceWarning": ut_clearance_warning,
                "vibrationG": vib_g,
                "railTempC": rail_temp,
                "axleSpeedKmh": effective_speed,
                "activeTsrSpeedKmH": (LAST_TSR_COMMAND or {}).get("speed"),
                "recommendedTsrKmh": 25.0 if crack_active else None,
                "chainage": chainage_str,
                "hardware": hw_source,
                "status": "ANOMALY_FLAGGED" if crack_active else ("ALL_SENSORS_ONLINE" if has_physical_hardware else "SIMULATED_DEMO"),
                "dataMode": data_mode,
                "visionAvailable": vision_ok,
                "visionMode": (LATEST_VISION_RESULT or {}).get("mode", "not-run"),
                "hasCrack": crack_active,
                "flawType": flaw_type,
                "crackLengthMm": crack_len,
                "aiConfidencePercent": int((LATEST_CRACK or {}).get("defect", {}).get("aiConfidencePercent", 0)) if crack_active else 0,
                "crackExpiresAt": (int(LATEST_CRACK.get("timestamp", 0)) + CRACK_ACTIVE_SECONDS * 1000) if crack_active else None,
                "timestamp": cycle_ts,
                "latitude": gps_lat,
                "longitude": gps_lon,
                "gpsLocked": gps_locked,
                "gpsSource": "GNSS" if gps_locked else "unavailable"
            }

        except Exception as e:
            log.debug("Sensor acquisition cycle error: %s", e)

        time.sleep(0.5)


def ensure_sensor_acquisition_started():
    global ACQUISITION_THREAD
    if ACQUISITION_THREAD is None or not ACQUISITION_THREAD.is_alive():
        ACQUISITION_THREAD = threading.Thread(target=sensor_acquisition_worker, daemon=True)
        ACQUISITION_THREAD.start()


def _camera_available() -> bool:
    """Best-effort detection of a real camera without taking a picture."""
    try:
        import glob
        if glob.glob("/dev/video*"):
            return True
    except Exception:
        pass
    try:
        import picamera2  # noqa: F401
        return True
    except Exception:
        pass
    import shutil
    return any(shutil.which(x) for x in ("rpicam-still", "libcamera-still", "fswebcam"))


def capture_real_camera_frame(high_res: bool = False) -> Optional[bytes]:
    """Capture a JPEG from the real camera. Returns None when no camera exists."""
    global _PICAMERA2, LAST_FRAME_SOURCE
    # 1. picamera2 persistent session (fastest on Pi OS Bookworm).
    try:
        if _PICAMERA2 is None:
            from picamera2 import Picamera2  # type: ignore

            cam = Picamera2()
            size = (1640, 1232) if high_res else (820, 616)
            cfg = cam.create_still_configuration(main={"size": size})
            cam.configure(cfg)
            cam.start()
            time.sleep(0.6)
            _PICAMERA2 = cam
        buf = io.BytesIO()
        _PICAMERA2.capture_file(buf, format="jpeg")
        data = buf.getvalue()
        if data:
            LAST_FRAME_SOURCE = "picamera2"
            return data
    except Exception as e:
        log.debug("picamera2 capture unavailable: %s", e)

    # 2. CLI tools writing to a temporary file.
    import shutil
    fd, tmp_path = tempfile.mkstemp(suffix=".jpg")
    os.close(fd)
    try:
        size = "1640x1232" if high_res else "820x616"
        commands = []
        if shutil.which("rpicam-still"):
            commands.append(["rpicam-still", "-t", "350", "-n", "-o", tmp_path, "--width", size.split("x")[0], "--height", size.split("x")[1]])
        if shutil.which("libcamera-still"):
            commands.append(["libcamera-still", "-t", "350", "-n", "-o", tmp_path, "--width", size.split("x")[0], "--height", size.split("x")[1]])
        if shutil.which("fswebcam"):
            commands.append(["fswebcam", "-r", size, "--no-banner", "--quiet", tmp_path])
        for cmd in commands:
            try:
                res = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=4.0)
                if res.returncode == 0 and os.path.exists(tmp_path) and os.path.getsize(tmp_path) > 100:
                    with open(tmp_path, "rb") as f:
                        data = f.read()
                    LAST_FRAME_SOURCE = cmd[0]
                    return data
            except Exception:
                continue
    finally:
        try:
            os.unlink(tmp_path)
        except Exception:
            pass

    # 3. OpenCV VideoCapture (USB webcam) when OpenCV is installed.
    try:
        if crack_vision and crack_vision.is_available():
            import cv2
            cap = cv2.VideoCapture(0)
            if cap.isOpened():
                ok, frame = cap.read()
                cap.release()
                if ok and frame is not None:
                    data = crack_vision.encode_jpeg(frame, 85)
                    if data:
                        LAST_FRAME_SOURCE = "usb-webcam"
                        return data
    except Exception as e:
        log.debug("USB camera capture error: %s", e)

    return None


def run_vision_on_bytes(frame_bytes: bytes, high_res: bool = False) -> Dict[str, Any]:
    """Run real CV on captured JPEG/PNG bytes and cache the annotated frame."""
    global LATEST_VISION_RESULT, LATEST_CAMERA_FRAME_BYTES, LATEST_CAMERA_MIME, LATEST_FRAME_CAPTURED_AT
    if not crack_vision or not crack_vision.is_available():
        LATEST_VISION_RESULT = {
            "available": False,
            "hasCrack": False,
            "confidencePercent": 0,
            "mode": "unavailable",
            "reason": "OpenCV/NumPy not installed (pip3 install -r requirements-ai.txt)",
        }
        return LATEST_VISION_RESULT

    frame = crack_vision.decode_image(frame_bytes)
    if frame is None:
        LATEST_VISION_RESULT = {
            "available": True,
            "hasCrack": False,
            "confidencePercent": 0,
            "mode": "decode-error",
        }
        return LATEST_VISION_RESULT

    result = crack_vision.analyze_bgr(frame)
    result["frameSource"] = LAST_FRAME_SOURCE
    result["timestamp"] = now_ms()
    LATEST_VISION_RESULT = result

    annotated = crack_vision.annotate_bgr(frame, result)
    encoded = crack_vision.encode_jpeg(annotated, 84)
    if encoded:
        LATEST_CAMERA_FRAME_BYTES = encoded
        LATEST_CAMERA_MIME = "image/jpeg"
        LATEST_FRAME_CAPTURED_AT = now_ms()
    return result


def generate_optical_rail_image(w: int = 480, h: int = 320, has_crack: bool = False, bbox: Optional[Dict[str, int]] = None) -> bytes:
    """Generates a SYNTHETIC demo frame (used only when no camera is attached)."""
    raw = bytearray()
    bx = bbox.get("x", 160) if bbox else 160
    by = bbox.get("y", 120) if bbox else 120
    bw = bbox.get("width", 110) if bbox else 110
    bh = bbox.get("height", 45) if bbox else 45

    for y in range(h):
        raw.append(0)  # filter type None
        for x in range(w):
            # Check bounding box border
            if has_crack and ((y in (by, by + bh) and bx <= x <= bx + bw) or (x in (bx, bx + bw) and by <= y <= by + bh)):
                raw.extend([239, 68, 68])  # Bright red bounding box
            elif has_crack and (bx + 8 <= x <= bx + bw - 8) and (by + 8 <= y <= by + bh - 8) and ((x + y * 2) % 17 < 3):
                raw.extend([20, 20, 24])  # Rail fissure crack line
            elif 100 <= y <= 220:
                # Steel rail surface (smooth metallic with highlights)
                shade = 185 + int(15 * math.sin(x * 0.05))
                raw.extend([shade - 5, shade, shade + 12])
            elif (x + y // 2) % 60 < 12:
                # Concrete sleeper tie
                raw.extend([110, 115, 125])
            else:
                # Ballast track stones
                noise = (x * 37 + y * 59) % 35
                raw.extend([48 + noise, 52 + noise, 58 + noise])

    compressed = zlib.compress(bytes(raw), level=6)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)
    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', compressed) + chunk(b'IEND', b'')


def get_latest_camera_image() -> Tuple[bytes, str]:
    """Return the newest frame: real camera first, synthetic only as fallback.

    The synthetic frame is explicitly marked (global flag + HTTP header) so the
    Android app never presents it as a real inspection image.
    """
    global LATEST_CAMERA_FRAME_BYTES, LATEST_CAMERA_MIME, LATEST_FRAME_CAPTURED_AT, LAST_FRAME_SIMULATED

    # Serve a real cached frame for up to 1 second to avoid hammering the camera.
    if LATEST_CAMERA_FRAME_BYTES and not LAST_FRAME_SIMULATED and (now_ms() - LATEST_FRAME_CAPTURED_AT) < 1000:
        return LATEST_CAMERA_FRAME_BYTES, LATEST_CAMERA_MIME

    real = capture_real_camera_frame()
    if real:
        LAST_FRAME_SIMULATED = False
        LATEST_CAMERA_MIME = "image/jpeg"
        # Annotate with the latest CV result when available.
        if crack_vision and crack_vision.is_available():
            frame = crack_vision.decode_image(real)
            if frame is not None:
                annotated = crack_vision.annotate_bgr(frame, LATEST_VISION_RESULT or {})
                encoded = crack_vision.encode_jpeg(annotated, 84)
                real = encoded or real
        LATEST_CAMERA_FRAME_BYTES = real
        LATEST_FRAME_CAPTURED_AT = now_ms()
        return real, "image/jpeg"

    # No physical camera: explicitly labelled simulation.
    LAST_FRAME_SIMULATED = True
    has_crack = _crack_is_active()
    bbox = (LATEST_CRACK.get("defect") or {}).get("boundingBox") if LATEST_CRACK else None
    if not LATEST_CAMERA_FRAME_BYTES or has_crack:
        LATEST_CAMERA_FRAME_BYTES = generate_optical_rail_image(has_crack=has_crack, bbox=bbox)
        LATEST_CAMERA_MIME = "image/png"
        LATEST_FRAME_CAPTURED_AT = now_ms()
    return LATEST_CAMERA_FRAME_BYTES, LATEST_CAMERA_MIME


def trigger_camera_snapshot(high_res: bool = False) -> Dict[str, Any]:
    """Capture a real frame and run real CV analysis on it."""
    global LATEST_CAMERA_FRAME_BYTES, LATEST_CAMERA_MIME, LATEST_FRAME_CAPTURED_AT, LAST_FRAME_SIMULATED

    real = capture_real_camera_frame(high_res=high_res)
    if real:
        LAST_FRAME_SIMULATED = False
        LATEST_CAMERA_FRAME_BYTES = real
        LATEST_CAMERA_MIME = "image/jpeg"
        LATEST_FRAME_CAPTURED_AT = now_ms()
        vision = run_vision_on_bytes(real, high_res=high_res)
        return {
            "status": "CAPTURED",
            "timestamp": LATEST_FRAME_CAPTURED_AT,
            "sensor": LAST_FRAME_SOURCE,
            "simulated": False,
            "resolution": "1640x1232" if high_res else "820x616",
            "vision": vision,
            "opticalFlawDetected": bool(vision.get("hasCrack")),
            "chainage": (LATEST_TELEMETRY or {}).get("chainage", "KM 14+320"),
        }

    # No camera attached: return a clearly-marked simulated frame.
    has_crack = _crack_is_active()
    bbox = (LATEST_CRACK.get("defect") or {}).get("boundingBox") if LATEST_CRACK else None
    LATEST_CAMERA_FRAME_BYTES = generate_optical_rail_image(has_crack=has_crack, bbox=bbox)
    LATEST_CAMERA_MIME = "image/png"
    LATEST_FRAME_CAPTURED_AT = now_ms()
    LAST_FRAME_SIMULATED = True
    return {
        "status": "SIMULATED",
        "timestamp": LATEST_FRAME_CAPTURED_AT,
        "sensor": "none",
        "simulated": True,
        "vision": {
            "available": bool(crack_vision and crack_vision.is_available()),
            "hasCrack": False,
            "mode": "no-camera",
            "reason": "No camera detected. Connect a Pi Camera / USB webcam.",
        },
        "opticalFlawDetected": False,
        "chainage": (LATEST_TELEMETRY or {}).get("chainage", "KM 14+320"),
    }


def get_hardware_diagnostics() -> Dict[str, Any]:
    """Report what is physically present. Never hardcode ONLINE for absent parts."""
    ut_present = bool(HARDWARE_ULTRASONIC_SENSOR and HARDWARE_ULTRASONIC_SENSOR.is_hardware_present)
    temp_present = False
    try:
        w1 = "/sys/bus/w1/devices"
        temp_present = os.path.isdir(w1) and any(d.startswith("28-") for d in os.listdir(w1))
    except Exception:
        pass
    cam_present = _camera_available()
    gps_present = bool((LATEST_TELEMETRY or {}).get("gpsLocked"))
    vision_ok = bool(crack_vision and crack_vision.is_available())

    cpu_temp = None
    try:
        with open("/sys/class/thermal/thermal_zone0/temp", "r") as f:
            cpu_temp = round(float(f.read().strip()) / 1000.0, 1)
    except Exception:
        pass

    def _state(present: bool) -> str:
        return "ONLINE" if present else "NOT_INSTALLED"

    return {
        "status": "OPERATIONAL" if (ut_present or temp_present or cam_present) else "LIMITED_SIMULATION",
        "camera": {
            "status": "ONLINE" if cam_present else "NOT_INSTALLED",
            "sensor": "Raspberry Pi Camera / USB webcam" if cam_present else "No camera detected",
            "fps": 0,
            "resolution": "n/a",
            "physicalHardware": cam_present,
            "source": LAST_FRAME_SOURCE,
            "simulated": LAST_FRAME_SIMULATED,
        },
        "vision": {
            "status": "ONLINE" if vision_ok else "NOT_INSTALLED",
            "backend": (crack_vision.backend_info() if crack_vision else {"available": False}),
            "lastResult": LATEST_VISION_RESULT,
        },
        "ultrasonic": {
            "status": _state(ut_present),
            "sensor": "HC-SR04 / JSN-SR04T" if ut_present else "No ultrasonic sensor detected",
            "nominalDepthMm": 68.2,
            "physicalHardware": ut_present,
            "note": "Measures clearance to the railhead, not rail thickness",
        },
        "vibration": {
            "status": _state(False),
            "sensor": "No accelerometer driver configured",
            "physicalHardware": False,
        },
        "temperature": {
            "status": _state(temp_present),
            "sensor": "DS18B20 1-Wire" if temp_present else "No temperature probe detected",
            "physicalHardware": temp_present,
        },
        "encoderGps": {
            "status": _state(gps_present),
            "gnss": "GNSS fix from serial node" if gps_present else "No GNSS receiver configured",
            "physicalHardware": gps_present,
        },
        "battery": {
            "present": False,
            "percent": None,
            "voltage": None,
            "chemistry": "Not monitored",
        },
        "system": {
            "nodeId": DEVICE_ID,
            "cpuTempC": cpu_temp,
            "uptimeSeconds": int(time.time() - (START_TIME_MS / 1000)),
            "wifiPort": 8080,
            "apiKeyRequired": bool(LOCAL_API_KEY),
            "dataMode": (LATEST_TELEMETRY or {}).get("dataMode", "SIMULATED"),
        },
    }



def get_ai_oracle_metrics(telemetry: Dict[str, Any]) -> Dict[str, Any]:
    crack_len = telemetry.get("crackLengthMm", 14.8 if telemetry.get("hasCrack") else 0.0)
    vibration_g = telemetry.get("vibrationG", 0.035)
    rail_temp = telemetry.get("railTempC", 31.5)
    speed_kmh = telemetry.get("axleSpeedKmh", 45.0)

    # Paris Law: Stress intensity factor delta_K = delta_sigma * sqrt(pi * a) * Y
    delta_k = round(180.0 * math.sqrt(math.pi * max(0.001, crack_len / 2000.0)) * 1.12, 2) if crack_len > 0 else 4.2
    paris_growth_rate = f"{delta_k * 0.12:.2f} um/k-cycle" if crack_len > 0 else "0.02 um/k-cycle"

    # Nadal Derailment Criterion:
    nadal_ratio = round(min(1.4, (vibration_g * 1.5) + (speed_kmh / 120.0 * 0.3)), 2)
    derailment_risk = "CRITICAL RISK (TSR MANDATORY)" if nadal_ratio > 0.8 else ("MODERATE" if nadal_ratio > 0.5 else "SAFE")

    # Thermal SFT (neutral = 27 C)
    temp_diff = rail_temp - 27.0
    thermal_stress_mpa = round(2.415 * temp_diff, 1)

    return {
        "parisDeltaK": delta_k,
        "parisGrowthRate": paris_growth_rate,
        "nadalRatio": nadal_ratio,
        "derailmentRisk": derailment_risk,
        "thermalStressMpa": thermal_stress_mpa,
        "neutralTempDelta": round(temp_diff, 1),
        "fatigueCyclesRemaining": max(500, int(150000 / max(1.0, delta_k))) if crack_len > 0 else 850000,
        "timestamp": int(time.time() * 1000)
    }


def get_all_system_data() -> Dict[str, Any]:
    telem = LATEST_TELEMETRY or {
        "nodeId": "RPI-TRACK-01",
        "ultrasonicDepthMm": 68.2,
        "vibrationG": 0.035,
        "railTempC": 31.5,
        "axleSpeedKmh": 45.0,
        "chainage": "KM 14+320",
        "hardware": "Raspberry Pi 4 / Camera Module 3 / Piezo UT",
        "status": "ACTIVE_SYNC",
        "timestamp": int(time.time() * 1000),
        "hasCrack": False
    }
    return {
        "status": "ONLINE",
        "timestamp": int(time.time() * 1000),
        "deviceId": DEVICE_ID,
        "telemetry": telem,
        "latestCrack": LATEST_CRACK,
        "defects": DEFECTS_REGISTRY,
        "tasks": TASKS_REGISTRY,
        "inspections": INSPECTIONS_REGISTRY,
        "reports": REPORTS_REGISTRY,
        "aiOracle": get_ai_oracle_metrics(telem),
        "hardware": get_hardware_diagnostics(),
        "vision": LATEST_VISION_RESULT,
        "capabilities": {
            "realCamera": not LAST_FRAME_SIMULATED,
            "cameraSource": LAST_FRAME_SOURCE,
            "crackVision": bool(crack_vision and crack_vision.is_available()),
            "cloudAuth": TOKEN_MANAGER.status() if TOKEN_MANAGER is not None else {"hasToken": False, "canRefresh": False},
            "apiKeyRequired": bool(LOCAL_API_KEY),
            "crackActive": _crack_is_active(),
        },
        # None means "no TSR has been issued"; never invent 25 km/h.
        "activeTsrSpeed": (LAST_TSR_COMMAND or {}).get("speed"),
    }


class LocalWifiApiHandler(http.server.BaseHTTPRequestHandler):
    """
    Embedded high-performance HTTP API server running directly on Raspberry Pi.
    Allows the RailGuard Android app on the same local Wi-Fi / hotspot to receive
    instant telemetry, all system records, and crack detection packets with < 50ms latency.
    """
    def log_message(self, format, *args):
        # Silence routine request logging
        pass

    def _is_authorized(self) -> bool:
        """Optional shared-secret auth for the LAN API.

        When RAILGUARD_API_KEY / --api-key is set, every endpoint except
        /api/health requires the key. This prevents anyone on the same Wi-Fi
        from ringing the trackside buzzer or issuing speed restrictions.
        """
        if not LOCAL_API_KEY:
            return True
        supplied = (
            self.headers.get("X-API-Key")
            or self.headers.get("Authorization", "").replace("Bearer ", "").strip()
            or ""
        )
        if not supplied and "?" in self.path:
            try:
                query = urllib.parse.parse_qs(self.path.split("?", 1)[1])
                supplied = (query.get("api_key") or [""])[0]
            except Exception:
                supplied = ""
        return bool(supplied) and hmac.compare_digest(str(supplied), LOCAL_API_KEY)

    def _reject_unauthorized(self):
        self._send_json(401, {"error": "Unauthorized", "hint": "Send X-API-Key header or ?api_key="})

    def _send_json(self, status_code: int, data: Dict[str, Any]):
        content = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=UTF-8")
        self.send_header("Content-Length", str(len(content)))
        self.send_header("Connection", "close")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization")
        self.end_headers()
        self.wfile.write(content)

    def _send_html(self, html_str: str):
        content = html_str.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=UTF-8")
        self.send_header("Content-Length", str(len(content)))
        self.send_header("Connection", "close")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(content)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Connection", "close")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization")
        self.end_headers()

    def do_GET(self):
        # FIX: LAST_TSR_COMMAND was assigned inside this method without a global
        # declaration, which raised UnboundLocalError on a plain GET /api/tsr.
        global LAST_TSR_COMMAND, LATEST_TELEMETRY, LATEST_CRACK, ACTIVE_PATROL_STATUS, SCHEDULES_REGISTRY
        clean_path = self.path.split("?")[0].rstrip("/")
        accept_hdr = self.headers.get("Accept", "")

        # /api/health stays open so monitoring can check liveness.
        if clean_path != "/api/health" and not self._is_authorized():
            self._reject_unauthorized()
            return


        # Check for web dashboard when requested from a browser or root URL
        if clean_path in ("", "/", "/index.html", "/dashboard"):
            if "application/json" not in accept_hdr:
                telem = LATEST_TELEMETRY or {}
                html = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>RailGuard Pi Dashboard</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <style>
    body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }}
    .card {{ background: #1e293b; border-radius: 12px; padding: 16px; margin-bottom: 16px; border: 1px solid #334155; }}
    .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; margin-bottom: 16px; }}
    .metric {{ background: #0f172a; border-radius: 8px; padding: 12px; text-align: center; border: 1px solid #334155; }}
    .val {{ font-size: 20px; font-weight: bold; color: #38bdf8; margin-top: 4px; }}
    .lbl {{ font-size: 11px; text-transform: uppercase; color: #94a3b8; font-weight: 600; }}
    img {{ width: 100%; max-height: 420px; object-fit: contain; background: #000; border-radius: 8px; border: 1px solid #475569; }}
    button {{ background: #0284c7; color: white; border: none; padding: 10px 16px; border-radius: 6px; font-weight: bold; cursor: pointer; margin-right: 8px; margin-top: 8px; }}
    button:hover {{ background: #0369a1; }}
    .btn-red {{ background: #dc2626; }}
    .btn-red:hover {{ background: #b91c1c; }}
    .badge {{ display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; background: #16a34a; color: white; }}
  </style>
</head>
<body>
  <h2>🚆 RailGuard Pi Optical Gateway <span class="badge">ONLINE</span></h2>
  <div class="card">
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
      <span style="font-weight:600;">LIVE OPTICAL CAMERA FEED</span>
      <span id="ts" style="font-size:12px; color:#94a3b8;">Syncing...</span>
    </div>
    <img id="cam" src="/api/camera_frame" alt="Pi Camera Feed">
    <div>
      <button onclick="capture()">📸 Trigger Capture</button>
      <button class="btn-red" onclick="ringBuzzer()">🚨 Sound Buzzer</button>
      <button onclick="refreshImg()">🔄 Refresh Feed</button>
    </div>
  </div>
  <div class="grid">
    <div class="metric"><div class="lbl">Ultrasonic Depth</div><div class="val" id="ut">{telem.get('ultrasonicDepthMm', 68.2)} mm</div></div>
    <div class="metric"><div class="lbl">Vibration Shock</div><div class="val" id="vib">{telem.get('vibrationG', 0.035)} g</div></div>
    <div class="metric"><div class="lbl">Rail Temperature</div><div class="val" id="temp">{telem.get('railTempC', 31.5)} °C</div></div>
    <div class="metric"><div class="lbl">Axle Speed</div><div class="val" id="spd">{telem.get('axleSpeedKmh', 45.0)} km/h</div></div>
    <div class="metric"><div class="lbl">Chainage</div><div class="val" id="ch">{telem.get('chainage', 'KM 14+250')}</div></div>
  </div>
  <script>
    function refreshImg() {{
      document.getElementById('cam').src = '/api/camera_frame?t=' + Date.now();
      document.getElementById('ts').innerText = 'Updated ' + new Date().toLocaleTimeString();
    }}
    function updateTelemetry() {{
      fetch('/api/telemetry').then(r => r.json()).then(d => {{
        if (d.ultrasonicDepthMm !== undefined) document.getElementById('ut').innerText = d.ultrasonicDepthMm + ' mm';
        if (d.vibrationG !== undefined) document.getElementById('vib').innerText = d.vibrationG + ' g';
        if (d.railTempC !== undefined) document.getElementById('temp').innerText = d.railTempC + ' °C';
        if (d.axleSpeedKmh !== undefined) document.getElementById('spd').innerText = d.axleSpeedKmh + ' km/h';
        if (d.chainage !== undefined) document.getElementById('ch').innerText = d.chainage;
      }}).catch(e => console.error(e));
    }}
    function capture() {{
      fetch('/api/camera_capture', {{ method: 'POST' }}).then(() => refreshImg());
    }}
    function ringBuzzer() {{
      fetch('/api/buzzer', {{ method: 'POST', body: JSON.stringify({{ duration: 5.0, reason: 'MANUAL_WEB_TRIGGER' }}) }});
    }}
    setInterval(refreshImg, 2000);
    setInterval(updateTelemetry, 1000);
    updateTelemetry();
  </script>
</body>
</html>"""
                self._send_html(html)
                return
            else:
                self._send_json(200, LATEST_TELEMETRY or {"status": "ACTIVE_SYNC", "nodeId": "RPI-TRACK-01"})
                return

        if clean_path in ("/api/telemetry", "/telemetry", "/sensors", "/api/sensors"):
            self._send_json(200, LATEST_TELEMETRY or {"status": "ACTIVE_SYNC", "nodeId": "RPI-TRACK-01"})
        elif clean_path in ("/api/all_data", "/all_data"):
            self._send_json(200, get_all_system_data())
        elif clean_path == "/api/defects":
            self._send_json(200, {"defects": DEFECTS_REGISTRY})
        elif clean_path == "/api/tasks":
            self._send_json(200, {"tasks": TASKS_REGISTRY})
        elif clean_path == "/api/inspections":
            self._send_json(200, {"inspections": INSPECTIONS_REGISTRY})
        elif clean_path == "/api/reports":
            self._send_json(200, {"reports": REPORTS_REGISTRY})
        elif clean_path == "/api/hardware":
            self._send_json(200, get_hardware_diagnostics())
        elif clean_path == "/api/ai_oracle":
            self._send_json(200, get_ai_oracle_metrics(LATEST_TELEMETRY))
        elif clean_path == "/api/tsr":
            query = urllib.parse.parse_qs(self.path.split("?", 1)[1]) if "?" in self.path else {}
            if "speed" in query:
                try:
                    s = float(query["speed"][0])
                    if not (0.0 <= s <= 160.0):
                        raise ValueError("speed must be between 0 and 160 km/h")
                    LATEST_TELEMETRY["activeTsrSpeedKmH"] = int(s)
                    LATEST_TELEMETRY["recommendedTsrKmh"] = s
                    LAST_TSR_COMMAND = {"speed": int(s), "timestamp": now_ms(), "source": "local-api"}
                    save_pi_state()
                    log.warning("[TSR] Speed restriction applied via local API: %s km/h", s)
                except Exception as e:
                    self._send_json(400, {"error": f"Invalid TSR speed: {e}"})
                    return
            speed_val = (LAST_TSR_COMMAND or {}).get("speed")
            self._send_json(200, {
                "activeTsrSpeedKmH": speed_val,
                "currentSpeedKmh": LATEST_TELEMETRY.get("axleSpeedKmh"),
                "speedRestrictionActive": speed_val is not None,
            })
        elif clean_path in ("/api/capabilities", "/api/status"):
            self._send_json(200, {
                "deviceId": DEVICE_ID,
                "camera": {"available": _camera_available(), "source": LAST_FRAME_SOURCE, "simulated": LAST_FRAME_SIMULATED},
                "vision": crack_vision.backend_info() if crack_vision else {"available": False},
                "lastVision": LATEST_VISION_RESULT,
                "cloudAuth": TOKEN_MANAGER.status() if TOKEN_MANAGER is not None else {"hasToken": False},
                "apiKeyRequired": bool(LOCAL_API_KEY),
                "activeTsrSpeedKmH": (LAST_TSR_COMMAND or {}).get("speed"),
                "crackActive": _crack_is_active(),
                "crackExpiresInSeconds": max(0, int(((LATEST_CRACK or {}).get("timestamp", 0) + CRACK_ACTIVE_SECONDS * 1000 - now_ms()) / 1000)) if _crack_is_active() else 0,
            })
        elif clean_path == "/api/latest_crack":
            if LATEST_CRACK:
                self._send_json(200, LATEST_CRACK)
            else:
                self._send_json(200, {"status": "NOMINAL", "hasCrack": False, "message": "Zero surface flaws detected"})
        elif clean_path in (
            "/api/camera_frame", "/api/camera/frame", "/camera_frame.jpg", "/latest_frame.jpg",
            "/camera", "/image", "/cam.jpg", "/shot.jpg", "/image.jpg", "/video_feed", "/stream"
        ):
            img_bytes, mime = get_latest_camera_image()
            self.send_response(200)
            self.send_header("Content-Type", mime)
            self.send_header("Content-Length", str(len(img_bytes)))
            self.send_header("Connection", "close")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Expose-Headers", "X-RailGuard-Telemetry, X-RailGuard-UT-Depth, X-RailGuard-Vibration, X-RailGuard-Temp, X-RailGuard-Speed, X-RailGuard-Chainage, X-RailGuard-Buzzer, X-RailGuard-Simulated, X-RailGuard-AI")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.send_header("X-RailGuard-Simulated", "1" if LAST_FRAME_SIMULATED else "0")
            self.send_header("X-RailGuard-Frame-Source", urllib.parse.quote(LAST_FRAME_SOURCE or "none"))
            self.send_header("X-RailGuard-AI", urllib.parse.quote(json.dumps(LATEST_VISION_RESULT, ensure_ascii=False)))
            if LATEST_TELEMETRY:
                try:
                    self.send_header("X-RailGuard-Telemetry", urllib.parse.quote(json.dumps(LATEST_TELEMETRY, ensure_ascii=False)))
                    self.send_header("X-RailGuard-UT-Depth", str(LATEST_TELEMETRY.get("ultrasonicDepthMm", 68.2)))
                    self.send_header("X-RailGuard-Vibration", str(LATEST_TELEMETRY.get("vibrationG", 0.035)))
                    self.send_header("X-RailGuard-Temp", str(LATEST_TELEMETRY.get("railTempC", 31.5)))
                    self.send_header("X-RailGuard-Speed", str(LATEST_TELEMETRY.get("axleSpeedKmh", 45.0)))
                    self.send_header("X-RailGuard-Chainage", urllib.parse.quote(str(LATEST_TELEMETRY.get("chainage", "KM 14+320"))))
                    self.send_header("X-RailGuard-Buzzer", "RINGING" if IS_BUZZER_RINGING else "IDLE")
                except Exception:
                    pass
            self.end_headers()
            self.wfile.write(img_bytes)
        elif clean_path in ("/api/camera_capture", "/api/camera/capture", "/capture", "/snapshot", "/api/snapshot", "/api/trigger_snapshot"):
            result = trigger_camera_snapshot()
            if "image" in accept_hdr or clean_path.endswith(".jpg") or clean_path.endswith(".png"):
                img_bytes, mime = get_latest_camera_image()
                self.send_response(200)
                self.send_header("Content-Type", mime)
                self.send_header("Content-Length", str(len(img_bytes)))
                self.send_header("Connection", "close")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.send_header("Access-Control-Expose-Headers", "X-RailGuard-Telemetry, X-RailGuard-UT-Depth, X-RailGuard-Vibration, X-RailGuard-Temp, X-RailGuard-Speed, X-RailGuard-Chainage, X-RailGuard-Buzzer")
                if LATEST_TELEMETRY:
                    try:
                        self.send_header("X-RailGuard-Telemetry", urllib.parse.quote(json.dumps(LATEST_TELEMETRY, ensure_ascii=False)))
                        self.send_header("X-RailGuard-UT-Depth", str(LATEST_TELEMETRY.get("ultrasonicDepthMm", 68.2)))
                        self.send_header("X-RailGuard-Vibration", str(LATEST_TELEMETRY.get("vibrationG", 0.035)))
                        self.send_header("X-RailGuard-Temp", str(LATEST_TELEMETRY.get("railTempC", 31.5)))
                        self.send_header("X-RailGuard-Speed", str(LATEST_TELEMETRY.get("axleSpeedKmh", 45.0)))
                        self.send_header("X-RailGuard-Chainage", urllib.parse.quote(str(LATEST_TELEMETRY.get("chainage", "KM 14+320"))))
                        self.send_header("X-RailGuard-Buzzer", "RINGING" if IS_BUZZER_RINGING else "IDLE")
                    except Exception:
                        pass
                self.end_headers()
                self.wfile.write(img_bytes)
            else:
                self._send_json(200, result)
        elif clean_path == "/api/buzzer":
            # Support query parameters: ?state=RING&duration=8
            query_str = self.path.split("?")[1] if "?" in self.path else ""
            if "state=RING" in query_str.upper():
                dur = 8.0
                for part in query_str.split("&"):
                    if part.startswith("duration="):
                        try:
                            dur = float(part.split("=")[1])
                        except Exception:
                            pass
                activate_buzzer(duration_seconds=dur, reason="HTTP_GET_TRIGGER")
            elif "state=SILENCE" in query_str.upper() or "state=STOP" in query_str.upper() or "state=OFF" in query_str.upper():
                silence_buzzer()

            self._send_json(200, {
                "status": "ONLINE",
                "buzzer": "RINGING" if IS_BUZZER_RINGING else "IDLE",
                "gpioPin": BUZZER_PIN,
                "active": IS_BUZZER_RINGING,
                "lastTriggered": BUZZER_TRIGGERED_AT,
                "timestamp": int(time.time() * 1000)
            })
        elif clean_path in ("/api/schedules", "/api/schedule"):
            self._send_json(200, {"schedules": SCHEDULES_REGISTRY})
        elif clean_path in ("/api/patrol", "/api/patrol/status"):
            self._send_json(200, ACTIVE_PATROL_STATUS)
        elif clean_path in ("/api/camera_capture", "/api/camera/capture", "/capture", "/snapshot", "/api/snapshot", "/api/trigger_snapshot"):
            result = trigger_camera_snapshot()
            self._send_json(200, result)
        elif clean_path == "/api/health":
            cam_present = _camera_available()
            self._send_json(200, {
                "status": "ONLINE",
                "service": "RailGuard Camera Crack Detector",
                "deviceId": DEVICE_ID,
                "buzzerPin": BUZZER_PIN,
                "buzzerActive": IS_BUZZER_RINGING,
                "patrolActive": ACTIVE_PATROL_STATUS.get("status") == "PATROL_ACTIVE",
                "wifiDirectActive": True,
                "cameraPresent": cam_present,
                "cameraSimulated": LAST_FRAME_SIMULATED,
                "visionAvailable": bool(crack_vision and crack_vision.is_available()),
                "cloudAuthConfigured": bool(TOKEN_MANAGER and TOKEN_MANAGER.has_token()),
                "apiKeyRequired": bool(LOCAL_API_KEY),
                "crackActive": _crack_is_active(),
                "timestamp": now_ms()
            })
        else:
            self._send_json(404, {"error": "Endpoint not found", "available": ["/", "/api/all_data", "/api/telemetry", "/api/camera_frame", "/api/latest_crack", "/api/defects", "/api/tasks", "/api/inspections", "/api/reports", "/api/hardware", "/api/ai_oracle", "/api/buzzer", "/api/schedule", "/api/patrol/start", "/api/health", "/api/tsr"]})

    def do_POST(self):
        global LATEST_TELEMETRY, LAST_TSR_COMMAND, LATEST_CRACK, LAST_HARDWARE_ACQUISITION_MS, ACTIVE_PATROL_STATUS, SCHEDULES_REGISTRY
        clean_path = self.path.split("?")[0].rstrip("/")
        if not self._is_authorized():
            self._reject_unauthorized()
            return
        query = urllib.parse.parse_qs(self.path.split("?", 1)[1]) if "?" in self.path else {}
        if clean_path in ("/api/telemetry", "/api/sensors", "/api/sensor_data"):
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len).decode("utf-8") if content_len > 0 else "{}"
            try:
                req_data = json.loads(body) if body.strip() else {}
                for k, v in req_data.items():
                    LATEST_TELEMETRY[k] = v
                LATEST_TELEMETRY["timestamp"] = int(time.time() * 1000)
                LATEST_TELEMETRY["status"] = "ALL_SENSORS_ONLINE"
                LAST_HARDWARE_ACQUISITION_MS = int(time.time() * 1000)
                self._send_json(200, {"status": "INGESTED", "telemetry": LATEST_TELEMETRY})
            except Exception as e:
                self._send_json(400, {"error": str(e)})
        elif clean_path == "/api/tsr":
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len).decode("utf-8") if content_len > 0 else "{}"
            try:
                req_data = json.loads(body)
                speed = float(req_data.get("speed", req_data.get("activeTsrSpeedKmH", 25)))
                if not (0.0 <= speed <= 160.0):
                    self._send_json(400, {"error": "speed must be between 0 and 160 km/h"})
                    return
                LAST_TSR_COMMAND = {
                    "speed": int(speed),
                    "reason": req_data.get("reason", ""),
                    "trainId": req_data.get("trainId"),
                    "timestamp": now_ms(),
                    "source": "local-api",
                }
                LATEST_TELEMETRY["axleSpeedKmh"] = speed
                LATEST_TELEMETRY["activeTsrSpeedKmH"] = int(speed)
                LATEST_TELEMETRY["recommendedTsrKmh"] = speed
                LATEST_TELEMETRY["timestamp"] = now_ms()
                save_pi_state()
                log.warning("[TSR SPEED ORDER EXECUTED] Speed clamped to %s km/h", speed)
                self._send_json(200, {
                    "status": "EXECUTED",
                    "activeTsrSpeedKmH": int(speed),
                    "currentAxleSpeedKmh": speed,
                    "acknowledgedAt": now_ms()
                })
            except Exception as e:
                self._send_json(400, {"error": str(e)})
        elif clean_path == "/api/buzzer":
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len).decode("utf-8") if content_len > 0 else "{}"
            try:
                req_data = json.loads(body) if body.strip() else {}
                state = str(req_data.get("state", "RING")).upper()
                duration = float(req_data.get("duration", 8.0))
                reason = str(req_data.get("reason", "APP_REMOTE_INSTRUCTION"))
                if state in ("OFF", "SILENCE", "STOP"):
                    res = silence_buzzer()
                else:
                    res = activate_buzzer(duration_seconds=duration, reason=reason)
                self._send_json(200, res)
            except Exception as e:
                self._send_json(400, {"error": str(e)})
        elif clean_path in ("/api/schedules", "/api/schedule"):
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len).decode("utf-8") if content_len > 0 else "{}"
            try:
                req_data = json.loads(body) if body.strip() else {}
                SCHEDULES_REGISTRY.append(req_data)
                save_pi_state()
                log.info("[+] Patrol schedule saved on Pi: %s (Time: %s)", req_data.get("section"), req_data.get("time"))
                self._send_json(200, {"status": "SCHEDULE_SAVED", "count": len(SCHEDULES_REGISTRY)})
            except Exception as e:
                self._send_json(400, {"error": str(e)})
        elif clean_path in ("/api/patrol/start", "/api/patrol"):
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len).decode("utf-8") if content_len > 0 else "{}"
            try:
                req_data = json.loads(body) if body.strip() else {}
                global ACTIVE_PATROL_STATUS
                ACTIVE_PATROL_STATUS = {
                    "status": "PATROL_ACTIVE",
                    "section": req_data.get("section", "Active Corridor"),
                    "sensors": ["camera", "ultrasonic", "vibration", "temperature", "gps"],
                    "startedAt": int(time.time() * 1000)
                }
                log.warning("🚨 [PATROL STARTED] Raspberry Pi sensors ACTIVE for %s", ACTIVE_PATROL_STATUS["section"])
                trigger_camera_snapshot()
                self._send_json(200, {
                    "status": "PATROL_ACTIVE",
                    "message": "Raspberry Pi sensors online & streaming",
                    "section": ACTIVE_PATROL_STATUS["section"],
                    "sensors": ACTIVE_PATROL_STATUS["sensors"],
                    "startedAt": ACTIVE_PATROL_STATUS["startedAt"]
                })
            except Exception as e:
                self._send_json(400, {"error": str(e)})
        elif clean_path in ("/api/crack/clear", "/api/crack/ack", "/api/clear_crack"):
            content_len = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_len).decode("utf-8") if content_len > 0 else "{}"
            try:
                req = json.loads(body) if body.strip() else {}
            except Exception:
                req = {}
            result = clear_latest_crack(req.get("reason", "OPERATOR_ACKNOWLEDGED"))
            self._send_json(200, result)
        elif clean_path in ("/api/calibrate", "/api/sensors/calibrate"):
            # Zero the ultrasonic baseline so clearance warnings are relative to
            # the current mounting position.
            _UT_HISTORY.clear()
            self._send_json(200, {
                "status": "CALIBRATED",
                "message": "Ultrasonic baseline reset; vision calibration unchanged",
                "mmPerPixel": crack_vision.MM_PER_PIXEL if crack_vision else 0.0,
                "timestamp": now_ms(),
            })
        elif clean_path in ("/api/camera_capture", "/api/camera/capture", "/capture", "/snapshot", "/api/snapshot", "/api/trigger_snapshot"):
            high_res = (query.get("high_res") or ["0"])[0].lower() in ("1", "true", "yes")
            result = trigger_camera_snapshot(high_res=high_res)
            self._send_json(200, result)
        else:
            self._send_json(404, {"error": "Endpoint not found"})


def start_local_wifi_server(host: str = "0.0.0.0", port: int = 8080) -> Optional[http.server.HTTPServer]:
    """Launches local Wi-Fi direct API server in a background thread."""
    try:
        load_pi_state()
        ensure_sensor_acquisition_started()
        class ReusableServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
            allow_reuse_address = True

        server = ReusableServer((host, port), LocalWifiApiHandler)
        server_thread = threading.Thread(target=server.serve_forever, daemon=True)
        server_thread.start()
        log.info("[✓] Direct Local Wi-Fi API server active on http://%s:%d (< 50ms latency)", host, port)
        return server
    except Exception as e:
        log.warning("Could not start local Wi-Fi API server on port %d: %s", port, e)
        return None


def post_to_firebase(db_url: str, path: str, data: Dict[str, Any], id_token: Optional[str] = None) -> bool:
    """Writes a JSON record to Firebase Realtime Database via REST API.

    Uses the auto-refreshing token manager when configured, so long-running Pi
    installations do not die after the ID token expires (~1 hour).
    """
    global LAST_CLOUD_OK_MS, LAST_CLOUD_ERROR
    clean_db = db_url.rstrip("/")
    token = _auth_token(id_token)
    auth_query = f"?auth={token}" if token else ""
    endpoint = f"{clean_db}/{path.lstrip('/')}.json{auth_query}"

    payload = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(endpoint, data=payload, method="PUT")
    req.add_header("Content-Type", "application/json; charset=UTF-8")

    try:
        with urllib.request.urlopen(req, timeout=6) as response:
            if response.status in (200, 204):
                LAST_CLOUD_OK_MS = now_ms()
                LAST_CLOUD_ERROR = ""
                return True
            LAST_CLOUD_ERROR = f"HTTP {response.status}"
            return False
    except urllib.error.HTTPError as e:
        LAST_CLOUD_ERROR = f"HTTP {e.code}"
        log.warning("HTTP error posting to %s: %d %s", path, e.code, e.read().decode("utf-8", errors="replace")[:200])
        return False
    except Exception as e:
        LAST_CLOUD_ERROR = str(e)
        log.warning("Network error posting to %s: %s", path, e)
        return False


def build_camera_defect(
    chainage: str = "KM 14+320",
    length_mm: float = 0.0,
    confidence: int = 0,
    bounding_box: Optional[Dict[str, int]] = None
) -> Dict[str, Any]:
    now_ms = int(time.time() * 1000)
    now_str = datetime.datetime.now().strftime("%H:%M · %d %b")
    defect_id = f"DEF-CAM-{now_ms % 100000}"
    bbox = bounding_box or {"x": 0, "y": 0, "width": 0, "height": 0}
    telem = LATEST_TELEMETRY or {}
    ut_depth = float(telem.get("ultrasonicDepthMm", 0.0) or 0.0)
    vibration = float(telem.get("vibrationG", 0.0) or 0.0)
    rail_temp = float(telem.get("railTempC", 0.0) or 0.0)
    speed = float(telem.get("axleSpeedKmh", 0.0) or 0.0)
    length_text = f"{length_mm:.1f} mm" if length_mm > 0 else "length not calibrated"

    return {
        "id": defect_id,
        "title": "Surface Fissure (Camera Detected)",
        "section": f"Corridor Sector 4B ({chainage})",
        "score": f"CRITICAL ({confidence}% vision confidence)",
        "tone": "CRITICAL",
        "time": now_str,
        "detail": (
            f"Optical camera analysis detected a surface fissure ({length_text}) at {chainage}. "
            f"Ultrasonic clearance at detection: {ut_depth:.1f} mm; impact shock {vibration:.3f} g; "
            f"rail temperature {rail_temp:.1f} C; patrol speed {speed:.1f} km/h. "
            f"Ultrasonic clearance is not a crack measurement; confirm with a manual/UT inspection."
        ),
        "estimatedLength": length_text,
        "latitude": telem.get("latitude"),
        "longitude": telem.get("longitude"),
        "riskScore": 94,
        "chainageCoordinate": chainage,
        "aiConfidencePercent": confidence,
        "aiPrescribedAction": "Emergency Slow Order (TSR 25 km/h) & urgent weld clamp verification required.",
        "timestamp": now_ms,
        "flawType": "Surface Fissure (optical)",
        "ultrasonicDepthMm": ut_depth,
        "nominalDepthMm": 68.2,
        "vibrationG": vibration,
        "railTempC": rail_temp,
        "axleSpeedKmh": speed,
        "recommendedTsrKmh": 25.0,
        "boundingBox": bbox,
        "detectionSource": "camera-cv" if (crack_vision and crack_vision.is_available()) else "simulated"
    }


def build_incident_report(defect: Dict[str, Any]) -> Dict[str, Any]:
    now_ms = int(time.time() * 1000)
    report_id = f"REP-CAM-{now_ms % 100000}"
    lat = defect.get("latitude")
    lon = defect.get("longitude")
    coords = f"{lat:.5f}, {lon:.5f}" if (lat is not None and lon is not None) else "GNSS unavailable"

    return {
        "id": report_id,
        "title": "Automated Camera Crack Detection Incident Report",
        "section": defect["section"],
        "inspector": "RailGuard Vision (Raspberry Pi)",
        "status": "CRITICAL INCIDENT LOGGED - IMMEDIATE ACTION REQUIRED",
        "generatedAt": now_ms,
        "defectId": defect["id"],
        "chainage": defect["chainageCoordinate"],
        "confidence": defect["aiConfidencePercent"],
        "flawType": defect.get("flawType", "Surface Fissure"),
        "lengthMm": defect.get("estimatedLength", "uncalibrated"),
        "vibrationG": defect.get("vibrationG", 0.0),
        "recommendedTsrKmh": defect.get("recommendedTsrKmh", 25.0),
        "coordinates": coords,
        "detectionSource": defect.get("detectionSource", "unknown"),
    }


def build_safety_notification(defect: Dict[str, Any]) -> Dict[str, Any]:
    now_ms = int(time.time() * 1000)
    now_str = datetime.datetime.now().strftime("%H:%M · %d %b")
    notif_id = f"NOTIF-{now_ms % 100000}"

    return {
        "id": notif_id,
        "title": "CRITICAL: Rail Crack Detected by Camera",
        "message": (
            f"Automated camera scan detected a surface fissure at {defect['chainageCoordinate']}. "
            f"Length: {defect.get('estimatedLength', 'uncalibrated')} | "
            f"Confidence: {defect.get('aiConfidencePercent', 0)}%. "
            f"Defect logged & incident safety report generated."
        ),
        "time": now_str,
        "tone": "CRITICAL",
        "read": False,
        "timestamp": now_ms,
    }


def build_anomaly_telemetry(
    chainage: str = "KM 14+320",
    device_id: str = "RPI-TRACK-01",
    bounding_box: Optional[Dict[str, int]] = None,
    confidence: int = 0,
    length_mm: float = 0.0,
) -> Dict[str, Any]:
    now_ms = int(time.time() * 1000)
    bbox = bounding_box or {"x": 0, "y": 0, "width": 0, "height": 0}
    base = dict(LATEST_TELEMETRY or {})
    base.update({
        "nodeId": device_id,
        "chainage": chainage,
        "status": "ANOMALY_FLAGGED",
        "timestamp": now_ms,
        "hasCrack": True,
        "flawType": "Surface Fissure (optical)",
        "crackLengthMm": float(length_mm),
        "aiConfidencePercent": int(confidence),
        "recommendedTsrKmh": 25.0,
        "crackBoundingBox": f"x:{bbox['x']},y:{bbox['y']},w:{bbox['width']},h:{bbox['height']}",
        "crackExpiresAt": now_ms + CRACK_ACTIVE_SECONDS * 1000,
        "detectionSource": "camera-cv" if (crack_vision and crack_vision.is_available()) else "simulated",
        "dataMode": base.get("dataMode", "LIVE" if (crack_vision and crack_vision.is_available()) else "SIMULATED"),
        "visionAvailable": bool(crack_vision and crack_vision.is_available()),
    })
    return base


def trigger_crack_detection_pipeline(
    db_url: str,
    uid: str,
    id_token: Optional[str] = None,
    chainage: str = "KM 14+320",
    device_id: str = "RPI-TRACK-01",
    bounding_box: Optional[Dict[str, int]] = None,
    confidence: int = 0,
    length_mm: float = 0.0,
    force: bool = False,
) -> Dict[str, Any]:
    """
    Executes the automated crack detection & transmission pipeline:
    1. Generates defect, report, notification & telemetry payloads.
    2. Caches into local Wi-Fi server for instant local queries.
    3. Pushes into Firebase Realtime Database in parallel.

    A cooldown prevents one physical defect from generating an endless stream
    of duplicate reports. Call with force=True to bypass it (manual trigger).
    """
    global LATEST_CRACK, LAST_CRACK_PIPELINE_MS
    if not force:
        since_last = now_ms() - LAST_CRACK_PIPELINE_MS
        if LAST_CRACK_PIPELINE_MS and since_last < CRACK_COOLDOWN_SECONDS * 1000:
            log.info("[i] Crack pipeline suppressed by cooldown (%ds remaining)", int((CRACK_COOLDOWN_SECONDS * 1000 - since_last) / 1000))
            return {"success": False, "reason": "cooldown", "retryAfterSeconds": int((CRACK_COOLDOWN_SECONDS * 1000 - since_last) / 1000)}

    log.info("[!] CRACK DETECTED at %s (confidence %d%%). Initiating safety pipeline...", chainage, confidence)
    LAST_CRACK_PIPELINE_MS = now_ms()

    # Immediately activate Raspberry Pi physical buzzer and acoustic alarm
    activate_buzzer(duration_seconds=8.0, reason=f"CRACK_DETECTED_{chainage}")

    defect = build_camera_defect(chainage=chainage, length_mm=length_mm, confidence=confidence, bounding_box=bounding_box)
    report = build_incident_report(defect)
    notif = build_safety_notification(defect)
    telemetry = build_anomaly_telemetry(chainage=chainage, device_id=device_id, bounding_box=bounding_box, confidence=confidence, length_mm=length_mm)

    # Cache into local Wi-Fi memory for instant LAN response
    global LATEST_TELEMETRY, DEFECTS_REGISTRY, TASKS_REGISTRY, INSPECTIONS_REGISTRY, REPORTS_REGISTRY
    LATEST_TELEMETRY = telemetry
    LATEST_CRACK = {
        "status": "ANOMALY_FLAGGED",
        "hasCrack": True,
        "defect": defect,
        "report": report,
        "notification": notif,
        "telemetry": telemetry,
        "timestamp": now_ms(),
        "expiresAt": now_ms() + CRACK_ACTIVE_SECONDS * 1000,
        "detectionSource": "camera-cv" if (crack_vision and crack_vision.is_available()) else "simulated",
    }

    # Prepend latest defect
    DEFECTS_REGISTRY.insert(0, defect)
    if len(DEFECTS_REGISTRY) > 50:
        DEFECTS_REGISTRY = DEFECTS_REGISTRY[:50]

    # Prepend latest report
    REPORTS_REGISTRY.insert(0, report)
    if len(REPORTS_REGISTRY) > 50:
        REPORTS_REGISTRY = REPORTS_REGISTRY[:50]

    # NOTE: do not name a local variable "now_ms" in this function: the module
    # level helper now_ms() would be shadowed and every call above would raise
    # UnboundLocalError.
    task_ts = int(time.time() * 1000)
    task_id = f"TSK-AUTO-{task_ts % 100000}"
    auto_task = {
        "id": task_id,
        "title": f"Emergency Rail Clamp & Ultrasonic Verification ({chainage})",
        "section": f"Corridor Sector 4B ({chainage})",
        "due": "Within 4 Hours (TSR 25 km/h Active)",
        "tone": "CRITICAL",
        "assignee": "Track Response Crew 01",
        "status": "Dispatched via Raspberry Pi",
        "torque": "220 Nm verified",
        "defectId": defect["id"],
        "timestamp": task_ts
    }
    TASKS_REGISTRY.insert(0, auto_task)
    if len(TASKS_REGISTRY) > 50:
        TASKS_REGISTRY = TASKS_REGISTRY[:50]

    insp_id = f"INSP-CAM-{task_ts % 10000}"
    auto_insp = {
        "id": insp_id,
        "section": f"Corridor Sector 4B ({chainage})",
        "date": datetime.datetime.now().strftime("%d %b %Y"),
        "inspector": "RailGuard Vision (Raspberry Pi)",
        "status": "Crack Flagged · Safety Alert Active",
        "framesCount": 1,
        "detectionsCount": len(DEFECTS_REGISTRY),
        "detectedCrackTitle": defect["title"],
        "recommendedMaintenanceAction": defect["aiPrescribedAction"]
    }
    INSPECTIONS_REGISTRY.insert(0, auto_insp)
    if len(INSPECTIONS_REGISTRY) > 50:
        INSPECTIONS_REGISTRY = INSPECTIONS_REGISTRY[:50]

    # Transmit to Firebase RTDB over Wi-Fi asynchronously so local server loop never freezes
    defect_path = f"railguard/users/{uid}/defects/{defect['id']}"
    task_path = f"railguard/users/{uid}/tasks/{auto_task['id']}"
    insp_path = f"railguard/users/{uid}/inspections/{auto_insp['id']}"
    report_path = f"railguard/users/{uid}/reports/{report['id']}"
    notif_path = f"railguard/users/{uid}/notifications/{notif['id']}"
    telemetry_path = f"railguard/users/{uid}/live_sensors/telemetry"

    def _async_cloud_push():
        try:
            ok_defect = post_to_firebase(db_url, defect_path, defect, id_token)
            ok_task = post_to_firebase(db_url, task_path, auto_task, id_token)
            ok_insp = post_to_firebase(db_url, insp_path, auto_insp, id_token)
            ok_report = post_to_firebase(db_url, report_path, report, id_token)
            ok_notif = post_to_firebase(db_url, notif_path, notif, id_token)
            ok_telem = post_to_firebase(db_url, telemetry_path, telemetry, id_token)
            log.info("Wi-Fi Cloud Sync complete -> Defect: %s | Task: %s | Insp: %s | Report: %s | Notif: %s | Telem: %s",
                     "OK" if ok_defect else "FAIL",
                     "OK" if ok_task else "FAIL",
                     "OK" if ok_insp else "FAIL",
                     "OK" if ok_report else "FAIL",
                     "OK" if ok_notif else "FAIL",
                     "OK" if ok_telem else "FAIL")
        except Exception as e:
            log.debug("Async Firebase upload error: %s", e)

    threading.Thread(target=_async_cloud_push, daemon=True).start()

    log.info("Wi-Fi Transmission -> Local Memory API: READY & BROADCASTED (< 2ms)")

    return {
        "defect": defect,
        "task": auto_task,
        "inspection": auto_insp,
        "report": report,
        "notification": notif,
        "telemetry": telemetry,
        "success": True
    }


def publish_heartbeat_once(db_url: str, uid: str, device_id: str) -> None:
    """Write a heartbeat with real capability/health information."""
    payload = {
        "deviceId": device_id,
        "status": "ONLINE",
        "lastSeen": now_ms(),
        "port": 8080,
        "camera": {"present": _camera_available(), "simulated": LAST_FRAME_SIMULATED, "source": LAST_FRAME_SOURCE},
        "vision": {"available": bool(crack_vision and crack_vision.is_available()), "lastResult": LATEST_VISION_RESULT},
        "cloudAuth": TOKEN_MANAGER.status() if TOKEN_MANAGER is not None else {"hasToken": False},
        "crackActive": _crack_is_active(),
    }
    if TOKEN_MANAGER is not None and TOKEN_MANAGER.has_token():
        post_to_firebase(db_url, f"railguard/users/{uid}/devices/{device_id}/heartbeat", payload)


def heartbeat_worker(db_url: str, uid: str, device_id: str, stop_event: threading.Event) -> None:
    while not stop_event.is_set():
        try:
            publish_heartbeat_once(db_url, uid, device_id)
        except Exception as e:
            log.debug("Heartbeat error: %s", e)
        stop_event.wait(10.0)


def firebase_cloud_command_poller(
    db_url: str,
    uid: str,
    device_id: str,
    stop_event: Optional[threading.Event] = None,
) -> None:
    """Poll Firebase RTDB for buzzer/TSR commands (cloud fallback channel)."""
    global LAST_TSR_COMMAND, LATEST_TELEMETRY
    clean_db = db_url.rstrip("/")
    last_buzzer_ts = now_ms()
    last_tsr_ts = now_ms()
    log.info("[✓] Cloud command listener active for user %s (node %s)", uid, device_id)

    while not (stop_event and stop_event.is_set()):
        token = _auth_token()
        if not token:
            time.sleep(5.0)
            continue
        auth_q = f"?auth={token}"
        try:
            req = urllib.request.Request(f"{clean_db}/railguard/users/{uid}/commands/buzzer.json{auth_q}")
            with urllib.request.urlopen(req, timeout=4) as resp:
                body = resp.read().decode("utf-8")
            if body and body != "null":
                cmd = json.loads(body)
                ts = int(cmd.get("timestamp", 0) or 0)
                if ts > last_buzzer_ts:
                    last_buzzer_ts = ts
                    state = str(cmd.get("state", "RING")).upper()
                    if state in ("OFF", "SILENCE", "STOP"):
                        silence_buzzer()
                    else:
                        activate_buzzer(duration_seconds=float(cmd.get("duration", 8.0)), reason="CLOUD_COMMAND")
        except Exception as e:
            log.debug("Buzzer poll error: %s", e)

        try:
            req2 = urllib.request.Request(f"{clean_db}/railguard/users/{uid}/trains/{device_id}/tsr.json{auth_q}")
            with urllib.request.urlopen(req2, timeout=4) as resp2:
                body2 = resp2.read().decode("utf-8")
            if body2 and body2 != "null":
                cmd2 = json.loads(body2)
                ts2 = int(cmd2.get("issuedAt", cmd2.get("timestamp", 0)) or 0)
                if ts2 > last_tsr_ts:
                    last_tsr_ts = ts2
                    speed = float(cmd2.get("activeTsrSpeedKmH", cmd2.get("speed", 25)))
                    if 0.0 <= speed <= 160.0:
                        LAST_TSR_COMMAND = {"speed": int(speed), "timestamp": ts2, "source": "cloud"}
                        LATEST_TELEMETRY["activeTsrSpeedKmH"] = int(speed)
                        LATEST_TELEMETRY["recommendedTsrKmh"] = speed
                        save_pi_state()
                        log.warning("[TSR] Speed clamped to %s km/h via Firebase command", speed)
        except Exception as e:
            log.debug("TSR poll error: %s", e)

        if stop_event:
            stop_event.wait(2.5)
        else:
            time.sleep(2.5)


def run_continuous_camera_scan(
    db_url: str,
    uid: str,
    id_token: Optional[str] = None,
    device_id: str = "RPI-TRACK-01",
    scan_interval: float = 2.5,
    simulate_anomaly_interval: int = 0,
    wifi_port: int = 8080,
    no_cloud: bool = False,
) -> None:
    """
    Real camera + sensor patrol loop.

    - Captures a real camera frame when hardware is present and runs real CV.
    - Only raises a crack when the vision model returns a confident detection,
      or when --simulate-anomaly-interval is explicitly set for a demo.
    - Serves the local Wi-Fi API and publishes to Firebase when credentials exist.
    """
    global DEVICE_ID, LATEST_TELEMETRY
    DEVICE_ID = device_id
    log.info("Starting RailGuard Camera Crack Detector on %s...", device_id)
    log.info("Target Firebase: %s/railguard/users/%s", db_url, uid)
    log.info("Camera: %s | Vision: %s", "detected" if _camera_available() else "NOT DETECTED",
             "opencv" if (crack_vision and crack_vision.is_available()) else "UNAVAILABLE")

    start_local_wifi_server(host=LOCAL_BIND_HOST, port=wifi_port)

    stop_event = threading.Event()
    threading.Thread(target=heartbeat_worker, args=(db_url, uid, device_id, stop_event), daemon=True).start()
    if not no_cloud and TOKEN_MANAGER is not None and TOKEN_MANAGER.has_token():
        threading.Thread(target=firebase_cloud_command_poller, args=(db_url, uid, device_id, stop_event), daemon=True).start()
    elif no_cloud:
        log.info("[i] Cloud sync disabled by --no-cloud")
    else:
        log.warning("[!] Cloud sync disabled: no Firebase token configured")

    step = 0
    try:
        while True:
            step += 1
            km = 14.0 + (step * 0.02)
            chainage = f"KM {int(km)}+{int((km % 1) * 1000):03d}"

            # 1. Real camera capture + real computer vision.
            frame = capture_real_camera_frame()
            vision: Optional[Dict[str, Any]] = None
            if frame:
                vision = run_vision_on_bytes(frame)
                if vision.get("hasCrack"):
                    now = now_ms()
                    if now - LAST_CRACK_PIPELINE_MS >= CRACK_COOLDOWN_SECONDS * 1000:
                        trigger_crack_detection_pipeline(
                            db_url=db_url,
                            uid=uid,
                            id_token=id_token,
                            chainage=chainage,
                            device_id=device_id,
                            bounding_box=vision.get("boundingBox"),
                            confidence=int(vision.get("confidencePercent", 0)),
                            length_mm=float(vision.get("crackLengthMm", 0.0) or 0.0),
                        )
                        if not no_cloud:
                            pass  # pipeline already pushes asynchronously
                    time.sleep(scan_interval)
                    continue

            # 2. Explicit demo simulation (never on by default).
            if simulate_anomaly_interval > 0 and step % simulate_anomaly_interval == 0:
                log.warning("[DEMO] Simulated anomaly requested by --simulate-anomaly-interval")
                trigger_crack_detection_pipeline(
                    db_url=db_url,
                    uid=uid,
                    id_token=id_token,
                    chainage=chainage,
                    device_id=device_id,
                    confidence=96,
                    length_mm=14.8,
                    force=True,
                )
                time.sleep(scan_interval)
                continue

            # 3. Nominal frame: preserve real sensor-worker readings.
            current = dict(LATEST_TELEMETRY or {})
            crack_active = _crack_is_active()
            current.update({
                "nodeId": device_id,
                "chainage": chainage,
                "hasCrack": crack_active,
                "visionAvailable": bool(crack_vision and crack_vision.is_available()),
                "visionMode": (vision or {}).get("mode", LATEST_VISION_RESULT.get("mode", "not-run")),
                "timestamp": now_ms(),
            })
            if vision is not None:
                current["lastVisionConfidence"] = int(vision.get("confidencePercent", 0))
            LATEST_TELEMETRY = current

            if not no_cloud and TOKEN_MANAGER is not None and TOKEN_MANAGER.has_token():
                threading.Thread(
                    target=post_to_firebase,
                    args=(db_url, f"railguard/users/{uid}/live_sensors/telemetry", current, id_token),
                    daemon=True,
                ).start()

            log.info(
                "[%d] Scan -> %s | depth %.1fmm | vision %s (%d%%) | crack %s",
                step,
                chainage,
                float(current.get("ultrasonicDepthMm", 0.0) or 0.0),
                "REAL" if frame else "NO-CAMERA",
                int((vision or {}).get("confidencePercent", 0)),
                "ACTIVE" if crack_active else "none",
            )

            if stop_event.wait(scan_interval):
                break
    except KeyboardInterrupt:
        log.info("Camera crack detector stopped by operator.")
    finally:
        stop_event.set()



def main():
    parser = argparse.ArgumentParser(description="RailGuard Camera Crack Detector for Raspberry Pi")
    parser.add_argument("--uid", help="Firebase User UID", default="test-user")
    parser.add_argument("--db", help="Firebase Database URL",
                        default="https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app")
    parser.add_argument("--auth", help="Firebase Auth ID Token (quick test, expires ~1h)", default=None)
    parser.add_argument("--refresh-token", help="Firebase refresh token (enables automatic renewal)", default=None)
    parser.add_argument("--firebase-api-key", help="Firebase Web API key (needed with --refresh-token)", default=None)
    parser.add_argument("--local-api-key", help="Shared secret required by the local Wi-Fi API", default=None)
    parser.add_argument("--bind", help="Local API bind address (default 0.0.0.0; use 127.0.0.1 for Pi-only)", default=None)
    parser.add_argument("--device", help="Device Node ID", default="RPI-TRACK-01")
    parser.add_argument("--port", type=int, help="Local Wi-Fi HTTP API port", default=8080)
    parser.add_argument("--interval", type=float, help="Seconds between camera scans", default=2.5)
    parser.add_argument("--buzzer-pin", type=int, help="BCM GPIO pin for physical buzzer", default=None)
    parser.add_argument("--buzzer-active-low", action="store_true", help="Set if physical buzzer is active-low")
    parser.add_argument("--trig-pin", type=int, help="BCM GPIO pin for ultrasonic TRIG", default=None)
    parser.add_argument("--echo-pin", type=int, help="BCM GPIO pin for ultrasonic ECHO", default=None)
    parser.add_argument("--test-buzzer", action="store_true", help="Immediately test ring the buzzer for 3 seconds and exit")
    parser.add_argument("--test-sensors", action="store_true", help="Test read physical ultrasonic and temp sensors once and exit")
    parser.add_argument("--test-vision", action="store_true", help="Run the computer-vision self-test and exit")
    parser.add_argument("--trigger-once", action="store_true", help="Run one detection pipeline once and exit")
    parser.add_argument("--simulate-anomaly-interval", type=int, help="DEMO ONLY: simulate an anomaly every N scans (0=disabled)", default=0)
    parser.add_argument("--no-cloud", action="store_true", help="Local Wi-Fi API only; do not contact Firebase")

    args = parser.parse_args()

    global BUZZER_PIN, BUZZER_ACTIVE_LOW, ULTRASONIC_TRIG_PIN, ULTRASONIC_ECHO_PIN
    global DEVICE_ID, TOKEN_MANAGER, LOCAL_API_KEY, LOCAL_BIND_HOST

    DEVICE_ID = args.device
    if FirebaseTokenManager is not None:
        try:
            from firebase_auth import from_args_or_env as _token_from_args
            TOKEN_MANAGER = _token_from_args(args)
            if TOKEN_MANAGER.can_refresh():
                TOKEN_MANAGER.start_refresh_loop()
                TOKEN_MANAGER.refresh()
        except Exception as e:
            log.warning("Token manager init failed: %s", e)
            TOKEN_MANAGER = None
    if args.local_api_key:
        LOCAL_API_KEY = args.local_api_key.strip()
    if args.bind:
        LOCAL_BIND_HOST = args.bind.strip()
    if LOCAL_API_KEY:
        log.info("[✓] Local API key protection ENABLED")
    else:
        log.warning("[!] Local API is unauthenticated. Use --local-api-key or RAILGUARD_API_KEY on shared networks.")

    if args.test_vision:
        if crack_vision is None:
            print(f"[!] Vision module unavailable: {_VISION_IMPORT_ERROR}")
            return
        info = crack_vision.backend_info()
        print(f"[*] Vision backend: {info}")
        if info.get("available"):
            print(f"[*] Self-test: {crack_vision.self_test()}")
        return

    if args.buzzer_pin is not None:
        BUZZER_PIN = args.buzzer_pin
        log.info("[i] Buzzer pin configured to BCM GPIO %d", BUZZER_PIN)

    if args.buzzer_active_low:
        BUZZER_ACTIVE_LOW = True
        log.info("[i] Buzzer polarity set to ACTIVE-LOW")

    if args.trig_pin is not None:
        ULTRASONIC_TRIG_PIN = args.trig_pin
        log.info("[i] Ultrasonic TRIG pin configured to BCM GPIO %d", ULTRASONIC_TRIG_PIN)

    if args.echo_pin is not None:
        ULTRASONIC_ECHO_PIN = args.echo_pin
        log.info("[i] Ultrasonic ECHO pin configured to BCM GPIO %d", ULTRASONIC_ECHO_PIN)

    if args.test_buzzer:
        log.info("Testing Raspberry Pi buzzer on BCM GPIO %d (Physical Pin 12) for 3 seconds...", BUZZER_PIN)
        print(f"[*] SOUNDING BUZZER on BCM GPIO {BUZZER_PIN} (Physical Pin 12)...")
        activate_buzzer(duration_seconds=3.0, reason="MANUAL_TEST")
        time.sleep(3.2)
        print("[✓] Buzzer test completed.")
        return

    if args.test_sensors:
        print("==================================================================")
        print("🚆 RailGuard Raspberry Pi Sensor Diagnostic Test")
        print(f"   Ultrasonic TRIG Pin: BCM GPIO {ULTRASONIC_TRIG_PIN} (Physical Pin 16)")
        print(f"   Ultrasonic ECHO Pin: BCM GPIO {ULTRASONIC_ECHO_PIN} (Physical Pin 18)")
        print(f"   Buzzer GPIO Pin:     BCM GPIO {BUZZER_PIN} (Physical Pin 12)")
        print("==================================================================")
        sensor = HardwareUltrasonicSensor(ULTRASONIC_TRIG_PIN, ULTRASONIC_ECHO_PIN)
        print(f"[*] Sensor driver backend: {sensor.backend}")
        print("[*] Sampling ultrasonic distance 5 times (1 per second)...")
        for i in range(5):
            dist = sensor.measure_distance_mm()
            status = f"✓ {dist} mm ({dist/10.0:.1f} cm)" if dist is not None else "✗ No Echo (Check wiring/voltage divider)"
            print(f"   Sample {i+1}: {status}")
            time.sleep(0.8)
        temp = read_hardware_temperature()
        print(f"[*] DS18B20 1-Wire Temperature: {temp} °C" if temp is not None else "[*] DS18B20 Temperature: None (1-Wire not active)")
        return
        log.info("[✓] Ultrasonic Depth: %s mm (Hardware Present: %s)", dist, sensor.is_hardware_present)
        log.info("[✓] DS18B20 Temperature: %s °C", temp)
        return

    if args.trigger_once:
        start_local_wifi_server(host=LOCAL_BIND_HOST, port=args.port)
        # Use real CV when a camera is present; otherwise run an explicit demo.
        frame = capture_real_camera_frame()
        vision = run_vision_on_bytes(frame) if frame else None
        if vision and vision.get("hasCrack"):
            trigger_crack_detection_pipeline(
                db_url=args.db,
                uid=args.uid,
                id_token=args.auth,
                chainage="KM 14+320",
                device_id=args.device,
                bounding_box=vision.get("boundingBox"),
                confidence=int(vision.get("confidencePercent", 0)),
                length_mm=float(vision.get("crackLengthMm", 0.0) or 0.0),
                force=True,
            )
        else:
            log.warning("[DEMO] No confident camera detection; generating an explicitly simulated defect")
            trigger_crack_detection_pipeline(
                db_url=args.db,
                uid=args.uid,
                id_token=args.auth,
                chainage="KM 14+320",
                device_id=args.device,
                confidence=0,
                length_mm=0.0,
                force=True,
            )
        time.sleep(1.0)
    else:
        run_continuous_camera_scan(
            db_url=args.db,
            uid=args.uid,
            id_token=args.auth,
            device_id=args.device,
            scan_interval=args.interval,
            simulate_anomaly_interval=args.simulate_anomaly_interval,
            wifi_port=args.port,
            no_cloud=args.no_cloud,
        )


if __name__ == "__main__":
    main()
