package com.carinfo.ar

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.carinfo.ar.ads.AdManager
import com.carinfo.ar.ads.BillingManager
import com.carinfo.ar.navigation.AppNavigation
import com.carinfo.ar.ui.theme.CarInfoTheme
import com.carinfo.ar.util.SoundManager
import com.google.android.gms.security.ProviderInstaller
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Read language from SharedPreferences (fast, no blocking I/O)
        // DataStore is too slow for attachBaseContext — risk of ANR
        val prefs = newBase.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_language", "") ?: ""

        if (langCode.isNotEmpty()) {
            val locale = Locale(langCode)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Update SSL provider for older Android versions (API < 29)
        // This fixes "Trust anchor not found" errors on Android 8.x
        try { ProviderInstaller.installIfNeeded(this) } catch (_: Exception) {}

        SoundManager.init(this)
        AdManager.initialize(this)
        BillingManager.initialize(this)

        setContent {
            CarInfoTheme {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
        BillingManager.disconnect()
    }
}
