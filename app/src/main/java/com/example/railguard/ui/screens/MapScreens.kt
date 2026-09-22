package com.example.railguard.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.model.Defect
import com.example.railguard.model.LocalAppSettings
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable
fun MapScreen(
    onNavigateToDefectMap: () -> Unit,
    onNavigateToHeatmap: () -> Unit,
    onNavigateToLocationDetails: () -> Unit,
    onSelectDefect: (Defect) -> Unit,
    defects: List<Defect>,
    onBack: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    val sections = listOf(
        Triple("West Cut · Section 01", "Chainage 01+000 - 02+800 · Health 98.1%", Tone.HEALTHY),
        Triple("North Loop · Section 14", "Chainage 14+000 - 15+250 · CRK-2048 (46mm)", Tone.CRITICAL),
        Triple("East Junction · Section 03", "Chainage 03+000 - 04+200 · MT-878 Grind", Tone.WARNING),
        Triple("South Yard · Section 08", "Chainage 08+000 - 09+600 · Switch 08A", Tone.WARNING)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Corridor Network Map",
                subtitle = "Active track lines, train telemetry, and geolocated defects",
                onBack = onBack
            )
        }

        item {
            RailwayLineGraphic(
                showMarkers = true,
                onMarkerClick = { onNavigateToDefectMap() }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    title = "Defect Pin Map",
                    icon = Icons.Default.Place,
                    onClick = onNavigateToDefectMap,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    title = "Risk Heatmap",
                    icon = Icons.Default.Layers,
                    secondary = true,
                    onClick = onNavigateToHeatmap,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionLabel(title = "CORRIDOR SECTIONS")
        }

        items(sections) { (title, subtitle, tone) ->
            ListRow(
                icon = Icons.Default.AltRoute,
                title = title,
                subtitle = subtitle,
                trailing = tone.name,
                tone = tone,
                onClick = onNavigateToLocationDetails
            )
        }
    }
}

@Composable
fun DefectMapScreen(
    defects: List<Defect>,
    onSelectDefect: (Defect) -> Unit,
    onNavigateToHeatmap: () -> Unit,
    onNavigateToGps: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings = LocalAppSettings.current
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedDefect = defects.getOrNull(selectedIndex) ?: defects.firstOrNull()

    // Live Train Telemetry Simulation State
    var isTrainDispatched by remember { mutableStateOf(true) }
    var trainProgress by remember { mutableFloatStateOf(0.18f) }
    var trainSpeed by remember { mutableIntStateOf(105) }

    LaunchedEffect(isTrainDispatched) {
        if (isTrainDispatched) {
            while (isActive && isTrainDispatched) {
                delay(80)
                trainProgress += 0.006f
                if (trainProgress > 1f) {
                    trainProgress = 0.02f
                }
                // Proximity to Defect 0 (at 0.28f, CRK-2048, 46mm)
                val distToDefect1 = abs(trainProgress - 0.28f)
                val distToDefect2 = abs(trainProgress - 0.58f)
                trainSpeed = when {
                    distToDefect1 < 0.08f -> 25 // Enforced TSR 25 km/h
                    distToDefect2 < 0.06f -> 45 // Enforced Caution 45 km/h
                    else -> 108 // Normal line cruising speed
                }
            }
        } else {
            trainSpeed = 0
        }
    }

    val currentKm = 12.0 + (trainProgress * 4.5)
    val kmWhole = currentKm.toInt()
    val meters = ((currentKm - kmWhole) * 1000).toInt()
    val chainageStr = String.format("KM %02d+%03d", kmWhole, meters)
    val isNearCriticalDefect = abs(trainProgress - 0.28f) < 0.08f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Defect Pin Map",
                subtitle = "Live telemetry & defect corridor tracking",
                onBack = onBack
            )
        }

        // Live Train Dispatch Controls & HUD status
        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isTrainDispatched) Color(0xFF22C55E) else Color(0xFFEAB308))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTrainDispatched) "LIVE TRAIN TELEMETRY ACTIVE" else "TRAIN HALTED AT DEPOT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTrainDispatched) Color(0xFF22C55E) else Color(0xFFEAB308),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "TR-104 Intercity · $chainageStr · ${settings.formatSpeed(trainSpeed)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    StatusPill(
                        label = if (isTrainDispatched) "LIVE TRACK" else "IDLE",
                        tone = if (isTrainDispatched) Tone.HEALTHY else Tone.WARNING
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dispatch & Reset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryButton(
                        title = if (isTrainDispatched) "Halt Train" else "Send / Dispatch Train",
                        icon = if (isTrainDispatched) Icons.Default.Pause else Icons.Default.PlayArrow,
                        secondary = isTrainDispatched,
                        onClick = { isTrainDispatched = !isTrainDispatched },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        title = "Reset Depot",
                        icon = Icons.Default.Refresh,
                        secondary = true,
                        onClick = {
                            trainProgress = 0.05f
                            trainSpeed = if (isTrainDispatched) 105 else 0
                        },
                        modifier = Modifier.weight(0.7f)
                    )
                }

                if (isNearCriticalDefect) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CAB SIGNAL TSR: Passing CRK-2048 (46mm). Automated clamp to ${settings.formatSpeed(25)}!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Dedicated Defect Map Workspaces
        item {
            SectionLabel(title = "MAP WORKSPACES")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Risk Heatmap",
                    subtitle = "Stress gradients",
                    icon = Icons.Default.Layers,
                    onClick = onNavigateToHeatmap,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "GPS Lock",
                    subtitle = "RTK fix ±1.2m",
                    icon = Icons.Default.GpsFixed,
                    onClick = onNavigateToGps,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Open Dossier",
                    subtitle = selectedDefect?.id ?: "Inspect",
                    icon = Icons.Default.FindInPage,
                    onClick = { selectedDefect?.let { onSelectDefect(it) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Interactive Live Map Canvas Area with Moving Train
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F1722))
                    .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                // Background grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                    }
                }

                // Speed restriction zone highlight band at Defect 0
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .align(Alignment.CenterStart)
                        .offset(x = 65.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                )

                // Track Line (Dual Rails)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.Center)
                        .offset(y = (-8).dp)
                        .background(Color(0xFF38BDF8))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.Center)
                        .offset(y = 8.dp)
                        .background(Color(0xFF94A3B8).copy(alpha = 0.7f))
                )

                // Defect Pins on Track Line
                val positions = listOf(0.28f, 0.58f, 0.85f)
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = maxWidth

                    defects.take(3).forEachIndexed { index, defect ->
                        val isSelected = selectedIndex == index
                        val toneColor = toneColor(defect.tone, true)
                        val pinX = (canvasWidth - 36.dp) * positions[index]

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = pinX, y = if (index % 2 == 0) (-34).dp else 22.dp)
                                .clip(CircleShape)
                                .background(toneColor)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedIndex = index }
                                .padding(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = defect.id,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Real-Time Moving Train
                    val trainX = (canvasWidth - 54.dp) * trainProgress
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = trainX, y = (-8).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (trainSpeed <= 25) Color(0xFFEF4444) else Color(0xFF0284C7))
                            .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = "TR-104",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "TR-104 ${settings.formatSpeed(trainSpeed)}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // HUD bottom info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS: 51°30'14.2\"N · 0°07'42.8\"W",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ETCS L2 ACTIVE",
                        color = Color(0xFF22C55E),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Selected Defect Card
        if (selectedDefect != null) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                RailCard(
                    onClick = { onSelectDefect(selectedDefect) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDefect.id,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        StatusPill(label = "RISK ${selectedDefect.score}", tone = selectedDefect.tone)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedDefect.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedDefect.section} · Estimated ${selectedDefect.estimatedLength}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrimaryButton(
                        title = "Open Defect Dossier",
                        icon = Icons.Default.ArrowForward,
                        onClick = { onSelectDefect(selectedDefect) }
                    )
                }
            }
        }
    }
}

data class SectorMetricDetail(
    val score: Int,
    val tone: Tone,
    val headline: String,
    val subtext: String,
    val formulaName: String,
    val formulaValue: String,
    val directive: String
)

data class CorridorSector(
    val id: String,
    val name: String,
    val line: String, // "UP" or "DOWN"
    val chainage: String,
    val lengthKm: Double,
    val baseRiskScore: Int,
    val activeDefects: Int,
    val tonnageMgt: Double,
    val cwrStressMpa: Double,
    val railTempDeltaC: Double,
    val activeTsr: String,
    val approachingTrain: String,
    val workOrder: String,
    val mapXRatio: Float, // 0.0f to 1.0f on canvas
    val mapTrackLine: Int // 1 for UP, 2 for DOWN
) {
    fun getMetric(dimensionIndex: Int, tempOffset: Float): SectorMetricDetail {
        return when (dimensionIndex) {
            1 -> {
                // Crack Propagation & Paris Kinetics
                val fatigueScore = kotlin.math.min(100, kotlin.math.max(8, (baseRiskScore * 1.05f + tempOffset * 0.4f).toInt()))
                val tone = when {
                    fatigueScore >= 75 -> Tone.CRITICAL
                    fatigueScore >= 26 -> Tone.WARNING
                    else -> Tone.HEALTHY
                }
                val growthRate = String.format("%.2f", (fatigueScore / 100f) * 0.78)
                val deltaK = String.format("%.1f", 5.0 + (fatigueScore / 100f) * 20.0)
                SectorMetricDetail(
                    score = fatigueScore,
                    tone = tone,
                    headline = "+$growthRate mm/MGT Paris Fatigue Rate",
                    subtext = if (fatigueScore >= 75) "Critical crack growth rate · Imminent rupture risk if unmonitored"
                              else if (fatigueScore >= 26) "Sub-critical growth · Cyclic stress monitoring advised"
                              else "Stable elastic state · Negligible flaw propagation",
                    formulaName = "Paris Law: da/dN = 3.2×10⁻¹¹ (ΔK)³·⁴",
                    formulaValue = "ΔK = $deltaK MPa√m (K_IC threshold: 30.0)",
                    directive = if (fatigueScore >= 75) "EMERGENCY: Immediate clamp & thermite re-weld possession"
                                else if (fatigueScore >= 26) "Schedule ultrasonic B-scan verification within 48 hours"
                                else "Standard maintenance schedule confirmed"
                )
            }
            2 -> {
                // Thermal SFT Stress & Buckling: sigma = E * alpha * dT
                val totalDelta = railTempDeltaC.toFloat() + tempOffset
                val stress = cwrStressMpa.toFloat() + (tempOffset * 2.4f)
                val thermalScore = kotlin.math.min(100, kotlin.math.max(10, ((stress / 75f) * 90f).toInt()))
                val tone = when {
                    thermalScore >= 75 -> Tone.CRITICAL
                    thermalScore >= 26 -> Tone.WARNING
                    else -> Tone.HEALTHY
                }
                SectorMetricDetail(
                    score = thermalScore,
                    tone = tone,
                    headline = "+${String.format("%.1f", totalDelta)}°C Neutral SFT Delta (+${String.format("%.1f", stress)} MPa)",
                    subtext = if (thermalScore >= 75) "Critical compressive axial load · Severe track lateral shift/buckle threat"
                              else if (thermalScore >= 26) "Elevated thermal stress · Monitor ballast shoulder resistance"
                              else "Normal thermal range · Continuous Welded Rail within stable neutral band",
                    formulaName = "Thermal Stress: σ = E · α · ΔT (E=210 GPa, α=1.15×10⁻⁵/°C)",
                    formulaValue = "Euler Axial Compressive Force: ${String.format("%.1f", stress * 7.68)} kN",
                    directive = if (thermalScore >= 75) "IMMEDIATE: Restrict line speed & dispatch thermal patrol crew"
                                else if (thermalScore >= 26) "Monitor track temperature sensors & ballast shoulder profile"
                                else "Normal operations authorized"
                )
            }
            3 -> {
                // Dynamic Gauge & Ballast Lateral Resistance
                val gaugeScore = kotlin.math.min(100, kotlin.math.max(12, (baseRiskScore * 0.95f + (if (line == "DOWN") 6 else 0)).toInt()))
                val tone = when {
                    gaugeScore >= 75 -> Tone.CRITICAL
                    gaugeScore >= 26 -> Tone.WARNING
                    else -> Tone.HEALTHY
                }
                val spreadMm = String.format("%.1f", 1435.0 + (gaugeScore / 100f) * 14.5)
                SectorMetricDetail(
                    score = gaugeScore,
                    tone = tone,
                    headline = "$spreadMm mm Track Gauge (${String.format("%+.1f", (gaugeScore / 100f) * 14.5)} mm Deviation)",
                    subtext = if (gaugeScore >= 75) "FRA Class 4 violation: Fastener clips yielding under lateral push"
                              else if (gaugeScore >= 26) "Slight gauge widening under heavy axle loads"
                              else "Nominal standard gauge (1435 mm ±1 mm) verified",
                    formulaName = "Gauge Degradation: G = G₀ + C(T_MGT) · P_lateral",
                    formulaValue = "Lateral Track Resistance: ${String.format("%.1f", 12.0 - (gaugeScore / 100f) * 6.5)} kN/sleeper",
                    directive = if (gaugeScore >= 75) "MANDATORY: Fastener clip replacement & automated tamping"
                                else if (gaugeScore >= 26) "Inspect fastening clips & tie pad degradation"
                                else "Track geometry nominal"
                )
            }
            4 -> {
                // Wheel Impact & Nadal Derailment Envelope: Y/Q
                val nadalScore = kotlin.math.min(100, kotlin.math.max(15, (baseRiskScore * 0.9f + (if (line == "DOWN") 10 else 0)).toInt()))
                val tone = when {
                    nadalScore >= 75 -> Tone.CRITICAL
                    nadalScore >= 26 -> Tone.WARNING
                    else -> Tone.HEALTHY
                }
                val yqRatio = String.format("%.2f", 0.35 + (nadalScore / 100f) * 0.52)
                SectorMetricDetail(
                    score = nadalScore,
                    tone = tone,
                    headline = "Nadal Y/Q = $yqRatio (Derailment Safety Factor)",
                    subtext = if (nadalScore >= 75) "Dangerous lateral-to-vertical wheel climb threshold approaching"
                              else if (nadalScore >= 26) "Elevated wheel-rail interaction during curved transit"
                              else "Safe dynamic contact envelope with high adhesion margin",
                    formulaName = "Nadal Formula: (Y/Q)_limit = (tan δ - μ) / (1 + μ tan δ)",
                    formulaValue = "Max Allowable Limit: 0.80 (Current margin: ${String.format("%.2f", kotlin.math.max(0.02, 0.80 - (0.35 + (nadalScore / 100f) * 0.52)))})",
                    directive = if (nadalScore >= 75) "INTERLOCK: Enforce strict speed reduction to prevent flange climb"
                                else if (nadalScore >= 26) "Schedule acoustic wheel impact detector (WILD) re-calibration"
                                else "Vehicle-track dynamic balance optimal"
                )
            }
            else -> {
                // Combined Risk Index
                val dynamicScore = kotlin.math.min(100, (baseRiskScore + (tempOffset * 1.2f)).toInt())
                val tone = when {
                    dynamicScore >= 75 -> Tone.CRITICAL
                    dynamicScore >= 26 -> Tone.WARNING
                    else -> Tone.HEALTHY
                }
                SectorMetricDetail(
                    score = dynamicScore,
                    tone = tone,
                    headline = "$dynamicScore/100 Composite Derailment Risk Index",
                    subtext = when {
                        dynamicScore >= 75 -> "Multiple critical track geometry & flaw parameters exceeded on $name"
                        dynamicScore >= 26 -> "Moderate wear parameters under cumulative $tonnageMgt MGT traffic on $name"
                        else -> "Track parameters within safe operating tolerances on $name"
                    },
                    formulaName = "Composite: 0.40(Fatigue) + 0.25(Gauge) + 0.20(Thermal) + 0.15(Nadal)",
                    formulaValue = "${String.format("%.2f", dynamicScore / 100f)} Overall Hazard Index",
                    directive = when {
                        dynamicScore >= 75 -> "Enforce TSR restriction · Immediate track maintenance dispatch"
                        dynamicScore >= 26 -> "Schedule corrective maintenance window within 5 business days"
                        else -> "Nominal line speed authorized"
                    }
                )
            }
        }
    }
}

@Composable
fun RiskHeatmapScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current

    val baseSectors = remember {
        listOf(
            CorridorSector(
                id = "SEC-14",
                name = "Section 14 (North Loop)",
                line = "UP",
                chainage = "14+000 - 15+250 UP",
                lengthKm = 1.25,
                baseRiskScore = 92,
                activeDefects = 8,
                tonnageMgt = 48.6,
                cwrStressMpa = 68.4,
                railTempDeltaC = 11.4,
                activeTsr = "25 km/h Mandatory TSR",
                approachingTrain = "TR-104 (InterCity Express)",
                workOrder = "WO-881: High-Priority Splice & Clamp",
                mapXRatio = 0.78f,
                mapTrackLine = 1
            ),
            CorridorSector(
                id = "SEC-08",
                name = "Section 08 (South Freight Yard)",
                line = "DOWN",
                chainage = "08+000 - 09+600 DOWN",
                lengthKm = 1.60,
                baseRiskScore = 68,
                activeDefects = 5,
                tonnageMgt = 52.1,
                cwrStressMpa = 54.0,
                railTempDeltaC = 8.2,
                activeTsr = "40 km/h Freight TSR",
                approachingTrain = "FR-802 (Heavy Haul Freight)",
                workOrder = "WO-872: Frog & Switch Point Tamping",
                mapXRatio = 0.50f,
                mapTrackLine = 2
            ),
            CorridorSector(
                id = "SEC-03",
                name = "Section 03 (East High-Speed Junction)",
                line = "UP",
                chainage = "03+000 - 04+200 UP",
                lengthKm = 1.20,
                baseRiskScore = 44,
                activeDefects = 3,
                tonnageMgt = 36.4,
                cwrStressMpa = 32.5,
                railTempDeltaC = 4.8,
                activeTsr = "Line Speed Authorized (180 km/h)",
                approachingTrain = "HS-301 (Arrow Bullet)",
                workOrder = "WO-865: Cyclical Acoustic Rail Grinding",
                mapXRatio = 0.28f,
                mapTrackLine = 1
            ),
            CorridorSector(
                id = "SEC-01",
                name = "Section 01 (West Deep Cut)",
                line = "DOWN",
                chainage = "01+000 - 02+800 DOWN",
                lengthKm = 1.80,
                baseRiskScore = 12,
                activeDefects = 2,
                tonnageMgt = 29.8,
                cwrStressMpa = 18.0,
                railTempDeltaC = 1.2,
                activeTsr = "Nominal (90 km/h Clear)",
                approachingTrain = "RC-515 (Regional Metro)",
                workOrder = "WO-840: Standard Visual Patrol",
                mapXRatio = 0.08f,
                mapTrackLine = 2
            ),
            CorridorSector(
                id = "SEC-19",
                name = "Section 19 (Mountain Viaduct)",
                line = "DOWN",
                chainage = "19+200 - 20+800 DOWN",
                lengthKm = 1.60,
                baseRiskScore = 86,
                activeDefects = 7,
                tonnageMgt = 51.2,
                cwrStressMpa = 62.0,
                railTempDeltaC = 9.8,
                activeTsr = "30 km/h Viaduct TSR",
                approachingTrain = "FR-802 (Heavy Haul Freight)",
                workOrder = "WO-890: Expansion Joint & Bearing",
                mapXRatio = 0.90f,
                mapTrackLine = 2
            ),
            CorridorSector(
                id = "SEC-11",
                name = "Section 11 (River Basin Truss)",
                line = "UP",
                chainage = "11+100 - 12+450 UP",
                lengthKm = 1.35,
                baseRiskScore = 78,
                activeDefects = 6,
                tonnageMgt = 46.8,
                cwrStressMpa = 59.2,
                railTempDeltaC = 8.6,
                activeTsr = "35 km/h Bridge TSR",
                approachingTrain = "TR-104 (InterCity Express)",
                workOrder = "WO-885: Pier Settlement Clamp",
                mapXRatio = 0.64f,
                mapTrackLine = 1
            ),
            CorridorSector(
                id = "SEC-05",
                name = "Section 05 (Central Metro Terminal)",
                line = "DOWN",
                chainage = "05+000 - 06+300 DOWN",
                lengthKm = 1.30,
                baseRiskScore = 36,
                activeDefects = 3,
                tonnageMgt = 38.5,
                cwrStressMpa = 26.0,
                railTempDeltaC = 3.6,
                activeTsr = "Nominal (80 km/h)",
                approachingTrain = "RC-515 (Regional Metro)",
                workOrder = "WO-855: Platform Clearance Check",
                mapXRatio = 0.40f,
                mapTrackLine = 2
            ),
            CorridorSector(
                id = "SEC-02",
                name = "Section 02 (Valley Tangent Run)",
                line = "UP",
                chainage = "02+000 - 03+000 UP",
                lengthKm = 1.00,
                baseRiskScore = 16,
                activeDefects = 1,
                tonnageMgt = 31.0,
                cwrStressMpa = 15.0,
                railTempDeltaC = 1.8,
                activeTsr = "Line Speed Clear (160 km/h)",
                approachingTrain = "HS-301 (Arrow Bullet)",
                workOrder = "WO-830: Standard Routine Inspection",
                mapXRatio = 0.18f,
                mapTrackLine = 1
            )
        )
    }

    // Interactive Filter States
    var selectedDimension by remember { mutableIntStateOf(0) }
    val dimensions = listOf(
        "Combined Risk",
        "Crack Propagation",
        "Thermal SFT Stress",
        "Dynamic Gauge",
        "Wheel Impact"
    )

    var severityFilter by remember { mutableStateOf("All") } // "All", "Critical", "Warning", "Nominal"
    var lineFilter by remember { mutableStateOf("All Lines") } // "All Lines", "UP Line", "DOWN Line"
    var sortBy by remember { mutableStateOf("Highest Risk") } // "Highest Risk", "Lowest Risk", "Chainage"
    var simulationTempOffset by remember { mutableFloatStateOf(0f) } // +0°C to +15°C What-If ambient heat delta
    var selectedSectorId by remember { mutableStateOf("SEC-14") }
    var cabInterlockAlertSent by remember { mutableStateOf(false) }

    // Dynamic filtering & sorting
    val filteredSectors = remember(selectedDimension, severityFilter, lineFilter, sortBy, simulationTempOffset) {
        baseSectors
            .filter { sector ->
                val metric = sector.getMetric(selectedDimension, simulationTempOffset)
                val matchesSeverity = when (severityFilter) {
                    "Critical" -> metric.score >= 75
                    "Warning" -> metric.score in 26..74
                    "Nominal" -> metric.score <= 25
                    else -> true
                }
                val matchesLine = when (lineFilter) {
                    "UP Line" -> sector.line == "UP"
                    "DOWN Line" -> sector.line == "DOWN"
                    else -> true
                }
                matchesSeverity && matchesLine
            }
            .sortedWith { a, b ->
                val metricA = a.getMetric(selectedDimension, simulationTempOffset)
                val metricB = b.getMetric(selectedDimension, simulationTempOffset)
                when (sortBy) {
                    "Highest Risk" -> metricB.score.compareTo(metricA.score)
                    "Lowest Risk" -> metricA.score.compareTo(metricB.score)
                    "Chainage" -> a.chainage.compareTo(b.chainage)
                    else -> metricB.score.compareTo(metricA.score)
                }
            }
    }

    val selectedSector = filteredSectors.find { it.id == selectedSectorId }
        ?: filteredSectors.firstOrNull()
        ?: baseSectors.first()
    val activeMetric = selectedSector.getMetric(selectedDimension, simulationTempOffset)

    LaunchedEffect(filteredSectors) {
        if (filteredSectors.isNotEmpty() && filteredSectors.none { it.id == selectedSectorId }) {
            selectedSectorId = filteredSectors.first().id
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "HeatPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val reticleAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ReticleAngle"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Corridor Risk Heatmap",
                subtitle = "Multi-zone criticality density, thermal stress & active trains",
                onBack = onBack
            )
        }

        // Network Summary Statistics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "120 km",
                    label = "Total Corridor",
                    tone = Tone.INFO,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "${filteredSectors.count { it.getMetric(selectedDimension, simulationTempOffset).score >= 75 }} Zones",
                    label = "Critical (≥75)",
                    tone = Tone.CRITICAL,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "${filteredSectors.size} Shown",
                    label = "Filtered Sectors",
                    tone = Tone.WARNING,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Heatmap Dimension Selector (Filter 1)
        item {
            SectionLabel(title = "1. HEATMAP METRIC DIMENSION")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dimensions.forEachIndexed { idx, dim ->
                    val isSel = selectedDimension == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) colorScheme.primary else colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                if (isSel) colorScheme.primary else colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedDimension = idx }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = dim,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Secondary Filters: Severity & Line & Sort (Filters 2, 3, 4)
        item {
            SectionLabel(title = "2. SEVERITY & TRACK LINE FILTERS")

            // Severity Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Critical", "Warning", "Nominal").forEach { filter ->
                    val isSel = severityFilter == filter
                    val lineScopedSectors = when (lineFilter) {
                        "UP Line" -> baseSectors.filter { it.line == "UP" }
                        "DOWN Line" -> baseSectors.filter { it.line == "DOWN" }
                        else -> baseSectors
                    }
                    val count = when (filter) {
                        "Critical" -> lineScopedSectors.count { it.getMetric(selectedDimension, simulationTempOffset).score >= 75 }
                        "Warning" -> lineScopedSectors.count { it.getMetric(selectedDimension, simulationTempOffset).score in 26..74 }
                        "Nominal" -> lineScopedSectors.count { it.getMetric(selectedDimension, simulationTempOffset).score <= 25 }
                        else -> lineScopedSectors.size
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSel) {
                                    when (filter) {
                                        "Critical" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                        "Warning" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        "Nominal" -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                        else -> colorScheme.primary.copy(alpha = 0.2f)
                                    }
                                } else colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .border(
                                1.dp,
                                if (isSel) {
                                    when (filter) {
                                        "Critical" -> Color(0xFFEF4444)
                                        "Warning" -> Color(0xFFF59E0B)
                                        "Nominal" -> Color(0xFF22C55E)
                                        else -> colorScheme.primary
                                    }
                                } else colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { severityFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$filter ($count)",
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) {
                                when (filter) {
                                    "Critical" -> Color(0xFFEF4444)
                                    "Warning" -> Color(0xFFF59E0B)
                                    "Nominal" -> Color(0xFF22C55E)
                                    else -> colorScheme.primary
                                }
                            } else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Line Direction & Sorting Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All Lines", "UP Line", "DOWN Line").forEach { line ->
                    val isSel = lineFilter == line
                    val sevScopedSectors = when (severityFilter) {
                        "Critical" -> baseSectors.filter { it.getMetric(selectedDimension, simulationTempOffset).score >= 75 }
                        "Warning" -> baseSectors.filter { it.getMetric(selectedDimension, simulationTempOffset).score in 26..74 }
                        "Nominal" -> baseSectors.filter { it.getMetric(selectedDimension, simulationTempOffset).score <= 25 }
                        else -> baseSectors
                    }
                    val lineCount = when (line) {
                        "UP Line" -> sevScopedSectors.count { it.line == "UP" }
                        "DOWN Line" -> sevScopedSectors.count { it.line == "DOWN" }
                        else -> sevScopedSectors.size
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, if (isSel) colorScheme.primary else colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .clickable { lineFilter = line }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "$line ($lineCount)",
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                listOf("Highest Risk", "Lowest Risk", "Chainage").forEach { sort ->
                    val isSel = sortBy == sort
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) Color(0xFF0284C7).copy(alpha = 0.25f) else colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, if (isSel) Color(0xFF0284C7) else colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .clickable { sortBy = sort }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Sort: $sort",
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color(0xFF38BDF8) else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Filter Summary Banner
            if (severityFilter != "All" || lineFilter != "All Lines" || simulationTempOffset > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Filter: $severityFilter · $lineFilter (${filteredSectors.size}/${baseSectors.size} Sectors)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Reset All",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.clickable {
                            severityFilter = "All"
                            lineFilter = "All Lines"
                            simulationTempOffset = 0f
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Live Simulation Slider (What-If Analysis)
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F1728) else Color(0xFFF1F5F9)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WHAT-IF AMBIENT HEAT STRESS SIMULATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Text(
                            text = "+${String.format("%.1f", simulationTempOffset)}°C Delta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (simulationTempOffset > 5f) Color(0xFFEF4444) else Color(0xFFF59E0B)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = simulationTempOffset,
                        onValueChange = { simulationTempOffset = it },
                        valueRange = 0f..15f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color(0xFFEF4444),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Drag slider to simulate summer afternoon heatwave effect on CWR rail compressive buckling and Nadal wheel derailment index across all sectors in real time.",
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Interactive Graphical Heatmap Canvas (Tap to select sector)
        item {
            SectionLabel(title = "CORRIDOR SCHEMATIC (TAP ANY ZONE ON TRACK)")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF070A11) else Color(0xFF0F172A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E2D44), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "METRIC: ${dimensions[selectedDimension].uppercase()}",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "INTERACTIVE CANVASES",
                                color = Color(0xFF86EFAC),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Responsive Canvas with Tap Interaction
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF04070E))
                            .pointerInput(filteredSectors, selectedDimension, simulationTempOffset) {
                                detectTapGestures { tapOffset ->
                                    val w = size.width
                                    val targetPool = if (filteredSectors.isNotEmpty()) filteredSectors else baseSectors
                                    var closestSector = targetPool.first()
                                    var minDistance = Float.MAX_VALUE
                                    targetPool.forEach { sector ->
                                        val sectorX = sector.mapXRatio * w
                                        val dist = kotlin.math.abs(tapOffset.x - sectorX)
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestSector = sector
                                        }
                                    }
                                    selectedSectorId = closestSector.id
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        val trackY1 = h * 0.38f // UP Line
                        val trackY2 = h * 0.68f // DOWN Line

                        // Draw Ballast bed
                        drawRect(
                            color = Color(0xFF0C121E),
                            topLeft = Offset(0f, trackY1 - 20f),
                            size = androidx.compose.ui.geometry.Size(w, (trackY2 - trackY1) + 40f)
                        )

                        // Draw Ties (Sleepers)
                        val numTies = 32
                        for (i in 0..numTies) {
                            val tieX = (w / numTies) * i
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(tieX, trackY1 - 10f),
                                end = Offset(tieX, trackY2 + 10f),
                                strokeWidth = 2f
                            )
                        }

                        // Steel Rails
                        drawLine(
                            color = Color(0xFF475569),
                            start = Offset(0f, trackY1),
                            end = Offset(w, trackY1),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xFF475569),
                            start = Offset(0f, trackY2),
                            end = Offset(w, trackY2),
                            strokeWidth = 3f
                        )

                        // Draw Dynamic Heat Glows for each sector
                        baseSectors.forEach { sector ->
                            val metric = sector.getMetric(selectedDimension, simulationTempOffset)
                            val score = metric.score
                            val secX = sector.mapXRatio * w
                            val secY = if (sector.mapTrackLine == 1) trackY1 else trackY2
                            val isSelected = sector.id == selectedSectorId
                            val isMatchingFilter = filteredSectors.any { it.id == sector.id }

                            if (!isMatchingFilter) {
                                // Dimmed ghost indicator for sectors not in active filter
                                drawCircle(
                                    color = Color(0xFF334155).copy(alpha = 0.35f),
                                    radius = 3.5f,
                                    center = Offset(secX, secY)
                                )
                                return@forEach
                            }

                            val glowColor = when {
                                score >= 75 -> Color(0xFFEF4444)
                                score >= 50 -> Color(0xFFF97316)
                                score >= 25 -> Color(0xFFFBBF24)
                                else -> Color(0xFF22C55E)
                            }

                            val glowRadius = 30f + (score / 100f) * 45f

                            // Radial Heat Halo
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = if (isSelected) pulseAlpha else 0.55f),
                                        glowColor.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    center = Offset(secX, secY),
                                    radius = glowRadius
                                ),
                                radius = glowRadius,
                                center = Offset(secX, secY)
                            )

                            // Center core pin
                            drawCircle(
                                color = glowColor,
                                radius = if (isSelected) 7f else 5f,
                                center = Offset(secX, secY)
                            )

                            // White dot inside
                            drawCircle(
                                color = Color.White,
                                radius = 2f,
                                center = Offset(secX, secY)
                            )

                            // Targeting Reticle if Selected
                            if (isSelected) {
                                drawCircle(
                                    color = Color(0xFF38BDF8),
                                    radius = glowRadius * 0.8f,
                                    center = Offset(secX, secY),
                                    style = Stroke(
                                        width = 1.5f,
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                            floatArrayOf(8f, 6f),
                                            reticleAngle
                                        )
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Heatmap Scale Legend & Selected Reticle indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target: ${selectedSector.id} (${activeMetric.score}/100)",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("0-25 Low", color = Color(0xFF94A3B8), fontSize = 8.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFBBF24), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("26-50 Med", color = Color(0xFF94A3B8), fontSize = 8.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFF97316), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("51-74 High", color = Color(0xFF94A3B8), fontSize = 8.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("75+ Crit", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Active Selected Sector Telemetry Inspector Card
        item {
            SectionLabel(title = "INSPECT SELECTED SECTOR: ${selectedSector.name.uppercase()}")

            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedSector.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedSector.line} Line · ${selectedSector.chainage} (${selectedSector.lengthKm} km)",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        label = "${activeMetric.score}/100",
                        tone = activeMetric.tone
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dimension-specific headline callout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(toneColor(activeMetric.tone, isDark).copy(alpha = 0.12f))
                        .border(1.dp, toneColor(activeMetric.tone, isDark).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = activeMetric.headline,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = toneColor(activeMetric.tone, isDark)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeMetric.subtext,
                            fontSize = 11.sp,
                            color = colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                DetailRow(label = "Selected Metric Formula", value = activeMetric.formulaName)
                DetailRow(label = "Computed Calculation", value = activeMetric.formulaValue)
                DetailRow(label = "Active Defect Density", value = "${selectedSector.activeDefects} Flaws (${String.format("%.1f", selectedSector.activeDefects / selectedSector.lengthKm)}/km)")
                DetailRow(label = "Corridor Gross Tonnage", value = "${selectedSector.tonnageMgt} MGT")
                DetailRow(label = "Cab Speed Restriction", value = selectedSector.activeTsr)
                DetailRow(label = "Approaching Train Interlock", value = selectedSector.approachingTrain)
                DetailRow(label = "Remediation Directive", value = activeMetric.directive)

                Spacer(modifier = Modifier.height(10.dp))

                // Action to Interlock Cab
                if (cabInterlockAlertSent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ SPEED RESTRICTION & THERMAL ADVISORY TRANSMITTED TO CAB DMI",
                            color = Color(0xFF22C55E),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    PrimaryButton(
                        title = "Dispatch TSR Alert to ${selectedSector.approachingTrain.substringBefore(" ")}",
                        icon = Icons.Default.Send,
                        onClick = { cabInterlockAlertSent = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Filtered Corridor Sectors List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(title = "CORRIDOR SECTORS (${filteredSectors.size} MATCHING)")
                if (severityFilter != "All" || lineFilter != "All Lines") {
                    Text(
                        text = "Reset Filters",
                        fontSize = 11.sp,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                severityFilter = "All"
                                lineFilter = "All Lines"
                            }
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }
            }
        }

        if (filteredSectors.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.FilterAltOff,
                    title = "No Sectors Match Filters",
                    body = "Try selecting 'All' severity or 'All Lines' to view the entire corridor.",
                    action = "Reset Filters",
                    onAction = {
                        severityFilter = "All"
                        lineFilter = "All Lines"
                    }
                )
            }
        } else {
            items(filteredSectors) { sector ->
                val isSelected = sector.id == selectedSectorId
                val metric = sector.getMetric(selectedDimension, simulationTempOffset)

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) colorScheme.primary.copy(alpha = 0.12f) else colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSectorId = sector.id }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sector.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                            )
                            StatusPill(
                                label = "${dimensions[selectedDimension].take(7).uppercase()} ${metric.score}",
                                tone = metric.tone
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metric.headline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = toneColor(metric.tone, isDark)
                        )
                        Text(
                            text = "${sector.chainage} · ${sector.activeDefects} defects · ${sector.approachingTrain}",
                            fontSize = 10.sp,
                            color = colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { metric.score / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = toneColor(metric.tone, isDark),
                            trackColor = colorScheme.outline.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationDetailsScreen(onBack: () -> Unit) {
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
                title = "Section 14 Specifications",
                subtitle = "North Loop Line engineering parameters",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "INFRASTRUCTURE ASSET DATA",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Corridor", value = "North Loop Line (Up)")
                DetailRow(label = "Chainage", value = "14+000 to 15+250")
                DetailRow(label = "Rail Profile", value = "60E1 (UIC 60) Continuous Welded")
                DetailRow(label = "Sleeper Type", value = "G44 Prestressed Concrete")
                DetailRow(label = "Fastening", value = "Pandrol e-Clip / Fastclip")
                DetailRow(label = "Ballast Depth", value = "300 mm granite ballast")
                DetailRow(label = "Last Tamped", value = "14 days ago (Track machine T-04)")
                DetailRow(label = "Design Speed", value = "120 km/h (Restricted to 25 km/h)")
            }
        }
    }
}
