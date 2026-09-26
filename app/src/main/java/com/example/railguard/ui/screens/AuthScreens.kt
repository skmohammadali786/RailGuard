package com.example.railguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.railguard.components.PrimaryButton
import com.example.railguard.components.StatusPill
import com.example.railguard.data.RailGuardFirebaseService
import com.example.railguard.model.LocalAppSettings
import com.example.railguard.model.Tone
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onContinue: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "RAILGUARD",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "FIELD ENGINEERING CONTROL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            StatusPill(label = "FIELD SYSTEM READY", tone = Tone.HEALTHY)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Inspect with evidence.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A trusted instrument for live track inspections, defect review, and maintenance handoff.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(
                title = "Sign In to RailGuard",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onLoginClick
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Account (Sign Up)",
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Field Operations & Safety Overview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var currentStep by remember { mutableIntStateOf(0) }

    val slides = listOf(
        Triple(
            Icons.Default.CameraAlt,
            "Capture with confidence",
            "Record sharp, geolocated rail evidence from the field with automated defect localization."
        ),
        Triple(
            Icons.Default.Warning,
            "Review what matters",
            "Prioritize AI findings by confidence, severity, and chainage with immediate speed restriction triggers."
        ),
        Triple(
            Icons.Default.Shield,
            "Close the loop",
            "Keep verification, maintenance tasks, and safety decisions connected in a verifiable audit trail."
        )
    )

    val currentSlide = slides[currentStep]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RAILGUARD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
            }

            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onFinish() }
            )
        }

        // Hero Graphic Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surface)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = currentSlide.first,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = currentSlide.second,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = currentSlide.third,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentStep) 18.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (index == currentStep) colorScheme.primary else colorScheme.outline)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                title = if (currentStep == slides.size - 1) "Get started" else "Next",
                onClick = {
                    if (currentStep < slides.size - 1) {
                        currentStep++
                    } else {
                        onFinish()
                    }
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings = LocalAppSettings.current
    val firebaseService = remember { RailGuardFirebaseService.instance }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Auth Mode: 0 = Sign In, 1 = Create Account / Sign Up
    var authMode by remember { mutableIntStateOf(0) }
    
    // Sign In fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    // Sign Up fields
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regAcceptedTerms by remember { mutableStateOf(true) }
    
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Brand Icon & Firebase Cloud Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F766E).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF0F766E).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SECURE CLOUD · TELEMETRY SYNC",
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prominent Mode Switcher Tabs: Sign In / Create Account
                TabRow(
                    selectedTabIndex = authMode,
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = authMode == 0,
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            authMode = 0
                            errorMessage = ""
                        },
                        text = { Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = authMode == 1,
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            authMode = 1
                            errorMessage = ""
                        },
                        text = { Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (authMode == 0) {
                    // =================== SIGN IN VIEW ===================
                    Text(
                        text = settings.translate("welcome_back"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign in to authenticate and sync track inspection telemetry to the central safety cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = "" },
                        label = { Text("Work Email") },
                        placeholder = { Text("inspector@railguard.field") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = "" },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = settings.translate("forgot_password"),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onForgotPasswordClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        title = if (isLoading) "Connecting to Cloud Network..." else settings.translate("sign_in"),
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = {
                            if (!email.contains("@")) {
                                errorMessage = "Enter a valid work email."
                            } else if (password.length < 4) {
                                errorMessage = "Enter your password."
                            } else {
                                isLoading = true
                                errorMessage = ""
                                scope.launch {
                                    firebaseService.signInWithEmailAndPassword(
                                        email = email.trim(),
                                        pass = password,
                                        onSuccess = {
                                            isLoading = false
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                            onLoginSuccess()
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "New inspector? Tap to Create an Account",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { authMode = 1 }
                    )
                } else {
                    // =================== CREATE ACCOUNT / SIGN UP VIEW ===================
                    Text(
                        text = settings.translate("create_account"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create your inspector account to synchronize track inspection telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it; errorMessage = "" },
                        label = { Text("Inspector Full Name") },
                        placeholder = { Text("e.g. Alex Morgan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it; errorMessage = "" },
                        label = { Text("Work Email") },
                        placeholder = { Text("name@railguard.field") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it; errorMessage = "" },
                        label = { Text("Password (min 6 characters)") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { regAcceptedTerms = !regAcceptedTerms }
                    ) {
                        Checkbox(
                            checked = regAcceptedTerms,
                            onCheckedChange = { regAcceptedTerms = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I agree to track field safety & audit guidelines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        title = if (isLoading) "Creating Cloud Account..." else "Create Account & Sign In",
                        icon = Icons.Default.PersonAdd,
                        onClick = {
                            if (regFullName.trim().length < 2) {
                                errorMessage = "Please enter your full name."
                            } else if (!regEmail.contains("@")) {
                                errorMessage = "Please enter a valid email."
                            } else if (regPassword.length < 6) {
                                errorMessage = "Password must be at least 6 characters."
                            } else if (!regAcceptedTerms) {
                                errorMessage = "Please accept the field safety guidelines."
                            } else {
                                isLoading = true
                                errorMessage = ""
                                scope.launch {
                                    firebaseService.signUpWithEmailAndPassword(
                                        name = regFullName.trim(),
                                        email = regEmail.trim(),
                                        pass = regPassword,
                                        onSuccess = {
                                            isLoading = false
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                            onLoginSuccess()
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Already have an account? Tap to Sign In",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { authMode = 0 }
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    onRegisterSuccess: (name: String, email: String) -> Unit,
    onLoginClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings = LocalAppSettings.current
    val firebaseService = remember { RailGuardFirebaseService.instance }
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Brand Icon & Firebase Cloud Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F766E).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF0F766E).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SECURE CLOUD · TELEMETRY SYNC",
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prominent Mode Switcher Tabs
                TabRow(
                    selectedTabIndex = 1,
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = false,
                        onClick = { onLoginClick() },
                        text = { Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = true,
                        onClick = { },
                        text = { Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = settings.translate("create_account"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = settings.translate("create_account_sub"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = "" },
                    label = { Text("Full Name (Inspector Name)") },
                    placeholder = { Text("e.g. Alex Vance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    label = { Text("Work Email") },
                    placeholder = { Text("name@railguard.field") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = "" },
                    label = { Text("Password (min 6 chars)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { acceptedTerms = !acceptedTerms }
                ) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = { acceptedTerms = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "I agree to the field safety and evidence terms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    title = if (isRegistering) "Creating Cloud Account..." else settings.translate("create_account_btn"),
                    onClick = {
                        if (fullName.trim().length < 2) {
                            errorMessage = "Enter your full name."
                        } else if (!email.contains("@")) {
                            errorMessage = "Enter a valid work email."
                        } else if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters."
                        } else if (!acceptedTerms) {
                            errorMessage = "Accept the field safety terms to continue."
                        } else {
                            isRegistering = true
                            errorMessage = ""
                            scope.launch {
                                firebaseService.signUpWithEmailAndPassword(
                                    name = fullName.trim(),
                                    email = email.trim(),
                                    pass = password,
                                    onSuccess = { user ->
                                        isRegistering = false
                                        onRegisterSuccess(user.displayName.ifBlank { fullName.trim() }, user.email)
                                    },
                                    onError = { err ->
                                        isRegistering = false
                                        errorMessage = err
                                    }
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = settings.translate("already_registered"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val firebaseService = remember { RailGuardFirebaseService.instance }
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = settings.translate("recover_access"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = settings.translate("recover_access_sub"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    label = { Text("Work Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        color = Color(0xFF16A34A),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    title = if (isSending) "Sending reset email..." else settings.translate("send_code"),
                    onClick = {
                        if (!email.contains("@")) {
                            errorMessage = "Enter the email associated with your account."
                        } else {
                            isSending = true
                            errorMessage = ""
                            statusMessage = ""
                            scope.launch {
                                firebaseService.sendPasswordResetEmail(
                                    email = email,
                                    onSuccess = {
                                        isSending = false
                                        statusMessage = "Password reset email sent. Open the link in your email to choose a new password."
                                    },
                                    onError = { message ->
                                        isSending = false
                                        errorMessage = message
                                    }
                                )
                            }
                        }
                    },
                    disabled = isSending
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Back to sign in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onBackToLogin() }
                )
            }
        }
    }
}

@Composable
fun OtpScreen(
    onVerifySuccess: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var otpCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Pin,
                contentDescription = null,
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Verify your code",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter the 6-digit passcode dispatched to your email address",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = {
                if (it.length <= 6) {
                    otpCode = it
                    errorMessage = ""
                }
            },
            label = { Text("6-digit code") },
            placeholder = { Text("748291") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            title = "Verify and continue",
            onClick = {
                if (otpCode.length < 6) {
                    errorMessage = "Enter the complete 6-digit code."
                } else {
                    onVerifySuccess()
                }
            }
        )
    }
}

@Composable
fun ResetPasswordScreen(
    onResetSuccess: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Set a new passcode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose a strong password to protect your field evidence sign-offs.",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; errorMessage = "" },
            label = { Text("New password (min 8 chars)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = "" },
            label = { Text("Confirm new password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            title = "Save new password",
            onClick = {
                if (newPassword.length < 8) {
                    errorMessage = "Password must be at least 8 characters."
                } else if (newPassword != confirmPassword) {
                    errorMessage = "Passwords do not match."
                } else {
                    onResetSuccess()
                }
            }
        )
    }
}

@Composable
fun PasscodeLockScreen(
    expectedPin: String,
    allowBiometric: Boolean,
    onUnlock: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Terminal Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter 4-digit security PIN or scan biometric",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN display dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { idx ->
                val filled = idx < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) colorScheme.primary else colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }

        if (errorMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Keypad grid
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("BIO", "0", "DEL")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable {
                                when (key) {
                                    "DEL" -> {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                            errorMsg = ""
                                        }
                                    }
                                    "BIO" -> {
                                        if (allowBiometric) {
                                            onUnlock()
                                        } else {
                                            errorMsg = "Biometrics disabled in settings"
                                        }
                                    }
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            val newPin = enteredPin + key
                                            enteredPin = newPin
                                            if (newPin.length == 4) {
                                                if (newPin == expectedPin) {
                                                    onUnlock()
                                                } else {
                                                    errorMsg = "Incorrect PIN. Try again."
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "DEL" -> Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = colorScheme.onSurface)
                            "BIO" -> Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = if (allowBiometric) colorScheme.primary else colorScheme.outline)
                            else -> Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
