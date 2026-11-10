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

## 4. Trackside Hardware Buzzer Connection

When a crack or track anomaly is detected:
1. The Raspberry Pi immediately triggers the physical buzzer via GPIO using its multi-backend driver (`RPi.GPIO`, `gpiozero`, `pinctrl`, `raspi-gpio`, or sysfs).
2. The Android app displays an emergency crack alert and simultaneously commands the buzzer over direct Wi-Fi HTTP (`/api/buzzer`) and Firebase Cloud.

### Buzzer Hardware Wiring:
- **Buzzer Positive (+)**: Connect to **BCM GPIO 18** (Physical Pin 12) on Raspberry Pi.
- **Buzzer Negative (-)**: Connect to **GND** (Physical Pin 14) on Raspberry Pi.
- *Active-Low Buzzers*: If your buzzer module is active-low (sounds when input is pulled low), pass `--buzzer-active-low` or set `export RAILGUARD_BUZZER_ACTIVE_LOW=1`.
- *Custom Pin*: Pass `--buzzer-pin <number>` or set `export RAILGUARD_BUZZER_PIN=<number>`.

### Testing the Buzzer:
```bash
# Test ring buzzer for 3 seconds directly on the Pi
python3 camera_crack_detector.py --test-buzzer

# Or test via HTTP GET from terminal or browser
curl http://<pi_ip>:8080/api/buzzer?state=RING&duration=5

# Or test via HTTP POST
curl -X POST http://<pi_ip>:8080/api/buzzer -H "Content-Type: application/json" -d '{"state":"RING","duration":5}'
```

---

## 5. Raspberry Pi Zero W Pinout & Complete Hardware Wiring

### 40-Pin Header Reference:

```text
                  3.3V Power  (01) [02]  5V Power (VCC for Ultrasonic)
          I2C1 SDA / GPIO 02  (03) [04]  5V Power
          I2C1 SCL / GPIO 03  (05) [06]  Ground (GND)
   DS18B20 1-Wire  / GPIO 04  (07) [08]  GPIO 14 / UART TX
                      Ground  (09) [10]  GPIO 15 / UART RX
                     GPIO 17  (11) [12]  GPIO 18 ---> [BUZZER POSITIVE (+)]
                     GPIO 27  (13) [14]  Ground  ---> [BUZZER NEGATIVE (-)]
     [UT TRIG] ----> GPIO 22  (15) [16]  GPIO 23 ---> [UT TRIG DEFAULT]
                      3.3V    (17) [18]  GPIO 24 <--- [UT ECHO (via divider)]
         SPI MOSI  / GPIO 10  (19) [20]  Ground
         SPI MISO  / GPIO 09  (21) [22]  GPIO 25
         SPI SCLK  / GPIO 11  (23) [24]  GPIO 08
                      Ground  (25) [26]  GPIO 07
```

### Complete Sensor Connection Table:

| Sensor / Actuator | Sensor Pin | Raspberry Pi Zero W Pin | Physical Pin # | Notes |
|---|---|---|---|---|
| **Buzzer** | Positive (+) | BCM GPIO 18 | **Pin 12** | Active High / Active Low supported |
| **Buzzer** | Negative (-) | GND | **Pin 14** | Shared system ground |
| **Ultrasonic (HC-SR04)** | VCC | 5V Power | **Pin 2** | Needs 5V power supply |
| **Ultrasonic (HC-SR04)** | GND | Ground | **Pin 6** | Common ground with Pi |
| **Ultrasonic (HC-SR04)** | TRIG | BCM GPIO 23 | **Pin 16** | 10µs pulse output from Pi |
| **Ultrasonic (HC-SR04)** | ECHO | BCM GPIO 24 | **Pin 18** | **Use 1kΩ / 2kΩ divider** (5V -> 3.3V) |
| **Temp (DS18B20)** | VCC | 3.3V Power | **Pin 1** | Operating voltage 3.0V - 5.0V |
| **Temp (DS18B20)** | GND | Ground | **Pin 9** | Common ground with Pi |
| **Temp (DS18B20)** | DATA | BCM GPIO 4 | **Pin 7** | 4.7kΩ pullup between DATA & 3.3V |
| **CSI Camera** | Ribbon cable | CSI Connector | **CSI Port** | Ribbon blue side faces board back |

> [!WARNING]
> The HC-SR04 ECHO pin outputs **5V** logic, while the Raspberry Pi GPIO pins are strictly **3.3V tolerant**.
> Always insert a voltage divider between the sensor ECHO pin and Pi GPIO 24:
> `ECHO pin` ─── `[1kΩ Resistor]` ──┬─── `GPIO 24 (Pin 18)`
>                                   │
>                            `[2kΩ Resistor]`
>                                   │
>                                 `GND (Pin 6)`

### Verify Physical Sensors:
```bash
# Verify sensor readings from connected hardware
python3 camera_crack_detector.py --test-sensors

# Run full continuous scan with live hardware streaming
python3 camera_crack_detector.py --uid <USER_UID> --db <DATABASE_URL> --port 8080
```