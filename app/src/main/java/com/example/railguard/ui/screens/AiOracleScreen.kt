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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun AiOracleScreen(
    defects: List<Defect>,
    tasks: List<MaintenanceTask>,
    onInspectDefect: (Defect) -> Unit,
    onDispatchTask: (MaintenanceTask) -> Unit,
    onNavigateCrackGrowth: () -> Unit,
    onNavigateHeatmap: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Failure Time", "Derailment Risk", "Work Windows", "Thermal Buckle")

    var queryText by remember { mutableStateOf("") }
    var isRunningTensorScan by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var lastTensorScanResult by remember { mutableStateOf("RailVision-DeepTrack v4.2 · Multi-spectral pass synced with track sensors") }

    // Live AI query chat history
    val chatMessages = remember {
        mutableStateListOf(
            Pair("assistant", "Hello Inspector Chen. RailVision-DeepTrack v4.2 is online. Connected to Section 14 track telemetry and Train TR-104 live uplink. Ask me anything regarding track safety, derailment thresholds, or maintenance windows.")
        )
    }

    LaunchedEffect(isRunningTensorScan) {
        if (isRunningTensorScan) {
            scanProgress = 0f
            for (i in 1..10) {
                kotlinx.coroutines.delay(120)
                scanProgress = i / 10f
            }
            isRunningTensorScan = false
            lastTensorScanResult = "Tensor inference completed: CRK-2048 verified at 46.2mm depth (93.1% confidence). Nadal derailment ratio confirmed at 0.68. TSR 25 km/h restriction active."
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
                title = "AI Predictive Safety Engine",
                subtitle = "RailVision-DeepTrack v4.2 · Multi-Tensor Analytics",
                onBack = onBack
            )
        }

        // Engine Status Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F1B2B) else Color(0xFF132338)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0284C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "AI Oracle",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "RAILVISION-DEEPTRACK v4.2",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Edge TPU Accelerated · Latency: 16 ms",
                                    color = Color(0xFF7DD3FC),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(4.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                color = Color(0xFF4ADE80),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0B1420))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("DERAILMENT RISK", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("0.68 / 1.00", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0B1420))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("CONFIDENCE", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("97.8%", color = Color(0xFF22C55E), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0B1420))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("SECTOR", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("Section 14", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tensor Re-Scan Action
                    if (isRunningTensorScan) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Running Neural Tensor Re-Scan...", color = Color(0xFF38BDF8), fontSize = 10.sp)
                                Text("${(scanProgress * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lastTensorScanResult,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { isRunningTensorScan = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-Scan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Analytical Models Tabs
        item {
            SectionLabel(title = "PREDICTIVE RISK MODELS")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSel = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) colorScheme.primary else colorScheme.surfaceVariant)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Selected Predictive Model Detail
        item {
            when (selectedTab) {
                0 -> {
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CRACK KINETICS & FAILURE TIME",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "CRITICAL 72H", tone = Tone.CRITICAL)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Defect CRK-2048 (Chainage 14+320 UP Line)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Current Crack Depth: 46 mm (92% through railhead)\n" +
                                "• Growth Velocity: +0.41 mm/day under 2,400t freight axle loads\n" +
                                "• Critical Rail Break Threshold: 50 mm (Estimated in 72 hours)\n" +
                                "• Paris-Erdogan Equation: da/dN = 2.4e-11 * (ΔK)^3.2\n" +
                                "• Recommendation: Immediate rail clamp installation + 25 km/h restriction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PrimaryButton(
                                title = "Inspect CRK-2048",
                                icon = Icons.Default.Warning,
                                onClick = {
                                    if (defects.isNotEmpty()) onInspectDefect(defects.first())
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PrimaryButton(
                                title = "Growth Analytics",
                                icon = Icons.Default.ShowChart,
                                secondary = true,
                                onClick = onNavigateCrackGrowth,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                1 -> {
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DERAILMENT RISK MATRIX (NADAL LIMIT)",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "HIGH RISK", tone = Tone.CRITICAL)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chainage 14+320 Curvature (300m Radius)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Wheel flange climb risk calculated via Nadal Derailment Criteria (Y/Q):\n" +
                                "• Speed @ 45 km/h: 96.2% derailment probability (DANGEROUS)\n" +
                                "• Speed @ 35 km/h: 48.7% flange climb potential\n" +
                                "• Speed @ 25 km/h: 12.4% safe envelope (ACTIVE RESTRICTION ENFORCED)\n" +
                                "• Wheel / Rail Angle of Attack: 1.42° with dynamic lateral gauge variance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PrimaryButton(
                            title = "View Spatial Risk Heatmap",
                            icon = Icons.Default.Layers,
                            onClick = onNavigateHeatmap
                        )
                    }
                }
                2 -> {
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TIMETABLE & MAINTENANCE BLACKOUT",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "OPTIMAL SLOT", tone = Tone.HEALTHY)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "North Loop Corridor Window: 01:15 - 04:30 GMT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Traffic Blackout: 3 hours 15 minutes without passenger interference.\n" +
                                "• Work Order MT-881: Replace Rail Clip Pair & Emergency Fishplate Clamp\n" +
                                "• Estimated Repair Time: 85 minutes\n" +
                                "• Available Crew: Crew 04 (Lead: M. Ross) ready for dispatch\n" +
                                "• Service Disruption: 0% morning peak delay impact.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PrimaryButton(
                            title = "Dispatch Work Order MT-881",
                            icon = Icons.Default.Build,
                            onClick = {
                                if (tasks.isNotEmpty()) onDispatchTask(tasks.first())
                            }
                        )
                    }
                }
                else -> {
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "THERMAL STRESS & CWR BUCKLE EVALUATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "ELEVATED", tone = Tone.WARNING)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Continuous Welded Rail (60E1 CWR)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Tomorrow Ambient Forecast: 34°C (Rail surface projected > 52°C)\n" +
                                "• Stress-Free Neutral Temp (SFT): 27°C\n" +
                                "• Compressive Stress at 14:00: 128 MPa between KM 14+100 and 14+450\n" +
                                "• Lateral Ballast Resistance: 4.8 kN/sleeper (Slightly degraded at 14+380)\n" +
                                "• Recommendation: Deploy destressing crew prior to 11:30.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Interactive Natural Language AI Assistant Chat
        item {
            SectionLabel(title = "INTERACTIVE AI FIELD ASSISTANT")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0B1420) else Color(0xFFF1F5F9)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    // Chat messages history
                    chatMessages.forEach { (sender, text) ->
                        val isUser = sender == "user"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isUser) colorScheme.primary else (if (isDark) Color(0xFF1E293B) else Color.White))
                                    .border(1.dp, if (isUser) colorScheme.primary else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = if (isUser) colorScheme.onPrimary else colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Suggested Prompts Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Safest speed for Section 14?",
                            "When will CRK-2048 break?",
                            "Work window tonight?",
                            "Train TR-104 status?"
                        ).forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.12f))
                                    .border(1.dp, colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        chatMessages.add(Pair("user", suggestion))
                                        val reply = when {
                                            suggestion.contains("speed", ignoreCase = true) ->
                                                "⚡ Under current 46mm transverse crack conditions, max permissible operating speed is strictly 25 km/h. Exceeding 35 km/h escalates wheel climb derailment risk to 48.7%."
                                            suggestion.contains("break", ignoreCase = true) ->
                                                "💥 CRK-2048 will reach the 50mm critical railhead rupture limit in approximately 72 hours under 2,400t freight passes without clamp reinforcement."
                                            suggestion.contains("window", ignoreCase = true) ->
                                                "🕒 North Loop is completely dark to commercial traffic between 01:15 and 04:30 GMT tonight. Ideal for Work Order MT-881 (85 mins duration)."
                                            else ->
                                                "🚆 Train TR-104 is actively connected via 5G uplink at IP 10.142.8.50. Current speed: 118 km/h. Speed restriction alert has been transmitted to the cab console."
                                        }
                                        chatMessages.add(Pair("assistant", reply))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 10.sp,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            placeholder = { Text("Ask RailGuard AI...", fontSize = 11.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (queryText.isNotBlank()) {
                                    val userMsg = queryText
                                    chatMessages.add(Pair("user", userMsg))
                                    queryText = ""
                                    chatMessages.add(
                                        Pair(
                                            "assistant",
                                            "🤖 AI Evaluation for '$userMsg':\n" +
                                                "• Section 14 track health is 94.2%.\n" +
                                                "• 1 Critical defect active (CRK-2048 46mm at 14+320).\n" +
                                                "• Safety confidence is 97.8% based on optical tensor & live accelerometer telemetry.\n" +
                                                "• Temporary Speed Restriction of 25 km/h remains in effect."
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
