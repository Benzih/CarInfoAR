package com.carinfo.ar.data.model

// ============ Universal vehicle info (used by UI) ============

data class VehicleInfo(
    val manufacturer: String?,
    val model: String?,
    val year: Int?,
    val color: String?,
    val fuelType: String?,
    val country: String  // "IL", "NL", "GB", "US"
)

// ============ Israel: data.gov.il ============

data class DataStoreResponse(
    val success: Boolean,
    val result: DataStoreResult
)

data class DataStoreResult(
    val records: List<VehicleRecord>
)

data class VehicleRecord(
    val mispar_rechev: Long,
    val tozeret_nm: String?,
    val kinuy_mishari: String?,
    val degem_nm: String?,
    val shnat_yitzur: Int?,
    val tzeva_rechev: String?,
    val sug_delek_nm: String?,
    val ramat_gimur: String?
) {
    fun toVehicleInfo() = VehicleInfo(
        manufacturer = tozeret_nm,
        model = kinuy_mishari ?: degem_nm,
        year = shnat_yitzur,
        color = tzeva_rechev,
        fuelType = sug_delek_nm,
        country = "IL"
    )
}

// ============ Netherlands: RDW Open Data ============

data class RdwVehicleRecord(
    val kenteken: String?,
    val merk: String?,             // brand
    val handelsbenaming: String?,  // model name
    val eerste_kleur: String?,     // color
    val brandstof_omschrijving: String?, // fuel
    val datum_eerste_toelating: String?  // first registration YYYYMMDD
) {
    fun toVehicleInfo() = VehicleInfo(
        manufacturer = merk,
        model = handelsbenaming,
        year = datum_eerste_toelating?.take(4)?.toIntOrNull(),
        color = eerste_kleur,
        fuelType = brandstof_omschrijving,
        country = "NL"
    )
}

// ============ UK: DVLA VES ============

data class DvlaVehicleResponse(
    val registrationNumber: String?,
    val make: String?,
    val colour: String?,
    val yearOfManufacture: Int?,
    val fuelType: String?,
    val engineCapacity: Int?,
    val co2Emissions: Int?,
    val taxStatus: String?,
    val motStatus: String?
) {
    fun toVehicleInfo() = VehicleInfo(
        manufacturer = make,
        model = null,  // DVLA doesn't return model
        year = yearOfManufacture,
        color = colour,
        fuelType = fuelType,
        country = "GB"
    )
}

// ============ USA: NHTSA vPIC (VIN decode) ============

data class NhtsaDecodeResponse(
    val Results: List<NhtsaResult>?
)

data class NhtsaResult(
    val Variable: String?,
    val Value: String?
)

// USA: Plate to VIN (via third party)
data class PlateToVinResponse(
    val vin: String?
)
