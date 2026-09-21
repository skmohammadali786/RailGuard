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
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun MapScreen(
    onNavigateToDefectMap: () -> Unit,
    onNavigateToHeatmap: () -> Unit,
    onNavigateToLocationDetails: () -> Unit,
    onSelectDefect: (Defect) -> Unit,
    defects: List<Defect>,
    onBack: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    val sections = listOf(
        Triple("West Cut · Section 01", "Chainage 01+000 - 02+800 · Health 98.1%", Tone.HEALTHY),
        Triple("North Loop · Section 14", "Chainage 14+000 - 15+250 · CRK-2048 (46mm)", Tone.CRITICAL),
        Triple("East Junction · Section 03", "Chainage 03+000 - 04+200 · MT-878 Grind", Tone.WARNING),
        Triple("South Yard · Section 08", "Chainage 08+000 - 09+600 · Switch 08A", Tone.WARNING)
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
                title = "Corridor Network Map",
                subtitle = "Active track lines, train telemetry, and geolocated defects",
                onBack = onBack
            )
        }

        item {
            RailwayLineGraphic(
                showMarkers = true,
                onMarkerClick = { onNavigateToDefectMap() }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    title = "Defect Pin Map",
                    icon = Icons.Default.Place,
                    onClick = onNavigateToDefectMap,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    title = "Risk Heatmap",
                    icon = Icons.Default.Layers,
                    secondary = true,
                    onClick = onNavigateToHeatmap,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionLabel(title = "CORRIDOR SECTIONS")
        }

        items(sections) { (title, subtitle, tone) ->
            ListRow(
                icon = Icons.Default.AltRoute,
                title = title,
                subtitle = subtitle,
                trailing = tone.name,
                tone = tone,
                onClick = onNavigateToLocationDetails
            )
        }
    }
}

@Composable
fun DefectMapScreen(
    defects: List<Defect>,
    onSelectDefect: (Defect) -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedDefect = defects.getOrNull(selectedIndex) ?: defects.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Defect Pin Map",
                subtitle = "Geolocated findings across North Loop Line",
                onBack = onBack
            )
        }

        // Map Canvas Area
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16202A))
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                // Grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                }

                // Track Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.Center)
                        .background(colorScheme.primary)
                )

                // Defect Pins on Track Line
                val positions = listOf(0.25f, 0.55f, 0.85f)
                defects.take(3).forEachIndexed { index, defect ->
                    val isSelected = selectedIndex == index
                    val toneColor = toneColor(defect.tone, true)

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (positions[index] * 260).dp, y = if (index % 2 == 0) (-25).dp else 25.dp)
                            .clip(CircleShape)
                            .background(toneColor)
                            .border(
                                if (isSelected) 3.dp else 1.dp,
                                if (isSelected) Color.White else Color.Transparent,
                                CircleShape
                            )
                            .clickable { selectedIndex = index }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = defect.id,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "GPS: 51°30'14.2\" N · 0°07'42.8\" W",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        // Selected Defect Card
        if (selectedDefect != null) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                RailCard(
                    onClick = { onSelectDefect(selectedDefect) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDefect.id,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        StatusPill(label = "RISK ${selectedDefect.score}", tone = selectedDefect.tone)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedDefect.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedDefect.section} · Estimated ${selectedDefect.estimatedLength}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrimaryButton(
                        title = "Open Defect Dossier",
                        icon = Icons.Default.ArrowForward,
                        onClick = { onSelectDefect(selectedDefect) }
                    )
                }
            }
        }
    }
}

@Composable
fun RiskHeatmapScreen(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    val heatmapData = listOf(
        Pair("Section 14 (North Loop)", 92 to Tone.CRITICAL),
        Pair("Section 08 (South Yard)", 68 to Tone.WARNING),
        Pair("Section 03 (East Junction)", 44 to Tone.WARNING),
        Pair("Section 01 (West Cut)", 12 to Tone.HEALTHY)
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
                title = "Corridor Risk Heatmap",
                subtitle = "Aggregated criticality density across lines",
                onBack = onBack
            )
        }

        items(heatmapData) { (section, pair) ->
            val (risk, tone) = pair
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = section,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(label = "INDEX $risk", tone = tone)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { risk / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = toneColor(tone, LocalIsDark.current),
                    trackColor = colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun LocationDetailsScreen(onBack: () -> Unit) {
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
                title = "Section 14 Specifications",
                subtitle = "North Loop Line engineering parameters",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "INFRASTRUCTURE ASSET DATA",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Corridor", value = "North Loop Line (Up)")
                DetailRow(label = "Chainage", value = "14+000 to 15+250")
                DetailRow(label = "Rail Profile", value = "60E1 (UIC 60) Continuous Welded")
                DetailRow(label = "Sleeper Type", value = "G44 Prestressed Concrete")
                DetailRow(label = "Fastening", value = "Pandrol e-Clip / Fastclip")
                DetailRow(label = "Ballast Depth", value = "300 mm granite ballast")
                DetailRow(label = "Last Tamped", value = "14 days ago (Track machine T-04)")
                DetailRow(label = "Design Speed", value = "120 km/h (Restricted to 25 km/h)")
            }
        }
    }
}
