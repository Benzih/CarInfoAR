package com.carinfo.ar.data.api

import com.carinfo.ar.data.model.DvlaVehicleResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DvlaApi {
    // DVLA Vehicle Enquiry Service - free with API key
    @POST("vehicle-enquiry/v1/vehicles")
    suspend fun searchVehicle(
        @Header("x-api-key") apiKey: String,
        @Body request: DvlaRequest
    ): DvlaVehicleResponse
}

data class DvlaRequest(
    val registrationNumber: String
)
