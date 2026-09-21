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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.model.Defect
import com.example.railguard.model.Observation
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun DefectsListScreen(
    defects: List<Defect>,
    onSelectDefect: (Defect) -> Unit,
    onViewObservations: () -> Unit,
    onOpenDefectMap: () -> Unit,
    onOpenCrackGrowth: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf("All defects", "Critical only", "Warnings")

    val filtered = remember(searchQuery, selectedFilter, defects) {
        defects.filter {
            val matchesQuery = it.title.contains(searchQuery, ignoreCase = true) ||
                    it.id.contains(searchQuery, ignoreCase = true) ||
                    it.section.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                1 -> it.tone == Tone.CRITICAL
                2 -> it.tone == Tone.WARNING
                else -> true
            }
            matchesQuery && matchesFilter
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
                title = "Defect Register",
                subtitle = "Prioritized AI findings across operational corridors",
                onBack = onBack
            )
        }

        // Dedicated Defect Workspaces (3 Dedicated actions requested by user)
        item {
            SectionLabel(title = "DEFECT WORKSPACES")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Defect Map",
                    subtitle = "Corridor GIS pins",
                    icon = Icons.Default.Place,
                    onClick = onOpenDefectMap,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Observations",
                    subtitle = "Raw AI bounding",
                    icon = Icons.Default.Visibility,
                    onClick = onViewObservations,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Crack Gauge",
                    subtitle = "Growth kinetics",
                    icon = Icons.Default.ShowChart,
                    onClick = onOpenCrackGrowth,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by ID, title, or chainage...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEachIndexed { index, label ->
                    val isSelected = selectedFilter == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) colorScheme.primary else colorScheme.surface)
                            .border(1.dp, if (isSelected) colorScheme.primary else colorScheme.outline, RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filtered.size} FINDINGS LOGGED",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Raw observations →",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onViewObservations() }
                )
            }
        }

        items(filtered) { defect ->
            val isDark = LocalIsDark.current
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onSelectDefect(defect) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = defect.id,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    StatusPill(
                        label = "RISK ${defect.score}",
                        tone = defect.tone
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = defect.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "${defect.section} · ${defect.trackSide}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Coordinates: ${defect.gpsCoordinates} · ${defect.tieSleeperNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // AI Result Pill in defect list item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(toneColor(defect.tone, isDark).copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Engine: ${defect.aiEngineModel.take(19)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = toneColor(defect.tone, isDark)
                    )
                    Text(
                        text = "${defect.aiConfidencePercent}% Conf · Risk ${defect.aiDerailmentRiskIndex}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = toneColor(defect.tone, isDark)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = defect.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Length: ${defect.estimatedLength}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = toneColor(defect.tone, isDark)
                    )
                    Text(
                        text = "Timestamp: ${defect.detectedTimestamp.take(16)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DefectDetailsScreen(
    defect: Defect,
    onOpenMeasurement: () -> Unit,
    onOpenComparison: () -> Unit,
    onOpenObjectDetection: () -> Unit,
    onOpenAlignment: () -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenVerify: () -> Unit,
    onOpenComments: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = defect.id,
                subtitle = "${defect.title} · ${defect.section}",
                onBack = onBack
            )
        }

        // Top Status & Risk Card
        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(label = "ACTIVE DEFECT", tone = defect.tone)
                    Text(
                        text = "Detected ${defect.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = defect.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "${defect.section} · ${defect.trackSide}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Risk Score Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(toneColor(defect.tone, isDark).copy(alpha = 0.12f))
                        .border(1.dp, toneColor(defect.tone, isDark).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI RISK INDEX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = toneColor(defect.tone, isDark)
                        )
                        Text(
                            text = "${defect.score}/100 Criticality",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = toneColor(defect.tone, isDark)
                        )
                    }
                    Text(
                        text = "Estimated: ${defect.estimatedLength}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = toneColor(defect.tone, isDark)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = defect.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
            }
        }

        // Operational Restriction Warning
        item {
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                backgroundColor = toneColor(Tone.CRITICAL, isDark).copy(alpha = 0.08f),
                borderColor = toneColor(Tone.CRITICAL, isDark).copy(alpha = 0.3f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = toneColor(Tone.CRITICAL, isDark),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Speed Restriction Active: ${defect.aiRecommendedSpeedLimitKmH} km/h",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = toneColor(Tone.CRITICAL, isDark)
                        )
                        Text(
                            text = "Mandatory limit applied to all Up Line freight and passenger traffic.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Dedicated Card: Inspection Timestamps
        item {
            Spacer(modifier = Modifier.height(4.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSPECTION TIMESTAMPS & TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "First Detected Timestamp", value = defect.detectedTimestamp)
                DetailRow(label = "Latest Verification Audit", value = defect.lastAuditedTimestamp)
                DetailRow(label = "Next Inspection Due", value = "Within 24 hours (Daily Mandatory Sweeps)")
                DetailRow(label = "Auditor / Lead Inspector", value = "E. Chen · Certified Track Inspector (#TC-4091)")
                DetailRow(label = "Digital Ledger Hash", value = "SHA-256: 7d49...a32e (Tamper-proof Logged)")
            }
        }

        // Dedicated Card: Track Coordinates & Geolocation
        item {
            Spacer(modifier = Modifier.height(4.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRACK COORDINATES & GEOLOCATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "LRS Track Chainage", value = defect.chainageCoordinate)
                DetailRow(label = "Corridor & Line", value = "${defect.section} · ${defect.trackSide}")
                DetailRow(label = "GPS Geolocation", value = defect.gpsCoordinates)
                DetailRow(label = "Decimal Lat / Lon", value = "${defect.latitude}° N, ${defect.longitude}° W")
                DetailRow(label = "Track Elevation (AMSL)", value = "${defect.altitudeMeters} meters")
                DetailRow(label = "Tie / Sleeper ID", value = defect.tieSleeperNumber)
                DetailRow(label = "Dynamic Track Gauge", value = "${defect.trackGaugeMm} mm (+3.2 mm tolerance)")
                DetailRow(label = "GNSS RTK Fix", value = "±1.2m Accuracy · 14 Satellites Fixed")
            }
        }

        // Dedicated Card: AI Analysis Engine Results
        item {
            Spacer(modifier = Modifier.height(4.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI ANALYSIS ENGINE RESULTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        label = "CONF: ${defect.aiConfidencePercent}%",
                        tone = defect.tone
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "AI Inference Model", value = defect.aiEngineModel)
                DetailRow(label = "Detection Confidence", value = "${defect.aiConfidencePercent}% (Multi-spectral tensor pass)")
                DetailRow(label = "Derailment Risk Index", value = "${defect.aiDerailmentRiskIndex} / 1.00 (Nadal Limit 0.80)")
                DetailRow(label = "Crack Growth Velocity", value = "${defect.aiGrowthRateMmPerDay} mm/day (Warning: >0.15 mm/day)")
                DetailRow(label = "Predicted Failure Horizon", value = "${defect.aiPredictedFailureDays} days to transverse rupture")
                DetailRow(label = "Prescribed Speed Restriction", value = "Mandatory ${defect.aiRecommendedSpeedLimitKmH} km/h TSR active")
                DetailRow(label = "Corrective Work Directive", value = defect.aiPrescribedAction)
            }
        }

        // Analysis Modules
        item {
            SectionLabel(title = "DIAGNOSTIC & AUDIT WORKSPACE")

            ListRow(
                icon = Icons.Default.Straighten,
                title = "Crack Measurement & Gauge",
                subtitle = "Optical gauge: 46 mm · Reference calibrated",
                trailing = "46 mm",
                tone = Tone.CRITICAL,
                onClick = onOpenMeasurement
            )
            ListRow(
                icon = Icons.Default.Compare,
                title = "Image Comparison",
                subtitle = "12 Jun (31 mm) vs 18 Jun (46 mm)",
                trailing = "+48%",
                tone = Tone.WARNING,
                onClick = onOpenComparison
            )
            ListRow(
                icon = Icons.Default.ShowChart,
                title = "Growth Progression Chart",
                subtitle = "Propagation velocity: 15 mm over 37 days",
                trailing = "Exceeds",
                tone = Tone.CRITICAL,
                onClick = onOpenGrowth
            )
            ListRow(
                icon = Icons.Default.CameraAlt,
                title = "Object Detection Breakdown",
                subtitle = "Rail head anomaly, fasteners, concrete sleepers",
                trailing = "Verified",
                tone = Tone.INFO,
                onClick = onOpenObjectDetection
            )
            ListRow(
                icon = Icons.Default.Sensors,
                title = "Alignment & Dynamic Vibration",
                subtitle = "0.34g vertical acceleration peak at 14+320",
                trailing = "0.34g",
                tone = Tone.WARNING,
                onClick = onOpenAlignment
            )
            ListRow(
                icon = Icons.Default.VerifiedUser,
                title = "Qualified Engineer Sign-Off",
                subtitle = "Sign verification or authorize restriction release",
                trailing = "Action",
                tone = Tone.INFO,
                onClick = onOpenVerify
            )
            ListRow(
                icon = Icons.Default.Comment,
                title = "Field Comments & Logs",
                subtitle = "2 engineer notes recorded",
                trailing = "2",
                tone = Tone.NEUTRAL,
                onClick = onOpenComments
            )
        }
    }
}

@Composable
fun CrackMeasurementScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var measuredMm by remember { mutableFloatStateOf(46.0f) }
    var isSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Crack Measurement",
                subtitle = "CRK-2048 · Calibrated optical gauge verification",
                onBack = onBack
            )
        }

        item {
            // Simulated Rail Head Surface Graphic with Crack Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E2630))
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "OPTICAL SURFACE 10X ZOOM",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Rail contour
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.Center)
                        .background(Color(0xFF2C3844))
                )

                // Crack fracture line
                Box(
                    modifier = Modifier
                        .width((measuredMm * 4).dp)
                        .height(6.dp)
                        .align(Alignment.Center)
                        .background(Color(0xFFE95D5D))
                )

                // Dimension callout
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "GAUGE: ${String.format("%.1f", measuredMm)} mm (±0.4 mm)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            RailCard {
                Text(
                    text = "MANUAL CALIBRATION ADJUSTMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { measuredMm = (measuredMm - 1f).coerceAtLeast(10f); isSaved = false },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = "${String.format("%.1f", measuredMm)} mm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { measuredMm = (measuredMm + 1f).coerceAtMost(120f); isSaved = false },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = measuredMm,
                    onValueChange = { measuredMm = it; isSaved = false },
                    valueRange = 10f..100f
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = if (isSaved) "Measurement Saved ✓" else "Save Calibrated Measurement",
                icon = Icons.Default.Check,
                onClick = { isSaved = true }
            )
        }
    }
}

@Composable
fun GrowthAnalysisScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val history = listOf(
        Triple("12 May", "18 mm", Tone.HEALTHY),
        Triple("28 May", "31 mm", Tone.WARNING),
        Triple("18 Jun", "46 mm", Tone.CRITICAL)
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
                title = "Crack Growth Analysis",
                subtitle = "CRK-2048 · Propagation telemetry & rate threshold",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "CRITICAL GROWTH ACCELERATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = toneColor(Tone.CRITICAL, LocalIsDark.current),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "+48% Growth in 21 Days",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Rate: 0.71 mm/day · Standard threshold is 0.15 mm/day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Visual progression steps
                history.forEach { (date, length, tone) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = length,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = toneColor(tone, LocalIsDark.current)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusPill(label = tone.name, tone = tone)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "PREDICTIVE ACTION TRIGGER",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Based on cyclic axle loading, crack will breach the 55 mm transverse failure risk limit within 11 days. Immediate rail replacement recommended.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ImageComparisonScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
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
                title = "Image Comparison",
                subtitle = "Side-by-side surface progression review",
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Current (18 Jun · 46mm)", "Previous (12 Jun · 31mm)").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Button(
                        onClick = { selectedTab = index },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) colorScheme.primary else colorScheme.surface,
                            contentColor = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141C24))
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = if (selectedTab == 0) "CAPTURE 2024-06-18 · 46 MM" else "CAPTURE 2024-06-12 · 31 MM",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Rail silhouette
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(90.dp)
                        .align(Alignment.Center)
                        .background(Color(0xFF23303E))
                )

                // Defect marker in image
                Box(
                    modifier = Modifier
                        .width(if (selectedTab == 0) 120.dp else 75.dp)
                        .height(8.dp)
                        .align(Alignment.Center)
                        .background(if (selectedTab == 0) Color(0xFFE95D5D) else Color(0xFFE3A336))
                )

                Text(
                    text = if (selectedTab == 0) "Severe longitudinal growth (+15mm)" else "Initial baseline inspection",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }
}

@Composable
fun ObjectDetectionScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    val detections = listOf(
        Triple("Rail Head Surface", "Gauge corner longitudinal crack (46mm)", Tone.CRITICAL),
        Triple("Fastener Clip #1", "E-clip tension nominal at 220 Nm", Tone.HEALTHY),
        Triple("Fastener Clip #2", "E-clip loose / slight displacement", Tone.WARNING),
        Triple("Concrete Sleeper #14", "Prestressed concrete - No cracks detected", Tone.HEALTHY),
        Triple("Ballast Profile", "Clean shoulder, proper drainage angle", Tone.HEALTHY)
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
                title = "Object Detection",
                subtitle = "Multimodal rail component segmentation",
                onBack = onBack
            )
        }

        items(detections) { (comp, status, tone) ->
            ListRow(
                icon = when (tone) {
                    Tone.CRITICAL -> Icons.Default.Warning
                    Tone.WARNING -> Icons.Default.PriorityHigh
                    else -> Icons.Default.CheckCircle
                },
                title = comp,
                subtitle = status,
                trailing = tone.name,
                tone = tone
            )
        }
    }
}

@Composable
fun AlignmentAnalysisScreen(onBack: () -> Unit) {
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
                title = "Alignment & Dynamics",
                subtitle = "Inertial telemetry at chainage 14+320",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "TRACK GEOMETRY TOLERANCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Nominal Track Gauge", value = "1,435.0 mm")
                DetailRow(label = "Measured Gauge", value = "1,438.2 mm (+3.2 mm)")
                DetailRow(label = "Horizontal Alignment", value = "±1.8 mm (Pass)")
                DetailRow(label = "Vertical Cross-Level", value = "2.1 mm (Pass)")
                DetailRow(label = "Peak Vertical Accel", value = "0.34g (Elevated)")
                DetailRow(label = "Lateral Jerk Index", value = "0.08 m/s³ (Nominal)")
            }
        }
    }
}

@Composable
fun EngineerVerificationScreen(
    onSigned: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isSigned by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Engineer Sign-Off",
                subtitle = "Qualified safety endorsement & audit logging",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "SAFETY ENDORSEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Inspector", value = "E. Chen (Safety Lead)")
                DetailRow(label = "Engineering License", value = "IRSE-UK #849201")
                DetailRow(label = "Target Defect", value = "CRK-2048 (Gauge crack)")
                DetailRow(label = "Action Decision", value = "Impose 25 km/h limit")
                DetailRow(label = "Sign-Off Status", value = if (isSigned) "Digitally Signed" else "Pending Signature")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = if (isSigned) "Sign-Off Confirmed ✓" else "Sign with Digital Certificate",
                icon = Icons.Default.VerifiedUser,
                onClick = {
                    isSigned = true
                    onSigned()
                }
            )
        }
    }
}

@Composable
fun CommentsScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val comments = remember {
        mutableStateListOf(
            Pair("E. Chen · 12 min ago", "Surface crack confirmed during patrol. 25 km/h restriction notice dispatched to control center."),
            Pair("M. Alvarez · 35 min ago", "Maintenance crew MT-881 scheduled for clip replacement and rail head grinding.")
        )
    }
    var newComment by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Engineering Notes",
                subtitle = "Field notes & dispatcher instructions",
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    placeholder = { Text("Add field engineering note...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newComment.isNotBlank()) {
                            comments.add(0, Pair("E. Chen · Just now", newComment.trim()))
                            newComment = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = colorScheme.onPrimary
                    )
                }
            }
        }

        items(comments) { (author, text) ->
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AllObservationsScreen(
    observations: List<Observation>,
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
                title = "All Observations",
                subtitle = "Complete sensor and visual finding history",
                onBack = onBack
            )
        }

        items(observations) { obs ->
            ListRow(
                icon = Icons.Default.Visibility,
                title = obs.title,
                subtitle = obs.meta,
                trailing = obs.length.ifEmpty { obs.date },
                tone = obs.tone
            )
        }
    }
}
