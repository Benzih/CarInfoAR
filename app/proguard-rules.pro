# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.carinfo.ar.data.model.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson — keep TypeToken for generic deserialization
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all data classes used with Gson (field names must survive for JSON keys)
-keepclassmembers class com.carinfo.ar.data.ScanRecord { *; }
-keepclassmembers class com.carinfo.ar.data.model.VehicleInfo { *; }

# API request/response models
-keep class com.carinfo.ar.data.api.** { *; }
-keep class com.carinfo.ar.data.ScanRecord { *; }

# ML Kit
-dontwarn com.google.mlkit.**

# AdMob
-dontwarn com.google.android.gms.ads.**

# Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
