package com.example.railguard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.PrimaryButton
import com.example.railguard.components.RailCard
import com.example.railguard.components.StatusPill
import com.example.railguard.data.ConnectionTestResult
import com.example.railguard.data.DatabaseBackendType
import com.example.railguard.data.RailGuardFirebaseService
import com.example.railguard.model.Defect
import com.example.railguard.model.InspectionRecord
import com.example.railguard.model.MaintenanceTask
import com.example.railguard.model.Tone
import kotlinx.coroutines.launch

@Composable
fun FirebaseSyncScreen(
    defects: List<Defect>,
    tasks: List<MaintenanceTask>,
    inspections: List<InspectionRecord>,
    onDefectsUpdated: (List<Defect>) -> Unit = {},
    onTasksUpdated: (List<MaintenanceTask>) -> Unit = {},
    onInspectionsUpdated: (List<InspectionRecord>) -> Unit = {},
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val firebaseService = remember { RailGuardFirebaseService.instance }
    val scope = rememberCoroutineScope()

    var editProjectId by remember { mutableStateOf(firebaseService.projectId) }
    var editApiKey by remember { mutableStateOf(firebaseService.webApiKey) }
    var editDbUrl by remember { mutableStateOf(firebaseService.databaseUrl) }
    var editDbType by remember { mutableStateOf(firebaseService.databaseType) }

    var showConfigDialog by remember { mutableStateOf(false) }
    var syncResultMsg by remember { mutableStateOf<String?>(null) }
    var syncIsError by remember { mutableStateOf(false) }
    var showSetupGuide by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Central Safety Cloud Backend",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )
                    Text(
                        text = "Live database telemetry sync & secure identity authentication",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Backend Connection Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (firebaseService.isConnectedToFirebase) Color(0xFF16A34A).copy(alpha = 0.5f)
                        else colorScheme.outline.copy(alpha = 0.3f),
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enterprise Cloud Engine",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onBackground
                                )
                                Text(
                                    text = "Realtime Cloud Database (REST)",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        StatusPill(
                            label = if (firebaseService.isConnectedToFirebase) "ONLINE & LINKED" else "SERVER-CONFIGURED",
                            tone = Tone.HEALTHY
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Project Name & ID:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = "railguard (${firebaseService.projectId})",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Project Number:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = RailGuardFirebaseService.DEFAULT_PROJECT_NUMBER,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cloud App ID:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = "1:590063963377...6d50",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Web API Key:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = "AIzaSyDzM8••••••••••••4OX1uA",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16A34A)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Database Region:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = "asia-southeast1 (Active)",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Auth User:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = firebaseService.currentUser?.email ?: "Unauthenticated",
                            fontSize = 12.sp,
                            color = if (firebaseService.currentUser != null) Color(0xFF16A34A) else colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Sync:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = firebaseService.lastSyncTimestamp,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = firebaseService.syncStatusMessage,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Test Connection & Configure Buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            firebaseService.testFirebaseConnection { result ->
                                syncIsError = !result.isSuccess
                                syncResultMsg = result.message
                            }
                        }
                    },
                    enabled = !firebaseService.isTestingConnection,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    if (firebaseService.isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        editProjectId = firebaseService.projectId
                        editApiKey = firebaseService.webApiKey
                        editDbUrl = firebaseService.databaseUrl
                        editDbType = firebaseService.databaseType
                        showConfigDialog = true
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Project Config", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Push / Pull Action Buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            firebaseService.syncAllToFirebase(
                                defects = defects,
                                tasks = tasks,
                                inspections = inspections,
                                onSuccess = {
                                    syncIsError = false
                                    syncResultMsg = "Successfully pushed ${defects.size} defects & ${tasks.size} tasks to Central Cloud!"
                                },
                                onError = { err ->
                                    syncIsError = true
                                    syncResultMsg = err
                                }
                            )
                        }
                    },
                    enabled = !firebaseService.isSyncing,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    if (firebaseService.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pushing...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Push to Cloud", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            firebaseService.pullAllFromFirebase(
                                onSuccess = { pulledDefects, pulledTasks, pulledInspections ->
                                    if (pulledDefects.isNotEmpty()) onDefectsUpdated(pulledDefects)
                                    if (pulledTasks.isNotEmpty()) onTasksUpdated(pulledTasks)
                                    if (pulledInspections.isNotEmpty()) onInspectionsUpdated(pulledInspections)
                                    syncIsError = false
                                    syncResultMsg = "Pulled ${pulledDefects.size} defects & ${pulledTasks.size} tasks from Central Cloud!"
                                },
                                onError = { err ->
                                    syncIsError = true
                                    syncResultMsg = err
                                }
                            )
                        }
                    },
                    enabled = !firebaseService.isSyncing,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pull from Cloud", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Sync Result Notification Banner
        if (syncResultMsg != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (syncIsError) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF16A34A).copy(alpha = 0.15f))
                        .border(1.dp, if (syncIsError) Color(0xFFEF4444) else Color(0xFF16A34A), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (syncIsError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (syncIsError) Color(0xFFEF4444) else Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = syncResultMsg ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (syncIsError) Color(0xFFEF4444) else Color(0xFF16A34A)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Live Database Collections
        item {
            Text(
                text = "REALTIME CLOUD DATA COLLECTIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Collection 1: Track Defects
        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDC2626).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Track Defects Database", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${defects.size} active records (/railguard/defects/)", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        }
                    }
                    StatusPill(label = "REPLICATED", tone = Tone.HEALTHY)
                }
            }
        }

        // Collection 2: Maintenance Tasks
        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Maintenance Work Orders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${tasks.size} active records (/railguard/tasks/)", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        }
                    }
                    StatusPill(label = "REPLICATED", tone = Tone.HEALTHY)
                }
            }
        }

        // Collection 3: Corridor Inspections
        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Corridor Inspections", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${inspections.size} active records (/railguard/inspections/)", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        }
                    }
                    StatusPill(label = "REPLICATED", tone = Tone.HEALTHY)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Setup Guide Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSetupGuide = !showSetupGuide }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enterprise Cloud Telemetry Architecture",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colorScheme.onBackground
                            )
                        }
                        Icon(
                            imageVector = if (showSetupGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showSetupGuide) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "1. Central Realtime Database aggregates telemetry from GPS, cameras, and AI inference engines.\n\n" +
                                        "2. Instant two-way synchronization transmits temporary speed restrictions (TSR) to train cabs.\n\n" +
                                        "3. Multi-spectral crack detections and sensor geometry are persisted with cryptographic audit trails.\n\n" +
                                        "4. High-frequency 60 FPS video frames and sub-millimeter measurements are archived in real time.\n\n" +
                                        "5. All changes sync seamlessly offline-to-online with automated conflict resolution.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Server Cloud Configuration") },
            text = {
                Column {
                    Text(
                        text = "Pre-configured server-side for project 'railguard' (${RailGuardFirebaseService.DEFAULT_PROJECT_ID}):",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editProjectId,
                        onValueChange = { editProjectId = it },
                        label = { Text("Project ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editApiKey,
                        onValueChange = { editApiKey = it },
                        label = { Text("Web API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDbUrl,
                        onValueChange = { editDbUrl = it },
                        label = { Text("Database URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            editProjectId = RailGuardFirebaseService.DEFAULT_PROJECT_ID
                            editApiKey = RailGuardFirebaseService.DEFAULT_WEB_API_KEY
                            editDbUrl = RailGuardFirebaseService.DEFAULT_DB_URL
                        }
                    ) {
                        Text("Reset to Server Defaults", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        firebaseService.saveConfig(editProjectId, editApiKey, editDbUrl, editDbType)
                        showConfigDialog = false
                    }
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
