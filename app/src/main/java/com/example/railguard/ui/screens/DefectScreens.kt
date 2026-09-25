package com.example.railguard.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import com.example.railguard.data.RailGuardFirebaseService
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.model.Defect
import com.example.railguard.model.ObservationItem
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun DefectsListScreen(
    defects: List<Defect>,
    onSelectDefect: (Defect) -> Unit,
    onViewObservations: () -> Unit,
    onOpenDefectMap: () -> Unit,
    onOpenCrackGrowth: () -> Unit
) {
    val isDark = LocalIsDark.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredDefects = defects.filter {
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) ||
                it.section.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Critical" -> it.tone == Tone.CRITICAL
            "Warning" -> it.tone == Tone.WARNING
            "Advisory" -> it.tone == Tone.INFO
            else -> true
        }
        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Defect Registry",
                subtitle = "${defects.size} active anomalies detected across corridors"
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("defect_search_input"),
                placeholder = { Text("Search by ID, crack type, or chainage...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Critical", "Warning", "Advisory").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenDefectMap,
                    modifier = Modifier.weight(1f).testTag("defect_map_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Geo-Plot", fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenCrackGrowth,
                    modifier = Modifier.weight(1f).testTag("crack_growth_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Growth AI", fontSize = 12.sp)
                }

                Button(
                    onClick = onViewObservations,
                    modifier = Modifier.weight(1f).testTag("observations_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Observations", fontSize = 12.sp)
                }
            }
        }

        items(filteredDefects, key = { it.id }) { defect ->
            DefectListItem(defect = defect, onClick = { onSelectDefect(defect) })
        }
    }
}

@Composable
fun DefectListItem(defect: Defect, onClick: () -> Unit) {
    val isDark = LocalIsDark.current
    val accentColor = toneColor(defect.tone, isDark)

    RailCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("defect_item_${defect.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = defect.id,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = defect.section,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = defect.time,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = defect.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = defect.detail,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        title = "Length: ${defect.estimatedLength}",
                        tone = Tone.NEUTRAL
                    )
                    StatusChip(
                        title = "Risk: ${defect.riskScore}/100",
                        tone = defect.tone
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DefectDetailsScreen(
    defect: Defect,
    onOpenMeasurement: () -> Unit,
    onOpenComparison: () -> Unit,
    onOpenObjectDetection: () -> Unit,
    onOpenAlignment: () -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenVerify: () -> Unit,
    onOpenComments: () -> Unit,
    onBack: () -> Unit
) {
    val isDark = LocalIsDark.current
    val accentColor = toneColor(defect.tone, isDark)
    val scope = rememberCoroutineScope()
    var isSyncingToFirebase by remember { mutableStateOf(false) }
    var firebaseSyncMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = defect.id,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    )
                    Text(
                        text = defect.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        // Firebase Cloud Sync Action
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFA000).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cloud Telemetry Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(firebaseSyncMessage ?: "Replicate defect to central safety cloud", fontSize = 10.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            isSyncingToFirebase = true
                            scope.launch {
                                RailGuardFirebaseService.instance.uploadDefectToFirebase(defect)
                                isSyncingToFirebase = false
                                firebaseSyncMessage = "Synchronized to Central Cloud!"
                            }
                        },
                        enabled = !isSyncingToFirebase,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (isSyncingToFirebase) "Pushing..." else "Sync Now", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            RailCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ANOMALY TELEMETRY & GPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MetricRow(label = "Corridor Section", value = defect.section)
                    MetricRow(label = "Chainage Marker", value = defect.chainageCoordinate)
                    MetricRow(label = "GPS Coordinates", value = "${defect.latitude} N, ${defect.longitude} E")
                    MetricRow(label = "Estimated Crack Size", value = defect.estimatedLength)
                    MetricRow(label = "AI Confidence", value = "${defect.aiConfidencePercent}% Match")
                    MetricRow(label = "Overall Risk Index", value = "${defect.riskScore} / 100")
                }
            }
        }

        item {
            RailCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI PRESCRIBED MITIGATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = defect.aiPrescribedAction,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        item {
            Text(
                text = "ENGINEERING TOOLS & VERIFICATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionToolTile(
                    title = "Crack Width & Depth Measurement",
                    desc = "Computer vision calibrated optical gauge",
                    icon = Icons.Default.SquareFoot,
                    onClick = onOpenMeasurement
                )
                ActionToolTile(
                    title = "Crack Growth Propagation Model",
                    desc = "Finite element strain & cycle prediction",
                    icon = Icons.Default.Timeline,
                    onClick = onOpenGrowth
                )
                ActionToolTile(
                    title = "Historical Image Comparison",
                    desc = "Side-by-side time-lapse delta analysis",
                    icon = Icons.Default.Compare,
                    onClick = onOpenComparison
                )
                ActionToolTile(
                    title = "Multi-Class Object Detection",
                    desc = "Segmented rail, sleeper, clips & ballast",
                    icon = Icons.Default.Layers,
                    onClick = onOpenObjectDetection
                )
                ActionToolTile(
                    title = "Track Alignment & Profile",
                    desc = "Horizontal gauge & cross-level deviation",
                    icon = Icons.Default.Tune,
                    onClick = onOpenAlignment
                )
                ActionToolTile(
                    title = "Field Inspector Comments",
                    desc = "Add operational notes & dispatch tags",
                    icon = Icons.Default.Comment,
                    onClick = onOpenComments
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            PrimaryButton(
                title = "Sign & Verify Defect Record",
                onClick = onOpenVerify,
                modifier = Modifier.testTag("verify_defect_action_btn")
            )
        }
    }
}

@Composable
fun ActionToolTile(
    title: String,
    desc: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val isDark = LocalIsDark.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    val isDark = LocalIsDark.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
    }
}

@Composable
fun CrackMeasurementScreen(onBack: () -> Unit) {
    val isDark = LocalIsDark.current
    val scope = rememberCoroutineScope()
    var isTransmitting by remember { mutableStateOf(false) }
    var transmitMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Crack Width & Depth", subtitle = "Sub-millimeter optical measurement", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Optical Sensor Geometry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                MetricRow("Crack Surface Aperture", "2.42 mm (±0.05 mm)")
                MetricRow("Estimated Depth Profile", "8.90 mm (Ultrasonic Verified)")
                MetricRow("Crack Propagation Angle", "45° Oblique to Rail Web")
                MetricRow("Stress Concentration K_t", "3.48")

                Spacer(modifier = Modifier.height(14.dp))
                if (transmitMsg != null) {
                    Text(transmitMsg ?: "", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PrimaryButton(
                    title = if (isTransmitting) "Uploading Analysis..." else "Sync Measurement to Cloud DB",
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        isTransmitting = true
                        scope.launch {
                            RailGuardFirebaseService.instance.recordAiAnalysis(
                                "CRACK_MEASUREMENT",
                                mapOf("apertureMm" to 2.42, "depthMm" to 8.90, "propagationAngle" to "45 deg", "kt" to 3.48)
                            )
                            isTransmitting = false
                            transmitMsg = "Measurement synced to Cloud Telemetry ✓"
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GrowthAnalysisScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isTransmitting by remember { mutableStateOf(false) }
    var transmitMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Growth Dynamics", subtitle = "Paris law fatigue crack propagation", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Simulated Cycles To Critical Limit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Under 25-ton axle load, estimated critical threshold reached in 1,200 freight cycles (~14 operational days).", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                MetricRow("Delta-K Stress Intensity", "24.6 MPa·m^(1/2)")
                MetricRow("Fatigue Life Remaining", "14 Days at Current Tonnage")
                MetricRow("Speed Restriction Required", "Yes (Max 30 km/h)")

                Spacer(modifier = Modifier.height(14.dp))
                if (transmitMsg != null) {
                    Text(transmitMsg ?: "", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PrimaryButton(
                    title = if (isTransmitting) "Syncing..." else "Sync Fatigue Profile to Cloud",
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        isTransmitting = true
                        scope.launch {
                            RailGuardFirebaseService.instance.recordAiAnalysis(
                                "FATIGUE_GROWTH_PREDICTION",
                                mapOf("deltaK" to 24.6, "remainingDays" to 14, "speedLimit" to 30)
                            )
                            isTransmitting = false
                            transmitMsg = "Fatigue simulation archived in Cloud DB ✓"
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ImageComparisonScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isTransmitting by remember { mutableStateOf(false) }
    var transmitMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Historical Comparison", subtitle = "30-Day image delta comparison", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Visual Delta Detection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Crack has extended by +4.2 mm since previous patrol on 24 August 2026.", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(14.dp))
                if (transmitMsg != null) {
                    Text(transmitMsg ?: "", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PrimaryButton(
                    title = if (isTransmitting) "Syncing..." else "Upload Delta Comparison to Cloud",
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        isTransmitting = true
                        scope.launch {
                            RailGuardFirebaseService.instance.recordAiAnalysis(
                                "IMAGE_DELTA_COMPARISON",
                                mapOf("extensionMm" to 4.2, "baselineDate" to "2026-08-24")
                            )
                            isTransmitting = false
                            transmitMsg = "Visual delta synced to Cloud DB ✓"
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ObjectDetectionScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isTransmitting by remember { mutableStateOf(false) }
    var transmitMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "AI Object Segmentation", subtitle = "Multi-class track components", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Rail Head & Web", "Detected (99% conf)")
                MetricRow("Sleeper Concrete Block", "Detected (96% conf)")
                MetricRow("Pandrol Fastener Clips", "1 Present, 1 Loose")
                MetricRow("Ballast Bed Level", "Normal Clearance")

                Spacer(modifier = Modifier.height(14.dp))
                if (transmitMsg != null) {
                    Text(transmitMsg ?: "", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PrimaryButton(
                    title = if (isTransmitting) "Syncing..." else "Push AI Segmentation to Cloud",
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        isTransmitting = true
                        scope.launch {
                            RailGuardFirebaseService.instance.recordAiAnalysis(
                                "AI_OBJECT_SEGMENTATION",
                                mapOf("railheadConf" to 0.99, "sleeperConf" to 0.96, "fastenerState" to "Loose")
                            )
                            isTransmitting = false
                            transmitMsg = "Segmentation telemetry saved to Cloud ✓"
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AlignmentAnalysisScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isTransmitting by remember { mutableStateOf(false) }
    var transmitMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Track Alignment", subtitle = "Gauge & cant geometric evaluation", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Standard Track Gauge", "1435.0 mm")
                MetricRow("Current Measured Gauge", "1438.2 mm (+3.2 mm)")
                MetricRow("Cross-Level Cant", "2.1 mm (Within Tolerance)")
                MetricRow("Twist over 3m Base", "1.4 mm/m")

                Spacer(modifier = Modifier.height(14.dp))
                if (transmitMsg != null) {
                    Text(transmitMsg ?: "", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PrimaryButton(
                    title = if (isTransmitting) "Syncing..." else "Upload Track Geometry to Cloud",
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        isTransmitting = true
                        scope.launch {
                            RailGuardFirebaseService.instance.recordAiAnalysis(
                                "TRACK_ALIGNMENT_GEOMETRY",
                                mapOf("gauge" to 1438.2, "cant" to 2.1, "twist" to 1.4)
                            )
                            isTransmitting = false
                            transmitMsg = "Track geometry saved to Cloud DB ✓"
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EngineerVerificationScreen(onSigned: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var engineerNotes by remember { mutableStateOf("") }
    var signedBy by remember { mutableStateOf("E. Chen, Lead Track Inspector #4092") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(
            title = "Engineer Sign-Off",
            subtitle = "Official safety audit certificate",
            onBack = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                onBack()
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Verification Authority", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = signedBy,
                    onValueChange = { signedBy = it },
                    label = { Text("Inspector Name & ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = engineerNotes,
                    onValueChange = { engineerNotes = it },
                    label = { Text("Engineering Audit Notes") },
                    placeholder = { Text("e.g. Verified with gauge caliper. Immediate weld scheduled.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    title = "Apply Cryptographic Field Signature",
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        scope.launch {
                            RailGuardFirebaseService.instance.logSafetyAuditEvent(
                                "ENGINEER_SIGNOFF",
                                mapOf("signedBy" to signedBy, "notes" to engineerNotes, "timestamp" to System.currentTimeMillis())
                            )
                        }
                        onSigned()
                    }
                )
            }
        }
    }
}

@Composable
fun CommentsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var newComment by remember { mutableStateOf("") }
    val comments = remember {
        mutableStateListOf(
            "Shift 1: Noticed light ballast vibration during 08:30 express train passage.",
            "Shift 2: Dispatch gang #3 notified for speed restriction flag installation."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(
            title = "Inspector Log Notes",
            subtitle = "Field observations & remarks",
            onBack = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                onBack()
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = newComment,
            onValueChange = { newComment = it },
            placeholder = { Text("Add comment or field observation...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    if (newComment.isNotBlank()) {
                        val textToSave = newComment
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        comments.add(0, textToSave)
                        newComment = ""
                        scope.launch {
                            val author = RailGuardFirebaseService.instance.currentUser?.email ?: "Lead Inspector"
                            RailGuardFirebaseService.instance.saveComment("DEF-2048", author, textToSave)
                        }
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Add")
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(comments) { comment ->
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = comment, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
fun AllObservationsScreen(observations: List<ObservationItem>, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isSyncingObs by remember { mutableStateOf(false) }
    var syncObsMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "All Observations", subtitle = "${observations.size} logged field observations", onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))

        if (syncObsMsg != null) {
            Text(syncObsMsg ?: "", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = {
                isSyncingObs = true
                scope.launch {
                    observations.forEach { obs ->
                        RailGuardFirebaseService.instance.saveObservation(obs)
                    }
                    isSyncingObs = false
                    syncObsMsg = "All observations pushed to Cloud DB ✓"
                }
            },
            enabled = !isSyncingObs,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isSyncingObs) "Syncing..." else "Sync Observations to Cloud Telemetry", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(observations, key = { it.id }) { obs ->
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = obs.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "${obs.chainage} · ${obs.time}", fontSize = 11.sp, color = Color.Gray)
                        }
                        StatusChip(title = obs.severity, tone = obs.tone)
                    }
                }
            }
        }
    }
}

@Composable
fun SubScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    val isDark = LocalIsDark.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}
