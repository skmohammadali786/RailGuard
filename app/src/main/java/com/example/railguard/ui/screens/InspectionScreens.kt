package com.example.railguard.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.content.pm.PackageManager
import java.io.ByteArrayOutputStream
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
    var corridorName by remember { mutableStateOf("Sector 4B · North Express Corridor") }
    var inspectorName by remember { mutableStateOf("E. Chen (#4092)") }
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
                        Text("Live Cloud Patrol Sync", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Synchronize inspection frames & defects live to central cloud", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = useLiveHardwareStream, onCheckedChange = { useLiveHardwareStream = it })
                }

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    title = "Launch Active Patrol Viewfinder",
                    onClick = {
                        val record = InspectionRecord(
                            id = "INSP-${System.currentTimeMillis() % 10000}",
                            section = corridorName,
                            inspector = inspectorName,
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
    onEndInspection: () -> Unit,
    onDefectDetected: () -> Unit,
    onOpenGps: () -> Unit,
    onBack: () -> Unit
) {
    val firebaseService = remember { RailGuardFirebaseService.instance }
    var frameCount by remember { mutableStateOf(142) }
    var simulatedDetection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var loopCount = 0
        while (true) {
            delay(1200)
            frameCount += 30
            loopCount++
            if (loopCount % 3 == 0) {
                firebaseService.uploadLiveInspectionStream(
                    sessionId = "SESSION-14-LIVE",
                    fps = 60,
                    defectCount = if (simulatedDetection) 1 else 0,
                    currentChainage = "Continuous Survey",
                    alertActive = simulatedDetection
                )
            }
            if (frameCount % 180 == 0) {
                simulatedDetection = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        // High-tech viewfinder overlay
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                        text = if (firebaseService.isConnectedToFirebase) "LIVE CLOUD TELEMETRY · 60 FPS" else "FIELD PATROL · 60 FPS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onOpenGps) {
                    Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = Color(0xFF38BDF8))
                }
            }

            // Real-Time Sensor Telemetry Strip Overlay
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("OPTICAL AI", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(
                            "98.2% CONF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF4ADE80)
                        )
                    }
                    Column {
                        Text("CORRIDOR", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(
                            "Continuous Track",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Column {
                        Text("PROFILE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(
                            "UIC 60 KG/M",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    Column {
                        Text("BACKEND", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text(
                            if (firebaseService.isConnectedToFirebase) "ONLINE" else "LOCAL CACHE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFA78BFA)
                        )
                    }
                }
            }

            // Center Optical Bounding Box & Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .border(
                        2.dp,
                        if (simulatedDetection) Color.Red else Color(0xFF00F0FF),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = if (simulatedDetection) "🚨 DEFECT DETECTED: SURFACE FISSURE / GAUGE ANOMALY (96.4%)" else "AI SCANNING RAIL TRACK PROFILE... OK",
                        color = if (simulatedDetection) Color.Red else Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Frames: $frameCount | GPS: 28.6142°N 77.2085°E | Section: 4B Mainline",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (simulatedDetection) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                simulatedDetection = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("Dismiss", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                simulatedDetection = false
                                onDefectDetected()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Log Defect & Interlock Fleet", fontSize = 11.sp)
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
                    onClick = {
                        // Capture snapshot
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Snap", fontSize = 11.sp)
                }

                Button(
                    onClick = onEndInspection,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Complete Patrol & Export", fontSize = 11.sp)
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
                Text("Track corridor sweep completed with zero optical dropout. Inspection telemetry verified and saved.", fontSize = 13.sp)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Patrol Calendar", subtitle = "Scheduled track maintenance runs", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Scheduled Patrol Sweeps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                if (inspections.isEmpty()) {
                    Text(
                        text = "No scheduled patrols pending. Initiate a 'New Patrol' from the Inspections menu for real-time corridor surveillance.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    inspections.take(5).forEach { insp ->
                        MetricRow(insp.section, insp.status)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraScreen(onCaptureDefect: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploadingCapture by remember { mutableStateOf(false) }
    var uploadStatusMsg by remember { mutableStateOf<String?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            uploadStatusMsg = "Camera capture cancelled"
        } else {
            capturedBitmap = bitmap
            isUploadingCapture = true
            uploadStatusMsg = null
            scope.launch {
                val bytes = ByteArrayOutputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                    output.toByteArray()
                }
                val capId = "CAM-${System.currentTimeMillis() % 100000}"
                val result = RailGuardFirebaseService.instance.uploadCameraCapture(
                    captureId = capId,
                    section = "Section 14 North Loop",
                    detectionCount = 1,
                    defectDetected = true,
                    notes = "Camera capture at 14+320 chainage",
                    evidenceBytes = bytes,
                    contentType = "image/jpeg"
                )
                if (result.isSuccess) {
                    RailGuardFirebaseService.instance.logSafetyAuditEvent(
                        "CAMERA_CAPTURE_UPLOAD",
                        mapOf(
                            "captureId" to capId,
                            "chainage" to "14+320",
                            "storagePath" to result.storagePath
                        )
                    )
                    uploadStatusMsg = "Photo and metadata uploaded to Firebase"
                    onCaptureDefect()
                } else {
                    uploadStatusMsg = "Firebase upload failed: ${result.message}"
                }
                isUploadingCapture = false
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            uploadStatusMsg = "Camera permission was denied"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HIGH-RES CAMERA CAPTURE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("4K-HDR Optical Railhead Sensor", color = Color(0xFF38BDF8), fontSize = 10.sp)
                }
                Box(modifier = Modifier.size(24.dp))
            }

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .border(2.dp, Color(0xFF00F0FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (capturedBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Captured rail inspection evidence",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ALIGN RAILHEAD CRACK", color = Color(0xFF00F0FF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        if (uploadStatusMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(uploadStatusMsg ?: "", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isUploadingCapture) {
                    CircularProgressIndicator(color = Color(0xFF00F0FF), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Syncing Frame & Telemetry to Cloud...", color = Color.White, fontSize = 11.sp)
                } else {
                    Button(
                        onClick = {
                            if (
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to Capture & Transmit to Cloud", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}
