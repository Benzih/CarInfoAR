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
 * Offline market-value estimator (v2 — calibrated Apr 2026).
 *
 * Combines 15+ factors available from data.gov.il (IL), RDW (NL), and DVLA (UK):
 *  base catalog price × age curve × fuel-type modifier × ownership × mileage
 *      × body type × brand tier × trim × safety × open recall × test expiry
 *      × emissions × originality × LPG × parallel-import
 *
 * ## Calibration
 *
 * v2 tuned against 43 Levi-Yitzhak (authoritative IL pricelist) + 5 Yad2
 * reference prices collected in-app. Mean absolute delta dropped from 27.4%
 * (v1) to 15.7% (v2). Key changes from v1 findings:
 *
 * 1. AGE CURVE completely reshaped — IL cars hold value better Y0-5 (less
 *    drop than v1 assumed) but drop MUCH harder Y10+ than v1's 0.90/yr
 *    (real Y14 retention ≈ 0.11, v1 predicted 0.23).
 * 2. HYBRID DETECTION from MODEL NAME (HSD/HEV/HYBRID) — data.gov.il lists
 *    Toyota HSD / Hyundai HEV with fuelType="בנזין" (gasoline), so v1 never
 *    applied the hybrid boost and underestimated them by up to 58%.
 * 3. HYBRID BOOST 1.02 → 1.15 (Toyota/Hyundai hybrids hold value unusually
 *    well in IL).
 * 4. EV Y0-1 PENALTY REMOVED — Chinese EVs in IL are priced competitively
 *    and don't drop 25% in year 1.
 * 5. COMMERCIAL VAN DETECTION (Berlingo/Vito/Caddy/…) — old work vans
 *    retain value because they stay useful; v1 miscategorized them as
 *    regular cars and under-predicted by 90%+ for old vans.
 * 6. HAND PENALTIES HALVED — v1 assumed 14-18% drop for h4-5, reality is 9-13%.
 * 7. USAGE PENALTIES SOFTENED — lease/rental 18% → 10%.
 * 8. DIESEL STAGED by age + commercial-van exception.
 * 9. CHINESE penalty Y0-1 SOFTENED (0.85 → 0.93) — brand-new Chinese EVs
 *    weren't hit with Y1 over-depreciation.
 * 10. PREMIUM-LUX Y5+ STEEPER (0.95 → 0.85) — luxury German + Volvo + Land
 *     Rover lose value hard past Y5.
 * 11. MILEAGE slope halved and caps softened.
 * 12. SCRAP FLOOR — no IL car estimate below ₪8,000 (observed market floor).
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

        val fAgeFuel = ageFuelFactor(age, fuel, country, info.model)
        val fOwners = ownersFactor(info)
        val fMileage = mileageFactor(info.lastTestKm, age, country)
        val fBody = bodyFactor(info.bodyType)
        val fBrand = brandFactor(info.manufacturer, info.model, age, country, info.bodyType)
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

        val rawFactor = fAgeFuel * fOwners * fMileage * fBody * fBrand * fTrim *
                fSafety * fRecall * fTest * fEmission * fOrig * fLpg *
                fParallel * fTaxi * fExport

        // v3d: Japanese-SUV Y15+ retention floor. Raw age factor for Y15+ is
        // ~0.12; even with Premium-reliable×SUV-old (×1.15) and body (×1.05),
        // retention caps around 10%. Forester Y17 was -28% vs LY (23.6k vs 17k).
        // Floor retention at 14% — old Japanese 4WD SUVs hold demand better
        // than the raw depreciation curve implies.
        val japaneseSuvKeywords = setOf("TOYOTA","LEXUS","HONDA","MAZDA","SUBARU",
            "טויוטה","לקסוס","הונדה","מאזדה","מזדה","סובארו")
        val mkUpper = (info.manufacturer ?: "").uppercase().trim()
        val bodyLower = (info.bodyType ?: "").lowercase()
        val isSuvBody = "suv" in bodyLower || "crossover" in bodyLower ||
            "פנאי" in bodyLower || "ג'יפ" in bodyLower || "jeep" in bodyLower
        val isJapaneseSuvOld = country == "IL" && age >= 15.0 && isSuvBody &&
            japaneseSuvKeywords.any { mkUpper.startsWith(it) }
        val factor = if (isJapaneseSuvOld && rawFactor < 0.14) 0.14 else rawFactor

        // IL scrap floor: a running car rarely trades below ~₪8,000 even at Y16+.
        // Calibrated against Levi-Yitzhak 2009 FIAT 500 (₪8,700) and 2011 Cruze (₪8,000).
        // v3c: raised to ₪10,000 for Y13+ cars that had decent catalog prices
        // (≥₪100k). Hyundai i30 Y17.9 on ₪111k catalog was hitting the ₪8k
        // floor but LY valued it at ₪11.3k — small Korean/Japanese cars with
        // some brand recall hold a higher residual than true scrap.
        val floor = when {
            country != "IL" -> 0.0
            age >= 13.0 && base >= 100_000 -> 10_000.0
            else -> 8_000.0
        }
        // Y<1 ceiling: a car barely off the lot can't be worth more than ~95% of
        // catalog. v3b: Kona Supreme Y0.9 hybrid was computed at ₪209k on ₪186k
        // catalog (1.12×) because Korean × Hybrid-Y0 × SUV × 0-km compounded
        // to >1.0. Clamp prevents absurd > catalog values.
        val rawMid = base * factor
        val ceiled = if (age <= 1.0) minOf(rawMid, base * 0.95) else rawMid
        val mid = maxOf(ceiled, floor)
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
     *
     * IL curve (v2 — calibrated against 43 Levi-Yitzhak prices):
     *   Y0 → 0.92,  Y1 → 0.85,  Y5 → 0.62,  Y10 → 0.25,  Y14 → 0.12,  Y16 → 0.08
     * Non-IL curve is steeper (matches EU/UK classical depreciation).
     *
     * Fuel modifier (IL):
     *   Hybrid (HSD/HEV/HYBRID badge OR fuel=hybrid): ×1.15 (Toyota/Hyundai
     *     hybrids hold value unusually well)
     *   EV: no Y0-1 penalty (Chinese EVs priced competitively), mild Y1-3, ×0.96 Y3+
     *   PHEV: mild penalty Y1-3, neutral Y3+
     *   Diesel passenger cars: staged (0.95 → 0.88 → 0.80)
     *   Diesel COMMERCIAL VAN (Berlingo/Vito/Caddy/Transit/…): retention
     *     BONUS on old vans — they stay useful as work vehicles.
     *
     * Hybrid detection reads the MODEL NAME because data.gov.il lists
     * Toyota HSD / Hyundai HEV with fuelType="בנזין" (gasoline).
     */
    private fun ageFuelFactor(
        age: Double,
        fuelType: String?,
        country: String,
        model: String? = null,
    ): Double {
        // v3b (Apr 2026): validated against 80 Levi-Yitzhak prices (55 train +
        // 25 held-out). MAD 13.15% overall, 15.4% on held-out (27% on prior).
        // v3b changes vs v3: Y5-10 slope softened 0.83 → 0.84 and Y10 anchor
        // bumped 0.249 → 0.279 — LY prices Y7-10 cars ~10-15% higher than v3.
        val yearsRetained = when (country) {
            "IL" -> when {
                age <= 1.0  -> 0.92 - (age * 0.07)                      // 0.92 → 0.85
                age <= 5.0  -> 0.85 * 0.925.pow(age - 1.0)              // 0.85 → 0.622
                age <= 10.0 -> 0.622 * 0.84.pow(age - 5.0)              // 0.622 → 0.279
                else        -> 0.279 * 0.85.pow(age - 10.0)              // 0.279 → 0.124 @ Y15
            }
            else -> when {
                age <= 1.0  -> 0.80 - (age * 0.06)                      // 0.80 → 0.74
                age <= 5.0  -> 0.74 * 0.88.pow(age - 1.0)               // 0.74 → 0.44
                age <= 10.0 -> 0.44 * 0.85.pow(age - 5.0)               // 0.44 → 0.195
                else        -> 0.195 * 0.85.pow(age - 10.0)
            }
        }
        val f = (fuelType ?: "").lowercase()
        val mUp = (model ?: "").uppercase()
        // data.gov.il often labels hybrids as "בנזין" — detect via badge
        val hybridBadge = "HSD" in mUp || "HEV" in mUp ||
            " HYBRID" in mUp || "HYBRID " in mUp || mUp == "HYBRID"
        val isPhev = "חשמל/בנז" in f || "phev" in f || "plug" in f || "plugin" in f ||
            ("hybrid" in f && "plug" in f) || "PHEV" in mUp
        val isEv = (("חשמל" in f) && ("בנז" !in f) && !isPhev) ||
            (("electric" in f) && ("hybrid" !in f) && !isPhev)
        val isHybrid = !isEv && !isPhev && (hybridBadge ||
            "hybrid" in f || "היבר" in f || "hybride" in f)
        val isDiesel = "דיזל" in f || "diesel" in f
        val isLpg = "lpg" in f || "גפ\"מ" in f || "גפמ" in f
        // Commercial vans — model-name check
        val isCommercialVan = listOf(
            "BERLINGO","VITO","PARTNER","CADDY","KANGOO","TRANSIT",
            "DOBLO","DUCATO","SPRINTER","MASTER","TRAFIC","EXPRESS",
            "PROACE","JUMPY","EXPERT"
        ).any { it in mUp }

        val fuelMul: Double = if (country == "IL") {
            when {
                isEv -> when {
                    age <= 1.0 -> 1.00
                    age <= 3.0 -> 0.93
                    else       -> 0.96
                }
                isPhev -> when {
                    age <= 1.0 -> 1.02
                    age <= 3.0 -> 0.98
                    else       -> 1.00
                }
                // Toyota/Hyundai hybrids sustain value well in IL. Staged: Y0-3
                // gets a modest boost; Y3+ gets a stronger one since the
                // Premium-reliable / Korean-IL brand multiplier compounds and
                // Y3+ data (Corolla HSD Y4, Elantra HEV Y4) under-predicted
                // with the flat 1.15.
                isHybrid -> if (age >= 3.0) 1.15 else 1.10
                isDiesel -> when {
                    // v3c fix: DON'T apply the "old work van" ×1.20 fuel bonus
                    // when brand tier is also Commercial (×1.15) — that's a
                    // double-boost. Berlingo Y13.9 was +25% overestimate from
                    // the two bonuses stacking. Keep mild Y<8 bonus so diesel
                    // vans beat gas vans, but not the +20% on top.
                    isCommercialVan -> when {
                        age <= 4.0 -> 0.97
                        age <= 8.0 -> 0.98
                        else       -> 1.00   // was 1.20 — brand Commercial handles retention
                    }
                    age <= 4.0 -> 0.95
                    age <= 7.0 -> 0.88
                    else       -> 0.80
                }
                isLpg -> 0.88
                else -> 1.0
            }
        } else {
            when {
                isEv -> when {
                    age <= 1.0 -> 0.82
                    age <= 3.0 -> 0.88
                    else       -> 0.95
                }
                isHybrid || isPhev -> 1.02
                isDiesel -> if (age >= 5) 0.92 else 0.98
                isLpg -> 0.90
                else -> 1.0
            }
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

        // v3b calibration: Python reference used stronger penalties than the
        // previous Kotlin. Drift noticed when user pointed out that Accent
        // INSPIRE 2022 (ex-lease, hand 2, 68k km) estimated at ₪81,500 in
        // the app while my Python said ₪77,000. The gap came entirely from
        // 0.08 vs 0.12 history-lease penalty and 0.03 vs 0.05 hand-2 penalty.
        // Aligning Kotlin to Python values (calibrated against 80 LY prices).
        val usagePenalty = when {
            "מונית" in o || "taxi" in o -> 0.30
            "לימוד" in o || "driving school" in o -> 0.25
            "השכר" in o || "החכר" in o || "חכיר" in o || "ליסינג" in o ||
                "rental" in o || "lease" in o -> 0.18
            "חברה" in o || "company" in o -> 0.12
            "ממשלתי" in o || "government" in o || "מוניציפלי" in o -> 0.25
            else -> 0.0
        }
        // Past fleet/rental origin — doubled vs old Kotlin (0.08 → 0.12)
        val historyPenalty = if (history.any { rec ->
                val t = rec.type.lowercase()
                "השכר" in t || "החכר" in t || "חכיר" in t || "ליסינג" in t ||
                    "rental" in t || "lease" in t || "מונית" in t || "taxi" in t
            } && usagePenalty == 0.0) 0.12 else 0.0

        // Hand count — exclude dealers ("סוחר"). Stronger penalties than
        // old Kotlin (h2 0.03→0.05, h3 0.06→0.10, h4 0.09→0.14 ...).
        val realOwnerCount = history.count { "סוחר" !in it.type }
        val handCount = max(realOwnerCount, 1)
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
        // v3: negative cap tightened -0.12 → -0.05. Levi-Yitzhak barely
        // penalizes high-km cars (e.g. Grand Coupe 227k km Y9 still valued
        // at ₪47k; Kodiaq 242k km Y8 still ₪79k). Over-penalizing km made us
        // miss those cars by ~30%. Mild penalty kept to preserve ordering:
        // a low-km car is still worth more than a high-km peer.
        val adj = -0.01 * (delta / 10_000.0)
        return 1.0 + adj.coerceIn(-0.05, 0.08)
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
     * Brand tier (v3 — calibrated against 55 Levi-Yitzhak prices, MAD 13.1%).
     *
     * Naming format from data.gov.il: `{brand} {country-of-assembly}` —
     *   "רנו טורקיה" (Renault Turkey), "קיה סלובקיה" (Kia Slovakia), etc.
     * Brand keywords must match as PREFIX, not substring: the Hebrew word
     * for Turkey ("טורקיה") ends in "קיה" (Hebrew for Kia), so substring
     * matching would mis-classify Renault Turkey as Korean.
     *
     * Tiers (by order of checking — first match wins):
     *   Performance-Lux  — BMW M/AMG/RS. 0.95/0.82/0.70 by age bracket.
     *   Commercial        — vans by model (BERLINGO/VITO/CADDY/…) hold
     *                        value past Y5 as work tools.
     *   Chinese-IL        — Staged: 1.00 Y0-1, 0.95 Y1-3, 0.88 Y3-5, 0.78 Y5+.
     *                        v2 was too aggressive on Y0-1 (penalty 0.85).
     *   Premium-reliable  — Toyota/Lexus/Honda/Mazda/Subaru 1.10 past Y2
     *                        (no boost for brand-new; depreciation still
     *                        normal in first 2 years).
     *   Suzuki-solid      — 1.05 past Y2. Separate tier: Suzuki Swift/Celerio
     *                        hold value well in IL but not at Japanese
     *                        premium level.
     *   Korean-IL         — 1.10 (bumped from 1.06 — Picanto/Sportage/Ioniq
     *                        consistently under-estimated at 1.06).
     *   Premium-Lux       — BMW/Mercedes/Audi/Porsche/Volvo/Land Rover/Jaguar.
     *                        1.00 <Y3, 0.92 Y3-5, 0.85 Y5+. Unchanged.
     *   Weak-resale       — Fiat/Renault/Citroen/Peugeot/Dacia/Alfa/Lancia.
     *                        0.92 Y≤10, 0.78 Y>10. French/Italian budget
     *                        brands lose value sharply past Y10 in IL.
     *   Old-generic       — Chevrolet/Opel/Daewoo: normal Y≤10, but 0.70
     *                        past Y10. Aging Chevys/Opels drop hard in IL
     *                        (Cruze, Sonic, Corsa at Y10+ sell for ₪13-16k
     *                        off ₪100-125k catalog).
     *   Mid-reliable      — VW/Skoda/Ford/Mitsubishi/Nissan: 1.02 flat.
     *                        Volume brands that don't crash like Weak-resale
     *                        but aren't Premium-reliable.
     *   Standard          — 1.00 (fallback).
     */
    private fun brandFactor(
        make: String?,
        model: String?,
        age: Double,
        country: String,
        bodyType: String? = null,
    ): Double {
        val m = (make ?: "").uppercase().trim()
        val mdl = (model ?: "").uppercase()
        val body = (bodyType ?: "").lowercase()
        // Crossover/SUV models that data.gov.il sometimes classifies as MPV,
        // sedan, or hatchback. v3d regression: QASHQAI PLUS 2 was classified
        // as MPV and fell into Mid-reliable sedan tier (1.02 vs 1.08 SUV),
        // losing 6.5 points vs v3c. Same risk for crossovers like TIGUAN
        // sometimes labeled "sedan" or KODIAQ labeled "MPV".
        val crossoverModels = setOf(
            // Nissan
            "QASHQAI", "X-TRAIL", "XTRAIL", "JUKE", "MURANO", "PATHFINDER",
            "ARMADA", "TERRANO", "ROGUE",
            // VW
            "TIGUAN", "TOUAREG", "T-ROC", "T-CROSS", "TAOS", "ATLAS", "ID.4", "ID4",
            // Skoda
            "KODIAQ", "KAROQ", "YETI",
            // Ford
            "KUGA", "EDGE", "EXPLORER", "EXPEDITION", "BRONCO", "ESCAPE", "ECOSPORT",
            // Mitsubishi
            "OUTLANDER", "ECLIPSE CROSS", "ASX", "PAJERO", "XPANDER"
        )
        val isCrossoverModel = crossoverModels.any { it in mdl }
        val isSuv = "suv" in body || "crossover" in body || "פנאי" in body ||
            "ג'יפ" in body || "jeep" in body || isCrossoverModel
        fun matches(tokens: Set<String>) = tokens.any { m.startsWith(it) }

        // Performance-Lux first (narrower than Premium-Lux; needs German tier)
        val performanceTags = listOf(
            "M8", "M850", "M5 ", " M5", "M3 ", " M3", "M4 ", " M4",
            "AMG", " RS ", " RS4", " RS6", " RS7", " S4 ", " S6 ", " S8 "
        )
        val germanLux = setOf("BMW", "MERCEDES", "MERCEDES-BENZ", "AUDI", "PORSCHE",
            "ב.מ.וו", "ב.מ.ו", "ב מ וו", "בי אם דבליו",
            "מרצדס", "אאודי", "אודי", "פורשה",
            "VOLVO", "LAND ROVER", "RANGE ROVER", "JAGUAR",
            "וולבו", "לנד רובר", "ריינג רובר", "רובר", "יגואר")
        val isPerformance = performanceTags.any { it in mdl }
        if (isPerformance && matches(germanLux)) {
            return when {
                age < 3 -> 0.95
                age < 5 -> 0.82
                else    -> 0.70
            }
        }

        // Commercial vans — also matched inside ageFuelFactor for fuel modifier,
        // here they get a BRAND boost past Y5 (regardless of fuel).
        val commercialModels = setOf(
            "BERLINGO","VITO","PARTNER","CADDY","KANGOO","TRANSIT",
            "DOBLO","DUCATO","SPRINTER","MASTER","TRAFIC","VIVARO",
            "EXPRESS","EXPERT","PROACE","JUMPY","COMBO","CONNECT",
            "ברלינגו","פרטנר","קאנגו","קדי","טרנזיט","ויוואר",
            "אקספרט","דוקאטו","ויטו","ספרינטר"
        )
        val isCommercial = commercialModels.any { it in mdl }
        if (isCommercial) return if (age >= 5) 1.15 else 1.00

        // Chinese — IL only, staged penalty
        val chinese = setOf(
            "BYD", "CHERY", "GEELY", "MG", "JAECOO", "OMODA",
            "GREAT WALL", "HAVAL", "NIO", "XPENG", "LEAPMOTOR",
            "DONGFENG", "MAXUS", "ZEEKR", "SAIC", "ROEWE",
            "צ'רי", "ביי די", "ליפמוטור", "אומודה",
            "ג'אקו", "ג'יקו", "זיקר", "מ.ג"
        )
        if (country == "IL" && matches(chinese)) {
            return when {
                age <= 1.0 -> 1.00
                age <= 3.0 -> 0.95
                age <= 5.0 -> 0.88
                else       -> 0.78
            }
        }

        // SsangYong — Korean budget/older image, priced like Weak-resale (v3b)
        val ssangYong = setOf("SSANGYONG", "SANGYONG", "סאנגיונג", "סאנגיוג")
        if (matches(ssangYong)) return 0.92

        // Premium-reliable — Japanese reliable. v3d extends v3c fade:
        //   (a) Y13+ SUV = 1.15 (Forester Y17 retention boost — 4WD demand)
        //   (b) Y13-15 non-SUV = 1.00 (was 1.05 — Mazda 3 Y13-15 was +24% over)
        //   (c) Y15+ non-SUV = 0.95 (was 1.00 — Accord Y17 sedan was +23% over)
        //   Extra retention floor for Japanese SUV Y15+ applied after all factors
        //   in estimate() — see floor block.
        val premiumReliable = setOf("TOYOTA", "LEXUS", "HONDA", "MAZDA", "SUBARU",
            "טויוטה", "לקסוס", "הונדה", "מאזדה", "מזדה", "סובארו")
        if (matches(premiumReliable)) {
            return when {
                age >= 13.0 && isSuv -> 1.15   // old Japanese SUVs keep value
                age >= 15.0 -> 0.95            // v3d: non-SUV sedan Y15+ fades more
                age >= 13.0 -> 1.00            // v3d: non-SUV Y13-15 fade deeper
                else        -> 1.10
            }
        }

        // Suzuki — v3d: split by model. Budget subcompacts (SWIFT/CELERIO/ALTO/
        // SPLASH/IGNIS) don't hold value like the solid ones (JIMNY/SX4/VITARA/
        // S-CROSS/BALENO). CELERIO Y9.9 was +27% overestimate under flat 1.05.
        if (m.startsWith("סוזוקי") || m.startsWith("SUZUKI")) {
            val suzukiBudget = listOf("SWIFT", "CELERIO", "ALTO", "SPLASH", "IGNIS")
            val isBudget = suzukiBudget.any { it in mdl }
            return when {
                age >= 15.0 -> 1.00
                isBudget    -> 1.00           // v3d: budget Suzukis drop faster
                else        -> 1.05            // solid (Jimny etc.) keeps boost
            }
        }

        // Korean — IL only. v3b: Y15+ fades to 1.00 (i30 2008 was overpriced).
        val korean = setOf("HYUNDAI", "KIA", "GENESIS", "יונדאי", "קיה", "גנסיס")
        if (country == "IL" && matches(korean)) {
            return if (age >= 15.0) 1.00 else 1.10
        }

        // German/European luxury — Tesla added as young-tech-luxury (harsh
        // depreciation matches this tier). v3c: Y<3 dropped 1.00 → 0.92
        // (Audi Q5 Y2 was +18%, Tesla Model Y Y1 was +17%). Y10+ new tier
        // at 0.70 (Volvo XC60 Y11.9 was +26%).
        val germanLuxPlusTesla = germanLux + setOf("TESLA", "טסלה")
        if (matches(germanLuxPlusTesla)) {
            return when {
                age < 3   -> 0.92
                age < 5   -> 0.90
                age < 10  -> 0.85
                else      -> 0.70
            }
        }

        // Weak-resale — steeper past Y10
        val weakResale = setOf("FIAT", "ALFA", "ALFA ROMEO", "RENAULT", "CITROEN", "PEUGEOT",
            "DACIA", "LANCIA",
            "פיאט", "אלפא", "רנו", "סיטרואן", "פיג'ו", "דאצ'יה", "דאציה")
        if (matches(weakResale)) {
            return if (age > 10) 0.78 else 0.92
        }

        // Old-generic (Chevrolet/Opel/Daewoo) — only penalty past Y10
        val oldGeneric = setOf("CHEVROLET", "OPEL", "DAEWOO", "HOLDEN",
            "שברולט", "אופל", "דוו", "דיאו")
        if (matches(oldGeneric)) {
            return if (age > 10) 0.70 else 1.00
        }

        // v3d: Jeep/Chrysler-old tier. Compass Y14 was +18% overestimate under
        // Standard 1.00. American mid-size SUVs depreciate harder than Japanese.
        val jeepChrysler = setOf("JEEP", "CHRYSLER", "DODGE",
            "ג'יפ", "קרייזלר", "דודג'")
        if (matches(jeepChrysler)) {
            return if (age > 10) 0.85 else 1.00
        }

        // Mid-reliable — v3d: split by body. Sentra Y5.9 SEDAN was +24% under
        // flat 1.08, because Nissan SUVs (X-Trail/Qashqai) hold value but
        // compact sedans don't. SUVs keep the 1.08 retention; sedans/hatches
        // get only 1.02.
        val midReliable = setOf("VW", "VOLKSWAGEN", "SKODA", "FORD", "MITSUBISHI", "NISSAN",
            "פולקסווגן", "סקודה", "פורד", "מיצובישי", "ניסאן")
        if (matches(midReliable)) {
            return when {
                age >= 5.0 && isSuv -> 1.08   // X-Trail/Qashqai/Kodiaq keep boost
                age >= 5.0          -> 1.02   // v3d: Sentra/Space Star sedan dropped
                else                 -> 1.02
            }
        }

        return 1.00
    }

    // --- Minor factors ---

    private fun trimFactor(trim: String?): Double {
        val t = (trim ?: "").lowercase()
        // v3b trim calibration: removed INSPIRE (Hyundai mid-trim, not top)
        // and STYLE (Skoda Kodiaq mid-trim). Also removed BUSINESS (fleet trim,
        // not premium). The Accent INSPIRE was overpriced by 3% because of this.
        return when {
            "luxury" in t || "premium" in t || "יוקרה" in t || "עליון" in t ||
                "top" in t || "executive" in t ||
                "prestige" in t || "limited" in t || "gls" in t ||
                "supreme" in t || "signature" in t || "highline" in t ||
                "titanium" in t -> 1.03
            "base" in t || "basic" in t || "בסיס" in t || "standard" in t ||
                "expression" in t || "pop" in t || "essential" in t -> 0.97
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
