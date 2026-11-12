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
import com.example.railguard.model.Defect
import com.example.railguard.model.MaintenanceTask
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
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Settings & Preferences",
                subtitle = "Inspector account, security, regionalization & system parameters"
            )
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


                    SettingsRowTile(
                        icon = Icons.Default.Language,
                        title = "Language & Locale",
                        subtitle = "English, Spanish, Hindi, German",
                        onClick = { onNavigate("language") }
                    )

                    SettingsRowTile(
                        icon = Icons.Default.Info,
                        title = "About RailGuard Platform",
                        subtitle = "Hardware architecture, AI models & safety standards",
                        onClick = { onNavigate("about") }
                    )

                    SettingsRowTile(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        subtitle = "Telemetry, edge processing & data handling",
                        onClick = { onNavigate("privacy_policy") }
                    )

                    SettingsRowTile(
                        icon = Icons.Default.Description,
                        title = "Terms & Conditions",
                        subtitle = "Operational guidelines, safety interlocks & compliance",
                        onClick = { onNavigate("terms_and_conditions") }
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    firebaseService.signOut()
                    onSignOut()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                PrimaryButton(title = "Save", onClick = {
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
    var autoSyncRtdb by remember { mutableStateOf(preferences.autoSyncRtdb) }
    var esp32LinkActive by remember { mutableStateOf(preferences.esp32LinkActive) }
    var highPrecisionAi by remember { mutableStateOf(preferences.highPrecisionAi) }
    var tsrInterlockEnabled by remember { mutableStateOf(preferences.tsrInterlockEnabled) }
    var syncFeedbackMsg by remember { mutableStateOf<String?>(null) }

    val firebaseService = RailGuardFirebaseService.instance
    var rpiIp by remember { mutableStateOf(firebaseService.localWifiIp) }
    var rpiApiKey by remember { mutableStateOf(firebaseService.localWifiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var pingResultMsg by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "System & Sensor Configurations", subtitle = "Raspberry Pi, Wi-Fi & Safety Directives", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))

        // Direct Raspberry Pi Hardware & Wi-Fi Gateway Card
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RASPBERRY PI WI-FI LINK", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF0284C7))
                    }
                    com.example.railguard.components.StatusPill(
                        label = if (firebaseService.isDirectWifiConnected) "CONNECTED (${firebaseService.directWifiLatencyMs}ms)" else "DISCONNECTED",
                        tone = if (firebaseService.isDirectWifiConnected) com.example.railguard.model.Tone.HEALTHY else com.example.railguard.model.Tone.WARNING
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rpiIp,
                    onValueChange = {
                        rpiIp = it
                        firebaseService.updateLocalWifiIp(it)
                    },
                    label = { Text("Raspberry Pi Wi-Fi IP Address") },
                    placeholder = { Text("192.168.4.1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rpiApiKey,
                    onValueChange = {
                        rpiApiKey = it
                        firebaseService.updateLocalWifiApiKey(it)
                    },
                    label = { Text("Pi API Key (optional)") },
                    placeholder = { Text("Leave empty if the Pi has no --local-api-key") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) androidx.compose.ui.text.input.VisualTransformation.None
                        else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showApiKey = !showApiKey }) {
                            Text(if (showApiKey) "Hide" else "Show", fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Must match RAILGUARD_API_KEY / --local-api-key on the Pi. Sent as X-API-Key.",
                    fontSize = 9.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isPinging = true
                            pingResultMsg = null
                            scope.launch {
                                val (ok, latency) = firebaseService.testDirectWifiConnection(rpiIp)
                                isPinging = false
                                pingResultMsg = if (ok) {
                                    "✓ Successfully connected to Raspberry Pi (:8080)! Latency: ${latency}ms"
                                } else {
                                    "✗ Cannot reach http://$rpiIp:8080. Check Pi Wi-Fi & port 8080."
                                }
                            }
                        },
                        enabled = !isPinging,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPinging) "Testing..." else "Test Wi-Fi Ping", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            rpiIp = "192.168.4.1"
                            firebaseService.updateLocalWifiIp("192.168.4.1")
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset AP IP", fontSize = 11.sp)
                    }
                }

                if (pingResultMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pingResultMsg ?: "",
                        color = if (pingResultMsg?.startsWith("✓") == true) Color(0xFF16A34A) else Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Live Sensor Diagnostics
                val hw = firebaseService.hardwareHealth
                if (hw != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("ACTIVE HARDWARE SENSORS", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Camera: " + (if (hw.cameraOnline) "${hw.cameraSensor} (${hw.cameraFps} FPS)" else "not installed"),
                            fontSize = 11.sp, color = if (hw.cameraOnline) Color(0xFF16A34A) else Color(0xFFDC2626), fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Battery: " + (hw.batteryPercent?.let { "$it% (${hw.batteryVoltage ?: 0f}V)" } ?: "not monitored"),
                            fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sensors: ${hw.ultrasonicSensor} | ${hw.vibrationSensor} | ${hw.temperatureSensor}", fontSize = 10.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "CPU: ${hw.cpuTempC?.let { "%.1f°C".format(it) } ?: "not reported"} · GNSS: ${if (hw.gpsOnline) "locked" else "unavailable"} · ${hw.status}",
                        fontSize = 10.sp, color = Color(0xFF475569)
                    )
                }
            }
        }

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
                        Text("Auto-Sync Sensor Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Continuous telemetry stream from sensors", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("IoT Sensor Interface", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    title = "Save",
                    icon = Icons.Default.Save,
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
                            onUpdatePreferences(
                                preferences.copy(
                                    autoSync = autoSyncRtdb,
                                    autoSyncRtdb = autoSyncRtdb,
                                    esp32LinkActive = esp32LinkActive,
                                    highPrecisionAi = highPrecisionAi,
                                    tsrInterlockEnabled = tsrInterlockEnabled
                                )
                            )
                            syncFeedbackMsg = "✓ System preferences saved"
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
    LaunchedEffect(Unit) { onBack() }
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
fun AboutScreen(
    onBack: () -> Unit,
    onNavigatePrivacyPolicy: () -> Unit = {},
    onNavigateTerms: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "About RailGuard Platform", subtitle = "Autonomous track integrity & safety architecture", onBack = onBack)
        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0284C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Train, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("RailGuard Autonomous Track AI", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                Text("Production Build v1.0.0 · Field Deployment Edition", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "RailGuard is a mission-critical permanent way inspection and surveillance platform engineered for automated flaw detection, real-time sensor fusion, ultrasonic railhead verification, and automated train signaling interlocks.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. Hardware & Sensor Gateway Stack", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricRow("Compute Host", "Raspberry Pi 4B / 5 (ARM Quad-Core Linux)")
                        MetricRow("Optical Sensor", "Sony IMX219 / OV5647 Macro HDR (1080p@30FPS)")
                        MetricRow("Ultrasonic Transducer", "5MHz High-Frequency Piezo Gauge Thickness")
                        MetricRow("Inertial Profiler", "MPU-6050 / ADXL345 6-DoF Vibration & Shocks")
                        MetricRow("Thermal Transducer", "DS18B20 Rail Temperature (CWR Neutral Monitoring)")
                        MetricRow("Trackside Buzzer", "5V Active Piezo Alarm & Strobe Interlock")
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. Edge AI & Crack Detection Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Computer Vision Neural Core: Live contour and morphological gradient analysis for transverse fissures, squats, head checks, and rail burns.\n" +
                            "• Paris-Erdogan Fatigue Growth Kinetics: Predicts remaining life cycles (da/dN) and stress intensity factors (ΔK).\n" +
                            "• Dynamic Track Health: Real-time calculation from ultrasonic wear, vibration shocks, and active defect severities.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3. Dual-Channel Network & Interlocks", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Direct Wi-Fi REST Gateway: Field ad-hoc connectivity over http://192.168.4.1:8080 without requiring public internet.\n" +
                            "• Cloud RTDB Synchronization: Central dispatch signaling with real-time cab signaling and telemetry broadcast.\n" +
                            "• TSR Interlock: Automated slow orders (25, 60, 120 km/h) dispatched to both Raspberry Pi and Cloud.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("4. Safety & Industry Compliance", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Compliant with EN 13848 (Track Geometry Quality), AREMA Chapter 4 (Rail Safety Standards), and IRSE Level 3 Signalling Principles.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigatePrivacyPolicy,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privacy Policy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }

                    OutlinedButton(
                        onClick = onNavigateTerms,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terms & Conditions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun AttentionScreen(
    defects: List<Defect> = emptyList(),
    tasks: List<MaintenanceTask> = emptyList(),
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
    val topDefect = defects.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Urgent Action Required", subtitle = "High severity alerts", onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (topDefect != null) {
                    Text("Critical ${topDefect.section} Alert", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Red)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${topDefect.title} at ${topDefect.chainageCoordinate}. Risk score: ${topDefect.riskScore}/100.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onNavigateDefect, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                            Text("View Defect")
                        }
                        Button(onClick = onCreateTask) {
                            Text("Dispatch Gang")
                        }
                    }
                } else {
                    Text("All Track Sectors Clear", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Zero critical defects or emergency restrictions pending. Monitored corridors nominal.", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onBack: () -> Unit,
    onClearAll: () -> Unit = {},
    onMarkAllRead: () -> Unit = {}
) {
    val isDark = LocalIsDark.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Dispatch & System Alerts", subtitle = "${notifications.size} recorded notifications", onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))

        if (notifications.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onMarkAllRead) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0284C7))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark All Read", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onClearAll) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (notifications.isEmpty()) {
            RailCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No System Alerts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "All corridor sections and telemetry streams are nominal. Emergency slow orders or track hazard alerts will appear here.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

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
                            Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text(notif.message, fontSize = 11.sp, color = Color(0xFF475569))
                            Text(notif.time, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Privacy Policy", subtitle = "Telemetry handling, edge processing & data governance", onBack = onBack)
        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. Data Scope & Collection", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "RailGuard operates solely for track integrity inspection, railway infrastructure safety monitoring, and automated train signaling. Telemetry captured includes optical camera frames, 5MHz ultrasonic thickness echoes, 3-axis vibration/acceleration vectors, rail surface temperatures, and high-precision GNSS/RTK coordinates. No personally identifiable consumer data or commercial passenger information is collected.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. On-Device & Direct Wi-Fi Edge Processing", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "All Computer Vision defect classification and acoustic signal processing are performed directly on the edge hardware (Raspberry Pi & Android device). Direct Wi-Fi communications occur entirely within the closed local network of the inspection vehicle (192.168.4.x) and never transit public internet servers unless cloud sync is explicitly enabled.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3. Cloud Synchronization & Encryption", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "When connected to central railway cloud services, all telemetry, defect logs, and speed restriction orders are transmitted via TLS 1.3 encrypted HTTPS endpoints with authenticated tokens. Access is restricted to certified railway operators and maintenance engineers.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("4. Local Storage & Cache Governance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Local inspection records, scheduled patrols, and notification logs are stored in private sandboxed SharedPreferences and local storage. Inspectors may export audit PDF reports to device Downloads or clear cached notifications at any time.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TermsAndConditionsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        SubScreenHeader(title = "Terms & Conditions", subtitle = "Operational rules, safety interlocks & compliance terms", onBack = onBack)
        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. Mission-Critical Operational Status", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "RailGuard is an automated decision-support system designed to assist certified Permanent Way Inspectors. While the AI neural classifier and ultrasonic algorithms detect defects and recommend emergency slow orders, final track certification, line reopening, and repair approval remain the strict responsibility of authorized railway personnel.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. Temporary Speed Restriction (TSR) Protocol", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Emergency slow orders (25 km/h, 60 km/h, 120 km/h) dispatched through this platform interface with trackside signaling relays and cab communication units. Field operators must follow standard operating operating rules (SOR) and notify Central Train Control immediately when issuing severe restrictions.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3. Hardware Integrity & Sensor Maintenance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Correct operation requires proper alignment and regular calibration of the optical camera lens, 5MHz ultrasonic piezoelectric transducer, acoustic buzzer, and inertial shock sensors. Damaged probes or degraded batteries must be replaced before conducting passenger line sweeps.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                RailCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("4. Railway Safety Standard Compliance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "This system is engineered in accordance with European Standard EN 13848 (Track Geometry Quality), AREMA Section 4, and IRSE Level 3 signalling safety integrity. Audit packages and PDF exports represent legal records for permanent way maintenance archives.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
