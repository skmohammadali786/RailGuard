---
name: Android build environment
description: Environment constraint for verifying the imported native Android project
---

The imported Android project can use the available Gradle and Java tooling, but
this workspace does not currently expose an Android SDK or sdkmanager. Gradle
therefore stops at SDK discovery before Kotlin compilation.

**Why:** A native Android project is not previewable as a Replit web workflow,
and claiming an APK build passed without the SDK would be misleading.

**How to apply:** Keep the Android/Compose structure intact, document the
required SDK/API level, and treat `gradle assembleDebug` as blocked until an
Android SDK is supplied to the environment.