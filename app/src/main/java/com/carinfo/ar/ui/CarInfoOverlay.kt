package com.carinfo.ar.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carinfo.ar.data.model.VehicleInfo
import com.carinfo.ar.ui.theme.BrandPrimary
import com.carinfo.ar.ui.theme.GlassBorder
import com.carinfo.ar.ui.theme.GlassOverlay

@Composable
fun FloatingCarInfo(
    vehicleInfo: VehicleInfo,
    modifier: Modifier = Modifier
) {
    val countryFlag = when (vehicleInfo.country) {
        "IL" -> "\uD83C\uDDEE\uD83C\uDDF1"
        "NL" -> "\uD83C\uDDF3\uD83C\uDDF1"
        "GB" -> "\uD83C\uDDEC\uD83C\uDDE7"
        else -> ""
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Glass card
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GlassOverlay)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Flag + plate number row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = countryFlag, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = vehicleInfo.manufacturer ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            // Manufacturer + Model - main title
            Text(
                text = buildString {
                    vehicleInfo.manufacturer?.let { append(it) }
                    vehicleInfo.model?.let {
                        if (isNotEmpty()) append(" ")
                        append(it)
                    }
                }.ifEmpty { "Unknown" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Start
            )

            // Year + Color
            val details = buildString {
                vehicleInfo.year?.let { append("$it") }
                vehicleInfo.color?.let {
                    if (isNotEmpty()) append("  \u2022  ")
                    append(it)
                }
                vehicleInfo.fuelType?.let {
                    if (isNotEmpty()) append("  \u2022  ")
                    append(it)
                }
            }
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA)
                )
            }

            // Accent line
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrandPrimary, BrandPrimary.copy(alpha = 0.2f))
                        )
                    )
            )
        }

        // Arrow pointing down
        Canvas(modifier = Modifier.size(width = 14.dp, height = 8.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, GlassOverlay)
        }
    }
}

@Composable
fun LoadingPlateIndicator(
    plateNumber: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Text(
        text = "$plateNumber",
        color = BrandPrimary.copy(alpha = alpha),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GlassOverlay)
            .border(1.dp, BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
