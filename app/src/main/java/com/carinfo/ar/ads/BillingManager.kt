package com.carinfo.ar.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BillingManager {
    private const val TAG = "BillingManager"
    private const val PREFS_NAME = "purchases"
    private const val KEY_ADS_REMOVED = "remove_ads_purchased"
    const val PRODUCT_REMOVE_ADS = "remove_ads"

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private var appContext: Context? = null
    private var reconnectAttempts = 0
    private const val MAX_RECONNECT_ATTEMPTS = 3
    private val handler = Handler(Looper.getMainLooper())

    private val _adsRemoved = MutableStateFlow(false)
    val adsRemoved: StateFlow<Boolean> = _adsRemoved

    private val _purchasePending = MutableStateFlow(false)
    val purchasePending: StateFlow<Boolean> = _purchasePending

    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Check local cache first (instant, no flicker)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _adsRemoved.value = prefs.getBoolean(KEY_ADS_REMOVED, false)
        Log.d(TAG, "Ads removed (cached): ${_adsRemoved.value}")

        connectBillingClient(context)
    }

    private fun connectBillingClient(context: Context) {
        val client = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(context, purchase)
                    }
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        billingClient = client

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    reconnectAttempts = 0
                    queryProduct()
                    restorePurchases(context)
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing disconnected, attempt $reconnectAttempts")
                // Retry with exponential backoff
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    val delay = (1000L * (1 shl reconnectAttempts)).coerceAtMost(8000L)
                    reconnectAttempts++
                    handler.postDelayed({
                        appContext?.let { connectBillingClient(it) }
                    }, delay)
                }
            }
        })
    }

    private fun queryProduct() {
        val client = billingClient ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_REMOVE_ADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList.firstOrNull()
                Log.d(TAG, "Product: ${productDetails?.title} - ${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice}")
            } else {
                Log.e(TAG, "Query product failed: ${billingResult.debugMessage}")
                // Retry after 5 seconds
                handler.postDelayed({ queryProduct() }, 5000)
            }
        }
    }

    private fun restorePurchases(context: Context) {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { _, purchases ->
            val hasPurchase = purchases.any { purchase ->
                purchase.products.contains(PRODUCT_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            val hasPending = purchases.any { purchase ->
                purchase.products.contains(PRODUCT_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PENDING
            }

            if (hasPurchase) {
                grantRemoveAds(context)
            } else if (!hasPending) {
                // No valid purchase found on Google's servers — revoke local entitlement
                // This handles refunds: user got money back, ads come back
                revokeRemoveAds(context)
            }

            _purchasePending.value = hasPending
            Log.d(TAG, "Restored: adsRemoved=$hasPurchase, pending=$hasPending")
        }
    }

    fun launchPurchase(activity: Activity): Boolean {
        val client = billingClient
        if (client == null || !client.isReady) {
            Toast.makeText(activity, "Store not available, please try again", Toast.LENGTH_SHORT).show()
            // Try to reconnect
            connectBillingClient(activity)
            return false
        }

        val details = productDetails
        if (details == null) {
            Toast.makeText(activity, "Loading price, please wait...", Toast.LENGTH_SHORT).show()
            queryProduct()
            return false
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        client.launchBillingFlow(activity, params)
        return true
    }

    private fun handlePurchase(context: Context, purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient?.acknowledgePurchase(params) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Purchase acknowledged")
                            grantRemoveAds(context)
                            // Reset ad detection counter
                            AdManager.resetCount(context)
                        } else {
                            Log.e(TAG, "Acknowledge failed: ${billingResult.debugMessage}")
                            // Retry acknowledge after 3 seconds
                            handler.postDelayed({ handlePurchase(context, purchase) }, 3000)
                        }
                    }
                } else {
                    grantRemoveAds(context)
                }
            }
            Purchase.PurchaseState.PENDING -> {
                _purchasePending.value = true
                Log.d(TAG, "Purchase pending — waiting for payment to complete")
                handler.post {
                    Toast.makeText(context, "Purchase pending — ads will be removed once payment completes", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Log.d(TAG, "Purchase state: ${purchase.purchaseState}")
            }
        }
    }

    private fun grantRemoveAds(context: Context) {
        _adsRemoved.value = true
        _purchasePending.value = false
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ADS_REMOVED, true)
            .apply()
        Log.d(TAG, "Ads removed granted!")
    }

    private fun revokeRemoveAds(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ADS_REMOVED, false)) {
            // Only revoke if it was previously granted — this means a refund happened
            _adsRemoved.value = false
            prefs.edit().putBoolean(KEY_ADS_REMOVED, false).apply()
            Log.d(TAG, "Ads removed REVOKED — refund detected")
        }
    }

    fun getFormattedPrice(): String? {
        return productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    fun disconnect() {
        handler.removeCallbacksAndMessages(null)
        billingClient?.endConnection()
    }
}
