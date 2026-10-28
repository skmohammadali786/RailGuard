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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
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
    defects: List<Defect>,
    telemetry: RailGuardFirebaseService.LiveSensorTelemetry? = null
) {
    val isDark = LocalIsDark.current
    val currentChainage = telemetry?.chainage?.takeIf { it != "Unknown" && it.isNotBlank() }
        ?: if (defects.isNotEmpty()) defects.first().chainageCoordinate else "KM 14+320"
    val axleSpeed = telemetry?.axleSpeedKmh ?: 45.0f
    val railTemp = telemetry?.railTempC ?: 31.4f
    val ultrasonicDepth = telemetry?.ultrasonicDepthMm ?: 68.2f
    val statusText = telemetry?.status ?: "ACTIVE_SYNC"
    val isAnomaly = statusText.contains("ANOMALY", ignoreCase = true) || ultrasonicDepth < 55.0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Corridor GIS & Live Track",
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
                    Text("Defect Pins", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToHeatmap,
                    modifier = Modifier.weight(1f).testTag("risk_heatmap_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE2E8F0),
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEA580C))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Risk Heatmap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Corridor HUD Card
        item {
            RailCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isAnomaly) Color(0xFFEF4444) else Color(0xFF16A34A))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CORRIDOR SECTOR 4B TELEMETRY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF0284C7)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isAnomaly) Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else Color(0xFF16A34A).copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isAnomaly) "ANOMALY FLAGGED" else "PATROL LOCK · NOMINAL",
                                fontSize = 10.sp,
                                color = if (isAnomaly) Color(0xFFDC2626) else Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current Chainage", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(currentChainage, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Column {
                            Text("Line Speed", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("${axleSpeed.toInt()} km/h", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Column {
                            Text("Railhead Temp", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("%.1f °C".format(railTemp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Column {
                            Text("UT Depth", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                "%.1f mm".format(ultrasonicDepth),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ultrasonicDepth < 55.0f) Color(0xFFDC2626) else Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interactive Railway Track Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .clickable { onNavigateToLocationDetails() }
                            .padding(12.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val upperRailY = h * 0.44f
                            val lowerRailY = h * 0.58f

                            // Draw sleeper ties
                            val tieCount = 14
                            val tieSpacing = w / (tieCount + 1)
                            for (i in 1..tieCount) {
                                val x = i * tieSpacing
                                drawLine(
                                    color = Color(0xFFCBD5E1),
                                    start = Offset(x, upperRailY - 14f),
                                    end = Offset(x, lowerRailY + 14f),
                                    strokeWidth = 7f
                                )
                            }

                            // Draw two parallel steel rails
                            drawLine(
                                color = Color(0xFF475569),
                                start = Offset(0f, upperRailY),
                                end = Offset(w, upperRailY),
                                strokeWidth = 5f
                            )
                            drawLine(
                                color = Color(0xFF475569),
                                start = Offset(0f, lowerRailY),
                                end = Offset(w, lowerRailY),
                                strokeWidth = 5f
                            )

                            // Chainage tick markers along track
                            val ticks = listOf("KM 14+000", "KM 14+200", "KM 14+400", "KM 14+600", "KM 14+800")
                            ticks.forEachIndexed { idx, _ ->
                                val tx = (idx + 1) * (w / 6f)
                                drawCircle(
                                    color = Color(0xFF94A3B8),
                                    radius = 3.5f,
                                    center = Offset(tx, upperRailY - 24f)
                                )
                            }

                            // Train / Patrol Cart position (approx 45% along corridor)
                            val trainX = w * 0.45f
                            val trainY = (upperRailY + lowerRailY) / 2f

                            // Outer pulse
                            drawCircle(
                                color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                radius = 18f,
                                center = Offset(trainX, trainY)
                            )
                            // Train vehicle icon body
                            drawCircle(
                                color = Color(0xFF0284C7),
                                radius = 10f,
                                center = Offset(trainX, trainY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4f,
                                center = Offset(trainX, trainY)
                            )

                            // Defect marker at KM 14+320
                            if (defects.isNotEmpty() || isAnomaly) {
                                val defX = w * 0.58f
                                val defY = upperRailY
                                drawCircle(
                                    color = Color(0xFFEF4444).copy(alpha = 0.3f),
                                    radius = 16f,
                                    center = Offset(defX, defY)
                                )
                                drawCircle(
                                    color = Color(0xFFEF4444),
                                    radius = 8f,
                                    center = Offset(defX, defY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3f,
                                    center = Offset(defX, defY)
                                )
                            }
                        }

                        // Overlay labels
                        Column(modifier = Modifier.align(Alignment.TopStart)) {
                            Text("Active Track Corridor: KM 14+000 → KM 14+800", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            Text("Patrol Cart at $currentChainage", fontSize = 10.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                        }

                        // Chainage Ticks Label Overlay at Top
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 28.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("KM 14+000", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            Text("KM 14+200", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            Text("KM 14+400", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            Text("KM 14+600", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            Text("KM 14+800", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                        }

                        // Clickable Defect Badges
                        if (defects.isNotEmpty()) {
                            Row(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                defects.take(3).forEach { d ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(toneColor(d.tone, isDark))
                                            .clickable { onSelectDefect(d) }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("${d.id}: ${d.chainageCoordinate}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Tap map canvas to open satellite geodesy & coordinates →",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
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
                    MetricRow("Recommended Speed Limit", "Emergency Slow Order Active")
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
                MetricRow("Tracked Satellites", "Awaiting GPS")
                MetricRow("HDOP / VDOP", "N/A")
                MetricRow("Ground Speed", "Awaiting GPS")
                MetricRow("Magnetic Heading", "N/A")
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
