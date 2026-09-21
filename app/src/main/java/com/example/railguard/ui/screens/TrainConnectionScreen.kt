package com.example.railguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import kotlinx.coroutines.delay

@Composable
fun TrainConnectionScreen(
    onNavigateLiveScan: () -> Unit,
    onNavigateMap: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current

    var isConnected by remember { mutableStateOf(true) }
    var isStreamingLive by remember { mutableStateOf(true) }
    var packetCount by remember { mutableIntStateOf(14820) }
    var currentSpeed by remember { mutableFloatStateOf(118.4f) }
    var currentChainage by remember { mutableStateOf("14+320") }
    var currentTemp by remember { mutableFloatStateOf(28.4f) }
    var cabAlertDispatched by remember { mutableStateOf(false) }
    var alertBannerMessage by remember { mutableStateOf<String?>(null) }

    val liveLogEntries = remember {
        mutableStateListOf(
            "[14:32:14.200] LINK ESTABLISHED: 5G Train-to-Ground Radio -> TR-104 (IP: 10.142.8.50:50051)",
            "[14:32:14.450] SYNC: Locomotive OBU Atlas 200 Handshake OK · Latency 12ms",
            "[14:32:15.102] TX -> TR-104: {\"gps\": [51.50394, -0.12856], \"speed_kmh\": 118.4, \"chainage\": \"14+320\", \"gauge_mm\": 1438.2}",
            "[14:32:15.114] RX <- TR-104: {\"ack\": true, \"cab_sync\": \"TELEMETRY_LOCKED\"}",
            "[14:32:15.820] TX -> TR-104: {\"vibe_z_g\": 0.042, \"rail_temp_c\": 28.4, \"tsr_warning\": \"25_KMH_ACTIVE_14+320\"}",
            "[14:32:15.832] RX <- TR-104: {\"ack\": true, \"cab_dmi_display\": \"TSR_RESTRICTION_25_ACKNOWLEDGED\"}"
        )
    }

    val logListState = rememberLazyListState()

    // Real-time live data streaming simulation loop
    LaunchedEffect(isStreamingLive, isConnected) {
        while (isStreamingLive && isConnected) {
            delay(1200)
            packetCount += 6
            currentSpeed = (117.8f + (kotlin.random.Random.nextFloat() * 1.6f))
            currentTemp = (28.2f + (kotlin.random.Random.nextFloat() * 0.4f))
            val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            val newLog = "[$ts] TX -> TR-104: {\"gps\": [51.50394, -0.12856], \"spd\": ${String.format("%.1f", currentSpeed)}, \"vibe\": 0.041g, \"pkts\": $packetCount}"
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
                title = "Train Live Uplink",
                subtitle = "Connected to Train TR-104 · Real-time bidirectional telemetry",
                onBack = onBack
            )
        }

        // Connection Status Card
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isConnected) Color(0xFF0284C7) else Color(0xFF64748B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "TRAIN TR-104 (LOCOMOTIVE OBU)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "IP: 10.142.8.50:50051 · 5G Ground Link",
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
                                Text("${String.format("%.1f", currentSpeed)} km/h", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
                                Text("14+320 UP", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
                                Text("PACKETS SENT", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("$packetCount", color = Color(0xFF22C55E), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
                                text = "Auto-Send Telemetry:",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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

        // Real-Time Sensor Telemetry Sent to Train
        item {
            SectionLabel(title = "LIVE TELEMETRY STREAM SENT TO TRAIN")

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
                    text = "TRAIN POSITION & AXLE SENSOR ARRAY",
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
                        Text("51°30'14.2\"N 0°07'42.8\"W", color = colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("GNSS RTK LOCK:", color = colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("±1.2 cm Precision", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Wheel Bearing Infrared Temperature Sensors:\n" +
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

        // Dispatch Alert Directly to Train Driver
        item {
            SectionLabel(title = "TRAIN CAB DISPATCH CONTROLS")

            if (alertBannerMessage != null) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        cabAlertDispatched = true
                        alertBannerMessage = "SENT TO TR-104: 25 km/h Temporary Speed Restriction enforced on driver DMI screen!"
                        liveLogEntries.add("[MANUAL] TX -> TR-104: {\"emergency_tsr\": 25, \"chainage\": \"14+320\", \"reason\": \"CRK-2048_46MM\"}")
                        liveLogEntries.add("[MANUAL] RX <- TR-104: {\"cab_driver_ack\": true, \"speed_reduced_to\": 25}")
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send 25 km/h TSR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        alertBannerMessage = "SENT TO TR-104: Defect CRK-2048 coordinates & bounding box sent to cab computer."
                        liveLogEntries.add("[MANUAL] TX -> TR-104: {\"defect_target\": \"CRK-2048\", \"lat\": 51.50394, \"lon\": -0.12856}")
                        liveLogEntries.add("[MANUAL] RX <- TR-104: {\"map_pin_plotted\": true}")
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Defect Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Outbound Telemetry Log Console
        item {
            SectionLabel(title = "LIVE OUTBOUND PACKET CONSOLE (5G STREAM)")

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
                            text = "UDP PORT 50051 STREAM",
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

                    liveLogEntries.takeLast(8).forEach { entry ->
                        Text(
                            text = entry,
                            color = if (entry.contains("TX")) Color(0xFF7DD3FC) else if (entry.contains("RX")) Color(0xFF86EFAC) else Color(0xFFFDE047),
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
