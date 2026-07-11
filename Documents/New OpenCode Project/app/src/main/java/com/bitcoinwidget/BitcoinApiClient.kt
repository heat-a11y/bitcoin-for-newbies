package com.bitcoinwidget

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToLong

/**
 * Lightweight HTTP client that fetches live Bitcoin data from free, no-auth APIs.
 *
 * All three data points are fetched independently so a failure in one doesn't
 * cascade to the others.
 */
object BitcoinApiClient {

    private const val PRICE_URL = "https://mempool.space/api/v1/prices"
    private const val BLOCK_HEIGHT_URL = "https://mempool.space/api/blocks/tip/height"
    private const val HASHRATE_URL = "https://mempool.space/api/v1/mining/pools/24h"

    private const val TIMEOUT_MS = 10_000

    // ── Public API ──────────────────────────────────────────────────────

    /** Fetches the current Bitcoin price in USD. */
    fun fetchPriceUsd(): Double? {
        return try {
            val body = httpGetString(PRICE_URL) ?: return null
            JSONObject(body).optDouble("USD", -1.0).takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
    }

    /** Fetches the current Bitcoin block height. */
    fun fetchBlockHeight(): Int? {
        return try {
            httpGetString(BLOCK_HEIGHT_URL)?.trim()?.toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches the estimated network hashrate (H/s) and converts to EH/s.
     * The /api/v1/mining/pools/24h endpoint returns a top-level
     * `lastEstimatedHashrate` field in H/s.
     */
    fun fetchHashrateEh(): Double? {
        return try {
            val body = httpGetString(HASHRATE_URL) ?: return null
            val obj = JSONObject(body)
            val hashrateHs = obj.optDouble("lastEstimatedHashrate", -1.0)
            if (hashrateHs <= 0) null else hashrateHs / 1e18
        } catch (_: Exception) {
            null
        }
    }

    // ── Internal HTTP helpers ──────────────────────────────────────────

    private fun httpGetString(urlString: String): String? {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.requestMethod = "GET"
        conn.instanceFollowRedirects = true

        return try {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            reader.readText()
        } catch (_: Exception) {
            // If inputStream fails, try the error stream for diagnostics
            val errorStream = conn.errorStream
            if (errorStream != null) {
                BufferedReader(InputStreamReader(errorStream)).readText()
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    // ── Formatting helpers ─────────────────────────────────────────────

    fun formatPrice(usd: Double?): String {
        if (usd == null) return "--,---"
        val whole = usd.roundToLong()
        return "$${formatWithCommas(whole)}"
    }

    fun formatBlockHeight(height: Int?): String {
        if (height == null) return "---,---"
        return formatWithCommas(height.toLong())
    }

    fun formatHashrate(eh: Double?): String {
        if (eh == null) return "--- EH/s"
        return "${"%.1f".format(eh)} EH/s"
    }

    private fun formatWithCommas(value: Long): String {
        val str = value.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in str.lastIndex downTo 0) {
            if (count > 0 && count % 3 == 0) sb.insert(0, ',')
            sb.insert(0, str[i])
            count++
        }
        return sb.toString()
    }
}
