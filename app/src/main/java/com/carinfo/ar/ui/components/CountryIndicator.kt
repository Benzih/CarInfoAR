package com.carinfo.ar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carinfo.ar.data.SupportedCountry
import com.carinfo.ar.ui.theme.GlassOverlay

@Composable
fun CountryIndicator(
    country: SupportedCountry,
    modifier: Modifier = Modifier
) {
    val (flag, name) = when (country) {
        SupportedCountry.ISRAEL -> "\uD83C\uDDEE\uD83C\uDDF1" to "Israel"
        SupportedCountry.NETHERLANDS -> "\uD83C\uDDF3\uD83C\uDDF1" to "Netherlands"
        SupportedCountry.UK -> "\uD83C\uDDEC\uD83C\uDDE7" to "UK"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlassOverlay)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = flag, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}
