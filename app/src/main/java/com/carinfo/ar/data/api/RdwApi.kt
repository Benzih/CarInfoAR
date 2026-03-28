package com.carinfo.ar.data.api

import com.carinfo.ar.data.model.RdwFuelRecord
import com.carinfo.ar.data.model.RdwRecallRecord
import com.carinfo.ar.data.model.RdwVehicleRecord
import retrofit2.http.GET
import retrofit2.http.Query

interface RdwApi {
    // RDW Open Data - Socrata SODA API - completely free, no auth

    @GET("resource/m9d7-ebf2.json")
    suspend fun searchVehicle(
        @Query("kenteken") kenteken: String
    ): List<RdwVehicleRecord>

    @GET("resource/8ys7-d773.json")
    suspend fun searchFuel(
        @Query("kenteken") kenteken: String
    ): List<RdwFuelRecord>

    @GET("resource/t49b-isb7.json")
    suspend fun searchRecalls(
        @Query("kenteken") kenteken: String
    ): List<RdwRecallRecord>
}
