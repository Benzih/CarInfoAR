package com.carinfo.ar.data

import android.content.Context
import android.util.Log
import com.carinfo.ar.data.model.OwnershipRecord
import com.carinfo.ar.data.model.VehicleInfo
import com.google.gson.Gson
import java.io.File

private val historyLock = Any()

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
    val wheelbase: Int? = null,
    // Israel Extended fields
    val horsepower: Int? = null,
    val engineDisplacement: Int? = null,
    val driveType: String? = null,
    val driveTechnology: String? = null,
    val standardType: String? = null,
    val transmission: String? = null,
    val sunroof: Boolean? = null,
    val alloyWheels: Boolean? = null,
    val electricWindows: Int? = null,
    val tirePressureSensors: Boolean? = null,
    val reverseCamera: Boolean? = null,
    val towingWithBrakes: Int? = null,
    val towingWithoutBrakes: Int? = null,
    val licensingGroup: Int? = null,
    val safetyScore: Int? = null,
    val safetyRating: Int? = null,
    val countryOfOrigin: String? = null,
    val greenIndex: Int? = null,
    val airbagCount: Int? = null,
    val abs: Boolean? = null,
    val laneDeparture: Boolean? = null,
    val stabilityControl: Boolean? = null,
    val forwardDistanceMonitoring: Boolean? = null,
    val adaptiveCruise: Boolean? = null,
    val pedestrianDetection: Boolean? = null,
    val blindSpotDetection: Boolean? = null,
    val engineNumber: String? = null,
    val lastTestKm: Int? = null,
    val lpgAdded: Boolean? = null,
    val colorChanged: Boolean? = null,
    val tiresChanged: Boolean? = null,
    val originality: String? = null,
    val towHook: String? = null,
    val importerName: String? = null,
    val priceAtRegistration: Int? = null,
    val ownershipHistory: List<OwnershipRecord>? = null,
    val disabledTag: Boolean? = null,
    val activeVehiclesCount: Int? = null,
    val modelCode: String? = null,
    val manufacturerCode: Int? = null,
    val registrationDirective: Int? = null,
    // Netherlands Extended
    val secondaryColor: String? = null,
    val vehicleLength: Int? = null,
    val vehicleWidth: Int? = null,
    val vehicleHeight: Int? = null,
    val purchaseTax: Int? = null,
    val ownerRegistrationDate: String? = null,
    val hasOpenRecall: Boolean? = null,
    val odometerJudgment: String? = null,
    val odometerYear: Int? = null,
    val fuelEfficiencyClass: String? = null,
    val isExported: Boolean? = null,
    val isTaxi: Boolean? = null,
    val maxTowingBraked: Int? = null,
    val maxTowingUnbraked: Int? = null,
    val euCategory: String? = null,
    val emptyMass: Int? = null,
    val enginePowerKw: Double? = null,
    val euroEmissionClass: String? = null,
    val fuelConsumptionCombined: String? = null,
    val fuelConsumptionCity: String? = null,
    val fuelConsumptionHighway: String? = null,
    val recallStatus: String? = null,
    // UK Extended
    val markedForExport: Boolean? = null,
    val v5cDate: String? = null,
    val typeApproval: String? = null
)

fun ScanRecord.toVehicleInfo() = VehicleInfo(
    manufacturer = manufacturer,
    model = model,
    year = year,
    color = color,
    fuelType = fuelType,
    country = country,
    trimLevel = trimLevel,
    ownership = ownership,
    lastTestDate = lastTestDate,
    testValidUntil = testValidUntil,
    engineModel = engineModel,
    engineCapacity = engineCapacity,
    chassisNumber = chassisNumber,
    frontTires = frontTires,
    rearTires = rearTires,
    onRoadDate = onRoadDate,
    emissionGroup = emissionGroup,
    co2Emissions = co2Emissions,
    taxStatus = taxStatus,
    taxDueDate = taxDueDate,
    motStatus = motStatus,
    numCylinders = numCylinders,
    numDoors = numDoors,
    numSeats = numSeats,
    catalogPrice = catalogPrice,
    weight = weight,
    bodyType = bodyType,
    insured = insured,
    wheelbase = wheelbase,
    horsepower = horsepower,
    engineDisplacement = engineDisplacement,
    driveType = driveType,
    driveTechnology = driveTechnology,
    standardType = standardType,
    transmission = transmission,
    sunroof = sunroof,
    alloyWheels = alloyWheels,
    electricWindows = electricWindows,
    tirePressureSensors = tirePressureSensors,
    reverseCamera = reverseCamera,
    towingWithBrakes = towingWithBrakes,
    towingWithoutBrakes = towingWithoutBrakes,
    licensingGroup = licensingGroup,
    safetyScore = safetyScore,
    safetyRating = safetyRating,
    countryOfOrigin = countryOfOrigin,
    greenIndex = greenIndex,
    airbagCount = airbagCount,
    abs = abs,
    laneDeparture = laneDeparture,
    stabilityControl = stabilityControl,
    forwardDistanceMonitoring = forwardDistanceMonitoring,
    adaptiveCruise = adaptiveCruise,
    pedestrianDetection = pedestrianDetection,
    blindSpotDetection = blindSpotDetection,
    engineNumber = engineNumber,
    lastTestKm = lastTestKm,
    lpgAdded = lpgAdded,
    colorChanged = colorChanged,
    tiresChanged = tiresChanged,
    originality = originality,
    towHook = towHook,
    importerName = importerName,
    priceAtRegistration = priceAtRegistration,
    ownershipHistory = ownershipHistory,
    disabledTag = disabledTag,
    activeVehiclesCount = activeVehiclesCount,
    modelCode = modelCode,
    manufacturerCode = manufacturerCode,
    registrationDirective = registrationDirective,
    secondaryColor = secondaryColor,
    vehicleLength = vehicleLength,
    vehicleWidth = vehicleWidth,
    vehicleHeight = vehicleHeight,
    purchaseTax = purchaseTax,
    ownerRegistrationDate = ownerRegistrationDate,
    hasOpenRecall = hasOpenRecall,
    odometerJudgment = odometerJudgment,
    odometerYear = odometerYear,
    fuelEfficiencyClass = fuelEfficiencyClass,
    isExported = isExported,
    isTaxi = isTaxi,
    maxTowingBraked = maxTowingBraked,
    maxTowingUnbraked = maxTowingUnbraked,
    euCategory = euCategory,
    emptyMass = emptyMass,
    enginePowerKw = enginePowerKw,
    euroEmissionClass = euroEmissionClass,
    fuelConsumptionCombined = fuelConsumptionCombined,
    fuelConsumptionCity = fuelConsumptionCity,
    fuelConsumptionHighway = fuelConsumptionHighway,
    recallStatus = recallStatus,
    markedForExport = markedForExport,
    v5cDate = v5cDate,
    typeApproval = typeApproval
)

object ScanHistory {
    private const val FILE_NAME = "scan_history.json"
    private val gson = Gson()

    private fun getFile(context: Context): File =
        File(context.filesDir, FILE_NAME)

    fun load(context: Context): List<ScanRecord> = synchronized(historyLock) {
        val file = getFile(context)
        if (!file.exists()) {
            Log.w("ScanHistory", "LOAD: file does not exist")
            return emptyList()
        }
        return try {
            val json = file.readText()
            Log.d("ScanHistory", "LOAD: file size=${json.length}, first100=${json.take(100)}")
            val array = gson.fromJson(json, Array<ScanRecord>::class.java)
            val result = array?.toList() ?: emptyList()
            Log.d("ScanHistory", "LOAD: parsed ${result.size} records")
            result
        } catch (e: Exception) {
            Log.e("ScanHistory", "LOAD FAILED: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }

    fun save(context: Context, plateNumber: String, info: VehicleInfo) = synchronized(historyLock) {
        Log.d("ScanHistory", "SAVE: plate=$plateNumber, manufacturer=${info.manufacturer}")
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
            wheelbase = info.wheelbase,
            // Israel Extended
            horsepower = info.horsepower,
            engineDisplacement = info.engineDisplacement,
            driveType = info.driveType,
            driveTechnology = info.driveTechnology,
            standardType = info.standardType,
            transmission = info.transmission,
            sunroof = info.sunroof,
            alloyWheels = info.alloyWheels,
            electricWindows = info.electricWindows,
            tirePressureSensors = info.tirePressureSensors,
            reverseCamera = info.reverseCamera,
            towingWithBrakes = info.towingWithBrakes,
            towingWithoutBrakes = info.towingWithoutBrakes,
            licensingGroup = info.licensingGroup,
            safetyScore = info.safetyScore,
            safetyRating = info.safetyRating,
            countryOfOrigin = info.countryOfOrigin,
            greenIndex = info.greenIndex,
            airbagCount = info.airbagCount,
            abs = info.abs,
            laneDeparture = info.laneDeparture,
            stabilityControl = info.stabilityControl,
            forwardDistanceMonitoring = info.forwardDistanceMonitoring,
            adaptiveCruise = info.adaptiveCruise,
            pedestrianDetection = info.pedestrianDetection,
            blindSpotDetection = info.blindSpotDetection,
            engineNumber = info.engineNumber,
            lastTestKm = info.lastTestKm,
            lpgAdded = info.lpgAdded,
            colorChanged = info.colorChanged,
            tiresChanged = info.tiresChanged,
            originality = info.originality,
            towHook = info.towHook,
            importerName = info.importerName,
            priceAtRegistration = info.priceAtRegistration,
            ownershipHistory = info.ownershipHistory,
            disabledTag = info.disabledTag,
            activeVehiclesCount = info.activeVehiclesCount,
            modelCode = info.modelCode,
            manufacturerCode = info.manufacturerCode,
            registrationDirective = info.registrationDirective,
            // Netherlands Extended
            secondaryColor = info.secondaryColor,
            vehicleLength = info.vehicleLength,
            vehicleWidth = info.vehicleWidth,
            vehicleHeight = info.vehicleHeight,
            purchaseTax = info.purchaseTax,
            ownerRegistrationDate = info.ownerRegistrationDate,
            hasOpenRecall = info.hasOpenRecall,
            odometerJudgment = info.odometerJudgment,
            odometerYear = info.odometerYear,
            fuelEfficiencyClass = info.fuelEfficiencyClass,
            isExported = info.isExported,
            isTaxi = info.isTaxi,
            maxTowingBraked = info.maxTowingBraked,
            maxTowingUnbraked = info.maxTowingUnbraked,
            euCategory = info.euCategory,
            emptyMass = info.emptyMass,
            enginePowerKw = info.enginePowerKw,
            euroEmissionClass = info.euroEmissionClass,
            fuelConsumptionCombined = info.fuelConsumptionCombined,
            fuelConsumptionCity = info.fuelConsumptionCity,
            fuelConsumptionHighway = info.fuelConsumptionHighway,
            recallStatus = info.recallStatus,
            // UK Extended
            markedForExport = info.markedForExport,
            v5cDate = info.v5cDate,
            typeApproval = info.typeApproval
        ))
        // Keep max 100
        val trimmed = records.take(100)
        val json = gson.toJson(trimmed)
        Log.d("ScanHistory", "SAVE: writing ${trimmed.size} records, json size=${json.length}")
        try {
            getFile(context).writeText(json)
            Log.d("ScanHistory", "SAVE: write SUCCESS")
        } catch (e: Exception) {
            Log.e("ScanHistory", "SAVE WRITE FAILED: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun delete(context: Context, plateNumber: String) = synchronized(historyLock) {
        val records = load(context).toMutableList()
        records.removeAll { it.plateNumber == plateNumber }
        getFile(context).writeText(gson.toJson(records))
    }

    fun clear(context: Context) = synchronized(historyLock) {
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
