package com.bitcoinwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 4x2 Bitcoin Widget Provider
 *
 * Displays a TextClock (updates every second via system clock, no battery cost)
 * and three Bitcoin metrics (price, block height, hashrate) refreshed by
 * [BitcoinDataWorker] every 30 minutes.
 */
class BitcoinWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        schedulePeriodicSync(context)
        triggerImmediateSync(context)
    }

    override fun onEnabled(context: Context) {
        schedulePeriodicSync(context)
        triggerImmediateSync(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(
            BitcoinDataWorker.UNIQUE_WORK_NAME
        )
    }

    companion object {
        /**
         * Refreshes the RemoteViews for a given widget instance.
         * Falls back to placeholder strings when no cached data exists.
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.bitcoin_widget)

            // Attempt to show cached values, otherwise fall back to placeholders
            val prefs = context.getSharedPreferences(
                "bitcoin_widget_cache",
                Context.MODE_PRIVATE
            )

            val cachedPrice = prefs.getFloat("price", -1f).toDouble()
            val cachedBlock = prefs.getInt("block", -1)
            val cachedHashrate = prefs.getFloat("hashrate", -1f).toDouble()

            if (cachedPrice > 0) {
                views.setTextViewText(
                    R.id.value_price,
                    BitcoinApiClient.formatPrice(cachedPrice)
                )
            } else {
                views.setTextViewText(R.id.value_price, "\$ --,---")
            }

            if (cachedBlock >= 0) {
                views.setTextViewText(
                    R.id.value_block,
                    BitcoinApiClient.formatBlockHeight(cachedBlock)
                )
            } else {
                views.setTextViewText(R.id.value_block, "---,---")
            }

            if (cachedHashrate > 0) {
                views.setTextViewText(
                    R.id.value_hashrate,
                    BitcoinApiClient.formatHashrate(cachedHashrate)
                )
            } else {
                views.setTextViewText(R.id.value_hashrate, "--- EH/s")
            }

            // Tap on widget opens the host app
            val openIntent = context.packageManager.getLaunchIntentForPackage(
                context.packageName
            )
            if (openIntent != null) {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.clock, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Enqueues (or reuses) a 30-minute periodic [BitcoinDataWorker].
         * Uses [ExistingPeriodicWorkPolicy.KEEP] so we never stack workers.
         */
        private fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<BitcoinDataWorker>(
                30, TimeUnit.MINUTES
            )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BitcoinDataWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun triggerImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<BitcoinDataWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${BitcoinDataWorker.UNIQUE_WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
