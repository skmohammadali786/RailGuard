#!/usr/bin/env python3
"""
Unit and integration tests for the RailGuard Raspberry Pi edge gateway.
Tests packet normalization, sensor key aliases, command expiry, and idempotency.
"""

import json
import os
import sys
import time
import unittest
import urllib.error
import urllib.request
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
        # The old test asserted a fake 46.2 mm "crack depth". Ultrasonic
        # distance is not a crack measurement, and with no sensor sample yet the
        # packet must NOT invent one.
        self.assertNotIn("ultrasonicDepthMm", telem)
        self.assertNotEqual(telem.get("dataMode"), "LIVE")
        self.assertTrue(telem["hasCrack"])
        self.assertIn("x:", telem["crackBoundingBox"])
        self.assertIn("crackExpiresAt", telem)

        # A real live reading must survive the anomaly packet unchanged.
        previous = ccd.LATEST_TELEMETRY
        try:
            ccd.LATEST_TELEMETRY = {
                "ultrasonicDepthMm": 71.4,
                "dataMode": "PARTIAL",
                "ultrasonicLive": True,
            }
            preserved = ccd.build_anomaly_telemetry("KM 14+320")
            self.assertEqual(preserved["ultrasonicDepthMm"], 71.4)
            self.assertTrue(preserved["ultrasonicLive"])
            self.assertEqual(preserved["dataMode"], "PARTIAL")
        finally:
            ccd.LATEST_TELEMETRY = previous

    def test_local_wifi_server_endpoints(self):
        import camera_crack_detector as ccd
        import urllib.request
        server = ccd.start_local_wifi_server(host="127.0.0.1", port=18080)
        time.sleep(0.1)

        # 1. Health endpoint
        with urllib.request.urlopen("http://127.0.0.1:18080/api/health", timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["status"], "ONLINE")
            # Process identity lets the app detect a stale instance.
            self.assertIn("instance", data)
            self.assertEqual(data["instance"]["version"], ccd.RAILGUARD_VERSION)
            self.assertGreater(data["instance"]["pid"], 0)
            self.assertGreater(data["instance"]["startedAt"], 0)

        # 2. Latest crack endpoint
        with urllib.request.urlopen("http://127.0.0.1:18080/api/latest_crack", timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertIn("status", data)

        # 3. All Data snapshot endpoint
        with urllib.request.urlopen("http://127.0.0.1:18080/api/all_data", timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            # "ONLINE" only means the HTTP server answers. A brand-new process
            # may legitimately answer with NO_TELEMETRY before its first sample.
            self.assertIn(data["status"], ("ONLINE", "STALE", "NO_TELEMETRY"))
            self.assertIn("sensorLoop", data)
            self.assertIn("alive", data["sensorLoop"])
            self.assertIn("telemetryAgeSeconds", data["sensorLoop"])
            self.assertIn("telemetry", data)
            self.assertIn("defects", data)
            self.assertIn("tasks", data)
            self.assertIn("inspections", data)
            self.assertIn("hardware", data)
            self.assertIn("aiOracle", data)
            self.assertIn("instance", data)
            # Every packet must state whether the readings are real or simulated.
            telem = data["telemetry"]
            self.assertIn(telem.get("dataMode"), ("LIVE", "PARTIAL", "SIMULATED", "NO_DATA"))
            self.assertIn("sensorMode", telem)
            self.assertIn("ultrasonic", telem["sensorMode"])
            self.assertIn("ultrasonicLive", telem)
            self.assertIn("dataSource", telem)
            if telem.get("dataMode") == "NO_DATA":
                # Never invent a reading when the loop has not produced one.
                self.assertIsNone(telem.get("ultrasonicDepthMm"))
                self.assertEqual(telem.get("timestamp"), 0)
                self.assertTrue(telem.get("stale"))

        # 4. Hardware diagnostics endpoint
        with urllib.request.urlopen("http://127.0.0.1:18080/api/hardware", timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertIn(data["status"], ("OPERATIONAL", "LIMITED_SIMULATION"))
            # Diagnostics must reflect reality, not hardcoded ONLINE values.
            self.assertIn(data["camera"]["status"], ("ONLINE", "NOT_INSTALLED"))
            self.assertIn(data["ultrasonic"]["status"], ("ONLINE", "NOT_INSTALLED"))
            self.assertIn("vision", data)
            self.assertFalse(data["battery"]["present"])

        # 5. TSR emergency slow order POST
        req = urllib.request.Request(
            "http://127.0.0.1:18080/api/tsr",
            data=json.dumps({"speed": 25}).encode(),
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["status"], "EXECUTED")
            self.assertEqual(data["activeTsrSpeedKmH"], 25)

        # 5b. Regression: plain GET /api/tsr used to raise UnboundLocalError.
        with urllib.request.urlopen("http://127.0.0.1:18080/api/tsr", timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["activeTsrSpeedKmH"], 25)
            self.assertTrue(data["speedRestrictionActive"])

        # 5c. Invalid speed must be rejected, not crash.
        bad = urllib.request.Request(
            "http://127.0.0.1:18080/api/tsr?speed=999",
            headers={"Accept": "application/json"},
        )
        try:
            urllib.request.urlopen(bad, timeout=2)
            self.fail("invalid TSR speed should return HTTP 400")
        except urllib.error.HTTPError as e:
            self.assertEqual(e.code, 400)

        # 5d. Operator can acknowledge/clear a crack latch.
        clear_req = urllib.request.Request(
            "http://127.0.0.1:18080/api/crack/clear",
            data=json.dumps({"reason": "TEST_ACK"}).encode(),
            headers={"Content-Type": "application/json"},
        )
        with urllib.request.urlopen(clear_req, timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["status"], "CLEARED")
            self.assertFalse(data["hasCrack"])

        # 6. Buzzer Ring POST & Status GET
        req_buzzer = urllib.request.Request(
            "http://127.0.0.1:18080/api/buzzer",
            data=json.dumps({"state": "RING", "duration": 0.5, "reason": "TEST_UNIT"}).encode(),
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req_buzzer, timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["status"], "SUCCESS")
            self.assertEqual(data["buzzer"], "RINGING")

        with urllib.request.urlopen("http://127.0.0.1:18080/api/buzzer", timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["status"], "ONLINE")
            self.assertIn("buzzer", data)

        # 7. Buzzer Silence POST
        req_silence = urllib.request.Request(
            "http://127.0.0.1:18080/api/buzzer",
            data=json.dumps({"state": "SILENCE"}).encode(),
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req_silence, timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["status"], "SUCCESS")
            self.assertEqual(data["buzzer"], "SILENCED")

        # 8. Schedule Save POST & GET
        req_sched = urllib.request.Request(
            "http://127.0.0.1:18080/api/schedule",
            data=json.dumps({"id": "SCHED-TEST", "section": "Corridor Sector 4B", "time": "08:00"}).encode(),
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req_sched, timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["status"], "SCHEDULE_SAVED")

        # 9. Patrol Start POST
        req_patrol = urllib.request.Request(
            "http://127.0.0.1:18080/api/patrol/start",
            data=json.dumps({"section": "Sector 4B", "action": "START_PATROL"}).encode(),
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req_patrol, timeout=2) as res:
            self.assertEqual(res.status, 200)
            data = json.loads(res.read().decode())
            self.assertEqual(data["status"], "PATROL_ACTIVE")
            self.assertIn("sensors", data)

        if server:
            server.shutdown()

    def test_local_api_key_enforcement(self):
        import camera_crack_detector as ccd
        server = ccd.start_local_wifi_server(host="127.0.0.1", port=18083)
        time.sleep(0.1)
        old_key = ccd.LOCAL_API_KEY
        ccd.LOCAL_API_KEY = "unit-test-secret"
        try:
            try:
                urllib.request.urlopen("http://127.0.0.1:18083/api/telemetry", timeout=2)
                self.fail("request without API key should be rejected")
            except urllib.error.HTTPError as e:
                self.assertEqual(e.code, 401)

            req = urllib.request.Request(
                "http://127.0.0.1:18083/api/telemetry",
                headers={"X-API-Key": "unit-test-secret"},
            )
            with urllib.request.urlopen(req, timeout=2) as res:
                self.assertEqual(res.status, 200)

            # Health stays open for monitoring.
            with urllib.request.urlopen("http://127.0.0.1:18083/api/health", timeout=2) as res:
                self.assertEqual(res.status, 200)
        finally:
            ccd.LOCAL_API_KEY = old_key
            if server:
                server.shutdown()

    def test_ultrasonic_distance_is_not_a_crack(self):
        import camera_crack_detector as ccd
        # A "weird" distance must never set hasCrack by itself.
        ccd.LATEST_CRACK = None
        ccd._UT_HISTORY[:] = [68.2] * 10
        ccd._UT_DEVIATION_COUNT = 0
        self.assertFalse(ccd._crack_is_active())
        # A crack latch expires automatically.
        ccd.LATEST_CRACK = {"hasCrack": True, "timestamp": ccd.now_ms() - (ccd.CRACK_ACTIVE_SECONDS + 1) * 1000}
        self.assertFalse(ccd._crack_is_active())
        ccd.LATEST_CRACK = None

    def test_crack_pipeline_runs_without_shadowing_error(self):
        """Regression: a local variable named now_ms shadowed the now_ms() helper
        and made the crack pipeline / sensor worker raise UnboundLocalError."""
        import camera_crack_detector as ccd

        old_crack = ccd.LATEST_CRACK
        old_telem = ccd.LATEST_TELEMETRY
        old_pipeline_ms = ccd.LAST_CRACK_PIPELINE_MS
        try:
            ccd.LATEST_CRACK = None
            ccd.LAST_CRACK_PIPELINE_MS = 0
            ccd.trigger_crack_detection_pipeline(
                db_url="https://example.invalid",
                uid="test-user",
                chainage="KM 14+320",
                device_id="TEST-PI-01",
                confidence=88,
                length_mm=12.5,
                force=True,
            )
            self.assertTrue(ccd._crack_is_active())
            self.assertEqual(ccd.LATEST_TELEMETRY.get("flawType"), "Surface Fissure (optical)")
            # The cooldown must suppress a second automatic pipeline.
            result = ccd.trigger_crack_detection_pipeline(
                db_url="https://example.invalid",
                uid="test-user",
                chainage="KM 14+321",
                device_id="TEST-PI-01",
                confidence=88,
                length_mm=12.5,
                force=False,
            )
            self.assertFalse(result.get("success", False))
            self.assertEqual(result.get("reason"), "cooldown")

            # FAIL-SAFE INTERLOCK: the crack must cut the motors and apply TSR 25.
            motor = ccd.motor_status()
            self.assertEqual(motor.get("state"), "CUT")
            self.assertTrue(motor.get("motorStopped"))
            self.assertTrue(motor.get("latched"))
            self.assertEqual((ccd.LAST_TSR_COMMAND or {}).get("speed"), 25)
            self.assertTrue((ccd.LAST_TSR_COMMAND or {}).get("automatic"))

            # A reset is refused while the crack alert is still active...
            refused = ccd.get_motor_controller().reset(reason="unit-test")
            self.assertEqual(refused.get("status"), "REFUSED")
            self.assertEqual(ccd.motor_status().get("state"), "CUT")

            # ...and allowed once the alert is acknowledged.
            ccd.clear_latest_crack("unit-test")
            allowed = ccd.get_motor_controller().reset(reason="unit-test")
            self.assertEqual(allowed.get("status"), "RESET")
            self.assertEqual(ccd.motor_status().get("state"), "ALLOWED")
        finally:
            ccd.LATEST_CRACK = old_crack
            ccd.LATEST_TELEMETRY = old_telem
            ccd.LAST_CRACK_PIPELINE_MS = old_pipeline_ms
            try:
                ccd.get_motor_controller().reset(reason="unit-test-cleanup", force=True)
            except Exception:
                pass


    def test_port_conflict_is_reported_not_swallowed(self):
        """A second instance must never silently serve nothing on a busy port."""
        import socket
        import camera_crack_detector as ccd
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.bind(("127.0.0.1", 0))
        sock.listen(1)
        busy_port = sock.getsockname()[1]
        try:
            ccd.LOCAL_SERVER_BIND_ERROR = None
            result = ccd.start_local_wifi_server(host="127.0.0.1", port=busy_port, strict=False)
            self.assertIsNone(result)
            self.assertIn("in use", (ccd.LOCAL_SERVER_BIND_ERROR or "").lower())
        finally:
            sock.close()

    def test_speed_resolution_never_calls_float_none(self):
        """Regression: a null TSR speed froze the whole telemetry worker."""
        import camera_crack_detector as ccd
        self.assertEqual(
            ccd.resolve_effective_speed(None, {"activeTsrSpeedKmH": None, "axleSpeedKmh": 33.0}), 33.0
        )
        self.assertEqual(ccd.resolve_effective_speed(None, {}), 45.0)
        self.assertEqual(ccd.resolve_effective_speed(25, {"axleSpeedKmh": 80.0}), 25.0)
        self.assertEqual(
            ccd.resolve_effective_speed("bad", {"activeTsrSpeedKmH": "worse", "axleSpeedKmh": 12.5}), 12.5
        )


class TestNoDemoTelemetry(unittest.TestCase):
    """Regressions for the "app shows 68.2 / 0.035 / 31.5 / 45.0 forever" bug."""

    def _restore(self, ccd, old_telem, old_ts):
        ccd.LATEST_TELEMETRY = old_telem
        ccd.TELEMETRY_UPDATED_AT_MS = old_ts

    def test_no_sample_means_no_data_not_demo_numbers(self):
        import camera_crack_detector as ccd
        old_telem, old_ts = ccd.LATEST_TELEMETRY, ccd.TELEMETRY_UPDATED_AT_MS
        try:
            ccd.LATEST_TELEMETRY = {}
            ccd.TELEMETRY_UPDATED_AT_MS = 0
            telem = ccd.current_telemetry()
            self.assertEqual(telem["dataMode"], "NO_DATA")
            self.assertEqual(telem["status"], "NO_TELEMETRY_YET")
            self.assertIsNone(telem["ultrasonicDepthMm"])
            self.assertIsNone(telem["vibrationG"])
            self.assertIsNone(telem["railTempC"])
            self.assertIsNone(telem["axleSpeedKmh"])
            self.assertIsNone(telem["chainage"])
            self.assertEqual(telem["timestamp"], 0)
            self.assertTrue(telem["stale"])
            self.assertEqual(telem["sensorMode"]["ultrasonic"], "unavailable")

            snapshot = ccd.get_all_system_data()
            self.assertEqual(snapshot["status"], "NO_TELEMETRY")
            self.assertFalse(snapshot["aiOracle"]["available"])
            self.assertIsNone(snapshot["aiOracle"]["nadalRatio"])
        finally:
            self._restore(ccd, old_telem, old_ts)

    def test_persisted_telemetry_is_ignored_on_restart(self):
        import tempfile
        import camera_crack_detector as ccd
        old_path, old_telem = ccd.STATE_FILE_PATH, ccd.LATEST_TELEMETRY
        old_schedules = ccd.SCHEDULES_REGISTRY
        try:
            with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as f:
                json.dump({
                    "telemetry": {
                        "version": "1.0.1",
                        "uptimeSeconds": 9641,
                        "ultrasonicDepthMm": 68.2,
                        "vibrationG": 0.035,
                        "railTempC": 31.5,
                        "axleSpeedKmh": 45.0,
                        "chainage": "KM 14+320",
                        "timestamp": 1700000000000,
                    },
                    "schedules": [{"id": "S-1", "section": "Mainline"}],
                }, f)
                temp_path = f.name
            ccd.STATE_FILE_PATH = temp_path
            ccd.LATEST_TELEMETRY = {}
            ccd.SCHEDULES_REGISTRY = []
            ccd.load_pi_state()
            self.assertEqual(ccd.LATEST_TELEMETRY, {}, "old telemetry must never be restored")
            self.assertEqual(len(ccd.SCHEDULES_REGISTRY), 1)
            self.assertEqual(ccd.SCHEDULES_REGISTRY[0]["id"], "S-1")
        finally:
            ccd.STATE_FILE_PATH = old_path
            ccd.LATEST_TELEMETRY = old_telem
            ccd.SCHEDULES_REGISTRY = old_schedules
            try:
                os.unlink(temp_path)
            except Exception:
                pass

    def test_identity_and_staleness_are_computed_from_this_process(self):
        import camera_crack_detector as ccd
        old_telem, old_ts = ccd.LATEST_TELEMETRY, ccd.TELEMETRY_UPDATED_AT_MS
        try:
            ccd.LATEST_TELEMETRY = {
                "version": "1.0.1",
                "instanceId": "old-process",
                "uptimeSeconds": 9641,
                "ultrasonicDepthMm": 68.2,
                "vibrationG": 0.035,
                "railTempC": 31.5,
                "axleSpeedKmh": 45.0,
                "dataMode": "SIMULATED",
                "timestamp": int(time.time() * 1000) - 4 * 86400 * 1000,
            }
            ccd.TELEMETRY_UPDATED_AT_MS = int(time.time() * 1000)
            telem = ccd.current_telemetry()
            self.assertEqual(telem["version"], ccd.RAILGUARD_VERSION)
            self.assertEqual(telem["instanceId"], ccd.INSTANCE_ID)
            self.assertLess(telem["uptimeSeconds"], 3600)
            self.assertFalse(telem["stale"])
            self.assertLess(telem["telemetryAgeSeconds"], 2)

            # A packet whose worker timestamp has not advanced is STALE even if
            # its own payload timestamp looks fresh (Pi clocks can be wrong).
            ccd.TELEMETRY_UPDATED_AT_MS = int(time.time() * 1000) - 120_000
            stale = ccd.current_telemetry()
            self.assertTrue(stale["stale"])
            self.assertGreater(stale["telemetryAgeSeconds"], 60)
        finally:
            self._restore(ccd, old_telem, old_ts)


if __name__ == "__main__":
    unittest.main()

