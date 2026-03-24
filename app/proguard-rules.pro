# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.carinfo.ar.data.model.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod

# API request/response models
-keep class com.carinfo.ar.data.api.** { *; }
-keep class com.carinfo.ar.data.ScanRecord { *; }

# ML Kit
-dontwarn com.google.mlkit.**

# AdMob
-dontwarn com.google.android.gms.ads.**

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
