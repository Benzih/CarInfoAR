package com.carinfo.ar.data.model

// ============ Universal vehicle info (used by UI) ============

data class VehicleInfo(
    val manufacturer: String?,
    val model: String?,
    val year: Int?,
    val color: String?,
    val fuelType: String?,
    val country: String,  // "IL", "NL", "GB", "US"
    // Extended fields
    val trimLevel: String? = null,        // ramat_gimur (IL)
    val ownership: String? = null,         // baalut (IL)
    val lastTestDate: String? = null,      // mivchan_acharon_dt (IL), motExpiryDate (GB), vervaldatum_apk (NL)
    val testValidUntil: String? = null,    // tokef_dt (IL), motExpiryDate (GB), vervaldatum_apk (NL)
    val engineModel: String? = null,       // degem_manoa (IL)
    val engineCapacity: Int? = null,       // cilinderinhoud (NL), engineCapacity (GB)
    val chassisNumber: String? = null,     // misgeret (IL)
    val frontTires: String? = null,        // zmig_kidmi (IL)
    val rearTires: String? = null,         // zmig_ahori (IL)
    val onRoadDate: String? = null,        // moed_aliya_lakvish (IL), monthOfFirstRegistration (GB), datum_eerste_toelating (NL)
    val emissionGroup: Int? = null,        // kvutzat_zihum (IL)
    val co2Emissions: Int? = null,         // co2Emissions (GB)
    val taxStatus: String? = null,         // taxStatus (GB)
    val taxDueDate: String? = null,        // taxDueDate (GB)
    val motStatus: String? = null,         // motStatus (GB)
    val numCylinders: Int? = null,         // aantal_cilinders (NL)
    val numDoors: Int? = null,             // aantal_deuren (NL)
    val numSeats: Int? = null,             // aantal_zitplaatsen (NL)
    val catalogPrice: Int? = null,         // catalogusprijs (NL)
    val weight: Int? = null,               // massa_rijklaar (NL)
    val bodyType: String? = null,          // inrichting (NL), wheelplan (GB)
    val insured: String? = null,           // wam_verzekerd (NL)
    val wheelbase: Int? = null             // wielbasis (NL)
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
    val ramat_gimur: String?,
    val baalut: String? = null,
    val mivchan_acharon_dt: String? = null,
    val tokef_dt: String? = null,
    val degem_manoa: String? = null,
    val misgeret: String? = null,
    val zmig_kidmi: String? = null,
    val zmig_ahori: String? = null,
    val moed_aliya_lakvish: String? = null,
    val kvutzat_zihum: Int? = null
) {
    fun toVehicleInfo() = VehicleInfo(
        manufacturer = tozeret_nm,
        model = kinuy_mishari ?: degem_nm,
        year = shnat_yitzur,
        color = tzeva_rechev,
        fuelType = sug_delek_nm,
        country = "IL",
        trimLevel = ramat_gimur,
        ownership = baalut,
        lastTestDate = mivchan_acharon_dt,
        testValidUntil = tokef_dt,
        engineModel = degem_manoa,
        chassisNumber = misgeret,
        frontTires = zmig_kidmi,
        rearTires = zmig_ahori,
        onRoadDate = moed_aliya_lakvish,
        emissionGroup = kvutzat_zihum
    )
}

// ============ Netherlands: RDW Open Data ============

data class RdwVehicleRecord(
    val kenteken: String?,
    val merk: String?,
    val handelsbenaming: String?,
    val eerste_kleur: String?,
    val brandstof_omschrijving: String?,
    val datum_eerste_toelating: String?,
    val cilinderinhoud: Int? = null,
    val aantal_cilinders: Int? = null,
    val aantal_deuren: Int? = null,
    val aantal_zitplaatsen: Int? = null,
    val vervaldatum_apk: String? = null,
    val catalogusprijs: Int? = null,
    val massa_rijklaar: Int? = null,
    val inrichting: String? = null,
    val wam_verzekerd: String? = null,
    val wielbasis: Int? = null
) {
    fun toVehicleInfo() = VehicleInfo(
        manufacturer = merk,
        model = handelsbenaming,
        year = datum_eerste_toelating?.take(4)?.toIntOrNull(),
        color = eerste_kleur,
        fuelType = brandstof_omschrijving,
        country = "NL",
        engineCapacity = cilinderinhoud,
        numCylinders = aantal_cilinders,
        numDoors = aantal_deuren,
        numSeats = aantal_zitplaatsen,
        testValidUntil = vervaldatum_apk?.let {
            if (it.length == 8) "${it.substring(0,4)}-${it.substring(4,6)}-${it.substring(6,8)}" else it
        },
        catalogPrice = catalogusprijs,
        weight = massa_rijklaar,
        bodyType = inrichting,
        insured = wam_verzekerd,
        wheelbase = wielbasis,
        onRoadDate = datum_eerste_toelating?.let {
            if (it.length == 8) "${it.substring(0,4)}-${it.substring(4,6)}-${it.substring(6,8)}" else it
        }
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
    val taxDueDate: String?,
    val motStatus: String?,
    val motExpiryDate: String?,
    val wheelplan: String?,
    val monthOfFirstRegistration: String?,
    val markedForExport: Boolean? = null,
    val dateOfLastV5CIssued: String? = null,
    val typeApproval: String? = null
) {
    fun toVehicleInfo() = VehicleInfo(
        manufacturer = make,
        model = null,
        year = yearOfManufacture,
        color = colour,
        fuelType = fuelType,
        country = "GB",
        engineCapacity = engineCapacity,
        co2Emissions = co2Emissions,
        taxStatus = taxStatus,
        taxDueDate = taxDueDate,
        motStatus = motStatus,
        testValidUntil = motExpiryDate,
        bodyType = wheelplan,
        onRoadDate = monthOfFirstRegistration
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
