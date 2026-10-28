# RailGuard

> **Autonomous AI-Powered Railway Track Surveillance, Ultrasonic Defect Detection, and Safety Analysis Platform**

RailGuard is a complete, field-grade railway infrastructure safety platform engineered for track inspectors, permanent way engineers, and safety leads. It pairs native **Jetpack Compose Android mobile terminals** with **Raspberry Pi edge gateways** and **Firebase Cloud Realtime Database** to deliver real-time acoustic crack detection, wheel impact vibration monitoring, continuous rail thermal stress analysis, and automated speed restriction interlocks.

---

## 1. System Architecture

```
[ Track Sensors ]
  • Piezoelectric Ultrasonic Transducer (mm thickness & subsurface echoes)
  • ADXL345 3-Axis Accelerometer (g impact shock & vibration)
  • PT100 Contact Thermistor (°C rail surface temperature)
  • GNSS Receiver (axle ground speed & corridor chainage)
         │
         ▼ (UART Serial /dev/ttyUSB0 @ 115200 baud)
[ Raspberry Pi Edge Gateway ] (raspberry_pi/railguard_gateway.py)
  • Normalizes sensor packets & validates incoming data
  • Publishes telemetry & device heartbeat
  • Listens for TSR speed restriction & calibration commands
         │
         ▼ (HTTPS / TLS over Wi-Fi, Ethernet, or 4G LTE)
[ Firebase Cloud Backend ]
  • Firebase Authentication (Google Identity Toolkit v1)
  • Realtime Database (Multi-tenant isolated under railguard/users/{UID}/)
  • Cloud Storage (Photographic crack evidence & generated PDF reports)
         │
         ▲ (HTTPS REST polling every 3–10s with JWT bearer authentication)
[ RailGuard Android App ] (Jetpack Compose)
  • Real-time diagnostics console & gauge displays
  • Live camera inspection viewfinder & defect registry
  • AI Oracle: dynamic A-scan echo & thermal buckling physics
  • Two-way remote hardware actuation (TSR dispatch & sensor calibration)
  • Cryptographically signed forensic PDF safety dossiers
```

---

## 2. Key Capabilities & Screen Workflows

### A. Real-Time Hardware Diagnostics
- **Live Sensor Telemetry**: Continuous readout of ultrasonic head thickness, vibration shock ($g$), rail surface temperature (°C), axle speed (km/h), and track milepost.
- **Liveness & Heartbeat**: Real-time heartbeat tracking with automated stale/offline alerts (`LIVE`, `STALE`, `OFFLINE`).
- **Two-Way Hardware Actuation**:
  - **TSR Orders**: Dispatch 25 km/h, 60 km/h, or 120 km/h emergency speed orders down to train cab interlocks.
  - **Zero Calibration**: Transmit sensor offset calibration commands to the microcontroller.

### B. Track Defect & Crack Inspection
- **Defect Registry**: Searchable and filterable registry tracking transverse fissures, head checking, and rail squats.
- **Field Defect Creation**: Log defects directly from the field with chainage coordinate, crack aperture, and severity.
- **A-Scan Oscilloscope**: Visualizes ultrasonic acoustic wave entrance, flaw, and backwall reflection echoes.
- **Continuous Welded Rail (CWR) Stability**: Calculates axial compressive stress and Euler track buckling risk from live PT100 temperatures.

### C. Inspections, Maintenance & Reporting
- **Field Inspection Patrols**: Live viewfinder camera HUD with real-time sensor overlays and photo evidence upload.
- **Corrective Work Orders**: Maintenance task dispatching with torque requirements and before/after verification.
- **Safety Audit Trail**: Immutable audit journal logging every operator sign-off, verification, and speed order.
- **Native PDF Dossiers**: Generates on-device PDF inspection reports with SHA-256 verification signatures.

---

## 3. Directory Layout

```
RailGuard/
├── app/                                 # Native Android Application (Jetpack Compose)
│   ├── build.gradle.kts                 # Android build configuration
│   └── src/main/java/com/example/railguard/
│       ├── data/                        # Firebase service layer & REST engine
│       │   └── RailGuardFirebaseService.kt
│       ├── model/                       # Core domain models (Defect, Task, Telemetry)
│       ├── ui/
│       │   ├── navigation/              # Navigation router & app shell
│       │   │   └── RailGuardApp.kt
│       │   └── screens/                 # Compose UI screens
│       │       ├── HomeScreen.kt
│       │       ├── HardwareTelemetryScreen.kt
│       │       ├── DefectScreens.kt
│       │       ├── InspectionScreens.kt
│       │       ├── MaintenanceScreens.kt
│       │       ├── MapScreens.kt
│       │       ├── ReportScreens.kt
│       │       ├── SettingsScreens.kt
│       │       └── AiOracleScreen.kt
│       └── util/                        # PDF generator & formatters
│           └── PdfExporter.kt
├── docs/
│   └── FIREBASE_SCHEMA.md               # Complete canonical database schema specification
├── raspberry_pi/                        # Edge Gateway & Hardware Simulation
│   ├── railguard_gateway.py             # Production UART serial-to-Firebase gateway
│   ├── test_gateway.py                  # Automated unit test suite
│   ├── simulate_sensors.py              # Hardware simulator for end-to-end testing
│   ├── requirements.txt                 # Python dependencies
│   └── README.md                        # Gateway deployment guide
├── database.rules.json                  # Firebase Realtime Database security rules
└── storage.rules                       # Firebase Cloud Storage security rules
```

---

## 4. Running the Raspberry Pi Gateway

### Prerequisites
```bash
cd raspberry_pi
pip install -r requirements.txt
```

### Starting the Gateway
```bash
export RAILGUARD_DATABASE_URL="https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app"
export RAILGUARD_USER_UID="<YOUR_USER_UID>"
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"

python3 railguard_gateway.py
```

### Running Automated Gateway Tests
```bash
python3 raspberry_pi/test_gateway.py
```

### Simulating Sensor Hardware (Without Physical Cart)
```bash
# Push realistic simulated telemetry to Firebase
python3 raspberry_pi/simulate_sensors.py --uid <YOUR_USER_UID> --count 50
```

---

## 5. Security & Multi-Tenancy

- All database access is strictly isolated under `railguard/users/{UID}/` via `auth.uid == $uid`.
- No sensitive keys or credentials are baked into client binaries.
- Two-way command channels enforce 10-minute expiry windows and idempotent cursor tracking to prevent re-execution of stale speed orders.
