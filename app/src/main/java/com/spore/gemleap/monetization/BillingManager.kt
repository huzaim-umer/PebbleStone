package com.spore.gemleap.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.spore.gemleap.data.StatsRepository

/**
 * Wraps Play Billing for the single "Remove Ads" non-consumable purchase.
 *
 * Billing is the source of truth: a cached flag gives instant UI on launch, then
 * [queryPurchases] reconciles it. Exposes Compose state so the UI recomposes when
 * ownership or the localized price becomes known.
 */
class BillingManager(
    context: Context,
    private val repo: StatsRepository,
) {
    var adsRemoved by mutableStateOf(repo.loadAdsRemoved())
        private set

    var priceText by mutableStateOf<String?>(null)
        private set

    private var productDetails: ProductDetails? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach(::handlePurchase)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.w(TAG, "Purchase update failed: ${result.debugMessage}")
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    queryPurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Left for a future retry; the app also reconnects on next launch.
            }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(AdIds.PRODUCT_REMOVE_ADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        // Billing 8.0 changed this callback to deliver a QueryProductDetailsResult
        // instead of a raw List<ProductDetails>.
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = queryResult.productDetailsList.firstOrNull()
                priceText = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            } else {
                Log.w(TAG, "Product query failed: ${result.debugMessage}")
            }
        }
    }

    /** Reconciles ownership from the store (authoritative on a successful query). */
    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val owned = purchases.any {
                    it.products.contains(AdIds.PRODUCT_REMOVE_ADS) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                setEntitlement(owned)
                purchases.forEach(::handlePurchase)
            }
        }
    }

    fun purchaseRemoveAds(activity: Activity) {
        val details = productDetails ?: run {
            queryProduct()
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.products.contains(AdIds.PRODUCT_REMOVE_ADS)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        setEntitlement(true)
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
                }
            }
        }
    }

    private fun setEntitlement(owned: Boolean) {
        if (adsRemoved != owned) {
            adsRemoved = owned
            repo.saveAdsRemoved(owned)
        } else if (owned) {
            repo.saveAdsRemoved(true)
        }
    }

    fun endConnection() {
        client.endConnection()
    }

    private companion object {
        const val TAG = "BillingManager"
    }
}
