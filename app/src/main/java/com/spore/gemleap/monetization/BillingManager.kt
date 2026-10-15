package com.spore.gemleap.monetization

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
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
 *
 * Every user-initiated action reconnects if needed and reports failures with a
 * short message, so the Remove Ads button never fails silently.
 */
class BillingManager(
    context: Context,
    private val repo: StatsRepository,
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    var adsRemoved by mutableStateOf(repo.loadAdsRemoved())
        private set

    var priceText by mutableStateOf<String?>(null)
        private set

    private var productDetails: ProductDetails? = null
    private var connecting = false
    private var reconnectAttempts = 0
    private val pendingActions = mutableListOf<() -> Unit>()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it, fromUser = true) }
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                toast("You already own Remove Ads — restoring it.")
                queryPurchases(userInitiated = false)
            }
            else -> reportFailure("Purchase", result)
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() = whenReady {
        Log.i(TAG, "connected; cached adsRemoved=$adsRemoved")
        queryProduct()
        queryPurchases(userInitiated = false)
    }

    /** Runs [action] once the billing service is connected, connecting first if needed. */
    private fun whenReady(onFailure: ((BillingResult) -> Unit)? = null, action: () -> Unit) {
        if (client.isReady) {
            action()
            return
        }
        pendingActions += action
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                main.post {
                    connecting = false
                    val actions = pendingActions.toList()
                    pendingActions.clear()
                    if (result.responseCode == BillingResponseCode.OK) {
                        reconnectAttempts = 0
                        actions.forEach { it() }
                    } else {
                        Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                        onFailure?.invoke(result)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                main.post {
                    connecting = false
                    // Reconnect quietly with a growing delay; user actions also reconnect on demand.
                    if (reconnectAttempts < MAX_RECONNECTS) {
                        reconnectAttempts++
                        main.postDelayed({ whenReady { queryProduct() } }, RECONNECT_DELAY_MS * reconnectAttempts)
                    }
                }
            }
        })
    }

    private fun queryProduct(then: ((ProductDetails?) -> Unit)? = null) {
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
        // Billing 8.0 delivers a QueryProductDetailsResult instead of a raw list.
        client.queryProductDetailsAsync(params) { result, queryResult ->
            main.post {
                if (result.responseCode == BillingResponseCode.OK) {
                    productDetails = queryResult.productDetailsList.firstOrNull()
                    priceText = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                    if (productDetails == null) {
                        Log.w(TAG, "Product ${AdIds.PRODUCT_REMOVE_ADS} not returned; unfetched: ${queryResult.unfetchedProductList}")
                    }
                } else {
                    Log.w(TAG, "Product query failed: ${result.responseCode} ${result.debugMessage}")
                }
                then?.invoke(productDetails)
            }
        }
    }

    /** Reconciles ownership from the store (authoritative on a successful query). */
    fun queryPurchases(userInitiated: Boolean = true) = whenReady(
        onFailure = { if (userInitiated) reportFailure("Restore", it) },
    ) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            main.post {
                if (result.responseCode != BillingResponseCode.OK) {
                    if (userInitiated) reportFailure("Restore", result)
                    return@post
                }
                Log.i(TAG, "queryPurchases: ${purchases.size} purchase(s)")
                purchases.forEach {
                    Log.i(TAG, "  ${it.products} state=${it.purchaseState} order=${it.orderId} ack=${it.isAcknowledged}")
                }
                val owned = purchases.any {
                    it.products.contains(AdIds.PRODUCT_REMOVE_ADS) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                setEntitlement(owned)
                purchases.forEach { handlePurchase(it, fromUser = false) }
                if (userInitiated) toast(if (owned) "Remove Ads restored." else "No Remove Ads purchase found on this account.")
            }
        }
    }

    /**
     * Debug builds only: consumes any Remove Ads purchase so Play stops returning it,
     * letting a test account see ads again. Never called from release builds.
     */
    fun debugResetRemoveAds() = whenReady {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { _, purchases ->
            val targets = purchases.filter { it.products.contains(AdIds.PRODUCT_REMOVE_ADS) }
            Log.i(TAG, "debugReset: consuming ${targets.size} purchase(s)")
            targets.forEach { purchase ->
                val consume = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                client.consumeAsync(consume) { result, _ ->
                    Log.i(TAG, "debugReset: consume ${purchase.orderId} -> ${result.responseCode} ${result.debugMessage}")
                    main.post { if (result.responseCode == BillingResponseCode.OK) setEntitlement(false) }
                }
            }
            if (targets.isEmpty()) main.post { setEntitlement(false) }
        }
    }

    fun purchaseRemoveAds(activity: Activity) {
        if (adsRemoved) {
            toast("Ads are already removed.")
            return
        }
        whenReady(onFailure = { reportFailure("Purchase", it) }) {
            val details = productDetails
            if (details != null) {
                launchPurchase(activity, details)
            } else {
                queryProduct { fresh ->
                    if (fresh != null) {
                        launchPurchase(activity, fresh)
                    } else {
                        toast("Remove Ads isn't available right now. Please try again later.")
                    }
                }
            }
        }
    }

    private fun launchPurchase(activity: Activity, details: ProductDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        val result = client.launchBillingFlow(activity, flowParams)
        when (result.responseCode) {
            BillingResponseCode.OK -> Unit // Outcome arrives in purchasesListener.
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                toast("You already own Remove Ads — restoring it.")
                queryPurchases(userInitiated = false)
            }
            else -> reportFailure("Purchase", result)
        }
    }

    private fun handlePurchase(purchase: Purchase, fromUser: Boolean) {
        if (!purchase.products.contains(AdIds.PRODUCT_REMOVE_ADS)) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> {
                if (fromUser) toast("Payment pending. Ads will be removed once it completes.")
                return
            }
            Purchase.PurchaseState.PURCHASED -> Unit
            else -> return
        }
        val wasRemoved = adsRemoved
        setEntitlement(true)
        if (fromUser && !wasRemoved) toast("Ads removed. Thank you!")
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${result.responseCode} ${result.debugMessage}")
                }
            }
        }
    }

    private fun setEntitlement(owned: Boolean) {
        Log.i(TAG, "setEntitlement($owned), was $adsRemoved")
        if (adsRemoved != owned) {
            adsRemoved = owned
            repo.saveAdsRemoved(owned)
        } else if (owned) {
            repo.saveAdsRemoved(true)
        }
    }

    private fun reportFailure(what: String, result: BillingResult) {
        Log.w(TAG, "$what failed: ${result.responseCode} ${result.debugMessage}")
        val reason = when (result.responseCode) {
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.NETWORK_ERROR -> "Couldn't reach Google Play. Check your connection and try again."
            BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play billing isn't available on this device or account."
            BillingResponseCode.ITEM_UNAVAILABLE -> "Remove Ads isn't available for this account or country."
            BillingResponseCode.DEVELOPER_ERROR -> "This copy of GemLeap can't make purchases. Install it from Google Play."
            else -> "$what didn't go through (code ${result.responseCode}). Please try again."
        }
        toast(reason)
    }

    private fun toast(message: String) {
        main.post { Toast.makeText(appContext, message, Toast.LENGTH_LONG).show() }
    }

    fun endConnection() {
        main.removeCallbacksAndMessages(null)
        client.endConnection()
    }

    private companion object {
        const val TAG = "BillingManager"
        const val MAX_RECONNECTS = 3
        const val RECONNECT_DELAY_MS = 2_000L
    }
}
