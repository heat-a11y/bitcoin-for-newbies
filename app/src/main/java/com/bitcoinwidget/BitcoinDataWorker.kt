package com.bitcoinwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A [CoroutineWorker] that fetches live Bitcoin data from the free mempool.space
 * API and pushes the values into all active widget instances.
 *
 * On failure the worker falls back to previously cached values so the widget
 * never shows blank data.
 */
class BitcoinDataWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // 1. Fetch fresh values (each returns null on failure)
        val price = BitcoinApiClient.fetchPriceUsd()
        val blockHeight = BitcoinApiClient.fetchBlockHeight()
        val hashrate = BitcoinApiClient.fetchHashrateEh()

        // 2. Use cached values for any failed fetches
        val cache = Cache(applicationContext)
        val finalPrice = price ?: cache.price
        val finalBlock = blockHeight ?: cache.blockHeight
        val finalHashrate = hashrate ?: cache.hashrate

        // 3. Persist whatever we got
        cache.save(finalPrice, finalBlock, finalHashrate)

        // 4. Format and push to every widget instance
        val views = RemoteViews(
            applicationContext.packageName,
            R.layout.bitcoin_widget
        )
        views.setTextViewText(
            R.id.value_price,
            BitcoinApiClient.formatPrice(finalPrice)
        )
        views.setTextViewText(
            R.id.value_block,
            BitcoinApiClient.formatBlockHeight(finalBlock)
        )
        views.setTextViewText(
            R.id.value_hashrate,
            BitcoinApiClient.formatHashrate(finalHashrate)
        )

        val manager = AppWidgetManager.getInstance(applicationContext)
        val component = ComponentName(
            applicationContext,
            BitcoinWidgetProvider::class.java
        )
        manager.updateAppWidget(component, views)

        Result.success()
    }

    // ── SharedPreferences cache ────────────────────────────────────────

    private class Cache(context: Context) {
        private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val price: Double?
            get() = prefs.getFloat(KEY_PRICE, -1f).toDouble().takeIf { it >= 0 }

        val blockHeight: Int?
            get() = prefs.getInt(KEY_BLOCK, -1).takeIf { it >= 0 }

        val hashrate: Double?
            get() = prefs.getFloat(KEY_HASHRATE, -1f).toDouble().takeIf { it >= 0 }

        fun save(price: Double?, block: Int?, hashrate: Double?) {
            prefs.edit().apply {
                if (price != null) putFloat(KEY_PRICE, price.toFloat())
                if (block != null) putInt(KEY_BLOCK, block)
                if (hashrate != null) putFloat(KEY_HASHRATE, hashrate.toFloat())
                apply()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "bitcoin_widget_cache"
        private const val KEY_PRICE = "price"
        private const val KEY_BLOCK = "block"
        private const val KEY_HASHRATE = "hashrate"

        const val UNIQUE_WORK_NAME = "bitcoin_data_refresh"
    }
}
