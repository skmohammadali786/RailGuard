package com.example.railguard.model

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
    val estimatedLength: String = "46 mm",
    val trackSide: String = "Up line",
    val riskScore: Int = 92,
    // Track Coordinates & Geolocation
    val gpsCoordinates: String = "51°30'14.2\"N 0°07'42.8\"W",
    val latitude: Double = 51.50394,
    val longitude: Double = -0.12856,
    val altitudeMeters: Double = 48.2,
    val chainageCoordinate: String = "14+320 (Up Line)",
    val tieSleeperNumber: String = "Sleeper #14-320",
    val trackGaugeMm: Double = 1438.2,
    // Inspection Timestamps
    val detectedTimestamp: String = "2024-06-18 08:54:32 UTC",
    val lastAuditedTimestamp: String = "2024-06-18 09:20:27 UTC",
    // AI Analysis Engine Results
    val aiConfidencePercent: Double = 92.4,
    val aiEngineModel: String = "RailVision-DeepTrack-v4.2",
    val aiDerailmentRiskIndex: Double = 0.68,
    val aiGrowthRateMmPerDay: Double = 0.71,
    val aiPredictedFailureDays: Int = 11,
    val aiRecommendedSpeedLimitKmH: Int = 25,
    val aiPrescribedAction: String = "Impose 25 km/h limit; replace 6m rail segment before Day 11."
)

data class MaintenanceTask(
    val id: String,
    val title: String,
    val section: String,
    val due: String,
    val tone: Tone,
    val assignee: String,
    val status: String = "Open",
    val beforeDesc: String = "Loose rail clip pair at sleeper 14+320 with excessive lateral play.",
    val afterDesc: String = "Replacement e-clip pair seated and torqued to 220 Nm.",
    val torque: String = "220 Nm"
)

data class InspectionRecord(
    val id: String,
    val section: String,
    val startChainage: String,
    val endChainage: String,
    val scheduledAt: String,
    val inspector: String,
    val captureProfile: String,
    val notes: String,
    val status: String,
    val createdAt: String,
    val framesCount: Int = 42,
    val detectionsCount: Int = 3,
    // Inspection Timestamps
    val startTimestamp: String = "2024-06-18 08:42:15 UTC",
    val completionTimestamp: String = "2024-06-18 09:20:27 UTC",
    val durationFormatted: String = "38 min 12 sec",
    val lastSyncedTimestamp: String = "2024-06-18 09:21:04 UTC",
    // Track Coordinates & LRS Geolocation
    val startGps: String = "51°30'08.4\"N 0°07'31.2\"W",
    val endGps: String = "51°30'28.1\"N 0°07'58.4\"W",
    val elevationAmsl: String = "48.2 m AMSL",
    val trackCorridorLrs: String = "North Loop Line · Up Line (Track 1)",
    val railProfile: String = "60E1 (UIC 60) Continuous Welded Rail",
    // AI Analysis Engine Results
    val aiModelName: String = "RailVision-DeepTrack-v4.2",
    val aiConfidenceScore: String = "92.4%",
    val aiDerailmentRiskScore: Double = 0.68,
    val aiCriticalFindingsCount: Int = 1,
    val aiWarningFindingsCount: Int = 1,
    val aiInferenceLatencyMs: Int = 16,
    val aiVerdictSummary: String = "CRITICAL: Gauge corner fracture identified at 14+320. Derailment risk index 0.68. Mandatory 25 km/h restriction enacted."
)

data class Observation(
    val id: String,
    val title: String,
    val meta: String,
    val tone: Tone,
    val length: String = "",
    val date: String = ""
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val tone: Tone,
    val read: Boolean = false
)

object RailDataRepository {
    val initialDefects = listOf(
        Defect(
            id = "CRK-2048",
            title = "Gauge corner crack",
            section = "North Loop · 14+320",
            score = "92",
            tone = Tone.CRITICAL,
            time = "12 min ago",
            detail = "Longitudinal surface crack at the gauge corner. AI confidence 92%. Immediate 25 km/h speed restriction active.",
            estimatedLength = "46 mm",
            trackSide = "Up line",
            riskScore = 92
        ),
        Defect(
            id = "CRK-2044",
            title = "Head check wear",
            section = "North Loop · 14+108",
            score = "74",
            tone = Tone.WARNING,
            time = "Yesterday",
            detail = "Progressive head check with visible polishing. Monitor growth on the next inspection cycle.",
            estimatedLength = "28 mm",
            trackSide = "Up line",
            riskScore = 74
        ),
        Defect(
            id = "CRK-2037",
            title = "Fastener shadow anomaly",
            section = "South Yard · 08+902",
            score = "61",
            tone = Tone.WARNING,
            time = "Jun 17",
            detail = "Possible loose fastening detected beside sleeper 08+902. Field confirmation required.",
            estimatedLength = "12 mm",
            trackSide = "Down line",
            riskScore = 61
        )
    )

    val initialTasks = listOf(
        MaintenanceTask(
            id = "MT-881",
            title = "Replace rail clip pair",
            section = "North Loop · 14+320",
            due = "Due today",
            tone = Tone.CRITICAL,
            assignee = "M. Alvarez"
        ),
        MaintenanceTask(
            id = "MT-878",
            title = "Grind head check",
            section = "East Junction · 03+660",
            due = "Due Jun 22",
            tone = Tone.WARNING,
            assignee = "J. Patel"
        ),
        MaintenanceTask(
            id = "MT-864",
            title = "Torque verification",
            section = "South Yard · 08+902",
            due = "Due Jun 24",
            tone = Tone.HEALTHY,
            assignee = "S. Morgan"
        )
    )

    val initialInspections = listOf(
        InspectionRecord(
            id = "INSP-240618-04",
            section = "North Loop · Section 14",
            startChainage = "14+000",
            endChainage = "15+250",
            scheduledAt = "2024-06-18 08:42",
            inspector = "E. Chen",
            captureProfile = "Visual + thermal",
            notes = "Live corridor patrol pass",
            status = "In progress",
            createdAt = "2024-06-18T08:42:00Z",
            framesCount = 42,
            detectionsCount = 3
        ),
        InspectionRecord(
            id = "INSP-240616-03",
            section = "East Junction · Section 03",
            startChainage = "03+000",
            endChainage = "04+200",
            scheduledAt = "2024-06-16 07:15",
            inspector = "J. Patel",
            captureProfile = "Visual 4K",
            notes = "Routine scheduled track verification",
            status = "Completed",
            createdAt = "2024-06-16T07:15:00Z",
            framesCount = 58,
            detectionsCount = 1
        ),
        InspectionRecord(
            id = "INSP-240619-01",
            section = "South Yard · Section 08",
            startChainage = "08+000",
            endChainage = "09+600",
            scheduledAt = "2024-06-19 13:30",
            inspector = "S. Morgan",
            captureProfile = "Thermal + acoustic",
            notes = "Switch and crossing geometry inspection",
            status = "Planned",
            createdAt = "2024-06-17T11:00:00Z",
            framesCount = 0,
            detectionsCount = 0
        )
    )

    val initialObservations = listOf(
        Observation("OBS-482", "Gauge corner crack", "CRK-2048 · North Loop 14+320 · 12 min ago", Tone.CRITICAL, "46 mm", "18 Jun"),
        Observation("OBS-481", "Head check wear", "CRK-2044 · North Loop 14+108 · Yesterday", Tone.WARNING, "28 mm", "17 Jun"),
        Observation("OBS-479", "Fastener shadow anomaly", "CRK-2037 · South Yard 08+902 · Jun 17", Tone.WARNING, "12 mm", "17 Jun"),
        Observation("OBS-476", "Rail profile within tolerance", "INSP-240616-03 · East Junction · Jun 16", Tone.HEALTHY, "Nominal", "16 Jun")
    )

    val initialNotifications = listOf(
        NotificationItem("N1", "Critical Defect CRK-2048", "Gauge corner crack detected at North Loop 14+320. Speed restricted to 25 km/h.", "12 min ago", Tone.CRITICAL),
        NotificationItem("N2", "Maintenance Assigned", "Task MT-881 assigned to M. Alvarez. Work window 14:00-15:00 today.", "35 min ago", Tone.WARNING),
        NotificationItem("N3", "Inspection INSP-240618-04", "Visual and GPS trace synchronized with local storage. 42 frames logged.", "1 hour ago", Tone.HEALTHY),
        NotificationItem("N4", "Weekly Safety Report Published", "North corridor weekly assessment ready for download and signature.", "Yesterday", Tone.INFO)
    )
}
