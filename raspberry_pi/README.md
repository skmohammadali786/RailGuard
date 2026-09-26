# RailGuard Raspberry Pi sensor gateway

The Android app does not connect directly to GPIO, UART, CAN, or an ESP32.
Use the Raspberry Pi as the edge gateway:

```text
Rail sensors / ESP32
        │ USB, UART, I2C, SPI, GPIO, CAN, or RS-485
        ▼
Raspberry Pi gateway
        │ Firebase Admin SDK over HTTPS
        ▼
Firebase Realtime Database
        │ authenticated Android REST reads
        ▼
RailGuard Android app
```

## 1. Install on the Pi

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
```

Copy the Firebase service-account JSON to the Pi without committing it:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/etc/railguard/firebase-service-account.json
export RAILGUARD_DATABASE_URL=https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app
export RAILGUARD_USER_UID=<authenticated-railguard-user-uid>
export RAILGUARD_DEVICE_ID=RPI-TRACK-01
export RAILGUARD_SERIAL_PORT=/dev/ttyUSB0
export RAILGUARD_BAUD_RATE=115200
python railguard_gateway.py
```

The service-account file is a credential. Keep it outside the repository and
restrict its filesystem permissions to the gateway service account.

## 2. Sensor packet contract

The serial device must emit one JSON object per line. The gateway normalizes
the following fields:

```json
{
  "nodeId": "RPI-TRACK-01",
  "ultrasonicDepthMm": 46.2,
  "vibrationG": 0.041,
  "railTempC": 31.4,
  "axleSpeedKmh": 14.2,
  "chainage": "14+320",
  "hardware": "Pi + ESP32 + UT + ADXL345 + PT100",
  "status": "ACTIVE_SYNC"
}
```

For I2C, SPI, GPIO, CAN, or RS-485 sensors, replace the serial reader with an
adapter that produces the same dictionary. Keep sensor calibration and unit
conversion in that adapter.

## 3. Firebase paths

Telemetry is written to:

```text
railguard/users/{RAILGUARD_USER_UID}/live_sensors/telemetry
railguard/users/{RAILGUARD_USER_UID}/devices/{DEVICE_ID}/last_telemetry
```

The Android train corridor screen polls the first path every three seconds.
TSR and calibration commands are read from:

```text
railguard/users/{RAILGUARD_USER_UID}/trains/{DEVICE_ID}/tsr
railguard/users/{RAILGUARD_USER_UID}/esp_sensors/{DEVICE_ID}/command
```

The gateway acknowledges consumed commands at the same paths. Implement
`apply_tsr` and `apply_calibration` for the actual train controller and sensor
hardware before operating a real railway system.