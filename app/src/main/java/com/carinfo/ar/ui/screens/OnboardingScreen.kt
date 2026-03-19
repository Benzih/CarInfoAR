package com.carinfo.ar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carinfo.ar.data.SupportedCountry
import com.carinfo.ar.data.UserPreferences
import com.carinfo.ar.ui.theme.AccentIsrael
import com.carinfo.ar.ui.theme.AccentNetherlands
import com.carinfo.ar.ui.theme.AccentUK
import com.carinfo.ar.ui.theme.BrandPrimary
import com.carinfo.ar.ui.theme.BrandSurface
import com.carinfo.ar.ui.theme.GlassBorder
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }
    var selectedCountry by remember { mutableStateOf(SupportedCountry.fromLocale()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            when (currentPage) {
                0 -> WelcomePage()
                1 -> CountryPage(
                    selectedCountry = selectedCountry,
                    onCountrySelected = { selectedCountry = it }
                )
                2 -> ReadyPage()
            }

            Spacer(Modifier.weight(1f))

            // Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(3) { index ->
                    val width by animateDpAsState(
                        targetValue = if (index == currentPage) 24.dp else 8.dp,
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPage) BrandPrimary
                                else Color(0xFF333333)
                            )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (currentPage < 2) {
                        currentPage++
                    } else {
                        scope.launch {
                            selectedCountry?.let {
                                UserPreferences.setSelectedCountry(context, it.code)
                            }
                            UserPreferences.setOnboardingComplete(context, true)
                            onComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text(
                    text = if (currentPage < 2) "Next" else "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    OnboardingPage(
        icon = Icons.Default.DirectionsCar,
        title = "Scan Any\nLicense Plate",
        description = "Point your camera at any vehicle and instantly see its details floating in AR"
    )
}

@Composable
private fun CountryPage(
    selectedCountry: SupportedCountry?,
    onCountrySelected: (SupportedCountry) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Select Your\nCountry",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "We'll use the right database for your region",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        CountryOption("IL", "Israel", AccentIsrael, selectedCountry == SupportedCountry.ISRAEL) {
            onCountrySelected(SupportedCountry.ISRAEL)
        }
        Spacer(Modifier.height(12.dp))
        CountryOption("NL", "Netherlands", AccentNetherlands, selectedCountry == SupportedCountry.NETHERLANDS) {
            onCountrySelected(SupportedCountry.NETHERLANDS)
        }
        Spacer(Modifier.height(12.dp))
        CountryOption("GB", "United Kingdom", AccentUK, selectedCountry == SupportedCountry.UK) {
            onCountrySelected(SupportedCountry.UK)
        }
    }
}

@Composable
private fun CountryOption(
    flag: String,
    name: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val flagEmoji = when (flag) {
        "IL" -> "\uD83C\uDDEE\uD83C\uDDF1"
        "NL" -> "\uD83C\uDDF3\uD83C\uDDF1"
        "GB" -> "\uD83C\uDDEC\uD83C\uDDE7"
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else BrandSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else GlassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = flagEmoji, fontSize = 32.sp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (flag == "GB") "Requires API key" else "Free - no key needed",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF888888)
            )
        }
    }
}

@Composable
private fun ReadyPage() {
    OnboardingPage(
        icon = Icons.Default.CameraAlt,
        title = "Ready to\nScan",
        description = "Grant camera access and start discovering vehicles around you"
    )
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )
    }
}
