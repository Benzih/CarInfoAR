package com.carinfo.ar.util

import android.util.Log
import com.carinfo.ar.BuildConfig
import com.carinfo.ar.data.model.VehicleInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Offline market-value estimator.
 *
 * Combines 15+ factors available from data.gov.il (IL), RDW (NL), and DVLA (UK):
 *  base catalog price × age curve × fuel-type modifier × ownership × mileage
 *      × body type × brand tier × trim × safety × open recall × test expiry
 *      × emissions × originality × LPG × parallel-import
 *
 * Returns null when there is no base price anchor (UK has no catalog price;
 * some IL models miss the importer/price resource).
 */
object PriceEstimator {

    /** Local currency of the estimate — caller formats accordingly. */
    enum class Currency { ILS, EUR, GBP }

    data class Estimate(
        val low: Int,
        val mid: Int,
        val high: Int,
        val currency: Currency,
        val confidence: Float
    )

    fun estimate(info: VehicleInfo): Estimate? {
        val base = basePrice(info) ?: return null
        val age = ageYears(info) ?: return null
        val country = info.country
        val fuel = info.fuelType

        val fAgeFuel = ageFuelFactor(age, fuel, country)
        val fOwners = ownersFactor(info)
        val fMileage = mileageFactor(info.lastTestKm, age, country)
        val fBody = bodyFactor(info.bodyType)
        val fBrand = brandFactor(info.manufacturer, age, country)
        val fTrim = trimFactor(info.trimLevel)
        val fSafety = safetyFactor(info.safetyScore ?: info.safetyRating)
        val fRecall = if (info.hasOpenRecall == true) 0.94 else 1.0
        val fTest = testExpiryFactor(info)
        val fEmission = emissionFactor(info)
        val fOrig = originalityFactor(info)
        val fLpg = if (info.lpgAdded == true) 0.88 else 1.0
        val fParallel = if (isParallelImport(info)) 0.97 else 1.0
        val fTaxi = if (info.isTaxi == true) 0.70 else 1.0
        val fExport = if (info.isExported == true) 0.85 else 1.0

        val factor = fAgeFuel * fOwners * fMileage * fBody * fBrand * fTrim *
                fSafety * fRecall * fTest * fEmission * fOrig * fLpg *
                fParallel * fTaxi * fExport

        val mid = base * factor
        val confidence = confidence(info)
        val spread = 0.12 + (1.0 - confidence) * 0.08

        if (BuildConfig.DEBUG) {
            Log.d("PriceEstimator",
                "=== ${info.manufacturer} ${info.model} ${info.year} (${info.country}) ===")
            Log.d("PriceEstimator",
                "base=$base age=${"%.2f".format(age)} fuel=$fuel km=${info.lastTestKm} " +
                "owners='${info.ownership}' history=${info.ownershipHistory?.size}")
            Log.d("PriceEstimator",
                "factors: ageFuel=${fmt(fAgeFuel)} owners=${fmt(fOwners)} " +
                "mileage=${fmt(fMileage)} body=${fmt(fBody)} brand=${fmt(fBrand)} " +
                "trim=${fmt(fTrim)} safety=${fmt(fSafety)} recall=${fmt(fRecall)} " +
                "test=${fmt(fTest)} emission=${fmt(fEmission)} orig=${fmt(fOrig)} " +
                "lpg=${fmt(fLpg)} parallel=${fmt(fParallel)} taxi=${fmt(fTaxi)} " +
                "export=${fmt(fExport)}")
            Log.d("PriceEstimator",
                "combined factor=${fmt(factor)} → mid=${mid.toInt()} " +
                "(low=${(mid * (1 - spread)).toInt()}, high=${(mid * (1 + spread)).toInt()}) " +
                "confidence=${"%.2f".format(confidence)}")
        }

        return Estimate(
            low = (mid * (1 - spread)).roundToCleanInt(),
            mid = mid.roundToCleanInt(),
            high = (mid * (1 + spread)).roundToCleanInt(),
            currency = currencyFor(country),
            confidence = confidence
        )
    }

    private fun fmt(d: Double): String = "%.3f".format(d)

    // --- Base price resolution ---

    private fun basePrice(info: VehicleInfo): Double? {
        info.priceAtRegistration?.takeIf { it > 0 }?.let { return it.toDouble() }
        info.catalogPrice?.takeIf { it > 0 }?.let { return it.toDouble() }
        return null
    }

    private fun currencyFor(country: String): Currency = when (country) {
        "NL" -> Currency.EUR
        "GB" -> Currency.GBP
        else -> Currency.ILS
    }

    // --- Age + fuel curve ---

    /**
     * Core depreciation curve combined with fuel-type modifier.
     * IL market retains value better than US/EU (inflated new-car prices
     * + supply constraints). Non-IL curve is steeper.
     * EVs depreciate ~25% extra Y1; diesels take a ULEZ hit after Y5.
     */
    private fun ageFuelFactor(age: Double, fuelType: String?, country: String): Double {
        val yearsRetained = when (country) {
            "IL" -> when {
                age <= 1.0 -> 0.87 - (age * 0.04)                       // Y0-Y1: 87 → 83%
                age <= 3.0 -> 0.83 * 0.93.pow(age - 1.0)                // -7%/yr
                age <= 6.0 -> 0.83 * 0.93.pow(2.0) * 0.92.pow(age - 3.0) // -8%/yr
                // After Y6 IL market drops ~10%/yr (validated against Levi Itzhak
                // pricelist: 2016 Dacia Duster ₪31.5k on ₪100k catalog = 31.5% at
                // Y10, which 0.93/yr overstated to 42%).
                else       -> 0.83 * 0.93.pow(2.0) * 0.92.pow(3.0) * 0.90.pow(age - 6.0)
            }
            else -> when {
                age <= 1.0 -> 0.82 - (age * 0.06)                       // Y0-Y1: 82 → 76%
                age <= 3.0 -> 0.76 * 0.88.pow(age - 1.0)                // -12%/yr
                else       -> 0.76 * 0.88.pow(2.0) * 0.92.pow(age - 3.0) // -8%/yr
            }
        }
        val f = (fuelType ?: "").lowercase()
        val fuelMul = when {
            "חשמל" in f || "electric" in f || "elektr" in f ->
                when {
                    age <= 1.0 -> 0.75
                    age <= 3.0 -> 0.85
                    else       -> 0.95
                }
            "hybrid" in f || "היבר" in f || "hybride" in f -> 1.02
            "דיזל" in f || "diesel" in f -> if (age >= 5) 0.92 else 0.98
            "lpg" in f || "גפ\"מ" in f || "גפמ" in f -> 0.90
            else -> 1.0
        }
        return yearsRetained * fuelMul
    }

    // --- Ownership (hands + fleet origin) ---

    /**
     * Israeli appraiser rule: penalties don't sum — apply the biggest
     * discount fully, then only 50% of the next.
     */
    private fun ownersFactor(info: VehicleInfo): Double {
        val o = (info.ownership ?: "").lowercase()
        val history = info.ownershipHistory.orEmpty()

        // Usage-type penalty (biggest single factor).
        // Hebrew variants: השכרה/השכר = rental, ליסינג = leasing,
        // החכר/חכירה = leased-out, מונית = taxi, לימוד = driving-school.
        val usagePenalty = when {
            "מונית" in o || "taxi" in o -> 0.30
            "לימוד" in o || "driving school" in o -> 0.25
            "השכר" in o || "החכר" in o || "חכיר" in o || "ליסינג" in o ||
                "rental" in o || "lease" in o -> 0.18
            "חברה" in o || "company" in o -> 0.12
            "ממשלתי" in o || "government" in o || "מוניציפלי" in o -> 0.25
            else -> 0.0
        }
        // Also check entire ownership history for past fleet/rental origin
        val historyPenalty = if (history.any { rec ->
                val t = rec.type.lowercase()
                "השכר" in t || "החכר" in t || "חכיר" in t || "ליסינג" in t ||
                    "rental" in t || "lease" in t || "מונית" in t || "taxi" in t
            } && usagePenalty == 0.0) 0.12 else 0.0

        // Hands count
        val handCount = max(history.size, 1)
        val handPenalty = when (handCount) {
            1 -> 0.0
            2 -> 0.05
            3 -> 0.10
            4 -> 0.14
            else -> 0.18
        }

        // Chain: biggest × (1 - 0.5 × next) × (1 - 0.25 × next)
        val penalties = listOf(usagePenalty, historyPenalty, handPenalty)
            .filter { it > 0 }
            .sortedDescending()
        var factor = 1.0
        penalties.forEachIndexed { idx, p ->
            val weight = when (idx) { 0 -> 1.0; 1 -> 0.5; else -> 0.25 }
            factor *= (1.0 - p * weight)
        }
        return factor
    }

    // --- Mileage ---

    private fun mileageFactor(km: Int?, age: Double, country: String): Double {
        if (km == null || km <= 0 || age <= 0) return 1.0
        val avgPerYear = if (country == "NL") 13_000 else 15_000
        val expected = avgPerYear * age
        val delta = km - expected
        // ~2% per 10k km off average, capped
        val adj = -0.02 * (delta / 10_000.0)
        return 1.0 + adj.coerceIn(-0.20, 0.12)
    }

    // --- Body type ---

    private fun bodyFactor(body: String?): Double {
        val b = (body ?: "").lowercase()
        return when {
            "suv" in b || "crossover" in b || "פנאי" in b || "ג'יפ" in b || "jeep" in b -> 1.05
            "sedan" in b || "סדאן" in b -> 1.00
            "hatchback" in b || "האצ" in b -> 0.98
            "mpv" in b || "minivan" in b || "מיניואן" in b || "van" in b -> 0.94
            "coupe" in b || "cabrio" in b || "convertible" in b || "קברי" in b -> 0.97
            else -> 1.00
        }
    }

    // --- Brand tier ---

    /**
     * Chinese brands depreciate significantly faster in the Israeli market
     * (Globes, 2025 — Jaecoo/BYD/Chery flooded the market).
     * Toyota/Lexus hold value best; German premium loses more post-Y3.
     */
    private fun brandFactor(make: String?, age: Double, country: String): Double {
        val m = (make ?: "").uppercase().trim()
        val chineseBrands = setOf(
            "BYD", "CHERY", "GEELY", "MG", "JAECOO", "OMODA",
            "GREAT WALL", "HAVAL", "NIO", "XPENG", "LEAPMOTOR",
            "DONGFENG", "MAXUS", "ZEEKR", "SAIC", "ROEWE",
            "צ'רי", "ביי די", "ג'יקו", "ליפמוטור", "אומודה"
        )
        if (country == "IL" && chineseBrands.any { m.contains(it) }) {
            return when {
                age <= 1.0 -> 0.85
                age <= 3.0 -> 0.78
                else -> 0.72
            }
        }
        val premiumReliable = setOf("TOYOTA", "LEXUS", "HONDA", "MAZDA", "SUBARU",
            "טויוטה", "לקסוס", "הונדה", "מאזדה", "סובארו")
        // Korean brands (Hyundai/Kia) hold value especially well in the
        // Israeli market due to importer service network + high volume.
        val koreanIL = setOf("HYUNDAI", "KIA", "GENESIS", "יונדאי", "קיה", "גנסיס")
        val premiumGerman = setOf("BMW", "MERCEDES", "MERCEDES-BENZ", "AUDI", "PORSCHE",
            "ב.מ.וו", "ב.מ.ו", "מרצדס", "אאודי", "פורשה")
        // Dacia is a Renault budget subsidiary; Romanian builds drop faster than
        // the French parents in IL — grouped with the French weak-resale set.
        val weakResale = setOf("FIAT", "ALFA", "ALFA ROMEO", "RENAULT", "CITROEN", "PEUGEOT",
            "DACIA", "פיאט", "אלפא", "רנו", "סיטרואן", "פיג'ו", "דאצ'יה", "דאציה")
        return when {
            premiumReliable.any { m.contains(it) } -> 1.06
            country == "IL" && koreanIL.any { m.contains(it) } -> 1.04
            premiumGerman.any { m.contains(it) } -> if (age >= 3) 0.95 else 1.00
            weakResale.any { m.contains(it) } -> 0.93
            else -> 1.00
        }
    }

    // --- Minor factors ---

    private fun trimFactor(trim: String?): Double {
        val t = (trim ?: "").lowercase()
        return when {
            "luxury" in t || "premium" in t || "יוקרה" in t || "עליון" in t ||
                "top" in t || "executive" in t || "inspire" in t ||
                "prestige" in t || "limited" in t || "gls" in t -> 1.03
            "base" in t || "basic" in t || "בסיס" in t || "standard" in t -> 0.98
            else -> 1.00
        }
    }

    private fun safetyFactor(score: Int?): Double = when (score ?: 0) {
        in 7..8 -> 1.02
        in 4..6 -> 1.00
        in 1..3 -> 0.96
        else -> 1.00
    }

    private fun testExpiryFactor(info: VehicleInfo): Double {
        val expiry = info.testValidUntil ?: return 1.0
        val d = parseDate(expiry) ?: return 1.0
        return if (d.isBefore(LocalDate.now())) 0.94 else 1.0
    }

    private fun emissionFactor(info: VehicleInfo): Double {
        info.fuelEfficiencyClass?.trim()?.uppercase()?.let {
            return when (it) {
                "A" -> 1.02
                "B" -> 1.01
                "F", "G" -> 0.95
                else -> 1.00
            }
        }
        // Israeli green index is 1-15 (lower = cleaner).
        // Guard against out-of-range values (some data returns 266 etc.)
        info.greenIndex?.let { g ->
            if (g in 1..15) {
                return when {
                    g <= 3 -> 1.02
                    g >= 10 -> 0.96
                    else -> 1.00
                }
            }
        }
        info.co2Emissions?.let { co2 ->
            return when {
                co2 <= 100 -> 1.02
                co2 >= 200 -> 0.95
                else -> 1.00
            }
        }
        return 1.0
    }

    private fun originalityFactor(info: VehicleInfo): Double {
        var f = 1.0
        info.originality?.let { orig ->
            val o = orig.lowercase()
            // Israeli field returns "מקורי" / "לא מקורי"
            if ("לא" in o || "not" in o) f *= 0.85
        }
        if (info.colorChanged == true) f *= 0.92
        if (info.tiresChanged == true) f *= 0.98
        return f
    }

    private fun isParallelImport(info: VehicleInfo): Boolean {
        val imp = info.importerName ?: return false
        val s = imp.lowercase()
        return "מקביל" in s || "parallel" in s
    }

    // --- Helpers ---

    private fun ageYears(info: VehicleInfo): Double? {
        val registration = parseDate(info.onRoadDate)
            ?: info.year?.let { LocalDate.of(it, 6, 1) }
            ?: return null
        val days = ChronoUnit.DAYS.between(registration, LocalDate.now())
        return max(0.0, days / 365.25)
    }

    private fun parseDate(s: String?): LocalDate? {
        if (s.isNullOrBlank()) return null
        val clean = s.replace("/", "-").trim()
        // Try ISO_LOCAL_DATE
        runCatching { return LocalDate.parse(clean.take(10), DateTimeFormatter.ISO_LOCAL_DATE) }
        // Try month-year only (e.g. "2024-03")
        runCatching {
            if (clean.length >= 7) {
                val y = clean.substring(0, 4).toInt()
                val m = clean.substring(5, 7).toInt()
                return LocalDate.of(y, m, 15)
            }
        }
        // Try "MM/YYYY" or similar — already replaced / with -
        return null
    }

    /** Round to nearest 100 for small values, 500 for mid, 1000 for large — reads cleaner. */
    private fun Double.roundToCleanInt(): Int {
        val v = this
        return when {
            v < 10_000 -> ((v / 100).toLong() * 100).toInt()
            v < 100_000 -> ((v / 500).toLong() * 500).toInt()
            else -> ((v / 1000).toLong() * 1000).toInt()
        }
    }

    private fun confidence(info: VehicleInfo): Float {
        var c = 0.5f
        if (info.priceAtRegistration != null || info.catalogPrice != null) c += 0.15f
        if (info.lastTestKm != null) c += 0.10f
        if (info.ownership != null) c += 0.05f
        if (!info.ownershipHistory.isNullOrEmpty()) c += 0.05f
        if (info.onRoadDate != null) c += 0.10f
        if (info.bodyType != null) c += 0.05f
        return min(c, 1.0f)
    }
}
