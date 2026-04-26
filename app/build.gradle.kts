plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
}

android {
    namespace = "com.carinfo.ar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.carinfo.ar"
        minSdk = 24
        targetSdk = 35
        versionCode = 22
        versionName = "1.2.8"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getProperty("user.home") + "/.keystores/carinfo-release.keystore")
            storePassword = "CarInfo2026!"
            keyAlias = "carinfo"
            keyPassword = "CarInfo2026!"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    implementation(libs.mlkit.text.recognition)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    implementation(libs.navigation.compose)
    implementation(libs.splashscreen)
    implementation(libs.datastore.preferences)
    implementation(libs.play.services.ads)
    implementation(libs.billing)
    implementation(libs.exifinterface)
    implementation("com.google.guava:guava:32.1.3-android")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
