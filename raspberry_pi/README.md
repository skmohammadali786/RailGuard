# RailGuard Raspberry Pi edge node

This folder contains **two** separate but complementary programs.

| File | What it is |
|---|---|
| `camera_crack_detector.py` | **The main service.** Serves the local Wi-Fi HTTP API on port 8080, reads real sensors, captures real camera frames, runs real OpenCV crack analysis, and optionally syncs to Firebase. |
| `railguard_gateway.py` | UART/serial gateway for an external microcontroller (ESP32 etc.) that pushes JSON sensor lines. Polls Firebase for TSR / calibration / buzzer commands. |

Full end-to-end setup instructions (wiring, systemd, Firebase auth, verification) are in the
**root `README.md`**, section *"Raspberry Pi installation"*.

---

## 1. Quick start (local Wi-Fi only, no cloud)

```bash
cd ~/RailGuard/raspberry_pi
python3 -m venv ~/railguard-venv
source ~/railguard-venv/bin/activate
pip install -r requirements.txt
pip install -r requirements-ai.txt        # optional: enables real crack detection

python3 camera_crack_detector.py \
  --port 8080 \
  --no-cloud \
  --local-api-key my-secret \
  --buzzer-pin 18 --trig-pin 23 --echo-pin 24
```

```bash
curl http://127.0.0.1:8080/api/health
curl -H "X-API-Key: my-secret" http://127.0.0.1:8080/api/capabilities
```

## 2. Useful flags

| Flag | Meaning |
|---|---|
| `--uid <UID>` | Firebase user UID used for cloud paths |
| `--db <URL>` | Firebase Realtime Database URL |
| `--auth <ID_TOKEN>` | Firebase ID token (expires in ~1 hour) |
| `--refresh-token <T>` + `--firebase-api-key <K>` | Auto-renewing Firebase auth (**use this**) |
| `--local-api-key <K>` | Shared secret for the local API (`/api/health` stays open) |
| `--bind <ADDR>` | Bind address, e.g. `127.0.0.1` for Pi-only access |
| `--no-cloud` | Never contact Firebase |
| `--device <ID>` | Device node id (default `RPI-TRACK-01`) |
| `--port <N>` | HTTP port (default 8080) |
| `--interval <S>` | Seconds between scans (default 2.5) |
| `--buzzer-pin <N>` | BCM pin of the trackside buzzer |
| `--trig-pin <N>` / `--echo-pin <N>` | BCM pins of the HC-SR04 |
| `--test-vision` | Run the OpenCV self-test and exit |
| `--test-buzzer` | Ring the buzzer once and exit |
| `--test-sensors` | Read the sensors once and exit |
| `--simulate-anomaly-interval N` | **DEMO ONLY** – forces a fake anomaly every N scans |

## 3. Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `RAILGUARD_API_KEY` | *(empty)* | Local API shared secret |
| `RAILGUARD_BIND_HOST` | `0.0.0.0` | Bind address |
| `RAILGUARD_DEVICE_ID` | `RPI-TRACK-01` | Node identifier |
| `RAILGUARD_CRACK_ACTIVE_SECONDS` | `60` | How long a crack stays latched |
| `RAILGUARD_CRACK_COOLDOWN_SECONDS` | `120` | Minimum gap between automatic crack pipelines |
| `RAILGUARD_MM_PER_PIXEL` | `0` | Pixel→mm calibration (0 = uncalibrated) |
| `RAILGUARD_CRACK_CONFIDENCE` | `0.55` | Vision confidence threshold |
| `RAILGUARD_ID_TOKEN` / `RAILGUARD_REFRESH_TOKEN` / `RAILGUARD_FIREBASE_API_KEY` | – | Firebase credentials |

## 4. Important behaviour notes

* **The ultrasonic sensor measures clearance to the railhead — it is not a thickness gauge
  and it cannot detect a crack.** An out-of-range reading only raises a
  `clearanceWarning` after several consecutive deviations from a rolling baseline.
* **A crack is only reported when the OpenCV model finds one.** If OpenCV is missing the
  service says so instead of faking a detection.
* **Crack latches expire.** After `RAILGUARD_CRACK_ACTIVE_SECONDS` the anomaly clears
  itself, and `POST /api/crack/clear` acknowledges it immediately.
* **Synthetic frames are labelled.** Any generated image is returned with
  `X-RailGuard-Simulated: 1`.
* **Diagnostics are honest.** Missing sensors are reported as `NOT_INSTALLED`; the battery is
  reported as not monitored; GNSS is only "locked" with a real fix.

## 5. Tests

```bash
python3 -m py_compile *.py
python3 test_gateway.py     # HTTP API, TSR validation, crack latch, API-key enforcement
python3 test_vision.py      # OpenCV tests (auto-skips if OpenCV is not installed)
```

## 6. Serial gateway (`railguard_gateway.py`)

For an external microcontroller emitting one JSON object per line:

```bash
export RAILGUARD_DATABASE_URL=https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app
export RAILGUARD_USER_UID=<authenticated-railguard-user-uid>
export RAILGUARD_DEVICE_ID=RPI-TRACK-01
export RAILGUARD_SERIAL_PORT=/dev/ttyUSB0
export RAILGUARD_BAUD_RATE=115200
export GOOGLE_APPLICATION_CREDENTIALS=/etc/railguard/firebase-service-account.json
python3 railguard_gateway.py
```

Packet contract:

```json
{
  "nodeId": "RPI-TRACK-01",
  "ultrasonicDepthMm": 68.2,
  "vibrationG": 0.041,
  "railTempC": 31.4,
  "axleSpeedKmh": 14.2,
  "chainage": "14+320",
  "hardware": "Pi + ESP32 + UT + ADXL345 + PT100",
  "status": "ACTIVE_SYNC"
}
```

The gateway keeps polling Firebase commands even when the serial port is disconnected, and
it reconnects to the serial port automatically when it comes back.
