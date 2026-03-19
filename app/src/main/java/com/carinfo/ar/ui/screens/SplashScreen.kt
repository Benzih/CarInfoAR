package com.carinfo.ar.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carinfo.ar.ui.theme.BrandPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val iconScale = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        delay(100)
        titleAlpha.animateTo(1f, animationSpec = tween(500))
        delay(100)
        subtitleAlpha.animateTo(1f, animationSpec = tween(500))
        delay(800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        // Scan line effect behind everything
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * scanLineY
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, BrandPrimary.copy(alpha = 0.3f), Color.Transparent)
                ),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 3f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Car icon with scale animation
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier
                    .size(80.dp)
                    .scale(iconScale.value)
            )

            Spacer(Modifier.height(24.dp))

            // App name
            Text(
                text = "CarInfo AR",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Scan. Identify. Discover.",
                style = MaterialTheme.typography.bodyLarge,
                color = BrandPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}
