package com.carinfo.ar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.carinfo.ar.ads.AdManager
import com.carinfo.ar.navigation.AppNavigation
import com.carinfo.ar.ui.theme.CarInfoTheme
import com.carinfo.ar.util.SoundManager

class MainActivity : ComponentActivity() {
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
