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
import androidx.compose.material.icons.filled.Close
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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.carinfo.ar.data.model.VehicleInfo
import com.carinfo.ar.ui.theme.BrandPrimary
import com.carinfo.ar.ui.theme.GlassBorder
import com.carinfo.ar.ui.theme.GlassOverlay
import com.carinfo.ar.util.PriceEstimator
import androidx.compose.ui.res.stringResource
import com.carinfo.ar.R
import java.net.URLEncoder
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

/**
 * Format an estimated value using the appropriate currency symbol.
 */
fun formatEstimateValue(value: Int, currency: PriceEstimator.Currency): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val symbol = when (currency) {
        PriceEstimator.Currency.ILS -> "₪"
        PriceEstimator.Currency.EUR -> "€"
        PriceEstimator.Currency.GBP -> "£"
    }
    return "$symbol${formatter.format(value)}"
}

@Composable
fun CompactEstimateCard(estimate: PriceEstimator.Estimate, modifier: Modifier = Modifier) {
    val (dotColor) = when {
        estimate.confidence >= 0.85f -> listOf(Color(0xFF4CAF50))
        estimate.confidence >= 0.65f -> listOf(Color(0xFFFFB300))
        else -> listOf(Color(0xFFFF7043))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BrandPrimary.copy(alpha = 0.12f))
            .border(1.dp, BrandPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.label_estimated_value),
                color = Color(0xFFAAAAAA),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatEstimateValue(estimate.mid, estimate.currency),
            color = BrandPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            text = "${formatEstimateValue(estimate.low, estimate.currency)} – ${formatEstimateValue(estimate.high, estimate.currency)}",
            color = Color(0xFF888888),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun EstimatedValueCard(estimate: PriceEstimator.Estimate) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BrandPrimary.copy(alpha = 0.12f))
            .border(1.dp, BrandPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.label_estimated_value),
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            ConfidenceDot(estimate.confidence)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatEstimateValue(estimate.mid, estimate.currency),
            color = BrandPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${formatEstimateValue(estimate.low, estimate.currency)} – ${formatEstimateValue(estimate.high, estimate.currency)}",
            color = Color(0xFFBBBBBB),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.estimated_value_disclaimer),
            color = Color(0xFF888888),
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 13.sp
        )
    }
}

data class PricelistLink(val label: String, val url: String)

/**
 * Yad2 manufacturer IDs scraped from yad2.co.il/price-list (April 2026).
 * Keys are data.gov.il Hebrew manufacturer names, normalized (no dots/spaces).
 */
private val yad2ManufacturerIds: Map<String, Int> = mapOf(
    "אאודי" to 1, "אופל" to 2, "אינפיניטי" to 3, "איסוזו" to 4,
    "אלפארומיאו" to 5, "אםגי" to 6, "במוו" to 7, "ביואיק" to 8,
    "גיאמאמס" to 9, "גיפ" to 10, "גרייטוול" to 11, "דאציה" to 12,
    "דודג" to 13, "דיאס" to 14, "דייהטסו" to 15, "האמר" to 16,
    "הונדה" to 17, "וולוו" to 18, "טויוטה" to 19, "יגואר" to 20,
    "יונדאי" to 21, "לוטוס" to 22, "לינקולן" to 23, "לנדרובר" to 24,
    "לנציה" to 25, "לקסוס" to 26, "מאזדה" to 27, "מזראטי" to 28,
    "מיני" to 29, "מיצובישי" to 30, "מרצדסבנץ" to 31, "מרצדס" to 31,
    "ניסאן" to 32, "סאאב" to 33, "סאנגיונג" to 34, "סובארו" to 35,
    "סוזוקי" to 36, "סיאט" to 37, "סיטרואן" to 38, "סמארט" to 39,
    "סקודה" to 40, "פולקסווגן" to 41, "פונטיאק" to 42, "פורד" to 43,
    "פורשה" to 44, "פיאט" to 45, "פיגו" to 46, "קאדילק" to 47,
    "קיה" to 48, "קרייזלר" to 49, "רובר" to 50, "רנו" to 51,
    "שברולט" to 52, "אבארט" to 53, "אסטוןמרטין" to 54, "בנטלי" to 55,
    "סאנשיין" to 56, "פרארי" to 57, "רולסרויס" to 58, "תעשיותרכב" to 59,
    "דייהו" to 60, "טסלה" to 62, "למבורגיני" to 63, "מקלארן" to 73,
    "אלטיאי" to 77, "ננגינג" to 78, "לאדה" to 80, "איווקו" to 85,
    "מאן" to 86, "טאטא" to 87, "דונגפנג" to 88, "מקסוס" to 89,
    "פיאגיו" to 90, "ראם" to 91, "קופרה" to 92, "גנסיס" to 93,
    "אוטוביאנקי" to 96, "סנטרו" to 97, "גיאייסי" to 99, "אקורה" to 111,
    "אלפין" to 115, "ארקפוקס" to 117, "באייק" to 126, "בייד" to 141,
    "צרי" to 147, "גילי" to 177, "ביאידאבליו" to 193, "גייאיסי" to 200,
    "קארמה" to 203, "מורגן" to 219, "אורה" to 224, "פולסטאר" to 231,
    "ויי" to 284, "סרס" to 287, "איוויס" to 288, "ניאו" to 289,
    "אקספנג" to 290, "אלאיוויסי" to 299, "סקייוול" to 300, "הונגצי" to 301,
    "אינאוס" to 310, "גופיל" to 319, "ליפמוטור" to 320, "לינקאנדקו" to 321,
    "וויה" to 322, "איויאיזי" to 323, "זיקר" to 333, "פורתינג" to 334,
    "אקסאיוי" to 335, "אווטאר" to 338, "קיגיאם" to 344, "אסדאבליואם" to 345,
    "גיאיוואן" to 346, "נטע" to 348, "אקסיד" to 349, "פוטון" to 352,
    "גאקו" to 355, "יודו" to 357, "דאיון" to 360, "ריהיי" to 361,
    "דיפאל" to 362, "לינקסיס" to 363, "פאריזון" to 364, "אומודה" to 369,
    "אייאם" to 374, "איון" to 379
)

/** Normalize a Hebrew manufacturer name — strip dots, spaces, apostrophes, dashes. */
private fun normalizeMake(s: String): String =
    s.trim().replace(".", "").replace(" ", "").replace("'", "").replace("\"", "")
        .replace("-", "").replace("׳", "").replace("״", "")

/**
 * Per-country direct deep links to valuation & listing sites.
 * Each country has two: a valuation authority + a real-listings marketplace.
 * Links go STRAIGHT to the site's search with concrete filters — no Google.
 */
private fun pricelistLinksFor(info: VehicleInfo): List<PricelistLink> {
    val rawMake = info.manufacturer?.trim().orEmpty()
    val rawModel = info.model?.trim().orEmpty()
    val year = info.year
    if (rawMake.isEmpty()) return emptyList()

    val queryText = buildString {
        append(rawMake)
        if (rawModel.isNotEmpty()) { append(' '); append(rawModel) }
        year?.let { append(' '); append(it) }
    }
    val encText = URLEncoder.encode(queryText, "UTF-8")
    val encMake = URLEncoder.encode(rawMake, "UTF-8")
    val encModel = URLEncoder.encode(rawModel, "UTF-8")
    val makeSlug = rawMake.lowercase().replace(' ', '-')
    val modelSlug = rawModel.lowercase().replace(' ', '-')

    return when (info.country) {
        "IL" -> {
            val mfgId = yad2ManufacturerIds[normalizeMake(rawMake)]
            val yearParam = year?.let { "&year=$it-$it" } ?: ""
            val yad2Url = if (mfgId != null) {
                "https://www.yad2.co.il/price-list/feed?manufacturer=$mfgId$yearParam"
            } else {
                "https://www.yad2.co.il/price-list"
            }
            // Levi Itzhak — their dedicated free pricelist page (better than homepage).
            val leviUrl = "https://levi-itzhak.co.il/%D7%90%D7%A4%D7%9C%D7%99%D7%A7%D7%A6%D7%99%D7%94-%D7%9C%D7%91%D7%93%D7%99%D7%A7%D7%AA-%D7%A8%D7%9B%D7%91-%D7%9E%D7%97%D7%99%D7%A8%D7%95%D7%9F-%D7%A8%D7%9B%D7%91-%D7%9C%D7%95%D7%99-%D7%99%D7%A6%D7%97%D7%A7"
            listOf(
                PricelistLink("לוי יצחק", leviUrl),
                PricelistLink("יד 2", yad2Url)
            )
        }
        "NL" -> listOf(
            PricelistLink("ANWB", "https://www.anwb.nl/auto/koerslijst"),
            PricelistLink("Marktplaats", "https://www.marktplaats.nl/q/$encText/")
        )
        "GB" -> {
            val yearParam = year?.let { "&year-from=$it&year-to=$it" } ?: ""
            listOf(
                PricelistLink("Parkers", "https://www.parkers.co.uk/$makeSlug/$modelSlug/"),
                PricelistLink("Auto Trader", "https://www.autotrader.co.uk/car-search?postcode=SW1A1AA&make=$encMake&model=$encModel$yearParam")
            )
        }
        else -> emptyList()
    }
}

@Composable
fun PricelistLinksRow(info: VehicleInfo, modifier: Modifier = Modifier) {
    val links = remember(info.country, info.manufacturer, info.model, info.year) {
        pricelistLinksFor(info)
    }
    if (links.isEmpty()) return
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        links.forEach { link ->
            PricelistPill(link) {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) { /* No browser installed */ }
            }
        }
    }
}

@Composable
private fun PricelistPill(link: PricelistLink, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(BrandPrimary.copy(alpha = 0.12f))
            .border(0.5.dp, BrandPrimary.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = link.label,
            color = BrandPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun ConfidenceDot(confidence: Float) {
    val (color, label) = when {
        confidence >= 0.85f -> Color(0xFF4CAF50) to stringResource(R.string.confidence_high)
        confidence >= 0.65f -> Color(0xFFFFB300) to stringResource(R.string.confidence_medium)
        else -> Color(0xFFFF7043) to stringResource(R.string.confidence_low)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
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
fun SectionDivider() {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)
    Spacer(Modifier.height(6.dp))
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = BrandPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 1.dp)
    )
}

@Composable
fun FloatingCarInfo(
    vehicleInfo: VehicleInfo,
    plateNumber: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onSaveToHistory: ((buttonOffset: androidx.compose.ui.geometry.Offset) -> Unit)? = null,
    onOpenModelInfo: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
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
            // === HEADER: Flag + (♿ if disabled) + Manufacturer + Action Buttons ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = countryFlag, fontSize = 20.sp)
                if (vehicleInfo.disabledTag == true) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "♿",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFFAA00).copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
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
                        fontSize = 18.sp
                    )
                    val subtitle = buildString {
                        vehicleInfo.year?.let { append("$it") }
                        vehicleInfo.trimLevel?.let {
                            if (isNotEmpty()) append(" • ")
                            append(it)
                        }
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(subtitle, color = BrandPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                // Close button — same pill style as Save, tinted red
                if (onDelete != null) {
                    Spacer(Modifier.width(6.dp))
                    val closeTint = Color(0xFFFF7A7A)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onDelete() }
                            .background(closeTint.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.overlay_dismiss),
                            tint = closeTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            stringResource(R.string.overlay_dismiss),
                            color = closeTint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Accent line
            Spacer(Modifier.height(6.dp))
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
            Spacer(Modifier.height(4.dp))

            // === ESTIMATED VALUE + TEST / MOT / APK — side by side at the top ===
            val estimate = remember(vehicleInfo) { PriceEstimator.estimate(vehicleInfo) }
            val hasTest = vehicleInfo.testValidUntil != null || vehicleInfo.lastTestDate != null ||
                    vehicleInfo.motStatus != null
            if (hasTest || estimate != null) {
                if (hasTest) SectionHeader(stringResource(R.string.label_section_test))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        vehicleInfo.motStatus?.let {
                            val expired = it.lowercase() != "valid" && it.lowercase() != "geldig"
                            InfoRow(stringResource(R.string.label_mot_test), it, isExpired = expired)
                        }
                        vehicleInfo.testValidUntil?.let {
                            InfoRow(stringResource(R.string.label_valid_until), it, isExpired = isDateExpired(it))
                        }
                        vehicleInfo.lastTestDate?.let { InfoRow(stringResource(R.string.label_last_test), it) }
                        vehicleInfo.lastTestKm?.let {
                            InfoRow(stringResource(R.string.label_last_test_km), "${NumberFormat.getNumberInstance().format(it)} km")
                        }
                    }
                    estimate?.let {
                        Spacer(Modifier.width(8.dp))
                        CompactEstimateCard(it)
                    }
                }
            }

            // === OWNERSHIP HISTORY (IL) — right after test ===
            val hasOwnership = !vehicleInfo.ownershipHistory.isNullOrEmpty()
            if (hasOwnership) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_ownership_history))
                vehicleInfo.ownershipHistory?.forEach { record ->
                    InfoRow(record.date, record.type)
                }
            }

            // === PRICE (IL) — right after ownership ===
            vehicleInfo.priceAtRegistration?.let { price ->
                SectionDivider()
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

            // Estimated market value is now shown inline at the top (next to Test section).

            // === DISABLED TAG — right after price ===
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
                        text = "♿",
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

            // === BASIC INFO ===
            SectionDivider()
            SectionHeader(stringResource(R.string.label_section_basic))
            vehicleInfo.color?.let { InfoRow(stringResource(R.string.label_color), it) }
            vehicleInfo.secondaryColor?.let { InfoRow(stringResource(R.string.label_secondary_color), it) }
            vehicleInfo.fuelType?.let { InfoRow(stringResource(R.string.label_fuel), it) }
            vehicleInfo.ownership?.let { InfoRow(stringResource(R.string.label_ownership), it) }
            vehicleInfo.bodyType?.let { InfoRow(stringResource(R.string.label_body), it) }
            vehicleInfo.onRoadDate?.let { InfoRow(stringResource(R.string.label_registered), it) }
            vehicleInfo.ownerRegistrationDate?.let { InfoRow(stringResource(R.string.label_owner_since), it) }
            vehicleInfo.countryOfOrigin?.let { InfoRow(stringResource(R.string.label_country_origin), it) }
            vehicleInfo.importerName?.let { InfoRow(stringResource(R.string.label_importer), it) }
            vehicleInfo.euCategory?.let { InfoRow(stringResource(R.string.label_eu_category), it) }
            vehicleInfo.isTaxi?.let { if (it) InfoRow(stringResource(R.string.label_taxi), stringResource(R.string.label_yes)) }
            vehicleInfo.isExported?.let { if (it) InfoRow(stringResource(R.string.label_exported), stringResource(R.string.label_yes)) }
            plateNumber?.let { InfoRow(stringResource(R.string.label_plate), it) }

            // === ENGINE ===
            val hasEngine = vehicleInfo.engineCapacity != null || vehicleInfo.engineModel != null ||
                    vehicleInfo.numCylinders != null || vehicleInfo.co2Emissions != null ||
                    vehicleInfo.horsepower != null || vehicleInfo.engineDisplacement != null ||
                    vehicleInfo.enginePowerKw != null
            if (hasEngine) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_engine))
                vehicleInfo.horsepower?.let { InfoRow(stringResource(R.string.label_horsepower), "$it HP") }
                vehicleInfo.enginePowerKw?.let { kw ->
                    if (vehicleInfo.horsepower == null) {
                        InfoRow(stringResource(R.string.label_engine_power), "${kw.toInt()} kW (${(kw * 1.36).toInt()} HP)")
                    } else {
                        InfoRow(stringResource(R.string.label_engine_power), "${kw.toInt()} kW")
                    }
                }
                vehicleInfo.engineDisplacement?.let { InfoRow(stringResource(R.string.label_displacement), "${NumberFormat.getNumberInstance().format(it)} cc") }
                vehicleInfo.engineCapacity?.let {
                    if (vehicleInfo.engineDisplacement == null) InfoRow(stringResource(R.string.label_engine), "${it}cc")
                }
                vehicleInfo.engineModel?.let { InfoRow(stringResource(R.string.label_engine_model), it) }
                vehicleInfo.numCylinders?.let { InfoRow(stringResource(R.string.label_cylinders), "$it") }
                vehicleInfo.co2Emissions?.let { InfoRow(stringResource(R.string.label_co2), "${it} g/km") }
                vehicleInfo.euroEmissionClass?.let { InfoRow(stringResource(R.string.label_euro_class), it) }
                vehicleInfo.emissionGroup?.let { InfoRow(stringResource(R.string.label_emission_group), "$it") }
                vehicleInfo.greenIndex?.let { InfoRow(stringResource(R.string.label_green_index), "$it") }
                vehicleInfo.fuelEfficiencyClass?.let { InfoRow(stringResource(R.string.label_efficiency_class), it) }
            }

            // === FUEL CONSUMPTION (NL) ===
            val hasFuelConsumption = vehicleInfo.fuelConsumptionCombined != null
            if (hasFuelConsumption) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_fuel))
                vehicleInfo.fuelConsumptionCombined?.let { InfoRow(stringResource(R.string.label_fuel_combined), "$it l/100km") }
                vehicleInfo.fuelConsumptionCity?.let { InfoRow(stringResource(R.string.label_fuel_city), "$it l/100km") }
                vehicleInfo.fuelConsumptionHighway?.let { InfoRow(stringResource(R.string.label_fuel_highway), "$it l/100km") }
            }

            // === SPECS ===
            val hasSpecs = vehicleInfo.numDoors != null || vehicleInfo.numSeats != null ||
                    vehicleInfo.weight != null || vehicleInfo.wheelbase != null ||
                    vehicleInfo.catalogPrice != null || vehicleInfo.driveType != null ||
                    vehicleInfo.transmission != null || vehicleInfo.vehicleLength != null
            if (hasSpecs) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_specs))
                vehicleInfo.driveType?.let { InfoRow(stringResource(R.string.label_drive), it) }
                vehicleInfo.driveTechnology?.let { InfoRow(stringResource(R.string.label_drive_tech), it) }
                vehicleInfo.transmission?.let { InfoRow(stringResource(R.string.label_transmission), it) }
                vehicleInfo.standardType?.let { InfoRow(stringResource(R.string.label_standard), it) }
                vehicleInfo.numDoors?.let { InfoRow(stringResource(R.string.label_doors), "$it") }
                vehicleInfo.numSeats?.let { InfoRow(stringResource(R.string.label_seats), "$it") }
                vehicleInfo.weight?.let { InfoRow(stringResource(R.string.label_weight), "${NumberFormat.getNumberInstance().format(it)} kg") }
                vehicleInfo.emptyMass?.let { InfoRow(stringResource(R.string.label_empty_mass), "${NumberFormat.getNumberInstance().format(it)} kg") }
                // Dimensions (NL)
                if (vehicleInfo.vehicleLength != null && vehicleInfo.vehicleWidth != null) {
                    val dims = buildString {
                        append("${vehicleInfo.vehicleLength} × ${vehicleInfo.vehicleWidth}")
                        vehicleInfo.vehicleHeight?.let { append(" × $it") }
                        append(" cm")
                    }
                    InfoRow(stringResource(R.string.label_dimensions), dims)
                }
                vehicleInfo.wheelbase?.let { InfoRow(stringResource(R.string.label_wheelbase), "${it} cm") }
                vehicleInfo.catalogPrice?.let { InfoRow(stringResource(R.string.label_catalog_price), "€${NumberFormat.getNumberInstance().format(it)}") }
                vehicleInfo.purchaseTax?.let { InfoRow(stringResource(R.string.label_bpm_tax), "€${NumberFormat.getNumberInstance().format(it)}") }
                vehicleInfo.licensingGroup?.let { InfoRow(stringResource(R.string.label_licensing_group), "$it") }
            }

            // === ODOMETER (NL) ===
            val hasOdometer = vehicleInfo.odometerJudgment != null
            if (hasOdometer) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_odometer))
                vehicleInfo.odometerJudgment?.let { InfoRow(stringResource(R.string.label_odometer_judgment), it) }
                vehicleInfo.odometerYear?.let { InfoRow(stringResource(R.string.label_odometer_year), "$it") }
            }

            // === RECALL (NL) ===
            vehicleInfo.hasOpenRecall?.let { hasRecall ->
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_recall))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (hasRecall) Color(0xFFFF4444).copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            1.dp,
                            if (hasRecall) Color(0xFFFF4444).copy(alpha = 0.4f)
                            else Color(0xFF333333),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasRecall) "⚠️" else "✅",
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasRecall) stringResource(R.string.label_has_recall)
                               else stringResource(R.string.label_no_recall),
                        color = if (hasRecall) Color(0xFFFF4444) else Color(0xFF888888),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_tax))
                vehicleInfo.taxStatus?.let {
                    val expired = it.lowercase() != "taxed"
                    InfoRow(stringResource(R.string.label_tax), it, isExpired = expired)
                }
                vehicleInfo.taxDueDate?.let {
                    InfoRow(stringResource(R.string.label_tax_due), it, isExpired = isDateExpired(it))
                }
            }

            // === UK EXTRA ===
            val hasUkExtra = vehicleInfo.v5cDate != null || vehicleInfo.typeApproval != null ||
                    vehicleInfo.markedForExport != null
            if (hasUkExtra) {
                SectionDivider()
                vehicleInfo.v5cDate?.let { InfoRow(stringResource(R.string.label_v5c_date), it) }
                vehicleInfo.typeApproval?.let { InfoRow(stringResource(R.string.label_type_approval), it) }
                vehicleInfo.markedForExport?.let { if (it) InfoRow(stringResource(R.string.label_marked_export), stringResource(R.string.label_yes)) }
            }

            // === TOWING ===
            val hasTowing = vehicleInfo.towingWithBrakes != null || vehicleInfo.towingWithoutBrakes != null ||
                    vehicleInfo.towHook != null || vehicleInfo.maxTowingBraked != null
            if (hasTowing) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_towing))
                vehicleInfo.towHook?.let { InfoRow(stringResource(R.string.label_tow_hook), it) }
                vehicleInfo.towingWithBrakes?.let { InfoRow(stringResource(R.string.label_towing_brakes), "$it kg") }
                vehicleInfo.towingWithoutBrakes?.let { InfoRow(stringResource(R.string.label_towing_no_brakes), "$it kg") }
                vehicleInfo.maxTowingBraked?.let {
                    if (vehicleInfo.towingWithBrakes == null) InfoRow(stringResource(R.string.label_towing_brakes), "$it kg")
                }
                vehicleInfo.maxTowingUnbraked?.let {
                    if (vehicleInfo.towingWithoutBrakes == null) InfoRow(stringResource(R.string.label_towing_no_brakes), "$it kg")
                }
            }

            // === TIRES (IL) ===
            val hasTires = vehicleInfo.frontTires != null
            if (hasTires) {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_tires))
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
                // lastTestKm moved up to Test section
                vehicleInfo.lpgAdded?.let { InfoRow(stringResource(R.string.label_lpg_added), boolToYesNo(it)) }
                vehicleInfo.colorChanged?.let { InfoRow(stringResource(R.string.label_color_changed), boolToYesNo(it)) }
                vehicleInfo.tiresChanged?.let { InfoRow(stringResource(R.string.label_tires_changed), boolToYesNo(it)) }
                vehicleInfo.originality?.let { InfoRow(stringResource(R.string.label_originality), it) }
            }

            // === STATISTICS ===
            vehicleInfo.activeVehiclesCount?.let {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_statistics))
                InfoRow(stringResource(R.string.label_active_vehicles), "$it")
            }

            // === INSURANCE (NL) ===
            vehicleInfo.insured?.let {
                SectionDivider()
                SectionHeader(stringResource(R.string.label_section_insurance))
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
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassOverlay)
            .border(1.dp, BrandPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = BrandPrimary.copy(alpha = alpha),
            strokeWidth = 2.5.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.overlay_loading_title, plateNumber),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.overlay_loading_wait),
                color = BrandPrimary.copy(alpha = alpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (onDelete != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.overlay_dismiss),
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun PlateNotFoundIndicator(
    plateNumber: String,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GlassOverlay)
            .border(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$plateNumber — ${stringResource(R.string.overlay_not_found)}",
            color = Color(0xFFFF6B6B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (onDelete != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.overlay_dismiss),
                    tint = Color(0xFFBBBBBB),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
