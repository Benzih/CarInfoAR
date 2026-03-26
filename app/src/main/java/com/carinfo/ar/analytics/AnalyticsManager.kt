package com.carinfo.ar.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized analytics tracking for all user actions.
 * All events are sent to Firebase Analytics.
 */
object AnalyticsManager {
    private var analytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }

    private fun logEvent(event: String, params: Bundle? = null) {
        analytics?.logEvent(event, params)
    }

    private fun bundle(vararg pairs: Pair<String, Any?>): Bundle {
        return Bundle().apply {
            for ((key, value) in pairs) {
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                }
            }
        }
    }

    // ==================== APP LIFECYCLE ====================

    fun appOpened() = logEvent("app_opened")

    fun appBackgrounded() = logEvent("app_backgrounded")

    fun appResumed() = logEvent("app_resumed")

    // ==================== ONBOARDING ====================

    fun onboardingStarted() = logEvent("onboarding_started")

    fun onboardingCountrySelected(country: String) = logEvent(
        "onboarding_country_selected",
        bundle("country" to country)
    )

    fun onboardingCompleted(country: String) = logEvent(
        "onboarding_completed",
        bundle("country" to country)
    )

    // ==================== CAMERA / SCANNING ====================

    fun cameraOpened() = logEvent("camera_opened")

    fun cameraPermissionGranted() = logEvent("camera_permission_granted")

    fun cameraPermissionDenied() = logEvent("camera_permission_denied")

    fun plateDetectedByOcr(plate: String, country: String) = logEvent(
        "plate_detected_ocr",
        bundle("country" to country)
    )

    fun plateAccepted(plate: String, country: String) = logEvent(
        "plate_accepted",
        bundle("country" to country)
    )

    fun vehicleInfoLoaded(plate: String, manufacturer: String?, model: String?, country: String) = logEvent(
        "vehicle_info_loaded",
        bundle(
            "manufacturer" to (manufacturer ?: "unknown"),
            "model" to (model ?: "unknown"),
            "country" to country
        )
    )

    fun vehicleNotFound(plate: String, country: String) = logEvent(
        "vehicle_not_found",
        bundle("country" to country)
    )

    fun apiError(plate: String, country: String, error: String) = logEvent(
        "api_error",
        bundle(
            "country" to country,
            "error_type" to error.take(100)
        )
    )

    fun scanReset() = logEvent("scan_reset")

    // ==================== ZOOM ====================

    fun pinchZoomUsed() = logEvent("pinch_zoom_used")

    fun doubleTapZoomUsed() = logEvent("double_tap_zoom_used")

    // ==================== MANUAL INPUT ====================

    fun manualInputOpened() = logEvent("manual_input_opened")

    fun manualInputSearched(plate: String, country: String) = logEvent(
        "manual_input_searched",
        bundle("country" to country)
    )

    fun manualInputCancelled() = logEvent("manual_input_cancelled")

    // ==================== HISTORY ====================

    fun historyOpened(itemCount: Int) = logEvent(
        "history_opened",
        bundle("item_count" to itemCount)
    )

    fun historySavedFromCamera(plate: String, country: String) = logEvent(
        "history_saved_camera",
        bundle("country" to country)
    )

    fun historyItemExpanded(plate: String) = logEvent("history_item_expanded")

    fun historyItemCollapsed(plate: String) = logEvent("history_item_collapsed")

    fun historyItemDeleted() = logEvent("history_item_deleted")

    fun historyClearedAll(itemCount: Int) = logEvent(
        "history_cleared_all",
        bundle("item_count" to itemCount)
    )

    fun historyInfoButtonClicked() = logEvent("history_info_clicked")

    // ==================== SETTINGS ====================

    fun settingsOpened() = logEvent("settings_opened")

    fun languageChanged(language: String) = logEvent(
        "language_changed",
        bundle("language" to language)
    )

    fun countryChanged(country: String) = logEvent(
        "country_changed",
        bundle("country" to country)
    )

    fun soundToggled(enabled: Boolean) = logEvent(
        "sound_toggled",
        bundle("enabled" to enabled)
    )

    // ==================== ADS ====================

    fun bannerAdLoaded() = logEvent("banner_ad_loaded")

    fun bannerAdFailed(error: String) = logEvent(
        "banner_ad_failed",
        bundle("error" to error.take(100))
    )

    fun interstitialShown() = logEvent("interstitial_shown")

    fun interstitialDismissed() = logEvent("interstitial_dismissed")

    fun interstitialFailed(error: String) = logEvent(
        "interstitial_failed",
        bundle("error" to error.take(100))
    )

    // ==================== PREMIUM / BILLING ====================

    fun removeAdsClicked() = logEvent("remove_ads_clicked")

    fun purchaseStarted() = logEvent("purchase_started")

    fun purchaseCompleted(price: String) = logEvent(
        "purchase_completed",
        bundle("price" to price)
    )

    fun purchaseFailed(error: String) = logEvent(
        "purchase_failed",
        bundle("error" to error.take(100))
    )

    fun purchaseRestored() = logEvent("purchase_restored")

    // ==================== MODEL INFO ====================

    fun modelInfoClicked(manufacturer: String?, model: String?) = logEvent(
        "model_info_clicked",
        bundle(
            "manufacturer" to (manufacturer ?: "unknown"),
            "model" to (model ?: "unknown")
        )
    )

    // ==================== COUNTRY DETECTION ====================

    fun countryAutoDetected(country: String, method: String) = logEvent(
        "country_auto_detected",
        bundle(
            "country" to country,
            "method" to method
        )
    )

    fun countryDetectionFailed() = logEvent("country_detection_failed")

    // ==================== ERRORS ====================

    fun sslError(domain: String) = logEvent(
        "ssl_error",
        bundle("domain" to domain)
    )

    fun crashRecovered(screen: String) = logEvent(
        "crash_recovered",
        bundle("screen" to screen)
    )
}
