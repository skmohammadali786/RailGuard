package com.example.railguard.theme

import androidx.compose.ui.graphics.Color
import com.example.railguard.model.Tone

// Light theme colors
val LightBackground = Color(0xFFF5F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceRaised = Color(0xFFFFFFFF)
val LightSurfaceInset = Color(0xFFEDF2F7)
val LightForeground = Color(0xFF15202B)
val LightPrimary = Color(0xFF1459A6)
val LightPrimaryForeground = Color(0xFFFFFFFF)
val LightSecondary = Color(0xFFE9EFF5)
val LightSecondaryForeground = Color(0xFF1D3348)
val LightMuted = Color(0xFFEEF2F6)
val LightMutedForeground = Color(0xFF687786)
val LightBorder = Color(0xFFD7E0E8)

val LightHealthy = Color(0xFF19734A)
val LightWarning = Color(0xFF9A6700)
val LightCritical = Color(0xFFB42318)
val LightInfo = Color(0xFF1459A6)

// Dark theme colors
val DarkBackground = Color(0xFF0B0E11)
val DarkSurface = Color(0xFF151A1F)
val DarkSurfaceRaised = Color(0xFF1D242B)
val DarkSurfaceInset = Color(0xFF101418)
val DarkForeground = Color(0xFFF3F5F7)
val DarkPrimary = Color(0xFF6FA8FF)
val DarkPrimaryForeground = Color(0xFF08101E)
val DarkSecondary = Color(0xFF20272E)
val DarkSecondaryForeground = Color(0xFFD8E0E7)
val DarkMuted = Color(0xFF20272E)
val DarkMutedForeground = Color(0xFF8D99A5)
val DarkBorder = Color(0xFF29323A)

val DarkHealthy = Color(0xFF3FB984)
val DarkWarning = Color(0xFFE3A336)
val DarkCritical = Color(0xFFE95D5D)
val DarkInfo = Color(0xFF6FA8FF)

fun toneColor(tone: Tone, isDark: Boolean): Color {
    return when (tone) {
        Tone.CRITICAL -> if (isDark) DarkCritical else LightCritical
        Tone.WARNING -> if (isDark) DarkWarning else LightWarning
        Tone.HEALTHY -> if (isDark) DarkHealthy else LightHealthy
        Tone.INFO -> if (isDark) DarkInfo else LightInfo
        Tone.NEUTRAL -> if (isDark) DarkMutedForeground else LightMutedForeground
    }
}
