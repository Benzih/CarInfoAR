package com.carinfo.ar.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    // Production IDs (uncomment when AdMob account is approved):
    // const val BANNER_AD_UNIT_ID = "ca-app-pub-6755700667333024/9070814697"
    // private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6755700667333024/6137529598"

    // Test IDs (for development/testing):
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var detectionCount = 0
    private const val DETECTIONS_BEFORE_INTERSTITIAL = 2

    fun initialize(context: Context) {
        MobileAds.initialize(context) {
            Log.d(TAG, "AdMob initialized")
        }
        loadInterstitial(context)
    }

    private fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    fun onNewPlateDetected(activity: Activity) {
        detectionCount++
        Log.d(TAG, "Detection count: $detectionCount / $DETECTIONS_BEFORE_INTERSTITIAL")

        if (detectionCount >= DETECTIONS_BEFORE_INTERSTITIAL) {
            detectionCount = 0
            showInterstitial(activity)
        }
    }

    private fun showInterstitial(activity: Activity) {
        val ad = interstitialAd ?: run {
            Log.d(TAG, "Interstitial not ready, reloading")
            loadInterstitial(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                interstitialAd = null
                loadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Interstitial failed to show: ${error.message}")
                interstitialAd = null
                loadInterstitial(activity)
            }
        }

        ad.show(activity)
    }
}
