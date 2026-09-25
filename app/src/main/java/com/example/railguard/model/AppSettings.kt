package com.example.railguard.model

import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val displayName: String, val code: String) {
    EN_UK("English (UK)", "en_uk"),
    EN_US("English (US)", "en_us"),
    HI("Hindi (हिंदी)", "hi"),
    ES("Spanish (Español)", "es")
}

data class AppPreferences(
    val language: AppLanguage = AppLanguage.EN_UK,
    val isDarkMode: Boolean = false,
    val passcodeEnabled: Boolean = true,
    val passcodePin: String = "1234",
    val isBiometricEnabled: Boolean = true,
    val isAppLocked: Boolean = false,
    val isMetric: Boolean = true,
    val autoSync: Boolean = true,
    val autoSyncRtdb: Boolean = true,
    val esp32LinkActive: Boolean = true,
    val highPrecisionAi: Boolean = true,
    val tsrInterlockEnabled: Boolean = true
) {
    fun formatLength(lengthMm: Double): String {
        return if (isMetric) {
            "${lengthMm.toInt()} mm"
        } else {
            String.format("%.2f in", lengthMm * 0.0393701)
        }
    }

    fun formatSpeed(speedKmh: Int): String {
        return if (isMetric) {
            "$speedKmh km/h"
        } else {
            "${(speedKmh * 0.621371).toInt()} mph"
        }
    }

    fun formatSpeedStr(speedKmhStr: String): String {
        val num = speedKmhStr.filter { it.isDigit() }.toIntOrNull() ?: 25
        return formatSpeed(num)
    }

    fun translate(key: String): String {
        return AppTranslations.get(key, language)
    }
}

val LocalAppSettings = compositionLocalOf { AppPreferences() }

object AppTranslations {
    fun get(key: String, lang: AppLanguage): String {
        val map = when (lang) {
            AppLanguage.HI -> hindi
            AppLanguage.ES -> spanish
            AppLanguage.EN_US -> englishUs
            AppLanguage.EN_UK -> englishUk
        }
        return map[key] ?: englishUk[key] ?: key
    }

    private val englishUk = mapOf(
        "home" to "Home",
        "inspect" to "Inspect",
        "defects" to "Defects",
        "map" to "Map",
        "control" to "Control",
        "tab_home" to "Home",
        "tab_inspect" to "Inspect",
        "tab_defects" to "Defects",
        "tab_map" to "Map",
        "tab_control" to "Control",
        "welcome_back" to "Welcome Back",
        "welcome_back_sub" to "Sign in to continue your inspections and safety decisions.",
        "create_account" to "Create Account",
        "create_account_sub" to "Access live track inspections, defect register, and audit trails.",
        "recover_access" to "Forgot Password",
        "recover_access_sub" to "We’ll send a 6-digit verification code to your work email.",
        "sign_in" to "Sign In",
        "sign_out" to "Sign Out Session",
        "create_account_btn" to "Create Account",
        "already_registered" to "Already registered? Sign in",
        "new_to_railguard" to "New to RailGuard? Create an account",
        "forgot_password" to "Forgot password?",
        "send_code" to "Send verification code",
        "corridor_overview" to "RAILWAY OVERVIEW",
        "corridor_schematic" to "CORRIDOR SCHEMATIC",
        "ai_oracle_title" to "RAILGUARD AI ORACLE",
        "train_telemetry" to "TRAIN TELEMETRY & CAB UPLINK",
        "defect_pin_map" to "Defect Pin Map",
        "interactive_map" to "Interactive Corridor Map",
        "risk_heatmap" to "Risk Heatmap",
        "control_settings" to "Control Settings",
        "dark_mode" to "Dark Mode",
        "dark_mode_sub" to "High-contrast theme for nighttime inspection",
        "security_audit" to "Security & Passcode",
        "security_sub" to "Biometric sign-off, PIN lock & audit safeguards",
        "lock_app_now" to "Lock Terminal Now",
        "language_standards" to "Language & Regional Standards",
        "app_settings" to "App Configuration & Units",
        "metric_units" to "Metric Units (mm, km/h)",
        "metric_sub" to "Standard railway gauge specification",
        "auto_sync" to "Auto-Sync Local Store",
        "auto_sync_sub" to "Synchronize frames when network is available",
        "app_locked" to "Terminal Locked",
        "enter_pin" to "Enter 4-Digit Passcode PIN",
        "unlock" to "Unlock Terminal",
        "biometric_unlock" to "Fingerprint / Face Unlock",
        "incorrect_pin" to "Incorrect PIN passcode. Default is 1234.",
        "send_train" to "Dispatch Live Train",
        "train_en_route" to "TRAIN TR-104 EN ROUTE",
        "reset_train" to "Reset Train",
        "pause_train" to "Halt Train",
        "attention_required" to "Requires Immediate Attention",
        "field_engineering" to "Field Engineering Guides",
        "notifications" to "Notification Center",
        "about_railguard" to "About RailGuard Control",
        "inspector_profile" to "Inspector Profile",
        "core_reliability_metrics" to "CORE RELIABILITY METRICS",
        "track_health" to "Track Health",
        "active_cracks" to "Active Cracks",
        "coverage" to "Coverage",
        "load_tonnage" to "Load Tonnage",
        "passcode_pin_protection" to "Passcode PIN Protection",
        "biometric_authentication" to "Biometric Authentication",
        "save_passcode" to "Save Passcode PIN",
        "passcode_saved" to "Passcode PIN saved successfully",
        "north_corridor_patrol" to "North Corridor Patrol"
    )

    private val englishUs = englishUk.toMutableMap().apply {
        put("control", "Settings")
        put("tab_control", "Settings")
        put("control_settings", "Settings & Control")
        put("about_railguard", "About RailGuard")
    }

    private val hindi = mapOf(
        "home" to "होम",
        "inspect" to "निरीक्षण",
        "defects" to "दोष सूची",
        "map" to "नक्शा",
        "control" to "नियंत्रण",
        "tab_home" to "होम",
        "tab_inspect" to "निरीक्षण",
        "tab_defects" to "दोष सूची",
        "tab_map" to "नक्शा",
        "tab_control" to "नियंत्रण",
        "welcome_back" to "वापसी पर स्वागत है",
        "welcome_back_sub" to "अपने निरीक्षण और सुरक्षा निर्णयों को जारी रखने के लिए साइन इन करें।",
        "create_account" to "खाता बनाएं",
        "create_account_sub" to "लाइव ट्रैक निरीक्षण, दोष रजिस्टर और ऑडिट ट्रेल्स तक पहुंचें।",
        "recover_access" to "पासवर्ड भूल गए",
        "recover_access_sub" to "हम आपके कार्य ईमेल पर 6-अंकीय सत्यापन कोड भेजेंगे।",
        "sign_in" to "साइन इन करें",
        "sign_out" to "सत्र समाप्त करें",
        "create_account_btn" to "खाता बनाएं",
        "already_registered" to "पहले से पंजीकृत हैं? साइन इन करें",
        "new_to_railguard" to "रेलगार्ड पर नए हैं? खाता बनाएं",
        "forgot_password" to "पासवर्ड भूल गए?",
        "send_code" to "सत्यापन कोड भेजें",
        "corridor_overview" to "रेलवे अवलोकन",
        "corridor_schematic" to "गलियारा योजनाबद्ध नक्शा",
        "ai_oracle_title" to "रेलगार्ड एआई ओरेकल",
        "train_telemetry" to "ट्रेन टेलीमेट्री और कैब अपलिंक",
        "defect_pin_map" to "दोष पिन नक्शा",
        "interactive_map" to "इंटरएक्टिव कॉरिडोर नक्शा",
        "risk_heatmap" to "जोखिम हीटमैप",
        "control_settings" to "नियंत्रण सेटिंग्स",
        "dark_mode" to "डार्क मोड",
        "dark_mode_sub" to "रात के निरीक्षण के लिए उच्च-कंट्रास्ट थीम",
        "security_audit" to "सुरक्षा और पासकोड",
        "security_sub" to "बायोमेट्रिक साइन-ऑफ, पिन लॉक और सुरक्षा उपाय",
        "lock_app_now" to "टर्मिनल अभी लॉक करें",
        "language_standards" to "भाषा और क्षेत्रीय मानक",
        "app_settings" to "ऐप कॉन्फ़िगरेशन और इकाइयां",
        "metric_units" to "मीट्रिक इकाइयाँ (मिमी, किमी/घंटा)",
        "metric_sub" to "मानक रेलवे गेज विनिर्देश",
        "auto_sync" to "ऑटो-सिंक स्थानीय स्टोर",
        "auto_sync_sub" to "नेटवर्क उपलब्ध होने पर फ़्रेम सिंक्रनाइज़ करें",
        "app_locked" to "टर्मिनल लॉक है",
        "enter_pin" to "4-अंकीय पासकोड पिन दर्ज करें",
        "unlock" to "टर्मिनल अनलॉक करें",
        "biometric_unlock" to "फ़िंगरप्रिंट / बायोमेट्रिक अनलॉक",
        "incorrect_pin" to "गलत पिन पासकोड। डिफ़ॉल्ट 1234 है।",
        "send_train" to "लाइव ट्रेन भेजें",
        "train_en_route" to "ट्रेन TR-104 रास्ते में है",
        "reset_train" to "ट्रेन रीसेट करें",
        "pause_train" to "ट्रेन रोकें",
        "attention_required" to "तत्काल ध्यान देने की आवश्यकता",
        "field_engineering" to "फ़ील्ड इंजीनियरिंग गाइड",
        "notifications" to "अधिसूचना केंद्र",
        "about_railguard" to "रेलगार्ड के बारे में",
        "inspector_profile" to "निरीक्षक प्रोफ़ाइल",
        "core_reliability_metrics" to "मुख्य विश्वसनीयता मेट्रिक्स",
        "track_health" to "ट्रैक स्वास्थ्य",
        "active_cracks" to "सक्रिय दरारें",
        "coverage" to "कवरेज",
        "load_tonnage" to "लोड टन भार",
        "passcode_pin_protection" to "पासकोड पिन सुरक्षा",
        "biometric_authentication" to "बायोमेट्रिक प्रमाणीकरण",
        "save_passcode" to "पासकोड पिन सहेजें",
        "passcode_saved" to "पासकोड पिन सफलतापूर्वक सहेजा गया",
        "north_corridor_patrol" to "उत्तर गलियारा गश्ती"
    )

    private val spanish = mapOf(
        "home" to "Inicio",
        "inspect" to "Inspección",
        "defects" to "Defectos",
        "map" to "Mapa",
        "control" to "Control",
        "tab_home" to "Inicio",
        "tab_inspect" to "Inspección",
        "tab_defects" to "Defectos",
        "tab_map" to "Mapa",
        "tab_control" to "Control",
        "welcome_back" to "Bienvenido de nuevo",
        "welcome_back_sub" to "Inicie sesión para continuar sus inspecciones y decisiones de seguridad.",
        "create_account" to "Crear Cuenta",
        "create_account_sub" to "Acceso a inspecciones en vivo, registro de defectos y auditorías.",
        "recover_access" to "¿Olvidó su contraseña?",
        "recover_access_sub" to "Enviaremos un código de verificación de 6 dígitos a su correo de trabajo.",
        "sign_in" to "Iniciar Sesión",
        "sign_out" to "Cerrar Sesión",
        "create_account_btn" to "Crear Cuenta",
        "already_registered" to "¿Ya registrado? Iniciar sesión",
        "new_to_railguard" to "¿Nuevo en RailGuard? Crear cuenta",
        "forgot_password" to "¿Olvidó su contraseña?",
        "send_code" to "Enviar código de verificación",
        "corridor_overview" to "RESUMEN DEL CORREDOR",
        "corridor_schematic" to "ESQUEMA DEL CORREDOR",
        "ai_oracle_title" to "ORÁCULO IA RAILGUARD",
        "train_telemetry" to "TELEMETRÍA DE TREN Y CABINA",
        "defect_pin_map" to "Mapa de Pines de Defectos",
        "interactive_map" to "Mapa Interactivo del Corredor",
        "risk_heatmap" to "Mapa de Calor de Riesgo",
        "control_settings" to "Ajustes de Control",
        "dark_mode" to "Modo Oscuro",
        "dark_mode_sub" to "Tema de alto contraste para inspecciones nocturnas",
        "security_audit" to "Seguridad y Código",
        "security_sub" to "Firma biométrica, bloqueo PIN y salvaguardas",
        "lock_app_now" to "Bloquear Terminal Ahora",
        "language_standards" to "Idioma y Estándares Regionales",
        "app_settings" to "Configuración y Unidades",
        "metric_units" to "Unidades Métricas (mm, km/h)",
        "metric_sub" to "Especificación estándar de vía ferroviaria",
        "auto_sync" to "Sincronización Automática",
        "auto_sync_sub" to "Sincronizar fotogramas cuando haya red",
        "app_locked" to "Terminal Bloqueada",
        "enter_pin" to "Ingrese código PIN de 4 dígitos",
        "unlock" to "Desbloquear Terminal",
        "biometric_unlock" to "Desbloqueo Biométrico / Huella",
        "incorrect_pin" to "PIN incorrecto. El predeterminado es 1234.",
        "send_train" to "Enviar Tren en Vivo",
        "train_en_route" to "TREN TR-104 EN RUTA",
        "reset_train" to "Reiniciar Tren",
        "pause_train" to "Detener Tren",
        "attention_required" to "Atención Inmediata Requerida",
        "field_engineering" to "Guías de Ingeniería de Campo",
        "notifications" to "Centro de Notificaciones",
        "about_railguard" to "Acerca de RailGuard",
        "inspector_profile" to "Perfil del Inspector",
        "core_reliability_metrics" to "MÉTRICAS DE CONFIABILIDAD",
        "track_health" to "Salud de Vía",
        "active_cracks" to "Fisuras Activas",
        "coverage" to "Cobertura",
        "load_tonnage" to "Tonelaje Transportado",
        "passcode_pin_protection" to "Protección por PIN",
        "biometric_authentication" to "Autenticación Biométrica",
        "save_passcode" to "Guardar PIN",
        "passcode_saved" to "PIN guardado exitosamente",
        "north_corridor_patrol" to "Patrulla del Corredor Norte"
    )
}
