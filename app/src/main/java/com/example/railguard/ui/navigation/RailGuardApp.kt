package com.example.railguard.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.railguard.data.RailGuardFirebaseService
import com.example.railguard.model.*
import com.example.railguard.theme.RailGuardTheme
import com.example.railguard.ui.screens.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Otp : Screen("otp")
    object ResetPassword : Screen("reset_password")

    // Main Tabs
    object Home : Screen("home")
    object Inspections : Screen("inspections")
    object Defects : Screen("defects")
    object Map : Screen("map")
    object Settings : Screen("settings")

    // Sub-screens
    object InspectionSetup : Screen("inspection_setup")
    object LiveInspection : Screen("live_inspection")
    object InspectionDetails : Screen("inspection_details")
    object InspectionSummary : Screen("inspection_summary")
    object InspectionCalendar : Screen("inspection_calendar")
    object Gps : Screen("gps")
    object Camera : Screen("camera")

    object DefectDetails : Screen("defect_details")
    object CrackMeasurement : Screen("crack_measurement")
    object GrowthAnalysis : Screen("growth_analysis")
    object ImageComparison : Screen("image_comparison")
    object ObjectDetection : Screen("object_detection")
    object Alignment : Screen("alignment")
    object EngineerVerification : Screen("engineer_verification")
    object Comments : Screen("comments")
    object AllObservations : Screen("all_observations")

    object DefectMap : Screen("defect_map")
    object RiskHeatmap : Screen("risk_heatmap")
    object LocationDetails : Screen("location_details")

    object Maintenance : Screen("maintenance")
    object CreateTask : Screen("create_task")
    object TaskDetails : Screen("task_details")
    object BeforeAfter : Screen("before_after")
    object MaintenanceVerification : Screen("maintenance_verification")
    object MaintenanceAnalytics : Screen("maintenance_analytics")

    object Reports : Screen("reports")
    object EvidencePackage : Screen("evidence_package")
    object PdfPreview : Screen("pdf_preview")
    object ShareReport : Screen("share_report")

    object Analytics : Screen("analytics")
    object TrackHealth : Screen("track_health")

    object Profile : Screen("profile")
    object AppSettings : Screen("app_settings")
    object Security : Screen("security")
    object Language : Screen("language")
    object Help : Screen("help")
    object About : Screen("about")
    object Attention : Screen("attention")
    object Notifications : Screen("notifications")
    object AiOracle : Screen("ai_oracle")
    object TrainConnection : Screen("train_connection")
    object FirebaseSync : Screen("firebase_sync")
}

data class NavigationTab(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun RailGuardApp() {
    val firebaseService = remember { RailGuardFirebaseService.instance }
    var appPreferences by remember { mutableStateOf(firebaseService.loadAppPreferences()) }
    var isDarkMode by remember { mutableStateOf(appPreferences.isDarkMode) }
    var isAppLocked by remember { mutableStateOf(false) }
    var currentRoute by remember { mutableStateOf<String>(Screen.Splash.route) }
    val backStack = remember { mutableStateListOf<String>() }

    // State data
    val defects = remember { mutableStateListOf(*RailDataRepository.initialDefects.toTypedArray()) }
    val tasks = remember { mutableStateListOf(*RailDataRepository.initialTasks.toTypedArray()) }
    val inspections = remember { mutableStateListOf(*RailDataRepository.initialInspections.toTypedArray()) }
    val observations = remember { mutableStateListOf(*RailDataRepository.initialObservations.toTypedArray()) }
    val notifications = remember { mutableStateListOf(*RailDataRepository.initialNotifications.toTypedArray()) }

    var latestSensorTelemetry by remember {
        mutableStateOf<RailGuardFirebaseService.LiveSensorTelemetry?>(null)
    }
    var selectedDefect by remember { mutableStateOf<Defect?>(null) }
    var selectedTask by remember { mutableStateOf<MaintenanceTask?>(null) }
    var selectedInspection by remember { mutableStateOf<InspectionRecord?>(null) }
    var selectedReportTitle by remember { mutableStateOf("RailGuard Corridor Safety Report") }

    var profileName by remember {
        mutableStateOf(
            firebaseService.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: firebaseService.currentUser?.email?.substringBefore("@")
                ?: "Field Inspector"
        )
    }
    var profileEmail by remember {
        mutableStateOf(firebaseService.currentUser?.email ?: "")
    }
    val scope = rememberCoroutineScope()

    fun updatePreferences(updated: AppPreferences) {
        appPreferences = updated
        isDarkMode = updated.isDarkMode
        firebaseService.saveAppPreferences(updated)
        scope.launch {
            firebaseService.saveUserSettings(
                mapOf(
                    "language" to updated.language.code,
                    "darkMode" to updated.isDarkMode,
                    "passcodeEnabled" to updated.passcodeEnabled,
                    "biometricEnabled" to updated.isBiometricEnabled,
                    "metricUnits" to updated.isMetric,
                    "autoSync" to updated.autoSync,
                    "autoSyncRtdb" to updated.autoSyncRtdb,
                    "esp32LinkActive" to updated.esp32LinkActive,
                    "highPrecisionAi" to updated.highPrecisionAi,
                    "tsrInterlockEnabled" to updated.tsrInterlockEnabled,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        }
    }

    // Keep the app state as a projection of the authenticated user's cloud
    // namespace. Empty cloud collections are intentionally empty; they are not
    // replaced with sample records.
    LaunchedEffect(firebaseService.currentUser?.localId) {
        if (firebaseService.currentUser == null) return@LaunchedEffect
        firebaseService.currentUser?.let { user ->
            if (user.displayName.isNotBlank()) profileName = user.displayName
            if (user.email.isNotBlank()) profileEmail = user.email
        }
        firebaseService.pullUserSettings { remotePreferences ->
            appPreferences = remotePreferences
            isDarkMode = remotePreferences.isDarkMode
        }
        while (true) {
            firebaseService.pullAllFromFirebase(
                onSuccess = { rDefects, rTasks, rInspections ->
                    defects.clear()
                    defects.addAll(rDefects)
                    tasks.clear()
                    tasks.addAll(rTasks)
                    inspections.clear()
                    inspections.addAll(rInspections)
                    selectedDefect = selectedDefect?.let { current ->
                        rDefects.firstOrNull { it.id == current.id }
                    }
                    selectedTask = selectedTask?.let { current ->
                        rTasks.firstOrNull { it.id == current.id }
                    }
                    selectedInspection = selectedInspection?.let { current ->
                        rInspections.firstOrNull { it.id == current.id }
                    }
                },
                onError = { /* keep the last confirmed cloud snapshot visible */ }
            )
            firebaseService.pullNotifications(
                onSuccess = {
                    notifications.clear()
                    notifications.addAll(it)
                }
            )
            firebaseService.pullObservations(
                onSuccess = {
                    observations.clear()
                    observations.addAll(it)
                }
            )
            latestSensorTelemetry = firebaseService.fetchLatestSensorTelemetry()
            delay(10_000)
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun navigateTo(route: String) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        if (currentRoute != route) {
            backStack.add(currentRoute)
            currentRoute = route
        }
    }

    fun navigateBack() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        if (backStack.isNotEmpty()) {
            currentRoute = backStack.removeAt(backStack.size - 1)
        }
    }

    BackHandler(enabled = backStack.isNotEmpty()) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        navigateBack()
    }

    val tabs = listOf(
        NavigationTab(Screen.Home.route, appPreferences.translate("home"), Icons.Default.Home),
        NavigationTab(Screen.Inspections.route, appPreferences.translate("inspect"), Icons.Default.FactCheck),
        NavigationTab(Screen.Defects.route, appPreferences.translate("defects"), Icons.Default.Warning),
        NavigationTab(Screen.Map.route, appPreferences.translate("map"), Icons.Default.Map),
        NavigationTab(Screen.Settings.route, appPreferences.translate("control"), Icons.Default.Settings)
    )

    val isTabScreen = currentRoute in tabs.map { it.route }

    CompositionLocalProvider(
        LocalAppSettings provides appPreferences
    ) {
        RailGuardTheme(darkTheme = isDarkMode) {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isTabScreen) {
                            NavigationBar {
                                tabs.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (currentRoute != tab.route) {
                                                backStack.clear()
                                                currentRoute = tab.route
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title
                                            )
                                        },
                                        label = { Text(tab.title) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentRoute,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                            },
                            label = "ScreenTransition"
                        ) { targetRoute ->
                            when (targetRoute) {
                                Screen.Splash.route -> SplashScreen(
                                    onContinue = { navigateTo(Screen.Onboarding.route) },
                                    onLoginClick = { navigateTo(Screen.Login.route) },
                                    onRegisterClick = { navigateTo(Screen.Register.route) }
                                )
                                Screen.Onboarding.route -> OnboardingScreen(
                                    onFinish = {
                                        backStack.clear()
                                        navigateTo(Screen.Home.route)
                                    }
                                )
                                Screen.Login.route -> LoginScreen(
                                    onLoginSuccess = {
                                        firebaseService.currentUser?.let { u ->
                                            profileName = if (!u.displayName.isNullOrBlank()) u.displayName else u.email.substringBefore("@")
                                            profileEmail = u.email
                                        }
                                        backStack.clear()
                                        navigateTo(Screen.Home.route)
                                    },
                                    onForgotPasswordClick = { navigateTo(Screen.ForgotPassword.route) },
                                    onRegisterClick = { navigateTo(Screen.Register.route) }
                                )
                        Screen.Register.route -> RegistrationScreen(
                            onRegisterSuccess = { name, email ->
                                if (name.isNotBlank()) profileName = name
                                if (email.isNotBlank()) profileEmail = email
                                backStack.clear()
                                navigateTo(Screen.Home.route)
                            },
                            onLoginClick = { navigateTo(Screen.Login.route) }
                        )
                        Screen.ForgotPassword.route -> ForgotPasswordScreen(
                            onBackToLogin = { navigateTo(Screen.Login.route) }
                        )
                        Screen.Otp.route -> OtpScreen(
                            onVerifySuccess = { navigateTo(Screen.ResetPassword.route) }
                        )
                        Screen.ResetPassword.route -> ResetPasswordScreen(
                            onResetSuccess = { navigateTo(Screen.Login.route) }
                        )

                        // Tabs
                        Screen.Home.route -> HomeScreen(
                            defects = defects,
                            tasks = tasks,
                            telemetry = latestSensorTelemetry,
                            inspectorName = profileName,
                            onNavigate = { navigateTo(it) },
                            onDefectClick = {
                                selectedDefect = it
                                navigateTo(Screen.DefectDetails.route)
                            },
                            onTaskClick = {
                                selectedTask = it
                                navigateTo(Screen.TaskDetails.route)
                            }
                        )
                        Screen.Inspections.route -> InspectionsListScreen(
                            inspections = inspections,
                            onSelectInspection = {
                                selectedInspection = it
                                navigateTo(Screen.InspectionDetails.route)
                            },
                            onStartNew = { navigateTo(Screen.InspectionSetup.route) },
                            onViewCalendar = { navigateTo(Screen.InspectionCalendar.route) },
                            onOpenCamera = { navigateTo(Screen.Camera.route) },
                            onOpenGps = { navigateTo(Screen.Gps.route) }
                        )
                        Screen.Defects.route -> DefectsListScreen(
                            defects = defects,
                            onSelectDefect = {
                                selectedDefect = it
                                navigateTo(Screen.DefectDetails.route)
                            },
                            onViewObservations = { navigateTo(Screen.AllObservations.route) },
                            onOpenDefectMap = { navigateTo(Screen.DefectMap.route) },
                            onOpenCrackGrowth = { navigateTo(Screen.GrowthAnalysis.route) }
                        )
                        Screen.Map.route -> MapScreen(
                            onNavigateToDefectMap = { navigateTo(Screen.DefectMap.route) },
                            onNavigateToHeatmap = { navigateTo(Screen.RiskHeatmap.route) },
                            onNavigateToLocationDetails = { navigateTo(Screen.LocationDetails.route) },
                            onSelectDefect = {
                                selectedDefect = it
                                navigateTo(Screen.DefectDetails.route)
                            },
                            defects = defects
                        )
                        Screen.Settings.route -> SettingsScreen(
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { updatePreferences(appPreferences.copy(isDarkMode = it)) },
                            onNavigate = { navigateTo(it) },
                            onSignOut = {
                                firebaseService.signOut()
                                backStack.clear()
                                navigateTo(Screen.Login.route)
                            },
                            onLockApp = { isAppLocked = true }
                        )

                        // Sub-screens: Inspections
                        Screen.InspectionSetup.route -> InspectionSetupScreen(
                            onStartPatrol = { newRecord, isLive ->
                                inspections.add(0, newRecord)
                                selectedInspection = newRecord
                                scope.launch {
                                    firebaseService.uploadInspectionToFirebase(newRecord)
                                    firebaseService.logSafetyAuditEvent(
                                        action = "PATROL_LAUNCH",
                                        details = "Inspection patrol ${newRecord.id} started on section ${newRecord.section} by ${newRecord.inspector}"
                                    )
                                }
                                if (isLive) {
                                    navigateTo(Screen.LiveInspection.route)
                                } else {
                                    navigateTo(Screen.InspectionDetails.route)
                                }
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.LiveInspection.route -> selectedInspection?.let { inspection ->
                            LiveInspectionScreen(
                            inspection = inspection,
                            telemetry = latestSensorTelemetry,
                            cloudDefectCount = defects.size,
                            onEndInspection = {
                                selectedInspection?.let { inspection ->
                                    val completed = inspection.copy(
                                        status = "Completed · Pending Sign-off"
                                    )
                                    val idx = inspections.indexOfFirst { it.id == inspection.id }
                                    if (idx >= 0) inspections[idx] = completed
                                    selectedInspection = completed
                                    scope.launch {
                                        firebaseService.uploadInspectionToFirebase(completed)
                                        firebaseService.logSafetyAuditEvent(
                                            action = "PATROL_COMPLETED",
                                            details = "Inspection ${completed.id} completed on ${completed.section}"
                                        )
                                    }
                                    navigateTo(Screen.InspectionSummary.route)
                                }
                            },
                            onDefectDetected = {
                                selectedDefect = defects.firstOrNull()
                                if (selectedDefect != null) navigateTo(Screen.DefectDetails.route)
                            },
                            onOpenGps = { navigateTo(Screen.Gps.route) },
                            onOpenCamera = { navigateTo(Screen.Camera.route) },
                            onBack = { navigateBack() }
                            )
                        } ?: CloudDataRequiredScreen("Start a cloud inspection before opening the live viewfinder", ::navigateBack)
                        Screen.InspectionDetails.route -> selectedInspection?.let { inspection ->
                            InspectionDetailsScreen(
                                inspection = inspection,
                                onOpenLive = { navigateTo(Screen.LiveInspection.route) },
                                onOpenSummary = { navigateTo(Screen.InspectionSummary.route) },
                                onBack = { navigateBack() }
                            )
                        } ?: CloudDataRequiredScreen("Select a cloud inspection before opening details", ::navigateBack)
                        Screen.InspectionSummary.route -> InspectionSummaryScreen(
                            onDone = {
                                navigateBack()
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.InspectionCalendar.route -> InspectionCalendarScreen(
                            inspections = inspections,
                            onBack = { navigateBack() }
                        )
                        Screen.Gps.route -> GpsScreen(onBack = { navigateBack() })
                        Screen.Camera.route -> CameraScreen(
                            onCaptureDefect = {
                                selectedDefect = defects.firstOrNull()
                                if (selectedDefect != null) navigateTo(Screen.DefectDetails.route)
                            },
                            onBack = { navigateBack() }
                        )

                        // Sub-screens: Defects
                        Screen.DefectDetails.route -> selectedDefect?.let { defect ->
                            DefectDetailsScreen(
                                defect = defect,
                                onOpenMeasurement = { navigateTo(Screen.CrackMeasurement.route) },
                                onOpenComparison = { navigateTo(Screen.ImageComparison.route) },
                                onOpenObjectDetection = { navigateTo(Screen.ObjectDetection.route) },
                                onOpenAlignment = { navigateTo(Screen.Alignment.route) },
                                onOpenGrowth = { navigateTo(Screen.GrowthAnalysis.route) },
                                onOpenVerify = { navigateTo(Screen.EngineerVerification.route) },
                                onOpenComments = { navigateTo(Screen.Comments.route) },
                                onBack = { navigateBack() }
                            )
                        } ?: CloudDataRequiredScreen("Select a cloud defect before opening details", ::navigateBack)
                        Screen.CrackMeasurement.route -> CrackMeasurementScreen(
                            defect = selectedDefect,
                            onBack = { navigateBack() }
                        )
                        Screen.GrowthAnalysis.route -> GrowthAnalysisScreen(
                            defect = selectedDefect,
                            onBack = { navigateBack() }
                        )
                        Screen.ImageComparison.route -> ImageComparisonScreen(
                            defect = selectedDefect,
                            onBack = { navigateBack() }
                        )
                        Screen.ObjectDetection.route -> ObjectDetectionScreen(
                            defect = selectedDefect,
                            onBack = { navigateBack() }
                        )
                        Screen.Alignment.route -> AlignmentAnalysisScreen(
                            defect = selectedDefect,
                            onBack = { navigateBack() }
                        )
                        Screen.EngineerVerification.route -> EngineerVerificationScreen(
                            onSigned = {
                                scope.launch {
                                     val defect = selectedDefect ?: return@launch
                                     val verified = defect.copy(
                                        score = "Verified (100%)",
                                        tone = Tone.HEALTHY,
                                        aiPrescribedAction = "Verified & signed off by Lead Engineer ($profileName)"
                                    )
                                     val idx = defects.indexOfFirst { it.id == defect.id }
                                    if (idx >= 0) defects[idx] = verified
                                    selectedDefect = verified
                                    firebaseService.uploadDefectToFirebase(verified)
                                    firebaseService.logSafetyAuditEvent(
                                        action = "ENGINEER_VERIFICATION",
                                        details = "Defect ${verified.id} (${verified.title}) verified and signed by $profileName"
                                    )
                                }
                                navigateBack()
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.Comments.route -> CommentsScreen(onBack = { navigateBack() })
                        Screen.AllObservations.route -> AllObservationsScreen(
                            observations = observations,
                            onBack = { navigateBack() }
                        )

                        // Sub-screens: Map
                        Screen.DefectMap.route -> DefectMapScreen(
                            defects = defects,
                            onSelectDefect = {
                                selectedDefect = it
                                navigateTo(Screen.DefectDetails.route)
                            },
                            onNavigateToHeatmap = { navigateTo(Screen.RiskHeatmap.route) },
                            onNavigateToGps = { navigateTo(Screen.Gps.route) },
                            onBack = { navigateBack() }
                        )
                        Screen.RiskHeatmap.route -> RiskHeatmapScreen(defects = defects, onBack = { navigateBack() })
                        Screen.LocationDetails.route -> LocationDetailsScreen(onBack = { navigateBack() })

                        // Sub-screens: Maintenance
                        Screen.Maintenance.route -> MaintenanceScreen(
                            tasks = tasks,
                            onSelectTask = {
                                selectedTask = it
                                navigateTo(Screen.TaskDetails.route)
                            },
                            onCreateTask = { navigateTo(Screen.CreateTask.route) },
                            onViewAnalytics = { navigateTo(Screen.MaintenanceAnalytics.route) },
                            onBack = { navigateBack() }
                        )
                        Screen.CreateTask.route -> CreateMaintenanceTaskScreen(
                            onTaskCreated = { newTask: MaintenanceTask ->
                                tasks.add(0, newTask)
                                selectedTask = newTask
                                scope.launch {
                                    firebaseService.uploadTaskToFirebase(newTask)
                                    firebaseService.logSafetyAuditEvent(
                                        action = "MAINTENANCE_TASK_CREATED",
                                        details = "Task ${newTask.id} dispatched for ${newTask.section} to ${newTask.assignee}"
                                    )
                                }
                                navigateTo(Screen.TaskDetails.route)
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.TaskDetails.route -> selectedTask?.let { task ->
                            TaskDetailsScreen(
                                task = task,
                                onOpenBeforeAfter = { navigateTo(Screen.BeforeAfter.route) },
                                onOpenVerify = { navigateTo(Screen.MaintenanceVerification.route) },
                                onBack = { navigateBack() }
                            )
                        } ?: CloudDataRequiredScreen("Select a cloud maintenance task before opening details", ::navigateBack)
                        Screen.BeforeAfter.route -> BeforeAfterScreen(onBack = { navigateBack() })
                        Screen.MaintenanceVerification.route -> MaintenanceVerificationScreen(
                            onVerified = {
                                scope.launch {
                                     selectedTask?.let { task ->
                                         val verifiedTask = task.copy(
                                             status = "Completed · Verified",
                                             tone = Tone.HEALTHY
                                         )
                                         val idx = tasks.indexOfFirst { it.id == task.id }
                                         if (idx >= 0) tasks[idx] = verifiedTask
                                         selectedTask = verifiedTask
                                         firebaseService.uploadTaskToFirebase(verifiedTask)
                                         firebaseService.logSafetyAuditEvent(
                                             action = "MAINTENANCE_VERIFIED",
                                             details = "Task ${verifiedTask.id} completed and verified for ${verifiedTask.section}"
                                         )
                                     }
                                }
                                navigateBack()
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.MaintenanceAnalytics.route -> MaintenanceAnalyticsScreen(tasks = tasks, onBack = { navigateBack() })

                        // Sub-screens: Reports
                        Screen.Reports.route -> ReportsScreen(
                            onBuildPackage = { navigateTo(Screen.EvidencePackage.route) },
                            onOpenPdf = {
                                selectedReportTitle = it
                                navigateTo(Screen.PdfPreview.route)
                            },
                            onOpenShare = {
                                selectedReportTitle = it
                                navigateTo(Screen.ShareReport.route)
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.EvidencePackage.route -> BuildEvidencePackageScreen(
                            onGenerated = {
                                selectedReportTitle = "Compiled Field Package · SHA-256 Verified"
                                navigateTo(Screen.PdfPreview.route)
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.PdfPreview.route -> PdfPreviewScreen(
                            reportTitle = selectedReportTitle,
                            inspectorName = profileName,
                            defects = defects,
                            tasks = tasks,
                            onBack = { navigateBack() }
                        )
                        Screen.ShareReport.route -> ShareReportScreen(
                            reportTitle = selectedReportTitle,
                            onBack = { navigateBack() }
                        )

                        // Sub-screens: Analytics
                        Screen.Analytics.route -> AnalyticsScreen(
                            onNavigateTrackHealth = { navigateTo(Screen.TrackHealth.route) },
                            onNavigateCrackAnalytics = { navigateTo(Screen.GrowthAnalysis.route) },
                            onNavigateRiskAnalytics = { navigateTo(Screen.RiskHeatmap.route) },
                            onExportPdf = {
                                selectedReportTitle = "Full Network Analytics & Sensor Telemetry Dossier"
                                navigateTo(Screen.PdfPreview.route)
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.TrackHealth.route -> TrackHealthScreen(onBack = { navigateBack() })

                        // Sub-screens: Settings
                        Screen.Profile.route -> ProfileScreen(
                            name = profileName,
                            email = profileEmail,
                            onSave = { n, e ->
                                profileName = n
                                profileEmail = e
                                scope.launch {
                                    firebaseService.updateInspectorProfile(n, e)
                                }
                            },
                            onBack = { navigateBack() }
                        )
                        Screen.AppSettings.route -> AppSettingsScreen(
                            preferences = appPreferences,
                            onUpdatePreferences = { updatePreferences(it) },
                            onBack = { navigateBack() }
                        )
                        Screen.Security.route -> SecurityScreen(
                            preferences = appPreferences,
                            onUpdatePreferences = { updatePreferences(it) },
                            onLockApp = { isAppLocked = true },
                            onBack = { navigateBack() }
                        )
                        Screen.Language.route -> LanguageScreen(
                            preferences = appPreferences,
                            onUpdatePreferences = { updatePreferences(it) },
                            onBack = { navigateBack() }
                        )
                        Screen.Help.route -> HelpCenterScreen(onBack = { navigateBack() })
                        Screen.About.route -> AboutScreen(onBack = { navigateBack() })
                        Screen.Attention.route -> AttentionScreen(
                            defects = defects,
                            tasks = tasks,
                            onNavigateDefect = {
                                 selectedDefect = defects.firstOrNull()
                                 if (selectedDefect != null) navigateTo(Screen.DefectDetails.route)
                            },
                            onNavigateTask = {
                                 selectedTask = tasks.firstOrNull()
                                 if (selectedTask != null) navigateTo(Screen.TaskDetails.route)
                            },
                            onCreateTask = { navigateTo(Screen.CreateTask.route) },
                            onCompareImages = { navigateTo(Screen.ImageComparison.route) },
                            onAllObservations = { navigateTo(Screen.AllObservations.route) },
                            onReviewDetection = { navigateTo(Screen.ObjectDetection.route) },
                            onBuildEvidencePackage = { navigateTo(Screen.EvidencePackage.route) },
                            onRiskHeatmap = { navigateTo(Screen.RiskHeatmap.route) },
                            onBack = { navigateBack() }
                        )
                        Screen.Notifications.route -> NotificationsScreen(
                            notifications = notifications,
                            onBack = { navigateBack() }
                        )
                        Screen.AiOracle.route -> AiOracleScreen(
                            defects = defects,
                            tasks = tasks,
                            telemetry = latestSensorTelemetry,
                            onInspectDefect = {
                                selectedDefect = it
                                navigateTo(Screen.DefectDetails.route)
                            },
                            onDispatchTask = {
                                selectedTask = it
                                navigateTo(Screen.TaskDetails.route)
                            },
                            onNavigateCrackGrowth = { navigateTo(Screen.GrowthAnalysis.route) },
                            onNavigateHeatmap = { navigateTo(Screen.RiskHeatmap.route) },
                            onBack = { navigateBack() }
                        )
                        Screen.TrainConnection.route -> HardwareTelemetryScreen(
                            onBack = { navigateBack() }
                        )
                        Screen.FirebaseSync.route -> AppSettingsScreen(
                            preferences = appPreferences,
                            onUpdatePreferences = { updatePreferences(it) },
                            onBack = { navigateBack() }
                        )
                    }
                }
            }
        }

        if (isAppLocked && appPreferences.passcodeEnabled) {
            PasscodeLockScreen(
                expectedPin = appPreferences.passcodePin,
                allowBiometric = appPreferences.isBiometricEnabled,
                onUnlock = { isAppLocked = false }
            )
        }
    }
}
}
}
