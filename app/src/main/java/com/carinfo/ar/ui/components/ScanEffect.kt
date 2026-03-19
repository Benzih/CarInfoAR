package com.carinfo.ar.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.carinfo.ar.ui.theme.BrandPrimary

@Composable
fun ScanEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanY"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val y = size.height * scanY
        val glowHeight = 60f

        // Glow area
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    BrandPrimary.copy(alpha = 0.08f),
                    BrandPrimary.copy(alpha = 0.15f),
                    BrandPrimary.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = y - glowHeight,
                endY = y + glowHeight
            )
        )

        // Main scan line
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    BrandPrimary.copy(alpha = 0.6f),
                    BrandPrimary,
                    BrandPrimary.copy(alpha = 0.6f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2f
        )
    }
}
