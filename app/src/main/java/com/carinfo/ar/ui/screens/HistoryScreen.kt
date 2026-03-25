package com.carinfo.ar.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carinfo.ar.R
import com.carinfo.ar.data.ScanHistory
import com.carinfo.ar.data.ScanRecord
import com.carinfo.ar.data.model.VehicleInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.HorizontalDivider
import com.carinfo.ar.ui.InfoRow
import com.carinfo.ar.ui.theme.BrandPrimary
import com.carinfo.ar.ui.theme.BrandSurface
import com.carinfo.ar.ui.theme.GlassOverlay
import com.carinfo.ar.util.SoundManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(emptyList<ScanRecord>()) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Reload history every time this screen appears
    LaunchedEffect(Unit) {
        records = ScanHistory.load(context)
    }

    // Confirmation dialog
    if (showClearDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_all), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.history_confirm_clear)) },
            confirmButton = {
                Text(
                    stringResource(R.string.history_confirm_yes),
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        ScanHistory.clear(context)
                        SoundManager.playDeleteAll()
                        records = emptyList()
                        showClearDialog = false
                    }.padding(16.dp)
                )
            },
            dismissButton = {
                Text(
                    stringResource(R.string.history_confirm_no),
                    color = Color.Gray,
                    modifier = Modifier.clickable { showClearDialog = false }.padding(16.dp)
                )
            },
            containerColor = BrandSurface,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFAAAAAA)
        )
    }

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
                    Text(
                        stringResource(R.string.history_clear_all),
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showClearDialog = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
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
                items(records, key = { it.plateNumber + it.timestamp }) { record ->
                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            ScanHistory.delete(context, record.plateNumber)
                            SoundManager.playDelete()
                            records = ScanHistory.load(context)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                            val color by animateColorAsState(
                                if (isSwiping) Color(0xFFFF3B30) else Color.Transparent,
                                label = "bg"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (isSwiping) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.history_delete), tint = Color.White)
                                }
                            }
                        }
                    ) {
                        HistoryItem(
                            record = record,
                            onClick = {
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
                            },
                            onDelete = {
                                ScanHistory.delete(context, record.plateNumber)
                                records = ScanHistory.load(context)
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

private fun isDateExpired(dateStr: String?): Boolean {
    if (dateStr == null) return false
    return try {
        val cleanDate = dateStr.replace("/", "-").take(10)
        val date = LocalDate.parse(cleanDate, DateTimeFormatter.ISO_LOCAL_DATE)
        date.isBefore(LocalDate.now())
    } catch (_: Exception) { false }
}

@Composable
private fun HistoryItem(record: ScanRecord, onClick: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val countryFlag = when (record.country) {
        "IL" -> "\uD83C\uDDEE\uD83C\uDDF1"
        "NL" -> "\uD83C\uDDF3\uD83C\uDDF1"
        "GB" -> "\uD83C\uDDEC\uD83C\uDDE7"
        else -> ""
    }
    val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(record.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassOverlay)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        // Header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(countryFlag, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        buildString {
                            record.manufacturer?.let { append(it) }
                            record.model?.let { if (isNotEmpty()) append(" "); append(it) }
                            record.year?.let { if (isNotEmpty()) append(" \u2022 "); append(it) }
                        },
                        color = BrandPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
                if (record.color != null || record.fuelType != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            buildString {
                                record.color?.let { append(it) }
                                record.fuelType?.let { if (isNotEmpty()) append(" \u2022 "); append(it) }
                            },
                            color = Color(0xFFAAAAAA), fontSize = 12.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.overlay_info),
                            color = BrandPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandPrimary.copy(alpha = 0.15f))
                                .clickable { onClick() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(dateStr, color = Color(0xFF666666), fontSize = 11.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Delete, stringResource(R.string.history_delete), tint = Color(0xFFFF4444), modifier = Modifier.size(24.dp))
            }
        }

        // Expandable details
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))

                // Test / MOT (first after line)
                val hasTest = record.testValidUntil != null || record.motStatus != null
                if (hasTest) {
                    record.motStatus?.let {
                        val expired = it.lowercase() != "valid" && it.lowercase() != "geldig"
                        InfoRow(stringResource(R.string.label_mot_test), it, isExpired = expired)
                    }
                    record.testValidUntil?.let {
                        InfoRow(stringResource(R.string.label_valid_until), it, isExpired = isDateExpired(it))
                    }
                    record.lastTestDate?.let { InfoRow(stringResource(R.string.label_last_test), it) }
                    Spacer(Modifier.height(4.dp))
                }

                // Basic
                record.ownership?.let { InfoRow(stringResource(R.string.label_ownership), it) }
                record.bodyType?.let { InfoRow(stringResource(R.string.label_body), it) }
                record.onRoadDate?.let { InfoRow(stringResource(R.string.label_registered), it) }
                InfoRow(stringResource(R.string.label_plate), record.plateNumber)
                record.trimLevel?.let { InfoRow(stringResource(R.string.label_trim), it) }

                // Engine
                val hasEngine = record.engineCapacity != null || record.engineModel != null || record.numCylinders != null
                if (hasEngine) {
                    Spacer(Modifier.height(4.dp))
                    record.engineCapacity?.let { InfoRow(stringResource(R.string.label_engine), "${it}cc") }
                    record.engineModel?.let { InfoRow(stringResource(R.string.label_engine_model), it) }
                    record.numCylinders?.let { InfoRow(stringResource(R.string.label_cylinders), "$it") }
                    record.co2Emissions?.let { InfoRow(stringResource(R.string.label_co2), "${it} g/km") }
                    record.emissionGroup?.let { InfoRow(stringResource(R.string.label_emission_group), "$it") }
                }

                // Tax
                record.taxStatus?.let {
                    val expired = it.lowercase() != "taxed"
                    InfoRow(stringResource(R.string.label_tax), it, isExpired = expired)
                }
                record.taxDueDate?.let { InfoRow(stringResource(R.string.label_tax_due), it, isExpired = isDateExpired(it)) }

                // Specs
                val hasSpecs = record.numDoors != null || record.numSeats != null || record.weight != null
                if (hasSpecs) {
                    Spacer(Modifier.height(4.dp))
                    record.numDoors?.let { InfoRow(stringResource(R.string.label_doors), "$it") }
                    record.numSeats?.let { InfoRow(stringResource(R.string.label_seats), "$it") }
                    record.weight?.let { InfoRow(stringResource(R.string.label_weight), "${it} kg") }
                    record.wheelbase?.let { InfoRow(stringResource(R.string.label_wheelbase), "${it} cm") }
                    record.catalogPrice?.let { InfoRow(stringResource(R.string.label_catalog_price), "€$it") }
                }

                // Tires
                record.frontTires?.let { InfoRow(stringResource(R.string.label_front_tires), it) }
                record.rearTires?.let { InfoRow(stringResource(R.string.label_rear_tires), it) }

                // Insurance & Chassis
                record.insured?.let { InfoRow(stringResource(R.string.label_insured), it) }
                record.chassisNumber?.let { InfoRow(stringResource(R.string.label_chassis), it) }
            }
        }
    }
}
