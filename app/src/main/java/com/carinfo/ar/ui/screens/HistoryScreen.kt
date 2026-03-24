package com.carinfo.ar.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carinfo.ar.R
import com.carinfo.ar.data.ScanHistory
import com.carinfo.ar.data.ScanRecord
import com.carinfo.ar.data.model.VehicleInfo
import com.carinfo.ar.ui.theme.BrandPrimary
import com.carinfo.ar.ui.theme.GlassBorder
import com.carinfo.ar.ui.theme.GlassOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(ScanHistory.load(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.history_title), color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.history_back), tint = Color.White)
                }
            },
            actions = {
                if (records.isNotEmpty()) {
                    IconButton(onClick = {
                        ScanHistory.clear(context)
                        records = emptyList()
                    }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.history_clear_all), tint = Color(0xFFFF6B6B))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
        )

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.history_empty), color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(records) { record ->
                    HistoryItem(record = record, onClick = {
                        val info = VehicleInfo(
                            manufacturer = record.manufacturer,
                            model = record.model,
                            year = record.year,
                            color = record.color,
                            fuelType = record.fuelType,
                            country = record.country
                        )
                        val url = ScanHistory.buildSearchUrl(info)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryItem(record: ScanRecord, onClick: () -> Unit) {
    val countryFlag = when (record.country) {
        "IL" -> "\uD83C\uDDEE\uD83C\uDDF1"
        "NL" -> "\uD83C\uDDF3\uD83C\uDDF1"
        "GB" -> "\uD83C\uDDEC\uD83C\uDDE7"
        else -> ""
    }
    val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(record.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassOverlay)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(countryFlag, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    record.plateNumber,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    record.manufacturer?.let { append(it) }
                    record.model?.let {
                        if (isNotEmpty()) append(" ")
                        append(it)
                    }
                    record.year?.let {
                        if (isNotEmpty()) append(" \u2022 ")
                        append(it)
                    }
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            if (record.color != null || record.fuelType != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        record.color?.let { append(it) }
                        record.fuelType?.let {
                            if (isNotEmpty()) append(" \u2022 ")
                            append(it)
                        }
                    },
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(dateStr, color = Color(0xFF666666), fontSize = 11.sp)
        }
        Icon(Icons.Default.OpenInNew, stringResource(R.string.overlay_info), tint = Color(0xFF666666), modifier = Modifier.size(20.dp))
    }
}
