# RailGuard

RailGuard is a native Android application built with Kotlin and Jetpack Compose.

## Firebase backend

The app uses Firebase REST APIs for email/password authentication and Realtime
Database persistence. The Firebase service is initialized from `MainActivity`
and keeps the ID token and refresh token in Android `SharedPreferences`.

Authenticated records are stored under:

```text
railguard/users/{firebaseUid}/
```

The `database.rules.json` file contains the matching per-user Realtime Database
rules. Deploy those rules with the Firebase CLI from the Firebase project when
you are ready to enforce them in production.

## Cloud data behavior

The Compose app keeps defects, maintenance tasks, inspections, observations,
notifications, settings, evidence metadata, audit events, GPS updates, and
sensor commands under the signed-in user's Firebase UID. Empty collections stay
empty; the app does not seed sample records or read shared fallback paths.
Authenticated data is refreshed periodically while the user is signed in.

The Raspberry Pi gateway publishes its latest normalized packet to:

```text
railguard/users/{firebaseUid}/live_sensors/telemetry
railguard/users/{firebaseUid}/devices/{deviceId}/last_telemetry
```

The app polls the first path and displays the packet on the home, live
inspection, AI request, and hardware telemetry views. The Pi must use the same
Firebase UID as the signed-in inspector and must be configured with a Firebase
Admin service-account file outside this repository. See
`raspberry_pi/README.md` for the required environment variables and packet
contract.

## Local build

This repository does not include a Gradle wrapper. With an Android SDK
configured, build the debug APK with:

```bash
gradle assembleDebug
```

The imported project currently requires an Android SDK with API 36 installed.
No Replit web workflow is configured because this is an Android-only project.