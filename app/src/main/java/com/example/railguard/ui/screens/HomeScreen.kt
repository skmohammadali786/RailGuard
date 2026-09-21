package com.example.railguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.model.Defect
import com.example.railguard.model.MaintenanceTask
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun HomeScreen(
    defects: List<Defect>,
    tasks: List<MaintenanceTask>,
    onNavigate: (String) -> Unit,
    onDefectClick: (Defect) -> Unit,
    onTaskClick: (MaintenanceTask) -> Unit
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
                title = "North corridor patrol",
                subtitle = "Shift 1 · E. Chen · On duty",
                isHome = true,
                onNotificationClick = { onNavigate("notifications") }
            )
        }

        // Shift Status Card
        item {
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onNavigate("live_inspection") }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(label = "PATROL IN PROGRESS", tone = Tone.INFO)
                    Text(
                        text = "14:00 - 18:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Section 14 · North Loop Line",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Chainage 14+000 to 15+250 · 60E1 Rail Profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Corridor scan progress",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "61%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { 0.61f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = colorScheme.primary,
                        trackColor = colorScheme.outline.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active speed restriction banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(toneColor(Tone.CRITICAL, isDark).copy(alpha = 0.1f))
                        .border(1.dp, toneColor(Tone.CRITICAL, isDark).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = toneColor(Tone.CRITICAL, isDark),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Restriction: 25 km/h at 14+320 (CRK-2048)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = toneColor(Tone.CRITICAL, isDark)
                    )
                }
            }
        }

        // Network Pulse
        item {
            SectionLabel(
                title = "NETWORK PULSE",
                action = "Live telemetry",
                onAction = { onNavigate("analytics") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "18",
                    label = "Open defects",
                    tone = Tone.CRITICAL,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("defects") }
                )
                MetricTile(
                    value = "06",
                    label = "Due today",
                    tone = Tone.WARNING,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("maintenance") }
                )
                MetricTile(
                    value = "94.2%",
                    label = "Track health",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("track_health") }
                )
            }
        }

        // Active Railway Line Map Preview
        item {
            SectionLabel(
                title = "CORRIDOR SCHEMATIC",
                action = "Open full map",
                onAction = { onNavigate("map") }
            )
            RailwayLineGraphic(
                showMarkers = true,
                onMarkerClick = { onNavigate("defect_map") }
            )
        }

        // Requires Attention
        item {
            SectionLabel(
                title = "REQUIRES ATTENTION",
                action = "View all",
                onAction = { onNavigate("attention") }
            )

            // Top Defect
            if (defects.isNotEmpty()) {
                val defect = defects.first()
                ListRow(
                    icon = Icons.Default.Warning,
                    title = defect.title,
                    subtitle = "${defect.id} · ${defect.section} · ${defect.time}",
                    trailing = "Risk ${defect.score}",
                    tone = defect.tone,
                    onClick = { onDefectClick(defect) }
                )
            }

            // Top Task
            if (tasks.isNotEmpty()) {
                val task = tasks.first()
                ListRow(
                    icon = Icons.Default.Build,
                    title = task.title,
                    subtitle = "${task.id} · ${task.section} · ${task.due}",
                    trailing = task.assignee,
                    tone = task.tone,
                    onClick = { onTaskClick(task) }
                )
            }
        }

        // Quick Workspace Grid
        item {
            SectionLabel(title = "WORKSPACE MODULES")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkspaceTile(
                    title = "Live Scan",
                    subtitle = "Camera HUD",
                    icon = Icons.Default.CameraAlt,
                    onClick = { onNavigate("live_inspection") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "New Run",
                    subtitle = "Plan inspection",
                    icon = Icons.Default.PlayArrow,
                    onClick = { onNavigate("inspection_setup") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Crack Analysis",
                    subtitle = "Growth gauge",
                    icon = Icons.Default.ShowChart,
                    onClick = { onNavigate("growth_analysis") },
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
                    subtitle = "SHA-256 PDF",
                    icon = Icons.Default.Description,
                    onClick = { onNavigate("evidence_package") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Heatmap",
                    subtitle = "Risk zones",
                    icon = Icons.Default.Layers,
                    onClick = { onNavigate("risk_heatmap") },
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTile(
                    title = "Sign-Off",
                    subtitle = "Audit verify",
                    icon = Icons.Default.VerifiedUser,
                    onClick = { onNavigate("engineer_verification") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun WorkspaceTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
