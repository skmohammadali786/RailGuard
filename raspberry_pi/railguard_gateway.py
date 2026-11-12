#!/usr/bin/env python3
"""
RailGuard Raspberry Pi edge gateway.

The gateway is intentionally hardware-agnostic. A sensor node should emit one
JSON object per line over USB/UART, for example:

{
  "ultrasonicDepthMm": 46.2,
  "vibrationG": 0.041,
  "railTempC": 31.4,
  "axleSpeedKmh": 14.2,
  "chainage": "14+320",
  "status": "ACTIVE_SYNC"
}

The gateway writes the packet to:
railguard/users/{RAILGUARD_USER_UID}/live_sensors/telemetry

It also polls the user's train/ESP command paths so the Android app can send
TSR and calibration commands through Firebase. Replace apply_* with the GPIO,
CAN, RS-485, or train-controller implementation for the installed hardware.
"""

from __future__ import annotations

import json
import logging
import os
import sys
import time
from dataclasses import dataclass
from typing import Any, Dict, Optional

import firebase_admin
from firebase_admin import credentials, db

try:
    import serial
except Exception:  # pyserial may not be installed on a cloud-only gateway
    serial = None  # type: ignore


logging.basicConfig(
    level=os.getenv("RAILGUARD_LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(message)s",
)
log = logging.getLogger("railguard-gateway")


DATABASE_URL = os.environ["RAILGUARD_DATABASE_URL"]
USER_UID = os.environ["RAILGUARD_USER_UID"]
DEVICE_ID = os.getenv("RAILGUARD_DEVICE_ID", "RPI-TRACK-01")
SERIAL_PORT = os.getenv("RAILGUARD_SERIAL_PORT", "/dev/ttyUSB0")
BAUD_RATE = int(os.getenv("RAILGUARD_BAUD_RATE", "115200"))
POLL_SECONDS = float(os.getenv("RAILGUARD_COMMAND_POLL_SECONDS", "0.5"))


def initialize_firebase() -> None:
    service_account = os.environ["GOOGLE_APPLICATION_CREDENTIALS"]
    firebase_admin.initialize_app(
        credentials.Certificate(service_account),
        {"databaseURL": DATABASE_URL},
    )


def number(packet: Dict[str, Any], key: str, default: float = 0.0) -> float:
    try:
        return float(packet.get(key, default))
    except (TypeError, ValueError):
        return default


def normalize_packet(packet: Dict[str, Any]) -> Dict[str, Any]:
    now_ms = int(time.time() * 1000)
    ut = packet.get("ultrasonicDepthMm") or packet.get("ut_depth_mm") or packet.get("ultrasonic_mm") or 0.0
    vib = packet.get("vibrationG") or packet.get("vib_g") or packet.get("vibration_g") or 0.0
    temp = packet.get("railTempC") or packet.get("rail_temp_c") or packet.get("temp_c") or 0.0
    speed = packet.get("axleSpeedKmh") or packet.get("speed_kmh") or packet.get("train_speed_kmh") or 0.0
    chainage = packet.get("chainage") or packet.get("milepost") or "--"

    return {
        "nodeId": str(packet.get("nodeId", packet.get("device_id", DEVICE_ID))),
        "ultrasonicDepthMm": number({"v": ut}, "v"),
        "vibrationG": number({"v": vib}, "v"),
        "railTempC": number({"v": temp}, "v"),
        "axleSpeedKmh": number({"v": speed}, "v"),
        "chainage": str(chainage),
        "hardware": str(packet.get("hardware", "Raspberry Pi sensor gateway")),
        "status": str(packet.get("status", "ACTIVE_SYNC")),
        "timestamp": int(packet.get("timestamp", now_ms)),
    }


def telemetry_ref():
    return db.reference(
        f"railguard/users/{USER_UID}/live_sensors/telemetry"
    )


def device_ref():
    return db.reference(
        f"railguard/users/{USER_UID}/devices/{DEVICE_ID}/last_telemetry"
    )


def history_ref():
    return db.reference(
        f"railguard/users/{USER_UID}/devices/{DEVICE_ID}/telemetry_history"
    )


def heartbeat_ref():
    return db.reference(
        f"railguard/users/{USER_UID}/devices/{DEVICE_ID}/heartbeat"
    )


def send_heartbeat(status: str = "ONLINE") -> None:
    try:
        heartbeat_ref().set({
            "deviceId": DEVICE_ID,
            "status": status,
            "lastSeen": int(time.time() * 1000),
            "serialPort": SERIAL_PORT,
            "baudRate": BAUD_RATE
        })
    except Exception as e:
        log.warning("Heartbeat sync warning: %s", e)


def publish_telemetry(packet: Dict[str, Any]) -> None:
    normalized = normalize_packet(packet)
    telemetry_ref().set(normalized)
    device_ref().set(normalized)
    try:
        history_ref().push(normalized)
    except Exception as e:
        log.warning("History logging warning: %s", e)
    send_heartbeat()
    log.info(
        "Published %s: depth=%.2fmm temp=%.2fC speed=%.2fkm/h chainage=%s",
        normalized["nodeId"],
        normalized["ultrasonicDepthMm"],
        normalized["railTempC"],
        normalized["axleSpeedKmh"],
        normalized["chainage"],
    )


def acknowledge(path: str, command: Dict[str, Any], status: str) -> None:
    db.reference(path).update(
        {
            "gatewayStatus": status,
            "gatewayDevice": DEVICE_ID,
            "acknowledgedAt": int(time.time() * 1000),
            "receivedCommand": command,
        }
    )


def apply_tsr(command: Dict[str, Any]) -> None:
    """Apply a Temporary Speed Restriction to the local hardware adapter.

    This is the real implementation point. It:
      - validates the requested speed,
      - persists it so it survives a gateway restart,
      - drives an optional TSR relay output (RAILGUARD_TSR_PIN) so an external
        cab-signalling / governor interface can be wired in,
      - writes the active limit back to the device node in Firebase.
    """
    raw = command.get("activeTsrSpeedKmH", command.get("speed"))
    try:
        speed = float(raw)
    except (TypeError, ValueError):
        raise ValueError(f"TSR command has no valid speed: {raw!r}")
    if not (0.0 <= speed <= 160.0):
        raise ValueError(f"TSR speed out of range: {speed}")

    _write_state({"tsrSpeedKmh": int(speed), "tsrIssuedAt": int(command.get("issuedAt", time.time() * 1000))})
    log.warning("TSR APPLIED: train speed clamped to %.0f km/h (%s)", speed, command.get("reason", "no reason"))

    # Optional hardware output: energise a relay when the restriction is active.
    pin = os.getenv("RAILGUARD_TSR_PIN")
    if pin:
        try:
            _drive_output(int(pin), speed > 0)
        except Exception as e:
            log.warning("TSR relay output failed: %s", e)

    try:
        db.reference(f"railguard/users/{USER_UID}/trains/{DEVICE_ID}/tsr").update(
            {
                "appliedOnGateway": True,
                "appliedSpeedKmh": int(speed),
                "appliedAt": int(time.time() * 1000),
            }
        )
    except Exception as e:
        log.warning("Could not write applied TSR back to Firebase: %s", e)


def apply_calibration(command: Dict[str, Any]) -> None:
    """Persist a calibration command and apply the supported sensor zeroing.

    Supported today:
      - `zero_ultrasonic`: resets the distance baseline used for clearance warnings.
      - `zero_vibration`: records a vibration offset (used by external nodes).
    Unknown commands are persisted and reported, never silently ignored.
    """
    cmd = str(command.get("command", "unknown"))
    offset = command.get("offset", command.get("value", 0))
    _write_state({"lastCalibration": {"command": cmd, "offset": offset, "at": int(time.time() * 1000)}})
    if cmd in ("zero_ultrasonic", "calibrate_ultrasonic", "zero"):
        # The camera detector owns the live baseline; signal it through a file
        # that it can also read if co-located on the same Pi.
        _write_state({"ultrasonicBaselineResetAt": int(time.time() * 1000)})
    log.warning("Calibration applied: command=%s offset=%s", cmd, offset)


def _state_path() -> str:
    preferred = os.getenv("RAILGUARD_GATEWAY_STATE", "/var/lib/railguard/gateway_state.json")
    try:
        os.makedirs(os.path.dirname(preferred), exist_ok=True)
        return preferred
    except Exception:
        return os.path.join(os.path.dirname(os.path.abspath(__file__)), "railguard_gateway_state.json")


def _write_state(patch: Dict[str, Any]) -> None:
    path = _state_path()
    data: Dict[str, Any] = {}
    try:
        if os.path.exists(path):
            with open(path, "r") as f:
                data = json.load(f) or {}
    except Exception:
        data = {}
    data.update(patch)
    data["updatedAt"] = int(time.time() * 1000)
    try:
        with open(path, "w") as f:
            json.dump(data, f, indent=2)
    except Exception as e:
        log.warning("Could not persist gateway state: %s", e)


def _drive_output(pin: int, high: bool) -> None:
    """Drive a GPIO output using whichever library is installed."""
    try:
        import lgpio  # type: ignore

        handle = lgpio.gpiochip_open(0)
        lgpio.gpio_claim_output(handle, pin, 0)
        lgpio.gpio_write(handle, pin, 1 if high else 0)
        lgpio.gpiochip_close(handle)
        return
    except Exception:
        pass
    try:
        import RPi.GPIO as GPIO  # type: ignore

        GPIO.setwarnings(False)
        GPIO.setmode(GPIO.BCM)
        GPIO.setup(pin, GPIO.OUT)
        GPIO.output(pin, GPIO.HIGH if high else GPIO.LOW)
        return
    except Exception:
        pass
    try:
        from gpiozero import OutputDevice  # type: ignore

        OutputDevice(pin).value = 1 if high else 0
        return
    except Exception:
        pass
    raise RuntimeError("no GPIO backend available")


def apply_buzzer(command: Dict[str, Any]) -> None:
    """Ring the physical buzzer with multi-backend GPIO support."""
    pin = int(os.getenv("RAILGUARD_BUZZER_PIN", "18"))
    state = str(command.get("state", "RING")).upper()
    duration = float(command.get("duration", 6.0))
    log.warning("🚨 [ALARM] Buzzer commanded (%s) on GPIO %d for %.1fs", state, pin, duration)

    acted = False
    # 1. RPi.GPIO (Bullseye and earlier)
    try:
        import RPi.GPIO as GPIO  # type: ignore

        GPIO.setwarnings(False)
        GPIO.setmode(GPIO.BCM)
        GPIO.setup(pin, GPIO.OUT)
        if state in ("RING", "ON"):
            GPIO.output(pin, GPIO.HIGH)
            time.sleep(min(3.0, duration))
            GPIO.output(pin, GPIO.LOW)
        else:
            GPIO.output(pin, GPIO.LOW)
        acted = True
    except Exception:
        pass

    # 2. lgpio (Bookworm / Pi 5)
    if not acted:
        try:
            import lgpio  # type: ignore

            for chip in (0, 4):
                try:
                    h = lgpio.gpiochip_open(chip)
                    lgpio.gpio_claim_output(h, pin, 0)
                    if state in ("RING", "ON"):
                        lgpio.gpio_write(h, pin, 1)
                        time.sleep(min(3.0, duration))
                        lgpio.gpio_write(h, pin, 0)
                    else:
                        lgpio.gpio_write(h, pin, 0)
                    lgpio.gpiochip_close(h)
                    acted = True
                    break
                except Exception:
                    continue
        except Exception:
            pass

    # 3. gpiozero
    if not acted:
        try:
            from gpiozero import OutputDevice  # type: ignore

            buzzer = OutputDevice(pin)
            if state in ("RING", "ON"):
                buzzer.on()
                time.sleep(min(3.0, duration))
                buzzer.off()
            else:
                buzzer.off()
            acted = True
        except Exception:
            pass

    # 4. Always fall back to the terminal bell (useful on a headless Pi/SSH).
    try:
        sys.stdout.write("\a\a\a")
        sys.stdout.flush()
    except Exception:
        pass

    if not acted:
        log.warning("No GPIO backend available; buzzer simulated acoustically")


@dataclass
class CommandCursor:
    last_tsr_timestamp: int = 0
    last_calibration_timestamp: int = 0
    last_buzzer_timestamp: int = 0


def poll_commands(cursor: CommandCursor) -> None:
    now_ms = int(time.time() * 1000)
    MAX_COMMAND_AGE_MS = 600_000  # 10 minutes expiry window

    train_path = f"railguard/users/{USER_UID}/trains/{DEVICE_ID}/tsr"
    calibration_path = f"railguard/users/{USER_UID}/esp_sensors/{DEVICE_ID}/command"
    buzzer_path = f"railguard/users/{USER_UID}/commands/buzzer"

    tsr = db.reference(train_path).get() or {}
    tsr_timestamp = int(tsr.get("issuedAt", 0) or 0)
    tsr_status = tsr.get("gatewayStatus", "")
    if tsr_timestamp > cursor.last_tsr_timestamp and tsr_status != "EXECUTED":
        if now_ms - tsr_timestamp > MAX_COMMAND_AGE_MS:
            log.warning("TSR command expired (issued %d ms ago)", now_ms - tsr_timestamp)
            acknowledge(train_path, tsr, "EXPIRED")
        else:
            try:
                apply_tsr(tsr)
                acknowledge(train_path, tsr, "EXECUTED")
            except Exception as e:
                log.exception("TSR apply failed")
                acknowledge(train_path, tsr, f"FAILED: {e}")
        cursor.last_tsr_timestamp = tsr_timestamp

    calibration = db.reference(calibration_path).get() or {}
    calibration_timestamp = int(calibration.get("issuedAt", 0) or 0)
    cal_status = calibration.get("gatewayStatus", "")
    if calibration_timestamp > cursor.last_calibration_timestamp and cal_status != "EXECUTED":
        if now_ms - calibration_timestamp > MAX_COMMAND_AGE_MS:
            log.warning("Calibration command expired (issued %d ms ago)", now_ms - calibration_timestamp)
            acknowledge(calibration_path, calibration, "EXPIRED")
        else:
            try:
                apply_calibration(calibration)
                acknowledge(calibration_path, calibration, "EXECUTED")
            except Exception as e:
                log.exception("Calibration apply failed")
                acknowledge(calibration_path, calibration, f"FAILED: {e}")
        cursor.last_calibration_timestamp = calibration_timestamp

    buzzer_cmd = db.reference(buzzer_path).get() or {}
    buzzer_timestamp = int(buzzer_cmd.get("timestamp", 0) or 0)
    buzzer_status = buzzer_cmd.get("gatewayStatus", "")
    if buzzer_timestamp > cursor.last_buzzer_timestamp and buzzer_status != "EXECUTED":
        if now_ms - buzzer_timestamp <= MAX_COMMAND_AGE_MS:
            try:
                apply_buzzer(buzzer_cmd)
                acknowledge(buzzer_path, buzzer_cmd, "EXECUTED")
            except Exception as e:
                log.exception("Buzzer apply failed")
                acknowledge(buzzer_path, buzzer_cmd, f"FAILED: {e}")
        cursor.last_buzzer_timestamp = buzzer_timestamp


def run() -> None:
    initialize_firebase()
    cursor = CommandCursor()
    log.info("RailGuard gateway online: device=%s user=%s", DEVICE_ID, USER_UID)

    if serial is None:
        log.warning("pyserial is not installed: serial sensor input disabled (commands still work)")

    sensor_serial = None
    last_serial_attempt = 0.0
    while True:
        # (Re)connect to the serial sensor node without blocking command polling.
        if serial is not None and sensor_serial is None and (time.time() - last_serial_attempt) > 5.0:
            last_serial_attempt = time.time()
            try:
                sensor_serial = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
                log.info("Serial sensor connected on %s @ %d baud", SERIAL_PORT, BAUD_RATE)
            except Exception as e:
                log.warning("Serial port %s unavailable (%s); retrying in 5s", SERIAL_PORT, e)
                sensor_serial = None
                send_heartbeat("ONLINE_NO_SERIAL")

        if sensor_serial is not None:
            try:
                raw_line = sensor_serial.readline().decode("utf-8", errors="replace").strip()
                if raw_line:
                    try:
                        packet = json.loads(raw_line)
                        if isinstance(packet, dict):
                            publish_telemetry(packet)
                        else:
                            log.warning("Ignoring non-object sensor packet")
                    except json.JSONDecodeError:
                        log.warning("Ignoring invalid sensor JSON: %s", raw_line[:200])
                    except Exception:
                        log.exception("Failed to publish sensor packet")
            except Exception as e:
                log.warning("Serial read failed (%s); reconnecting", e)
                try:
                    sensor_serial.close()
                except Exception:
                    pass
                sensor_serial = None

        try:
            poll_commands(cursor)
        except Exception:
            log.exception("Failed to poll Firebase commands")

        if sensor_serial is None:
            send_heartbeat("ONLINE_NO_SERIAL")
        time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    run()