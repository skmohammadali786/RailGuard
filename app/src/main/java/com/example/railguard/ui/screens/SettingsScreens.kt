package com.example.railguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.railguard.model.AppLanguage
import com.example.railguard.model.AppPreferences
import com.example.railguard.model.LocalAppSettings
import com.example.railguard.model.NotificationItem
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    profileName: String = "E. Chen",
    onLockApp: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings = LocalAppSettings.current
    val initials = profileName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifEmpty { "EC" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = settings.translate("control_settings"),
                subtitle = "Inspector credentials, device preferences, and security",
                onBack = onBack
            )
        }

        // Profile Badge Card
        item {
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onNavigate("profile") }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profileName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Lead Safety Inspector · IRSE #849201",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Toggles
        item {
            SectionLabel(title = "INTERFACE PREFERENCES")
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dark Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "High-contrast theme for nighttime inspection",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode
                    )
                }
            }
        }

        // Settings Navigation Rows
        item {
            SectionLabel(title = "SYSTEM MODULES")

            ListRow(
                icon = Icons.Default.Train,
                title = settings.translate("train_telemetry"),
                subtitle = "Live ETCS Level 2 train connection, speed & dispatch",
                trailing = "Live",
                tone = Tone.HEALTHY,
                onClick = { onNavigate("train_connection") }
            )
            ListRow(
                icon = Icons.Default.Psychology,
                title = settings.translate("ai_oracle_title"),
                subtitle = "Tensor crack kinetics, derailment risk & Paris law",
                trailing = "92.4%",
                tone = Tone.INFO,
                onClick = { onNavigate("ai_oracle") }
            )
            ListRow(
                icon = Icons.Default.PriorityHigh,
                title = settings.translate("attention_required"),
                subtitle = "Active restrictions and open priority tasks",
                trailing = "5",
                tone = Tone.CRITICAL,
                onClick = { onNavigate("attention") }
            )
            ListRow(
                icon = Icons.Default.Notifications,
                title = settings.translate("notifications"),
                subtitle = "Shift alerts, defect alerts, and system notices",
                trailing = "4",
                tone = Tone.INFO,
                onClick = { onNavigate("notifications") }
            )
            ListRow(
                icon = Icons.Default.Security,
                title = settings.translate("security_audit"),
                subtitle = if (settings.passcodeEnabled) "PIN Active (${settings.passcodePin}) · Biometric Active" else "Protection Disabled",
                trailing = if (settings.passcodeEnabled) "ON" else "OFF",
                tone = if (settings.passcodeEnabled) Tone.HEALTHY else Tone.WARNING,
                onClick = { onNavigate("security") }
            )
            ListRow(
                icon = Icons.Default.Lock,
                title = settings.translate("lock_app_now"),
                subtitle = "Require PIN (${settings.passcodePin}) or Biometric to unlock",
                trailing = "Lock",
                tone = Tone.WARNING,
                onClick = { onLockApp?.invoke() }
            )
            ListRow(
                icon = Icons.Default.Tune,
                title = settings.translate("app_settings"),
                subtitle = if (settings.isMetric) "Metric units (mm, km/h) · Auto-Sync On" else "Imperial units (in, mph) · Auto-Sync On",
                trailing = if (settings.isMetric) "Metric" else "Imperial",
                onClick = { onNavigate("app_settings") }
            )
            ListRow(
                icon = Icons.Default.Translate,
                title = settings.translate("language_standards"),
                subtitle = settings.language.displayName,
                trailing = settings.language.code.uppercase(),
                tone = Tone.INFO,
                onClick = { onNavigate("language") }
            )
            ListRow(
                icon = Icons.Default.Help,
                title = settings.translate("field_engineering"),
                subtitle = "Track tolerances, crack codes, and camera guidance",
                onClick = { onNavigate("help") }
            )
            ListRow(
                icon = Icons.Default.Info,
                title = settings.translate("about_railguard"),
                subtitle = "Version 1.0.0 · SHA-256 integrity active",
                onClick = { onNavigate("about") }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = settings.translate("sign_out"),
                icon = Icons.AutoMirrored.Filled.Logout,
                secondary = true,
                onClick = onSignOut
            )
        }
    }
}

@Composable
fun ProfileScreen(
    name: String,
    email: String,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var editName by remember { mutableStateOf(name) }
    var editEmail by remember { mutableStateOf(email) }
    var saved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Inspector Profile",
                subtitle = "Operational credentials & engineering license",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it; saved = false },
                    label = { Text("Inspector Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editEmail,
                    onValueChange = { editEmail = it; saved = false },
                    label = { Text("Official Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Role", value = "Lead Safety Inspector")
                DetailRow(label = "Engineering License", value = "IRSE-UK #849201")
                DetailRow(label = "Assigned Division", value = "North Corridor (Sec 01-16)")
                DetailRow(label = "Certification Validity", value = "Valid until Dec 2026")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = if (saved) "Profile Saved ✓" else "Update Credentials",
                icon = Icons.Default.Check,
                onClick = {
                    onSave(editName, editEmail)
                    saved = true
                }
            )
        }
    }
}

@Composable
fun AppSettingsScreen(
    preferences: AppPreferences,
    onUpdatePreferences: (AppPreferences) -> Unit,
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
                title = preferences.translate("app_settings"),
                subtitle = "Field units and local caching configuration",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preferences.translate("metric_units"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (preferences.isMetric) "Using mm for crack width & km/h for speeds" else "Using inches for crack width & mph for speeds",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.isMetric,
                        onCheckedChange = { onUpdatePreferences(preferences.copy(isMetric = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preferences.translate("auto_sync"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = preferences.translate("auto_sync_sub"),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.autoSync,
                        onCheckedChange = { onUpdatePreferences(preferences.copy(autoSync = it)) }
                    )
                }
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
    val colorScheme = MaterialTheme.colorScheme
    var currentPinInput by remember { mutableStateOf(preferences.passcodePin) }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = preferences.translate("security_audit"),
                subtitle = "Cryptographic protection for restriction releases & terminal locking",
                onBack = onBack
            )
        }

        if (saveStatusMsg != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF16A34A).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF16A34A), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = saveStatusMsg ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preferences.translate("passcode_pin_protection"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Mandatory 4-digit PIN before releasing speed limits or leaving idle", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.passcodeEnabled,
                        onCheckedChange = {
                            onUpdatePreferences(preferences.copy(passcodeEnabled = it))
                            saveStatusMsg = if (it) "Passcode PIN protection enabled" else "Passcode PIN protection disabled"
                        }
                    )
                }

                if (preferences.passcodeEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                currentPinInput = it
                            }
                        },
                        label = { Text("4-Digit Passcode PIN (Active: ${preferences.passcodePin})") },
                        placeholder = { Text("e.g. 1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (currentPinInput.length == 4) {
                                onUpdatePreferences(preferences.copy(passcodePin = currentPinInput))
                                saveStatusMsg = "Passcode PIN updated to $currentPinInput"
                            } else {
                                saveStatusMsg = "PIN must be exactly 4 digits"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(preferences.translate("save_passcode"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preferences.translate("biometric_authentication"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Fingerprint / Face unlock for rapid field HUD resumption", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.isBiometricEnabled,
                        onCheckedChange = {
                            onUpdatePreferences(preferences.copy(isBiometricEnabled = it))
                            saveStatusMsg = if (it) "Biometric authentication enabled" else "Biometric authentication disabled"
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = preferences.translate("lock_app_now"),
                icon = Icons.Default.Lock,
                onClick = onLockApp
            )
        }
    }
}

@Composable
fun LanguageScreen(
    preferences: AppPreferences,
    onUpdatePreferences: (AppPreferences) -> Unit,
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
                title = preferences.translate("language_standards"),
                subtitle = "Select operational vocabulary and engineering terminology",
                onBack = onBack
            )
        }

        items(AppLanguage.entries.toList()) { lang ->
            val isSelected = preferences.language == lang
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onUpdatePreferences(preferences.copy(language = lang)) },
                backgroundColor = if (isSelected) colorScheme.primary.copy(alpha = 0.08f) else colorScheme.surface,
                borderColor = if (isSelected) colorScheme.primary else colorScheme.outline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = lang.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                            text = "Regional profile · ${lang.code.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HelpCenterScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    val faqs = listOf(
        Pair("How is the AI Risk Score calculated?", "Scores range from 0 to 100 based on crack length, orientation (transverse vs longitudinal), track curvature, and axle-load cycles."),
        Pair("What triggers an immediate speed restriction?", "Any gauge corner crack exceeding 35 mm or propagating at >0.3 mm/day mandates an automated 25 km/h restriction notice."),
        Pair("How to calibrate the optical gauge?", "Hold camera perpendicular to rail head at 40 cm distance. The HUD uses standard 60E1 rail width (72 mm rail head) as reference.")
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
                title = "Field Engineering Guides",
                subtitle = "Standard procedures, tolerances, and HUD calibration",
                onBack = onBack
            )
        }

        items(faqs) { (q, a) ->
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = q,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = a,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
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
                title = "About RailGuard",
                subtitle = "Platform architecture & integrity certificate",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "RailGuard Control Edition",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI-Powered Railway Crack Detection & Safety Platform",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                DetailRow(label = "Version", value = "1.0.0 (Build 2026.09)")
                DetailRow(label = "Framework", value = "Android Jetpack Compose")
                DetailRow(label = "Neural Engine", value = "Railway Multimodal Vision v3")
                DetailRow(label = "Encryption", value = "AES-256 / SHA-256 Immutable Trace")
                DetailRow(label = "Compliance", value = "EN 50128 / CENELEC SIL-2 Ready")
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
    val colorScheme = MaterialTheme.colorScheme

    val attentionItems = listOf(
        Triple("CRK-2048: 46 mm Gauge Crack", "North Loop 14+320 · Speed restriction 25 km/h active", Tone.CRITICAL),
        Triple("MT-881: Replace Rail Clip Pair", "North Loop 14+320 · Work window due 18:00 today", Tone.CRITICAL),
        Triple("CRK-2044: Head Check Wear (28 mm)", "North Loop 14+108 · 2nd cycle monitor warning", Tone.WARNING),
        Triple("MT-878: Grind Head Check", "East Junction 03+660 · Due in 4 days", Tone.WARNING),
        Triple("Sensor Calibration Due", "Inertial unit accelerometer recalibration in 48h", Tone.INFO)
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
                title = "Requires Attention",
                subtitle = "Shift critical actions, diagnostic workspaces, and risk interventions",
                onBack = onBack
            )
        }

        // Dedicated Actions Hub
        item {
            SectionLabel(title = "CRITICAL WORKSPACES & INTERVENTIONS")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Create Task",
                    subtitle = "Work order dispatch",
                    icon = Icons.Default.AddCircle,
                    onClick = onCreateTask,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Compare Images",
                    subtitle = "Crack growth audit",
                    icon = Icons.Default.Compare,
                    onClick = onCompareImages,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "All Observations",
                    subtitle = "Raw sensor findings",
                    icon = Icons.Default.Visibility,
                    onClick = onAllObservations,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Review Detection",
                    subtitle = "Vision AI models",
                    icon = Icons.Default.CameraAlt,
                    onClick = onReviewDetection,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Evidence Pack",
                    subtitle = "Cryptographic PDF",
                    icon = Icons.Default.AddModerator,
                    onClick = onBuildEvidencePackage,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Risk Heatmap",
                    subtitle = "Corridor density",
                    icon = Icons.Default.Layers,
                    onClick = onRiskHeatmap,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionLabel(title = "HIGH PRIORITY FIELD FINDINGS")
        }

        items(attentionItems) { (title, subtitle, tone) ->
            ListRow(
                icon = Icons.Default.PriorityHigh,
                title = title,
                subtitle = subtitle,
                trailing = tone.name,
                tone = tone,
                onClick = {
                    if (title.startsWith("CRK")) onNavigateDefect() else onNavigateTask()
                }
            )
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
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
                title = "Notification Center",
                subtitle = "Dispatcher orders, defect alerts, and shift logs",
                onBack = onBack
            )
        }

        items(notifications) { notif ->
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notif.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(label = notif.tone.name, tone = notif.tone)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notif.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notif.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary
                )
            }
        }
    }
}
