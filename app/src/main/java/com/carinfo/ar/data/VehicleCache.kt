package com.carinfo.ar.data

import android.util.Log
import com.carinfo.ar.BuildConfig
import com.carinfo.ar.data.api.DvlaRequest
import com.carinfo.ar.data.api.RetrofitClient
import com.carinfo.ar.data.model.ImporterPriceRecord
import com.carinfo.ar.data.model.OwnershipHistoryRecord
import com.carinfo.ar.data.model.OwnershipRecord
import com.carinfo.ar.data.model.VehicleHistoryRecord
import com.carinfo.ar.data.model.VehicleInfo
import com.carinfo.ar.data.model.VehicleStatsRecord
import com.carinfo.ar.data.model.VehicleSummaryRecord
import com.carinfo.ar.data.model.WltpRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object VehicleCache {
    // Using synchronized HashMap because ConcurrentHashMap doesn't allow null values
    // and we need to cache null for "not found" plates
    private val cache = HashMap<String, VehicleInfo?>()
    private val cacheLock = Any()
    private val inFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val mutex = Mutex()

    // Model-level cache for WLTP/importer/stats (shared across vehicles of same model)
    private val wltpCache = HashMap<String, WltpRecord?>()
    private val importerCache = HashMap<String, ImporterPriceRecord?>()
    private val statsCache = HashMap<String, VehicleStatsRecord?>()

    // DVLA API key - users need to register at https://developer-portal.driver-vehicle-licensing.api.gov.uk/
    // Leave empty to disable UK support
    var dvlaApiKey: String = "vgkhEOnZPp3rF5U7qXC848wHZ4RBV0kg5PtTKsCK"

    // Resource IDs for Israel data.gov.il datasets
    private const val RES_MAIN = "053cea08-09bc-40ec-8f7a-156f0677aff3"
    private const val RES_WLTP = "142afde2-6228-49f9-8a29-9b6c3a0cbe40"
    private const val RES_HISTORY = "56063a99-8a3e-4ff4-912e-5966c0279bad"
    private const val RES_OWNERSHIP = "bb2355dc-9ec7-4f06-9c3f-3344672171da"
    private const val RES_DISABLED_TAG = "c8b9f9c8-4612-4068-934f-d4acd2e3c06e"
    private const val RES_SUMMARY = "0866573c-40cd-4ca8-91d2-9dd2d7a492e5"
    private const val RES_IMPORTER = "39f455bf-6db0-4926-859d-017f34eacbcb"
    private const val RES_STATS = "5e87a7a1-2f6f-41c1-8aec-7216d52a6cf6"

    private val gson = Gson()

    fun getCached(plateNumber: String): VehicleInfo? = synchronized(cacheLock) {
        cache[plateNumber]
    }

    fun isKnown(plateNumber: String): Boolean = synchronized(cacheLock) {
        cache.containsKey(plateNumber)
    }

    fun isLoading(plateNumber: String): Boolean {
        return inFlight.contains(plateNumber)
    }

    suspend fun fetchIfNeeded(plateNumber: String, country: SupportedCountry): VehicleInfo? {
        synchronized(cacheLock) {
            if (cache.containsKey(plateNumber)) return cache[plateNumber]
        }

        mutex.withLock {
            if (plateNumber in inFlight) return null
            inFlight.add(plateNumber)
        }

        return try {
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "Fetching plate: $plateNumber (${country.code})")
            val info = when (country) {
                SupportedCountry.ISRAEL -> fetchIsrael(plateNumber)
                SupportedCountry.NETHERLANDS -> fetchNetherlands(plateNumber)
                SupportedCountry.UK -> fetchUk(plateNumber)
            }
            synchronized(cacheLock) { cache[plateNumber] = info }
            if (info != null) {
                if (BuildConfig.DEBUG) Log.d("VehicleCache", "Found: ${info.manufacturer} ${info.model} ${info.year}")
            } else {
                if (BuildConfig.DEBUG) Log.d("VehicleCache", "No record found for $plateNumber")
            }
            info
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("VehicleCache", "API error for $plateNumber (${country.code})", e)
            synchronized(cacheLock) { cache[plateNumber] = null }
            null
        } finally {
            mutex.withLock { inFlight.remove(plateNumber) }
        }
    }

    private suspend fun fetchIsrael(plateNumber: String): VehicleInfo? {
        // Step 1: Fetch main registration data
        val filters = """{"mispar_rechev":$plateNumber}"""
        val response = RetrofitClient.israelApi.searchVehicle(
            resourceId = RES_MAIN,
            filters = filters
        )
        val mainRecord = response.result?.records?.firstOrNull() ?: return null
        val baseInfo = mainRecord.toVehicleInfo()

        // Extract join keys for WLTP/importer/stats queries
        val tozeretCd = mainRecord.tozeret_cd
        val degemCd = mainRecord.degem_cd
        val shnatYitzur = mainRecord.shnat_yitzur
        val sugDegem = mainRecord.sug_degem ?: "P"

        // Step 2: Fetch all secondary resources IN PARALLEL
        return coroutineScope {
            val wltpDeferred = async { fetchWltpSafe(sugDegem, tozeretCd, degemCd, shnatYitzur) }
            val historyDeferred = async { fetchHistorySafe(plateNumber) }
            val ownershipDeferred = async { fetchOwnershipSafe(plateNumber) }
            val disabledTagDeferred = async { fetchDisabledTagSafe(plateNumber) }
            val summaryDeferred = async { fetchSummarySafe(plateNumber) }
            val importerDeferred = async { fetchImporterSafe(sugDegem, tozeretCd, degemCd, shnatYitzur) }
            val statsDeferred = async { fetchStatsSafe(sugDegem, tozeretCd, degemCd, shnatYitzur) }

            val wltp = wltpDeferred.await()
            val history = historyDeferred.await()
            val ownership = ownershipDeferred.await()
            val hasDisabledTag = disabledTagDeferred.await()
            val summary = summaryDeferred.await()
            val importer = importerDeferred.await()
            val stats = statsDeferred.await()

            // Step 3: Merge all data into VehicleInfo
            baseInfo.copy(
                // WLTP specs
                horsepower = wltp?.koah_sus,
                engineDisplacement = wltp?.nefah_manoa,
                driveType = wltp?.hanaa_nm,
                driveTechnology = wltp?.technologiat_hanaa_nm,
                standardType = wltp?.sug_tkina_nm,
                transmission = when (wltp?.automatic_ind) { 1 -> "אוטומטית"; 0 -> "ידנית"; else -> null },
                sunroof = wltp?.halon_bagg_ind?.let { it == 1 },
                alloyWheels = wltp?.galgaley_sagsoget_kala_ind?.let { it == 1 },
                electricWindows = wltp?.mispar_halonot_hashmal,
                tirePressureSensors = wltp?.hayshaney_lahatz_avir_batzmigim_ind?.let { it == 1 },
                reverseCamera = wltp?.matzlemat_reverse_ind?.let { it == 1 },
                towingWithBrakes = wltp?.kosher_grira_im_blamim,
                towingWithoutBrakes = wltp?.kosher_grira_bli_blamim,
                licensingGroup = wltp?.kvuzat_agra_cd,
                safetyScore = wltp?.nikud_betihut,
                safetyRating = wltp?.ramat_eivzur_betihuty ?: baseInfo.safetyRating,
                countryOfOrigin = wltp?.tozeret_eretz_nm,
                greenIndex = wltp?.madad_yarok,
                bodyType = wltp?.merkav ?: baseInfo.bodyType,
                numDoors = wltp?.mispar_dlatot ?: baseInfo.numDoors,
                numSeats = wltp?.mispar_moshavim ?: baseInfo.numSeats,
                weight = wltp?.mishkal_kolel ?: baseInfo.weight,
                // Safety systems
                airbagCount = wltp?.mispar_kariot_avir,
                abs = wltp?.abs_ind?.let { it == 1 },
                laneDeparture = wltp?.bakarat_stiya_menativ_ind?.let { it == 1 },
                stabilityControl = wltp?.bakarat_yatzivut_ind?.let { it == 1 },
                forwardDistanceMonitoring = wltp?.nitur_merhak_milfanim_ind?.let { it == 1 },
                adaptiveCruise = wltp?.bakarat_shyut_adaptivit_ind?.let { it == 1 },
                pedestrianDetection = wltp?.zihuy_holchey_regel_ind?.let { it == 1 },
                blindSpotDetection = wltp?.zihuy_beshetah_nistar_ind?.let { it == 1 },
                // History
                engineNumber = history?.mispar_manoa,
                lastTestKm = history?.kilometer_test_aharon,
                lpgAdded = history?.gapam_ind?.let { it == 1 },
                colorChanged = history?.shnui_zeva_ind?.let { it == 1 },
                tiresChanged = history?.shinui_zmig_ind?.let { it == 1 },
                originality = history?.mkoriut_nm,
                // Tow hook
                towHook = summary?.grira_nm,
                // Importer & price
                importerName = importer?.shem_yevuan,
                priceAtRegistration = importer?.mehir,
                // Ownership history
                ownershipHistory = ownership,
                // Disabled tag
                disabledTag = hasDisabledTag,
                // Statistics
                activeVehiclesCount = stats?.mispar_rechavim_pailim
            )
        }
    }

    // === Secondary fetch methods (each with independent error handling) ===

    private suspend fun fetchWltpSafe(sugDegem: String, tozeretCd: Int?, degemCd: Int?, shnatYitzur: Int?): WltpRecord? {
        if (tozeretCd == null || degemCd == null || shnatYitzur == null) return null
        val modelKey = "${sugDegem}_${tozeretCd}_${degemCd}_$shnatYitzur"
        synchronized(cacheLock) { wltpCache[modelKey]?.let { return it } }
        return try {
            val filters = """{"sug_degem":"$sugDegem","tozeret_cd":$tozeretCd,"degem_cd":$degemCd,"shnat_yitzur":$shnatYitzur}"""
            val response = RetrofitClient.israelApi.searchRaw(resourceId = RES_WLTP, filters = filters)
            val record = parseRecords<WltpRecord>(response)?.firstOrNull()
            synchronized(cacheLock) { wltpCache[modelKey] = record }
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "WLTP: ${if (record != null) "found" else "empty"} for $modelKey")
            record
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "WLTP fetch failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchHistorySafe(plateNumber: String): VehicleHistoryRecord? {
        return try {
            val filters = """{"mispar_rechev":$plateNumber}"""
            val response = RetrofitClient.israelApi.searchRaw(resourceId = RES_HISTORY, filters = filters)
            val record = parseRecords<VehicleHistoryRecord>(response)?.firstOrNull()
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "History: ${if (record != null) "found" else "empty"}")
            record
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "History fetch failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchOwnershipSafe(plateNumber: String): List<OwnershipRecord>? {
        return try {
            val filters = """{"mispar_rechev":$plateNumber}"""
            val response = RetrofitClient.israelApi.searchRaw(resourceId = RES_OWNERSHIP, filters = filters)
            val records = parseRecords<OwnershipHistoryRecord>(response)
            if (records.isNullOrEmpty()) return null
            val sorted = records.sortedBy { it.baalut_dt }
            sorted.map { record ->
                val dt = record.baalut_dt?.toString() ?: ""
                val formatted = if (dt.length == 6) "${dt.substring(4,6)}/${dt.substring(0,4)}" else dt
                OwnershipRecord(date = formatted, type = record.baalut ?: "")
            }.also {
                if (BuildConfig.DEBUG) Log.d("VehicleCache", "Ownership: ${it.size} records")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "Ownership fetch failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchDisabledTagSafe(plateNumber: String): Boolean? {
        return try {
            // Note: field name has a space in this dataset
            val filters = """{"MISPAR RECHEV":$plateNumber}"""
            val response = RetrofitClient.israelApi.searchRaw(resourceId = RES_DISABLED_TAG, filters = filters)
            val records = parseRecords<Any>(response)
            val hasTag = !records.isNullOrEmpty()
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "DisabledTag: $hasTag")
            hasTag
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "DisabledTag fetch failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchSummarySafe(plateNumber: String): VehicleSummaryRecord? {
        return try {
            val filters = """{"mispar_rechev":$plateNumber}"""
            val response = RetrofitClient.israelApi.searchRaw(resourceId = RES_SUMMARY, filters = filters)
            val record = parseRecords<VehicleSummaryRecord>(response)?.firstOrNull()
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "Summary: ${if (record != null) "found" else "empty"}")
            record
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "Summary fetch failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchImporterSafe(sugDegem: String, tozeretCd: Int?, degemCd: Int?, shnatYitzur: Int?): ImporterPriceRecord? {
        if (tozeretCd == null || degemCd == null || shnatYitzur == null) return null
        val modelKey = "${sugDegem}_${tozeretCd}_${degemCd}_$shnatYitzur"
        synchronized(cacheLock) { importerCache[modelKey]?.let { return it } }
        return try {
            val filters = """{"sug_degem":"$sugDegem","tozeret_cd":$tozeretCd,"degem_cd":$degemCd,"shnat_yitzur":$shnatYitzur}"""
            val response = RetrofitClient.israelApi.searchRaw(resourceId = RES_IMPORTER, filters = filters)
            val record = parseRecords<ImporterPriceRecord>(response)?.firstOrNull()
            synchronized(cacheLock) { importerCache[modelKey] = record }
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "Importer: ${record?.shem_yevuan ?: "empty"}")
            record
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "Importer fetch failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchStatsSafe(sugDegem: String, tozeretCd: Int?, degemCd: Int?, shnatYitzur: Int?): VehicleStatsRecord? {
        if (tozeretCd == null || degemCd == null || shnatYitzur == null) return null
        val modelKey = "${sugDegem}_${tozeretCd}_${degemCd}_$shnatYitzur"
        synchronized(cacheLock) { statsCache[modelKey]?.let { return it } }
        return try {
            val filters = """{"sug_degem":"$sugDegem","tozeret_cd":$tozeretCd,"degem_cd":$degemCd,"shnat_yitzur":$shnatYitzur}"""
            val response = RetrofitClient.israelApi.searchRaw(resourceId = RES_STATS, filters = filters)
            val record = parseRecords<VehicleStatsRecord>(response)?.firstOrNull()
            synchronized(cacheLock) { statsCache[modelKey] = record }
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "Stats: ${record?.mispar_rechavim_pailim ?: "empty"}")
            record
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "Stats fetch failed: ${e.message}")
            null
        }
    }

    /**
     * Parse records from a raw JsonObject API response into a list of typed objects.
     * The CKAN API returns: { success: true, result: { records: [...] } }
     */
    private inline fun <reified T> parseRecords(response: com.google.gson.JsonObject): List<T>? {
        return try {
            val success = response.get("success")?.asBoolean ?: false
            if (!success) return null
            val result = response.getAsJsonObject("result") ?: return null
            val recordsArray = result.getAsJsonArray("records") ?: return null
            val type = TypeToken.getParameterized(List::class.java, T::class.java).type
            gson.fromJson<List<T>>(recordsArray, type)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "parseRecords failed for ${T::class.simpleName}: ${e.message}")
            null
        }
    }

    private suspend fun fetchNetherlands(plateNumber: String): VehicleInfo? {
        // RDW expects plate without dashes, uppercase
        val kenteken = plateNumber.uppercase()
        var mainRecord: com.carinfo.ar.data.model.RdwVehicleRecord? = null
        var usedKenteken = kenteken

        // Try main plate first
        val records = RetrofitClient.rdwApi.searchVehicle(kenteken = kenteken)
        if (records.isNotEmpty()) {
            mainRecord = records.first()
        } else {
            // OCR confuses O/0, I/1 — try variants
            val swaps = mapOf('O' to '0', '0' to 'O', 'I' to '1', '1' to 'I')
            for (i in kenteken.indices) {
                val swap = swaps[kenteken[i]] ?: continue
                val variant = kenteken.substring(0, i) + swap + kenteken.substring(i + 1)
                if (BuildConfig.DEBUG) Log.d("VehicleCache", "NL: trying variant $variant")
                val variantRecords = RetrofitClient.rdwApi.searchVehicle(kenteken = variant)
                if (variantRecords.isNotEmpty()) {
                    mainRecord = variantRecords.first()
                    usedKenteken = variant
                    break
                }
            }
        }

        if (mainRecord == null) return null
        val baseInfo = mainRecord.toVehicleInfo()

        // Fetch secondary resources in parallel
        return coroutineScope {
            val fuelDeferred = async { fetchNlFuelSafe(usedKenteken) }
            val recallDeferred = async { fetchNlRecallsSafe(usedKenteken) }

            val fuel = fuelDeferred.await()
            val recalls = recallDeferred.await()

            baseInfo.copy(
                // Fuel/emissions data
                co2Emissions = fuel?.co2_uitstoot_gecombineerd ?: baseInfo.co2Emissions,
                enginePowerKw = fuel?.nettomaximumvermogen?.toDoubleOrNull(),
                horsepower = fuel?.nettomaximumvermogen?.toDoubleOrNull()?.let { (it * 1.36).toInt() },
                euroEmissionClass = fuel?.uitlaatemissieniveau,
                fuelConsumptionCombined = fuel?.brandstofverbruik_gecombineerd,
                fuelConsumptionCity = fuel?.brandstofverbruik_stad,
                fuelConsumptionHighway = fuel?.brandstofverbruik_buiten,
                // Recall status
                recallStatus = recalls
            )
        }
    }

    private suspend fun fetchNlFuelSafe(kenteken: String): com.carinfo.ar.data.model.RdwFuelRecord? {
        return try {
            val records = RetrofitClient.rdwApi.searchFuel(kenteken = kenteken)
            val record = records.firstOrNull()
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "NL Fuel: ${if (record != null) "found" else "empty"}")
            record
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "NL Fuel fetch failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchNlRecallsSafe(kenteken: String): String? {
        return try {
            val records = RetrofitClient.rdwApi.searchRecalls(kenteken = kenteken)
            if (records.isEmpty()) {
                if (BuildConfig.DEBUG) Log.d("VehicleCache", "NL Recalls: none")
                null
            } else {
                val status = records.joinToString("; ") { it.status ?: "Unknown" }
                if (BuildConfig.DEBUG) Log.d("VehicleCache", "NL Recalls: ${records.size} found")
                status
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("VehicleCache", "NL Recalls fetch failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchUk(plateNumber: String): VehicleInfo? {
        if (dvlaApiKey.isEmpty()) {
            Log.w("VehicleCache", "DVLA API key not set - UK lookups disabled")
            return null
        }
        val response = RetrofitClient.dvlaApi.searchVehicle(
            apiKey = dvlaApiKey,
            request = DvlaRequest(registrationNumber = plateNumber)
        )
        return response.toVehicleInfo()
    }
}
