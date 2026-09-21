package com.example.railguard.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.railguard.components.*
import com.example.railguard.model.InspectionRecord
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun InspectionsListScreen(
    inspections: List<InspectionRecord>,
    onSelectInspection: (InspectionRecord) -> Unit,
    onStartNew: () -> Unit,
    onViewCalendar: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGps: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    var filterIndex by remember { mutableIntStateOf(0) }
    val filterLabels = listOf("All", "In progress", "Planned", "Completed")

    val filteredInspections = remember(filterIndex, inspections) {
        when (filterIndex) {
            1 -> inspections.filter { it.status == "In progress" }
            2 -> inspections.filter { it.status == "Planned" }
            3 -> inspections.filter { it.status == "Completed" }
            else -> inspections
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Inspections Hub",
                subtitle = "Active patrols, scheduled sweeps, and vision feeds",
                onBack = onBack
            )
        }

        // Dedicated Primary Workspace Actions (3 Dedicated Modes + Camera + GPS)
        item {
            SectionLabel(title = "INSPECTION WORKSPACES")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Live Scan",
                    subtitle = "Real-time AI HUD",
                    icon = Icons.Default.PlayArrow,
                    onClick = onStartNew,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "New Run",
                    subtitle = "Plan route & bounds",
                    icon = Icons.Default.AddCircle,
                    onClick = onStartNew,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Calendar",
                    subtitle = "Shift sweeps",
                    icon = Icons.Default.CalendarMonth,
                    onClick = onViewCalendar,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Camera HUD",
                    subtitle = "Optical 4K calibrator",
                    icon = Icons.Default.CameraAlt,
                    onClick = onOpenCamera,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Field GPS",
                    subtitle = "RTK satellite telemetry",
                    icon = Icons.Default.GpsFixed,
                    onClick = onOpenGps,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionLabel(title = "CORRIDOR INSPECTION LOG")
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterLabels.forEachIndexed { index, label ->
                    val isSelected = filterIndex == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) colorScheme.primary else colorScheme.surface)
                            .border(
                                1.dp,
                                if (isSelected) colorScheme.primary else colorScheme.outline,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { filterIndex = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(filteredInspections) { insp ->
            val statusTone = when (insp.status) {
                "In progress" -> Tone.INFO
                "Completed" -> Tone.HEALTHY
                else -> Tone.NEUTRAL
            }

            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onSelectInspection(insp) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = insp.id,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    StatusPill(label = insp.status.uppercase(), tone = statusTone)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = insp.section,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "LRS Track: ${insp.startChainage} → ${insp.endChainage} (${insp.trackCorridorLrs})",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Coordinates: ${insp.startGps} · Alt: ${insp.elevationAmsl}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // AI Analysis Engine Summary Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (insp.detectionsCount > 0)
                                toneColor(Tone.CRITICAL, LocalIsDark.current).copy(alpha = 0.10f)
                            else
                                colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI: ${insp.aiModelName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (insp.detectionsCount > 0) toneColor(Tone.CRITICAL, LocalIsDark.current) else colorScheme.onSurface
                    )
                    Text(
                        text = "Conf: ${insp.aiConfidenceScore} · Risk ${insp.aiDerailmentRiskScore}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (insp.detectionsCount > 0) toneColor(Tone.CRITICAL, LocalIsDark.current) else colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Timestamp: ${insp.startTimestamp.take(16)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${insp.framesCount} frames · ${insp.detectionsCount} findings",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (insp.detectionsCount > 0) toneColor(Tone.CRITICAL, LocalIsDark.current) else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InspectionSetupScreen(
    onStartPatrol: (InspectionRecord, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var section by remember { mutableStateOf("North Loop · Section 14") }
    var startChainage by remember { mutableStateOf("14+000") }
    var endChainage by remember { mutableStateOf("15+250") }
    var startGps by remember { mutableStateOf("51°30'08.4\"N 0°07'31.2\"W") }
    var endGps by remember { mutableStateOf("51°30'28.1\"N 0°07'58.4\"W") }
    var scheduledTimestamp by remember { mutableStateOf("2024-06-18 08:42:00 UTC") }
    var captureProfile by remember { mutableStateOf("Visual + thermal (4K)") }
    var inspector by remember { mutableStateOf("E. Chen") }
    var notes by remember { mutableStateOf("Pre-shift routine inspection pass with ultrasonic check.") }
    var gpsFilled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Inspection Setup",
                subtitle = "Configure route, chainage interval, and capture profile",
                onBack = onBack
            )
        }

        // Quick GPS & Timestamp fill
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIVE FIELD TELEMETRY SYNC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Text(
                            text = if (gpsFilled) "RTK GPS Coordinates & UTC Clock Synced" else "Acquire current sensor GPS & UTC timestamp",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            startGps = "51°30'14.2\"N 0°07'42.8\"W"
                            endGps = "51°30'32.6\"N 0°08'02.1\"W"
                            val nowTime = "2024-06-18 " + String.format("%02d:%02d:%02d UTC", (8..17).random(), (10..59).random(), (10..59).random())
                            scheduledTimestamp = nowTime
                            gpsFilled = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gpsFilled) toneColor(Tone.HEALTHY, LocalIsDark.current) else colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (gpsFilled) "Locked ✓" else "Lock GPS", fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "CORRIDOR PARAMETERS & TRACK COORDINATES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Corridor Line & Section") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startChainage,
                        onValueChange = { startChainage = it },
                        label = { Text("Start Chainage") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endChainage,
                        onValueChange = { endChainage = it },
                        label = { Text("End Chainage") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Track Coordinates GIS Inputs
                OutlinedTextField(
                    value = startGps,
                    onValueChange = { startGps = it },
                    label = { Text("Start Track Coordinates (GIS GPS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endGps,
                    onValueChange = { endGps = it },
                    label = { Text("End Track Coordinates (GIS GPS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Inspection Timestamp Input
                OutlinedTextField(
                    value = scheduledTimestamp,
                    onValueChange = { scheduledTimestamp = it },
                    label = { Text("Inspection Scheduled Timestamp (UTC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = captureProfile,
                    onValueChange = { captureProfile = it },
                    label = { Text("AI Sensor Profile") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inspector,
                    onValueChange = { inspector = it },
                    label = { Text("Qualified Inspector") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Operational Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            PrimaryButton(
                title = "Save & Show Inspection Details",
                icon = Icons.Default.CheckCircle,
                onClick = {
                    val record = InspectionRecord(
                        id = "INSP-${System.currentTimeMillis() % 100000}",
                        section = section,
                        startChainage = startChainage,
                        endChainage = endChainage,
                        scheduledAt = scheduledTimestamp,
                        inspector = inspector,
                        captureProfile = captureProfile,
                        notes = notes,
                        status = "Planned",
                        createdAt = scheduledTimestamp,
                        framesCount = 0,
                        detectionsCount = 0,
                        startTimestamp = scheduledTimestamp,
                        completionTimestamp = "Pending",
                        durationFormatted = "0 min",
                        startGps = startGps,
                        endGps = endGps,
                        elevationAmsl = "48.2 m AMSL",
                        trackCorridorLrs = "$section · Up Line (Track 1)",
                        railProfile = "60E1 (UIC 60) Continuous Welded Rail",
                        aiModelName = "RailVision-DeepTrack-v4.2",
                        aiConfidenceScore = "92.4%",
                        aiDerailmentRiskScore = 0.68,
                        aiCriticalFindingsCount = 1,
                        aiWarningFindingsCount = 1,
                        aiInferenceLatencyMs = 16,
                        aiVerdictSummary = "Planned pass: Multi-spectral inspection scheduled across $startChainage to $endChainage."
                    )
                    onStartPatrol(record, false)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val record = InspectionRecord(
                        id = "INSP-${System.currentTimeMillis() % 100000}",
                        section = section,
                        startChainage = startChainage,
                        endChainage = endChainage,
                        scheduledAt = scheduledTimestamp,
                        inspector = inspector,
                        captureProfile = captureProfile,
                        notes = notes,
                        status = "In progress",
                        createdAt = scheduledTimestamp,
                        framesCount = 42,
                        detectionsCount = 1,
                        startTimestamp = scheduledTimestamp,
                        completionTimestamp = "Live",
                        durationFormatted = "In progress",
                        startGps = startGps,
                        endGps = endGps,
                        elevationAmsl = "48.2 m AMSL",
                        trackCorridorLrs = "$section · Up Line (Track 1)",
                        railProfile = "60E1 (UIC 60) Continuous Welded Rail",
                        aiModelName = "RailVision-DeepTrack-v4.2",
                        aiConfidenceScore = "92.4%",
                        aiDerailmentRiskScore = 0.68,
                        aiCriticalFindingsCount = 1,
                        aiWarningFindingsCount = 1,
                        aiInferenceLatencyMs = 16,
                        aiVerdictSummary = "CRITICAL DEFECT DETECTED: Gauge corner crack at 14+320. Derailment risk index 0.68. Mandatory 25 km/h restriction active."
                    )
                    onStartPatrol(record, true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Begin Live Patrol Run",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LiveInspectionScreen(
    onEndInspection: () -> Unit,
    onDefectDetected: () -> Unit,
    onOpenGps: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current
    var secondsElapsed by remember { mutableIntStateOf(48) }
    var frameCount by remember { mutableIntStateOf(42) }
    var defectCount by remember { mutableIntStateOf(1) }
    var flashActive by remember { mutableStateOf(false) }
    var isReanalyzing by remember { mutableStateOf(false) }
    var aiConfidence by remember { mutableFloatStateOf(92.4f) }
    var aiAnalysisMessage by remember { mutableStateOf("RailVision DeepTrack v4.2 active · 16ms latency") }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            secondsElapsed++
        }
    }

    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val timerString = String.format("%02d:%02d", minutes, seconds)
    val liveUtcClock = String.format("2024-06-18 08:%02d:%02d UTC", 42 + minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F14))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top HUD bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit HUD",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Red.copy(alpha = 0.2f))
                        .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REC $timerString",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = liveUtcClock,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(onClick = { onOpenGps() }) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GPS info",
                    tint = Color.White
                )
            }
        }

        // Live Camera Viewfinder HUD Simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141C24))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            // Simulated Rail View track lines
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .offset(x = (-40).dp)
                    .background(Color.White.copy(alpha = 0.4f))
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .offset(x = 40.dp)
                    .background(Color.White.copy(alpha = 0.4f))
            )

            // Cross ties
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(6) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            }

            // Top HUD Overlay: Track Coordinates & Geolocation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TRACK: NORTH LOOP · 14+320 UP",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "SLEEPER #14-320",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GIS: 51°30'14.2\"N 0°07'42.8\"W",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "ALT: 48.2m · RTK LOCK ±1.2m",
                            color = Color(0xFF4ADE80),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Simulated AI Bounding Box around defect
            Box(
                modifier = Modifier
                    .size(140.dp, 80.dp)
                    .align(Alignment.Center)
                    .offset(x = (-20).dp, y = (-15).dp)
                    .border(2.dp, toneColor(Tone.CRITICAL, true), RoundedCornerShape(4.dp))
                    .background(toneColor(Tone.CRITICAL, true).copy(alpha = 0.15f))
                    .clickable { onDefectDetected() }
                    .padding(6.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CRK-2048",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${String.format("%.1f", aiConfidence)}%",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "14+320 · GAUGE CORNER",
                        color = toneColor(Tone.CRITICAL, true),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RISK 0.68 · 25 km/h LIMIT",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Bottom HUD Overlay: AI Analysis Engine live readout & re-scan
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI ENGINE: RailVision v4.2",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = aiAnalysisMessage,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Interactive Re-scan button
                    Button(
                        onClick = {
                            isReanalyzing = true
                            aiConfidence = 94.8f
                            aiAnalysisMessage = "Deep multi-spectral tensor pass confirmed: 94.8%"
                            isReanalyzing = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Re-Scan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SPEED: 11.8 km/h · GAUGE: 1438.2mm",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TS: 08:54:32 UTC",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Bottom Action Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { flashActive = !flashActive },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (flashActive) Color.Yellow else Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (flashActive) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (flashActive) Color.Black else Color.White
                        )
                    }
                    Text("Torch", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }

                // Main Shutter / Capture Frame Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            frameCount++
                        }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onEndInspection() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Red)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Finish",
                            tint = Color.White
                        )
                    }
                    Text("End Run", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$frameCount frames logged · $defectCount critical defect highlighted · UTC Synced",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun InspectionDetailsScreen(
    inspection: InspectionRecord,
    onOpenLive: () -> Unit,
    onOpenSummary: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isRerunningAi by remember { mutableStateOf(false) }
    var currentAiConfidence by remember { mutableStateOf(inspection.aiConfidenceScore) }
    var currentAiRisk by remember { mutableDoubleStateOf(inspection.aiDerailmentRiskScore) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = inspection.id,
                subtitle = inspection.section,
                onBack = onBack
            )
        }

        // Primary Record Card
        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "INSPECTION RECORD",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        label = inspection.status.uppercase(),
                        tone = if (inspection.status == "In progress") Tone.INFO else Tone.HEALTHY
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = inspection.section,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                DetailRow(label = "Interval", value = "${inspection.startChainage} → ${inspection.endChainage}")
                DetailRow(label = "Inspector", value = inspection.inspector)
                DetailRow(label = "Profile", value = inspection.captureProfile)
                DetailRow(label = "Frames", value = "${inspection.framesCount} captured")
                DetailRow(label = "Detections", value = "${inspection.detectionsCount} findings flagged")
                DetailRow(label = "Notes", value = inspection.notes)
            }
        }

        // Dedicated Card: Inspection Timestamps
        item {
            Spacer(modifier = Modifier.height(8.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSPECTION TIMESTAMPS & TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Scheduled Timestamp", value = inspection.scheduledAt)
                DetailRow(label = "Patrol Start Timestamp", value = inspection.startTimestamp)
                DetailRow(label = "Defect CRK-2048 Tagged", value = "2024-06-18 08:54:32 UTC (14+320)")
                DetailRow(label = "Checkpoint 15+250 Cleared", value = "2024-06-18 09:18:44 UTC")
                DetailRow(label = "Patrol Completion", value = inspection.completionTimestamp)
                DetailRow(label = "Elapsed Patrol Duration", value = inspection.durationFormatted)
                DetailRow(label = "Cloud Synchronization", value = inspection.lastSyncedTimestamp)
            }
        }

        // Dedicated Card: Track Coordinates & Geolocation
        item {
            Spacer(modifier = Modifier.height(8.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRACK COORDINATES & GEOLOCATION (LRS & GIS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "LRS Track Corridor", value = inspection.trackCorridorLrs)
                DetailRow(label = "Start GIS Coordinates", value = "${inspection.startGps} (KM ${inspection.startChainage})")
                DetailRow(label = "Critical Defect Coordinate", value = "51°30'14.2\"N 0°07'42.8\"W (KM 14+320)")
                DetailRow(label = "Sleeper / Tie Number", value = "Tie #14-320 (Left Rail Gauge Face)")
                DetailRow(label = "End GIS Coordinates", value = "${inspection.endGps} (KM ${inspection.endChainage})")
                DetailRow(label = "Track Elevation (AMSL)", value = inspection.elevationAmsl)
                DetailRow(label = "Continuous Rail Profile", value = inspection.railProfile)
                DetailRow(label = "RTK Positioning Precision", value = "±1.2 meters (Dual Constellation)")
            }
        }

        // Dedicated Card: AI Analysis Engine Results
        item {
            Spacer(modifier = Modifier.height(8.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI ANALYSIS ENGINE RESULTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        label = "CONF: $currentAiConfidence",
                        tone = Tone.CRITICAL
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "AI Engine Model", value = inspection.aiModelName)
                DetailRow(label = "Inference Processing Core", value = "${inspection.aiInferenceLatencyMs} ms · Edge Tensor TPU")
                DetailRow(label = "Derailment Risk (Nadal Ratio)", value = "$currentAiRisk / 1.00 (Critical Limit: 0.80)")
                DetailRow(label = "Primary Defect Confidence", value = "$currentAiConfidence (CRK-2048 Gauge Crack)")
                DetailRow(label = "Secondary Finding Confidence", value = "88.1% (CRK-2044 Head Check)")
                DetailRow(label = "Fastener Anomaly Confidence", value = "76.5% (Loose Elastic Clip)")
                DetailRow(label = "Crack Growth Rate Prediction", value = "0.71 mm/day (Warning Threshold: 0.15 mm/day)")
                DetailRow(label = "Time to Transverse Rupture", value = "11 days remaining under full axle load")
                DetailRow(label = "AI Operational Verdict", value = inspection.aiVerdictSummary)

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Re-run AI Analysis Button
                OutlinedButton(
                    onClick = {
                        isRerunningAi = true
                        currentAiConfidence = "94.8%"
                        currentAiRisk = 0.69
                        isRerunningAi = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRerunningAi) "Analyzing Sensor Tensors..." else "Re-Run AI Analysis Engine", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Action Buttons
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (inspection.status == "In progress") {
                    PrimaryButton(
                        title = "Resume HUD",
                        icon = Icons.Default.CameraAlt,
                        onClick = onOpenLive,
                        modifier = Modifier.weight(1f)
                    )
                }
                PrimaryButton(
                    title = "View Summary",
                    icon = Icons.Default.CheckCircle,
                    secondary = inspection.status == "In progress",
                    onClick = onOpenSummary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InspectionSummaryScreen(
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Inspection Summary",
                subtitle = "Pass finalized · Verification saved to local store",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(toneColor(Tone.HEALTHY, LocalIsDark.current).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = toneColor(Tone.HEALTHY, LocalIsDark.current)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "INSP-240618-04 Complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Audited by E. Chen · SHA-256 verified",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                DetailRow(label = "Inspection Start Timestamp", value = "2024-06-18 08:42:15 UTC")
                DetailRow(label = "Completion Timestamp", value = "2024-06-18 09:20:27 UTC")
                DetailRow(label = "Duration", value = "38 min 12 sec")
                DetailRow(label = "LRS Track Corridor", value = "North Loop Line (Section 14)")
                DetailRow(label = "Start Track Coordinates", value = "51°30'08.4\"N 0°07'31.2\"W (14+000)")
                DetailRow(label = "Defect Track Coordinates", value = "51°30'14.2\"N 0°07'42.8\"W (14+320)")
                DetailRow(label = "End Track Coordinates", value = "51°30'28.1\"N 0°07'58.4\"W (15+250)")
                DetailRow(label = "Distance Inspected", value = "1,250 meters")
                DetailRow(label = "Frames Logged", value = "42 frames (Optical 4K + Thermal)")
                DetailRow(label = "AI Analysis Model", value = "RailVision-DeepTrack v4.2")
                DetailRow(label = "AI Confidence Score", value = "92.4% (Critical finding)")
                DetailRow(label = "AI Derailment Risk Index", value = "0.68 / 1.00")
                DetailRow(label = "AI Operational Action", value = "25 km/h restriction active · Replace 6m rail")
                DetailRow(label = "Cryptographic Seal", value = "SHA256: 9b2d8f...4e1c (Verified)")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = "Save & Return to Dashboard",
                icon = Icons.Default.Home,
                onClick = onDone
            )
        }
    }
}

@Composable
fun InspectionCalendarScreen(
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val days = listOf("Mon 17", "Tue 18", "Wed 19", "Thu 20", "Fri 21")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Shift Schedule",
                subtitle = "Scheduled corridor sweeps and assigned inspectors",
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { i, d ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (i == 1) colorScheme.primary else colorScheme.surface)
                            .border(1.dp, if (i == 1) colorScheme.primary else colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = d,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (i == 1) colorScheme.onPrimary else colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            SectionLabel(title = "SCHEDULED RUNS FOR TODAY")
            ListRow(
                icon = Icons.Default.PlayArrow,
                title = "INSP-240618-04 · North Loop Line",
                subtitle = "14:00 - 18:00 · E. Chen · Section 14",
                trailing = "Live",
                tone = Tone.INFO
            )
            ListRow(
                icon = Icons.Default.Schedule,
                title = "INSP-240618-05 · South Yard Switch",
                subtitle = "19:00 - 21:00 · S. Morgan · Section 08",
                trailing = "Planned",
                tone = Tone.NEUTRAL
            )
        }
    }
}

@Composable
fun GpsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Field GPS Telemetry",
                subtitle = "Live satellite position and chainage correlation",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "SATELLITE POSITION",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Latitude", value = "51°30'14.2\" N")
                DetailRow(label = "Longitude", value = "0°07'42.8\" W")
                DetailRow(label = "Altitude", value = "48.2 m AMSL")
                DetailRow(label = "Correlated Chainage", value = "14+320 Up Line")
                DetailRow(label = "Accuracy", value = "±1.2 meters (RTK Lock)")
                DetailRow(label = "Satellites", value = "14 in fix (GPS + Galileo)")
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasPermission) {
                PrimaryButton(
                    title = "Request Location Permission",
                    icon = Icons.Default.GpsFixed,
                    onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )
            } else {
                StatusPill(label = "SYSTEM GPS ACCESS GRANTED", tone = Tone.HEALTHY)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
    }
}

@Composable
fun CameraScreen(
    onCaptureDefect: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val colorScheme = MaterialTheme.colorScheme
    var flashMode by remember { mutableStateOf(false) }
    var isoValue by remember { mutableStateOf("ISO 200") }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var frameCount by remember { mutableIntStateOf(184) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Optical Camera HUD",
                subtitle = "4K 60FPS rail profiling sensor with real-time AI bounding",
                onBack = onBack
            )
        }

        item {
            // Viewfinder Simulated HUD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, Color(0xFF0284C7), RoundedCornerShape(12.dp))
            ) {
                // Background Track Grid
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Telemetry Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "REC [4K UHD · 60fps]",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "EXP: 1/1200 · $isoValue · ${String.format("%.1fx", zoomLevel)}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Crosshair & Rail Profile Guide
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .align(Alignment.CenterHorizontally)
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterCenterFocus,
                            contentDescription = "Target Crosshair",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "60E1 PROFILE ALIGNED",
                            color = Color(0xFF22C55E),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                        )
                    }

                    // Bottom Chainage Stamp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHAINAGE: 14+320 UP",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "FRAMES: $frameCount",
                            color = Color(0xFF7DD3FC),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))

            // Camera Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { flashMode = !flashMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (flashMode) Color(0xFFF59E0B) else colorScheme.surfaceVariant,
                        contentColor = if (flashMode) Color.Black else colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(
                        imageVector = if (flashMode) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (flashMode) "Torch ON" else "Torch OFF", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        zoomLevel = if (zoomLevel == 1.0f) 2.0f else if (zoomLevel == 2.0f) 5.0f else 1.0f
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${zoomLevel.toInt()}x Zoom", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryButton(
                title = "Capture Frame & Analyze Finding",
                icon = Icons.Default.CameraAlt,
                onClick = {
                    frameCount++
                    onCaptureDefect()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!hasCameraPermission) {
                OutlinedButton(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Hardware Camera Permission")
                }
            }
        }
    }
}
