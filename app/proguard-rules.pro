# Keep Firestore models and serialization classes
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
    @androidx.annotation.Keep <fields>;
}
-keep @androidx.annotation.Keep class * { *; }
