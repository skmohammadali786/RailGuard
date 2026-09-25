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
    val initialDefects = emptyList<Defect>()
    val initialTasks = emptyList<MaintenanceTask>()
    val initialInspections = emptyList<InspectionRecord>()
    val initialObservations = emptyList<ObservationItem>()
    val initialNotifications = emptyList<NotificationItem>()
}
