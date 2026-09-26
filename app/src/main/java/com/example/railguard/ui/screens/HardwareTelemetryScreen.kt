package com.example.railguard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.MetricRow
import com.example.railguard.components.PrimaryButton
import com.example.railguard.components.RailCard
import com.example.railguard.components.SubScreenHeader
import com.example.railguard.data.RailGuardFirebaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HardwareTelemetryScreen(onBack: () -> Unit) {
    val service = remember { RailGuardFirebaseService.instance }
    val scope = rememberCoroutineScope()
    var telemetry by remember { mutableStateOf<RailGuardFirebaseService.LiveSensorTelemetry?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Waiting for Raspberry Pi / ESP32 telemetry") }

    suspend fun refreshTelemetry() {
        isRefreshing = true
        val latest = service.fetchLatestSensorTelemetry()
        if (latest != null) {
            telemetry = latest
            statusMessage = "Live packet received from ${latest.nodeId}"
        } else {
            statusMessage = "No live hardware packet available in Firebase"
        }
        isRefreshing = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshTelemetry()
            delay(3000)
        }
    }

    val current = telemetry
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SubScreenHeader(
            title = "Hardware & Train Corridor Link",
            subtitle = "Raspberry Pi / ESP32 telemetry through Firebase",
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(14.dp))

        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(statusMessage, fontSize = 12.sp, color = Color(0xFF0284C7))
                Spacer(modifier = Modifier.height(10.dp))
                if (current == null) {
                    Text(
                        "Connect the gateway and publish a JSON telemetry packet to the authenticated user's live_sensors/telemetry path.",
                        fontSize = 12.sp
                    )
                } else {
                    MetricRow("Gateway", current.nodeId)
                    MetricRow("Hardware", current.hardware)
                    MetricRow("Ultrasonic depth", "${current.ultrasonicDepthMm} mm")
                    MetricRow("Vibration", "${current.vibrationG} g")
                    MetricRow("Rail temperature", "${current.railTempC} °C")
                    MetricRow("Axle speed", "${current.axleSpeedKmh} km/h")
                    MetricRow("Chainage", current.chainage)
                    MetricRow("State", current.status)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { scope.launch { refreshTelemetry() } },
                        enabled = !isRefreshing,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Refresh")
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val ok = service.sendEspCalibrationCommand(
                                    nodeId = current?.nodeId ?: "RPI-TRACK-01",
                                    command = "ZERO_CALIBRATE"
                                )
                                statusMessage = if (ok) {
                                    "Calibration command sent to Firebase"
                                } else {
                                    "Calibration command failed"
                                }
                            }
                        },
                        enabled = current != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Calibrate")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        RailCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("TRAIN SAFETY COMMAND", fontSize = 11.sp, color = Color(0xFF0284C7))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "TSR commands are written to the authenticated user's train command path. A gateway or train controller must consume and acknowledge them.",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25, 60, 120).forEach { speed ->
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val ok = service.dispatchTsrToFirebase(
                                        trainId = current?.nodeId ?: "RPI-TRACK-01",
                                        tsrSpeedKmH = speed,
                                        reason = "Operator command from RailGuard hardware corridor"
                                    )
                                    statusMessage = if (ok) {
                                        "TSR $speed km/h sent to Firebase"
                                    } else {
                                        "TSR command failed"
                                    }
                                }
                            }
                        ) {
                            Text("$speed km/h", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        PrimaryButton(
            title = "Return to Settings",
            onClick = onBack,
            secondary = true
        )
    }
}