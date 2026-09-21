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
    val isDark = LocalIsDark.current

    val individualReportTypes = listOf(
        Triple("Daily Shift Safety Report", "Full patrol sweep, chainage coverage, active restrictions & sign-off", Tone.INFO),
        Triple("Critical Defect & Rupture Dossier", "CRK-2048 growth kinetics, ultrasonic trace, derailment factor", Tone.CRITICAL),
        Triple("Maintenance Work Orders & Dispatch", "MT-881 clip pair repair, welder schedule & clearance window", Tone.WARNING),
        Triple("Track Geometry & Alignment Audit", "Gauge uniformity, cant deficiency, cross-level variance index", Tone.HEALTHY),
        Triple("Thermal Stress & CWR Buckle Risk", "Continuous welded rail neutral temp evaluation & expansion", Tone.WARNING),
        Triple("Cryptographic Evidence Bundle", "Multi-sensor frames, RTK GPS logs & SHA-256 digital seals", Tone.HEALTHY)
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
                subtitle = "Cryptographically signed safety packages & PDF generation",
                onBack = onBack
            )
        }

        // Prominent Daily Report Generation Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0C243C) else Color(0xFFE0F2FE)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF0284C7) else Color(0xFF38BDF8),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(label = "DAILY SAFETY PASS", tone = Tone.INFO)
                        Text(
                            text = "Shift 1 · 2024-06-18",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Generate Today's Daily Shift Safety Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Instantly compile all findings, 42 visual frames, speed restrictions, and engineer signatures into a colorful, verified PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryButton(
                        title = "Generate Daily Report PDF",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = { onOpenPdf("Daily Shift Safety Report · 2024-06-18") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Custom Multi-Sensor Evidence Bundle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Build Bundle",
                    subtitle = "Custom sensor chunks",
                    icon = Icons.Default.AddModerator,
                    onClick = onBuildPackage,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Share Link",
                    subtitle = "Dispatcher portal",
                    icon = Icons.Default.Share,
                    onClick = { onOpenShare("North Corridor Weekly Safety Report") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            SectionLabel(title = "INDIVIDUAL REPORT GENERATORS")
        }

        items(individualReportTypes) { (title, subtitle, tone) ->
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
                        text = "PDF GENERATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(label = tone.name, tone = tone)
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share link →",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onOpenShare(title) }
                    )

                    Button(
                        onClick = { onOpenPdf(title) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
    var isDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var selectedPage by remember { mutableIntStateOf(1) }

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            kotlinx.coroutines.delay(1200)
            isDownloading = false
            isDownloaded = true
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
                title = "Verified PDF Document",
                subtitle = reportTitle,
                onBack = onBack
            )
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isDownloading = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) Color(0xFF16A34A) else colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = "Download PDF",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDownloading) "Generating PDF..." else if (isDownloaded) "PDF Downloaded ✓" else "Download PDF (3.2 MB)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { /* Print simulated */ },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print PDF",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page Selector Tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1 to "Page 1: Executive Findings", 2 to "Page 2: Sensor Telemetry & Hashes").forEach { (pg, title) ->
                    val isSel = selectedPage == pg
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) colorScheme.primary else colorScheme.surfaceVariant)
                            .clickable { selectedPage = pg }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Clean and Colorful PDF Sheet
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                    .padding(18.dp)
            ) {
                // Official Colorful Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0369A1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RAILGUARD AUDIT NETWORK",
                                color = Color(0xFF0369A1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "INFRASTRUCTURE SAFETY & DERAILMENT PREVENTION",
                                color = Color(0xFF64748B),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFDC2626))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SAFETY RESTRICTION",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedPage == 1) {
                    // Page 1: Executive Summary & Table
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "DOCUMENT REF:", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "REP-2024-W25/S14", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text(text = "DATE OF AUDIT:", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "2024-06-18 14:32 UTC", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "CORRIDOR:", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "North Loop (Sec 14)", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Colorful Metric Highlight Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("DEFECT SEVERITY", color = Color(0xFFDC2626), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("CRITICAL 92/100", color = Color(0xFF991B1B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF9C3))
                                .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("SPEED RESTRICTION", color = Color(0xFF854D0E), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("25 km/h CAP", color = Color(0xFF713F12), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFF6EE7B7), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("AUDIT STATUS", color = Color(0xFF059669), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("DISPATCHED", color = Color(0xFF065F46), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Executive Inspection Summary",
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "During visual & ultrasonic inspection pass INSP-240618-04, optical neural detection identified rapid propagation of transverse fatigue crack CRK-2048 at chainage 14+320. Crack has penetrated 46 mm into the rail head. Immediate derailment mitigation has been enforced.",
                        color = Color(0xFF334155),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Colorful Finding Table
                    Text(
                        text = "Identified Corridor Findings",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DEFECT ID / TYPE", color = Color(0xFF475569), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("CHAINAGE", color = Color(0xFF475569), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("ACTION", color = Color(0xFF475569), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Row 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CRK-2048 · Transverse Crack", color = Color(0xFFB91C1C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Depth: 46 mm · Severity: 92", color = Color(0xFF64748B), fontSize = 9.sp)
                            }
                            Text("14+320", color = Color(0xFF0F172A), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFEE2E2))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("RESTRICT 25", color = Color(0xFFB91C1C), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Row 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFAFAFA))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CRK-2044 · Head Check Flaw", color = Color(0xFFD97706), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Length: 28 mm · Severity: 74", color = Color(0xFF64748B), fontSize = 9.sp)
                            }
                            Text("14+890", color = Color(0xFF0F172A), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("MONITOR 48H", color = Color(0xFFD97706), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Page 2: Sensor Telemetry & Cryptographic Verification
                    Text(
                        text = "Technical Telemetry & Calibration",
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text("• Sensor Rig: Sony Pregius S 4K Optical + FLIR Boson LWIR + Olympus Ultrasonic", color = Color(0xFF334155), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• RTK GPS Correction: Base Station Oxford South (RTK Lock ± 1.2 cm)", color = Color(0xFF334155), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Rail Gauge Variance: +4.2 mm deviation from nominal 1435 mm standard", color = Color(0xFF334155), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Rail Temperature: 38.4°C (Safe limit below buckling threshold 54°C)", color = Color(0xFF334155), fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Cryptographic Package Ledger",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .clip(RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "SHA-256 ROOTHASH:",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TIMESTAMP STAMP: 2024-06-18T14:32:00Z · BLOCK #849102",
                            color = Color(0xFF22C55E),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Official Signature Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CERTIFIED BY:", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("E. Chen · Chartered Safety Inspector", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("License IRSE-UK #849201 · Level 3", color = Color(0xFF64748B), fontSize = 9.sp)
                    }

                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF16A34A), RoundedCornerShape(4.dp))
                            .background(Color(0xFFF0FDF4))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "DIGITALLY SIGNED ✓",
                            color = Color(0xFF16A34A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
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
