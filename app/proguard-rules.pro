# Proguard rules for RailGuard
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
