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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.*
import com.example.railguard.model.MaintenanceTask
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

@Composable
fun MaintenanceScreen(
    tasks: List<MaintenanceTask>,
    onSelectTask: (MaintenanceTask) -> Unit,
    onCreateTask: () -> Unit,
    onViewAnalytics: () -> Unit,
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
                title = "Maintenance Tasks",
                subtitle = "Work orders, clip replacements, and restriction releases",
                onBack = onBack
            )
        }

        // Summary Metrics
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "06",
                    label = "Due today",
                    tone = Tone.CRITICAL,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "14",
                    label = "Open work",
                    tone = Tone.WARNING,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "91%",
                    label = "On schedule",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f),
                    onClick = onViewAnalytics
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryButton(
                title = "Create Maintenance Task",
                icon = Icons.Default.Add,
                onClick = onCreateTask
            )
        }

        item {
            SectionLabel(
                title = "ACTIVE WORK ORDERS",
                action = "Work Analytics →",
                onAction = onViewAnalytics
            )
        }

        items(tasks) { task ->
            RailCard(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { onSelectTask(task) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.id,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    StatusPill(label = task.due.uppercase(), tone = task.tone)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = task.section,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Assigned: ${task.assignee}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Status: ${task.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CreateMaintenanceTaskScreen(
    onTaskCreated: (MaintenanceTask) -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var title by remember { mutableStateOf("Replace rail clip pair") }
    var section by remember { mutableStateOf("North Loop · 14+320") }
    var due by remember { mutableStateOf("Due today") }
    var assignee by remember { mutableStateOf("M. Alvarez") }
    var torque by remember { mutableStateOf("220 Nm") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Create Work Order",
                subtitle = "Issue corrective maintenance ticket",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Corridor & Chainage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = due,
                    onValueChange = { due = it },
                    label = { Text("Due Window") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it },
                    label = { Text("Lead Technician") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = torque,
                    onValueChange = { torque = it },
                    label = { Text("Required Fastener Torque") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = "Dispatch Maintenance Task",
                icon = Icons.Default.Send,
                onClick = {
                    val task = MaintenanceTask(
                        id = "MT-${System.currentTimeMillis() % 1000}",
                        title = title,
                        section = section,
                        due = due,
                        tone = Tone.CRITICAL,
                        assignee = assignee,
                        torque = torque
                    )
                    onTaskCreated(task)
                }
            )
        }
    }
}

@Composable
fun TaskDetailsScreen(
    task: MaintenanceTask,
    onOpenBeforeAfter: () -> Unit,
    onOpenVerify: () -> Unit,
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
                title = task.id,
                subtitle = task.title,
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
                    Text(
                        text = "WORK TICKET",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(label = task.status.uppercase(), tone = task.tone)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = task.section,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(label = "Assignee", value = task.assignee)
                DetailRow(label = "Due Date", value = task.due)
                DetailRow(label = "Torque Requirement", value = task.torque)
                DetailRow(label = "Restriction", value = "25 km/h restriction bound")
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionLabel(title = "MAINTENANCE ACTIONS")
            ListRow(
                icon = Icons.Default.Compare,
                title = "Before / After Evidence",
                subtitle = "Verify sleeper clip seating & torque 220 Nm",
                trailing = "Review",
                tone = Tone.INFO,
                onClick = onOpenBeforeAfter
            )
            ListRow(
                icon = Icons.Default.VerifiedUser,
                title = "Sign-Off & Restriction Release",
                subtitle = "Release 25 km/h limit after field completion",
                trailing = "Sign",
                tone = Tone.HEALTHY,
                onClick = onOpenVerify
            )
        }
    }
}

@Composable
fun BeforeAfterScreen(onBack: () -> Unit) {
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
                title = "Before / After Evidence",
                subtitle = "MT-881 · Fastener replacement verification",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "BEFORE INTERVENTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = toneColor(Tone.CRITICAL, LocalIsDark.current),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Loose rail clip pair at sleeper 14+320 with 4.2 mm lateral play. Fastener fractured under cyclic freight load.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "AFTER INTERVENTION (VERIFIED)",
                    style = MaterialTheme.typography.labelSmall,
                    color = toneColor(Tone.HEALTHY, LocalIsDark.current),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "New Pandrol e-clip pair seated and torqued to 220 Nm. Lateral play measured at 0.0 mm. Ultrasonic test clean.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatusPill(label = "TORQUE VERIFIED: 220 NM", tone = Tone.HEALTHY)
            }
        }
    }
}

@Composable
fun MaintenanceVerificationScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var restrictionReleased by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Header(
                title = "Restriction Release",
                subtitle = "Authorize lifting 25 km/h limit on Up Line",
                onBack = onBack
            )
        }

        item {
            RailCard(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "RELEASE AUTHORIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Corridor", value = "North Loop (Section 14)")
                DetailRow(label = "Chainage", value = "14+320 Up Line")
                DetailRow(label = "Restoring Line Speed", value = "120 km/h nominal")
                DetailRow(label = "Authorizing Lead", value = "E. Chen")
                DetailRow(label = "Work Order", value = "MT-881 (Replace clip)")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                title = if (restrictionReleased) "Restriction Lifted ✓" else "Authorize Line Speed Restoration",
                icon = Icons.Default.CheckCircle,
                onClick = {
                    restrictionReleased = true
                    onVerified()
                }
            )
        }
    }
}

@Composable
fun MaintenanceAnalyticsScreen(onBack: () -> Unit) {
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
                title = "Maintenance Analytics",
                subtitle = "Response time, SLA adherence, and work volume",
                onBack = onBack
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    value = "18.4h",
                    label = "Mean fix time",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "91%",
                    label = "Closed in SLA",
                    tone = Tone.HEALTHY,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    value = "38",
                    label = "Monthly tickets",
                    tone = Tone.INFO,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            RailCard {
                Text(
                    text = "SLA BREAKDOWN BY SEVERITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Critical (Immediate/4h)", value = "100% adherence (2 of 2)")
                DetailRow(label = "High (24h SLA)", value = "94% adherence (16 of 17)")
                DetailRow(label = "Routine (7-day SLA)", value = "89% adherence (17 of 19)")
            }
        }
    }
}
