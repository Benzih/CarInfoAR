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
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.carinfo.ar.R
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
    val appLanguage by UserPreferences.getAppLanguage(context).collectAsState(initial = "")

    val country = selectedCountryCode?.let { SupportedCountry.fromCode(it) } ?: SupportedCountry.fromLocale()
    val countryFlag = when (country) {
        SupportedCountry.ISRAEL -> "\uD83C\uDDEE\uD83C\uDDF1 ${stringResource(R.string.onboarding_country_israel)}"
        SupportedCountry.NETHERLANDS -> "\uD83C\uDDF3\uD83C\uDDF1 ${stringResource(R.string.onboarding_country_netherlands)}"
        SupportedCountry.UK -> "\uD83C\uDDEC\uD83C\uDDE7 ${stringResource(R.string.onboarding_country_uk)}"
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
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.history_back),
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
            // Language section
            SectionTitle(stringResource(R.string.settings_language))
            val languages = listOf(
                "" to stringResource(R.string.settings_language_auto),
                "iw" to stringResource(R.string.settings_language_hebrew),
                "en" to stringResource(R.string.settings_language_english),
                "nl" to stringResource(R.string.settings_language_dutch),
                "fr" to stringResource(R.string.settings_language_french),
                "de" to stringResource(R.string.settings_language_german),
                "es" to stringResource(R.string.settings_language_spanish),
                "it" to stringResource(R.string.settings_language_italian),
                "pt" to stringResource(R.string.settings_language_portuguese),
                "ar" to stringResource(R.string.settings_language_arabic),
                "tr" to stringResource(R.string.settings_language_turkish),
                "ru" to stringResource(R.string.settings_language_russian),
                "zh" to stringResource(R.string.settings_language_chinese),
                "ja" to stringResource(R.string.settings_language_japanese),
                "ko" to stringResource(R.string.settings_language_korean)
            )
            var langExpanded by remember { mutableStateOf(false) }
            val currentLangName = languages.firstOrNull { it.first == appLanguage }?.second
                ?: stringResource(R.string.settings_language_auto)
            SettingsCard {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { langExpanded = !langExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_language),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                currentLangName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF888888)
                            )
                        }
                        Text(
                            if (langExpanded) "▲" else "▼",
                            color = Color(0xFF555555),
                            fontSize = 14.sp
                        )
                    }
                    if (langExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            languages.forEach { (code, name) ->
                                val isSelected = code == appLanguage
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BrandPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            langExpanded = false
                                            scope.launch {
                                                UserPreferences.setAppLanguage(context, code)
                                                (context as? android.app.Activity)?.recreate()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        name,
                                        color = if (isSelected) BrandPrimary else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Text("✓", color = BrandPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Sound section
            SectionTitle(stringResource(R.string.settings_sound_feedback))
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
                        stringResource(R.string.settings_sound_effects),
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
            SectionTitle(stringResource(R.string.settings_about))
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.settings_version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF888888)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Country/Region section (last — most users won't need this)
            SectionTitle(stringResource(R.string.settings_region))
            SettingsCard {
                val countries = SupportedCountry.entries
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val currentIndex = countries.indexOf(country)
                                val nextIndex = (currentIndex + 1) % countries.size
                                scope.launch {
                                    UserPreferences.setSelectedCountry(context, countries[nextIndex].code)
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_country),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                countryFlag,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF888888)
                            )
                        }
                        Text(stringResource(R.string.settings_tap_to_change), color = Color(0xFF555555), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_country_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF6B6B).copy(alpha = 0.7f)
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
