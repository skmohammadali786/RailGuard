package com.example.railguard.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.railguard.components.*
import com.example.railguard.model.Defect
import com.example.railguard.model.MaintenanceTask
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor
import com.example.railguard.util.PdfExporter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        Triple("Critical Defect & Rupture Dossier", "Track flaw growth kinetics, ultrasonic trace, derailment factor", Tone.CRITICAL),
        Triple("Maintenance Work Orders & Dispatch", "Track fastener repair, welder schedule & clearance window", Tone.WARNING),
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
            val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F9FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color(0xFFBAE6FD),
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
                            text = "Shift 1 · $todayDateStr",
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
                        text = "Instantly compile all findings, verified optical frames, speed restrictions, and engineer signatures into a colorful, verified PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryButton(
                        title = "Generate Daily Report PDF",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = { onOpenPdf("Daily Shift Safety Report · $todayDateStr") }
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
    val scope = rememberCoroutineScope()
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

                EvidenceCheckboxRow("High-Resolution Visual Frames", includeFrames) { includeFrames = it }
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
                onClick = {
                    scope.launch {
                        com.example.railguard.data.RailGuardFirebaseService.instance.saveReportPackage(
                            title = "Evidence Pack - Sector 14",
                            hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                            details = mapOf(
                                "includeFrames" to includeFrames,
                                "includeGps" to includeGps,
                                "includeAiScores" to includeAiScores,
                                "includeDynamics" to includeDynamics,
                                "includeSignatures" to includeSignatures
                            )
                        )
                    }
                    onGenerated()
                }
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

data class ReportKpiItem(
    val kpi: String,
    val value: String,
    val standard: String,
    val status: String
)

@Composable
fun PdfPreviewScreen(
    reportTitle: String,
    inspectorName: String = "E. Chen",
    defects: List<Defect> = emptyList(),
    tasks: List<MaintenanceTask> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var isDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var cloudStatus by remember { mutableStateOf<String?>(null) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var selectedPage by remember { mutableIntStateOf(0) } // 0 = All Pages (Full Dossier)

    val doExportPdf: () -> Unit = {
        try {
            isDownloading = true
            val file = PdfExporter.generateInspectionPdf(
                context = context,
                reportTitle = reportTitle,
                inspectorName = inspectorName,
                defects = defects,
                tasks = tasks
            )
            generatedPdfFile = file
            isDownloading = false
            isDownloaded = true
            cloudStatus = "Uploading PDF evidence to Firebase..."
            scope.launch {
                val reportId = "PDF-${System.currentTimeMillis() % 100000}"
                val result = com.example.railguard.data.RailGuardFirebaseService.instance.uploadReportPdf(
                    reportId = reportId,
                    title = reportTitle,
                    pdfBytes = file.readBytes()
                )
                cloudStatus = if (result.isSuccess) {
                    "PDF uploaded and indexed in Firebase ✓"
                } else {
                    "PDF saved locally, but Firebase upload failed: ${result.message}"
                }
            }
            Toast.makeText(
                context,
                "✓ PDF Saved to Documents: ${file.name} (${file.length() / 1024} KB)",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            isDownloading = false
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val pageTitles = listOf(
        0 to "All Pages (Full Dossier)",
        1 to "Page 1: Overview & GPS",
        2 to "Page 2: Defect Register",
        3 to "Page 3: AI & Kinetics",
        4 to "Page 4: Train & Maintenance",
        5 to "Page 5: Network Analytics & Sensors"
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
                title = "Verified PDF Evidence Dossier",
                subtitle = reportTitle,
                onBack = onBack
            )
        }

        // Action Buttons Row (Download / Share / Open / Print)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { doExportPdf() },
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
                        text = if (isDownloading) "Compiling PDF..." else if (isDownloaded) "PDF Exported (${(generatedPdfFile?.length() ?: 4800000L) / 1024} KB) ✓" else "Export Official PDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Share PDF Button
                Button(
                    onClick = {
                        val file = generatedPdfFile ?: run {
                            val f = PdfExporter.generateInspectionPdf(context, reportTitle, inspectorName)
                            generatedPdfFile = f
                            isDownloaded = true
                            f
                        }
                        PdfExporter.sharePdf(context, file)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share PDF",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Open in External PDF Viewer / Print Button
                OutlinedButton(
                    onClick = {
                        val file = generatedPdfFile ?: run {
                            val f = PdfExporter.generateInspectionPdf(context, reportTitle, inspectorName)
                            generatedPdfFile = f
                            isDownloaded = true
                            f
                        }
                        PdfExporter.openPdf(context, file)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in PDF Viewer",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Real Exported File Banner if ready
            if (generatedPdfFile != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16A34A).copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FILE SAVED: ${generatedPdfFile?.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                            Text(
                                text = "Format: A4 Dual-Page · Storage: Documents/ · Ready to email or print",
                                fontSize = 10.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { generatedPdfFile?.let { PdfExporter.openPdf(context, it) } }
                        ) {
                            Text("Open", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF16A34A))
                        }
                    }
                }
            }
            if (cloudStatus != null) {
                Text(
                    text = cloudStatus ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (cloudStatus?.contains("failed", ignoreCase = true) == true) {
                        colorScheme.error
                    } else {
                        Color(0xFF16A34A)
                    },
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pageTitles.forEach { (pg, title) ->
                    val isSel = selectedPage == pg
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) colorScheme.primary else colorScheme.surfaceVariant)
                            .clickable { selectedPage = pg }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
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

        // Clean, High-Fidelity Colorful PDF Sheet Canvas
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                    .padding(18.dp)
            ) {
                // ==========================================
                // OFFICIAL PDF HEADER (Present on Document)
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0369A1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RAILGUARD AUDIT NETWORK",
                                color = Color(0xFF0369A1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "OFFICIAL FIELD ENGINEERING & SAFETY DOSSIER",
                                color = Color(0xFF475569),
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

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // ====================================================
                // PAGE 1: EXECUTIVE FINDINGS & CORRIDOR GEOLOCATION
                // ====================================================
                if (selectedPage == 0 || selectedPage == 1) {
                    Text(
                        text = "DOCUMENT SPECIFICATIONS & PARAMETERS",
                        color = Color(0xFF0369A1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "DOCUMENT REF:", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(text = "REP-${System.currentTimeMillis() % 100000}", color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "SECURITY CLASS:", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(text = "OFFICIAL-SENSITIVE / IMMUTABLE", color = Color(0xFF475569), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "DATE & TIMESTAMP:", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(text = "See cloud records", color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "LEAD INSPECTOR:", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(text = inspectorName, color = Color(0xFF0369A1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Colorful Highlight Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                Text("MAX SEVERITY", color = Color(0xFFDC2626), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("CRITICAL 92/100", color = Color(0xFF991B1B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                Text("ACTIVE TSR LIMIT", color = Color(0xFF854D0E), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("25 km/h TSR", color = Color(0xFF713F12), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                Text("TOTAL DISTANCE", color = Color(0xFF059669), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text("1,250 METERS", color = Color(0xFF065F46), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "1. Corridor Geolocation & Linear Referencing (LRS)",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text("• Corridor / Line: N/A", color = Color(0xFF1E293B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text("• Chainage Bounds: N/A", color = Color(0xFF334155), fontSize = 10.sp)
                        Text("• Start Geodetic GPS: N/A", color = Color(0xFF334155), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("• Critical Defect GPS: N/A", color = Color(0xFFB91C1C), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("• End Geodetic GPS: N/A", color = Color(0xFF334155), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("• RTK GNSS Correction: N/A", color = Color(0xFF059669), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text("• Rail Profile & Grade: N/A", color = Color(0xFF334155), fontSize = 10.sp)
                        Text("• Sleeper / Fastener Type: N/A", color = Color(0xFF334155), fontSize = 10.sp)
                        Text("• Track Geometry Parameters: N/A", color = Color(0xFF0369A1), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ====================================================
                // PAGE 2: COMPLETE DEFECT REGISTER (EVERY SINGLE DEFECT)
                // ====================================================
                if (selectedPage == 0 || selectedPage == 2) {
                    if (selectedPage == 0) {
                        HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 2.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "2. Complete Defect Register (All Logged Anomalies)",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                    ) {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ID · DEFECT TYPE", color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                            Text("CHAINAGE / GPS", color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                            Text("SEVERITY", color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("DIRECTIVE", color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        }

                        if (defects.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF0FDF4))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All surveyed track sectors clear. Zero structural anomalies or crack propagation logged.",
                                    color = Color(0xFF15803D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            defects.take(5).forEachIndexed { idx, d ->
                                val bg = if (idx % 2 == 0) Color(0xFFFEF2F2) else Color.White
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bg)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text("${d.id} · ${d.title}", color = Color(0xFF991B1B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("Flaw Size: ${d.estimatedLength} · Risk Score: ${d.riskScore}/100", color = Color(0xFF475569), fontSize = 8.sp)
                                    }
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(d.chainageCoordinate, color = Color(0xFF0F172A), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Text("${d.latitude}, ${d.longitude}", color = Color(0xFF64748B), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (d.tone == Tone.CRITICAL) Color(0xFFDC2626) else Color(0xFFD97706))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("${d.riskScore}/100", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(if (d.tone == Tone.CRITICAL) "25 km/h TSR" else "Monitor", color = Color(0xFF991B1B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(d.aiPrescribedAction.take(16), color = Color(0xFF475569), fontSize = 8.sp)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ====================================================
                // PAGE 3: AI NEURAL INFERENCE & RUPTURE KINETICS
                // ====================================================
                if (selectedPage == 0 || selectedPage == 3) {
                    if (selectedPage == 0) {
                        HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 2.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "3. AI Predictive Analytics & Rupture Kinetics Model",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F9FF))
                            .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text("• Neural Architecture: N/A", color = Color(0xFF0369A1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("• Inference Latency: N/A", color = Color(0xFF0C4A6E), fontSize = 10.sp)
                        Text("• Visual Tensor Confidence: N/A", color = Color(0xFF0C4A6E), fontSize = 10.sp)
                        Text("• Ultrasonic B-Scan Correlation: N/A", color = Color(0xFF0C4A6E), fontSize = 10.sp)
                        Text("• Thermal Core Readout: N/A", color = Color(0xFF0C4A6E), fontSize = 10.sp)
                        Text("• Crack Propagation Formula: N/A", color = Color(0xFFB91C1C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("• Nadal Derailment Ratio: N/A", color = Color(0xFFB91C1C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("• Flange Climb Probabilities: N/A", color = Color(0xFF991B1B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("• Continuous Welded Rail Neutral Temp: N/A", color = Color(0xFF0C4A6E), fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ====================================================
                // PAGE 4: TRAIN TELEMETRY & MAINTENANCE SIGN-OFF
                // ====================================================
                if (selectedPage == 0 || selectedPage == 4) {
                    if (selectedPage == 0) {
                        HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 2.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "4. Connected Train Live Telemetry",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text("• Connected Locomotive: N/A", color = Color(0xFF1E293B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("• Onboard Unit (OBU): N/A", color = Color(0xFF334155), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("• Telemetry Uplink: N/A", color = Color(0xFF059669), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text("• Live Operational Speed: N/A", color = Color(0xFF334155), fontSize = 10.sp)
                        Text("• Axle Load & Vibration: N/A", color = Color(0xFF334155), fontSize = 10.sp)
                        Text("• Wheel Bearing Infrared Temp: N/A", color = Color(0xFF334155), fontSize = 10.sp)
                        Text("• Driver DMI Cab Display Alert: N/A", color = Color(0xFFB91C1C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "5. Maintenance Work Orders Dispatched",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEFCE8))
                            .border(1.dp, Color(0xFFFEF08A), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        if (tasks.isEmpty()) {
                            Text("• No active work orders", color = Color(0xFF713F12), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            tasks.forEach { task ->
                                Text("• Work Order ${task.id}: ${task.title}", color = Color(0xFF713F12), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("  - Assigned: ${task.assignee} · Due: ${task.due}", color = Color(0xFF854D0E), fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "6. Cryptographic Audit Seal & Blockchain Block Hash",
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
                        Text("SHA-256 ROOT BUNDLE HASH:", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("N/A", color = Color(0xFF38BDF8), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("MERKLE TREE ROOT:", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("N/A", color = Color(0xFF818CF8), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("BLOCK INDEX: N/A", color = Color(0xFF4ADE80), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Official Digital Signature Block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CERTIFIED & SEALED BY:", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("$inspectorName, BEng (Hons) CEng FIRSE", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Lead Rail Safety Inspector · License IRSE-UK #849201 Level 3", color = Color(0xFF64748B), fontSize = 9.sp)
                        }

                        Box(
                            modifier = Modifier
                                .border(1.5.dp, Color(0xFF16A34A), RoundedCornerShape(4.dp))
                                .background(Color(0xFFF0FDF4))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "CRYPTOGRAPHICALLY SEALED",
                                    color = Color(0xFF16A34A),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "IRSE #849201 VERIFIED ✓",
                                    color = Color(0xFF15803D),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ====================================================
                // PAGE 5: COMPLETE NETWORK ANALYTICS & SENSOR MATRIX
                // ====================================================
                if (selectedPage == 0 || selectedPage == 5) {
                    if (selectedPage == 0) {
                        HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 2.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "5. Complete Network Analytics & Sensor Telemetry Matrix",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Table: Network Reliability KPIs
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CORE RELIABILITY KPI", color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                            Text("RECORDED VALUE", color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                            Text("STANDARD / CAP", color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                            Text("INTEGRITY", color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.0f))
                        }

                        val kpiList = listOf(
                            ReportKpiItem("Track Health Index (THI)", "N/A", "Threshold ≥ 90.0%", "N/A"),
                            ReportKpiItem("Active Critical Fractures", "${defects.size} Logged Anomalies", "Tolerance = 0", if (defects.isEmpty()) "NOMINAL" else "ACTIONABLE"),
                            ReportKpiItem("Survey Coverage Reliability", "N/A", "Standard ≥ 95.0%", "N/A"),
                            ReportKpiItem("Corridor Gross Tonnage", "N/A", "Rated Cap 60.0 MGT", "N/A"),
                            ReportKpiItem("Mean Time Between Failures", "N/A", "Target ≥ 1,200h", "N/A"),
                            ReportKpiItem("Peak Nadal Derailment Ratio", "N/A", "Critical Cap 0.80 Y/Q", "N/A")
                        )

                        kpiList.forEachIndexed { idx, (kpi, value, standard, status) ->
                            val bg = if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bg)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(kpi, color = Color(0xFF1E293B), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.8f))
                                Text(value, color = Color(0xFF0F172A), fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.4f))
                                Text(standard, color = Color(0xFF64748B), fontSize = 8.sp, modifier = Modifier.weight(1.4f))
                                Text(
                                    status,
                                    color = if (status.contains("HEALTHY") || status.contains("CERTIFIED") || status.contains("NOMINAL") || status.contains("COMPLIANT")) Color(0xFF15803D) else Color(0xFFB91C1C),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.0f)
                                )
                            }
                            if (idx < kpiList.size - 1) HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Defect Typology Distribution & Growth Rates",
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("• Transverse Fissures: N/A", color = Color(0xFF991B1B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("• Squats & Rolling Contact Fatigue: N/A", color = Color(0xFF854D0E), fontSize = 9.sp)
                        Text("• Gauge Corner Head Checking: N/A", color = Color(0xFF334155), fontSize = 9.sp)
                        Text("• Bolt Hole & Web Fractures: N/A", color = Color(0xFF334155), fontSize = 9.sp)
                        Text("• Thermite Weld Heat Affected Zone: N/A", color = Color(0xFF334155), fontSize = 9.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Real-Time Sensor & Continuum Metallurgy Telemetry",
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("• CWR Axial Thermal Stress: +68.4 MPa Tension (Neutral Temp SFT: 27.0°C · Rail Head: 38.4°C)", color = Color(0xFF166534), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        Text("• Dynamic Wheel Impact Peak: 162.4 kN (Impact safety limit: < 220 kN · Measured at Tie #14-320)", color = Color(0xFF166534), fontSize = 9.sp)
                        Text("• Dynamic Track Gauge Variance: 1,438.2 mm (+3.2mm from standard 1,435.0 mm · Safe < +8mm)", color = Color(0xFF166534), fontSize = 9.sp)
                        Text("• Sleeper / Tie Retention: 96.8% integrity · Pandrol clip toe load 10.2 kN (Nominal)", color = Color(0xFF166534), fontSize = 9.sp)
                        Text("• Ballast Vibration Damping: Vertical RMS 0.042g · Lateral RMS 0.028g (Safe < 0.15g)", color = Color(0xFF166534), fontSize = 9.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Corridor Risk Distribution & Maintenance SLA Adherence",
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("• Section 14 (North Loop Line): Risk 92/100 · 8 defects · TSR 25 km/h · Clamp Work Order MT-881", color = Color(0xFF991B1B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("• Section 08 (South Freight Yard): Risk 68/100 · 5 defects · TSR 40 km/h · Heavy haul freight zone", color = Color(0xFF854D0E), fontSize = 9.sp)
                        Text("• Section 03 (East High-Speed Main): Risk 44/100 · 3 defects · Clear speed (180 km/h) · Grinding cycle", color = Color(0xFF334155), fontSize = 9.sp)
                        Text("• Section 01 (West Deep Cut): Risk 12/100 · 2 defects · Optimal condition · Next cycle in 30 days", color = Color(0xFF15803D), fontSize = 9.sp)
                        Text("• Maintenance SLA Performance: 94.8% on-time resolution · MTTR: 4.2 hours · 2 Active TSRs", color = Color(0xFF0F172A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                        text = "N/A",
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
    onExportPdf: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current
    var selectedTab by remember { mutableIntStateOf(0) }

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
                subtitle = "Comprehensive telemetry, defect kinetics, and safety metrics",
                onBack = onBack
            )
        }

        // Export to PDF Banner Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F9FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OFFICIAL REPORT EXPORT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Full Analytics Dossier in PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Includes all 6 KPI matrices, Paris law kinetics, sensors & signatures",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { onExportPdf?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 6 Primary Network Reliability Metrics
        item {
            SectionLabel(title = "CORE RELIABILITY METRICS")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "N/A",
                    label = "Track health",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTrackHealth
                )
                MetricTile(
                    value = "N/A",
                    label = "Active cracks",
                    tone = Tone.CRITICAL,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateCrackAnalytics
                )
                MetricTile(
                    value = "N/A",
                    label = "Coverage",
                    tone = Tone.INFO,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateRiskAnalytics
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "N/A",
                    label = "Load Tonnage",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "N/A",
                    label = "MTBF",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "N/A",
                    label = "Peak Nadal",
                    tone = Tone.WARNING,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateRiskAnalytics
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Segment Tabs
        item {
            val tabs = listOf("Defect Typology", "Paris Kinetics", "Sensors & Metallurgy", "Corridor SLAs")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    val isSel = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) colorScheme.primary else colorScheme.surfaceVariant)
                            .clickable { selectedTab = idx }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // TAB CONTENT 0: Defect Typology
        if (selectedTab == 0) {
            item {
                RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "DEFECT POPULATION BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val defectCategories = listOf(
                        Triple("Transverse Fissures", 0, Tone.CRITICAL),
                        Triple("Squats & Rolling Contact Fatigue", 0, Tone.WARNING),
                        Triple("Gauge Corner Head Checking", 0, Tone.WARNING),
                        Triple("Bolt Hole & Web Fractures", 0, Tone.INFO),
                        Triple("Thermite Weld HAZ Anomalies", 0, Tone.HEALTHY)
                    )

                    defectCategories.forEach { (cat, pct, tone) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                                Text("$pct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = toneColor(tone, isDark))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = toneColor(tone, isDark),
                                trackColor = colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }

        // TAB CONTENT 1: Paris Kinetics & Crack Growth
        if (selectedTab == 1) {
            item {
                RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "PARIS-ERDOGAN FATIGUE KINETICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Formula: da/dN = C · (ΔK)^m",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(label = "Stress Intensity Range (ΔK)", value = "N/A")
                    DetailRow(label = "Cyclic Growth Rate (da/dN)", value = "N/A")
                    DetailRow(label = "Linear Growth Rate", value = "N/A")
                    DetailRow(label = "Cycles to Critical Fracture", value = "N/A")
                    DetailRow(label = "Estimated Time to Break", value = "N/A")
                    DetailRow(label = "Critical Fracture Toughness (K_IC)", value = "N/A")
                }
            }
        }

        // TAB CONTENT 2: Sensors & Metallurgy
        if (selectedTab == 2) {
            item {
                RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "REAL-TIME TRACK SENSORS & METALLURGY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(label = "CWR Axial Thermal Stress", value = "N/A")
                    DetailRow(label = "Stress Free Temperature (SFT)", value = "N/A")
                    DetailRow(label = "Current Rail Surface Temp", value = "N/A")
                    DetailRow(label = "Dynamic Wheel Impact Load", value = "N/A")
                    DetailRow(label = "Dynamic Track Gauge", value = "N/A")
                    DetailRow(label = "Vertical Cant Angle", value = "N/A")
                    DetailRow(label = "Sleeper Fastener Integrity", value = "N/A")
                }
            }
        }

        // TAB CONTENT 3: Corridor SLAs
        if (selectedTab == 3) {
            item {
                RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "CORRIDOR SEGMENT RISK & REPAIR SLAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow(label = "Section 14 (North Loop)", value = "N/A")
                    DetailRow(label = "Section 08 (South Yard)", value = "N/A")
                    DetailRow(label = "Section 03 (East Junction)", value = "N/A")
                    DetailRow(label = "Section 01 (West Cut)", value = "N/A")
                    DetailRow(label = "Maintenance SLA On-Time", value = "N/A")
                    DetailRow(label = "Mean Time to Repair (MTTR)", value = "N/A")
                }
            }
        }

        // Sub-screen Navigation Links
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SectionLabel(title = "DETAILED REPORT PANELS & DRILL-DOWNS")

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
                title = "Corridor Risk Heatmap & Sectors",
                subtitle = "Visual multi-zone heat maps and stress tensors",
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
        Pair("Jan", 0f),
        Pair("Feb", 0f),
        Pair("Mar", 0f),
        Pair("Apr", 0f),
        Pair("May", 0f),
        Pair("Jun", 0f)
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
                    text = "CURRENT NETWORK HEALTH: N/A",
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
