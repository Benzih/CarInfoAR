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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import androidx.compose.ui.res.stringResource
import com.carinfo.ar.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Check if a date string (YYYY-MM-DD or similar) is expired
 */
private fun isDateExpired(dateStr: String?): Boolean {
    if (dateStr == null) return false
    return try {
        val cleanDate = dateStr.replace("/", "-").take(10)
        val date = LocalDate.parse(cleanDate, DateTimeFormatter.ISO_LOCAL_DATE)
        date.isBefore(LocalDate.now())
    } catch (_: Exception) {
        false
    }
}

/**
 * Format a boolean as "יש"/"אין" or "Yes"/"No"
 */
@Composable
private fun boolToYesNo(value: Boolean): String {
    return if (value) stringResource(R.string.label_yes) else stringResource(R.string.label_no)
}

/**
 * Format price with commas and ₪ symbol
 */
private fun formatPrice(price: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale("he", "IL"))
    return "₪${formatter.format(price)}"
}

@Composable
fun InfoRow(label: String, value: String, isExpired: Boolean = false) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Text("$label: ", color = Color(0xFF888888), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(
            value,
            color = if (isExpired) Color(0xFFFF4444) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(4.dp))
    Text(
        text = title,
        color = BrandPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(2.dp))
}

@Composable
fun FloatingCarInfo(
    vehicleInfo: VehicleInfo,
    plateNumber: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onSaveToHistory: ((buttonOffset: androidx.compose.ui.geometry.Offset) -> Unit)? = null,
    onOpenModelInfo: (() -> Unit)? = null
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
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassOverlay)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // === HEADER: Flag + Manufacturer + Action Buttons ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = countryFlag, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            vehicleInfo.manufacturer?.let { append(it) }
                            vehicleInfo.model?.let {
                                if (isNotEmpty()) append(" ")
                                append(it)
                            }
                        }.ifEmpty { stringResource(R.string.overlay_unknown) },
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    val subtitle = buildString {
                        vehicleInfo.year?.let { append("$it") }
                        vehicleInfo.trimLevel?.let {
                            if (isNotEmpty()) append(" • ")
                            append(it)
                        }
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(subtitle, color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Action buttons next to title
                if (onSaveToHistory != null) {
                    var saveBtnPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSaveToHistory(saveBtnPos) }
                            .background(BrandPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .onGloballyPositioned { coords ->
                                saveBtnPos = coords.positionInRoot()
                            }
                    ) {
                        Icon(Icons.Default.History, "Save", tint = BrandPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(stringResource(R.string.overlay_save), color = BrandPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (onOpenModelInfo != null) {
                    Spacer(Modifier.width(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenModelInfo() }
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, "Info", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(stringResource(R.string.overlay_info), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Accent line
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrandPrimary, BrandPrimary.copy(alpha = 0.1f))
                        )
                    )
            )
            Spacer(Modifier.height(8.dp))

            // === TEST / MOT / APK (first after line) ===
            val hasTest = vehicleInfo.testValidUntil != null || vehicleInfo.lastTestDate != null ||
                    vehicleInfo.motStatus != null
            if (hasTest) {
                vehicleInfo.motStatus?.let {
                    val expired = it.lowercase() != "valid" && it.lowercase() != "geldig"
                    InfoRow(stringResource(R.string.label_mot_test), it, isExpired = expired)
                }
                vehicleInfo.testValidUntil?.let {
                    InfoRow(stringResource(R.string.label_valid_until), it, isExpired = isDateExpired(it))
                }
                vehicleInfo.lastTestDate?.let { InfoRow(stringResource(R.string.label_last_test), it) }
            }

            // === BASIC INFO ===
            vehicleInfo.color?.let { InfoRow(stringResource(R.string.label_color), it) }
            vehicleInfo.fuelType?.let { InfoRow(stringResource(R.string.label_fuel), it) }
            vehicleInfo.ownership?.let { InfoRow(stringResource(R.string.label_ownership), it) }
            vehicleInfo.bodyType?.let { InfoRow(stringResource(R.string.label_body), it) }
            vehicleInfo.onRoadDate?.let { InfoRow(stringResource(R.string.label_registered), it) }
            vehicleInfo.countryOfOrigin?.let { InfoRow(stringResource(R.string.label_country_origin), it) }
            plateNumber?.let { InfoRow(stringResource(R.string.label_plate), it) }

            // === IMPORTER & PRICE (IL) ===
            val hasImporter = vehicleInfo.importerName != null || vehicleInfo.priceAtRegistration != null
            if (hasImporter) {
                SectionDivider()
                vehicleInfo.importerName?.let { InfoRow(stringResource(R.string.label_importer), it) }
                vehicleInfo.priceAtRegistration?.let { price ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${stringResource(R.string.label_price)}: ",
                            color = Color(0xFF888888),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            formatPrice(price),
                            color = BrandPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // === ENGINE ===
            val hasEngine = vehicleInfo.engineCapacity != null || vehicleInfo.engineModel != null ||
                    vehicleInfo.numCylinders != null || vehicleInfo.co2Emissions != null ||
                    vehicleInfo.horsepower != null || vehicleInfo.engineDisplacement != null
            if (hasEngine) {
                SectionDivider()
                vehicleInfo.horsepower?.let { InfoRow(stringResource(R.string.label_horsepower), "$it HP") }
                vehicleInfo.engineDisplacement?.let { InfoRow(stringResource(R.string.label_displacement), "${NumberFormat.getNumberInstance().format(it)} cc") }
                vehicleInfo.engineCapacity?.let {
                    if (vehicleInfo.engineDisplacement == null) InfoRow(stringResource(R.string.label_engine), "${it}cc")
                }
                vehicleInfo.engineModel?.let { InfoRow(stringResource(R.string.label_engine_model), it) }
                vehicleInfo.numCylinders?.let { InfoRow(stringResource(R.string.label_cylinders), "$it") }
                vehicleInfo.co2Emissions?.let { InfoRow(stringResource(R.string.label_co2), "${it} g/km") }
                vehicleInfo.emissionGroup?.let { InfoRow(stringResource(R.string.label_emission_group), "$it") }
                vehicleInfo.greenIndex?.let { InfoRow(stringResource(R.string.label_green_index), "$it") }
            }

            // === SPECS ===
            val hasSpecs = vehicleInfo.numDoors != null || vehicleInfo.numSeats != null ||
                    vehicleInfo.weight != null || vehicleInfo.wheelbase != null ||
                    vehicleInfo.catalogPrice != null || vehicleInfo.driveType != null ||
                    vehicleInfo.transmission != null
            if (hasSpecs) {
                SectionDivider()
                vehicleInfo.driveType?.let { InfoRow(stringResource(R.string.label_drive), it) }
                vehicleInfo.driveTechnology?.let { InfoRow(stringResource(R.string.label_drive_tech), it) }
                vehicleInfo.transmission?.let { InfoRow(stringResource(R.string.label_transmission), it) }
                vehicleInfo.standardType?.let { InfoRow(stringResource(R.string.label_standard), it) }
                vehicleInfo.numDoors?.let { InfoRow(stringResource(R.string.label_doors), "$it") }
                vehicleInfo.numSeats?.let { InfoRow(stringResource(R.string.label_seats), "$it") }
                vehicleInfo.weight?.let { InfoRow(stringResource(R.string.label_weight), "${NumberFormat.getNumberInstance().format(it)} kg") }
                vehicleInfo.wheelbase?.let { InfoRow(stringResource(R.string.label_wheelbase), "${it} cm") }
                vehicleInfo.catalogPrice?.let { InfoRow(stringResource(R.string.label_catalog_price), "€$it") }
                vehicleInfo.licensingGroup?.let { InfoRow(stringResource(R.string.label_licensing_group), "$it") }
            }

            // === EQUIPMENT (IL) ===
            val hasEquipment = vehicleInfo.electricWindows != null || vehicleInfo.sunroof != null ||
                    vehicleInfo.alloyWheels != null || vehicleInfo.tirePressureSensors != null ||
                    vehicleInfo.reverseCamera != null
            if (hasEquipment) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_equipment))
                vehicleInfo.electricWindows?.let { InfoRow(stringResource(R.string.label_electric_windows), "$it") }
                vehicleInfo.sunroof?.let { InfoRow(stringResource(R.string.label_sunroof), boolToYesNo(it)) }
                vehicleInfo.alloyWheels?.let { InfoRow(stringResource(R.string.label_alloy_wheels), boolToYesNo(it)) }
                vehicleInfo.tirePressureSensors?.let { InfoRow(stringResource(R.string.label_tire_pressure), boolToYesNo(it)) }
                vehicleInfo.reverseCamera?.let { InfoRow(stringResource(R.string.label_reverse_camera), boolToYesNo(it)) }
            }

            // === SAFETY SYSTEMS (IL) ===
            val hasSafety = vehicleInfo.airbagCount != null || vehicleInfo.abs != null ||
                    vehicleInfo.stabilityControl != null || vehicleInfo.laneDeparture != null ||
                    vehicleInfo.forwardDistanceMonitoring != null || vehicleInfo.adaptiveCruise != null
            if (hasSafety) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_safety))
                vehicleInfo.airbagCount?.let { InfoRow(stringResource(R.string.label_airbags), "$it") }
                vehicleInfo.abs?.let { InfoRow(stringResource(R.string.label_abs), boolToYesNo(it)) }
                vehicleInfo.stabilityControl?.let { InfoRow(stringResource(R.string.label_stability), boolToYesNo(it)) }
                vehicleInfo.laneDeparture?.let { InfoRow(stringResource(R.string.label_lane_departure), boolToYesNo(it)) }
                vehicleInfo.forwardDistanceMonitoring?.let { InfoRow(stringResource(R.string.label_distance_monitor), boolToYesNo(it)) }
                vehicleInfo.adaptiveCruise?.let { InfoRow(stringResource(R.string.label_adaptive_cruise), boolToYesNo(it)) }
                vehicleInfo.pedestrianDetection?.let { InfoRow(stringResource(R.string.label_pedestrian), boolToYesNo(it)) }
                vehicleInfo.blindSpotDetection?.let { InfoRow(stringResource(R.string.label_blind_spot), boolToYesNo(it)) }
                vehicleInfo.safetyScore?.let { InfoRow(stringResource(R.string.label_safety_score), "$it") }
            }

            // === TAX (UK) ===
            val hasTax = vehicleInfo.taxStatus != null
            if (hasTax) {
                vehicleInfo.taxStatus?.let {
                    val expired = it.lowercase() != "taxed"
                    InfoRow(stringResource(R.string.label_tax), it, isExpired = expired)
                }
                vehicleInfo.taxDueDate?.let {
                    InfoRow(stringResource(R.string.label_tax_due), it, isExpired = isDateExpired(it))
                }
            }

            // === TOWING (IL) ===
            val hasTowing = vehicleInfo.towingWithBrakes != null || vehicleInfo.towingWithoutBrakes != null ||
                    vehicleInfo.towHook != null
            if (hasTowing) {
                SectionDivider()
                vehicleInfo.towHook?.let { InfoRow(stringResource(R.string.label_tow_hook), it) }
                vehicleInfo.towingWithBrakes?.let { InfoRow(stringResource(R.string.label_towing_brakes), "$it kg") }
                vehicleInfo.towingWithoutBrakes?.let { InfoRow(stringResource(R.string.label_towing_no_brakes), "$it kg") }
            }

            // === TIRES (IL) ===
            val hasTires = vehicleInfo.frontTires != null
            if (hasTires) {
                SectionDivider()
                vehicleInfo.frontTires?.let { InfoRow(stringResource(R.string.label_front_tires), it) }
                vehicleInfo.rearTires?.let { InfoRow(stringResource(R.string.label_rear_tires), it) }
            }

            // === HISTORY & INTERNAL DETAILS (IL) ===
            val hasHistory = vehicleInfo.engineNumber != null || vehicleInfo.lastTestKm != null ||
                    vehicleInfo.lpgAdded != null || vehicleInfo.colorChanged != null ||
                    vehicleInfo.originality != null || vehicleInfo.chassisNumber != null
            if (hasHistory) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_internal))
                vehicleInfo.chassisNumber?.let { InfoRow(stringResource(R.string.label_chassis), it) }
                vehicleInfo.engineNumber?.let { InfoRow(stringResource(R.string.label_engine_number), it) }
                vehicleInfo.lastTestKm?.let { InfoRow(stringResource(R.string.label_last_test_km), "${NumberFormat.getNumberInstance().format(it)} km") }
                vehicleInfo.lpgAdded?.let { InfoRow(stringResource(R.string.label_lpg_added), boolToYesNo(it)) }
                vehicleInfo.colorChanged?.let { InfoRow(stringResource(R.string.label_color_changed), boolToYesNo(it)) }
                vehicleInfo.tiresChanged?.let { InfoRow(stringResource(R.string.label_tires_changed), boolToYesNo(it)) }
                vehicleInfo.originality?.let { InfoRow(stringResource(R.string.label_originality), it) }
            }

            // === OWNERSHIP HISTORY (IL) ===
            val hasOwnership = !vehicleInfo.ownershipHistory.isNullOrEmpty()
            if (hasOwnership) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_ownership_history))
                vehicleInfo.ownershipHistory?.forEach { record ->
                    InfoRow(record.date, record.type)
                }
            }

            // === DISABLED TAG ===
            vehicleInfo.disabledTag?.let { hasTag ->
                SectionDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (hasTag) Color(0xFFFFAA00).copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            1.dp,
                            if (hasTag) Color(0xFFFFAA00).copy(alpha = 0.4f)
                            else Color(0xFF333333),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasTag) "♿" else "♿",
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasTag) stringResource(R.string.label_has_disabled_tag)
                               else stringResource(R.string.label_no_disabled_tag),
                        color = if (hasTag) Color(0xFFFFAA00) else Color(0xFF888888),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // === STATISTICS ===
            vehicleInfo.activeVehiclesCount?.let {
                SectionDivider()
                InfoRow(stringResource(R.string.label_active_vehicles), "$it")
            }

            // === INSURANCE (NL) ===
            vehicleInfo.insured?.let {
                SectionDivider()
                InfoRow(stringResource(R.string.label_insured), it)
            }

            // === EXTRA CODES (IL) ===
            val hasCodes = vehicleInfo.modelCode != null || vehicleInfo.registrationDirective != null
            if (hasCodes) {
                SectionDivider()
                vehicleInfo.modelCode?.let { InfoRow(stringResource(R.string.label_model_code), it) }
                vehicleInfo.registrationDirective?.let { InfoRow(stringResource(R.string.label_registration_directive), "$it") }
            }

            // === DATA SOURCE ===
            val dataSource = when (vehicleInfo.country) {
                "IL" -> "data.gov.il"
                "NL" -> "opendata.rdw.nl"
                "GB" -> "dvla.gov.uk"
                else -> null
            }
            if (dataSource != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${stringResource(R.string.label_data_source)}: $dataSource",
                    color = Color(0xFF555555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Action buttons moved to header row
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GlassOverlay)
            .border(1.dp, BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = BrandPrimary.copy(alpha = alpha),
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = plateNumber,
            color = BrandPrimary.copy(alpha = alpha),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlateNotFoundIndicator(
    plateNumber: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$plateNumber — ${stringResource(R.string.overlay_not_found)}",
        color = Color(0xFFFF6B6B),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GlassOverlay)
            .border(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
