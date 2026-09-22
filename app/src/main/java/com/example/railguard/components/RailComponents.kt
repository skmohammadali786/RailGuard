package com.example.railguard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.model.LocalAppSettings
import com.example.railguard.model.Tone
import com.example.railguard.theme.LocalIsDark
import com.example.railguard.theme.toneColor

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
    val isDark = LocalIsDark.current
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Logo Mark
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.primary)
                    .border(1.dp, colorScheme.primary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "RailGuard",
                    tint = colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isHome) "RAILGUARD / CONTROL" else "RAILGUARD / SYSTEM",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actions != null) {
            actions()
        } else if (onNotificationClick != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(10.dp))
                    .clickable { onNotificationClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    tone: Tone = Tone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val color = toneColor(tone, isDark)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
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
    val colorScheme = MaterialTheme.colorScheme
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
            color = colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
fun RailCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
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
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1
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
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
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
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
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
    val colorScheme = MaterialTheme.colorScheme

    Button(
        onClick = onClick,
        enabled = !disabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) colorScheme.secondary else colorScheme.primary,
            contentColor = if (secondary) colorScheme.onSecondary else colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
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
                modifier = Modifier.size(16.dp)
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
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
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
    onMarkerClick: ((Int) -> Unit)? = null,
    onSelectTrain: ((String) -> Unit)? = null,
    onSelectStation: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalIsDark.current

    // Live continuous train movement animation
    val infiniteTransition = rememberInfiniteTransition(label = "TrainAnimation")
    val train1Progress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Train1Progress"
    )

    val train2Progress by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Train2Progress"
    )

    val signalPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SignalPulse"
    )

    val settings = LocalAppSettings.current
    var isCompactView by remember(compact) { mutableStateOf(compact) }
    var selectedStationName by remember { mutableStateOf<String?>(null) }
    var selectedTrainId by remember { mutableStateOf<String?>(null) }

    val stations = listOf(
        Pair("West Cut", "01+200"),
        Pair("North Loop", "14+000"),
        Pair("East Jct", "03+500"),
        Pair("South Yard", "08+800")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF0F1722) else Color(0xFF1E293B))
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
                        .background(Color(0xFF22C55E).copy(alpha = signalPulse))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCompactView) "CORRIDOR MAP (MINI)" else "REAL-TIME CORRIDOR LINE MAP",
                    color = Color.White,
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
                        .background(if (isCompactView) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF334155))
                        .border(1.dp, if (isCompactView) Color(0xFF38BDF8) else Color(0xFF64748B), RoundedCornerShape(4.dp))
                        .clickable { isCompactView = !isCompactView }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCompactView) "EXPAND" else "MINI",
                        color = if (isCompactView) Color(0xFF38BDF8) else Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${settings.formatSpeed(25)} ZONE",
                        color = Color(0xFFFCA5A5),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("LIVE", color = Color(0xFF22C55E), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(if (isCompactView) 4.dp else 10.dp))

        // Schematic Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompactView) 96.dp else 190.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0B131E))
                .padding(horizontal = 8.dp, vertical = if (isCompactView) 4.dp else 8.dp)
        ) {
            // Speed restriction zone highlight band
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .align(Alignment.CenterStart)
                    .offset(x = 80.dp)
                    .background(Color(0xFFEF4444).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            )

            // Up Line (Northbound)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .offset(y = if (isCompactView) (-12).dp else (-18).dp)
                    .background(Color(0xFF38BDF8).copy(alpha = 0.8f))
            )
            // Down Line (Southbound)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .offset(y = if (isCompactView) 12.dp else 18.dp)
                    .background(Color(0xFF94A3B8).copy(alpha = 0.6f))
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
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                }
            }

            // Stations along Top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stations.forEach { (name, chainage) ->
                    val isSelected = selectedStationName == name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedStationName = if (isSelected) null else name
                            onSelectStation?.invoke(name)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompactView) 8.dp else 11.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFF59E0B) else Color.White)
                                .border(if (isCompactView) 1.5.dp else 2.dp, if (isSelected) Color.White else Color(0xFF0284C7), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = name,
                            fontSize = if (isCompactView) 8.sp else 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFFFCD34D) else Color.White.copy(alpha = 0.9f)
                        )
                        if (!isCompactView) {
                            Text(
                                text = "km $chainage",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Signal lights along the track
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = if (isCompactView) (-24).dp else (-32).dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(Color(0xFF22C55E), Color(0xFFF59E0B), Color(0xFF22C55E)).forEach { sigColor ->
                    Box(
                        modifier = Modifier
                            .size(if (isCompactView) 5.dp else 7.dp)
                            .clip(CircleShape)
                            .background(sigColor)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    )
                }
            }

            // Animated Live Train 1: TR-104 Express (Up Line)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = if (isCompactView) (-12).dp else (-18).dp)
            ) {
                val trainX = maxWidth * train1Progress - 20.dp
                Box(
                    modifier = Modifier
                        .offset(x = trainX)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF0284C7))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(5.dp))
                        .clickable {
                            selectedTrainId = "TR-104"
                            onSelectTrain?.invoke("TR-104")
                        }
                        .padding(horizontal = 4.dp, vertical = 1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Train,
                            contentDescription = "TR-104",
                            tint = Color.White,
                            modifier = Modifier.size(if (isCompactView) 9.dp else 11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "TR-104 ${settings.formatSpeed(118)}",
                            color = Color.White,
                            fontSize = if (isCompactView) 7.sp else 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Animated Live Train 2: FR-802 Freight (Down Line)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = if (isCompactView) 12.dp else 18.dp)
            ) {
                val train2X = maxWidth * train2Progress - 20.dp
                Box(
                    modifier = Modifier
                        .offset(x = train2X)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFD97706))
                        .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(5.dp))
                        .clickable {
                            selectedTrainId = "FR-802"
                            onSelectTrain?.invoke("FR-802")
                        }
                        .padding(horizontal = 4.dp, vertical = 1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRailway,
                            contentDescription = "FR-802",
                            tint = Color.White,
                            modifier = Modifier.size(if (isCompactView) 9.dp else 11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "FR-802 ${settings.formatSpeed(45)}",
                            color = Color.White,
                            fontSize = if (isCompactView) 7.sp else 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Defect Pins along bottom
            if (showMarkers) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple(Tone.CRITICAL, "CRK-2048 (46mm)", "km 14+320"),
                        Triple(Tone.WARNING, "CRK-2044 (28mm)", "km 14+108"),
                        Triple(Tone.INFO, "Switch 08A", "km 08+800")
                    ).forEachIndexed { index, (tone, name, loc) ->
                        val color = toneColor(tone, true)
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
                                contentDescription = name,
                                tint = color,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$name · $loc",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Telemetry Readout Bar
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ 25kV OLE Power: NORMAL · 🌡️ Rail Temp: 28.4°C · 🚆 2 Trains Active",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Track Up: CLEAR",
                color = Color(0xFF22C55E),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
