package com.example.railguard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.railguard.data.RailGuardFirebaseService
import com.example.railguard.model.Defect
import com.example.railguard.model.LocalAppSettings
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneBgColor
import com.example.railguard.theme.toneBorderColor
import com.example.railguard.theme.toneColor
import kotlinx.coroutines.launch

/**
 * Meta Astryx Live Beacon
 * Continuously pulsing concentric radar wave indicating real-time telemetry or active sensor streaming.
 */
@Composable
fun AstryxLiveBeacon(
    tone: Tone = Tone.HEALTHY,
    size: androidx.compose.ui.unit.Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val color = toneColor(tone, isDark = false)
    val infiniteTransition = rememberInfiniteTransition(label = "beaconPulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size * 2.4f)
    ) {
        // Outer pulsing radar wave
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = waveScale
                    scaleY = waveScale
                    alpha = waveAlpha
                }
                .clip(CircleShape)
                .background(color)
        )
        // Core beacon dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun Header(
    title: String,
    subtitle: String? = null,
    isHome: Boolean = false,
    onBack: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Astryx Logo Mark (Shield with subtle Meta Blue tint)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0064E0))
                    .border(1.dp, Color(0xFF0052B8), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "RailGuard",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RAILGUARD",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF0064E0),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        var showHeaderWifiDialog by remember { mutableStateOf(false) }
        val fbService = RailGuardFirebaseService.instance
        val isWifiOnline = fbService.isDirectWifiConnected

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Astryx Wi-Fi Quick Access Pill with Live Beacon (Only displayed on Homepage)
            if (isHome) {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isWifiOnline) Color(0xFFF0FDF4) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isWifiOnline) Color(0xFFBBF7D0) else Color(0xFFE2E8F0), RoundedCornerShape(9999.dp))
                        .clickable { showHeaderWifiDialog = true }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isWifiOnline) {
                            AstryxLiveBeacon(tone = Tone.HEALTHY, size = 6.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Pi Wi-Fi",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = if (isWifiOnline) "${fbService.directWifiLatencyMs}ms" else "Pi Wi-Fi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWifiOnline) Color(0xFF16A34A) else Color(0xFF475569)
                        )
                    }
                }
            }

            if (actions != null) {
                actions()
            } else if (onNotificationClick != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showHeaderWifiDialog) {
            RaspberryPiWifiDialog(onDismiss = { showHeaderWifiDialog = false })
        }
    }
}

@Composable
fun StatusPill(
    label: String = "",
    title: String = label,
    tone: Tone = Tone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val displayLabel = if (title.isNotEmpty()) title else label
    val isDark = LocalIsDark.current
    val color = toneColor(tone, isDark)
    val bgColor = toneBgColor(tone, isDark)
    val borderColor = toneBorderColor(tone, isDark)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(9999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (tone == Tone.CRITICAL || tone == Tone.WARNING) {
            AstryxLiveBeacon(tone = tone, size = 6.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = displayLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
fun SectionLabel(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF0064E0),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
fun RailCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color(0xFFE2E8F0),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(14.dp),
                clip = false,
                ambientColor = Color(0x0A0F172A),
                spotColor = Color(0x0A0F172A)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun MetricTile(
    value: String,
    label: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDark.current
    val color = toneColor(tone, isDark)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "metricScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Astryx Top Micro Accent Bar
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String? = null,
    tone: Tone = Tone.NEUTRAL,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val color = toneColor(tone, isDark)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "listRowScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(toneBgColor(tone, isDark))
                .border(1.dp, toneBorderColor(tone, isDark), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }

        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PrimaryButton(
    title: String,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    onClick: () -> Unit,
    secondary: Boolean = false,
    disabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !disabled) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        enabled = !disabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) Color(0xFFF1F5F9) else Color(0xFF0064E0),
            contentColor = if (secondary) Color(0xFF1E293B) else Color.White,
            disabledContainerColor = Color(0xFFE2E8F0),
            disabledContentColor = Color(0xFF94A3B8)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (secondary) 0.dp else 1.dp,
            pressedElevation = 0.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF0064E0),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null && onAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(title = action, onClick = onAction)
        }
    }
}

@Composable
fun RailwayLineGraphic(
    showMarkers: Boolean = false,
    selectedMarkerIndex: Int = 0,
    onMarkerClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    RealTimeTrainLineMap(
        compact = true,
        showMarkers = showMarkers,
        onMarkerClick = onMarkerClick,
        modifier = modifier
    )
}

@Composable
fun RealTimeTrainLineMap(
    compact: Boolean = false,
    showMarkers: Boolean = true,
    defects: List<Defect> = emptyList(),
    onMarkerClick: ((Int) -> Unit)? = null,
    onSelectTrain: ((String) -> Unit)? = null,
    onSelectStation: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current
    val settings = LocalAppSettings.current

    var isCompactView by remember(compact) { mutableStateOf(compact) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(if (isCompactView) 8.dp else 14.dp)
    ) {
        // Map HUD Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCompactView) "CORRIDOR SCHEMATIC (MINI)" else "CORRIDOR LINE SCHEMATIC",
                    color = Color(0xFF0F172A),
                    fontSize = if (isCompactView) 9.5.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Mode Toggle (Mini / Full)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isCompactView) Color(0xFF0284C7).copy(alpha = 0.15f) else Color(0xFFE2E8F0))
                        .border(1.dp, if (isCompactView) Color(0xFF0284C7) else Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                        .clickable { isCompactView = !isCompactView }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCompactView) "EXPAND" else "MINI",
                        color = if (isCompactView) Color(0xFF0284C7) else Color(0xFF334155),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (defects.any { it.tone == Tone.CRITICAL }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${settings.formatSpeed(25)} ZONE",
                            color = Color(0xFFDC2626),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isCompactView) 4.dp else 10.dp))

        // Schematic Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompactView) 96.dp else 190.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 8.dp, vertical = if (isCompactView) 4.dp else 8.dp)
        ) {
            // Speed restriction zone highlight band only when critical defects exist
            if (defects.any { it.tone == Tone.CRITICAL }) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.35f)
                        .align(Alignment.CenterStart)
                        .offset(x = 80.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                )
            }

            // Up Line (Northbound)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .offset(y = if (isCompactView) (-12).dp else (-18).dp)
                    .background(Color(0xFF0284C7).copy(alpha = 0.8f))
            )
            // Down Line (Southbound)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .offset(y = if (isCompactView) 12.dp else 18.dp)
                    .background(Color(0xFF64748B).copy(alpha = 0.6f))
            )

            // Cross ties / sleepers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(18) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(if (isCompactView) 34.dp else 46.dp)
                            .background(Color(0xFFCBD5E1))
                    )
                }
            }

            // Defect Pins along bottom
            if (showMarkers) {
                if (defects.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        defects.take(3).forEachIndexed { index, d ->
                            val color = toneColor(d.tone, true)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color.copy(alpha = 0.2f))
                                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .clickable { onMarkerClick?.invoke(index) }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = d.id,
                                    tint = color,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${d.id} · ${d.chainageCoordinate}",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "No defect records loaded",
                            color = Color(0xFFCBD5E1),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Live Raspberry Pi / Cloud Telemetry Status
        val liveTelem = RailGuardFirebaseService.instance.latestSensorTelemetry
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (liveTelem != null) "Pi Gateway: ${liveTelem.nodeId} · Chainage ${liveTelem.chainage}" else "Awaiting Raspberry Pi telemetry stream",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (liveTelem != null) "%.1f km/h".format(liveTelem.axleSpeedKmh) else "STANDBY",
                color = if (liveTelem != null) Color(0xFF0284C7) else Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
    }
}

@Composable
fun SecondaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "secBtnScale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF0F172A),
            disabledContainerColor = Color(0xFFF8FAFC),
            disabledContentColor = Color(0xFF94A3B8)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Header(
        title = title,
        subtitle = subtitle,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun StatusChip(
    label: String = "",
    title: String = label,
    tone: Tone = Tone.INFO,
    modifier: Modifier = Modifier
) {
    StatusPill(
        label = label,
        title = title,
        tone = tone,
        modifier = modifier
    )
}

@Composable
fun RaspberryPiWifiGatewayCard(
    modifier: Modifier = Modifier,
    onOpenConfig: () -> Unit = {}
) {
    val firebaseService = remember { RailGuardFirebaseService.instance }
    val scope = rememberCoroutineScope()
    val isConnected = firebaseService.isDirectWifiConnected
    val latency = firebaseService.directWifiLatencyMs
    val telem = firebaseService.latestSensorTelemetry
    val hw = firebaseService.hardwareHealth
    var isConnecting by remember { mutableStateOf(false) }
    var connectionMsg by remember { mutableStateOf<String?>(null) }
    var ipInput by remember { mutableStateOf(firebaseService.localWifiIp) }

    RailCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenConfig
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AstryxLiveBeacon(
                        tone = if (isConnected) Tone.HEALTHY else Tone.CRITICAL,
                        size = 7.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "PI WI-FI GATEWAY · ONLINE" else "PI WI-FI GATEWAY · STANDBY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isConnected) Color(0xFFF0FDF4) else Color(0xFFEFF6FF))
                        .border(1.dp, if (isConnected) Color(0xFFBBF7D0) else Color(0xFFBFDBFE), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isConnected) "${latency}ms · sub-50ms LAN" else "DIRECT WI-FI",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF16A34A) else Color(0xFF0064E0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isConnected) {
                // Connected View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Node IP", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text("http://${firebaseService.localWifiIp}:8080", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Hardware", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text(hw?.cameraSensor ?: "IMX708 Camera + UT", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Telemetry Quick Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("Speed", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text(if (telem != null) "${telem.axleSpeedKmh.toInt()} km/h" else "--", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("UT Depth", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text(if (telem != null) "%.1f mm".format(telem.ultrasonicDepthMm) else "--", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("Temp", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text(if (telem != null) "%.1f°C".format(telem.railTempC) else "--", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("Vib G", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text(if (telem != null) "%.3f".format(telem.vibrationG) else "--", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                firebaseService.syncDirectWifiAllData()
                            }
                        },
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync All Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { firebaseService.disconnectDirectWifi() },
                        modifier = Modifier.weight(1f).height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Disconnected View - Easy Connect
                Text(
                    text = "Connect directly to the Raspberry Pi Wi-Fi / Hotspot to stream optical crack detections, ultrasonic A-scans, and live signals.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Presets chip row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "192.168.4.1" to "Pi Hotspot",
                        "192.168.1.100" to "Home LAN",
                        "raspberrypi.local" to "mDNS",
                        "10.0.2.2" to "Emulator",
                        "127.0.0.1" to "Local"
                    ).forEach { (ip, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (ipInput == ip) Color(0xFF0284C7).copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                .border(1.dp, if (ipInput == ip) Color(0xFF0284C7) else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .clickable { ipInput = ip }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("$label ($ip)", fontSize = 10.sp, fontWeight = if (ipInput == ip) FontWeight.Bold else FontWeight.Normal, color = if (ipInput == ip) Color(0xFF0284C7) else Color(0xFF334155))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        modifier = Modifier.weight(1f).height(46.dp),
                        singleLine = true,
                        placeholder = { Text("e.g. 192.168.4.1", fontSize = 11.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            isConnecting = true
                            connectionMsg = null
                            scope.launch {
                                val (ok, msg) = firebaseService.connectDirectWifi(ipInput)
                                isConnecting = false
                                connectionMsg = msg
                            }
                        },
                        enabled = !isConnecting && ipInput.isNotBlank(),
                        modifier = Modifier.height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (connectionMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = connectionMsg ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@Composable
fun RaspberryPiWifiDialog(
    onDismiss: () -> Unit
) {
    val firebaseService = remember { RailGuardFirebaseService.instance }
    val scope = rememberCoroutineScope()
    var ipInput by remember { mutableStateOf(firebaseService.localWifiIp) }
    var isTesting by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    val isConnected = firebaseService.isDirectWifiConnected

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                                .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Raspberry Pi Wi-Fi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("Direct sensor link (<50ms)", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Enter the IP address of your Raspberry Pi Gateway:", fontSize = 12.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("Raspberry Pi IP Address") },
                    placeholder = { Text("192.168.4.1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Presets:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "192.168.4.1" to "Pi Hotspot",
                        "192.168.1.100" to "Local Wi-Fi",
                        "raspberrypi.local" to "mDNS",
                        "10.0.2.2" to "Emulator",
                        "127.0.0.1" to "Localhost"
                    ).forEach { (ip, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (ipInput == ip) Color(0xFF0284C7).copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                .border(1.dp, if (ipInput == ip) Color(0xFF0284C7) else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .clickable { ipInput = ip }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("$label ($ip)", fontSize = 10.sp, fontWeight = if (ipInput == ip) FontWeight.Bold else FontWeight.Normal, color = if (ipInput == ip) Color(0xFF0284C7) else Color(0xFF334155))
                        }
                    }
                }

                if (statusMsg != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMsg ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isConnected) {
                        OutlinedButton(
                            onClick = {
                                firebaseService.disconnectDirectWifi()
                                statusMsg = "Disconnected from Pi"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Disconnect", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            isTesting = true
                            statusMsg = null
                            scope.launch {
                                val (ok, msg) = firebaseService.connectDirectWifi(ipInput)
                                isTesting = false
                                statusMsg = msg
                            }
                        },
                        enabled = !isTesting && ipInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (isConnected) "Re-test Connection" else "Connect Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


