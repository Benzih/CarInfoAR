package com.carinfo.ar.data.api

import com.carinfo.ar.data.model.DataStoreResponse
import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface GovIlApi {
    @GET("api/3/action/datastore_search")
    suspend fun searchVehicle(
        @Query("resource_id") resourceId: String = "053cea08-09bc-40ec-8f7a-156f0677aff3",
        @Query("filters") filters: String
    ): DataStoreResponse

    /**
     * Generic search that returns raw JSON — used for secondary resources
     * where the record structure differs from VehicleRecord.
     */
    @GET("api/3/action/datastore_search")
    suspend fun searchRaw(
        @Query("resource_id") resourceId: String,
        @Query("filters") filters: String
    ): JsonObject
}
