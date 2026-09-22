package com.example.railguard.theme

import androidx.compose.ui.graphics.Color
import com.example.railguard.model.Tone

// Light theme colors
val LightBackground = Color(0xFFF6F8FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceRaised = Color(0xFFFFFFFF)
val LightSurfaceInset = Color(0xFFEDF2F7)
val LightForeground = Color(0xFF0F172A)
val LightPrimary = Color(0xFF0284C7)
val LightPrimaryForeground = Color(0xFFFFFFFF)
val LightSecondary = Color(0xFFF1F5F9)
val LightSecondaryForeground = Color(0xFF1E293B)
val LightMuted = Color(0xFFF1F5F9)
val LightMutedForeground = Color(0xFF64748B)
val LightBorder = Color(0xFFE2E8F0)

val LightHealthy = Color(0xFF15803D)
val LightWarning = Color(0xFFB45309)
val LightCritical = Color(0xFFDC2626)
val LightInfo = Color(0xFF0284C7)

// Dark theme colors - High-precision mission control obsidian
val DarkBackground = Color(0xFF070A10)
val DarkSurface = Color(0xFF0E1420)
val DarkSurfaceRaised = Color(0xFF141D2E)
val DarkSurfaceInset = Color(0xFF090D15)
val DarkForeground = Color(0xFFF8FAFC)
val DarkPrimary = Color(0xFF38BDF8)
val DarkPrimaryForeground = Color(0xFF03101C)
val DarkSecondary = Color(0xFF131D2D)
val DarkSecondaryForeground = Color(0xFFE2E8F0)
val DarkMuted = Color(0xFF172233)
val DarkMutedForeground = Color(0xFF94A3B8)
val DarkBorder = Color(0xFF1E2D44)

val DarkHealthy = Color(0xFF22C55E)
val DarkWarning = Color(0xFFF59E0B)
val DarkCritical = Color(0xFFEF4444)
val DarkInfo = Color(0xFF38BDF8)

// Specialized Technical Accents
val CyanGlow = Color(0xFF00F0FF)
val VioletTensor = Color(0xFFA855F7)
val AmberAdvisory = Color(0xFFFBBF24)

fun toneColor(tone: Tone, isDark: Boolean): Color {
    return when (tone) {
        Tone.CRITICAL -> if (isDark) DarkCritical else LightCritical
        Tone.WARNING -> if (isDark) DarkWarning else LightWarning
        Tone.HEALTHY -> if (isDark) DarkHealthy else LightHealthy
        Tone.INFO -> if (isDark) DarkInfo else LightInfo
        Tone.NEUTRAL -> if (isDark) DarkMutedForeground else LightMutedForeground
    }
}
