package com.example.railguard.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.railguard.model.Defect
import com.example.railguard.model.AppLanguage
import com.example.railguard.model.AppPreferences
import com.example.railguard.model.InspectionRecord
import com.example.railguard.model.MaintenanceTask
import com.example.railguard.model.ObservationItem
import com.example.railguard.model.Tone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class FirebaseUser(
    val localId: String,
    val email: String,
    val displayName: String = "",
    val idToken: String = "",
    val refreshToken: String = "",
    val expiresIn: String = "",
    val tokenExpiresAt: Long = 0L
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val httpCode: Int,
    val latencyMs: Long,
    val message: String,
    val rawResponse: String = ""
)

data class FirebaseUploadResult(
    val isSuccess: Boolean,
    val storagePath: String = "",
    val downloadUrl: String = "",
    val message: String
)

enum class DatabaseBackendType {
    REALTIME_DATABASE,
    FIRESTORE
}

/**
 * RailGuardFirebaseService:
 * Real Firebase backend integration connected directly to Firebase Cloud REST APIs:
 * - Firebase Authentication (Identity Toolkit v1 API)
 * - Firebase Realtime Database & Cloud Firestore REST endpoints
 * - Permanent credential persistence via Android SharedPreferences
 * - Direct live responses from production cloud servers
 */
class RailGuardFirebaseService private constructor() {

    companion object {
        val instance by lazy { RailGuardFirebaseService() }
        private const val PREFS_NAME = "railguard_firebase_prefs"
        private const val KEY_PROJECT_ID = "firebase_project_id"
        private const val KEY_WEB_API_KEY = "firebase_web_api_key"
        private const val KEY_DB_URL = "firebase_database_url"
        private const val KEY_DB_TYPE = "firebase_database_type"
        private const val KEY_USER_ID = "firebase_user_id"
        private const val KEY_USER_EMAIL = "firebase_user_email"
        private const val KEY_USER_NAME = "firebase_user_name"
        private const val KEY_ID_TOKEN = "firebase_id_token"
        private const val KEY_REFRESH_TOKEN = "firebase_refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "firebase_token_expires_at"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_DARK_MODE = "app_dark_mode"
        private const val KEY_PASSCODE_ENABLED = "app_passcode_enabled"
        private const val KEY_PASSCODE_PIN = "app_passcode_pin"
        private const val KEY_BIOMETRIC_ENABLED = "app_biometric_enabled"
        private const val KEY_METRIC_UNITS = "app_metric_units"
        private const val KEY_AUTO_SYNC = "app_auto_sync"
        private const val KEY_AUTO_SYNC_RTDB = "app_auto_sync_rtdb"
        private const val KEY_ESP32_LINK = "app_esp32_link"
        private const val KEY_HIGH_PRECISION_AI = "app_high_precision_ai"
        private const val KEY_TSR_INTERLOCK = "app_tsr_interlock"

        // Server-Side Pre-Configured Firebase Cloud Project Credentials
        const val DEFAULT_PROJECT_NAME = "railguard"
        const val DEFAULT_PROJECT_ID = "railguard-72a70"
        const val DEFAULT_PROJECT_NUMBER = "590063963377"
        const val DEFAULT_APP_ID = "1:590063963377:android:4c81408ab31a67469e6d50"
        const val DEFAULT_WEB_API_KEY = "AIzaSyDzM8_dgvY9DHUWh9ZZJI-BQ8uwl4OX1uA"
        const val DEFAULT_DB_URL = "https://railguard-72a70-default-rtdb.asia-southeast1.firebasedatabase.app"
        const val DEFAULT_STORAGE_BUCKET = "railguard-72a70.firebasestorage.app"
    }

    private var sharedPreferences: SharedPreferences? = null

    // Server-Side Pre-Configured Firebase Backend
    var projectId by mutableStateOf(DEFAULT_PROJECT_ID)
    var webApiKey by mutableStateOf(DEFAULT_WEB_API_KEY)
    var databaseUrl by mutableStateOf(DEFAULT_DB_URL)
    var databaseType by mutableStateOf(DatabaseBackendType.REALTIME_DATABASE)

    var authState by mutableStateOf<AuthState>(AuthState.Unauthenticated)
    var currentUser by mutableStateOf<FirebaseUser?>(null)

    var isSyncing by mutableStateOf(false)
    var isTestingConnection by mutableStateOf(false)
    var lastSyncTimestamp by mutableStateOf("Never synced")
    var syncStatusMessage by mutableStateOf("Ready to connect with central safety cloud database")
    var isConnectedToFirebase by mutableStateOf(false)
    var lastTestResult by mutableStateOf<ConnectionTestResult?>(null)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(context: Context) {
        if (sharedPreferences != null) return
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        sharedPreferences?.let { prefs ->
            val savedProjectId = prefs.getString(KEY_PROJECT_ID, null)
            val savedApiKey = prefs.getString(KEY_WEB_API_KEY, null)
            val savedDbUrl = prefs.getString(KEY_DB_URL, null)

            // Ensure server-configured credentials are used
            projectId = if (!savedProjectId.isNullOrBlank() && savedProjectId != "railguard-official") savedProjectId else DEFAULT_PROJECT_ID
            webApiKey = if (!savedApiKey.isNullOrBlank()) savedApiKey else DEFAULT_WEB_API_KEY
            databaseUrl = if (!savedDbUrl.isNullOrBlank() && !savedDbUrl.contains("railguard-official") && !savedDbUrl.endsWith(".firebaseio.com")) savedDbUrl else DEFAULT_DB_URL

            // Auto-persist valid server-side credentials
            prefs.edit().apply {
                putString(KEY_PROJECT_ID, projectId)
                putString(KEY_WEB_API_KEY, webApiKey)
                putString(KEY_DB_URL, databaseUrl)
                apply()
            }

            val savedType = prefs.getString(KEY_DB_TYPE, DatabaseBackendType.REALTIME_DATABASE.name)
            databaseType = try {
                DatabaseBackendType.valueOf(savedType ?: DatabaseBackendType.REALTIME_DATABASE.name)
            } catch (e: Exception) {
                DatabaseBackendType.REALTIME_DATABASE
            }

            // Restore user session if saved
            val savedUserId = prefs.getString(KEY_USER_ID, null)
            val savedEmail = prefs.getString(KEY_USER_EMAIL, null)
            val savedToken = prefs.getString(KEY_ID_TOKEN, null)
            if (!savedUserId.isNullOrEmpty() && !savedEmail.isNullOrEmpty() && !savedToken.isNullOrEmpty()) {
                val user = FirebaseUser(
                    localId = savedUserId,
                    email = savedEmail,
                    displayName = prefs.getString(KEY_USER_NAME, "") ?: "",
                    idToken = savedToken,
                    refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: "",
                    tokenExpiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)
                )
                currentUser = user
                authState = AuthState.Authenticated(user)
                isConnectedToFirebase = true
                syncStatusMessage = "Session restored: ${user.email}"
            }
        }
    }

    fun getEffectiveDbUrl(): String {
        return if (databaseUrl.isBlank() || databaseUrl.contains("railguard-official") || databaseUrl.endsWith(".firebaseio.com")) {
            DEFAULT_DB_URL
        } else {
            databaseUrl
        }
    }

    fun getEffectiveApiKey(): String {
        return if (webApiKey.isBlank()) DEFAULT_WEB_API_KEY else webApiKey
    }

    fun saveConfig(newProjectId: String, newApiKey: String, newDbUrl: String, newType: DatabaseBackendType) {
        projectId = newProjectId.trim()
        webApiKey = newApiKey.trim()
        databaseUrl = newDbUrl.trim().removeSuffix("/")
        databaseType = newType

        sharedPreferences?.edit()?.apply {
            putString(KEY_PROJECT_ID, projectId)
            putString(KEY_WEB_API_KEY, webApiKey)
            putString(KEY_DB_URL, databaseUrl)
            putString(KEY_DB_TYPE, databaseType.name)
            apply()
        }

        syncStatusMessage = "Cloud configuration updated for $projectId"
    }

    private fun persistUser(user: FirebaseUser?) {
        sharedPreferences?.edit()?.apply {
            if (user != null) {
                putString(KEY_USER_ID, user.localId)
                putString(KEY_USER_EMAIL, user.email)
                putString(KEY_USER_NAME, user.displayName)
                putString(KEY_ID_TOKEN, user.idToken)
                putString(KEY_REFRESH_TOKEN, user.refreshToken)
                putLong(KEY_TOKEN_EXPIRES_AT, user.tokenExpiresAt)
            } else {
                remove(KEY_USER_ID)
                remove(KEY_USER_EMAIL)
                remove(KEY_USER_NAME)
                remove(KEY_ID_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_TOKEN_EXPIRES_AT)
            }
            apply()
        }
    }

    /**
     * Returns the authenticated user's current ID token, refreshing it when the
     * one issued by Firebase has expired or is close to expiry.
     */
    private suspend fun getValidIdToken(): String {
        val user = currentUser ?: throw IllegalStateException("Sign in is required to sync cloud data.")
        val now = System.currentTimeMillis()
        if (user.idToken.isNotBlank() && user.tokenExpiresAt > now + 60_000L) {
            return user.idToken
        }

        if (user.refreshToken.isBlank()) {
            throw IllegalStateException("Your Firebase session has expired. Please sign in again.")
        }

        val endpoint = "https://securetoken.googleapis.com/v1/token?key=${getEffectiveApiKey()}"
        val payload = "grant_type=refresh_token&refresh_token=${java.net.URLEncoder.encode(user.refreshToken, "UTF-8")}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        OutputStreamWriter(connection.outputStream).use { it.write(payload) }

        val responseCode = connection.responseCode
        val responseBody = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (responseCode !in 200..299) {
            throw IllegalStateException(parseFirebaseError(responseBody, responseCode))
        }

        val json = JSONObject(responseBody)
        val refreshed = user.copy(
            localId = json.optString("user_id", user.localId),
            idToken = json.getString("id_token"),
            refreshToken = json.optString("refresh_token", user.refreshToken),
            expiresIn = json.optString("expires_in", "3600"),
            tokenExpiresAt = now + json.optLong("expires_in", 3600L) * 1000L
        )
        withContext(Dispatchers.Main) {
            currentUser = refreshed
            authState = AuthState.Authenticated(refreshed)
            persistUser(refreshed)
            isConnectedToFirebase = true
        }
        return refreshed.idToken
    }

    private suspend fun authenticatedQueryParam(): String {
        val token = getValidIdToken()
        return "?auth=${java.net.URLEncoder.encode(token, "UTF-8")}"
    }

    private fun userDataRoot(): String {
        val uid = currentUser?.localId
        return if (!uid.isNullOrBlank()) "railguard/users/$uid" else "railguard"
    }

    fun loadAppPreferences(): AppPreferences {
        val prefs = sharedPreferences ?: return AppPreferences()
        val language = runCatching {
            AppLanguage.valueOf(prefs.getString(KEY_APP_LANGUAGE, AppLanguage.EN_UK.name) ?: AppLanguage.EN_UK.name)
        }.getOrDefault(AppLanguage.EN_UK)
        return AppPreferences(
            language = language,
            isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false),
            passcodeEnabled = prefs.getBoolean(KEY_PASSCODE_ENABLED, true),
            passcodePin = prefs.getString(KEY_PASSCODE_PIN, "1234") ?: "1234",
            isBiometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true),
            isMetric = prefs.getBoolean(KEY_METRIC_UNITS, true),
            autoSync = prefs.getBoolean(KEY_AUTO_SYNC, true),
            autoSyncRtdb = prefs.getBoolean(KEY_AUTO_SYNC_RTDB, true),
            esp32LinkActive = prefs.getBoolean(KEY_ESP32_LINK, true),
            highPrecisionAi = prefs.getBoolean(KEY_HIGH_PRECISION_AI, true),
            tsrInterlockEnabled = prefs.getBoolean(KEY_TSR_INTERLOCK, true)
        )
    }

    fun saveAppPreferences(preferences: AppPreferences) {
        sharedPreferences?.edit()?.apply {
            putString(KEY_APP_LANGUAGE, preferences.language.name)
            putBoolean(KEY_DARK_MODE, preferences.isDarkMode)
            putBoolean(KEY_PASSCODE_ENABLED, preferences.passcodeEnabled)
            putString(KEY_PASSCODE_PIN, preferences.passcodePin)
            putBoolean(KEY_BIOMETRIC_ENABLED, preferences.isBiometricEnabled)
            putBoolean(KEY_METRIC_UNITS, preferences.isMetric)
            putBoolean(KEY_AUTO_SYNC, preferences.autoSync)
            putBoolean(KEY_AUTO_SYNC_RTDB, preferences.autoSyncRtdb)
            putBoolean(KEY_ESP32_LINK, preferences.esp32LinkActive)
            putBoolean(KEY_HIGH_PRECISION_AI, preferences.highPrecisionAi)
            putBoolean(KEY_TSR_INTERLOCK, preferences.tsrInterlockEnabled)
            apply()
        }
    }

    suspend fun pullUserSettings(onSuccess: (AppPreferences) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/settings.json$authParam"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val code = connection.responseCode
                if (code !in 200..299) return@withContext
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                if (body.isBlank() || body == "null") return@withContext

                val json = JSONObject(body)
                val local = loadAppPreferences()
                val remote = local.copy(
                    language = runCatching {
                        AppLanguage.valueOf(
                            json.optString("language", local.language.name).uppercase()
                        )
                    }.getOrDefault(local.language),
                    isDarkMode = json.optBoolean("darkMode", local.isDarkMode),
                    passcodeEnabled = json.optBoolean("passcodeEnabled", local.passcodeEnabled),
                    isBiometricEnabled = json.optBoolean("biometricEnabled", local.isBiometricEnabled),
                    isMetric = json.optBoolean("metricUnits", local.isMetric),
                    autoSync = json.optBoolean("autoSync", local.autoSync),
                    autoSyncRtdb = json.optBoolean("autoSyncRtdb", local.autoSyncRtdb),
                    esp32LinkActive = json.optBoolean("esp32LinkActive", local.esp32LinkActive),
                    highPrecisionAi = json.optBoolean("highPrecisionAi", local.highPrecisionAi),
                    tsrInterlockEnabled = json.optBoolean("tsrInterlockEnabled", local.tsrInterlockEnabled)
                )
                withContext(Dispatchers.Main) {
                    saveAppPreferences(remote)
                    onSuccess(remote)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Settings pull error: ${e.message}")
            }
        }
    }

    // ==========================================
    // 1. FIREBASE AUTHENTICATION (Real REST API)
    // ==========================================

    suspend fun signInWithEmailAndPassword(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val apiKey = getEffectiveApiKey()
        webApiKey = apiKey

        authState = AuthState.Authenticating
        syncStatusMessage = "Authenticating with secure cloud identity service..."

        withContext(Dispatchers.IO) {
            try {
                val endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
                val jsonPayload = JSONObject().apply {
                    put("email", email.trim())
                    put("password", pass)
                    put("returnSecureToken", true)
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseStr = reader.readText()
                    reader.close()

                    val json = JSONObject(responseStr)
                    val user = FirebaseUser(
                        localId = json.getString("localId"),
                        email = json.optString("email", email),
                        displayName = json.optString("displayName", email.substringBefore("@")),
                        idToken = json.optString("idToken", ""),
                        refreshToken = json.optString("refreshToken", ""),
                        expiresIn = json.optString("expiresIn", "3600"),
                        tokenExpiresAt = System.currentTimeMillis() + json.optLong("expiresIn", 3600L) * 1000L
                    )

                    // Background sync of inspector profile and audit log on service scope (independent of composable lifecycle)
                    this@RailGuardFirebaseService.scope.launch {
                        try {
                            saveInspectorProfileToFirebase(user)
                            logSafetyAuditEvent("USER_SIGN_IN", "Inspector ${user.email} signed in successfully")
                        } catch (e: Exception) {
                            Log.w("RailGuardFirebase", "Failed to sync profile on login: ${e.message}")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        currentUser = user
                        authState = AuthState.Authenticated(user)
                        persistUser(user)
                        isConnectedToFirebase = true
                        syncStatusMessage = "Successfully authenticated: ${user.email}"
                        onSuccess(user)
                    }
                } else {
                    val errorStream = conn.errorStream
                    val errStr = if (errorStream != null) {
                        val errReader = BufferedReader(InputStreamReader(errorStream))
                        val text = errReader.readText()
                        errReader.close()
                        text
                    } else ""

                    val errorMsg = if (errStr.isNotBlank()) {
                        parseFirebaseError(errStr, responseCode)
                    } else {
                        "Firebase Auth error: HTTP $responseCode"
                    }

                    withContext(Dispatchers.Main) {
                        authState = AuthState.Error(errorMsg)
                        syncStatusMessage = errorMsg
                        onError(errorMsg)
                    }
                }
            } catch (e: CancellationException) {
                // Do not catch or log cancellation exceptions as errors
                throw e
            } catch (e: Exception) {
                val errorMsg = "Network connection failed: ${e.localizedMessage ?: e.message}"
                Log.e("RailGuardFirebase", "Sign in error", e)
                withContext(Dispatchers.Main) {
                    authState = AuthState.Error(errorMsg)
                    syncStatusMessage = errorMsg
                    onError(errorMsg)
                }
            }
        }
    }

    suspend fun signUpWithEmailAndPassword(
        name: String,
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val apiKey = getEffectiveApiKey()
        webApiKey = apiKey

        authState = AuthState.Authenticating
        syncStatusMessage = "Creating user account in safety cloud..."

        withContext(Dispatchers.IO) {
            try {
                val endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey"
                val jsonPayload = JSONObject().apply {
                    put("email", email.trim())
                    put("password", pass)
                    put("returnSecureToken", true)
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseStr = reader.readText()
                    reader.close()

                    val json = JSONObject(responseStr)
                    val idToken = json.optString("idToken", "")
                    val localId = json.getString("localId")

                    // Update displayName in Firebase
                    if (name.isNotBlank() && idToken.isNotBlank()) {
                        updateProfileDisplayName(idToken, name)
                    }

                    val user = FirebaseUser(
                        localId = localId,
                        email = json.optString("email", email),
                        displayName = name.ifBlank { email.substringBefore("@") },
                        idToken = idToken,
                        refreshToken = json.optString("refreshToken", ""),
                        expiresIn = json.optString("expiresIn", "3600"),
                        tokenExpiresAt = System.currentTimeMillis() + json.optLong("expiresIn", 3600L) * 1000L
                    )

                    // Background sync of inspector profile and audit log on service scope (independent of composable lifecycle)
                    this@RailGuardFirebaseService.scope.launch {
                        try {
                            saveInspectorProfileToFirebase(user)
                            logSafetyAuditEvent("USER_REGISTER", "New inspector registered: ${user.email} ($name)")
                        } catch (e: Exception) {
                            Log.w("RailGuardFirebase", "Failed to sync profile on register: ${e.message}")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        currentUser = user
                        authState = AuthState.Authenticated(user)
                        persistUser(user)
                        isConnectedToFirebase = true
                        syncStatusMessage = "Firebase account registered: ${user.email}"
                        onSuccess(user)
                    }
                } else {
                    val errorStream = conn.errorStream
                    val errorMsg = if (errorStream != null) {
                        val errReader = BufferedReader(InputStreamReader(errorStream))
                        val errStr = errReader.readText()
                        errReader.close()
                        parseFirebaseError(errStr, responseCode)
                    } else {
                        "Firebase Registration error: HTTP $responseCode"
                    }

                    withContext(Dispatchers.Main) {
                        authState = AuthState.Error(errorMsg)
                        syncStatusMessage = errorMsg
                        onError(errorMsg)
                    }
                }
            } catch (e: CancellationException) {
                // Do not catch or log cancellation exceptions as errors
                throw e
            } catch (e: Exception) {
                val errorMsg = "Registration failed: ${e.localizedMessage ?: e.message}"
                Log.e("RailGuardFirebase", "Sign up error", e)
                withContext(Dispatchers.Main) {
                    authState = AuthState.Error(errorMsg)
                    syncStatusMessage = errorMsg
                    onError(errorMsg)
                }
            }
        }
    }

    suspend fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val apiKey = getEffectiveApiKey()
        withContext(Dispatchers.IO) {
            try {
                val endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=$apiKey"
                val payload = JSONObject().apply {
                    put("requestType", "PASSWORD_RESET")
                    put("email", email.trim())
                }
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code in 200..299) {
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val message = parseFirebaseError(body, code)
                    withContext(Dispatchers.Main) {
                        onError(message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Unable to send the password reset email.")
                }
            }
        }
    }

    private fun updateProfileDisplayName(idToken: String, displayName: String) {
        try {
            val apiKey = getEffectiveApiKey()
            val endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=$apiKey"
            val payload = JSONObject().apply {
                put("idToken", idToken)
                put("displayName", displayName)
                put("returnSecureToken", false)
            }
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            conn.responseCode
        } catch (e: Exception) {
            Log.w("RailGuardFirebase", "Could not update display name: ${e.message}")
        }
    }

    suspend fun updateInspectorProfile(name: String, email: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val user = currentUser ?: throw IllegalStateException("Sign in is required to update your profile.")
                val token = getValidIdToken()
                val endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=${getEffectiveApiKey()}"
                val payload = JSONObject().apply {
                    put("idToken", token)
                    put("displayName", name.trim())
                    if (email.trim().isNotBlank() && email.trim() != user.email) {
                        put("email", email.trim())
                    }
                    put("returnSecureToken", true)
                }
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    throw IllegalStateException(parseFirebaseError(body, code))
                }

                val json = JSONObject(body)
                val updated = user.copy(
                    displayName = json.optString("displayName", name.trim()),
                    email = json.optString("email", user.email),
                    idToken = json.optString("idToken", token),
                    refreshToken = json.optString("refreshToken", user.refreshToken),
                    expiresIn = json.optString("expiresIn", user.expiresIn),
                    tokenExpiresAt = System.currentTimeMillis() + json.optLong("expiresIn", 3600L) * 1000L
                )
                withContext(Dispatchers.Main) {
                    currentUser = updated
                    authState = AuthState.Authenticated(updated)
                    persistUser(updated)
                }
                saveInspectorProfileToFirebase(updated)
                saveUserSettings(
                    mapOf(
                        "fullName" to name.trim(),
                        "email" to email.trim(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Profile update error: ${e.message}")
                false
            }
        }
    }

    suspend fun saveInspectorProfileToFirebase(user: FirebaseUser, role: String = "Field Track Inspector") {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/inspector_profile.json$authParam"
                val json = JSONObject().apply {
                    put("id", user.localId)
                    put("name", user.displayName)
                    put("email", user.email)
                    put("role", role)
                    put("registeredAt", System.currentTimeMillis())
                    put("lastActiveAt", System.currentTimeMillis())
                    put("status", "ACTIVE")
                }
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Could not sync inspector profile to RTDB: ${e.message}")
            }
        }
    }

    suspend fun logSafetyAuditEvent(action: String, details: String) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val logId = "log_${System.currentTimeMillis()}"
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/audit_logs/$logId.json$authParam"
                val json = JSONObject().apply {
                    put("id", logId)
                    put("action", action)
                    put("details", details)
                    put("timestamp", System.currentTimeMillis())
                    put("actorEmail", currentUser?.email ?: "inspector@railguard.io")
                    put("actorName", currentUser?.displayName ?: "Field Inspector")
                }
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Could not write audit log: ${e.message}")
            }
        }
    }

    suspend fun logSafetyAuditEvent(action: String, details: Map<String, Any?>) {
        val detailsStr = JSONObject(details).toString()
        logSafetyAuditEvent(action, detailsStr)
    }

    suspend fun uploadInspectionToFirebase(inspection: InspectionRecord): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/inspections/${inspection.id}.json$authParam"
                val json = JSONObject().apply {
                    put("id", inspection.id)
                    put("section", inspection.section)
                    put("date", inspection.date)
                    put("inspector", inspection.inspector)
                    put("status", inspection.status)
                    put("framesCount", inspection.framesCount)
                    put("detectionsCount", inspection.detectionsCount)
                    put("detectedCrackTitle", inspection.detectedCrackTitle)
                    put("recommendedMaintenanceAction", inspection.recommendedMaintenanceAction)
                    put("syncedAt", System.currentTimeMillis())
                }
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val code = conn.responseCode
                code in 200..299
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("RailGuardFirebase", "Failed to upload inspection: ${e.message}")
                false
            }
        }
    }

    fun signOut() {
        currentUser = null
        authState = AuthState.Unauthenticated
        persistUser(null)
        isConnectedToFirebase = false
        syncStatusMessage = "Signed out of safety cloud"
    }

    private fun parseFirebaseError(rawJson: String, defaultCode: Int): String {
        return try {
            val json = JSONObject(rawJson)
            val errObj = json.optJSONObject("error")
            val message = errObj?.optString("message", "") ?: ""
            when {
                message.contains("EMAIL_NOT_FOUND") -> "No user found with this email address."
                message.contains("INVALID_PASSWORD") -> "Incorrect password entered."
                message.contains("INVALID_LOGIN_CREDENTIALS") -> "Invalid email or password credentials."
                message.contains("USER_DISABLED") -> "This account has been disabled by the administrator."
                message.contains("EMAIL_EXISTS") -> "This email address is already in use by another account."
                message.contains("OPERATION_NOT_ALLOWED") -> "Password sign-in is disabled in Cloud Console."
                message.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Too many failed attempts. Please try again later."
                message.contains("API_KEY_INVALID") -> "Invalid Web API Key. Check Settings → Cloud Sync."
                message.contains("CONFIGURATION_NOT_FOUND") -> "Cloud project configuration not found."
                message.isNotBlank() -> "Cloud Error: $message"
                else -> "HTTP $defaultCode: Operation failed"
            }
        } catch (e: Exception) {
            "HTTP $defaultCode: $rawJson"
        }
    }

    // ==========================================
    // 2. CONNECTION HEALTHCHECK & TEST (Real Network Call)
    // ==========================================

    suspend fun testFirebaseConnection(onComplete: (ConnectionTestResult) -> Unit) {
        isTestingConnection = true
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                // Ping Realtime Database endpoint root or shallow query
                val authToken = if (currentUser != null) getValidIdToken() else null
                val authParam = authToken?.let { "?auth=${java.net.URLEncoder.encode(it, "UTF-8")}" } ?: ""
                val activeDbUrl = getEffectiveDbUrl()
                val targetUrl = if (databaseType == DatabaseBackendType.REALTIME_DATABASE) {
                    "$activeDbUrl/.json$authParam"
                } else {
                    "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"
                }

                val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    if (databaseType == DatabaseBackendType.FIRESTORE && !authToken.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $authToken")
                    }
                }

                val code = conn.responseCode
                val latency = System.currentTimeMillis() - startTime

                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""

                val result = when (code) {
                    200 -> ConnectionTestResult(
                        isSuccess = true,
                        httpCode = code,
                        latencyMs = latency,
                        message = "Connected to Central Cloud ($latency ms). Database is live and responsive.",
                        rawResponse = responseBody.take(200)
                    )
                    401, 403 -> ConnectionTestResult(
                        isSuccess = true, // Network reached, security rules enforce auth
                        httpCode = code,
                        latencyMs = latency,
                        message = "Connected to Central Cloud ($latency ms). Real-time database is online (Rules require authentication).",
                        rawResponse = responseBody.take(200)
                    )
                    404 -> ConnectionTestResult(
                        isSuccess = false,
                        httpCode = code,
                        latencyMs = latency,
                        message = "Database endpoint not found (HTTP 404). Verify your Database URL.",
                        rawResponse = responseBody.take(200)
                    )
                    else -> ConnectionTestResult(
                        isSuccess = false,
                        httpCode = code,
                        latencyMs = latency,
                        message = "Central Cloud responded with HTTP $code in $latency ms.",
                        rawResponse = responseBody.take(200)
                    )
                }

                withContext(Dispatchers.Main) {
                    isTestingConnection = false
                    lastTestResult = result
                    isConnectedToFirebase = result.isSuccess
                    syncStatusMessage = result.message
                    onComplete(result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                val result = ConnectionTestResult(
                    isSuccess = false,
                    httpCode = -1,
                    latencyMs = latency,
                    message = "Connection failed: ${e.localizedMessage ?: e.message}"
                )
                withContext(Dispatchers.Main) {
                    isTestingConnection = false
                    lastTestResult = result
                    isConnectedToFirebase = false
                    syncStatusMessage = result.message
                    onComplete(result)
                }
            }
        }
    }

    // ==========================================
    // 3. DATABASE SYNC & PERSISTENCE (Real REST API)
    // ==========================================

    /**
     * Pushes current local defects, tasks, and inspections to Firebase Realtime Database
     */
    suspend fun syncAllToFirebase(
        defects: List<Defect>,
        tasks: List<MaintenanceTask>,
        inspections: List<InspectionRecord>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activeDb = getEffectiveDbUrl()
        isSyncing = true
        syncStatusMessage = "Syncing ${defects.size} defects & ${tasks.size} tasks to cloud database..."

        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val dbEndpoint = "$activeDb/${userDataRoot()}.json$authParam"

                val payload = JSONObject().apply {
                    put("syncedAt", System.currentTimeMillis())
                    put("syncedBy", currentUser?.email ?: "field-inspector@railguard.io")
                    put("appVersion", "1.0.0")

                    // Serialize defects
                    val defectsObj = JSONObject()
                    defects.forEach { defect ->
                        defectsObj.put(defect.id, JSONObject().apply {
                            put("id", defect.id)
                            put("title", defect.title)
                            put("section", defect.section)
                            put("score", defect.score)
                            put("tone", defect.tone.name)
                            put("time", defect.time)
                            put("detail", defect.detail)
                            put("estimatedLength", defect.estimatedLength)
                            put("latitude", defect.latitude)
                            put("longitude", defect.longitude)
                            put("riskScore", defect.riskScore)
                            put("chainageCoordinate", defect.chainageCoordinate)
                            put("aiConfidencePercent", defect.aiConfidencePercent)
                            put("aiPrescribedAction", defect.aiPrescribedAction)
                        })
                    }
                    put("defects", defectsObj)

                    // Serialize tasks
                    val tasksObj = JSONObject()
                    tasks.forEach { task ->
                        tasksObj.put(task.id, JSONObject().apply {
                            put("id", task.id)
                            put("title", task.title)
                            put("section", task.section)
                            put("due", task.due)
                            put("tone", task.tone.name)
                            put("assignee", task.assignee)
                            put("status", task.status)
                            put("torque", task.torque)
                        })
                    }
                    put("tasks", tasksObj)

                    // Serialize inspections
                    val inspObj = JSONObject()
                    inspections.forEach { insp ->
                        inspObj.put(insp.id, JSONObject().apply {
                            put("id", insp.id)
                            put("section", insp.section)
                            put("inspector", insp.inspector)
                            put("status", insp.status)
                            put("framesCount", insp.framesCount)
                            put("detectionsCount", insp.detectionsCount)
                            put("detectedCrackTitle", insp.detectedCrackTitle)
                            put("recommendedMaintenanceAction", insp.recommendedMaintenanceAction)
                        })
                    }
                    put("inspections", inspObj)
                }

                val conn = (URL(dbEndpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 12000
                    readTimeout = 12000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

                withContext(Dispatchers.Main) {
                    isSyncing = false
                    lastSyncTimestamp = timestampStr
                    if (code in 200..299) {
                        isConnectedToFirebase = true
                        syncStatusMessage = "Successfully uploaded ${defects.size} defects, ${tasks.size} tasks to cloud database (HTTP $code)"
                        onSuccess()
                    } else {
                        val errMsg = "Cloud sync failed with HTTP $code. Check database security rules."
                        syncStatusMessage = errMsg
                        onError(errMsg)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = "Network error during sync: ${e.localizedMessage ?: e.message}"
                Log.e("RailGuardFirebase", "Sync error", e)
                withContext(Dispatchers.Main) {
                    isSyncing = false
                    syncStatusMessage = errMsg
                    onError(errMsg)
                }
            }
        }
    }

    /**
     * Pulls latest defects, tasks, and inspections from Central Realtime Database
     */
    suspend fun pullAllFromFirebase(
        onSuccess: (List<Defect>, List<MaintenanceTask>, List<InspectionRecord>) -> Unit,
        onError: (String) -> Unit
    ) {
        val activeDb = getEffectiveDbUrl()
        isSyncing = true
        syncStatusMessage = "Fetching data from cloud database..."

        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val dbEndpoint = "$activeDb/${userDataRoot()}.json$authParam"

                val conn = (URL(dbEndpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 12000
                }

                val code = conn.responseCode
                var effectiveContent = ""
                if (code in 200..299) {
                    val stream = conn.inputStream
                    effectiveContent = stream.bufferedReader().use { it.readText() }
                }

                if (effectiveContent.isBlank() || effectiveContent == "null") {
                    // Try global corridor root
                    try {
                        val fallbackConn = (URL("$activeDb/railguard.json$authParam").openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 8000
                            readTimeout = 8000
                        }
                        if (fallbackConn.responseCode in 200..299) {
                            effectiveContent = fallbackConn.inputStream.bufferedReader().use { it.readText() }
                        }
                    } catch (ignored: Exception) {}
                }

                if (effectiveContent.isNotBlank() && effectiveContent != "null") {
                    val content = effectiveContent

                    val json = JSONObject(content)
                    val defectsList = mutableListOf<Defect>()
                    val tasksList = mutableListOf<MaintenanceTask>()
                    val inspectionsList = mutableListOf<InspectionRecord>()

                    // Parse defects
                    val defectsObj = json.optJSONObject("defects")
                    if (defectsObj != null) {
                        val keys = defectsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val item = defectsObj.getJSONObject(key)
                            defectsList.add(
                                Defect(
                                    id = item.optString("id", key),
                                    title = item.optString("title", "Track Defect"),
                                    section = item.optString("section", "Mainline"),
                                    score = item.optString("score", "Warning"),
                                    tone = try { Tone.valueOf(item.optString("tone", "WARNING")) } catch (e: Exception) { Tone.WARNING },
                                    time = item.optString("time", "Just now"),
                                    detail = item.optString("detail", ""),
                                    estimatedLength = item.optString("estimatedLength", "10 mm"),
                                    latitude = item.optDouble("latitude", 28.6142),
                                    longitude = item.optDouble("longitude", 77.2085),
                                    riskScore = item.optInt("riskScore", 70),
                                    chainageCoordinate = item.optString("chainageCoordinate", "KM 42+000"),
                                    aiConfidencePercent = item.optInt("aiConfidencePercent", 90),
                                    aiPrescribedAction = item.optString("aiPrescribedAction", "Inspect track.")
                                )
                            )
                        }
                    }

                    // Parse tasks
                    val tasksObj = json.optJSONObject("tasks")
                    if (tasksObj != null) {
                        val keys = tasksObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val item = tasksObj.getJSONObject(key)
                            tasksList.add(
                                MaintenanceTask(
                                    id = item.optString("id", key),
                                    title = item.optString("title", "Maintenance Task"),
                                    section = item.optString("section", "Mainline"),
                                    due = item.optString("due", "Today"),
                                    tone = try { Tone.valueOf(item.optString("tone", "INFO")) } catch (e: Exception) { Tone.INFO },
                                    assignee = item.optString("assignee", "Field Gang"),
                                    status = item.optString("status", "Pending"),
                                    torque = item.optString("torque", "Nominal")
                                )
                            )
                        }
                    }

                    // Parse inspections
                    val inspObj = json.optJSONObject("inspections")
                    if (inspObj != null) {
                        val keys = inspObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val item = inspObj.getJSONObject(key)
                            inspectionsList.add(
                                InspectionRecord(
                                    id = item.optString("id", key),
                                    section = item.optString("section", "Corridor"),
                                    date = item.optString("date", "Today"),
                                    inspector = item.optString("inspector", "Inspector"),
                                    status = item.optString("status", "Completed"),
                                    framesCount = item.optInt("framesCount", 100),
                                    detectionsCount = item.optInt("detectionsCount", 0),
                                    detectedCrackTitle = item.optString("detectedCrackTitle", "None"),
                                    recommendedMaintenanceAction = item.optString("recommendedMaintenanceAction", "Routine inspection")
                                )
                            )
                        }
                    }

                    val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    withContext(Dispatchers.Main) {
                        isSyncing = false
                        lastSyncTimestamp = timestampStr
                        isConnectedToFirebase = true
                        syncStatusMessage = "Pulled ${defectsList.size} defects, ${tasksList.size} tasks & ${inspectionsList.size} inspections from cloud database"
                        onSuccess(defectsList, tasksList, inspectionsList)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isSyncing = false
                        syncStatusMessage = "Cloud database is clear. Ready to receive records from Raspberry Pi."
                        onSuccess(emptyList(), emptyList(), emptyList())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = "Network error: ${e.localizedMessage ?: e.message}"
                Log.e("RailGuardFirebase", "Pull error", e)
                withContext(Dispatchers.Main) {
                    isSyncing = false
                    syncStatusMessage = errMsg
                    onError(errMsg)
                }
            }
        }
    }

    /**
     * Uploads a single defect immediately to Firebase
     */
    suspend fun uploadDefectToFirebase(defect: Defect) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/defects/${defect.id}.json$authParam"
                val json = JSONObject().apply {
                    put("id", defect.id)
                    put("title", defect.title)
                    put("section", defect.section)
                    put("score", defect.score)
                    put("tone", defect.tone.name)
                    put("detail", defect.detail)
                    put("estimatedLength", defect.estimatedLength)
                    put("latitude", defect.latitude)
                    put("longitude", defect.longitude)
                    put("riskScore", defect.riskScore)
                    put("chainageCoordinate", defect.chainageCoordinate)
                    put("aiConfidencePercent", defect.aiConfidencePercent)
                    put("aiPrescribedAction", defect.aiPrescribedAction)
                    put("updatedAt", System.currentTimeMillis())
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Defect upload error: ${e.message}")
            }
        }
    }

    /**
     * Deletes a defect from Firebase
     */
    suspend fun deleteDefectFromFirebase(defectId: String) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/defects/$defectId.json$authParam"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Defect delete error: ${e.message}")
            }
        }
    }

    /**
     * Purges all defects, tasks, and inspections from cloud database to ensure clean real telemetry
     */
    suspend fun purgeAllDemoDataFromCloud(onDone: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val activeDb = getEffectiveDbUrl()
                val targets = listOf(
                    "$activeDb/${userDataRoot()}/defects.json$authParam",
                    "$activeDb/${userDataRoot()}/tasks.json$authParam",
                    "$activeDb/${userDataRoot()}/inspections.json$authParam",
                    "$activeDb/railguard/defects.json$authParam",
                    "$activeDb/railguard/tasks.json$authParam",
                    "$activeDb/railguard/inspections.json$authParam"
                )
                for (target in targets) {
                    try {
                        val conn = (URL(target).openConnection() as HttpURLConnection).apply {
                            requestMethod = "DELETE"
                            connectTimeout = 5000
                            readTimeout = 5000
                        }
                        conn.responseCode
                    } catch (ignored: Exception) {}
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Purge error: ${e.message}")
            }
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    /**
     * Uploads a newly updated or created maintenance task to Firebase
     */
    suspend fun uploadTaskToFirebase(task: MaintenanceTask) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/tasks/${task.id}.json$authParam"
                val json = JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("section", task.section)
                    put("due", task.due)
                    put("tone", task.tone.name)
                    put("assignee", task.assignee)
                    put("status", task.status)
                    put("torque", task.torque)
                    put("updatedAt", System.currentTimeMillis())
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Task upload error: ${e.message}")
            }
        }
    }

    /**
     * Dispatches temporary speed restriction (TSR) live to Firebase for train cab signaling
     */
    suspend fun dispatchTsrToFirebase(trainId: String, tsrSpeedKmH: Int, reason: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/trains/$trainId/tsr.json$authParam"
                val json = JSONObject().apply {
                    put("trainId", trainId)
                    put("activeTsrSpeedKmH", tsrSpeedKmH)
                    put("reason", reason)
                    put("issuedAt", System.currentTimeMillis())
                    put("issuedBy", currentUser?.email ?: "ground-safety-controller")
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode in 200..299
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "TSR dispatch error: ${e.message}")
                false
            }
        }
    }

    /**
     * Broadcasts GNSS / GPS telemetry live to Realtime Database
     */
    suspend fun recordGpsTelemetry(
        lat: Double,
        lng: Double,
        speedKmh: Double,
        heading: Double,
        accuracyM: Float,
        satellites: Int,
        chainage: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/telemetry/live_gps.json$authParam"
                val json = JSONObject().apply {
                    put("latitude", lat)
                    put("longitude", lng)
                    put("speedKmh", speedKmh)
                    put("heading", heading)
                    put("accuracyMeters", accuracyM)
                    put("satellitesTracked", satellites)
                    put("chainage", chainage)
                    put("timestamp", System.currentTimeMillis())
                    put("inspector", currentUser?.email ?: "inspector@railguard.io")
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode in 200..299
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "GPS telemetry sync error: ${e.message}")
                false
            }
        }
    }

    /**
     * Uploads camera frame captures and crack detections to the cloud database
     */
    suspend fun uploadCameraCapture(
        captureId: String,
        section: String,
        detectionCount: Int,
        defectDetected: Boolean,
        notes: String,
        evidenceBytes: ByteArray? = null,
        contentType: String = "image/jpeg"
    ): FirebaseUploadResult {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/camera_captures/$captureId.json$authParam"
                val storageResult = evidenceBytes?.let {
                    uploadEvidenceFile(
                        storagePath = "${userDataRoot()}/evidence/$captureId.jpg",
                        bytes = it,
                        contentType = contentType
                    )
                }
                if (storageResult != null && !storageResult.isSuccess) {
                    return@withContext storageResult
                }
                val json = JSONObject().apply {
                    put("id", captureId)
                    put("section", section)
                    put("detectionCount", detectionCount)
                    put("defectDetected", defectDetected)
                    put("notes", notes)
                    put("capturedAt", System.currentTimeMillis())
                    put("inspector", currentUser?.email ?: "field-inspector")
                    put("resolution", "4K-HDR (3840x2160)")
                    put("exposureMode", "Ultra-High Frequency Rail Shutter")
                    if (storageResult != null) {
                        put("storagePath", storageResult.storagePath)
                        put("downloadUrl", storageResult.downloadUrl)
                        put("contentType", contentType)
                        put("evidenceUploaded", true)
                    } else {
                        put("evidenceUploaded", false)
                    }
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    FirebaseUploadResult(
                        isSuccess = true,
                        storagePath = storageResult?.storagePath.orEmpty(),
                        downloadUrl = storageResult?.downloadUrl.orEmpty(),
                        message = "Camera capture metadata and evidence synchronized."
                    )
                } else {
                    FirebaseUploadResult(
                        isSuccess = false,
                        message = "Camera metadata upload failed: HTTP $responseCode"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Camera capture sync error: ${e.message}")
                FirebaseUploadResult(false, message = e.message ?: "Camera upload failed.")
            }
        }
    }

    suspend fun uploadEvidenceFile(
        storagePath: String,
        bytes: ByteArray,
        contentType: String
    ): FirebaseUploadResult {
        return withContext(Dispatchers.IO) {
            try {
                val token = getValidIdToken()
                val encodedBucket = java.net.URLEncoder
                    .encode(DEFAULT_STORAGE_BUCKET, "UTF-8")
                    .replace("+", "%20")
                val encodedPath = java.net.URLEncoder
                    .encode(storagePath, "UTF-8")
                    .replace("+", "%20")
                val endpoint =
                    "https://firebasestorage.googleapis.com/v0/b/$encodedBucket/o" +
                        "?uploadType=media&name=$encodedPath"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", contentType)
                    setRequestProperty("Content-Length", bytes.size.toString())
                }
                connection.outputStream.use { it.write(bytes) }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    return@withContext FirebaseUploadResult(
                        false,
                        message = "Evidence upload failed: HTTP $code"
                    )
                }

                val json = JSONObject(body)
                val objectName = json.optString("name", storagePath)
                val bucket = json.optString("bucket", DEFAULT_STORAGE_BUCKET)
                val encodedObjectName = java.net.URLEncoder
                    .encode(objectName, "UTF-8")
                    .replace("+", "%20")
                val downloadToken = json.optString("downloadTokens").substringBefore(",").trim()
                val tokenQuery = if (downloadToken.isNotBlank()) "&token=$downloadToken" else ""
                FirebaseUploadResult(
                    isSuccess = true,
                    storagePath = objectName,
                    downloadUrl = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedObjectName?alt=media$tokenQuery",
                    message = "Evidence file uploaded to Firebase Storage."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Evidence upload error: ${e.message}")
                FirebaseUploadResult(false, message = e.message ?: "Evidence upload failed.")
            }
        }
    }

    suspend fun uploadReportPdf(
        reportId: String,
        title: String,
        pdfBytes: ByteArray
    ): FirebaseUploadResult {
        return withContext(Dispatchers.IO) {
            val upload = uploadEvidenceFile(
                storagePath = "${userDataRoot()}/reports/$reportId.pdf",
                bytes = pdfBytes,
                contentType = "application/pdf"
            )
            if (!upload.isSuccess) return@withContext upload

            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/reports/$reportId.json$authParam"
                val json = JSONObject().apply {
                    put("id", reportId)
                    put("title", title)
                    put("status", "PDF_UPLOADED")
                    put("inspector", currentUser?.email ?: "field-inspector")
                    put("generatedAt", System.currentTimeMillis())
                    put("storagePath", upload.storagePath)
                    put("downloadUrl", upload.downloadUrl)
                    put("contentType", "application/pdf")
                    put("sizeBytes", pdfBytes.size)
                }
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(connection.outputStream).use { it.write(json.toString()) }
                val code = connection.responseCode
                if (code in 200..299) {
                    upload.copy(message = "PDF uploaded to Firebase Storage and indexed in Realtime Database.")
                } else {
                    FirebaseUploadResult(false, message = "PDF index upload failed: HTTP $code")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "PDF metadata sync error: ${e.message}")
                FirebaseUploadResult(false, message = e.message ?: "PDF metadata sync failed.")
            }
        }
    }

    /**
     * Records AI analytics and model inferences to cloud database
     */
    suspend fun recordAiAnalysis(
        analysisType: String,
        metrics: Map<String, Any>
    ) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val timestamp = System.currentTimeMillis()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/ai_analyses/$analysisType/$timestamp.json$authParam"
                val json = JSONObject().apply {
                    put("type", analysisType)
                    put("timestamp", timestamp)
                    put("operator", currentUser?.email ?: "ai-operator")
                    metrics.forEach { (k, v) -> put(k, v) }
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "AI analysis sync error: ${e.message}")
            }
        }
    }

    /**
     * Persists user and app configuration settings to cloud database
     */
    suspend fun saveUserSettings(settingsMap: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/settings.json$authParam"
                val json = JSONObject().apply {
                    put("updatedAt", System.currentTimeMillis())
                    put("userEmail", currentUser?.email ?: "inspector@railguard.io")
                    settingsMap.forEach { (k, v) -> put(k, v) }
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Settings sync error: ${e.message}")
            }
        }
    }

    /**
     * Persists inspector field notes & comments to cloud database
     */
    suspend fun saveComment(defectId: String, author: String, text: String) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val commentId = "cmt_${System.currentTimeMillis()}"
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/comments/$defectId/$commentId.json$authParam"
                val json = JSONObject().apply {
                    put("id", commentId)
                    put("defectId", defectId)
                    put("author", author)
                    put("text", text)
                    put("timestamp", System.currentTimeMillis())
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Comment sync error: ${e.message}")
            }
        }
    }

    /**
     * Uploads track observation item to cloud database
     */
    suspend fun saveObservation(obs: ObservationItem) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/observations/${obs.id}.json$authParam"
                val json = JSONObject().apply {
                    put("id", obs.id)
                    put("title", obs.title)
                    put("chainage", obs.chainage)
                    put("time", obs.time)
                    put("severity", obs.severity)
                    put("tone", obs.tone.name)
                    put("syncedAt", System.currentTimeMillis())
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Observation sync error: ${e.message}")
            }
        }
    }

    /**
     * Uploads AI Oracle reasoning and query log to cloud database
     */
    suspend fun saveAiOracleQuery(model: String, query: String, response: String) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val queryId = "ai_${System.currentTimeMillis()}"
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/ai_oracle_logs/$queryId.json$authParam"
                val json = JSONObject().apply {
                    put("id", queryId)
                    put("model", model)
                    put("query", query)
                    put("response", response)
                    put("timestamp", System.currentTimeMillis())
                    put("user", currentUser?.email ?: "rail-engineer")
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "AI Oracle log error: ${e.message}")
            }
        }
    }

    /**
     * Uploads formal safety report package to cloud database
     */
    suspend fun saveReportPackage(reportId: String, title: String, section: String, inspector: String, status: String) {
        withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/reports/$reportId.json$authParam"
                val json = JSONObject().apply {
                    put("id", reportId)
                    put("title", title)
                    put("section", section)
                    put("inspector", inspector)
                    put("status", status)
                    put("generatedAt", System.currentTimeMillis())
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "Report package sync error: ${e.message}")
            }
        }
    }

    suspend fun saveReportPackage(title: String, hash: String, details: Map<String, Any?>) {
        val repId = "REP-${System.currentTimeMillis() % 100000}"
        val userDisplay = currentUser?.displayName ?: "E. Chen (Senior Permanent Way)"
        saveReportPackage(
            reportId = repId,
            title = title,
            section = "Section 14 North Loop",
            inspector = userDisplay,
            status = "Signed & Sealed (Hash: ${hash.take(8)}...)"
        )
    }

    /**
     * Uploads live ESP32 hardware & sensor telemetry to cloud database
     */
    suspend fun uploadEspSensorData(
        nodeId: String,
        ultrasonicDepthMm: Float,
        vibrationG: Float,
        railTempC: Float,
        axleSpeedKmh: Float,
        chainage: String,
        status: String = "ACTIVE_TRACK_SCAN"
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/esp_sensors/$nodeId.json$authParam"
                val liveEndpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/live_sensors/telemetry.json$authParam"
                val json = JSONObject().apply {
                    put("nodeId", nodeId)
                    put("ultrasonicDepthMm", ultrasonicDepthMm.toDouble())
                    put("vibrationG", vibrationG.toDouble())
                    put("railTempC", railTempC.toDouble())
                    put("axleSpeedKmh", axleSpeedKmh.toDouble())
                    put("chainage", chainage)
                    put("status", status)
                    put("hardware", "ESP32-WROOM-32 / Piezo UT / ADXL345 / PT100")
                    put("timestamp", System.currentTimeMillis())
                }

                // Update node endpoint
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val code = conn.responseCode

                // Update live sensor stream endpoint
                val connLive = (URL(liveEndpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(connLive.outputStream).use { it.write(json.toString()) }
                connLive.responseCode

                code in 200..299
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "ESP sensor sync error: ${e.message}")
                false
            }
        }
    }

    /**
     * Transmits zero-calibration command to ESP32 node via cloud RTDB
     */
    suspend fun sendEspCalibrationCommand(nodeId: String, command: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/esp_sensors/$nodeId/command.json$authParam"
                val json = JSONObject().apply {
                    put("command", command)
                    put("issuedAt", System.currentTimeMillis())
                    put("operator", currentUser?.displayName ?: "E. Chen")
                }
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode in 200..299
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "ESP calibration error: ${e.message}")
                false
            }
        }
    }

    data class LiveSensorTelemetry(
        val nodeId: String = "RPI-TRACK-01",
        val ultrasonicDepthMm: Float = 0.0f,
        val vibrationG: Float = 0.0f,
        val railTempC: Float = 0.0f,
        val axleSpeedKmh: Float = 0.0f,
        val chainage: String = "--",
        val hardware: String = "Raspberry Pi 4 / ESP32 Sensor Node",
        val status: String = "IDLE",
        val timestamp: Long = 0L
    )

    var latestSensorTelemetry by mutableStateOf<LiveSensorTelemetry?>(null)

    /**
     * Reads real-time hardware telemetry pushed by Raspberry Pi or ESP32 nodes
     */
    suspend fun fetchLatestSensorTelemetry(): LiveSensorTelemetry? {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoints = listOf(
                    "${getEffectiveDbUrl()}/${userDataRoot()}/live_sensors/telemetry.json$authParam",
                    "${getEffectiveDbUrl()}/railguard/live_sensors/telemetry.json$authParam",
                    "${getEffectiveDbUrl()}/railguard/rpi_telemetry.json$authParam",
                    "${getEffectiveDbUrl()}/railguard/esp_sensors/telemetry.json$authParam"
                )

                for (liveEndpoint in endpoints) {
                    try {
                        val conn = (URL(liveEndpoint).openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 4000
                            readTimeout = 4000
                        }
                        if (conn.responseCode in 200..299) {
                            val body = conn.inputStream.bufferedReader().use { it.readText() }
                            if (body.isNotBlank() && body != "null") {
                                val json = JSONObject(body)
                                val telemetry = LiveSensorTelemetry(
                                    nodeId = json.optString("nodeId", json.optString("device_id", "RPI-TRACK-01")),
                                    ultrasonicDepthMm = (json.optDouble("ultrasonicDepthMm", json.optDouble("ultrasonic_mm", 0.0))).toFloat(),
                                    vibrationG = (json.optDouble("vibrationG", json.optDouble("vibration_g", 0.0))).toFloat(),
                                    railTempC = (json.optDouble("railTempC", json.optDouble("temp_c", 0.0))).toFloat(),
                                    axleSpeedKmh = (json.optDouble("axleSpeedKmh", json.optDouble("speed_kmh", 0.0))).toFloat(),
                                    chainage = json.optString("chainage", "--"),
                                    hardware = json.optString("hardware", "Raspberry Pi 4 / ESP32"),
                                    status = json.optString("status", "ACTIVE_SYNC"),
                                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                                )
                                withContext(Dispatchers.Main) {
                                    latestSensorTelemetry = telemetry
                                }
                                return@withContext telemetry
                            }
                        }
                    } catch (ignored: Exception) {
                        // try next endpoint
                    }
                }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Streams live camera inspection frame & detection telemetry to cloud
     */
    suspend fun uploadLiveInspectionStream(
        sessionId: String,
        fps: Int,
        defectCount: Int,
        currentChainage: String,
        alertActive: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/live_inspection/$sessionId.json$authParam"
                val json = JSONObject().apply {
                    put("sessionId", sessionId)
                    put("fps", fps)
                    put("defectCount", defectCount)
                    put("chainage", currentChainage)
                    put("alertActive", alertActive)
                    put("timestamp", System.currentTimeMillis())
                }
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 4000
                    readTimeout = 4000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode in 200..299
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Records AI physics evaluations (Paris law, Nadal derailment, Thermal SFT, Multi-Tensor)
     */
    suspend fun recordAiEvaluation(
        evalId: String,
        type: String,
        resultSummary: String,
        confidence: Float,
        telemetryInputs: Map<String, Any?>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/ai_evaluations/$evalId.json$authParam"
                val json = JSONObject().apply {
                    put("evalId", evalId)
                    put("type", type)
                    put("summary", resultSummary)
                    put("confidence", confidence.toDouble())
                    put("inputs", JSONObject(telemetryInputs))
                    put("evaluatedAt", System.currentTimeMillis())
                    put("evaluator", currentUser?.displayName ?: "RailVision-DeepTrack Neural Core")
                }
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode in 200..299
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("RailGuardFirebase", "AI evaluation sync error: ${e.message}")
                false
            }
        }
    }

    /**
     * Syncs train fleet status and cab speed interlocks to cloud database
     */
    suspend fun saveTrainTelemetry(
        trainId: String,
        name: String,
        speed: Float,
        tsr: Int?,
        section: String,
        chainage: String,
        status: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authParam = authenticatedQueryParam()
                val endpoint = "${getEffectiveDbUrl()}/${userDataRoot()}/trains/$trainId.json$authParam"
                val json = JSONObject().apply {
                    put("trainId", trainId)
                    put("name", name)
                    put("speed", speed.toDouble())
                    if (tsr != null) put("activeTsr", tsr) else put("activeTsr", JSONObject.NULL)
                    put("section", section)
                    put("chainage", chainage)
                    put("status", status)
                    put("lastPing", System.currentTimeMillis())
                }
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode in 200..299
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }
    }
}
