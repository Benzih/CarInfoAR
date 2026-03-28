package com.carinfo.ar.data.model

// ============ Universal vehicle info (used by UI) ============

data class OwnershipRecord(
    val date: String,    // formatted date (e.g. "08/2024")
    val type: String     // ownership type (פרטי, ליסינג, etc.)
)

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
    val bodyType: String? = null,          // inrichting (NL), wheelplan (GB), merkav (IL WLTP)
    val insured: String? = null,           // wam_verzekerd (NL)
    val wheelbase: Int? = null,            // wielbasis (NL)

    // === Israel Extended: WLTP Specs (resource 142afde2) ===
    val horsepower: Int? = null,           // koah_sus
    val engineDisplacement: Int? = null,   // nefah_manoa
    val driveType: String? = null,         // hanaa_nm (4X2, 4X4)
    val driveTechnology: String? = null,   // technologiat_hanaa_nm
    val standardType: String? = null,      // sug_tkina_nm (אירופאית)
    val transmission: String? = null,      // automatic_ind → "אוטומטית"/"ידנית"
    val sunroof: Boolean? = null,          // halon_bagg_ind
    val alloyWheels: Boolean? = null,      // galgaley_sagsoget_kala_ind
    val electricWindows: Int? = null,      // mispar_halonot_hashmal
    val tirePressureSensors: Boolean? = null, // hayshaney_lahatz_avir
    val reverseCamera: Boolean? = null,    // matzlemat_reverse_ind
    val towingWithBrakes: Int? = null,     // kosher_grira_im_blamim
    val towingWithoutBrakes: Int? = null,  // kosher_grira_bli_blamim
    val licensingGroup: Int? = null,       // kvuzat_agra_cd
    val safetyScore: Int? = null,          // nikud_betihut
    val safetyRating: Int? = null,         // ramat_eivzur_betihuty
    val countryOfOrigin: String? = null,   // tozeret_eretz_nm
    val greenIndex: Int? = null,           // madad_yarok

    // === Israel Extended: Safety systems (WLTP) ===
    val airbagCount: Int? = null,          // mispar_kariot_avir
    val abs: Boolean? = null,              // abs_ind
    val laneDeparture: Boolean? = null,    // bakarat_stiya_menativ_ind
    val stabilityControl: Boolean? = null, // bakarat_yatzivut_ind
    val forwardDistanceMonitoring: Boolean? = null, // nitur_merhak_milfanim_ind
    val adaptiveCruise: Boolean? = null,   // bakarat_shyut_adaptivit_ind
    val pedestrianDetection: Boolean? = null, // zihuy_holchey_regel_ind
    val blindSpotDetection: Boolean? = null, // zihuy_beshetah_nistar_ind

    // === Israel Extended: History (resource 56063a99) ===
    val engineNumber: String? = null,      // mispar_manoa
    val lastTestKm: Int? = null,           // kilometer_test_aharon
    val lpgAdded: Boolean? = null,         // gapam_ind
    val colorChanged: Boolean? = null,     // shnui_zeva_ind
    val tiresChanged: Boolean? = null,     // shinui_zmig_ind
    val originality: String? = null,       // mkoriut_nm

    // === Israel Extended: Tow hook (resource 0866573c) ===
    val towHook: String? = null,           // grira_nm

    // === Israel Extended: Importer & Price (resource 39f455bf) ===
    val importerName: String? = null,      // shem_yevuan
    val priceAtRegistration: Int? = null,  // mehir

    // === Israel Extended: Ownership history (resource bb2355dc) ===
    val ownershipHistory: List<OwnershipRecord>? = null,

    // === Israel Extended: Disabled tag (resource c8b9f9c8) ===
    val disabledTag: Boolean? = null,

    // === Israel Extended: Statistics (resource 5e87a7a1) ===
    val activeVehiclesCount: Int? = null,   // mispar_rechavim_pailim

    // === Israel Extended: Extra codes from main resource ===
    val modelCode: String? = null,         // degem_cd
    val manufacturerCode: Int? = null,     // tozeret_cd
    val registrationDirective: Int? = null, // horaat_rishum

    // === Netherlands Extended (RDW) ===
    val secondaryColor: String? = null,         // tweede_kleur
    val vehicleLength: Int? = null,             // lengte (cm)
    val vehicleWidth: Int? = null,              // breedte (cm)
    val vehicleHeight: Int? = null,             // hoogte_voertuig (cm)
    val purchaseTax: Int? = null,               // bruto_bpm (BPM)
    val ownerRegistrationDate: String? = null,  // datum_tenaamstelling
    val hasOpenRecall: Boolean? = null,         // openstaande_terugroepactie_indicator
    val odometerJudgment: String? = null,       // tellerstandoordeel
    val odometerYear: Int? = null,              // jaar_laatste_registratie_tellerstand
    val fuelEfficiencyClass: String? = null,    // zuinigheidsclassificatie (A-G)
    val isExported: Boolean? = null,            // export_indicator
    val isTaxi: Boolean? = null,                // taxi_indicator
    val maxTowingBraked: Int? = null,           // maximum_trekken_massa_geremd
    val maxTowingUnbraked: Int? = null,         // maximum_massa_trekken_ongeremd
    val euCategory: String? = null,             // europese_voertuigcategorie
    val emptyMass: Int? = null,                 // massa_ledig_voertuig
    val enginePowerKw: Double? = null,          // nettomaximumvermogen
    val euroEmissionClass: String? = null,      // uitlaatemissieniveau
    val fuelConsumptionCombined: String? = null, // brandstofverbruik_gecombineerd
    val fuelConsumptionCity: String? = null,     // brandstofverbruik_stad
    val fuelConsumptionHighway: String? = null,  // brandstofverbruik_buiten
    val recallStatus: String? = null,             // terugroepactie status

    // === UK Extended (DVLA) ===
    val markedForExport: Boolean? = null,        // markedForExport
    val v5cDate: String? = null,                 // dateOfLastV5CIssued
    val typeApproval: String? = null             // typeApproval
)

// ============ Israel: data.gov.il — Main registration ============

data class DataStoreResponse(
    val success: Boolean,
    val result: DataStoreResult?
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
    val kvutzat_zihum: Int? = null,
    // Join keys for secondary resources
    val tozeret_cd: Int? = null,
    val degem_cd: Int? = null,
    val sug_degem: String? = null,
    val horaat_rishum: Int? = null,
    val ramat_eivzur_betihuty: Int? = null
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
        emissionGroup = kvutzat_zihum,
        safetyRating = ramat_eivzur_betihuty,
        modelCode = degem_cd?.toString(),
        manufacturerCode = tozeret_cd,
        registrationDirective = horaat_rishum
    )
}

// ============ Israel: WLTP Specs (resource 142afde2) ============

data class WltpRecord(
    val koah_sus: Int? = null,
    val nefah_manoa: Int? = null,
    val hanaa_nm: String? = null,
    val technologiat_hanaa_nm: String? = null,
    val sug_tkina_nm: String? = null,
    val automatic_ind: Int? = null,
    val halon_bagg_ind: Int? = null,
    val galgaley_sagsoget_kala_ind: Int? = null,
    val mispar_halonot_hashmal: Int? = null,
    val hayshaney_lahatz_avir_batzmigim_ind: Int? = null,
    val matzlemat_reverse_ind: Int? = null,
    val kosher_grira_im_blamim: Int? = null,
    val kosher_grira_bli_blamim: Int? = null,
    val kvuzat_agra_cd: Int? = null,
    val nikud_betihut: Int? = null,
    val ramat_eivzur_betihuty: Int? = null,
    val tozeret_eretz_nm: String? = null,
    val madad_yarok: Int? = null,
    val merkav: String? = null,
    val mispar_dlatot: Int? = null,
    val mispar_moshavim: Int? = null,
    val mishkal_kolel: Int? = null,
    // Safety systems
    val mispar_kariot_avir: Int? = null,
    val abs_ind: Int? = null,
    val bakarat_stiya_menativ_ind: Int? = null,
    val bakarat_yatzivut_ind: Int? = null,
    val nitur_merhak_milfanim_ind: Int? = null,
    val bakarat_shyut_adaptivit_ind: Int? = null,
    val zihuy_holchey_regel_ind: Int? = null,
    val zihuy_beshetah_nistar_ind: Int? = null
)

// ============ Israel: Vehicle History (resource 56063a99) ============

data class VehicleHistoryRecord(
    val mispar_rechev: Long? = null,
    val mispar_manoa: String? = null,
    val kilometer_test_aharon: Int? = null,
    val shinui_mivne_ind: Int? = null,
    val gapam_ind: Int? = null,
    val shnui_zeva_ind: Int? = null,
    val shinui_zmig_ind: Int? = null,
    val rishum_rishon_dt: String? = null,
    val mkoriut_nm: String? = null
)

// ============ Israel: Ownership History (resource bb2355dc) ============

data class OwnershipHistoryRecord(
    val mispar_rechev: Long? = null,
    val baalut_dt: Long? = null,   // YYYYMM format
    val baalut: String? = null
)

// ============ Israel: Disabled Tag (resource c8b9f9c8) ============

data class DisabledTagRecord(
    val MISPAR_RECHEV: Long? = null,
    val TAARICH_HAFAKAT_TAG: Long? = null,
    val SUG_TAV: Int? = null
)

// ============ Israel: Vehicle Summary — tow hook (resource 0866573c) ============

data class VehicleSummaryRecord(
    val mispar_rechev: Long? = null,
    val grira_nm: String? = null
)

// ============ Israel: Importer & Price (resource 39f455bf) ============

data class ImporterPriceRecord(
    val shem_yevuan: String? = null,
    val mehir: Int? = null
)

// ============ Israel: Vehicle Statistics (resource 5e87a7a1) ============

data class VehicleStatsRecord(
    val mispar_rechavim_pailim: Int? = null,
    val mispar_rechavim_le_pailim: Int? = null
)

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
    val wielbasis: Int? = null,
    // Extended fields from same dataset
    val tweede_kleur: String? = null,
    val lengte: Int? = null,
    val breedte: Int? = null,
    val hoogte_voertuig: Int? = null,
    val bruto_bpm: Int? = null,
    val datum_tenaamstelling: String? = null,
    val openstaande_terugroepactie_indicator: String? = null,
    val tellerstandoordeel: String? = null,
    val jaar_laatste_registratie_tellerstand: Int? = null,
    val zuinigheidsclassificatie: String? = null,
    val export_indicator: String? = null,
    val taxi_indicator: String? = null,
    val maximum_trekken_massa_geremd: Int? = null,
    val maximum_massa_trekken_ongeremd: Int? = null,
    val europese_voertuigcategorie: String? = null,
    val massa_ledig_voertuig: Int? = null,
    val aantal_wielen: Int? = null,
    val voertuigsoort: String? = null
) {
    private fun formatRdwDate(d: String?): String? = d?.let {
        if (it.length == 8) "${it.substring(0,4)}-${it.substring(4,6)}-${it.substring(6,8)}" else it
    }

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
        testValidUntil = formatRdwDate(vervaldatum_apk),
        catalogPrice = catalogusprijs,
        weight = massa_rijklaar,
        bodyType = inrichting,
        insured = wam_verzekerd,
        wheelbase = wielbasis,
        onRoadDate = formatRdwDate(datum_eerste_toelating),
        // Extended NL fields
        secondaryColor = tweede_kleur,
        vehicleLength = lengte,
        vehicleWidth = breedte,
        vehicleHeight = hoogte_voertuig,
        purchaseTax = bruto_bpm,
        ownerRegistrationDate = formatRdwDate(datum_tenaamstelling),
        hasOpenRecall = openstaande_terugroepactie_indicator?.lowercase() == "ja",
        odometerJudgment = tellerstandoordeel,
        odometerYear = jaar_laatste_registratie_tellerstand,
        fuelEfficiencyClass = zuinigheidsclassificatie,
        isExported = export_indicator?.lowercase() == "ja",
        isTaxi = taxi_indicator?.lowercase() == "ja",
        maxTowingBraked = maximum_trekken_massa_geremd,
        maxTowingUnbraked = maximum_massa_trekken_ongeremd,
        euCategory = europese_voertuigcategorie,
        emptyMass = massa_ledig_voertuig
    )
}

// ============ Netherlands: RDW Fuel/Emissions (resource 8ys7-d773) ============

data class RdwFuelRecord(
    val kenteken: String? = null,
    val brandstof_omschrijving: String? = null,
    val co2_uitstoot_gecombineerd: Int? = null,
    val brandstofverbruik_gecombineerd: String? = null,
    val brandstofverbruik_stad: String? = null,
    val brandstofverbruik_buiten: String? = null,
    val nettomaximumvermogen: String? = null,
    val uitlaatemissieniveau: String? = null,
    val geluidsniveau_stationair: String? = null
)

// ============ Netherlands: RDW Recall Status (resource t49b-isb7) ============

data class RdwRecallRecord(
    val kenteken: String? = null,
    val referentiecode_rdw: String? = null,
    val status: String? = null
)

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
        onRoadDate = monthOfFirstRegistration,
        markedForExport = markedForExport,
        v5cDate = dateOfLastV5CIssued,
        typeApproval = typeApproval
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
