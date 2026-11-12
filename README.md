# RailGuard

> **Railway track surveillance, ultrasonic clearance monitoring, camera vision crack detection and safety speed restriction (TSR) — Android app + Raspberry Pi edge node + Firebase Realtime Database**

RailGuard pairs a native **Jetpack Compose Android app** with a **Raspberry Pi edge node** that serves a local Wi-Fi HTTP API and can publish to **Firebase Realtime Database**.

This README describes what the software **actually does today**. Where a feature depends on hardware that must be installed, that is stated explicitly. The software never invents a sensor reading, a GPS fix, a battery level or a crack detection.

---

## 1. System Architecture

```
[ Track sensors (optional, must be physically wired) ]
  • HC-SR04 / JSN-SR04T ultrasonic  → distance to railhead (clearance)
  • DS18B20 1-Wire                  → rail temperature
  • ADXL345 / ESP32 node           → vibration + speed (over UART)
  • Pi Camera / USB webcam         → rail surface images
  • GNSS receiver                  → only if a real fix is received
        │
        ▼
[ Raspberry Pi edge node ]  raspberry_pi/camera_crack_detector.py
  • Serves local Wi-Fi HTTP API on port 8080
  • Captures real camera frames (picamera2 / rpicam-still / libcamera-still /
    fswebcam / OpenCV VideoCapture)
  • Runs real OpenCV crack analysis (raspberry_pi/crack_vision.py)
  • Publishes telemetry + heartbeats to Firebase RTDB (optional)
  • Polls Firebase for cloud TSR / buzzer commands
        │
        ▼  HTTP over local Wi-Fi (sub-second)  +  HTTPS to Firebase (optional)
[ RailGuard Android app ]
  • Live telemetry, hardware health, defect register, inspections, work orders
  • Camera viewfinder, PDF dossier export, AI modelling screen
  • TSR dispatch, buzzer control, calibration commands
```

### Two data paths

| Path | Needs | Latency | Works without internet |
|---|---|---|---|
| **Pi Wi-Fi API** (`http://<pi-ip>:8080`) | Phone and Pi on the same Wi-Fi | ~1–20 ms | ✅ Yes |
| **Firebase RTDB** | Firebase credentials + internet | seconds | ❌ No |

The app always tries **Pi Wi-Fi first**, then falls back to the cloud.

---

## 2. What is real, and what depends on hardware

| Feature | Status |
|---|---|
| Live ultrasonic **clearance** reading | Real if an HC-SR04/JSN-SR04T is wired to the configured GPIO pins |
| Live rail temperature | Real if a DS18B20 is on `/sys/bus/w1/devices` |
| Vibration / speed / chainage | Real only when an ESP32/serial node pushes JSON over UART |
| Camera frames | Real if a Pi camera or USB webcam is present |
| **Crack detection** | Real OpenCV analysis of the camera image (`crack_vision.py`) |
| Crack length in mm | Requires `RAILGUARD_MM_PER_PIXEL` calibration, otherwise reported as *not calibrated* |
| GNSS position | Reported only when a receiver actually provides a fix |
| Battery level | **Not monitored** by the Pi — the app shows "not monitored" |
| TSR (speed restriction) | Delivered to the Pi over Wi-Fi and/or written to Firebase |
| Buzzer | Driven on the configured GPIO pin (RPi.GPIO / lgpio / gpiozero) |
| PDF dossier | Generated on-device, real SHA-256 of the package content |
| AI Oracle screen | Physics **model estimates** from the received telemetry — not a trained neural network |

### Things the software will NOT do

* It will not report a crack when OpenCV is not installed — it reports `vision.available = false`.
* It will not treat an ultrasonic distance as a crack (that bug caused permanent false alarms).
* It will not invent GPS coordinates, battery percentage or "all sensors online".
* It will not show a TSR as active unless the Pi or Firebase acknowledged it.
* It will not present a synthetic camera frame as real — such frames carry `X-RailGuard-Simulated: 1`.

---

## 3. Raspberry Pi installation (complete, copy/paste)

### 3.1 Base system

```bash
sudo apt update && sudo apt full-upgrade -y
sudo apt install -y python3 python3-pip python3-venv git \
                    i2c-tools libgpiod-dev pigpio libopenjp2-7 \
                    raspi-config openssh-server
sudo raspi-config
```

In `raspi-config`:
* `Interface Options → I2C` → **Yes** (needed for DS18B20)
* `Interface Options → SSH` → **Yes**
* `Interface Options → Camera` → **Yes** (if using a Pi camera, legacy only on older OS)

Reboot, then confirm:

```bash
sudo reboot
# after reboot:
ls /dev/i2c-*        # I2C present
vcgencmd get_throttled
```

### 3.2 Copy the code

```bash
sudo apt install -y git
cd ~
git clone https://github.com/skmohammadali786/RailGuard.git
cd RailGuard/raspberry_pi
```

### 3.3 Python environment

```bash
python3 -m venv ~/railguard-venv
source ~/railguard-venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

`requirements.txt` installs the Firebase Admin SDK and pyserial.

### 3.4 Real computer vision (required for crack detection)

```bash
pip install -r requirements-ai.txt     # opencv-python-headless + numpy
python3 camera_crack_detector.py --test-vision
```

Expected output when OpenCV is present:

```
[*] Vision backend: {'available': True, 'backend': 'opencv', 'opencvVersion': '4.x.x'}
[*] Self-test: {'ran': True, 'detected': True, ...}
```

If OpenCV is missing the service still runs, but `/api/capabilities` reports
`"vision": {"available": false, "reason": "OpenCV/NumPy not installed..."}` and **no crack is ever reported**.

### 3.5 Hardware wiring

**HC-SR04 (ultrasonic clearance)**

| HC-SR04 | Raspberry Pi |
|---|---|
| VCC | 5V |
| GND | GND |
| TRIG | BCM 23 (physical pin 16) |
| ECHO | BCM 24 (physical pin 18) through a 1 kΩ + 2 kΩ divider |

```bash
sudo nano /boot/firmware/config.txt      # Raspberry Pi OS Bookworm+
# add:
#   dtparam=i2c_arm=on
# Bookworm:  sudo raspi-config -> Interface Options -> I2C -> Yes
sudo reboot
```

**DS18B20 (temperature)**

```bash
sudo raspi-config            # enable I2C
# then wire: DS18B20 VCC->3.3V, GND->GND, DATA->GPIO4 (physical pin 7), 4.7kΩ pull-up to 3.3V
ls /sys/bus/w1/devices/       # should show 28-xxxxxxxxxxxx
```

**Buzzer**

```
Buzzer + leg  → 5V
Buzzer - leg  → BCM 18 (physical pin 12) through a 100 Ω resistor
```

### 3.6 Give the Pi a fixed IP on your Wi-Fi

```bash
sudo nmcli con add type wifi ifname wlan0 con-name railguard-wifi \
     ssid "YOUR_WIFI_NAME" password "YOUR_WIFI_PASSWORD" ipv4.method manual \
     ipv4.addresses 192.168.4.1/24 ipv4.gateway 192.168.4.254 ipv4.dns "192.168.4.254 8.8.8.8"
sudo nmcli con up railguard-wifi
ip -4 addr show wlan0        # confirm 192.168.4.1
```

Or edit `/etc/dhcpcd.conf`:

```
interface wlan0
static ip_address=192.168.4.1/24
static routers=192.168.4.254
static domain_name_servers=192.168.4.254 8.8.8.8
```

### 3.7 Firebase authentication (required only for cloud sync)

The Firebase **ID token expires after ~1 hour**. For a long-running install use a
refresh token + web API key so the node renews it automatically.

1. In the app: sign in → Settings → Firebase account → copy the **Refresh Token**
   and the **Web API Key** (Firebase console → Project settings → Your apps → Web API key).
2. On the Pi:

```bash
sudo mkdir -p /etc/railguard
sudo nano /etc/railguard/railguard.env
```

```ini
RAILGUARD_USER_UID=<your-firebase-user-uid>
RAILGUARD_DATABASE_URL=https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app
RAILGUARD_DEVICE_ID=RPI-TRACK-01
RAILGUARD_PORT=8080
RAILGUARD_SCAN_INTERVAL=2.5

# --- Firebase (auto-renewing) ---
RAILGUARD_REFRESH_TOKEN=
RAILGUARD_FIREBASE_API_KEY=

# --- Local Wi-Fi API protection (recommended) ---
RAILGUARD_API_KEY=change-this-long-random-string
RAILGUARD_BIND_HOST=0.0.0.0
```

```bash
sudo chmod 600 /etc/railguard/railguard.env
```

If you only want the local Wi-Fi API and no cloud, simply do **not** set the Firebase
values and start the service with `--no-cloud`.

### 3.8 Start the service

**Manual start (to test):**

```bash
cd ~/RailGuard/raspberry_pi
source ~/railguard-venv/bin/activate

python3 camera_crack_detector.py \
  --uid <YOUR_USER_UID> \
  --db https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app \
  --refresh-token "<REFRESH_TOKEN>" \
  --firebase-api-key "<WEB_API_KEY>" \
  --local-api-key "<SAME_KEY_AS_APP>" \
  --buzzer-pin 18 \
  --trig-pin 23 --echo-pin 24 \
  --port 8080
```

Add `--no-cloud` to run completely offline (Wi-Fi API only).

**Auto-start as a systemd service:**

```bash
cd ~/RailGuard/raspberry_pi
chmod +x setup_autostart.sh
./setup_autostart.sh <YOUR_USER_UID> https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app
```

This installs:
* `/etc/railguard/railguard.env` (mode 600)
* `/usr/local/bin/railguard-pi-start.sh` (launcher, reads the env file)
* `/etc/systemd/system/railguard.service`

```bash
sudo systemctl status railguard
sudo journalctl -u railguard -f
```

### 3.9 Verify the Pi

```bash
curl http://127.0.0.1:8080/api/health
curl http://127.0.0.1:8080/api/capabilities
curl -H "X-API-Key: <KEY>" http://127.0.0.1:8080/api/all_data | python3 -m json.tool
curl -D - -o /tmp/f.jpg http://127.0.0.1:8080/api/camera_frame | grep -i simulated
```

`/api/health` is deliberately **open** (for monitoring). Every other endpoint needs
the API key when `--local-api-key` is set.

---

## 4. The local Wi-Fi HTTP API (port 8080)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | Liveness (no key needed) |
| GET | `/api/telemetry` | Latest sensor telemetry |
| GET | `/api/all_data` | Telemetry + hardware + defects + tasks + AI metrics |
| GET | `/api/capabilities` | Camera/vision/cloud/auth capability report |
| GET | `/api/camera_frame` | Latest camera image (`X-RailGuard-Simulated` header) |
| GET | `/api/tsr` | Read the active TSR (`?speed=` sets it) |
| POST | `/api/tsr` | Apply a TSR (`{"speed": 25}`) |
| POST | `/api/buzzer` | Ring/silence the buzzer (`{"state":"RING","duration":8}`) |
| POST | `/api/crack/clear` | Acknowledge and clear the crack latch |
| POST | `/api/calibrate` | Reset the ultrasonic baseline |
| POST | `/api/camera_capture` | Capture now (`?high_res=1`) |
| GET | `/api/buzzer` | Buzzer status |

Authentication: send `X-API-Key: <key>` or `?api_key=<key>`.

---

## 5. Android app

### Build the APK

```bash
cd ~/RailGuard
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Connect the app to the Pi

1. Phone and Pi must be on the **same Wi-Fi** (or the phone joins the Pi's hotspot).
2. App → **Settings → System & Sensor Configurations**.
3. Enter **Raspberry Pi Wi-Fi IP Address** (e.g. `192.168.4.1`).
4. Enter **Pi API Key** — must match `RAILGUARD_API_KEY` / `--local-api-key` on the Pi.
   Leave it empty if the Pi runs without a key.
5. Tap **Test Wi-Fi Ping** → shows `✓ Successfully connected to Raspberry Pi (:8080)! Latency: Xms`.

### What the app then shows

* **Home / Live Telemetry hub** – ultrasonic clearance, vibration, rail temperature, speed,
  chainage, hardware status, TSR state.
* **Camera view** – the live Pi frame. If the Pi has no camera the frame is labelled
  **simulated** and the app says so.
* **Defect register / Inspections / Work orders** – populated from the Pi and/or Firebase.
* **PDF dossier** – real on-device PDF; the evidence pack is hashed with a real SHA-256.
* **TSR buttons (25 / 60 / 120 km/h)** – the button turns red if the order failed, and a
  crack interlock blocks raising the speed above 25 km/h while a crack alert is active.
* **Buzzer** – the app rings the phone locally *and* commands the Pi buzzer; if the Pi is
  unreachable the app says **"Buzzer command FAILED"** instead of pretending.

---

## 6. Running the tests

```bash
cd ~/RailGuard/raspberry_pi
python3 -m py_compile *.py          # syntax check
python3 test_gateway.py             # 8 tests: HTTP API, TSR, crack latch, API key
python3 test_vision.py              # OpenCV tests (auto-skips without OpenCV)
```

---

## 7. Directory layout

```
RailGuard/
├── app/                                  # Jetpack Compose Android app
│   └── src/main/java/com/example/railguard/
│       ├── data/RailGuardFirebaseService.kt   # REST + Firebase + Pi HTTP client
│       ├── model/Models.kt                    # domain models
│       ├── ui/screens/                        # Home, Defects, Inspections, Reports,
│       │                                       # Settings, AiOracle, Map, Maintenance
│       └── util/PdfExporter.kt                # on-device PDF dossier generator
├── raspberry_pi/
│   ├── camera_crack_detector.py         # Pi service: HTTP API, sensors, CV, cloud
│   ├── crack_vision.py                  # real OpenCV crack analysis
│   ├── firebase_auth.py                 # ID-token auto-refresh manager
│   ├── railguard_gateway.py             # UART sensor node → Firebase gateway
│   ├── simulate_sensors.py              # sensor simulator (needs credentials)
│   ├── setup_autostart.sh               # systemd installer
│   ├── test_gateway.py / test_vision.py # test suites
│   ├── requirements.txt                 # firebase-admin, pyserial
│   └── requirements-ai.txt              # opencv-python-headless, numpy
├── docs/FIREBASE_SCHEMA.md
├── database.rules.json
└── storage.rules
```

---

## 8. Security notes

* All cloud data lives under `railguard/users/{UID}/` (see `database.rules.json`).
* Credentials live only in `/etc/railguard/railguard.env` (mode 600) on the Pi — never in the APK.
* The local Wi-Fi API supports a shared secret; use a long random value.
* Bind to `127.0.0.1` with `--bind 127.0.0.1` if the Pi must not answer other machines.

---

## 9. Known limitations

* Crack detection is classical computer vision on the visible rail surface only. It does
  **not** replace ultrasonic internal-flaw inspection, and it cannot measure crack depth.
* Crack length in millimetres needs a `RAILGUARD_MM_PER_PIXEL` calibration.
* A detected crack is latched for `RAILGUARD_CRACK_ACTIVE_SECONDS` (default 60 s) and can be
  acknowledged with `POST /api/crack/clear`; automatic re-alerts are throttled by
  `RAILGUARD_CRACK_COOLDOWN_SECONDS` (default 120 s).
* Cloud sync stops when the ID token expires unless a refresh token + API key are configured.
* Firebase Cloud Storage / Firestore buckets were not available in this project, so
  photo/report uploads to Storage are not yet functional.
