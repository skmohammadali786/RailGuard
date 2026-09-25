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

## Local build

This repository does not include a Gradle wrapper. With an Android SDK
configured, build the debug APK with:

```bash
gradle assembleDebug
```

The imported project currently requires an Android SDK with API 36 installed.
No Replit web workflow is configured because this is an Android-only project.