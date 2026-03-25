package com.carinfo.ar.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    // Israel: data.gov.il
    val israelApi: GovIlApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://data.gov.il/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GovIlApi::class.java)
    }

    // Netherlands: RDW Open Data (Socrata)
    val rdwApi: RdwApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://opendata.rdw.nl/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RdwApi::class.java)
    }

    // UK: DVLA Vehicle Enquiry Service
    val dvlaApi: DvlaApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://driver-vehicle-licensing.api.gov.uk/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DvlaApi::class.java)
    }
}
