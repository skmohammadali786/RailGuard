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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
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
    onBack: (() -> Unit)? = null
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
                title = "Control Settings",
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
                            text = "EC",
                            color = colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "E. Chen",
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
                icon = Icons.Default.PriorityHigh,
                title = "Requires Immediate Attention",
                subtitle = "Active restrictions and open priority tasks",
                trailing = "5",
                tone = Tone.CRITICAL,
                onClick = { onNavigate("attention") }
            )
            ListRow(
                icon = Icons.Default.Notifications,
                title = "Notification Center",
                subtitle = "Shift alerts, defect alerts, and system notices",
                trailing = "4",
                tone = Tone.INFO,
                onClick = { onNavigate("notifications") }
            )
            ListRow(
                icon = Icons.Default.Security,
                title = "Security & Passcode",
                subtitle = "Biometric sign-off and audit pin",
                onClick = { onNavigate("security") }
            )
            ListRow(
                icon = Icons.Default.Tune,
                title = "App Configuration & Units",
                subtitle = "Metric units (mm, km/h), cache retention",
                onClick = { onNavigate("app_settings") }
            )
            ListRow(
                icon = Icons.Default.Translate,
                title = "Language & Regional Standards",
                subtitle = "English (UK / US), Hindi",
                onClick = { onNavigate("language") }
            )
            ListRow(
                icon = Icons.Default.Help,
                title = "Field Engineering Guides",
                subtitle = "Track tolerances, crack codes, and camera guidance",
                onClick = { onNavigate("help") }
            )
            ListRow(
                icon = Icons.Default.Info,
                title = "About RailGuard Control",
                subtitle = "Version 1.0.0 · SHA-256 integrity active",
                onClick = { onNavigate("about") }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = "Sign Out Session",
                icon = Icons.Default.ExitToApp,
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
fun AppSettingsScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var metricUnits by remember { mutableStateOf(true) }
    var autoSync by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "App Settings",
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
                    Column {
                        Text("Metric Units (mm, km/h)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Standard railway gauge specification", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = metricUnits, onCheckedChange = { metricUnits = it })
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-Sync Local Store", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Synchronize frames when network is available", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = autoSync, onCheckedChange = { autoSync = it })
                }
            }
        }
    }
}

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var passcodeRequired by remember { mutableStateOf(true) }
    var biometricActive by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Security & Audit",
                subtitle = "Cryptographic protection for restriction releases",
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
                    Column {
                        Text("Passcode on Sign-Off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Mandatory 6-digit PIN before releasing speed limits", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = passcodeRequired, onCheckedChange = { passcodeRequired = it })
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Biometric Authentication", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Fingerprint / Face unlock for quick HUD resumption", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = biometricActive, onCheckedChange = { biometricActive = it })
                }
            }
        }
    }
}

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedLang by remember { mutableStateOf("English (UK)") }
    val languages = listOf("English (UK)", "English (US)", "Hindi (हिंदी)", "Spanish (Español)")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Language & Locale",
                subtitle = "Select operational vocabulary and terminology",
                onBack = onBack
            )
        }

        items(languages) { lang ->
            val isSelected = selectedLang == lang
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { selectedLang = lang },
                backgroundColor = if (isSelected) colorScheme.primary.copy(alpha = 0.08f) else colorScheme.surface,
                borderColor = if (isSelected) colorScheme.primary else colorScheme.outline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lang,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
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
