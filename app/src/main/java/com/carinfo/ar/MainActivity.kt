package com.carinfo.ar

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.carinfo.ar.ads.AdManager
import com.carinfo.ar.data.UserPreferences
import com.carinfo.ar.navigation.AppNavigation
import com.carinfo.ar.ui.theme.CarInfoTheme
import com.carinfo.ar.util.SoundManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Apply language override only if user manually set one
        val langCode = runBlocking {
            UserPreferences.getAppLanguage(newBase).first()
        }
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
        SoundManager.init(this)
        AdManager.initialize(this)

        setContent {
            CarInfoTheme {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}
