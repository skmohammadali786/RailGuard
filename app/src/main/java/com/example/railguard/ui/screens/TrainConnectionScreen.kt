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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LiveTrain(
    val id: String,
    val name: String,
    val trainType: String,
    val corridorSection: String,
    val chainage: String,
    val normalSpeedCap: Int,
    val ipAddress: String,
    val driverName: String,
    val obuType: String,
    val gpsCoords: String,
    var liveSpeed: Float,
    var activeTsr: Int? = null,
    var lastAlertReceived: String? = null
)

@Composable
fun TrainConnectionScreen(
    onNavigateLiveScan: () -> Unit,
    onNavigateMap: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current
    val scope = rememberCoroutineScope()

    // Fleet of distinct trains
    val trainFleet = remember {
        mutableStateListOf(
            LiveTrain(
                id = "TR-104",
                name = "InterCity Express 104",
                trainType = "High-Speed Passenger EMU",
                corridorSection = "Section 14 (North Loop)",
                chainage = "14+320 UP",
                normalSpeedCap = 125,
                ipAddress = "10.142.8.50:50051",
                driverName = "Capt. E. Vance",
                obuType = "Alstom Atlas 200 ETCS L2",
                gpsCoords = "51°30'14.2\"N 0°07'42.8\"W",
                liveSpeed = 118.4f
            ),
            LiveTrain(
                id = "FR-802",
                name = "Heavy Freight Hauler 802",
                trainType = "3,400T Diesel-Electric Freight",
                corridorSection = "Section 08 (South Freight Yard)",
                chainage = "08+140 DOWN",
                normalSpeedCap = 75,
                ipAddress = "10.142.8.62:50052",
                driverName = "Capt. M. Kowalski",
                obuType = "Siemens Trainguard 200",
                gpsCoords = "51°29'08.6\"N 0°08'12.4\"W",
                liveSpeed = 62.5f
            ),
            LiveTrain(
                id = "HS-301",
                name = "Arrow Bullet High-Speed 301",
                trainType = "High-Speed Inter-Corridor Shinkansen",
                corridorSection = "Section 03 (East High-Speed Junction)",
                chainage = "03+450 UP",
                normalSpeedCap = 200,
                ipAddress = "10.142.8.77:50053",
                driverName = "Capt. K. Tanaka",
                obuType = "Hitachi ETCS Level 2",
                gpsCoords = "51°31'22.0\"N 0°05'55.1\"W",
                liveSpeed = 184.2f
            ),
            LiveTrain(
                id = "RC-515",
                name = "Regional Metro Commuter 515",
                trainType = "Class 387 8-Car EMU",
                corridorSection = "Section 01 (West Deep Cut)",
                chainage = "01+890 DOWN",
                normalSpeedCap = 90,
                ipAddress = "10.142.8.91:50054",
                driverName = "Capt. L. Gomez",
                obuType = "Bombardier EBI Cab 2000",
                gpsCoords = "51°28'45.3\"N 0°11'04.2\"W",
                liveSpeed = 78.0f
            ),
            LiveTrain(
                id = "MOW-909",
                name = "Track Patrol & Ultrasonic Tamper 909",
                trainType = "Specialized Maintenance-of-Way",
                corridorSection = "Section 14 (North Loop Patrol)",
                chainage = "14+050 UP",
                normalSpeedCap = 40,
                ipAddress = "10.142.8.105:50055",
                driverName = "Eng. D. Ross",
                obuType = "Plasser Onboard Diagnostic",
                gpsCoords = "51°30'11.8\"N 0°07'38.5\"W",
                liveSpeed = 24.5f
            )
        )
    }

    var selectedTrainIndex by remember { mutableIntStateOf(0) }
    val activeTrain = trainFleet[selectedTrainIndex]

    var isConnected by remember { mutableStateOf(true) }
    var isStreamingLive by remember { mutableStateOf(true) }
    var autoDispatchDefectsToTrain by remember { mutableStateOf(true) }
    var packetCount by remember { mutableIntStateOf(14820) }
    var currentTemp by remember { mutableFloatStateOf(28.4f) }
    var alertBannerMessage by remember { mutableStateOf<String?>(null) }
    var alertBannerTone by remember { mutableStateOf(Tone.CRITICAL) }

    var isCalibratingEsp by remember { mutableStateOf(false) }
    var espCalibrationMsg by remember { mutableStateOf<String?>(null) }
    var isSyncingEspToCloud by remember { mutableStateOf(false) }
    var espCloudSyncMsg by remember { mutableStateOf<String?>(null) }

    val liveLogEntries = remember {
        mutableStateListOf(
            "[14:32:14.200] LINK ESTABLISHED: 5G Train-to-Ground Radio Fleet Gateway",
            "[14:32:14.450] ACTIVE ROSTER: 5 Connected Trains across 4 Corridor Sections",
            "[14:32:15.102] SYNC TR-104: OBU Atlas 200 Handshake OK · Latency 12ms",
            "[14:32:15.114] SYNC FR-802: Trainguard 200 Handshake OK · Latency 14ms",
            "[14:32:15.820] TX -> TR-104: {\"gps\": [51.50394, -0.12856], \"spd\": 118.4, \"chainage\": \"14+320\"}",
            "[14:32:15.832] RX <- TR-104: {\"ack\": true, \"cab_dmi\": \"TELEMETRY_LOCKED\"}"
        )
    }

    // Function to trigger auto-send of detected defect to the specific matching train
    fun autoDispatchDefectToTrain(defectId: String, defectName: String, sectionName: String, chainage: String, suggestedTsr: Int) {
        val targetTrain = trainFleet.find { it.corridorSection.contains(sectionName.take(10)) } ?: activeTrain
        val trainIdx = trainFleet.indexOf(targetTrain)

        // Update train TSR and decelerate speed
        targetTrain.activeTsr = suggestedTsr
        targetTrain.lastAlertReceived = "AUTO-DISPATCH: $defectId $defectName · TSR $suggestedTsr km/h"
        targetTrain.liveSpeed = suggestedTsr.toFloat()
        if (trainIdx >= 0) {
            trainFleet[trainIdx] = targetTrain
        }

        alertBannerTone = Tone.CRITICAL
        alertBannerMessage = "AUTO-DISPATCH EXECUTED: Defect $defectId detected in $sectionName! Interlocked with ${targetTrain.id} (${targetTrain.name}) — Speed restricted to $suggestedTsr km/h. Driver ${targetTrain.driverName} acknowledged."

        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        liveLogEntries.add("[$ts] 🚨 [AUTO-DISPATCH] Defect $defectId detected at $chainage in $sectionName")
        liveLogEntries.add("[$ts] TX -> ${targetTrain.id} OBU: {\"event\": \"EMERGENCY_INTERLOCK\", \"defect\": \"$defectId\", \"tsr_kmh\": $suggestedTsr, \"chainage\": \"$chainage\"}")
        liveLogEntries.add("[$ts] RX <- ${targetTrain.id} CAB: {\"ack\": true, \"driver\": \"${targetTrain.driverName}\", \"service_brake\": \"APPLIED\", \"speed_target\": $suggestedTsr}")

        // Switch active view to the targeted train if not currently viewed
        selectedTrainIndex = trainIdx.coerceAtLeast(0)

        // Push live TSR command to Firebase Realtime Database
        scope.launch {
            com.example.railguard.data.RailGuardFirebaseService.instance.dispatchTsrToFirebase(
                trainId = targetTrain.id,
                tsrSpeedKmH = suggestedTsr,
                reason = "Auto-Dispatch: $defectId detected at $chainage ($sectionName)"
            )
        }
    }

    // Real-time live data streaming loop synced with cloud backend
    LaunchedEffect(isStreamingLive, isConnected) {
        var cycleCounter = 0
        while (isStreamingLive && isConnected) {
            delay(1200)
            packetCount += 6
            cycleCounter++
            currentTemp = (28.2f + (kotlin.random.Random.nextFloat() * 0.4f))

            // Jitter current speeds based on active TSR
            trainFleet.forEachIndexed { idx, tr ->
                val base = tr.activeTsr?.toFloat() ?: tr.normalSpeedCap.toFloat()
                val targetSpeed = base * 0.95f + (kotlin.random.Random.nextFloat() * 2.0f)
                trainFleet[idx] = tr.copy(liveSpeed = targetSpeed)
            }

            // Sync live ESP32 & Train telemetry every 3 cycles to cloud RTDB
            if (cycleCounter % 3 == 0) {
                com.example.railguard.data.RailGuardFirebaseService.instance.uploadEspSensorData(
                    nodeId = "ESP32-TRACK-01",
                    ultrasonicDepthMm = 46.2f,
                    vibrationG = 0.041f,
                    railTempC = currentTemp,
                    axleSpeedKmh = activeTrain.liveSpeed,
                    chainage = activeTrain.chainage,
                    status = "LIVE_TELEMETRY_LINKED"
                )
                com.example.railguard.data.RailGuardFirebaseService.instance.saveTrainTelemetry(
                    trainId = activeTrain.id,
                    name = activeTrain.name,
                    speed = activeTrain.liveSpeed,
                    tsr = activeTrain.activeTsr,
                    section = activeTrain.corridorSection,
                    chainage = activeTrain.chainage,
                    status = if (activeTrain.activeTsr != null) "TSR_RESTRICTED" else "TRACK_RUN_NOMINAL"
                )
            }

            val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            val newLog = "[$ts] TX -> ${activeTrain.id} & ESP32: {\"spd\": ${String.format("%.1f", activeTrain.liveSpeed)}, \"temp\": ${String.format("%.1f", currentTemp)}°C, \"vibe\": 0.041g, \"tsr\": ${activeTrain.activeTsr ?: "NONE"}, \"pkts\": $packetCount}"
            liveLogEntries.add(newLog)
            if (liveLogEntries.size > 25) {
                liveLogEntries.removeAt(0)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LinkPulse")
    val linkPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
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
                title = "Train Telemetry & Fleet Interlock",
                subtitle = "Multi-train ground-to-cab link with automatic defect dispatching",
                onBack = onBack
            )
        }

        // Fleet Train Selector Ribbon
        item {
            SectionLabel(title = "CONNECTED TRAIN FLEET (SELECT ACTIVE CAB)")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trainFleet.forEachIndexed { idx, train ->
                    val isSelected = selectedTrainIndex == idx
                    val hasTsr = train.activeTsr != null

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) colorScheme.primary else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier
                            .width(170.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTrainIndex = idx }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colorScheme.primary else if (hasTsr) Color(0xFFEF4444) else colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = train.id,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (hasTsr) Color(0xFFDC2626) else Color(0xFF16A34A))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (hasTsr) "TSR ${train.activeTsr}" else "OK",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = train.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                color = if (isSelected) colorScheme.onPrimary.copy(alpha = 0.9f) else colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format("%.1f", train.liveSpeed)} km/h",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) colorScheme.onPrimary else colorScheme.primary
                                )
                                Text(
                                    text = train.corridorSection.take(10),
                                    fontSize = 9.sp,
                                    color = if (isSelected) colorScheme.onPrimary.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Active Selected Train Connection Status Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFF0C2A44)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isConnected) Color(0xFF22C55E).copy(alpha = linkPulse) else Color(0xFFEF4444),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isConnected) Color(0xFF0284C7) else Color(0xFF64748B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${activeTrain.id} · ${activeTrain.name.uppercase()}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "IP: ${activeTrain.ipAddress} · 5G Ground Link",
                                    color = Color(0xFF7DD3FC),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Connected Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isConnected) Color(0xFF16A34A).copy(alpha = 0.2f) else Color(0xFFDC2626).copy(alpha = 0.2f))
                                .border(1.dp, if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                color = if (isConnected) Color(0xFF4ADE80) else Color(0xFFFCA5A5),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Key telemetry badges row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0B131F))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("LIVE SPEED", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("${String.format("%.1f", activeTrain.liveSpeed)} km/h", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0B131F))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("CHAINAGE", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(activeTrain.chainage, color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0B131F))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("ACTIVE TSR", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (activeTrain.activeTsr != null) "${activeTrain.activeTsr} km/h" else "CLEAR",
                                    color = if (activeTrain.activeTsr != null) Color(0xFFEF4444) else Color(0xFF22C55E),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Live 5G Uplink:",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = isStreamingLive && isConnected,
                                onCheckedChange = { isStreamingLive = it },
                                enabled = isConnected
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                isConnected = !isConnected
                                if (isConnected) isStreamingLive = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (isConnected) "Disconnect Link" else "Reconnect",
                                fontSize = 10.sp,
                                color = if (isConnected) Color(0xFFFCA5A5) else Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ESP32 HARDWARE SENSOR HUB & CLOUD RTDB TRANSDUCER ARRAY
        item {
            SectionLabel(title = "ESP32 TRACK SENSOR HUB & TRANSDUCER TELEMETRY")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ESP32-TRACK-01 HUB",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Xtensa 240MHz · WiFi/5G Gateway to RTDB",
                                    fontSize = 10.sp,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }

                        StatusPill(label = "CLOUD STREAMING", tone = Tone.HEALTHY)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-Quadrant Sensor Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("ULTRASONIC UT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                Text("46.2 mm", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444), fontFamily = FontFamily.Monospace)
                                Text("Flaw Peak Echo", fontSize = 8.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("VIBRATION RMS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                Text("0.041g", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF22C55E), fontFamily = FontFamily.Monospace)
                                Text("ADXL345 3-Axis", fontSize = 8.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("RAILHEAD TEMP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                Text("${String.format("%.1f", currentTemp)}°C", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF59E0B), fontFamily = FontFamily.Monospace)
                                Text("MLX90614 Contact", fontSize = 8.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("AXLE TACHOMETER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                                Text("${String.format("%.1f", activeTrain.liveSpeed)} km/h", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                                Text("Hall Effect Wheel", fontSize = 8.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (espCloudSyncMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = espCloudSyncMsg ?: "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (espCalibrationMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = espCalibrationMsg ?: "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isSyncingEspToCloud = true
                                scope.launch {
                                    val ok = com.example.railguard.data.RailGuardFirebaseService.instance.uploadEspSensorData(
                                        nodeId = "ESP32-TRACK-01",
                                        ultrasonicDepthMm = 46.2f,
                                        vibrationG = 0.041f,
                                        railTempC = currentTemp,
                                        axleSpeedKmh = activeTrain.liveSpeed,
                                        chainage = activeTrain.chainage,
                                        status = "MANUAL_BURST_SYNC"
                                    )
                                    isSyncingEspToCloud = false
                                    espCloudSyncMsg = if (ok) "✓ ESP32 sensor telemetry packet synced to Cloud RTDB (/railguard/esp_sensors)" else "Sync queued in local telemetry buffer"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSyncingEspToCloud) "Transmitting..." else "Push ESP32 Frame", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                isCalibratingEsp = true
                                scope.launch {
                                    val ok = com.example.railguard.data.RailGuardFirebaseService.instance.sendEspCalibrationCommand(
                                        nodeId = "ESP32-TRACK-01",
                                        command = "ZERO_CALIBRATE_TRANSDUCERS"
                                    )
                                    isCalibratingEsp = false
                                    espCalibrationMsg = if (ok) "✓ ESP32 zero-drift calibration command dispatched to node" else "Calibration command logged"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isCalibratingEsp) "Calibrating..." else "Zero Sensor Drift", fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // AUTO-DISPATCH ON DEFECT DETECTION ENGINE
        item {
            SectionLabel(title = "AUTOMATIC DEFECT DETECTION → CAB DISPATCH")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E1E2E) else Color(0xFFEFF6FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTO-TRANSMIT DEFECTS TO SPECIFIC TRAIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                            Text(
                                text = "When defects are detected in a sector, real-time TSR and flaw coordinates auto-transmit to the train in that corridor sector.",
                                fontSize = 10.sp,
                                color = colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }

                        Switch(
                            checked = autoDispatchDefectsToTrain,
                            onCheckedChange = { autoDispatchDefectsToTrain = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "TRIGGER REAL-TIME FIELD DEFECT DETECTION (TEST AUTO-SEND):",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val defectTriggers = listOf(
                        Tuple5("CRK-2048", "Transverse Crack (46mm)", "Section 14 (North Loop)", "14+320 UP", 25),
                        Tuple5("SQT-1092", "Squat Surface Spall (22mm)", "Section 08 (South Freight Yard)", "08+140 DOWN", 40),
                        Tuple5("GAU-0301", "Dynamic Gauge Spread (+9.4mm)", "Section 03 (East High-Speed)", "03+450 UP", 60),
                        Tuple5("BLT-0144", "Fishplate Web Fatigue Crack", "Section 01 (West Deep Cut)", "01+890 DOWN", 35)
                    )

                    defectTriggers.forEach { (defId, defName, sec, chn, tsr) ->
                        OutlinedButton(
                            onClick = {
                                autoDispatchDefectToTrain(defId, defName, sec, chn, tsr)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(text = "$defId · $defName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                        Text(text = "Target Sector: $sec ($chn)", fontSize = 9.sp, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFDC2626).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "Auto-Send $tsr km/h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Dispatch Alert Banner (if any)
        if (alertBannerMessage != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFDC2626), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = alertBannerMessage ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Real-Time Sensor Telemetry Sent to Active Train
        item {
            SectionLabel(title = "LIVE TELEMETRY STREAM SENT TO ${activeTrain.id}")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "1,438.2 mm",
                    label = "Dynamic Gauge",
                    tone = Tone.INFO,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "${String.format("%.1f", currentTemp)}°C",
                    label = "Rail Surface Temp",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "0.042g",
                    label = "Vertical Accel",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Geolocation and Axle Telemetry Card
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "${activeTrain.id} CAB & AXLE SENSOR ARRAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("LATITUDE / LONGITUDE:", color = colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(activeTrain.gpsCoords, color = colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("ASSIGNED DRIVER:", color = colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(activeTrain.driverName, color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Onboard Unit: ${activeTrain.obuType}\n" +
                        "Assigned Corridor: ${activeTrain.corridorSection} · Chainage ${activeTrain.chainage}\n" +
                        "• Locomotive Axle 1 (Front Bogie): 31.8°C · Nominal\n" +
                        "• Locomotive Axle 2: 32.4°C · Nominal\n" +
                        "• Locomotive Axle 3 (Rear Bogie): 33.1°C · Nominal",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Manual Cab Dispatch Buttons
        item {
            SectionLabel(title = "MANUAL CAB OVERRIDE CONTROLS (${activeTrain.id})")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        activeTrain.activeTsr = 25
                        activeTrain.liveSpeed = 25.0f
                        alertBannerTone = Tone.CRITICAL
                        alertBannerMessage = "SENT TO ${activeTrain.id}: 25 km/h Temporary Speed Restriction enforced on driver DMI screen!"
                        liveLogEntries.add("[MANUAL] TX -> ${activeTrain.id}: {\"emergency_tsr\": 25, \"chainage\": \"${activeTrain.chainage}\", \"reason\": \"MANUAL_SAFETY_INTERLOCK\"}")
                        liveLogEntries.add("[MANUAL] RX <- ${activeTrain.id}: {\"cab_driver_ack\": true, \"speed_reduced_to\": 25}")
                        scope.launch {
                            com.example.railguard.data.RailGuardFirebaseService.instance.dispatchTsrToFirebase(
                                trainId = activeTrain.id,
                                tsrSpeedKmH = 25,
                                reason = "Manual Cab Override TSR (Emergency Interlock)"
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Force 25 TSR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        activeTrain.activeTsr = null
                        alertBannerTone = Tone.HEALTHY
                        alertBannerMessage = "SENT TO ${activeTrain.id}: Speed restriction cleared. Train authorized for line speed."
                        liveLogEntries.add("[MANUAL] TX -> ${activeTrain.id}: {\"clear_tsr\": true, \"line_speed\": ${activeTrain.normalSpeedCap}}")
                        liveLogEntries.add("[MANUAL] RX <- ${activeTrain.id}: {\"ack\": true, \"resuming_line_speed\": ${activeTrain.normalSpeedCap}}")
                        scope.launch {
                            com.example.railguard.data.RailGuardFirebaseService.instance.dispatchTsrToFirebase(
                                trainId = activeTrain.id,
                                tsrSpeedKmH = 0,
                                reason = "TSR Cleared. Normal Line Speed Authorized."
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear TSR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Outbound Telemetry Log Console
        item {
            SectionLabel(title = "LIVE OUTBOUND PACKET CONSOLE (5G FLEET STREAM)")

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A0F1D)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UDP 5G RADIO STREAM · ${activeTrain.id}",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Bitrate: 256 kbps · Loss: 0.0%",
                            color = Color(0xFF4ADE80),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    liveLogEntries.takeLast(10).forEach { entry ->
                        Text(
                            text = entry,
                            color = if (entry.contains("AUTO-DISPATCH")) Color(0xFFF87171) else if (entry.contains("TX")) Color(0xFF7DD3FC) else if (entry.contains("RX")) Color(0xFF86EFAC) else Color(0xFFFDE047),
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) Color(0xFF0284C7)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
    }
}

data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
