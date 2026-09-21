package com.example.railguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun ReportsScreen(
    onBuildPackage: () -> Unit,
    onOpenPdf: (String) -> Unit,
    onOpenShare: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    val reports = listOf(
        Triple("North Corridor Weekly Safety Report", "REP-2024-W25 · 42 frames · Signed by E. Chen", Tone.CRITICAL),
        Triple("East Junction Routine Assessment", "REP-2024-W24 · 58 frames · Signed by J. Patel", Tone.HEALTHY),
        Triple("AI Anomaly & Crack Growth Digest", "REP-2024-DG06 · Multimodal vision & inertial trace", Tone.INFO)
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
                title = "Reports & Evidence",
                subtitle = "Cryptographically signed safety packages",
                onBack = onBack
            )
        }

        item {
            PrimaryButton(
                title = "Build Evidence Package",
                icon = Icons.Default.AddModerator,
                onClick = onBuildPackage
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            SectionLabel(title = "PUBLISHED FIELD DOSSIERS")
        }

        items(reports) { (title, subtitle, tone) ->
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onOpenPdf(title) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PDF DOSSIER",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(label = "VERIFIED SHA-256", tone = tone)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Share link →",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onOpenShare(title) }
                    )
                }
            }
        }
    }
}

@Composable
fun BuildEvidencePackageScreen(
    onGenerated: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var includeFrames by remember { mutableStateOf(true) }
    var includeGps by remember { mutableStateOf(true) }
    var includeAiScores by remember { mutableStateOf(true) }
    var includeDynamics by remember { mutableStateOf(true) }
    var includeSignatures by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Build Evidence Pack",
                subtitle = "Bundle field data into an immutable audit package",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "SELECT DATA CHUNKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                EvidenceCheckboxRow("42 High-Resolution Visual Frames", includeFrames) { includeFrames = it }
                EvidenceCheckboxRow("RTK GPS Coordinates & Chainage", includeGps) { includeGps = it }
                EvidenceCheckboxRow("AI Defect Bounding Boxes & Confidence", includeAiScores) { includeAiScores = it }
                EvidenceCheckboxRow("Inertial Accelerometer & Geometry Trace", includeDynamics) { includeDynamics = it }
                EvidenceCheckboxRow("Engineer Digital Signatures & License", includeSignatures) { includeSignatures = it }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "CRYPTOGRAPHIC INTEGRITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SHA-256 HASH: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = "Compile & Sign Evidence Pack",
                icon = Icons.Default.Lock,
                onClick = onGenerated
            )
        }
    }
}

@Composable
fun EvidenceCheckboxRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PdfPreviewScreen(
    reportTitle: String,
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
                title = "PDF Report Dossier",
                subtitle = reportTitle,
                onBack = onBack
            )
        }

        item {
            // Simulated PDF page container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(20.dp)
            ) {
                // Official Document Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RAILGUARD SAFETY DOSSIER",
                            color = Color(0xFF1459A6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "RAILWAY INSPECTION & INTEGRITY AUDIT",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    StatusPill(label = "OFFICIAL RECORD", tone = Tone.HEALTHY)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Executive Summary",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "On 2024-06-18, field patrol INSP-240618-04 identified critical defect CRK-2048 at chainage 14+320 of the North Loop Line. An immediate 25 km/h speed restriction has been imposed and dispatched to maintenance work order MT-881.",
                    color = Color(0xFF333333),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Defect Table
                Text(
                    text = "Identified Findings Register",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0))
                        .padding(10.dp)
                ) {
                    Text("CRK-2048 · Gauge Crack · 46 mm · Risk 92 · RESTRICTION 25 km/h", color = Color(0xFFB42318), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("CRK-2044 · Head Check · 28 mm · Risk 74 · MONITOR", color = Color(0xFF9A6700), fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Digital Signature Seal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SIGNATURE:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("E. Chen, Safety Lead", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("License IRSE-UK #849201", color = Color.Gray, fontSize = 10.sp)
                    }
                    Text("AUDIT HASH VERIFIED", color = Color(0xFF19734A), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun ShareReportScreen(
    reportTitle: String,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var copied by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Share Evidence Dossier",
                subtitle = reportTitle,
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "SECURE AUDIT LINK",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "https://audit.railguard.field/dossier/rep-2024-w25?token=sha256-e3b0c44",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                PrimaryButton(
                    title = if (copied) "Link Copied to Clipboard ✓" else "Copy Secure Link",
                    icon = Icons.Default.Share,
                    onClick = { copied = true }
                )
            }
        }
    }
}

@Composable
fun AnalyticsScreen(
    onNavigateTrackHealth: () -> Unit,
    onNavigateCrackAnalytics: () -> Unit,
    onNavigateRiskAnalytics: () -> Unit,
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
                title = "Network Analytics",
                subtitle = "Telemetry, health indices, and defect propagation",
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "94.2%",
                    label = "Track health",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTrackHealth
                )
                MetricTile(
                    value = "18",
                    label = "Active cracks",
                    tone = Tone.CRITICAL,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateCrackAnalytics
                )
                MetricTile(
                    value = "88.5%",
                    label = "Coverage",
                    tone = Tone.INFO,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateRiskAnalytics
                )
            }
        }

        item {
            SectionLabel(title = "DETAILED REPORT PANELS")

            ListRow(
                icon = Icons.Default.TrendingUp,
                title = "Monthly Track Health Trend",
                subtitle = "Historical trend: 55.4% in Jan → 94.2% in Jun",
                trailing = "94.2%",
                tone = Tone.HEALTHY,
                onClick = onNavigateTrackHealth
            )
            ListRow(
                icon = Icons.Default.ShowChart,
                title = "Crack Propagation Velocity",
                subtitle = "Analysis of growth rates across rail steel grades",
                trailing = "18 cracks",
                tone = Tone.CRITICAL,
                onClick = onNavigateCrackAnalytics
            )
            ListRow(
                icon = Icons.Default.PieChart,
                title = "Corridor Risk Distribution",
                subtitle = "North Loop holds 52% of high-severity anomalies",
                trailing = "52% North",
                tone = Tone.WARNING,
                onClick = onNavigateRiskAnalytics
            )
        }
    }
}

@Composable
fun TrackHealthScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val months = listOf(
        Pair("Jan", 55.4f),
        Pair("Feb", 68.2f),
        Pair("Mar", 79.1f),
        Pair("Apr", 86.3f),
        Pair("May", 91.0f),
        Pair("Jun", 94.2f)
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
                title = "Track Health Index",
                subtitle = "Continuous improvement across surveyed network",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "CURRENT NETWORK HEALTH: 94.2%",
                    style = MaterialTheme.typography.labelSmall,
                    color = toneColor(Tone.HEALTHY, LocalIsDark.current),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                months.forEach { (m, score) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = m, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "$score%", style = MaterialTheme.typography.bodyMedium, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = colorScheme.primary,
                            trackColor = colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
