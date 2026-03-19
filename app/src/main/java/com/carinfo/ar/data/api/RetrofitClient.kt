package com.carinfo.ar.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Israel: data.gov.il
    val israelApi: GovIlApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://data.gov.il/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GovIlApi::class.java)
    }

    // Netherlands: RDW Open Data (Socrata)
    val rdwApi: RdwApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://opendata.rdw.nl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RdwApi::class.java)
    }

    // UK: DVLA Vehicle Enquiry Service
    val dvlaApi: DvlaApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://driver-vehicle-licensing.api.gov.uk/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DvlaApi::class.java)
    }
}
