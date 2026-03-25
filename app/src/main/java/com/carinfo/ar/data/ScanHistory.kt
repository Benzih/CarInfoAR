package com.carinfo.ar.data

import android.content.Context
import com.carinfo.ar.data.model.VehicleInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class ScanRecord(
    val plateNumber: String,
    val manufacturer: String?,
    val model: String?,
    val year: Int?,
    val color: String?,
    val fuelType: String?,
    val country: String,
    val timestamp: Long = System.currentTimeMillis(),
    // Extended fields
    val trimLevel: String? = null,
    val ownership: String? = null,
    val lastTestDate: String? = null,
    val testValidUntil: String? = null,
    val engineModel: String? = null,
    val engineCapacity: Int? = null,
    val chassisNumber: String? = null,
    val frontTires: String? = null,
    val rearTires: String? = null,
    val onRoadDate: String? = null,
    val emissionGroup: Int? = null,
    val co2Emissions: Int? = null,
    val taxStatus: String? = null,
    val taxDueDate: String? = null,
    val motStatus: String? = null,
    val numCylinders: Int? = null,
    val numDoors: Int? = null,
    val numSeats: Int? = null,
    val catalogPrice: Int? = null,
    val weight: Int? = null,
    val bodyType: String? = null,
    val insured: String? = null,
    val wheelbase: Int? = null
)

object ScanHistory {
    private const val FILE_NAME = "scan_history.json"
    private val gson = Gson()

    private fun getFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    fun load(context: Context): List<ScanRecord> {
        val file = getFile(context)
        if (!file.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<ScanRecord>>() {}.type
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, plateNumber: String, info: VehicleInfo) {
        val records = load(context).toMutableList()
        // Don't duplicate — update if exists
        records.removeAll { it.plateNumber == plateNumber }
        records.add(0, ScanRecord(
            plateNumber = plateNumber,
            manufacturer = info.manufacturer,
            model = info.model,
            year = info.year,
            color = info.color,
            fuelType = info.fuelType,
            country = info.country,
            trimLevel = info.trimLevel,
            ownership = info.ownership,
            lastTestDate = info.lastTestDate,
            testValidUntil = info.testValidUntil,
            engineModel = info.engineModel,
            engineCapacity = info.engineCapacity,
            chassisNumber = info.chassisNumber,
            frontTires = info.frontTires,
            rearTires = info.rearTires,
            onRoadDate = info.onRoadDate,
            emissionGroup = info.emissionGroup,
            co2Emissions = info.co2Emissions,
            taxStatus = info.taxStatus,
            taxDueDate = info.taxDueDate,
            motStatus = info.motStatus,
            numCylinders = info.numCylinders,
            numDoors = info.numDoors,
            numSeats = info.numSeats,
            catalogPrice = info.catalogPrice,
            weight = info.weight,
            bodyType = info.bodyType,
            insured = info.insured,
            wheelbase = info.wheelbase
        ))
        // Keep max 100
        val trimmed = records.take(100)
        getFile(context).writeText(gson.toJson(trimmed))
    }

    fun delete(context: Context, plateNumber: String) {
        val records = load(context).toMutableList()
        records.removeAll { it.plateNumber == plateNumber }
        getFile(context).writeText(gson.toJson(records))
    }

    fun clear(context: Context) {
        getFile(context).delete()
    }

    fun buildSearchUrl(info: VehicleInfo): String {
        val query = buildString {
            info.manufacturer?.let { append(it) }
            info.model?.let {
                if (isNotEmpty()) append(" ")
                append(it)
            }
            info.year?.let {
                if (isNotEmpty()) append(" ")
                append(it)
            }
        }
        return "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
    }
}
