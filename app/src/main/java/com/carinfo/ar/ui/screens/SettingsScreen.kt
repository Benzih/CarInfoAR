package com.carinfo.ar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carinfo.ar.data.SupportedCountry
import com.carinfo.ar.data.UserPreferences
import com.carinfo.ar.ui.theme.BrandDark
import com.carinfo.ar.ui.theme.BrandPrimary
import com.carinfo.ar.ui.theme.BrandSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundEnabled by UserPreferences.isSoundEnabled(context).collectAsState(initial = true)
    val selectedCountryCode by UserPreferences.getSelectedCountry(context).collectAsState(initial = null)

    val country = selectedCountryCode?.let { SupportedCountry.fromCode(it) } ?: SupportedCountry.fromLocale()
    val countryFlag = when (country) {
        SupportedCountry.ISRAEL -> "\uD83C\uDDEE\uD83C\uDDF1 Israel"
        SupportedCountry.NETHERLANDS -> "\uD83C\uDDF3\uD83C\uDDF1 Netherlands"
        SupportedCountry.UK -> "\uD83C\uDDEC\uD83C\uDDE7 United Kingdom"
        null -> "Not set"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BrandDark,
                titleContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Country section
            SectionTitle("Region")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Country",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            countryFlag,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Sound section
            SectionTitle("Sound & Feedback")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = BrandPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Sound Effects",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            scope.launch { UserPreferences.setSoundEnabled(context, it) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BrandPrimary,
                            checkedTrackColor = BrandPrimary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // About section
            SectionTitle("About")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CarInfo AR",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Version 1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF888888)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan license plates and discover vehicle info in augmented reality.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = BrandPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface)
    ) {
        content()
    }
}
