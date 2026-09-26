package com.example.railguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
    val gatewayOnline = telemetry != null &&
        (telemetry.timestamp == 0L || System.currentTimeMillis() - telemetry.timestamp < 30_000L)

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
                        label = if (criticalDefect != null) "CRITICAL ATTENTION" else "PATROL IN PROGRESS",
                        tone = if (criticalDefect != null) Tone.CRITICAL else Tone.INFO
                    )
                    Text(
                        text = "Active Shift",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = topDefect?.section ?: telemetry?.chainage?.takeIf { it != "Unknown" }
                        ?: "Awaiting corridor telemetry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = when {
                        topDefect != null -> "Chainage ${topDefect.chainageCoordinate} · Cloud defect record"
                        telemetry != null -> "Chainage ${telemetry.chainage} · Gateway packet received"
                        else -> "No Raspberry Pi packet received"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (topDefect != null && (topDefect.latitude != 0.0 || topDefect.longitude != 0.0)) {
                        "Track Coordinates: ${topDefect.latitude}°N ${topDefect.longitude}°E"
                    } else {
                        "Track Coordinates: ${telemetry?.chainage ?: "Unknown"}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.primary
                )
                Text(
                    text = if (gatewayOnline) {
                        "Gateway: ${telemetry?.nodeId} · ${telemetry?.status}"
                    } else {
                        "Gateway: waiting for a current Firebase packet"
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
                            "No cloud analysis available"
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (gatewayOnline) {
                                "Gateway connected: ${telemetry?.status}"
                            } else {
                                "No current gateway telemetry"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
        }

        // Network Pulse
        item {
            SectionLabel(
                title = "NETWORK PULSE",
                action = "Live telemetry",
                onAction = { onNavigate("analytics") }
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
                    value = "${tasks.size}",
                    label = "Due tasks",
                    tone = if (tasks.isNotEmpty()) Tone.WARNING else Tone.HEALTHY,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("maintenance") }
                )
                MetricTile(
                    value = if (defects.isEmpty()) "100%" else "${maxOf(60, 100 - defects.size * 5)}%",
                    label = "Track health",
                    tone = if (defects.isEmpty()) Tone.HEALTHY else Tone.WARNING,
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
                    title = "Audit Sign-Off",
                    subtitle = "Engineer seal",
                    icon = Icons.Default.VerifiedUser,
                    onClick = { onNavigate("engineer_verification") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Observations",
                    subtitle = "Field log",
                    icon = Icons.Default.Visibility,
                    onClick = { onNavigate("all_observations") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Track Health",
                    subtitle = if (defects.isEmpty()) "100% Nominal" else "${maxOf(60, 100 - defects.size * 5)}% Condition",
                    icon = Icons.Default.HealthAndSafety,
                    onClick = { onNavigate("track_health") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
            .background(if (isDark) Color(0xFF0F1B2B) else Color(0xFF132338))
            .border(1.dp, Color(0xFF0284C7).copy(alpha = aiGlow), RoundedCornerShape(12.dp))
            .padding(12.dp)
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
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0284C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Oracle",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RAILGUARD AI ORACLE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Real-time Tensor Kinetics & Failure Window",
                        color = Color(0xFF7DD3FC),
                        fontSize = 9.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                val conf = if (defects.isNotEmpty()) "${defects.first().aiConfidencePercent}% CONF" else "ACTIVE CONF"
                Text(
                    text = conf,
                    color = Color(0xFF38BDF8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val topDefect = defects.firstOrNull()
        if (topDefect != null) {
            Text(
                text = "⚡ ${topDefect.id} (${topDefect.estimatedLength}) at ${topDefect.chainageCoordinate}. Action: ${topDefect.aiPrescribedAction}",
                color = Color(0xFFE2E8F0),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        } else {
            Text(
                text = "⚡ Real-time tensor kinetics active. No active structural track anomalies detected. Awaiting live sensor telemetry and camera frames from Raspberry Pi / ESP32.",
                color = Color(0xFFE2E8F0),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                    .height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open Full AI Oracle →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (topDefect != null) {
                OutlinedButton(
                    onClick = { onDefectClick(topDefect) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect ${topDefect.id}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(
                    onClick = { onNavigate("live_inspection") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF38BDF8)
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

