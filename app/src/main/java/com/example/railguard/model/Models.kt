package com.example.railguard.model

import androidx.compose.ui.graphics.Color

enum class Tone {
    CRITICAL,
    WARNING,
    HEALTHY,
    INFO,
    NEUTRAL
}

data class Defect(
    val id: String,
    val title: String,
    val section: String,
    val score: String,
    val tone: Tone,
    val time: String,
    val detail: String,
    val estimatedLength: String = "14.2 mm",
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val riskScore: Int = 85,
    val chainageCoordinate: String = "KM 42+180",
    val aiConfidencePercent: Int = 94,
    val aiPrescribedAction: String = "Immediate clamping and ultrasonic depth verification within 24h"
)

data class MaintenanceTask(
    val id: String,
    val title: String,
    val section: String,
    val due: String,
    val tone: Tone,
    val assignee: String,
    val status: String = "Pending",
    val torque: String = "320 Nm"
)

data class InspectionRecord(
    val id: String,
    val section: String,
    val date: String = "Today",
    val inspector: String,
    val status: String,
    val framesCount: Int = 1420,
    val detectionsCount: Int = 3,
    val detectedCrackTitle: String = "Surface Hairline Fissure (Gauge Face)",
    val recommendedMaintenanceAction: String = "Schedule track grinding and joint realignment"
)

data class ObservationItem(
    val id: String,
    val title: String,
    val chainage: String,
    val time: String,
    val severity: String,
    val tone: Tone
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val tone: Tone,
    val read: Boolean = false
)

data class TrackAnomaly(
    val title: String,
    val value: String,
    val severity: String,
    val tone: Tone
)

// ==========================================
// RAIL DATA REPOSITORY
// ==========================================

object RailDataRepository {
    val initialDefects = listOf(
        Defect(
            id = "DEF-8021",
            title = "Transverse Head Fissure",
            section = "Sector 4B · KM 42+180",
            score = "Critical (89%)",
            tone = Tone.CRITICAL,
            time = "14m ago",
            detail = "Deep railhead crack detected via ultrasonic resonance and camera bounding box. High derailment risk under freight load.",
            estimatedLength = "24.6 mm",
            latitude = 28.6142,
            longitude = 77.2085,
            riskScore = 92,
            chainageCoordinate = "KM 42+180",
            aiConfidencePercent = 95,
            aiPrescribedAction = "Immediate 20 km/h speed restriction and rail joint replacement."
        ),
        Defect(
            id = "DEF-8019",
            title = "Gauge Face Wear & Shelling",
            section = "Sector 2A · KM 38+940",
            score = "Warning (64%)",
            tone = Tone.WARNING,
            time = "1h ago",
            detail = "Surface metallic spalling and flaking along outer curve rail. Gauge clearance widened by 3.2 mm.",
            estimatedLength = "12.0 mm",
            latitude = 28.6120,
            longitude = 77.2060,
            riskScore = 68,
            chainageCoordinate = "KM 38+940",
            aiConfidencePercent = 88,
            aiPrescribedAction = "Schedule profilometer grinding run within 72 hours."
        ),
        Defect(
            id = "DEF-8014",
            title = "Loose Fastener Clip",
            section = "Sector 1C · KM 31+420",
            score = "Advisory (42%)",
            tone = Tone.INFO,
            time = "3h ago",
            detail = "Pandrol e-clip displaced by 15 mm. Tie plate elastomer pad intact.",
            estimatedLength = "5.0 mm",
            latitude = 28.6095,
            longitude = 77.2025,
            riskScore = 45,
            chainageCoordinate = "KM 31+420",
            aiConfidencePercent = 91,
            aiPrescribedAction = "Tighten to 320 Nm nominal torque during next scheduled patrol."
        )
    )

    val initialTasks = listOf(
        MaintenanceTask(
            id = "TSK-401",
            title = "Clamp & Thermite Weld Transverse Fissure",
            section = "Sector 4B · KM 42+180",
            due = "Today, 18:00",
            tone = Tone.CRITICAL,
            assignee = "Gang #3 (S. Miller)",
            status = "In Progress",
            torque = "340 Nm"
        ),
        MaintenanceTask(
            id = "TSK-398",
            title = "Gauge Profiling & Re-torquing Clips",
            section = "Sector 2A · KM 38+940",
            due = "Tomorrow, 09:00",
            tone = Tone.WARNING,
            assignee = "Gang #1 (R. Patel)",
            status = "Pending",
            torque = "320 Nm"
        ),
        MaintenanceTask(
            id = "TSK-385",
            title = "Ballast Tamping & Drainage Clearance",
            section = "Sector 1C · KM 31+420",
            due = "24 Sep",
            tone = Tone.INFO,
            assignee = "Contractor TrackCare",
            status = "Scheduled",
            torque = "Nominal"
        )
    )

    val initialInspections = listOf(
        InspectionRecord(
            id = "INSP-2026-0901",
            section = "North Mainline Corridor (Sector 4)",
            date = "Today, 08:30",
            inspector = "E. Chen (ID: #4092)",
            status = "Active Live Stream",
            framesCount = 2840,
            detectionsCount = 3,
            detectedCrackTitle = "Transverse Head Fissure",
            recommendedMaintenanceAction = "Clamp and replace rail plug"
        ),
        InspectionRecord(
            id = "INSP-2026-0899",
            section = "Southern Freight Bypass (Sector 2)",
            date = "Yesterday, 14:15",
            inspector = "M. Taylor (ID: #2104)",
            status = "Completed · Verified",
            framesCount = 5120,
            detectionsCount = 1,
            detectedCrackTitle = "Gauge Corner Shelling",
            recommendedMaintenanceAction = "Grind rail surface"
        )
    )

    val initialObservations = listOf(
        ObservationItem("OBS-101", "Minor Rail Corrugation", "KM 41+200", "09:12", "Medium", Tone.WARNING),
        ObservationItem("OBS-102", "Missing Fishplate Bolt", "KM 42+050", "09:44", "High", Tone.CRITICAL),
        ObservationItem("OBS-103", "Ballast Settlement Under Sleeper", "KM 42+180", "10:02", "High", Tone.CRITICAL),
        ObservationItem("OBS-104", "Vegetation Infringement Clear Zone", "KM 43+110", "10:35", "Low", Tone.INFO)
    )

    val initialNotifications = listOf(
        NotificationItem(
            id = "NOTIF-01",
            title = "Critical Defect Alert",
            message = "New transverse head crack verified on Sector 4B at KM 42+180.",
            time = "10m ago",
            tone = Tone.CRITICAL
        ),
        NotificationItem(
            id = "NOTIF-02",
            title = "Central Safety Cloud Backend",
            message = "Connected to Central Realtime Database and telemetry sync engine.",
            time = "25m ago",
            tone = Tone.HEALTHY
        ),
        NotificationItem(
            id = "NOTIF-03",
            title = "Shift Inspection Ready",
            message = "North Mainline safety report compiled and ready for sign-off.",
            time = "1h ago",
            tone = Tone.INFO
        )
    )
}
