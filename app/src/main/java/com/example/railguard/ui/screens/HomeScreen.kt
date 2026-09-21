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
                subtitle = "Shift 1 · E. Chen · On duty",
                isHome = true,
                onNotificationClick = { onNavigate("notifications") }
            )
        }

        // Dedicated AI Predictive Safety Engine
        item {
            DedicatedHomepageAiOracle(
                defects = defects,
                tasks = tasks,
                onNavigate = onNavigate,
                onDefectClick = onDefectClick,
                onTaskClick = onTaskClick
            )
            Spacer(modifier = Modifier.height(12.dp))
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

        // Active Railway Line Map Preview
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

        // Quick Workspace Grid
        item {
            SectionLabel(title = "WORKSPACE MODULES")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Live Scan",
                    subtitle = "Camera HUD",
                    icon = Icons.Default.CameraAlt,
                    onClick = { onNavigate("live_inspection") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "New Run",
                    subtitle = "Plan inspection",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onNavigate("inspection_setup") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Crack Analysis",
                    subtitle = "Growth gauge",
                    icon = Icons.Default.ShowChart,
                    onClick = { onNavigate("growth_analysis") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Evidence Pack",
                    subtitle = "SHA-256 PDF",
                    icon = Icons.Default.Description,
                    onClick = { onNavigate("evidence_package") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Heatmap",
                    subtitle = "Risk zones",
                    icon = Icons.Default.Layers,
                    onClick = { onNavigate("risk_heatmap") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Sign-Off",
                    subtitle = "Audit verify",
                    icon = Icons.Default.VerifiedUser,
                    onClick = { onNavigate("engineer_verification") },
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
    val colorScheme = MaterialTheme.colorScheme
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

    var queryText by remember { mutableStateOf("") }
    var selectedPredictionType by remember { mutableStateOf<String?>("failure_time") }
    var isExpandedDetails by remember { mutableStateOf(false) }
    var aiAnalysisText by remember {
        mutableStateOf(
            "⚡ AI PREDICTION FOR SECTION 14:\n" +
            "Defect CRK-2048 (46 mm) propagation velocity is +0.41 mm/day under current 2,400t freight traffic. " +
            "Critical 50 mm rail break threshold is estimated in 72 hours. Derailment risk is 84.7% if speed exceeds 35 km/h. " +
            "Optimal maintenance window identified tonight 01:15 - 04:30. Immediate work order MT-881 dispatch strongly advised."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF0F1B2B) else Color(0xFF132338))
            .border(1.dp, Color(0xFF0284C7).copy(alpha = aiGlow), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        // AI Header Bar - Compact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0284C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Oracle",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
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
                        text = "Live track telemetry connected",
                        color = Color(0xFF7DD3FC),
                        fontSize = 8.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE AI",
                        color = Color(0xFF38BDF8),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { isExpandedDetails = !isExpandedDetails },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpandedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle AI details",
                        tint = Color(0xFF7DD3FC),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Prediction Chips - Compact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple("failure_time", "💥 Failure Time", "CRK-2048 46mm breakdown curve"),
                Triple("derailment_risk", "⚠️ Derailment Risk", "Speed vs Curvature analysis"),
                Triple("maint_window", "🕒 Work Window", "Zero-traffic midnight slot"),
                Triple("thermal_stress", "🌡️ Thermal Buckle", "+36°C afternoon forecast")
            ).forEach { (key, label, desc) ->
                val isSelected = selectedPredictionType == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E2D42))
                        .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(6.dp))
                        .clickable {
                            selectedPredictionType = key
                            aiAnalysisText = when (key) {
                                "failure_time" ->
                                    "⚡ AI PREDICTION FOR SECTION 14:\n" +
                                    "Defect CRK-2048 (46 mm) propagation velocity is +0.41 mm/day under current 2,400t freight traffic. " +
                                    "Critical 50 mm rail break threshold is estimated in 72 hours. Derailment risk is 84.7% if speed exceeds 35 km/h. " +
                                    "Optimal maintenance window identified tonight 01:15 - 04:30. Immediate work order MT-881 dispatch strongly advised."
                                "derailment_risk" ->
                                    "⚠️ AI DERAILMENT RISK MATRIX:\n" +
                                    "Chainage 14+320 curvature (300m radius) combined with 46 mm gauge crack creates severe wheel flange climb risk. " +
                                    "• Speed at 40 km/h: 96.2% derailment probability.\n" +
                                    "• Speed at 25 km/h: 12.4% safe envelope (CURRENT RESTRICTION).\n" +
                                    "• Recommendation: Maintain 25 km/h limit until clamp installed."
                                "maint_window" ->
                                    "🕒 AI TIMETABLE OPTIMIZATION:\n" +
                                    "Analysis of North Loop timetable shows complete traffic blackout between 01:15 and 04:30 tomorrow. " +
                                    "Work order MT-881 (Replace Rail Clip Pair & Clamp) requires estimated 85 minutes. " +
                                    "Crew 04 can deploy at 01:30 with 0% impact on morning passenger services."
                                else ->
                                    "🌡️ AI THERMAL EXPANSION FORECAST:\n" +
                                    "Tomorrow ambient temperature is forecast to reach 34°C with track rail temperature exceeding 52°C at 14:00. " +
                                    "Compressive longitudinal stress between km 14+100 and 14+450 will exceed 128 MPa. " +
                                    "High risk of track misalignment buckle. Recommend thermal destressing patrol before 11:30."
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // AI Response Container - Compact
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A131F))
                .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSESSMENT & PREDICTION",
                        color = Color(0xFF38BDF8),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Confidence: 97.8%",
                        color = Color(0xFF22C55E),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = aiAnalysisText,
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = if (isExpandedDetails) Int.MAX_VALUE else 3,
                    fontFamily = FontFamily.Default
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action shortcuts suggested by AI - Compact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            if (defects.isNotEmpty()) onDefectClick(defects.first())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("Inspect CRK-2048", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (tasks.isNotEmpty()) onTaskClick(tasks.first())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("Dispatch MT-881", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Custom AI Query Input Field - Compact
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                placeholder = {
                    Text(
                        text = "Ask AI: e.g. Safest speed for North Loop?",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0A131F),
                    unfocusedContainerColor = Color(0xFF0A131F),
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    if (queryText.isNotBlank()) {
                        aiAnalysisText = "🤖 AI RESPONSE TO: \"$queryText\"\n" +
                            "Analysis for Corridor Section 14 (North Loop Line):\n" +
                            "• Defect Register: 1 Critical (CRK-2048 46mm), 1 Warning (CRK-2044 28mm).\n" +
                            "• Predicted Failure Time: 72 hours without maintenance.\n" +
                            "• Recommendation: Enforce 25 km/h restriction, execute work order MT-881 tonight.\n" +
                            "• Safety Confidence: 98.2% based on accelerometer trace & optical gauge."
                        queryText = ""
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0284C7))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Query",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
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
