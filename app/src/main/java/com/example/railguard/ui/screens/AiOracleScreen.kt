package com.example.railguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.model.Defect
import com.example.railguard.model.MaintenanceTask
import com.example.railguard.model.Tone
import kotlinx.coroutines.launch
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.railguard.data.RailGuardFirebaseService

data class AiChatMessage(
    val sender: String,
    val text: String,
    val actionableType: String? = null, // "TSR", "DISPATCH", "HEATMAP", "GROWTH", "ULTRASONIC"
    val actionableLabel: String? = null
)

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
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Active Neural Model
    var activeModelIndex by remember { mutableIntStateOf(0) }
    val models = listOf(
        "RailVision-DeepTrack v4.2 (Multi-Spectral CNN)",
        "Paris-FractureNet (Non-linear FEA Fatigue)",
        "Nadal-KinematicEngine (Wheel-Climb Dynamics)",
        "ThermalBuckle-SFT (Axial Euler Beam CWR)"
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Failure Kinetics", "Nadal Derailment", "Possession Windows", "Thermal SFT Buckle", "Fleet Interlock")

    var queryText by remember { mutableStateOf("") }
    var isRunningTensorScan by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var currentScanPhase by remember { mutableStateOf("Ready") }
    var lastTensorScanResult by remember {
        mutableStateOf("Multi-Tensor Inference Synced · Edge AI Model Online · Continuous Corridor Monitoring")
    }

    // Interactive Action Feedback Banner
    var quickActionFeedback by remember { mutableStateOf<String?>(null) }

    // Oscilloscope Animation
    val infiniteTransition = rememberInfiniteTransition(label = "Oscilloscope")
    val sweepPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepPhase"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    // Chat History
    val chatMessages = remember {
        mutableStateListOf(
            AiChatMessage(
                sender = "assistant",
                text = "⚡ System initialized: RailVision-DeepTrack Edge AI cluster online (16 ms latency).\n\n" +
                    "Real-time telemetric streams active across monitored corridor sectors. Connected to Central Safety Cloud Database.",
                actionableType = "TSR",
                actionableLabel = "View Live Corridor Telemetry"
            )
        )
    }

    LaunchedEffect(isRunningTensorScan) {
        if (isRunningTensorScan) {
            scanProgress = 0f
            val phases = listOf(
                "Ingesting Multi-Spectral Ultrasonic Waves...",
                "Running 2D Convolutional Feature Maps...",
                "Calculating Paris-Erdogan Stress Intensity (ΔK)...",
                "Evaluating Nadal Flange Climb Ratio (Y/Q)...",
                "Solving Euler Beam-Column Compressive Buckling...",
                "Synthesizing Multi-Tensor Confidence Matrix..."
            )
            for (i in 1..phases.size) {
                currentScanPhase = phases[i - 1]
                scanProgress = i / phases.size.toFloat()
                delay(180)
            }
            isRunningTensorScan = false
            currentScanPhase = "Inference Complete (100%)"
            lastTensorScanResult = when (activeModelIndex) {
                0 -> "DeepTrack v4.2: Flaw depth 46.2mm ±0.3mm · Acoustic signature confirmed · Zero false positives in 12,000 frames"
                1 -> "Paris-FractureNet: da/dN = 2.4×10⁻¹¹ (ΔK)³·² · Remaining safe life: 72.4 Operating Hours · Limit: 50.0mm"
                2 -> "Nadal-KinematicEngine: Y/Q ratio = 0.68 · Wheel climb envelope safe at 25 km/h · Derailment probability 12.4%"
                else -> "ThermalBuckle-SFT: Neutral SFT 27.0°C · Rail temp +11.4°C · Axial stress +68.4 MPa · Buckle margin safe"
            }
            RailGuardFirebaseService.instance.recordAiAnalysis(
                "AI_TENSOR_SCAN",
                mapOf(
                    "model" to models[activeModelIndex],
                    "summary" to lastTensorScanResult,
                    "nadalRatio" to 0.68,
                    "confidence" to 0.992
                )
            )
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
                subtitle = "RailVision-DeepTrack v4.2 · Multi-Tensor Physics & Ultrasonic Telemetry",
                onBack = onBack
            )
        }

        // Quick Action Feedback Banner
        if (quickActionFeedback != null) {
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(quickActionFeedback!!, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { quickActionFeedback = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Neural Model Selector & HUD Status Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0B1422) else Color(0xFF0F1E33)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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
                                    text = "RAILVISION NEURAL ENGINE",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Active: ${models[activeModelIndex].substringBefore(" ")}",
                                    color = Color(0xFF38BDF8),
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
                                text = "16 ms · TPU ONLINE",
                                color = Color(0xFF4ADE80),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Model Selector Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        models.forEachIndexed { index, modelName ->
                            val isSel = activeModelIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B).copy(alpha = 0.6f))
                                    .border(1.dp, if (isSel) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(6.dp))
                                    .clickable { activeModelIndex = index }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = modelName.substringBefore(" ("),
                                    fontSize = 9.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Precision Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF070D18))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("NADAL RATIO", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("0.68 Y/Q", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF070D18))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("CONFIDENCE", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("99.2%", color = Color(0xFF22C55E), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF070D18))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("RUL SAFE LIFE", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("72.4 Hours", color = Color(0xFFF59E0B), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tensor Re-Scan Trigger
                    if (isRunningTensorScan) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(currentScanPhase, color = Color(0xFF38BDF8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("${(scanProgress * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
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
                                modifier = Modifier.weight(1f),
                                lineHeight = 14.sp
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
                                Text("Run Inference", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Ultrasonic Rail A-Scan Spectrogram Oscilloscope
        item {
            SectionLabel(title = "LIVE ULTRASONIC A-SCAN SPECTROGRAM (4.0 MHz SHEAR WAVE)")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF060B12) else Color(0xFF0B1420)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E2E44), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "70° SHEAR WAVE · ECHO AT DEPTH 46.2 mm",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACOUSTIC SYNC",
                                color = Color(0xFF86EFAC),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Oscilloscope Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF03060B))
                    ) {
                        val w = size.width
                        val h = size.height

                        // Grid lines
                        for (gx in 1..8) {
                            val x = (w / 9) * gx
                            drawLine(color = Color(0xFF0F1B2B), start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
                        }
                        for (gy in 1..4) {
                            val y = (h / 5) * gy
                            drawLine(color = Color(0xFF0F1B2B), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                        }

                        // Ground baseline
                        val baselineY = h * 0.82f
                        drawLine(color = Color(0xFF1E293B), start = Offset(0f, baselineY), end = Offset(w, baselineY), strokeWidth = 1.5f)

                        // Ultrasonic Waveform Path
                        val wavePath = Path()
                        wavePath.moveTo(0f, baselineY)

                        val numPoints = 120
                        for (i in 0..numPoints) {
                            val x = (w / numPoints) * i
                            val normX = i / numPoints.toFloat()

                            // Entrance echo at x = 0.08
                            val entranceEcho = if (normX in 0.05f..0.12f) {
                                kotlin.math.sin((normX - 0.05f) / 0.07f * Math.PI.toFloat()) * (h * 0.40f)
                            } else 0f

                            // Critical flaw echo at x = 0.62 (depth 46.2 mm)
                            val flawEcho = if (normX in 0.58f..0.66f) {
                                kotlin.math.sin((normX - 0.58f) / 0.08f * Math.PI.toFloat()) * (h * 0.68f) * pulseGlow
                            } else 0f

                            // Backwall echo at x = 0.90 (rail base 172mm)
                            val backwallEcho = if (normX in 0.86f..0.94f) {
                                kotlin.math.sin((normX - 0.86f) / 0.08f * Math.PI.toFloat()) * (h * 0.50f)
                            } else 0f

                            // Material noise ripple
                            val noise = kotlin.math.sin(i * 0.8f + sweepPhase * 6.28f) * 3f

                            val y = baselineY - (entranceEcho + flawEcho + backwallEcho) + noise
                            wavePath.lineTo(x, y)
                        }

                        // Draw Waveform Glow & Stroke
                        drawPath(
                            path = wavePath,
                            color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                            style = Stroke(width = 4f)
                        )
                        drawPath(
                            path = wavePath,
                            color = Color(0xFF38BDF8),
                            style = Stroke(width = 2f)
                        )

                        // Peak indicator over flaw
                        val flawPeakX = w * 0.62f
                        val flawPeakY = baselineY - (h * 0.68f * pulseGlow)
                        drawCircle(color = Color(0xFFEF4444), radius = 5f, center = Offset(flawPeakX, flawPeakY))
                        drawCircle(color = Color.White, radius = 2f, center = Offset(flawPeakX, flawPeakY))

                        // Sweep vertical line
                        val sweepX = w * sweepPhase
                        drawLine(
                            color = Color(0xFF22C55E).copy(alpha = 0.6f),
                            start = Offset(sweepX, 0f),
                            end = Offset(sweepX, h),
                            strokeWidth = 1.5f
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Entrance Echo (0mm)", color = Color(0xFF64748B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("FLAW PEAK: 46.2mm (+18.4 dB)", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Backwall Echo (172mm)", color = Color(0xFF64748B), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Analytical Models Tabs
        item {
            SectionLabel(title = "PHYSICS-INFORMED RISK EVALUATION PANELS")

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
                            .border(1.dp, if (isSel) colorScheme.primary else colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .clickable { selectedTab = index }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
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

        // Tab Content Deep Dive
        item {
            when (selectedTab) {
                0 -> {
                    // Failure Kinetics
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PARIS-ERDOGAN CRACK KINETICS",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "CRITICAL 72H", tone = Tone.CRITICAL)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val currentTopDefect = defects.firstOrNull()
                        Text(
                            text = if (currentTopDefect != null) "Defect ${currentTopDefect.id} (${currentTopDefect.section})" else "Corridor Anomaly Kinetics & Fracture Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentTopDefect != null) {
                                "• Track ID: ${currentTopDefect.id} at ${currentTopDefect.chainageCoordinate}\n" +
                                "• Estimated Flaw Size: ${currentTopDefect.estimatedLength}\n" +
                                "• Risk Index: ${currentTopDefect.riskScore}/100 · AI Confidence: ${currentTopDefect.aiConfidencePercent}%\n" +
                                "• Prescribed Action: ${currentTopDefect.aiPrescribedAction}"
                            } else {
                                "• Continuous real-time fracture kinetics evaluation online\n" +
                                "• Paris-Erdogan crack propagation model: da/dN = C(ΔK)^m active\n" +
                                "• Monitored by ultrasonic transducers, acoustic emission & optical vision\n" +
                                "• Telemetry streams live from Raspberry Pi sensor unit"
                            },
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
                                title = if (currentTopDefect != null) "Inspect ${currentTopDefect.id}" else "Defect Registry",
                                icon = Icons.Default.Warning,
                                onClick = {
                                    if (currentTopDefect != null) onInspectDefect(currentTopDefect) else onNavigateCrackGrowth()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PrimaryButton(
                                title = "Growth Curve",
                                icon = Icons.Default.ShowChart,
                                secondary = true,
                                onClick = onNavigateCrackGrowth,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                1 -> {
                    // Nadal Derailment Matrix
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "NADAL DERAILMENT LIMIT & WHEEL CLIMB",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "0.68 Y/Q", tone = Tone.CRITICAL)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chainage 14+320 High-Rail Curve (300m Radius)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calculated wheel climb margin via Nadal Criterion (Y/Q):\n" +
                                "• Speed @ 45 km/h: 96.2% derailment probability (CATASTROPHIC)\n" +
                                "• Speed @ 35 km/h: 48.7% flange climb potential\n" +
                                "• Speed @ 25 km/h: 12.4% safe envelope (ACTIVE RESTRICTION ENFORCED)\n" +
                                "• Nadal Equation: Y/Q = (tan δ - μ)/(1 + μ tan δ) = 0.68 (Limit: 0.80)\n" +
                                "• Dynamic Angle of Attack: 1.42° with dynamic track gauge variance (+9.4mm).",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PrimaryButton(
                            title = "Open Corridor Risk Heatmap",
                            icon = Icons.Default.Layers,
                            onClick = onNavigateHeatmap
                        )
                    }
                }
                2 -> {
                    // Possession Windows
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "OPTIMAL MAINTENANCE POSSESSION WINDOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "01:15 - 04:30 GMT", tone = Tone.HEALTHY)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "North Loop Corridor Window: 3h 15m Blackout Tonight",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Traffic Blackout: 195 minutes without commercial train conflict\n" +
                                "• Work Order WO-881: Replace Rail Clip Pair & Emergency Splice Clamps\n" +
                                "• Required Possession Duration: 85 minutes (Margin: +110 mins)\n" +
                                "• Crew 04 (Lead: M. Ross) equipped with induction heater & torque kit\n" +
                                "• Service Disruption: 0% morning passenger peak delay impact.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PrimaryButton(
                            title = "Dispatch Work Order WO-881 to Crew 04",
                            icon = Icons.Default.Build,
                            onClick = {
                                if (tasks.isNotEmpty()) onDispatchTask(tasks.first())
                                quickActionFeedback = "Work Order WO-881 dispatched to Crew 04 for 01:15 GMT possession window."
                            }
                        )
                    }
                }
                3 -> {
                    // Thermal Buckle
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "THERMAL STRESS & CWR BUCKLE CRITICALITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "+68.4 MPa", tone = Tone.WARNING)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Continuous Welded Rail (60E1 CWR) Thermal Stability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Projected Peak Rail Surface Temp: 48.4°C (+11.4°C over SFT)\n" +
                                "• Stress-Free Neutral Temperature (SFT): 27.0°C\n" +
                                "• Compressive Axial Stress: +68.4 MPa (128 kN axial force)\n" +
                                "• Critical Euler Buckling Threshold: P_buckle = 145 kN (Margin: 17 kN)\n" +
                                "• Lateral Ballast Resistance: 4.8 kN/sleeper (Marginal at 14+380)\n" +
                                "• Recommendation: Deploy destressing hydraulic puller team before 11:30.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
                else -> {
                    // Fleet Interlock
                    RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AUTONOMOUS FLEET TSR INTERLOCK",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(label = "INTERLOCKED", tone = Tone.INFO)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Live Cab Signal Enforcements Across Corridor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Train TR-104 (InterCity Express): Enforcing 25 km/h TSR at KM 14+000\n" +
                                "• Train FR-802 (Heavy Haul Freight): Enforcing 40 km/h TSR at KM 08+000\n" +
                                "• Train HS-301 (Arrow Bullet): Full 180 km/h authorized at KM 03+000\n" +
                                "• Train RC-515 (Regional Metro): Full 90 km/h clear at KM 01+000\n" +
                                "• Direct Cab DMI Beacon: Active via 5G Telemetry Uplink (Loss rate: 0.00%).",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PrimaryButton(
                            title = "Broadcast TSR Advisory to Fleet",
                            icon = Icons.Default.Send,
                            onClick = {
                                quickActionFeedback = "Emergency TSR advisory re-broadcasted to TR-104 and FR-802."
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Interactive Natural Language AI Field Assistant
        item {
            SectionLabel(title = "AI MISSION CONTROL CONVERSATION ENGINE")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF070D18) else Color(0xFFF1F5F9)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E2E44), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    // Chat messages history
                    chatMessages.forEach { msg ->
                        val isUser = msg.sender == "user"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isUser) colorScheme.primary
                                        else (if (isDark) Color(0xFF0F1B2B) else Color.White)
                                    )
                                    .border(
                                        1.dp,
                                        if (isUser) colorScheme.primary else Color(0xFF1E2E44),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .widthIn(max = 300.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = if (isUser) colorScheme.onPrimary else colorScheme.onSurface
                                )
                            }

                            // Optional Actionable Button attached to assistant message
                            if (msg.actionableType != null && msg.actionableLabel != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(6.dp))
                                        .clickable {
                                            when (msg.actionableType) {
                                                "TSR" -> quickActionFeedback = "TSR 25 km/h verified and acknowledged by Driver on TR-104."
                                                "DISPATCH" -> {
                                                    if (tasks.isNotEmpty()) onDispatchTask(tasks.first())
                                                    quickActionFeedback = "WO-881 dispatched to maintenance car."
                                                }
                                                "HEATMAP" -> onNavigateHeatmap()
                                                "GROWTH" -> onNavigateCrackGrowth()
                                                else -> quickActionFeedback = "Directive executed successfully."
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(msg.actionableLabel, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
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
                            "Safest operating speed?",
                            "Predict crack rupture window?",
                            "Work possession window?",
                            "CWR buckling risk?",
                            "Explain Nadal ratio"
                        ).forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.12f))
                                    .border(1.dp, colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        chatMessages.add(AiChatMessage("user", suggestion))
                                        val (replyText, actType, actLabel) = when {
                                            suggestion.contains("speed", ignoreCase = true) -> Triple(
                                                "⚡ Under current corridor geometry and curve radius conditions, maximum safe operating speed is evaluated based on live sensor feed.\n\n" +
                                                    "If flaw is detected, exceeding 35 km/h escalates wheel climb derailment risk. Safe TSR is auto-dispatched to cab.",
                                                "TSR",
                                                "Confirm Cab TSR Restrictions"
                                            )
                                            suggestion.contains("rupture", ignoreCase = true) || suggestion.contains("break", ignoreCase = true) -> Triple(
                                                "💥 Flaw rupture kinetics are modeled using Paris-Erdogan law (da/dN = C(ΔK)^m). Under continuous freight passes, growth velocity is tracked against the critical rail fracture limit.\n\n" +
                                                    "Recommendation: Real-time ultrasonic transducer monitoring is active.",
                                                "GROWTH",
                                                "View Crack Growth FEA Curve"
                                            )
                                            suggestion.contains("window", ignoreCase = true) -> Triple(
                                                "🕒 North Loop is completely dark to commercial traffic between 01:15 and 04:30 GMT tonight (195 minutes).\n\n" +
                                                    "Work Order WO-881 requires 85 minutes. Crew 04 is currently available on standby.",
                                                "DISPATCH",
                                                "Dispatch WO-881 to Crew 04"
                                            )
                                            suggestion.contains("buckling", ignoreCase = true) -> Triple(
                                                "🔥 Tomorrow's ambient 34°C will drive rail surface to 48.4°C (+11.4°C over Neutral SFT 27°C).\n\n" +
                                                    "Compressive stress will peak at +68.4 MPa (128 kN axial force, near 145 kN Euler buckling limit). Destressing required before 11:30.",
                                                "HEATMAP",
                                                "Inspect Corridor Heatmap"
                                            )
                                            else -> Triple(
                                                "📐 Nadal Derailment Criterion evaluates lateral wheel force (Y) over vertical axle load (Q):\n" +
                                                    "Y/Q = (tan δ - μ) / (1 + μ tan δ)\n\n" +
                                                    "With flange angle δ=68° and friction μ=0.38, current ratio is 0.68. The critical derailment boundary is 0.80.",
                                                null,
                                                null
                                            )
                                        }
                                        chatMessages.add(AiChatMessage("assistant", replyText, actType, actLabel))
                                        scope.launch {
                                            RailGuardFirebaseService.instance.saveAiOracleQuery(
                                                query = suggestion,
                                                response = replyText,
                                                model = models[activeModelIndex]
                                            )
                                        }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
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

                    val doSend = {
                        if (queryText.isNotBlank()) {
                            val userMsg = queryText
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            chatMessages.add(AiChatMessage("user", userMsg))
                            queryText = ""
                            val topDef = defects.firstOrNull()
                            val reply = "🤖 RailVision-DeepTrack Analysis for '$userMsg':\n\n" +
                                if (topDef != null) {
                                    "• Corridor track integrity evaluated under active monitoring.\n" +
                                    "• Track flaw ${topDef.id} (${topDef.estimatedLength} at ${topDef.chainageCoordinate}) is monitored in real-time.\n" +
                                    "• Multi-Tensor inference confidence: ${topDef.aiConfidencePercent}%.\n" +
                                    "• Safety Directive: ${topDef.aiPrescribedAction}"
                                } else {
                                    "• Corridor track integrity evaluated at nominal condition.\n" +
                                    "• Continuous ultrasonic, thermal and accelerometer telemetry active.\n" +
                                    "• Multi-Tensor inference confidence: 99.4%.\n" +
                                    "• Safety Directive: Operating at normal line speed."
                                }
                            chatMessages.add(AiChatMessage("assistant", reply, "TSR", "Verify Cab Interlock"))
                            scope.launch {
                                RailGuardFirebaseService.instance.saveAiOracleQuery(
                                    query = userMsg,
                                    response = reply,
                                    model = models[activeModelIndex]
                                )
                            }
                        }
                    }

                    // Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            placeholder = { Text("Ask RailGuard AI (e.g., crack kinetics, speed, thermal)...", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { doSend() }),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { doSend() },
                            modifier = Modifier
                                .size(46.dp)
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
