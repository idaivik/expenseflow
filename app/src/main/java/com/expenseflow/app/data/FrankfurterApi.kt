package com.expenseflow.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin client for the free, key-less Frankfurter exchange-rate API
 * (https://frankfurter.dev), which serves European Central Bank reference rates.
 *
 * Used when the user switches their currency in Settings: we fetch the latest
 * `from → to` rate and convert every stored amount by it (see
 * [TransactionRepository.convertAllAmounts]). No API key or account is required.
 */
object FrankfurterApi {

    private const val BASE_URL = "https://api.frankfurter.dev/v1/latest"

    /** Map an in-app currency code ("usd"/"inr"/"eur") to its ISO-4217 code Frankfurter expects. */
    fun isoCode(appCode: String): String = appCode.trim().uppercase()

    /**
     * Latest conversion rate for 1 unit of [from] expressed in [to] (both ISO-4217,
     * e.g. "USD", "INR"). Returns 1.0 when the currencies match, or null on any
     * network / parsing error so callers can surface an "offline" message and
     * leave stored amounts untouched.
     */
    suspend fun fetchRate(from: String, to: String): Double? = withContext(Dispatchers.IO) {
        if (from.equals(to, ignoreCase = true)) return@withContext 1.0
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL?base=$from&symbols=$to")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val rates = JSONObject(body).optJSONObject("rates") ?: return@withContext null
            if (rates.has(to)) rates.getDouble(to) else null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
