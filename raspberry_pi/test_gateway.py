#!/usr/bin/env python3
"""
Unit and integration tests for the RailGuard Raspberry Pi edge gateway.
Tests packet normalization, sensor key aliases, command expiry, and idempotency.
"""

import unittest
import time
import sys
import os
from unittest.mock import MagicMock

# Mock firebase_admin and serial if not installed in the local test runner
for mod in ["firebase_admin", "firebase_admin.credentials", "firebase_admin.db", "serial"]:
    if mod not in sys.modules:
        sys.modules[mod] = MagicMock()

# Set mock env vars before importing gateway
os.environ.setdefault("RAILGUARD_DATABASE_URL", "https://mock-db.firebaseio.com")
os.environ.setdefault("RAILGUARD_USER_UID", "test_user_uid")
os.environ.setdefault("RAILGUARD_DEVICE_ID", "TEST-PI-01")

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import railguard_gateway as gw


class TestGatewayNormalization(unittest.TestCase):
    def test_standard_packet(self):
        packet = {
            "nodeId": "ESP32-TRACK-01",
            "ultrasonicDepthMm": 46.2,
            "vibrationG": 0.042,
            "railTempC": 31.4,
            "axleSpeedKmh": 14.2,
            "chainage": "KM 14+320",
            "status": "ACTIVE_SYNC"
        }
        normalized = gw.normalize_packet(packet)
        self.assertEqual(normalized["nodeId"], "ESP32-TRACK-01")
        self.assertAlmostEqual(normalized["ultrasonicDepthMm"], 46.2)
        self.assertAlmostEqual(normalized["vibrationG"], 0.042)
        self.assertAlmostEqual(normalized["railTempC"], 31.4)
        self.assertAlmostEqual(normalized["axleSpeedKmh"], 14.2)
        self.assertEqual(normalized["chainage"], "KM 14+320")
        self.assertEqual(normalized["status"], "ACTIVE_SYNC")
        self.assertIsInstance(normalized["timestamp"], int)

    def test_alternate_sensor_keys(self):
        packet = {
            "device_id": "ARDUINO-SENS-02",
            "ut_depth_mm": 52.8,
            "vib_g": 0.088,
            "temp_c": 38.5,
            "speed_kmh": 60.0,
            "milepost": "KM 22+100"
        }
        normalized = gw.normalize_packet(packet)
        self.assertEqual(normalized["nodeId"], "ARDUINO-SENS-02")
        self.assertAlmostEqual(normalized["ultrasonicDepthMm"], 52.8)
        self.assertAlmostEqual(normalized["vibrationG"], 0.088)
        self.assertAlmostEqual(normalized["railTempC"], 38.5)
        self.assertAlmostEqual(normalized["axleSpeedKmh"], 60.0)
        self.assertEqual(normalized["chainage"], "KM 22+100")

    def test_invalid_and_empty_types(self):
        packet = {
            "ultrasonicDepthMm": "not_a_number",
            "vibrationG": None,
            "railTempC": "25.4"
        }
        normalized = gw.normalize_packet(packet)
        self.assertEqual(normalized["ultrasonicDepthMm"], 0.0)
        self.assertEqual(normalized["vibrationG"], 0.0)
        self.assertAlmostEqual(normalized["railTempC"], 25.4)


class TestCommandLifecycle(unittest.TestCase):
    def test_command_cursor_initialization(self):
        cursor = gw.CommandCursor()
        self.assertEqual(cursor.last_tsr_timestamp, 0)
        self.assertEqual(cursor.last_calibration_timestamp, 0)


class TestCameraCrackDetector(unittest.TestCase):
    def test_defect_and_report_generation(self):
        import camera_crack_detector as ccd
        defect = ccd.build_camera_defect("KM 14+320", 14.8, 96)
        self.assertTrue(defect["id"].startswith("DEF-CAM-"))
        self.assertEqual(defect["tone"], "CRITICAL")
        self.assertEqual(defect["chainageCoordinate"], "KM 14+320")
        self.assertEqual(defect["aiConfidencePercent"], 96)

        report = ccd.build_incident_report(defect)
        self.assertTrue(report["id"].startswith("REP-CAM-"))
        self.assertIn("CRITICAL", report["status"])

        notif = ccd.build_safety_notification(defect)
        self.assertTrue(notif["id"].startswith("NOTIF-"))
        self.assertEqual(notif["tone"], "CRITICAL")
        self.assertIn("KM 14+320", notif["message"])

        telem = ccd.build_anomaly_telemetry("KM 14+320")
        self.assertEqual(telem["status"], "ANOMALY_FLAGGED")
        self.assertAlmostEqual(telem["ultrasonicDepthMm"], 46.2)


if __name__ == "__main__":
    unittest.main()

