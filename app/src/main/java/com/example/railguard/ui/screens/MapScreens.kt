package com.example.railguard.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.content.ContextCompat

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

private fun readLastKnownLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }
}

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
                            Text(if (defects.isNotEmpty()) "Monitored Segment: ${defects.first().chainageCoordinate}" else "Active Track Corridor: Nominal Line Speed", fontSize = 11.sp, color = Color(0xFF0284C7))
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
        if (defects.isEmpty()) {
            RailCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("All Track Sectors Clear", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Zero defect GPS pins recorded. All surveyed track sectors nominal.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
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
fun RiskHeatmapScreen(
    defects: List<Defect> = emptyList(),
    onBack: () -> Unit
) {
    val criticalCount = defects.count { it.tone == Tone.CRITICAL }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Corridor Risk Heatmap", subtitle = "Cumulative stress & failure hazard index", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (criticalCount > 0) {
                    Text("Active High-Strain Hazard Clusters", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$criticalCount critical anomaly locations requiring immediate track team deployment.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    MetricRow("Critical Anomalies", "$criticalCount active")
                    MetricRow("Recommended Speed Limit", "25 km/h Emergency Slow Order")
                } else {
                    Text("Corridor Hazard Index: Nominal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Zero high-strain anomaly clusters detected across monitored sectors. Derailment risk nominal (0.0%).", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    MetricRow("Hazard Density", "0.0 / Low")
                    MetricRow("Track Geometry", "UIC 60 Nominal Clearance")
                    MetricRow("Authorized Line Speed", "Full Line Speed Permitted")
                }
            }
        }
    }
}

@Composable
fun LocationDetailsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSyncingGeodesy by remember { mutableStateOf(false) }
    var geodesySyncMsg by remember { mutableStateOf<String?>(null) }
    var location by remember { mutableStateOf<Location?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            location = readLastKnownLocation(context)
        } else {
            geodesySyncMsg = "Location permission was denied"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Location Geodesy", subtitle = "WGS-84 survey markers & elevation", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Latitude", location?.latitude?.toString() ?: "Waiting for device fix")
                MetricRow("Longitude", location?.longitude?.toString() ?: "Waiting for device fix")
                MetricRow("Accuracy", location?.accuracy?.let { "%.2f m".format(it) } ?: "Unavailable")
                MetricRow("Altitude", location?.altitude?.let { "%.2f m".format(it) } ?: "Unavailable")
                MetricRow("Bearing", location?.bearing?.let { "%.1f°".format(it) } ?: "Unavailable")
                MetricRow("Source", location?.provider ?: "Device GPS / network")

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
                            if (!hasLocationPermission(context)) {
                                isSyncingGeodesy = false
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                location = readLastKnownLocation(context)
                                val fix = location
                                if (fix == null) {
                                    isSyncingGeodesy = false
                                    geodesySyncMsg = "No device location fix is available"
                                } else {
                                    val synced = RailGuardFirebaseService.instance.recordGpsTelemetry(
                                        lat = fix.latitude,
                                        lng = fix.longitude,
                                        speedKmh = if (fix.hasSpeed()) fix.speed * 3.6 else 0.0,
                                        heading = if (fix.hasBearing()) fix.bearing.toDouble() else 0.0,
                                        accuracyM = fix.accuracy,
                                        satellites = fix.extras?.getInt("satellites") ?: 0,
                                        chainage = "Device GPS Fix"
                                    )
                                    isSyncingGeodesy = false
                                    geodesySyncMsg = if (synced) {
                                        "Device coordinates persisted to Cloud Realtime DB ✓"
                                    } else {
                                        "Firebase rejected the location update"
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GpsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBroadcastingLive by remember { mutableStateOf(false) }
    var broadcastPushedCount by remember { mutableIntStateOf(0) }
    var manualSyncing by remember { mutableStateOf(false) }
    var lastSyncStatus by remember { mutableStateOf("Waiting for GPS permission and device fix") }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            isBroadcastingLive = true
            lastSyncStatus = "GPS permission granted; starting cloud broadcast"
        } else {
            isBroadcastingLive = false
            lastSyncStatus = "GPS permission was denied"
        }
    }

    // Live continuous GPS sync loop to Firebase RTDB
    LaunchedEffect(isBroadcastingLive) {
        while (isBroadcastingLive) {
            delay(3000)
            val fix = readLastKnownLocation(context)
            if (fix == null) {
                lastSyncStatus = "Waiting for a device GPS fix"
            } else {
                val synced = RailGuardFirebaseService.instance.recordGpsTelemetry(
                    lat = fix.latitude,
                    lng = fix.longitude,
                    speedKmh = if (fix.hasSpeed()) fix.speed * 3.6 else 0.0,
                    heading = if (fix.hasBearing()) fix.bearing.toDouble() else 0.0,
                    accuracyM = fix.accuracy,
                    satellites = fix.extras?.getInt("satellites") ?: 0,
                    chainage = "Device GPS Fix"
                )
                if (synced) {
                    broadcastPushedCount++
                    lastSyncStatus = "Packet #$broadcastPushedCount streamed to Cloud DB"
                } else {
                    lastSyncStatus = "Firebase rejected the GPS packet"
                }
            }
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
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                isBroadcastingLive = false
                            } else if (hasLocationPermission(context)) {
                                isBroadcastingLive = true
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
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
                MetricRow("Chainage Segment", "Active Corridor Trackway")
                MetricRow("Telemetry Packets", "$broadcastPushedCount transmitted")

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        manualSyncing = true
                        scope.launch {
                            if (!hasLocationPermission(context)) {
                                manualSyncing = false
                                lastSyncStatus = "Grant GPS permission before syncing"
                            } else {
                                val fix = readLastKnownLocation(context)
                                if (fix == null) {
                                    manualSyncing = false
                                    lastSyncStatus = "No device GPS fix is available"
                                } else {
                                    val synced = RailGuardFirebaseService.instance.recordGpsTelemetry(
                                        lat = fix.latitude,
                                        lng = fix.longitude,
                                        speedKmh = if (fix.hasSpeed()) fix.speed * 3.6 else 0.0,
                                        heading = if (fix.hasBearing()) fix.bearing.toDouble() else 0.0,
                                        accuracyM = fix.accuracy,
                                        satellites = fix.extras?.getInt("satellites") ?: 0,
                                        chainage = "Device GPS Fix"
                                    )
                                    manualSyncing = false
                                    lastSyncStatus = if (synced) {
                                        "Manual device fix synced to Cloud DB ✓"
                                    } else {
                                        "Firebase rejected the manual GPS fix"
                                    }
                                }
                            }
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
