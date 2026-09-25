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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.railguard.data.RailGuardFirebaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.model.Defect
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun MapScreen(
    onNavigateToDefectMap: () -> Unit,
    onNavigateToHeatmap: () -> Unit,
    onNavigateToLocationDetails: () -> Unit,
    onSelectDefect: (Defect) -> Unit,
    defects: List<Defect>
) {
    val isDark = LocalIsDark.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Corridor GIS & Live GPS",
                subtitle = "Sub-meter satellite mapping and track defect geolocation"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToDefectMap,
                    modifier = Modifier.weight(1f).testTag("defect_geo_plot_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Defect Pins", fontSize = 12.sp)
                }

                Button(
                    onClick = onNavigateToHeatmap,
                    modifier = Modifier.weight(1f).testTag("risk_heatmap_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Risk Heatmap", fontSize = 12.sp)
                }
            }
        }

        // Live Simulated Track Map Canvas
        item {
            RailCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECTOR 4B LIVE GPS GEO-FENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0284C7)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF16A34A).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("DGPS RTK LOCK · 1cm", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated Map Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF090D15) else Color(0xFFE2E8F0))
                            .border(1.dp, if (isDark) Color(0xFF1E2D44) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            .clickable { onNavigateToLocationDetails() }
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("● North Line Trackway (KM 38 → KM 44)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Current Hardware Cart: KM 42+180 (Speed: 14.2 km/h)", fontSize = 11.sp, color = Color(0xFF0284C7))
                        }

                        // Defect pin badges
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            defects.take(3).forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(toneColor(d.tone, isDark))
                                        .clickable { onSelectDefect(d) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(d.id, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "Tap for high-precision satellite coordinates",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "FLAGGED GEOLOCATION PINS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }

        items(defects) { defect ->
            RailCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectDefect(defect) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(toneColor(defect.tone, isDark).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = toneColor(defect.tone, isDark), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(defect.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${defect.chainageCoordinate} · ${defect.latitude} N, ${defect.longitude} E", fontSize = 11.sp, color = Color.Gray)
                    }
                    StatusChip(title = "${defect.riskScore}/100", tone = defect.tone)
                }
            }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Defect Geo-Plot", subtitle = "Pinpoint inspection anomaly coordinates", onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNavigateToGps, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) {
                Text("Sensor GPS Telemetry")
            }
            Button(onClick = onNavigateToHeatmap) {
                Text("View Heatmap")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(defects) { d ->
                RailCard(modifier = Modifier.fillMaxWidth().clickable { onSelectDefect(d) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(d.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(d.chainageCoordinate, fontSize = 12.sp, color = Color(0xFF0284C7))
                        Text("Lat: ${d.latitude}, Lng: ${d.longitude}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun RiskHeatmapScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Corridor Risk Heatmap", subtitle = "Cumulative stress & failure hazard index", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sector 4B: Red Hazard Cluster", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
                Text("3 high-strain fissures concentrated within 200 meters near KM 42+180. Derailment risk probability: 8.4%.", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                MetricRow("Highest Risk Segment", "KM 42+100 to KM 42+300")
                MetricRow("Axle Load Stress", "28.5 Tons / Axle")
                MetricRow("Speed Restriction Advice", "20 km/h Emergency Slow Order")
            }
        }
    }
}

@Composable
fun LocationDetailsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isSyncingGeodesy by remember { mutableStateOf(false) }
    var geodesySyncMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Location Geodesy", subtitle = "WGS-84 survey markers & elevation", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Latitude", "28.613938° N")
                MetricRow("Longitude", "77.209021° E")
                MetricRow("Ellipsoidal Height", "+216.42 m")
                MetricRow("Geoid Separation", "-32.10 m")
                MetricRow("Track Gradient", "+0.45% Rising Grade")
                MetricRow("Nearest Station", "Central Junction (3.8 km)")

                Spacer(modifier = Modifier.height(16.dp))

                if (geodesySyncMsg != null) {
                    Text(
                        text = geodesySyncMsg ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PrimaryButton(
                    title = if (isSyncingGeodesy) "Syncing Geodesy..." else "Transmit Survey Markers to Cloud",
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        isSyncingGeodesy = true
                        scope.launch {
                            RailGuardFirebaseService.instance.recordGpsTelemetry(
                                lat = 28.613938,
                                lng = 77.209021,
                                speedKmh = 14.2,
                                heading = 142.0,
                                accuracyM = 0.52f,
                                satellites = 18,
                                chainage = "14+320 Up Line"
                            )
                            isSyncingGeodesy = false
                            geodesySyncMsg = "Survey coordinates persisted to Cloud Realtime DB ✓"
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GpsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isBroadcastingLive by remember { mutableStateOf(true) }
    var broadcastPushedCount by remember { mutableIntStateOf(1) }
    var manualSyncing by remember { mutableStateOf(false) }
    var lastSyncStatus by remember { mutableStateOf("Live Cloud Broadcast Active") }

    // Live continuous GPS sync loop to Firebase RTDB
    LaunchedEffect(isBroadcastingLive) {
        while (isBroadcastingLive) {
            delay(3000)
            RailGuardFirebaseService.instance.recordGpsTelemetry(
                lat = 28.613938 + (kotlin.random.Random.nextDouble() - 0.5) * 0.0001,
                lng = 77.209021 + (kotlin.random.Random.nextDouble() - 0.5) * 0.0001,
                speedKmh = 14.2 + (kotlin.random.Random.nextDouble() - 0.5) * 0.4,
                heading = 142.0,
                accuracyM = 0.78f,
                satellites = 18,
                chainage = "14+320 Up Line"
            )
            broadcastPushedCount++
            lastSyncStatus = "Packet #$broadcastPushedCount streamed to Cloud DB"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "GPS Receiver Telemetry", subtitle = "Multi-constellation GNSS & Cloud Broadcast", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))

        // Cloud Broadcast Status Banner
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isBroadcastingLive) Color(0xFF16A34A) else Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud Telemetry Uplink",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = isBroadcastingLive,
                        onCheckedChange = { isBroadcastingLive = it }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lastSyncStatus,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF0F766E)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Constellations", "GPS + GLONASS + Galileo")
                MetricRow("Tracked Satellites", "18 In View / 14 Used")
                MetricRow("HDOP / VDOP", "0.78 / 1.12 (High Precision)")
                MetricRow("Ground Speed", "14.2 km/h (Cart Inspection Speed)")
                MetricRow("Magnetic Heading", "142° SE")
                MetricRow("Chainage Segment", "14+320 Up Line")
                MetricRow("Telemetry Packets", "$broadcastPushedCount transmitted")

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        manualSyncing = true
                        scope.launch {
                            RailGuardFirebaseService.instance.recordGpsTelemetry(
                                lat = 28.613938,
                                lng = 77.209021,
                                speedKmh = 14.2,
                                heading = 142.0,
                                accuracyM = 0.78f,
                                satellites = 18,
                                chainage = "14+320 Up Line"
                            )
                            manualSyncing = false
                            lastSyncStatus = "Manual Fix Synced to Cloud DB ✓"
                        }
                    },
                    enabled = !manualSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (manualSyncing) "Transmitting..." else "Broadcast Immediate Fix to Cloud", fontSize = 12.sp)
                }
            }
        }
    }
}
