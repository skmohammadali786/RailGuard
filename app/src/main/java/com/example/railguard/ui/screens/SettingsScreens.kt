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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.data.RailGuardFirebaseService
import com.example.railguard.model.AppPreferences
import com.example.railguard.model.NotificationItem
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    onLockApp: () -> Unit
) {
    val isDark = LocalIsDark.current
    val firebaseService = remember { RailGuardFirebaseService.instance }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Settings & Cloud Control",
                subtitle = "Central safety cloud backend, train fleet signaling & system security"
            )
        }

        item {
            RailCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BACKEND & FLEET GATEWAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsRowTile(
                        icon = Icons.Default.CloudSync,
                        title = "Central Safety Cloud Backend",
                        subtitle = if (firebaseService.isConnectedToFirebase) "Connected: Active Cloud Telemetry" else "Ready for Cloud Realtime DB & Auth",
                        onClick = { onNavigate("firebase_sync") }
                    )

                    SettingsRowTile(
                        icon = Icons.Default.Train,
                        title = "Train Fleet Interlock & Cab Link",
                        subtitle = "Ground-to-cab telemetry, ETCS speed caps & TSR dispatch",
                        onClick = { onNavigate("train_connection") }
                    )
                }
            }
        }

        item {
            RailCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "FIELD INSPECTOR PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsRowTile(
                        icon = Icons.Default.Person,
                        title = "Inspector Profile & Badges",
                        subtitle = firebaseService.currentUser?.email ?: "Registered Safety Inspector",
                        onClick = { onNavigate("profile") }
                    )

                    SettingsRowTile(
                        icon = Icons.Default.Notifications,
                        title = "Notifications & Dispatch Alerts",
                        subtitle = "Vibration, acoustic & derailment warnings",
                        onClick = { onNavigate("notifications") }
                    )
                }
            }
        }

        item {
            RailCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PREFERENCES & SYSTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Dark Mission Control Theme", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Switch(checked = isDarkMode, onCheckedChange = onToggleDarkMode)
                    }

                    SettingsRowTile(
                        icon = Icons.Default.Security,
                        title = "Security & PIN Lock",
                        subtitle = "Passcode, biometric & session lock",
                        onClick = { onNavigate("security") }
                    )

                    SettingsRowTile(
                        icon = Icons.Default.Language,
                        title = "Language & Locale",
                        subtitle = "English, Spanish, Hindi, German",
                        onClick = { onNavigate("language") }
                    )

                    SettingsRowTile(
                        icon = Icons.Default.Info,
                        title = "About RailGuard Platform",
                        subtitle = "v2.4.0 High-Speed Track AI Engine",
                        onClick = { onNavigate("about") }
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
                    onClick = onLockApp,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock Terminal", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        firebaseService.signOut()
                        onSignOut()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsRowTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isDark = LocalIsDark.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
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
fun ProfileScreen(
    name: String,
    email: String,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var editName by remember { mutableStateOf(name) }
    var editEmail by remember { mutableStateOf(email) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(
            title = "Inspector Profile",
            subtitle = "Field credentials & authority",
            onBack = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                onBack()
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editEmail,
                    onValueChange = { editEmail = it },
                    label = { Text("Official Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(title = "Save Inspector Credentials", onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    onSave(editName, editEmail)
                    scope.launch {
                        RailGuardFirebaseService.instance.saveUserSettings(
                            mapOf("fullName" to editName, "email" to editEmail, "updatedAt" to System.currentTimeMillis())
                        )
                    }
                    onBack()
                })

                val firebaseService = RailGuardFirebaseService.instance
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "CENTRAL CLOUD IDENTITY & ACCESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF0284C7)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Cloud Inspector UID: ${firebaseService.currentUser?.localId ?: "Local Session"}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Network Status: Authenticated & Linked",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppSettingsScreen(
    preferences: AppPreferences,
    onUpdatePreferences: (AppPreferences) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var autoSyncRtdb by remember { mutableStateOf(preferences.autoSync) }
    var esp32LinkActive by remember { mutableStateOf(true) }
    var highPrecisionAi by remember { mutableStateOf(true) }
    var tsrInterlockEnabled by remember { mutableStateOf(true) }
    var syncFeedbackMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "System & Sensor Configurations", subtitle = "ESP32, RTDB & Safety Directives", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Operational Mode: Rail Safety Standards EN 13848-1", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Sync Cloud RTDB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Continuous 5Hz sensor stream to cloud database", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = autoSyncRtdb, onCheckedChange = { autoSyncRtdb = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ESP32 Sensor Hardware Hub", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Link ultrasonic UT, accelerometer & thermal sensors", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = esp32LinkActive, onCheckedChange = { esp32LinkActive = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Multi-Tensor High-Precision AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Level 4 sub-millimeter flaw kinetics & Nadal ratio", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = highPrecisionAi, onCheckedChange = { highPrecisionAi = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-TSR Fleet Interlock", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Direct cab speed enforcement on verified critical cracks", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = tsrInterlockEnabled, onCheckedChange = { tsrInterlockEnabled = it })
                }

                if (syncFeedbackMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(syncFeedbackMsg ?: "", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    title = "Save & Push to Cloud RTDB",
                    icon = Icons.Default.CloudSync,
                    onClick = {
                        scope.launch {
                            RailGuardFirebaseService.instance.saveUserSettings(
                                mapOf(
                                    "autoSyncRtdb" to autoSyncRtdb,
                                    "esp32LinkActive" to esp32LinkActive,
                                    "highPrecisionAi" to highPrecisionAi,
                                    "tsrInterlockEnabled" to tsrInterlockEnabled,
                                    "operationalStandard" to "EN 13848-1",
                                    "updatedAt" to System.currentTimeMillis()
                                )
                            )
                            onUpdatePreferences(preferences.copy(autoSync = autoSyncRtdb))
                            syncFeedbackMsg = "✓ System preferences synchronized with Cloud Realtime DB"
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SecurityScreen(
    preferences: AppPreferences,
    onUpdatePreferences: (AppPreferences) -> Unit,
    onLockApp: () -> Unit,
    onBack: () -> Unit
) {
    var passcodeEnabled by remember { mutableStateOf(preferences.passcodeEnabled) }
    var pin by remember { mutableStateOf(preferences.passcodePin) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Security & Terminal Lock", subtitle = "Mission-critical authorization", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Passcode Lock", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(
                        checked = passcodeEnabled,
                        onCheckedChange = {
                            passcodeEnabled = it
                            onUpdatePreferences(preferences.copy(passcodeEnabled = it))
                        }
                    )
                }

                if (passcodeEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4) {
                                pin = it
                                onUpdatePreferences(preferences.copy(passcodePin = it))
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(title = "Lock Terminal Now", onClick = onLockApp)
            }
        }
    }
}

@Composable
fun LanguageScreen(
    preferences: AppPreferences,
    onUpdatePreferences: (AppPreferences) -> Unit,
    onBack: () -> Unit
) {
    val languages = listOf(
        com.example.railguard.model.AppLanguage.EN_UK,
        com.example.railguard.model.AppLanguage.EN_US,
        com.example.railguard.model.AppLanguage.HI,
        com.example.railguard.model.AppLanguage.ES
    )
    var selectedLang by remember { mutableStateOf(preferences.language) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Language & Regionalization", subtitle = "Select terminal language", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(languages) { lang ->
                RailCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedLang = lang
                            onUpdatePreferences(preferences.copy(language = lang))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(lang.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (selectedLang == lang) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpCenterScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Field Assistance & Manual", subtitle = "Operating procedures", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ultrasonic Gauge Calibration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Place sensor cart exactly 14.5 cm perpendicular to rail crown. Gauge should read 1435 mm (Standard).", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Emergency Slow Orders", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("For transverse fissures exceeding 20 mm length, immediately dispatch speed restriction flag and contact Central Dispatch.", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "About RailGuard", subtitle = "Track integrity platform", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RailGuard Autonomous Track AI", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0284C7))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Version 2.4.0 (Enterprise Hardware Build)", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Integrated with Central Safety Cloud Database, dual GPS telemetry fusion, computer vision crack estimation, and real-time prototype actuator control.", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AttentionScreen(
    onNavigateDefect: () -> Unit,
    onNavigateTask: () -> Unit,
    onCreateTask: () -> Unit,
    onCompareImages: () -> Unit,
    onAllObservations: () -> Unit,
    onReviewDetection: () -> Unit,
    onBuildEvidencePackage: () -> Unit,
    onRiskHeatmap: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Urgent Action Required", subtitle = "High severity alerts", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Critical Sector 4B Alert", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Red)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Transverse crack growth rate accelerated. Derailment risk score 92/100.", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNavigateDefect, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("View Defect")
                    }
                    Button(onClick = onCreateTask) {
                        Text("Dispatch Gang")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onBack: () -> Unit
) {
    val isDark = LocalIsDark.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Dispatch & System Alerts", subtitle = "${notifications.size} notifications", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notifications, key = { it.id }) { notif ->
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(toneColor(notif.tone, isDark).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = toneColor(notif.tone, isDark), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(notif.message, fontSize = 11.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                            Text(notif.time, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
