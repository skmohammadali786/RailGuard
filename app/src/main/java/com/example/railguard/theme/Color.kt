package com.example.railguard.theme

import androidx.compose.ui.graphics.Color
import com.example.railguard.model.Tone

// =========================================================================
// Meta Astryx Design System - Color Tokens
// =========================================================================

// Astryx Canvas & Surface Tokens (Light Mode Primary)
val LightBackground = Color(0xFFF8FAFC)        // Astryx Canvas
val LightSurface = Color(0xFFFFFFFF)           // Astryx Card / Surface
val LightSurfaceRaised = Color(0xFFFFFFFF)     // Astryx Elevated Container
val LightSurfaceInset = Color(0xFFF1F5F9)      // Astryx Subtle Inset / Input
val LightForeground = Color(0xFF0F172A)        // Astryx Primary Text (Slate 900)
val LightForegroundSecondary = Color(0xFF475569) // Astryx Secondary Text (Slate 600)
val LightForegroundMuted = Color(0xFF94A3B8)   // Astryx Muted Text (Slate 400)

// Astryx Primary Brand Accents
val LightPrimary = Color(0xFF0064E0)           // Meta Blue Core
val LightPrimaryHover = Color(0xFF0052B8)      // Meta Blue Darker
val LightPrimaryForeground = Color(0xFFFFFFFF) // White on Primary
val LightSecondary = Color(0xFFF1F5F9)         // Astryx Ghost / Subtle Surface
val LightSecondaryForeground = Color(0xFF1E293B)
val LightMuted = Color(0xFFF1F5F9)
val LightMutedForeground = Color(0xFF64748B)
val LightBorder = Color(0xFFE2E8F0)            // Astryx Border Default (Slate 200)
val LightBorderSubtle = Color(0xFFF1F5F9)      // Astryx Sub-divider

// Astryx Semantic Status Tones (High Contrast + WCAG AA compliant)
val LightHealthy = Color(0xFF16A34A)          // Astryx Positive (Emerald 600)
val LightHealthyBg = Color(0xFFF0FDF4)        // Emerald 50
val LightHealthyBorder = Color(0xFFBBF7D0)    // Emerald 200

val LightWarning = Color(0xFFD97706)          // Astryx Warning (Amber 600)
val LightWarningBg = Color(0xFFFFFBEB)        // Amber 50
val LightWarningBorder = Color(0xFFFDE68A)    // Amber 200

val LightCritical = Color(0xFFDC2626)         // Astryx Danger / Negative (Red 600)
val LightCriticalBg = Color(0xFFFEF2F2)       // Red 50
val LightCriticalBorder = Color(0xFFFECACA)   // Red 200

val LightInfo = Color(0xFF0284C7)             // Astryx Info / Telemetry Blue (Sky 600)
val LightInfoBg = Color(0xFFF0F9FF)           // Sky 50
val LightInfoBorder = Color(0xFFBAE6FD)       // Sky 200

// Dark theme fallback tokens (Obsidian Mission Control)
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

// Astryx Tone Helpers
fun toneColor(tone: Tone, isDark: Boolean = false): Color {
    return when (tone) {
        Tone.CRITICAL -> if (isDark) DarkCritical else LightCritical
        Tone.WARNING -> if (isDark) DarkWarning else LightWarning
        Tone.HEALTHY -> if (isDark) DarkHealthy else LightHealthy
        Tone.INFO -> if (isDark) DarkInfo else LightInfo
        Tone.NEUTRAL -> if (isDark) DarkMutedForeground else LightMutedForeground
    }
}

fun toneBgColor(tone: Tone, isDark: Boolean = false): Color {
    return when (tone) {
        Tone.CRITICAL -> LightCriticalBg
        Tone.WARNING -> LightWarningBg
        Tone.HEALTHY -> LightHealthyBg
        Tone.INFO -> LightInfoBg
        Tone.NEUTRAL -> LightSurfaceInset
    }
}

fun toneBorderColor(tone: Tone, isDark: Boolean = false): Color {
    return when (tone) {
        Tone.CRITICAL -> LightCriticalBorder
        Tone.WARNING -> LightWarningBorder
        Tone.HEALTHY -> LightHealthyBorder
        Tone.INFO -> LightInfoBorder
        Tone.NEUTRAL -> LightBorder
    }
}
