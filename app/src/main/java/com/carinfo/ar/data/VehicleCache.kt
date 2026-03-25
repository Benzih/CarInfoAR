package com.carinfo.ar.data

import android.util.Log
import com.carinfo.ar.BuildConfig
import com.carinfo.ar.data.api.DvlaRequest
import com.carinfo.ar.data.api.RetrofitClient
import com.carinfo.ar.data.model.VehicleInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object VehicleCache {
    // Using synchronized HashMap because ConcurrentHashMap doesn't allow null values
    // and we need to cache null for "not found" plates
    private val cache = HashMap<String, VehicleInfo?>()
    private val cacheLock = Any()
    private val inFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val mutex = Mutex()

    // DVLA API key - users need to register at https://developer-portal.driver-vehicle-licensing.api.gov.uk/
    // Leave empty to disable UK support
    var dvlaApiKey: String = "vgkhEOnZPp3rF5U7qXC848wHZ4RBV0kg5PtTKsCK"

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
        val filters = """{"mispar_rechev":$plateNumber}"""
        val response = RetrofitClient.israelApi.searchVehicle(filters = filters)
        return response.result?.records?.firstOrNull()?.toVehicleInfo()
    }

    private suspend fun fetchNetherlands(plateNumber: String): VehicleInfo? {
        // RDW expects plate without dashes, uppercase
        val kenteken = plateNumber.uppercase()
        val records = RetrofitClient.rdwApi.searchVehicle(kenteken = kenteken)
        if (records.isNotEmpty()) return records.first().toVehicleInfo()

        // OCR confuses O/0, I/1 — try variants
        val swaps = mapOf('O' to '0', '0' to 'O', 'I' to '1', '1' to 'I')
        for (i in kenteken.indices) {
            val swap = swaps[kenteken[i]] ?: continue
            val variant = kenteken.substring(0, i) + swap + kenteken.substring(i + 1)
            if (BuildConfig.DEBUG) Log.d("VehicleCache", "NL: trying variant $variant")
            val variantRecords = RetrofitClient.rdwApi.searchVehicle(kenteken = variant)
            if (variantRecords.isNotEmpty()) return variantRecords.first().toVehicleInfo()
        }
        return null
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
