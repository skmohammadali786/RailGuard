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
import com.example.railguard.model.Defect
import com.example.railguard.model.MaintenanceTask
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun HomeScreen(
    defects: List<Defect>,
    tasks: List<MaintenanceTask>,
    inspectorName: String = "E. Chen",
    onNavigate: (String) -> Unit,
    onDefectClick: (Defect) -> Unit,
    onTaskClick: (MaintenanceTask) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "North corridor patrol",
                subtitle = "Shift 1 · $inspectorName · On duty",
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

        // Live Connected Train Telemetry Banner
        item {
            TrainConnectionStatusBanner(
                onOpenTrainConnection = { onNavigate("train_connection") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            FirebaseSyncBanner(
                onOpenFirebaseSync = { onNavigate("firebase_sync") }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Shift Status Card
        item {
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onNavigate("live_inspection") }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(label = "PATROL IN PROGRESS", tone = Tone.INFO)
                    Text(
                        text = "14:00 - 18:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Section 14 · North Loop Line",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Chainage 14+000 to 15+250 · 60E1 Rail Profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Track Coordinates: 51°30'14.2\"N 0°07'42.8\"W · LRS 14+320 UP",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.primary
                )
                Text(
                    text = "Patrol Timestamp: 2024-06-18 08:42:15 UTC (38m elapsed)",
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
                        text = "AI Engine: RailVision-DeepTrack v4.2",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "92.4% Conf · Risk 0.68",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.error
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
                            text = "61%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { 0.61f },
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
                        text = "Active Restriction: 25 km/h at 14+320 (CRK-2048)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = toneColor(Tone.CRITICAL, isDark)
                    )
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
                    value = "18",
                    label = "Open defects",
                    tone = Tone.CRITICAL,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("defects") }
                )
                MetricTile(
                    value = "06",
                    label = "Due today",
                    tone = Tone.WARNING,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("maintenance") }
                )
                MetricTile(
                    value = "94.2%",
                    label = "Track health",
                    tone = Tone.HEALTHY,
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
                    title = "Train Uplink",
                    subtitle = "Live ETCS cab",
                    icon = Icons.Default.Train,
                    onClick = { onNavigate("train_connection") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Live Scan",
                    subtitle = "Camera HUD",
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
                    subtitle = "Field log (12)",
                    icon = Icons.Default.Visibility,
                    onClick = { onNavigate("all_observations") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Track Health",
                    subtitle = "94.2% condition",
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
                Text(
                    text = "92.4% CONF",
                    color = Color(0xFF38BDF8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "⚡ CRK-2048 (46 mm) propagation: +0.41 mm/day under 2,400t freight traffic. Critical 50 mm rail break threshold estimated in 72 hours. Recommended TSR 25 km/h active.",
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

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

            OutlinedButton(
                onClick = {
                    if (defects.isNotEmpty()) onDefectClick(defects.first())
                },
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
                Text("Inspect CRK-2048", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TrainConnectionStatusBanner(
    onOpenTrainConnection: () -> Unit
) {
    val isDark = LocalIsDark.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF0F1E2E) else Color(0xFFE0F2FE))
            .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable { onOpenTrainConnection() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsTransit,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TRAIN FLEET INTERLOCK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) Color.White else Color(0xFF0369A1)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF16A34A))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "4 ACTIVE CABS",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "TR-104 (118 km/h) · FR-802 · HS-301 · CR-512 | TSR Auto-Dispatch Ready",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open Train Fleet Interlock",
            tint = Color(0xFF0284C7),
            modifier = Modifier.size(16.dp)
        )
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

@Composable
fun FirebaseSyncBanner(
    onOpenFirebaseSync: () -> Unit
) {
    val isDark = LocalIsDark.current
    val firebaseService = remember { com.example.railguard.data.RailGuardFirebaseService.instance }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
            .clickable { onOpenFirebaseSync() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA000).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "CENTRAL SAFETY CLOUD SYNC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (firebaseService.isConnectedToFirebase) Color(0xFF16A34A) else Color(0xFF64748B))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (firebaseService.isConnectedToFirebase) "SYNCED" else "CLOUD READY",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Central Realtime Database · Tap to view live cloud records",
                fontSize = 10.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open Central Cloud Sync",
            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
        )
    }
}

