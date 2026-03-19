package com.investtrack.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class FetchedPrice(
    val price: Double,
    val epochMillis: Long? = null,
    val source: String
)

data class AmfiSchemeSuggestion(
    val schemeCode: String,
    val schemeName: String,
    val isinGrowth: String = "",
    val isinDividend: String = ""
)

object PriceAutoFetchers {

    @Volatile private var amfiCatalogCache: List<AmfiSchemeSuggestion>? = null
    @Volatile private var amfiCatalogFetchedAtMs: Long = 0L

    /**
     * Fetch latest NAV for an AMFI scheme code from AMFI NAVAll endpoint.
     * AMFI format: ...;Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date
     */
    suspend fun fetchAmfiNav(amfiSchemeCode: String): FetchedPrice = withContext(Dispatchers.IO) {
        val code = amfiSchemeCode.trim()
        require(code.isNotEmpty()) { "AMFI scheme code is empty" }

        val url = URL("https://www.amfiindia.com/spages/NAVAll.txt")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "InvestTrack/1.0")
        }
        val fetched = conn.inputStream.use { input ->
            BufferedReader(InputStreamReader(input)).useLines { lines ->
                val match = lines.firstOrNull { it.startsWith("$code;", ignoreCase = false) }
                    ?: throw IllegalStateException("AMFI scheme code not found in NAVAll.txt")

                val parts = match.split(';')
                if (parts.size < 6) throw IllegalStateException("Unexpected AMFI response format")
                val navStr = parts[4].trim()
                val nav = navStr.toDoubleOrNull()
                    ?: throw IllegalStateException("Invalid NAV value: $navStr")
                FetchedPrice(price = nav, epochMillis = null, source = "AMFI")
            }
        }
        fetched ?: throw IllegalStateException("Failed to read AMFI NAV")
    }

    /**
     * Download and parse AMFI NAVAll catalog into suggestions (scheme code, name, ISINs).
     * Cached in memory for a few hours to avoid repeated large downloads.
     */
    suspend fun getAmfiCatalog(forceRefresh: Boolean = false): List<AmfiSchemeSuggestion> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = amfiCatalogCache
        val cacheAgeMs = now - amfiCatalogFetchedAtMs
        if (!forceRefresh && cached != null && cacheAgeMs < 6 * 60 * 60 * 1000L) {
            return@withContext cached
        }

        val url = URL("https://www.amfiindia.com/spages/NAVAll.txt")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "InvestTrack/1.0")
        }

        val list = conn.inputStream.use { input ->
            BufferedReader(InputStreamReader(input)).useLines { lines ->
                lines
                    .filter { it.isNotBlank() && it.first().isDigit() && it.contains(';') }
                    .mapNotNull { line ->
                        val parts = line.split(';')
                        if (parts.size < 5) return@mapNotNull null
                        val code = parts[0].trim()
                        val isinDivOrGrowth = parts.getOrNull(1)?.trim().orEmpty()
                        val isinReinv = parts.getOrNull(2)?.trim().orEmpty()
                        val name = parts.getOrNull(3)?.trim().orEmpty()
                        if (code.isEmpty() || name.isEmpty()) return@mapNotNull null
                        AmfiSchemeSuggestion(
                            schemeCode = code,
                            schemeName = name,
                            isinGrowth = isinDivOrGrowth,
                            isinDividend = isinReinv
                        )
                    }
                    .toList()
            }
        }

        amfiCatalogCache = list
        amfiCatalogFetchedAtMs = now
        list
    }

    suspend fun searchAmfiSchemes(query: String, limit: Int = 12): List<AmfiSchemeSuggestion> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val catalog = getAmfiCatalog()
        return catalog.asSequence()
            .filter {
                it.schemeName.contains(q, ignoreCase = true) ||
                    it.schemeCode.contains(q, ignoreCase = true) ||
                    it.isinGrowth.contains(q, ignoreCase = true) ||
                    it.isinDividend.contains(q, ignoreCase = true)
            }
            .take(limit)
            .toList()
    }

    /**
     * Fetch latest price from Yahoo Finance chart API.
     * Uses /v8/finance/chart/ endpoint (the /v8/finance/quote endpoint is blocked by Yahoo).
     * Symbol examples: TCS.NS (NSE), RELIANCE.BO (BSE), AAPL (US).
     *
     * Response parsed from: chart.result[0].meta.regularMarketPrice
     * This field is always the latest traded price, updated in real-time by Yahoo,
     * regardless of range or interval params.
     *
     * Fallback chain:
     *   1. regularMarketPrice  — live/last traded price
     *   2. previousClose       — last session close if market not yet open
     */
    suspend fun fetchYahooQuote(yahooSymbol: String): FetchedPrice = withContext(Dispatchers.IO) {
        val symbol = yahooSymbol.trim().uppercase()
        require(symbol.isNotEmpty()) { "Yahoo symbol is empty" }

        fun readStream(stream: InputStream?): String {
            if (stream == null) return ""
            return stream.use { it.readBytes().toString(Charsets.UTF_8) }
        }

        // Uses chart endpoint — only working Yahoo endpoint as of 2025
        fun request(host: String): String {
            val url = URL("https://$host/v8/finance/chart/$symbol?interval=1d&range=5d")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Connection", "keep-alive")
            }

            val code = conn.responseCode
            val body = if (code in 200..299) readStream(conn.inputStream) else readStream(conn.errorStream)
            if (code !in 200..299) {
                val hint = when (code) {
                    401, 403 -> "Yahoo blocked the request (HTTP $code)."
                    429      -> "Yahoo rate-limited the request (HTTP 429). Try again later."
                    else     -> "Yahoo request failed (HTTP $code)."
                }
                throw IllegalStateException("$hint Body: ${body.take(250)}")
            }
            return body
        }

        // Try query1 first, fall back to query2 if blocked
        val json = try {
            request("query1.finance.yahoo.com")
        } catch (_: Exception) {
            request("query2.finance.yahoo.com")
        }

        // Parse: chart → result[0] → meta → regularMarketPrice
        val meta = JSONObject(json)
            .getJSONObject("chart")
            .getJSONArray("result")
            .getJSONObject(0)
            .getJSONObject("meta")

        val price = when {
            meta.has("regularMarketPrice") && !meta.isNull("regularMarketPrice")
                -> meta.getDouble("regularMarketPrice")
            meta.has("previousClose") && !meta.isNull("previousClose")
                -> meta.getDouble("previousClose")
            else -> throw IllegalStateException("Yahoo chart returned no price for $symbol")
        }

        // regularMarketTime is Unix seconds — convert to millis
        val epochMillis = if (meta.has("regularMarketTime") && !meta.isNull("regularMarketTime"))
            meta.getLong("regularMarketTime") * 1000L
        else null

        FetchedPrice(price = price, epochMillis = epochMillis, source = "Yahoo")
    }
}
