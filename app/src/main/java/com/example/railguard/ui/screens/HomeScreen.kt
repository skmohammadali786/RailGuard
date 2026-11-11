package com.example.railguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.data.RailGuardFirebaseService
import com.example.railguard.model.Defect
import com.example.railguard.model.MaintenanceTask
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun HomeScreen(
    defects: List<Defect>,
    tasks: List<MaintenanceTask>,
    telemetry: RailGuardFirebaseService.LiveSensorTelemetry?,
    inspectorName: String = "Field Inspector",
    onNavigate: (String) -> Unit,
    onDefectClick: (Defect) -> Unit,
    onTaskClick: (MaintenanceTask) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current
    val settings = com.example.railguard.model.LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val fbService = com.example.railguard.data.RailGuardFirebaseService.instance
    val effectiveTelemetry = fbService.latestSensorTelemetry ?: telemetry
    val gatewayOnline = effectiveTelemetry != null &&
        (effectiveTelemetry.timestamp == 0L || System.currentTimeMillis() - effectiveTelemetry.timestamp < 30_000L)
    var showWifiDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Rail Corridor Operations",
                subtitle = "Active Shift · $inspectorName · Telemetry Monitoring",
                isHome = true,
                onNotificationClick = { onNavigate("notifications") }
            )
        }



        // Dedicated AI Predictive Safety Engine - Compact Summary
        item {
            DedicatedHomepageAiOracle(
                defects = defects,
                tasks = tasks,
                onNavigate = onNavigate,
                onDefectClick = onDefectClick,
                onTaskClick = onTaskClick
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Shift Status Card
        item {
            val topDefect = defects.firstOrNull()
            val criticalDefect = defects.firstOrNull { it.tone == Tone.CRITICAL }
            val isDataStreaming = gatewayOnline || fbService.isDirectWifiConnected ||
                (effectiveTelemetry != null && effectiveTelemetry.chainage != "Unknown" && effectiveTelemetry.chainage != "Awaiting Chainage")

            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onNavigate("live_inspection") }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = when {
                            criticalDefect != null -> settings.translate("critical_attention")
                            isDataStreaming -> settings.translate("patrol_in_progress")
                            else -> settings.translate("standby_awaiting_sensors")
                        },
                        tone = when {
                            criticalDefect != null -> Tone.CRITICAL
                            isDataStreaming -> Tone.INFO
                            else -> Tone.NEUTRAL
                        }
                    )
                    Text(
                        text = settings.translate("active_shift"),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = topDefect?.section ?: effectiveTelemetry?.chainage?.takeIf { it != "Unknown" }
                        ?: "Awaiting corridor telemetry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = when {
                        topDefect != null -> "Chainage ${topDefect.chainageCoordinate} · Track defect record"
                        effectiveTelemetry != null -> "Chainage ${effectiveTelemetry.chainage} · Gateway packet received"
                        else -> "No Raspberry Pi packet received"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (topDefect != null && (topDefect.latitude != 0.0 || topDefect.longitude != 0.0)) {
                        "Track Coordinates: ${topDefect.latitude}°N ${topDefect.longitude}°E"
                    } else {
                        "Track Coordinates: ${effectiveTelemetry?.chainage ?: "Unknown"}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.primary
                )
                Text(
                    text = if (gatewayOnline) {
                        "Gateway: ${effectiveTelemetry?.nodeId} · ${effectiveTelemetry?.status}"
                    } else {
                        "Gateway: awaiting telemetry stream"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // AI Engine Live Summary Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Engine: RailVision-DeepTrack",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (topDefect != null) {
                            "${topDefect.aiConfidencePercent}% Conf · Risk ${topDefect.score}"
                        } else {
                            "Awaiting analysis"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (topDefect != null) colorScheme.error else colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Corridor scan progress",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (gatewayOnline) "Live" else "Waiting",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                            progress = { if (gatewayOnline) 1f else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = colorScheme.primary,
                        trackColor = colorScheme.outline.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active speed restriction banner
                if (criticalDefect != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(toneColor(Tone.CRITICAL, isDark).copy(alpha = 0.1f))
                            .border(1.dp, toneColor(Tone.CRITICAL, isDark).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = toneColor(Tone.CRITICAL, isDark),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active Restriction: 25 km/h at ${criticalDefect.chainageCoordinate} (${criticalDefect.id})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = toneColor(Tone.CRITICAL, isDark)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF16A34A).copy(alpha = 0.1f))
                            .border(1.dp, Color(0xFF16A34A).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (gatewayOnline) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (gatewayOnline) Color(0xFF16A34A) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (gatewayOnline) {
                                    "Gateway connected: ${effectiveTelemetry?.status}"
                                } else {
                                    "No current gateway telemetry"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (gatewayOnline) Color(0xFF16A34A) else Color(0xFF64748B)
                            )
                        }

                        val fbService = com.example.railguard.data.RailGuardFirebaseService.instance
                        if (fbService.isDirectWifiConnected) {
                            Text(
                                text = "Wi-Fi Direct (${fbService.directWifiLatencyMs}ms)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Live Hardware & Sensor Telemetry Hub (No separate screen needed)
        item {
            val scope = rememberCoroutineScope()
            var tsrFeedback by remember { mutableStateOf<String?>(null) }
            val isStale = effectiveTelemetry != null && effectiveTelemetry.timestamp > 0L && (System.currentTimeMillis() - effectiveTelemetry.timestamp > 30_000L)
            val isAnySensorActive = fbService.isDirectWifiConnected || gatewayOnline || effectiveTelemetry != null ||
                (fbService.hardwareHealth?.let { it.cameraOnline || it.ultrasonicOnline || it.vibrationOnline || it.temperatureOnline || it.gpsOnline } == true)

            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = settings.translate("live_track_telemetry"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF0284C7)
                            )
                        }
                        StatusPill(
                            label = if (isAnySensorActive) {
                                if (isStale) "STALE" else settings.translate("online")
                            } else settings.translate("offline"),
                            tone = if (isAnySensorActive) {
                                if (isStale) Tone.WARNING else Tone.HEALTHY
                            } else Tone.NEUTRAL
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-Sensor Live Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            value = effectiveTelemetry?.let { "${it.ultrasonicDepthMm} mm" } ?: "Awaiting sensor",
                            label = settings.translate("ultrasonic_depth"),
                            tone = if (effectiveTelemetry != null && (effectiveTelemetry.hasCrack || effectiveTelemetry.ultrasonicDepthMm in 10.0..55.0)) Tone.CRITICAL else Tone.HEALTHY,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            value = effectiveTelemetry?.let { "${it.vibrationG} g" } ?: "Awaiting sensor",
                            label = settings.translate("vibration_shock"),
                            tone = if (effectiveTelemetry != null && (effectiveTelemetry.vibrationG > 0.35 || effectiveTelemetry.hasCrack)) Tone.CRITICAL else if (effectiveTelemetry != null && effectiveTelemetry.vibrationG > 0.15) Tone.WARNING else Tone.HEALTHY,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            value = effectiveTelemetry?.let { "${it.railTempC} °C" } ?: "Awaiting sensor",
                            label = settings.translate("rail_temp"),
                            tone = if (effectiveTelemetry != null && effectiveTelemetry.railTempC > 45.0) Tone.WARNING else Tone.HEALTHY,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            value = effectiveTelemetry?.let { "${it.axleSpeedKmh} km/h" } ?: "Awaiting sensor",
                            label = settings.translate("axle_speed"),
                            tone = if (effectiveTelemetry != null && effectiveTelemetry.axleSpeedKmh > 80.0) Tone.WARNING else Tone.HEALTHY,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick TSR Interlock Dispatch
                    val activeTsr = com.example.railguard.data.RailGuardFirebaseService.instance.activeTsrSpeed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(settings.translate("tsr_speed_order"), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurfaceVariant)
                            Text(
                                text = "${settings.translate("active_limit")}: ${activeTsr ?: 25} km/h",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if ((activeTsr ?: 25) <= 25) Color(0xFFDC2626) else Color(0xFF0284C7)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(25, 60, 120).forEach { speed ->
                                val isSelected = activeTsr == speed
                                if (isSelected) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val fb = com.example.railguard.data.RailGuardFirebaseService.instance
                                                fb.dispatchTsrToFirebase(
                                                    trainId = effectiveTelemetry?.nodeId ?: "RPI-TRACK-01",
                                                    tsrSpeedKmH = speed,
                                                    reason = "Operator speed order: $speed km/h"
                                                )
                                                fb.issueEmergencyTsr(speedKmh = speed)
                                                tsrFeedback = "TSR $speed km/h active (Pi Hardware + Cloud Interlock) ✓"
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (speed == 25) Color(0xFFDC2626) else Color(0xFF0284C7)
                                        )
                                    ) {
                                        Text("$speed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                val fb = com.example.railguard.data.RailGuardFirebaseService.instance
                                                fb.dispatchTsrToFirebase(
                                                    trainId = effectiveTelemetry?.nodeId ?: "RPI-TRACK-01",
                                                    tsrSpeedKmH = speed,
                                                    reason = "Operator speed order: $speed km/h"
                                                )
                                                fb.issueEmergencyTsr(speedKmh = speed)
                                                tsrFeedback = "TSR $speed km/h sent to hardware ✓"
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (speed == 25) Color(0xFFDC2626) else Color(0xFF0284C7)
                                        )
                                    ) {
                                        Text("$speed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (tsrFeedback != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(tsrFeedback ?: "", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }

                    val fbService = com.example.railguard.data.RailGuardFirebaseService.instance
                    val hw = fbService.hardwareHealth
                    if (hw != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Camera: ${hw.cameraFps} FPS · UT: 5MHz · Battery: ${hw.batteryPercent}%",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
                            )
                            Text(
                                text = "Pi Node: RPI-TRACK-01",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pi Hardware Track Buzzer Status & Control Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (fbService.isPiBuzzerActive) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (fbService.isPiBuzzerActive) Color(0xFFF87171) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (fbService.isPiBuzzerActive) Color(0xFFDC2626) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (fbService.isPiBuzzerActive) "Trackside Buzzer: ALARM ACTIVE" else "Trackside Buzzer: READY / IDLE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (fbService.isPiBuzzerActive) Color(0xFFDC2626) else Color(0xFF475569)
                                    )
                                    Text(
                                        text = if (fbService.isPiBuzzerActive) "Audible alarm sounding at trackside & phone" else "Armed: rings immediately when anomaly detected",
                                        fontSize = 10.sp,
                                        color = if (fbService.isPiBuzzerActive) Color(0xFF991B1B) else Color(0xFF64748B)
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        fbService.triggerPiBuzzer(ring = !fbService.isPiBuzzerActive, durationSeconds = 6)
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (fbService.isPiBuzzerActive) Color(0xFFDC2626) else Color(0xFF0284C7))
                            ) {
                                Text(
                                    text = if (fbService.isPiBuzzerActive) "Silence Buzzer" else "Trigger Buzzer",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fbService.isPiBuzzerActive) Color(0xFFDC2626) else Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Network Pulse
        item {
            val isDirectWifiConnected = com.example.railguard.data.RailGuardFirebaseService.instance.isDirectWifiConnected
            val liveTrackHealth: Pair<String, Tone> = remember(defects, effectiveTelemetry, isDirectWifiConnected) {
                if (!isDirectWifiConnected && defects.isEmpty() && (effectiveTelemetry == null || (effectiveTelemetry.vibrationG == 0.0f && effectiveTelemetry.ultrasonicDepthMm == 0.0f))) {
                    Pair("Awaiting Scan", Tone.NEUTRAL)
                } else {
                    var score = 98.4
                    defects.forEach { d ->
                        when (d.tone) {
                            Tone.CRITICAL -> score -= 18.0
                            Tone.WARNING -> score -= 8.0
                            else -> score -= 4.0
                        }
                    }
                    val vib = effectiveTelemetry?.vibrationG?.toDouble() ?: 0.0
                    if (vib > 1.2) {
                        score -= minOf(15.0, (vib - 1.2) * 8.0)
                    }
                    val ut = effectiveTelemetry?.ultrasonicDepthMm?.toDouble() ?: 0.0
                    if (ut in 1.0..58.0) {
                        score -= minOf(20.0, (58.0 - ut) * 2.0)
                    }
                    val temp = effectiveTelemetry?.railTempC?.toDouble() ?: 24.0
                    if (temp > 45.0 || temp < -5.0) {
                        score -= 5.0
                    }
                    val clamped = score.coerceIn(25.0, 99.2)
                    val formatted = String.format(java.util.Locale.US, "%.1f%%", clamped)
                    val tone = when {
                        clamped >= 85.0 -> Tone.HEALTHY
                        clamped >= 65.0 -> Tone.WARNING
                        else -> Tone.CRITICAL
                    }
                    Pair(formatted, tone)
                }
            }

            SectionLabel(
                title = "NETWORK PULSE",
                action = "View all",
                onAction = { onNavigate("defects") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "${defects.size}",
                    label = "Open defects",
                    tone = if (defects.isNotEmpty()) Tone.CRITICAL else Tone.HEALTHY,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("defects") }
                )
                MetricTile(
                    value = "${tasks.count { !it.status.contains("Completed") }}",
                    label = "Active tasks",
                    tone = if (tasks.any { !it.status.contains("Completed") }) Tone.WARNING else Tone.HEALTHY,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("maintenance") }
                )
                MetricTile(
                    value = liveTrackHealth.first,
                    label = "Track health",
                    tone = liveTrackHealth.second,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("track_health") }
                )
            }
        }

        // Active Railway Line Map Preview - Compact
        item {
            SectionLabel(
                title = "CORRIDOR SCHEMATIC",
                action = "Open full map",
                onAction = { onNavigate("map") }
            )
            RealTimeTrainLineMap(
                compact = true,
                showMarkers = true,
                defects = defects,
                onMarkerClick = { onNavigate("defect_map") }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigate("map") },
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Interactive Corridor Map", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = { onNavigate("defect_map") },
                    modifier = Modifier.weight(1f).height(30.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Defect Pins & GPS", fontSize = 10.sp)
                }
            }
        }

        // Requires Attention
        item {
            SectionLabel(
                title = "REQUIRES ATTENTION",
                action = "View all",
                onAction = { onNavigate("attention") }
            )

            // Top Defect
            if (defects.isNotEmpty()) {
                val defect = defects.first()
                ListRow(
                    icon = Icons.Default.Warning,
                    title = defect.title,
                    subtitle = "${defect.id} · ${defect.section} · ${defect.time}",
                    trailing = "Risk ${defect.score}",
                    tone = defect.tone,
                    onClick = { onDefectClick(defect) }
                )
            }

            // Top Task
            if (tasks.isNotEmpty()) {
                val task = tasks.first()
                ListRow(
                    icon = Icons.Default.Build,
                    title = task.title,
                    subtitle = "${task.id} · ${task.section} · ${task.due}",
                    trailing = task.assignee,
                    tone = task.tone,
                    onClick = { onTaskClick(task) }
                )
            }

            if (defects.isEmpty() && tasks.isEmpty()) {
                RailCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "All Track Sectors Clear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Zero pending alerts. Real-time telemetry from Raspberry Pi will appear here automatically.",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Workspace Grid - Dedicated screens for every button
        item {
            SectionLabel(title = "WORKSPACE MODULES")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "AI Oracle",
                    subtitle = "Tensor kinetics",
                    icon = Icons.Default.Psychology,
                    onClick = { onNavigate("ai_oracle") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Field Camera",
                    subtitle = "Optical HUD",
                    icon = Icons.Default.PhotoCamera,
                    onClick = { onNavigate("camera") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Live Scan",
                    subtitle = "Realtime HUD",
                    icon = Icons.Default.CameraAlt,
                    onClick = { onNavigate("live_inspection") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "New Run",
                    subtitle = "Plan inspection",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onNavigate("inspection_setup") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Crack Gauge",
                    subtitle = "Growth analysis",
                    icon = Icons.Default.ShowChart,
                    onClick = { onNavigate("growth_analysis") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Evidence Pack",
                    subtitle = "SHA-256 PDF",
                    icon = Icons.Default.Description,
                    onClick = { onNavigate("evidence_package") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Risk Heatmap",
                    subtitle = "Hazard density",
                    icon = Icons.Default.Layers,
                    onClick = { onNavigate("risk_heatmap") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Corridor GIS",
                    subtitle = "Chainage map",
                    icon = Icons.Default.Map,
                    onClick = { onNavigate("map") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Defect Map",
                    subtitle = "GPS Geo pins",
                    icon = Icons.Default.LocationOn,
                    onClick = { onNavigate("defect_map") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = settings.translate("audit_signoff"),
                    subtitle = settings.translate("engineer_seal"),
                    icon = Icons.Default.VerifiedUser,
                    onClick = { onNavigate("engineer_verification") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = settings.translate("live_patrol"),
                    subtitle = settings.translate("camera_sweep"),
                    icon = Icons.Default.Sensors,
                    onClick = { onNavigate("live_inspection") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = settings.translate("track_health_title"),
                    subtitle = if (defects.isEmpty()) "100% Optimal" else "${maxOf(60, 100 - defects.size * 5)}% Condition",
                    icon = Icons.Default.HealthAndSafety,
                    onClick = { onNavigate("track_health") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showWifiDialog) {
        RaspberryPiWifiDialog(
            onDismiss = { showWifiDialog = false }
        )
    }
}

@Composable
fun DedicatedHomepageAiOracle(
    defects: List<Defect>,
    tasks: List<MaintenanceTask>,
    onNavigate: (String) -> Unit,
    onDefectClick: (Defect) -> Unit,
    onTaskClick: (MaintenanceTask) -> Unit
) {
    val isDark = LocalIsDark.current

    val infiniteTransition = rememberInfiniteTransition(label = "AiPulse")
    val aiGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AiGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0F9FF))
            .border(1.dp, Color(0xFF0284C7).copy(alpha = aiGlow), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // AI Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0284C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Oracle",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RAILGUARD AI ORACLE",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Real-time Tensor Kinetics & Failure Window",
                        color = Color(0xFF0284C7),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFBAE6FD))
                    .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                val conf = if (defects.isNotEmpty()) "${defects.first().aiConfidencePercent}% CONF" else "AI ACTIVE"
                Text(
                    text = conf,
                    color = Color(0xFF0369A1),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val topDefect = defects.firstOrNull()
        if (topDefect != null) {
            Text(
                text = "⚡ ${topDefect.id} (${topDefect.estimatedLength}) at ${topDefect.chainageCoordinate}. Action: ${topDefect.aiPrescribedAction}",
                color = Color(0xFF1E293B),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                text = "⚡ Real-time tensor kinetics active. No active structural track anomalies detected. Sensor gateway online and monitoring corridor.",
                color = Color(0xFF334155),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onNavigate("ai_oracle") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open AI Oracle →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (topDefect != null) {
                OutlinedButton(
                    onClick = { onDefectClick(topDefect) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect ${topDefect.id}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(
                    onClick = { onNavigate("live_inspection") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Live Track Scan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun WorkspaceTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

