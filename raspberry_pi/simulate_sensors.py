#!/usr/bin/env python3
"""
Hardware & Sensor Simulator for RailGuard.
Simulates realistic trackside sensor readings:
- Piezoelectric ultrasonic transducer (thickness & subsurface flaw pulses)
- ADXL345 3-axis accelerometer (wheel-rail impact shock and vibration)
- PT100 contact thermistor (CWR railhead surface temperature)
- GNSS GPS module (train/cart speed and chainage progression)

Can publish directly to Firebase Realtime Database REST API or print serial lines.

Usage:
  python3 simulate_sensors.py --uid <USER_UID> --db <DATABASE_URL> [--auth <ID_TOKEN>]
"""

import argparse
import json
import math
import random
import sys
import time
import urllib.request
import urllib.error


def generate_packet(step: int, device_id: str = "RPI-TRACK-01") -> dict:
    km = 14.0 + (step * 0.02)
    chainage = f"KM {int(km)}+{int((km % 1) * 1000):03d}"

    # Nominal railhead thickness ~ 68.2 mm with occasional crack depression
    has_flaw = (step % 20) in (10, 11)
    ultrasonic_depth = 46.2 if has_flaw else round(68.2 + random.uniform(-0.4, 0.4), 1)

    # Vibration: nominal 0.04g, spikes up to 0.45g on rail joints
    vibration = round(random.uniform(0.35, 0.55), 3) if (step % 15 == 0) else round(random.uniform(0.02, 0.06), 3)

    # Rail temperature: diurnal curve around 32°C
    rail_temp = round(31.5 + 4.0 * math.sin(step * 0.1) + random.uniform(-0.2, 0.2), 1)

    # Axle speed: 45 km/h patrol speed
    speed = round(45.0 + random.uniform(-1.5, 1.5), 1)

    return {
        "nodeId": device_id,
        "ultrasonicDepthMm": ultrasonic_depth,
        "vibrationG": vibration,
        "railTempC": rail_temp,
        "axleSpeedKmh": speed,
        "chainage": chainage,
        "hardware": "ESP32-WROOM-32 / Piezo UT / ADXL345 / PT100",
        "status": "ANOMALY_FLAGGED" if has_flaw else "ACTIVE_SYNC",
        "timestamp": int(time.time() * 1000)
    }


def publish_rest(db_url: str, uid: str, packet: dict, id_token: str = None) -> bool:
    clean_db = db_url.rstrip("/")
    auth_query = f"?auth={id_token}" if id_token else ""
    endpoint = f"{clean_db}/railguard/users/{uid}/live_sensors/telemetry.json{auth_query}"
    data = json.dumps(packet).encode("utf-8")

    req = urllib.request.Request(endpoint, data=data, method="PUT")
    req.add_header("Content-Type", "application/json; charset=UTF-8")

    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            return response.status in (200, 204)
    except urllib.error.HTTPError as e:
        print(f"[!] HTTP Error {e.code}: {e.read().decode('utf-8', errors='replace')}")
        return False
    except Exception as e:
        print(f"[!] Network error: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(description="RailGuard Sensor Telemetry Simulator")
    parser.add_argument("--uid", help="Firebase User UID", default="test-user")
    parser.add_argument("--db", help="Firebase Database URL", default="https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app")
    parser.add_argument("--auth", help="Firebase Auth ID Token (optional)", default=None)
    parser.add_argument("--device", help="Device Node ID", default="RPI-TRACK-01")
    parser.add_argument("--interval", type=float, help="Seconds between packets", default=2.0)
    parser.add_argument("--count", type=int, help="Number of packets (0 for infinite)", default=5)
    parser.add_argument("--print-only", action="store_true", help="Print JSON to stdout without pushing to Firebase")

    args = parser.parse_args()

    print(f"[*] Starting RailGuard Sensor Simulator (Device: {args.device})")
    if not args.print_only:
        print(f"[*] Target Endpoint: {args.db}/railguard/users/{args.uid}/live_sensors/telemetry")

    step = 0
    try:
        while True:
            step += 1
            packet = generate_packet(step, args.device)
            json_str = json.dumps(packet)

            if args.print_only:
                print(json_str)
            else:
                ok = publish_rest(args.db, args.uid, packet, args.auth)
                status = "✓ Pushed" if ok else "✗ Failed"
                print(f"[{step}] {status} -> {packet['chainage']} | Depth: {packet['ultrasonicDepthMm']}mm | Vib: {packet['vibrationG']}g | Temp: {packet['railTempC']}°C")

                # If anomaly/crack flagged, auto-log defect, incident report & alert notification
                if packet.get("status") == "ANOMALY_FLAGGED":
                    now_ms = int(time.time() * 1000)
                    now_str = time.strftime("%H:%M · %d %b")
                    defect_id = f"DEF-SIM-{now_ms % 100000}"
                    clean_db = args.db.rstrip("/")
                    auth_q = f"?auth={args.auth}" if args.auth else ""

                    defect_body = {
                        "id": defect_id,
                        "title": "Transverse Rail Fissure (Sensor Flagged)",
                        "section": f"Corridor Sector 4B ({packet['chainage']})",
                        "score": "CRITICAL (96% AI Flaw Match)",
                        "tone": "CRITICAL",
                        "time": now_str,
                        "detail": f"Piezoelectric ultrasonic depth depression ({packet['ultrasonicDepthMm']}mm) and vibration ({packet['vibrationG']}g) flagged railhead flaw at {packet['chainage']}.",
                        "estimatedLength": "14.8 mm",
                        "latitude": 51.5074,
                        "longitude": -0.1278,
                        "riskScore": 94,
                        "chainageCoordinate": packet["chainage"],
                        "aiConfidencePercent": 96,
                        "aiPrescribedAction": "Emergency Slow Order (TSR 25 km/h) & immediate track verification.",
                        "timestamp": now_ms
                    }
                    report_body = {
                        "id": f"REP-SIM-{now_ms % 100000}",
                        "title": "Automated Sensor Crack Detection Incident Report",
                        "section": defect_body["section"],
                        "inspector": f"Sensor Node ({args.device})",
                        "status": "CRITICAL INCIDENT LOGGED - IMMEDIATE ACTION REQUIRED",
                        "generatedAt": now_ms,
                        "defectId": defect_id,
                        "chainage": packet["chainage"]
                    }
                    notif_body = {
                        "id": f"NOTIF-SIM-{now_ms % 100000}",
                        "title": "CRITICAL: Rail Crack Auto-Detected",
                        "message": f"Track anomaly flagged at {packet['chainage']}. Defect logged & incident report generated.",
                        "time": now_str,
                        "tone": "CRITICAL",
                        "read": False,
                        "timestamp": now_ms
                    }

                    # Push defect, report, and notification in parallel
                    for sub_path, payload in [
                        (f"railguard/users/{args.uid}/defects/{defect_id}", defect_body),
                        (f"railguard/users/{args.uid}/reports/{report_body['id']}", report_body),
                        (f"railguard/users/{args.uid}/notifications/{notif_body['id']}", notif_body)
                    ]:
                        try:
                            ep = f"{clean_db}/{sub_path}.json{auth_q}"
                            r = urllib.request.Request(ep, data=json.dumps(payload).encode("utf-8"), method="PUT")
                            r.add_header("Content-Type", "application/json")
                            urllib.request.urlopen(r, timeout=5)
                        except Exception as e:
                            print(f"[!] Auto-log sync warning ({sub_path}): {e}")

                    print(f"     [⚡ AUTO-PIPELINE] Defect {defect_id}, Incident Report & Alert Notification logged to Firebase RTDB!")

            if args.count > 0 and step >= args.count:
                break
            time.sleep(args.interval)
    except KeyboardInterrupt:
        print("\n[*] Simulator stopped by user.")


if __name__ == "__main__":
    main()
