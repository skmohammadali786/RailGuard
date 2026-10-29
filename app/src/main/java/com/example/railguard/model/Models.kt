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
    val estimatedLength: String = "Unknown",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val riskScore: Int = 0,
    val chainageCoordinate: String = "Unknown",
    val aiConfidencePercent: Int = 0,
    val aiPrescribedAction: String = "No cloud recommendation available",
    val flawType: String = "Transverse Gauge Corner Fissure",
    val ultrasonicDepthMm: Float = 46.2f,
    val vibrationG: Float = 0.425f,
    val railTempC: Float = 32.8f,
    val speedKmh: Float = 45.0f,
    val boundingBox: String = "x:140,y:88,w:120,h:45"
)

data class CrackAlertData(
    val defectId: String,
    val flawType: String,
    val chainage: String,
    val section: String,
    val confidence: Int,
    val crackLength: String,
    val ultrasonicDepthMm: Float,
    val vibrationG: Float,
    val railTempC: Float,
    val speedKmh: Float,
    val recommendedTsrKmh: Float,
    val coordinates: String,
    val action: String,
    val boundingBox: String,
    val timestamp: Long
)


data class MaintenanceTask(
    val id: String,
    val title: String,
    val section: String,
    val due: String,
    val tone: Tone,
    val assignee: String,
    val status: String = "Pending",
    val torque: String = "Unknown"
)

data class InspectionRecord(
    val id: String,
    val section: String,
    val date: String = "Today",
    val inspector: String,
    val status: String,
    val framesCount: Int = 0,
    val detectionsCount: Int = 3,
    val detectedCrackTitle: String = "No cloud detection data",
    val recommendedMaintenanceAction: String = "No cloud recommendation available"
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

data class PatrolSchedule(
    val id: String,
    val section: String,
    val date: String,
    val time: String,
    val shift: String,
    val inspector: String,
    val patrolType: String,
    val frequency: String = "Once",
    val status: String = "Scheduled",
    val notes: String = ""
)

data class HardwareHealthStatus(
    val status: String = "OPERATIONAL",
    val cameraOnline: Boolean = true,
    val cameraSensor: String = "Sony IMX708 (Camera Module 3)",
    val cameraFps: Int = 30,
    val ultrasonicOnline: Boolean = true,
    val ultrasonicSensor: String = "Piezo UT Dual 5MHz",
    val vibrationOnline: Boolean = true,
    val vibrationSensor: String = "ADXL345 Tri-axial (+/-16g)",
    val temperatureOnline: Boolean = true,
    val temperatureSensor: String = "PT100 RTD High-Precision Probe",
    val gpsOnline: Boolean = true,
    val gpsSensor: String = "NEO-M9N RTK-DGPS Locked (1cm precision)",
    val batteryPercent: Int = 94,
    val batteryVoltage: Float = 12.4f,
    val cpuTempC: Float = 42.8f,
    val uptimeSeconds: Long = 0L,
    val latencyMs: Long = 14L
)

data class AiOracleData(
    val parisDeltaK: Float = 4.2f,
    val parisGrowthRate: String = "0.02 um/k-cycle",
    val nadalRatio: Float = 0.35f,
    val derailmentRisk: String = "SAFE",
    val thermalStressMpa: Float = 10.8f,
    val neutralTempDelta: Float = 4.5f,
    val fatigueCyclesRemaining: Int = 850000,
    val timestamp: Long = 0L
)

// ==========================================
// RAIL DATA REPOSITORY
// ==========================================

object RailDataRepository {
    val initialDefects = emptyList<Defect>()
    val initialTasks = emptyList<MaintenanceTask>()
    val initialInspections = emptyList<InspectionRecord>()
    val initialObservations = emptyList<ObservationItem>()
    val initialNotifications = emptyList<NotificationItem>()
    val initialSchedules = emptyList<PatrolSchedule>()
}
