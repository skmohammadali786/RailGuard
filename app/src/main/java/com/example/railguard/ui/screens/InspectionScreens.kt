package com.example.railguard.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import java.io.ByteArrayOutputStream
import java.util.Calendar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.data.RailGuardFirebaseService
import com.example.railguard.model.Defect
import com.example.railguard.model.InspectionRecord
import com.example.railguard.model.PatrolSchedule
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InspectionsListScreen(
    inspections: List<InspectionRecord>,
    onSelectInspection: (InspectionRecord) -> Unit,
    onStartNew: () -> Unit,
    onViewCalendar: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGps: () -> Unit
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
                title = "Inspection Patrols",
                subtitle = "Automated optical track surveillance & defect logging"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartNew,
                    modifier = Modifier.weight(1f).testTag("start_patrol_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Patrol", fontSize = 12.sp)
                }

                Button(
                    onClick = onViewCalendar,
                    modifier = Modifier.weight(1f).testTag("calendar_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schedule", fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenCamera,
                    modifier = Modifier.weight(1f).testTag("camera_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cam Snap", fontSize = 12.sp)
                }
            }
        }

        if (inspections.isEmpty()) {
            item {
                RailCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Past Patrol Inspections",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start a 'New Patrol' or stream camera frames from Raspberry Pi to log optical inspection runs.",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onStartNew,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch First Track Patrol", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        items(inspections, key = { it.id }) { insp ->
            RailCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectInspection(insp) }
                    .testTag("inspection_card_${insp.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = insp.id,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0284C7)
                        )
                        Text(
                            text = insp.date,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = insp.section,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Inspector: ${insp.inspector} · ${insp.framesCount} frames analyzed",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusChip(
                            title = "${insp.detectionsCount} Anomaly Flags",
                            tone = if (insp.detectionsCount > 0) Tone.WARNING else Tone.HEALTHY
                        )

                        Text(
                            text = insp.status,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (insp.status.contains("Live")) Color(0xFF16A34A) else Color(0xFF0284C7)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InspectionSetupScreen(
    onStartPatrol: (InspectionRecord, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val firebaseService = remember { RailGuardFirebaseService.instance }
    var corridorName by remember { mutableStateOf("") }
    var inspectorName by remember {
        mutableStateOf(firebaseService.currentUser?.displayName.orEmpty())
    }
    var useLiveHardwareStream by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Setup Inspection Patrol", subtitle = "Initialize track scanner parameters", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))

        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = corridorName,
                    onValueChange = { corridorName = it },
                    label = { Text("Corridor Track Section") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inspectorName,
                    onValueChange = { inspectorName = it },
                    label = { Text("Inspector Name / Badge") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Sensor Stream Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Synchronize inspection frames & sensor telemetry in real time", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = useLiveHardwareStream, onCheckedChange = { useLiveHardwareStream = it })
                }

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    title = "Launch Active Patrol Viewfinder",
                    onClick = {
                        val record = InspectionRecord(
                            id = "INSP-${System.currentTimeMillis() % 10000}",
                            section = corridorName.ifBlank { "Corridor KM 14" },
                            inspector = inspectorName.ifBlank { "Field Inspector" },
                            status = "Active Live Stream",
                            framesCount = 0,
                            detectionsCount = 0
                        )
                        onStartPatrol(record, true)
                    }
                )
            }
        }
    }
}

@Composable
fun LiveInspectionScreen(
    inspection: InspectionRecord,
    telemetry: RailGuardFirebaseService.LiveSensorTelemetry?,
    cloudDefectCount: Int,
    onEndInspection: () -> Unit,
    onDefectDetected: () -> Unit,
    onOpenGps: () -> Unit,
    onOpenCamera: () -> Unit,
    onBack: () -> Unit
) {
    val firebaseService = remember { RailGuardFirebaseService.instance }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // High-tech viewfinder overlay in 100% Light Mode
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (firebaseService.isConnectedToFirebase) Color(0xFF16A34A) else Color(0xFFF59E0B))
                    )
                    Text(
                        text = if (firebaseService.isDirectWifiConnected) {
                            "PI DIRECT WI-FI · ${firebaseService.directWifiLatencyMs}ms"
                        } else if (firebaseService.isConnectedToFirebase) {
                            "LIVE CLOUD TELEMETRY"
                        } else {
                            "WAITING FOR RASPBERRY PI"
                        },
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onOpenGps) {
                    Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = Color(0xFF0284C7))
                }
            }

            // Real-Time Sensor Telemetry Strip Overlay
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ULTRASONIC", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(
                            telemetry?.let { "${it.ultrasonicDepthMm} mm" } ?: "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFD97706)
                        )
                    }
                    Column {
                        Text("VIBRATION", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(
                            telemetry?.let { "${it.vibrationG} g" } ?: "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF16A34A)
                        )
                    }
                    Column {
                        Text("RAIL TEMP", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(
                            telemetry?.let { "${it.railTempC} °C" } ?: "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFEA580C)
                        )
                    }
                    Column {
                        Text("CHAINAGE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Text(
                            telemetry?.chainage ?: "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            // Center Optical Bounding Box & Reticle in Light Technical Style
            val isAnomaly = telemetry?.hasCrack == true ||
                telemetry?.status?.contains("ANOMALY", ignoreCase = true) == true ||
                ((telemetry?.ultrasonicDepthMm ?: 68.2f) in 10.0f..55.0f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAnomaly) Color(0xFFFEF2F2) else Color(0xFFF0F9FF))
                    .border(
                        2.dp,
                        if (isAnomaly) Color(0xFFDC2626) else if (telemetry != null) Color(0xFF0284C7) else Color(0xFF94A3B8),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAnomaly) Color(0xFFDC2626) else if (telemetry != null) Color(0xFF16A34A) else Color(0xFF0284C7))
                        )
                        Text(
                            text = if (isAnomaly) {
                                "AUTOMATED CRACK DETECTED · ${telemetry?.status ?: "ANOMALY"}"
                            } else if (telemetry != null) {
                                "RASPBERRY PI PACKET RECEIVED · ${telemetry.status}"
                            } else {
                                "WAITING FOR RASPBERRY PI TELEMETRY"
                            },
                            color = if (isAnomaly) Color(0xFFDC2626) else if (telemetry != null) Color(0xFF0284C7) else Color(0xFF475569),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Inspection: ${inspection.id} | Chainage: ${telemetry?.chainage ?: "Scanning"} | Defects: $cloudDefectCount",
                        color = Color(0xFF334155),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Optical Target Bounding Box Crosshairs when Anomaly Detected
                if (isAnomaly) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(56.dp)
                                .border(2.dp, Color(0xFFDC2626), RoundedCornerShape(6.dp))
                                .background(Color(0x22DC2626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TARGET: CRACK [${telemetry?.flawType?.ifBlank { "Transverse Fissure" } ?: "Transverse Fissure"}]",
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "SIZE: ${telemetry?.crackLengthMm ?: 14.8f}mm · DEPTH: ${telemetry?.ultrasonicDepthMm ?: 46.2f}mm",
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "WI-FI TELEMETRY FUSION: CONFIDENCE ${telemetry?.aiConfidencePercent ?: 96}%",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                if (cloudDefectCount > 0) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDefectDetected,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Cloud Defects ($cloudDefectCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenCamera,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE2E8F0),
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Snap", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onEndInspection,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete Patrol & Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = inspection.id, subtitle = inspection.section, onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))

        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Corridor Section", inspection.section)
                MetricRow("Inspector", inspection.inspector)
                MetricRow("Status", inspection.status)
                MetricRow("Analyzed Video Frames", "${inspection.framesCount} frames")
                MetricRow("Detected Anomalies", "${inspection.detectionsCount} flagged")
                MetricRow("Primary Anomaly", inspection.detectedCrackTitle)
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(title = "Open Live Sensor Stream", onClick = onOpenLive)
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(title = "View Shift Inspection Summary", onClick = onOpenSummary)
            }
        }
    }
}

@Composable
fun InspectionSummaryScreen(onDone: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Patrol Summary", subtitle = "Mission completion dossier", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Patrol Complete", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF16A34A))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Inspection completed. Review cloud records for details.", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(title = "Done & Return Home", onClick = onDone)
            }
        }
    }
}

@Composable
fun InspectionCalendarScreen(
    inspections: List<InspectionRecord> = emptyList(),
    onBack: () -> Unit
) {
    val firebaseService = remember { RailGuardFirebaseService.instance }
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var schedules by remember { mutableStateOf<List<PatrolSchedule>>(firebaseService.getCachedSchedules()) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val initialDateStr = remember {
        val y = calendar.get(Calendar.YEAR)
        val m = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val d = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        "$y-$m-$d"
    }

    // Form inputs for new schedule
    var newSection by remember { mutableStateOf("Mainline Track Corridor") }
    var newDate by remember { mutableStateOf(initialDateStr) }
    var newTime by remember { mutableStateOf("06:00") }
    var newShift by remember { mutableStateOf("Morning Shift") }
    var newInspector by remember {
        mutableStateOf(firebaseService.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Lead Inspector")
    }
    var newPatrolType by remember { mutableStateOf("Automated Optical & Ultrasonic") }
    var newFrequency by remember { mutableStateOf("Daily") }
    var newNotes by remember { mutableStateOf("") }

    val datePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedMonth = String.format("%02d", month + 1)
                val formattedDay = String.format("%02d", dayOfMonth)
                newDate = "$year-$formattedMonth-$formattedDay"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun refreshSchedules() {
        isLoading = true
        scope.launch {
            firebaseService.pullSchedules(
                onSuccess = {
                    schedules = it
                    isLoading = false
                },
                onError = {
                    isLoading = false
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshSchedules()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        SubScreenHeader(
            title = "Track Patrol Scheduler",
            subtitle = "Corridor maintenance & automated inspection calendar",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Action banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule New Patrol", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            IconButton(onClick = { refreshSchedules() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF0284C7))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color(0xFF0284C7))
            }
        } else if (schedules.isEmpty()) {
            RailCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Scheduled Patrols Pending", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Plan future corridor sweeps and automated camera/ultrasonic patrols using the button above.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(schedules) { item ->
                    RailCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.section, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (item.status) {
                                                "Completed" -> Color(0xFF16A34A).copy(alpha = 0.15f)
                                                "In Progress" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                else -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = item.status.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (item.status) {
                                            "Completed" -> Color(0xFF16A34A)
                                            "In Progress" -> Color(0xFFD97706)
                                            else -> Color(0xFF0284C7)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            MetricRow("Shift / Window", "${item.shift} (${item.time}) · ${item.date}")
                            MetricRow("Patrol Mode", item.patrolType)
                            MetricRow("Inspector", item.inspector)
                            MetricRow("Recurrence", item.frequency)
                            if (item.notes.isNotBlank()) {
                                MetricRow("Notes", item.notes)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.status != "Completed") {
                                    TextButton(
                                        onClick = {
                                            val completed = item.copy(status = "Completed")
                                            schedules = schedules.map { if (it.id == item.id) completed else it }
                                            firebaseService.addCachedSchedule(completed)
                                            scope.launch {
                                                firebaseService.saveSchedule(completed)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF16A34A))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark Completed", color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                TextButton(
                                    onClick = {
                                        schedules = schedules.filter { it.id != item.id }
                                        firebaseService.deleteCachedSchedule(item.id)
                                        scope.launch {
                                            firebaseService.deleteSchedule(item.id)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Schedule Creation Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Schedule Track Patrol", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newSection,
                        onValueChange = { newSection = it },
                        label = { Text("Corridor Track Section") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newDate,
                            onValueChange = { newDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        OutlinedTextField(
                            value = newTime,
                            onValueChange = { newTime = it },
                            label = { Text("Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = newShift,
                        onValueChange = { newShift = it },
                        label = { Text("Shift Window") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPatrolType,
                        onValueChange = { newPatrolType = it },
                        label = { Text("Patrol Type") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newInspector,
                        onValueChange = { newInspector = it },
                        label = { Text("Assigned Inspector") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Optional Dispatch Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val scheduleId = "SCHED-${System.currentTimeMillis() % 100000}"
                        val schedule = PatrolSchedule(
                            id = scheduleId,
                            section = newSection.ifBlank { "Mainline Track Corridor" },
                            date = newDate.ifBlank { "Today" },
                            time = newTime.ifBlank { "08:00" },
                            shift = newShift.ifBlank { "Morning Shift" },
                            inspector = newInspector.ifBlank { "Lead Inspector" },
                            patrolType = newPatrolType.ifBlank { "Automated Optical & Ultrasonic" },
                            frequency = newFrequency,
                            status = "Scheduled",
                            notes = newNotes
                        )
                        schedules = listOf(schedule) + schedules.filter { it.id != schedule.id }
                        firebaseService.addCachedSchedule(schedule)
                        scope.launch {
                            firebaseService.saveSchedule(schedule)
                        }
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Save Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CameraScreen(onCaptureDefect: () -> Unit, onBack: () -> Unit) {
    val firebaseService = remember { RailGuardFirebaseService.instance }
    val scope = rememberCoroutineScope()
    var isCapturing by remember { mutableStateOf(false) }
    var actionMsg by remember { mutableStateOf<String?>(null) }
    val piBitmap = firebaseService.latestPiCameraBitmap
    val telemetry = firebaseService.latestSensorTelemetry
    val isWifiOnline = firebaseService.isDirectWifiConnected

    // Periodically fetch camera frame from Raspberry Pi over Wi-Fi
    LaunchedEffect(Unit) {
        while (true) {
            firebaseService.fetchPiCameraFrame()
            delay(1500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "PI OPTICAL CRACK CAMERA",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        if (isWifiOnline) "Wi-Fi Feed Active · ${firebaseService.directWifiLatencyMs}ms" else "Wi-Fi Standby · http://${firebaseService.localWifiIp}:8080",
                        color = if (isWifiOnline) Color(0xFF16A34A) else Color(0xFF0284C7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            val bmp = firebaseService.fetchPiCameraFrame()
                            actionMsg = if (bmp != null) "Feed refreshed" else "Could not reach Pi camera"
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF0284C7)
                    )
                }
            }

            // Main Camera Feed Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (piBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = piBitmap.asImageBitmap(),
                            contentDescription = "Raspberry Pi Optical Camera Stream",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Camera OSD Overlay (Top)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "REC · RPI-CAM3 1080p",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                telemetry?.chainage ?: "Active Sector",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // AI Anomaly / Crack Bounding Box Overlay
                        if (telemetry?.hasCrack == true || telemetry?.status?.contains("ANOMALY", ignoreCase = true) == true) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(width = 180.dp, height = 90.dp)
                                    .border(2.dp, Color(0xFFDC2626), RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDC2626).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Text(
                                    " [!] FLAW: ${telemetry.flawType.ifBlank { "SURFACE DEFECT" }} (${telemetry.aiConfidencePercent}% AI) ",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFFDC2626))
                                        .padding(2.dp)
                                )
                            }
                        }

                        // Camera OSD Overlay (Bottom)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "UT: %.1f mm | Vib: %.3f G".format(
                                    telemetry?.ultrasonicDepthMm ?: 0f,
                                    telemetry?.vibrationG ?: 0f
                                ),
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                if (isWifiOnline) "WI-FI DIRECT" else "STANDBY",
                                color = if (isWifiOnline) Color(0xFF4ADE80) else Color(0xFFF87171),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Placeholder when frame is loading or waiting for Pi Wi-Fi
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "AWAITING PI CAMERA STREAM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Connect phone to Raspberry Pi Wi-Fi (http://${firebaseService.localWifiIp}:8080) to view live optical stream.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        actionMsg = "Connecting to Pi..."
                                        val (ok, msg) = firebaseService.connectDirectWifi()
                                        if (ok) {
                                            firebaseService.fetchPiCameraFrame()
                                        }
                                        actionMsg = msg
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect to Pi Wi-Fi", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Status message
            if (actionMsg != null) {
                Text(
                    actionMsg ?: "",
                    color = if (actionMsg?.contains("fail", ignoreCase = true) == true || actionMsg?.contains("cannot", ignoreCase = true) == true) Color(0xFFDC2626) else Color(0xFF16A34A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Sensor Telemetry Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Ultrasonic", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            if (telemetry != null) "%.1f mm".format(telemetry.ultrasonicDepthMm) else "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Vibration", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            if (telemetry != null) "%.3f G".format(telemetry.vibrationG) else "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Rail Temp", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            if (telemetry != null) "%.1f°C".format(telemetry.railTempC) else "--",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isCapturing = true
                        actionMsg = null
                        scope.launch {
                            val (ok, msg) = firebaseService.triggerPiCameraCapture()
                            isCapturing = false
                            actionMsg = msg
                        }
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isCapturing
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Snapshot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onCaptureDefect,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Defect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
