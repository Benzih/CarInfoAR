package com.carinfo.ar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.carinfo.ar.ui.theme.BrandPrimary

@Composable
fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cornerLen = 40f
        val strokeW = 3f
        val color = BrandPrimary.copy(alpha = 0.5f)

        val cx = size.width / 2f
        val cy = size.height / 2f
        val halfW = size.width * 0.35f
        val halfH = size.height * 0.12f

        val left = cx - halfW
        val right = cx + halfW
        val top = cy - halfH
        val bottom = cy + halfH

        // Top-left
        drawLine(color, Offset(left, top), Offset(left + cornerLen, top), strokeW, StrokeCap.Round)
        drawLine(color, Offset(left, top), Offset(left, top + cornerLen), strokeW, StrokeCap.Round)

        // Top-right
        drawLine(color, Offset(right, top), Offset(right - cornerLen, top), strokeW, StrokeCap.Round)
        drawLine(color, Offset(right, top), Offset(right, top + cornerLen), strokeW, StrokeCap.Round)

        // Bottom-left
        drawLine(color, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW, StrokeCap.Round)
        drawLine(color, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW, StrokeCap.Round)

        // Bottom-right
        drawLine(color, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW, StrokeCap.Round)
        drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW, StrokeCap.Round)
    }
}
