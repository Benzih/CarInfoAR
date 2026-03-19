package com.carinfo.ar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandDark,
    secondary = AccentIsrael,
    surface = BrandSurface,
    background = BrandDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = BrandSurfaceLight,
    outline = GlassBorder
)

@Composable
fun CarInfoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CarInfoTypography,
        content = content
    )
}
