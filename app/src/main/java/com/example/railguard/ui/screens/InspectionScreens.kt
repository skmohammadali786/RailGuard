package com.example.railguard.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.railguard.components.*
import com.example.railguard.model.InspectionRecord
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun InspectionsListScreen(
    inspections: List<InspectionRecord>,
    onSelectInspection: (InspectionRecord) -> Unit,
    onStartNew: () -> Unit,
    onViewCalendar: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    var filterIndex by remember { mutableIntStateOf(0) }
    val filterLabels = listOf("All", "In progress", "Planned", "Completed")

    val filteredInspections = remember(filterIndex, inspections) {
        when (filterIndex) {
            1 -> inspections.filter { it.status == "In progress" }
            2 -> inspections.filter { it.status == "Planned" }
            3 -> inspections.filter { it.status == "Completed" }
            else -> inspections
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
                title = "Inspections",
                subtitle = "Active patrols and scheduled verification passes",
                onBack = onBack
            )
        }

        item {
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    title = "Start live inspection",
                    icon = Icons.Default.PlayArrow,
                    onClick = onStartNew,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onViewCalendar,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondary,
                        contentColor = colorScheme.onSecondary
                    ),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterLabels.forEachIndexed { index, label ->
                    val isSelected = filterIndex == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) colorScheme.primary else colorScheme.surface)
                            .border(
                                1.dp,
                                if (isSelected) colorScheme.primary else colorScheme.outline,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { filterIndex = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(filteredInspections) { insp ->
            val statusTone = when (insp.status) {
                "In progress" -> Tone.INFO
                "Completed" -> Tone.HEALTHY
                else -> Tone.NEUTRAL
            }

            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onSelectInspection(insp) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = insp.id,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    StatusPill(label = insp.status.uppercase(), tone = statusTone)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = insp.section,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Chainage: ${insp.startChainage} → ${insp.endChainage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Inspector: ${insp.inspector}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${insp.framesCount} frames · ${insp.detectionsCount} findings",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (insp.detectionsCount > 0) toneColor(Tone.CRITICAL, LocalIsDark.current) else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InspectionSetupScreen(
    onStartPatrol: (InspectionRecord) -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var section by remember { mutableStateOf("North Loop · Section 14") }
    var startChainage by remember { mutableStateOf("14+000") }
    var endChainage by remember { mutableStateOf("15+250") }
    var captureProfile by remember { mutableStateOf("Visual + thermal (4K)") }
    var inspector by remember { mutableStateOf("E. Chen") }
    var notes by remember { mutableStateOf("Pre-shift routine inspection pass with ultrasonic check.") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Inspection Setup",
                subtitle = "Configure route, chainage interval, and capture profile",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "CORRIDOR PARAMETERS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Corridor Line & Section") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startChainage,
                        onValueChange = { startChainage = it },
                        label = { Text("Start Chainage") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endChainage,
                        onValueChange = { endChainage = it },
                        label = { Text("End Chainage") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = captureProfile,
                    onValueChange = { captureProfile = it },
                    label = { Text("Sensor Profile") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inspector,
                    onValueChange = { inspector = it },
                    label = { Text("Qualified Inspector") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Operational Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = "Begin Live Patrol",
                icon = Icons.Default.CameraAlt,
                onClick = {
                    val record = InspectionRecord(
                        id = "INSP-${System.currentTimeMillis() % 100000}",
                        section = section,
                        startChainage = startChainage,
                        endChainage = endChainage,
                        scheduledAt = "Now",
                        inspector = inspector,
                        captureProfile = captureProfile,
                        notes = notes,
                        status = "In progress",
                        createdAt = "Just now",
                        framesCount = 1,
                        detectionsCount = 0
                    )
                    onStartPatrol(record)
                }
            )
        }
    }
}

@Composable
fun LiveInspectionScreen(
    onEndInspection: () -> Unit,
    onDefectDetected: () -> Unit,
    onOpenGps: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current
    var secondsElapsed by remember { mutableIntStateOf(48) }
    var frameCount by remember { mutableIntStateOf(42) }
    var defectCount by remember { mutableIntStateOf(1) }
    var flashActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            secondsElapsed++
        }
    }

    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val timerString = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F14))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top HUD bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit HUD",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Red.copy(alpha = 0.2f))
                    .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REC $timerString",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = { onOpenGps() }) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GPS info",
                    tint = Color.White
                )
            }
        }

        // Live Camera Viewfinder HUD Simulation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141C24))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            // Simulated Rail View track lines
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .offset(x = (-40).dp)
                    .background(Color.White.copy(alpha = 0.4f))
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .offset(x = 40.dp)
                    .background(Color.White.copy(alpha = 0.4f))
            )

            // Cross ties
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(6) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            }

            // Simulated AI Bounding Box around defect
            Box(
                modifier = Modifier
                    .size(110.dp, 70.dp)
                    .align(Alignment.Center)
                    .offset(x = (-25).dp, y = (-20).dp)
                    .border(2.dp, toneColor(Tone.CRITICAL, true), RoundedCornerShape(4.dp))
                    .background(toneColor(Tone.CRITICAL, true).copy(alpha = 0.15f))
                    .clickable { onDefectDetected() }
                    .padding(4.dp)
            ) {
                Column {
                    Text(
                        text = "CRK-2048 92%",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "14+320 · GAUGE",
                        color = toneColor(Tone.CRITICAL, true),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Overlay Chainage & Speed info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = "CHAINAGE 14+320 UP LINE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SPEED: 11.8 km/h · ACC: ±1.2m",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bottom Action Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { flashActive = !flashActive },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (flashActive) Color.Yellow else Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (flashActive) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (flashActive) Color.Black else Color.White
                        )
                    }
                    Text("Torch", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }

                // Main Shutter / Capture Frame Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            frameCount++
                        }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onEndInspection() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Red)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Finish",
                            tint = Color.White
                        )
                    }
                    Text("End Run", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$frameCount frames logged · $defectCount critical defect highlighted",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun InspectionDetailsScreen(
    inspection: InspectionRecord,
    onOpenLive: () -> Unit,
    onOpenSummary: () -> Unit,
    onBack: () -> Unit
) {
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
                title = inspection.id,
                subtitle = inspection.section,
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "INSPECTION RECORD",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        label = inspection.status.uppercase(),
                        tone = if (inspection.status == "In progress") Tone.INFO else Tone.HEALTHY
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = inspection.section,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                DetailRow(label = "Interval", value = "${inspection.startChainage} → ${inspection.endChainage}")
                DetailRow(label = "Inspector", value = inspection.inspector)
                DetailRow(label = "Profile", value = inspection.captureProfile)
                DetailRow(label = "Frames", value = "${inspection.framesCount} captured")
                DetailRow(label = "Detections", value = "${inspection.detectionsCount} findings flagged")
                DetailRow(label = "Notes", value = inspection.notes)
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (inspection.status == "In progress") {
                    PrimaryButton(
                        title = "Resume HUD",
                        icon = Icons.Default.CameraAlt,
                        onClick = onOpenLive,
                        modifier = Modifier.weight(1f)
                    )
                }
                PrimaryButton(
                    title = "View Summary",
                    icon = Icons.Default.CheckCircle,
                    secondary = inspection.status == "In progress",
                    onClick = onOpenSummary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InspectionSummaryScreen(
    onDone: () -> Unit,
    onBack: () -> Unit
) {
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
                title = "Inspection Summary",
                subtitle = "Pass finalized · Verification saved to local store",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(toneColor(Tone.HEALTHY, LocalIsDark.current).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = toneColor(Tone.HEALTHY, LocalIsDark.current)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "INSP-240618-04 Complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Audited by E. Chen · SHA-256 verified",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                DetailRow(label = "Corridor", value = "North Loop (Section 14)")
                DetailRow(label = "Distance", value = "1,250 meters")
                DetailRow(label = "Duration", value = "38 min 12 sec")
                DetailRow(label = "Frames logged", value = "42 frames")
                DetailRow(label = "Critical findings", value = "1 (CRK-2048: 46 mm)")
                DetailRow(label = "Warning findings", value = "1 (CRK-2044)")
                DetailRow(label = "Restriction", value = "25 km/h limit active")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = "Save & Return to Dashboard",
                icon = Icons.Default.Home,
                onClick = onDone
            )
        }
    }
}

@Composable
fun InspectionCalendarScreen(
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val days = listOf("Mon 17", "Tue 18", "Wed 19", "Thu 20", "Fri 21")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Shift Schedule",
                subtitle = "Scheduled corridor sweeps and assigned inspectors",
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { i, d ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (i == 1) colorScheme.primary else colorScheme.surface)
                            .border(1.dp, if (i == 1) colorScheme.primary else colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = d,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (i == 1) colorScheme.onPrimary else colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            SectionLabel(title = "SCHEDULED RUNS FOR TODAY")
            ListRow(
                icon = Icons.Default.PlayArrow,
                title = "INSP-240618-04 · North Loop Line",
                subtitle = "14:00 - 18:00 · E. Chen · Section 14",
                trailing = "Live",
                tone = Tone.INFO
            )
            ListRow(
                icon = Icons.Default.Schedule,
                title = "INSP-240618-05 · South Yard Switch",
                subtitle = "19:00 - 21:00 · S. Morgan · Section 08",
                trailing = "Planned",
                tone = Tone.NEUTRAL
            )
        }
    }
}

@Composable
fun GpsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

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
                title = "Field GPS Telemetry",
                subtitle = "Live satellite position and chainage correlation",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "SATELLITE POSITION",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Latitude", value = "51°30'14.2\" N")
                DetailRow(label = "Longitude", value = "0°07'42.8\" W")
                DetailRow(label = "Altitude", value = "48.2 m AMSL")
                DetailRow(label = "Correlated Chainage", value = "14+320 Up Line")
                DetailRow(label = "Accuracy", value = "±1.2 meters (RTK Lock)")
                DetailRow(label = "Satellites", value = "14 in fix (GPS + Galileo)")
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasPermission) {
                PrimaryButton(
                    title = "Request Location Permission",
                    icon = Icons.Default.GpsFixed,
                    onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )
            } else {
                StatusPill(label = "SYSTEM GPS ACCESS GRANTED", tone = Tone.HEALTHY)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
    }
}
