package com.carinfo.ar.data

import android.util.Log
import com.carinfo.ar.data.api.DvlaRequest
import com.carinfo.ar.data.api.RetrofitClient
import com.carinfo.ar.data.model.VehicleInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object VehicleCache {
    private val cache = mutableMapOf<String, VehicleInfo?>()
    private val inFlight = mutableSetOf<String>()
    private val mutex = Mutex()

    // DVLA API key - users need to register at https://developer-portal.driver-vehicle-licensing.api.gov.uk/
    // Leave empty to disable UK support
    var dvlaApiKey: String = ""

    fun getCached(plateNumber: String): VehicleInfo? {
        return cache[plateNumber]
    }

    fun isKnown(plateNumber: String): Boolean {
        return plateNumber in cache
    }

    fun isLoading(plateNumber: String): Boolean {
        return plateNumber in inFlight
    }

    suspend fun fetchIfNeeded(plateNumber: String, country: SupportedCountry): VehicleInfo? {
        if (plateNumber in cache) return cache[plateNumber]

        mutex.withLock {
            if (plateNumber in inFlight) return null
            inFlight.add(plateNumber)
        }

        return try {
            Log.d("VehicleCache", "Fetching plate: $plateNumber (${country.code})")
            val info = when (country) {
                SupportedCountry.ISRAEL -> fetchIsrael(plateNumber)
                SupportedCountry.NETHERLANDS -> fetchNetherlands(plateNumber)
                SupportedCountry.UK -> fetchUk(plateNumber)
            }
            cache[plateNumber] = info
            if (info != null) {
                Log.d("VehicleCache", "Found: ${info.manufacturer} ${info.model} ${info.year}")
            } else {
                Log.d("VehicleCache", "No record found for $plateNumber")
            }
            info
        } catch (e: Exception) {
            Log.e("VehicleCache", "API error for $plateNumber (${country.code})", e)
            null
        } finally {
            mutex.withLock { inFlight.remove(plateNumber) }
        }
    }

    private suspend fun fetchIsrael(plateNumber: String): VehicleInfo? {
        val filters = """{"mispar_rechev":$plateNumber}"""
        val response = RetrofitClient.israelApi.searchVehicle(filters = filters)
        return response.result.records.firstOrNull()?.toVehicleInfo()
    }

    private suspend fun fetchNetherlands(plateNumber: String): VehicleInfo? {
        // RDW expects plate without dashes, uppercase
        val kenteken = plateNumber.uppercase()
        val records = RetrofitClient.rdwApi.searchVehicle(kenteken = kenteken)
        return records.firstOrNull()?.toVehicleInfo()
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
