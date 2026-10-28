# RailGuard Canonical Firebase Schema & Integration Specification

This document defines the production data schema, security access patterns, and hardware contract for **RailGuard**.

---

## 1. Firebase Realtime Database Namespace Architecture

All user-owned data is strictly partitioned by Firebase Authentication User ID (`UID`) to guarantee multi-tenant security and isolation under:

```
railguard/users/{UID}/
```

### Complete Node Tree

```
railguard/
└── users/
    └── {UID}/
        ├── profile/                         # Inspector profile and contact info
        ├── settings/                        # App and sync preferences
        ├── defects/                         # Track defect & crack register
        │   └── {defectId}/                  # e.g. "DEF-2024-001"
        ├── tasks/                           # Maintenance work orders & SLA
        │   └── {taskId}/                    # e.g. "MT-881"
        ├── inspections/                     # Patrol inspection history
        │   └── {inspectionId}/              # e.g. "INSP-4921"
        ├── observations/                    # Field optical & sensor observations
        │   └── {obsId}/                     # e.g. "OBS-101"
        ├── notifications/                   # Dispatch and emergency alerts
        │   └── {notifId}/                   # e.g. "NOTIF-01"
        ├── audit_logs/                      # Immutable safety audit trial
        │   └── {logId}/                     # e.g. "log_1718721135000"
        ├── live_sensors/
        │   └── telemetry/                   # Latest live sensor frame (read by phone)
        ├── devices/
        │   └── {deviceId}/
        │       ├── heartbeat/               # Node liveness and connectivity
        │       ├── last_telemetry/          # Device-specific telemetry cache
        │       └── telemetry_history/       # Append-only time-series telemetry log
        ├── trains/
        │   └── {deviceId}/
        │       └── tsr/                     # Active Temporary Speed Restriction command
        └── esp_sensors/
            └── {deviceId}/
                └── command/                 # Sensor zero/calibration command
```

---

## 2. Telemetry & Hardware Contract

### Live Sensor Telemetry (`railguard/users/{UID}/live_sensors/telemetry`)
Written by Raspberry Pi gateway (`railguard_gateway.py`), polled by Android app every 3–10s.

```json
{
  "nodeId": "RPI-TRACK-01",
  "ultrasonicDepthMm": 46.2,
  "vibrationG": 0.042,
  "railTempC": 31.5,
  "axleSpeedKmh": 45.0,
  "chainage": "KM 14+320",
  "hardware": "ESP32-WROOM-32 / Piezo UT / ADXL345 / PT100",
  "status": "ACTIVE_SYNC",
  "timestamp": 1718721135000
}
```

### Device Heartbeat (`railguard/users/{UID}/devices/{deviceId}/heartbeat`)
```json
{
  "deviceId": "RPI-TRACK-01",
  "status": "ONLINE",
  "lastSeen": 1718721135000,
  "serialPort": "/dev/ttyUSB0",
  "baudRate": 115200
}
```

---

## 3. Remote Hardware Command Lifecycle

### Temporary Speed Restriction (`railguard/users/{UID}/trains/{deviceId}/tsr`)
Dispatched by phone operator, consumed by Raspberry Pi / Train cab interlock.

```json
{
  "trainId": "RPI-TRACK-01",
  "activeTsrSpeedKmH": 25,
  "reason": "Operator emergency slow order for transverse fissure at KM 14+320",
  "issuedAt": 1718721135000,
  "issuedBy": "inspector@railguard.io",
  "gatewayStatus": "EXECUTED",
  "gatewayDevice": "RPI-TRACK-01",
  "acknowledgedAt": 1718721135500
}
```

### Sensor Calibration (`railguard/users/{UID}/esp_sensors/{deviceId}/command`)
```json
{
  "command": "ZERO_CALIBRATE",
  "issuedAt": 1718721135000,
  "operator": "Lead Inspector",
  "gatewayStatus": "EXECUTED",
  "gatewayDevice": "RPI-TRACK-01",
  "acknowledgedAt": 1718721135400
}
```

---

## 4. Defect & Task Models

### Defect Node (`railguard/users/{UID}/defects/{defectId}`)
```json
{
  "id": "DEF-2024-001",
  "title": "Transverse Railhead Fissure",
  "section": "Sector 4B (KM 14+320)",
  "score": "Risk 90/100",
  "tone": "CRITICAL",
  "time": "14:32 UTC",
  "detail": "Ultrasonic depth depression detected under cyclic load",
  "estimatedLength": "42.5 mm",
  "latitude": 51.5074,
  "longitude": -0.1278,
  "riskScore": 90,
  "chainageCoordinate": "KM 14+320",
  "aiConfidencePercent": 98,
  "aiPrescribedAction": "Immediate 25 km/h TSR and joint bar clamping"
}
```

### Maintenance Task Node (`railguard/users/{UID}/tasks/{taskId}`)
```json
{
  "id": "MT-881",
  "title": "Thermite Weld Repair & Re-alignment",
  "section": "Sector 4B (KM 14+320)",
  "due": "Within 24 Hours",
  "tone": "CRITICAL",
  "assignee": "Crew 04 - Track Maintenance",
  "status": "Pending Verification",
  "torque": "220 Nm"
}
```

---

## 5. Security Rules (`database.rules.json`)
- Strict multi-tenant isolation: `auth.uid == $uid`.
- Unauthenticated access is completely rejected.
- Optimized query indexes defined for `severity`, `date`, `priority`, `status`, and `timestamp`.
