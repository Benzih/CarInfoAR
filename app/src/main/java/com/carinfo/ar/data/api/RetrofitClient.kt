package com.carinfo.ar.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    // Trust-all client ONLY for data.gov.il — public government API, no sensitive data
    // Needed because Android 8.x doesn't trust the SSL.com certificate used by data.gov.il
    private val israelHttpClient: OkHttpClient by lazy {
        val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllManager)
            .hostnameVerifier { hostname, _ -> hostname == "data.gov.il" }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    // Israel: data.gov.il
    val israelApi: GovIlApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://data.gov.il/")
            .client(israelHttpClient)
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
