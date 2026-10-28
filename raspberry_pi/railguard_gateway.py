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
import time
from dataclasses import dataclass
from typing import Any, Dict, Optional

import firebase_admin
from firebase_admin import credentials, db
import serial


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
    """Send the TSR to the train controller, CAN bus, or modem."""
    log.warning(
        "TSR command received for hardware adapter: %s km/h (%s)",
        command.get("activeTsrSpeedKmH"),
        command.get("reason", "no reason"),
    )


def apply_calibration(command: Dict[str, Any]) -> None:
    """Trigger the sensor zero/calibration procedure on the installed node."""
    log.warning("Calibration command received: %s", command.get("command"))


@dataclass
class CommandCursor:
    last_tsr_timestamp: int = 0
    last_calibration_timestamp: int = 0


def poll_commands(cursor: CommandCursor) -> None:
    now_ms = int(time.time() * 1000)
    MAX_COMMAND_AGE_MS = 600_000  # 10 minutes expiry window

    train_path = f"railguard/users/{USER_UID}/trains/{DEVICE_ID}/tsr"
    calibration_path = f"railguard/users/{USER_UID}/esp_sensors/{DEVICE_ID}/command"

    tsr = db.reference(train_path).get() or {}
    tsr_timestamp = int(tsr.get("issuedAt", 0) or 0)
    tsr_status = tsr.get("gatewayStatus", "")
    if tsr_timestamp > cursor.last_tsr_timestamp and tsr_status != "EXECUTED":
        if now_ms - tsr_timestamp > MAX_COMMAND_AGE_MS:
            log.warning("TSR command expired (issued %d ms ago)", now_ms - tsr_timestamp)
            acknowledge(train_path, tsr, "EXPIRED")
        else:
            apply_tsr(tsr)
            acknowledge(train_path, tsr, "EXECUTED")
        cursor.last_tsr_timestamp = tsr_timestamp

    calibration = db.reference(calibration_path).get() or {}
    calibration_timestamp = int(calibration.get("issuedAt", 0) or 0)
    cal_status = calibration.get("gatewayStatus", "")
    if calibration_timestamp > cursor.last_calibration_timestamp and cal_status != "EXECUTED":
        if now_ms - calibration_timestamp > MAX_COMMAND_AGE_MS:
            log.warning("Calibration command expired (issued %d ms ago)", now_ms - calibration_timestamp)
            acknowledge(calibration_path, calibration, "EXPIRED")
        else:
            apply_calibration(calibration)
            acknowledge(calibration_path, calibration, "EXECUTED")
        cursor.last_calibration_timestamp = calibration_timestamp


def run() -> None:
    initialize_firebase()
    cursor = CommandCursor()
    log.info("RailGuard gateway online: device=%s user=%s", DEVICE_ID, USER_UID)

    with serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1) as sensor_serial:
        while True:
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

            try:
                poll_commands(cursor)
            except Exception:
                log.exception("Failed to poll Firebase commands")
            time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    run()