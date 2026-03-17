package com.investtrack.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
        conn.inputStream.use { input ->
            BufferedReader(InputStreamReader(input)).useLines { lines ->
                // Find the first matching scheme code line (the file usually has only one entry per scheme per date)
                val match = lines.firstOrNull { it.startsWith("$code;", ignoreCase = false) }
                    ?: throw IllegalStateException("AMFI scheme code not found in NAVAll.txt")

                val parts = match.split(';')
                if (parts.size < 6) throw IllegalStateException("Unexpected AMFI response format")
                val navStr = parts[4].trim()
                val nav = navStr.toDoubleOrNull()
                    ?: throw IllegalStateException("Invalid NAV value: $navStr")
                return@useLines FetchedPrice(price = nav, epochMillis = null, source = "AMFI")
            }
        }
        throw IllegalStateException("Failed to read AMFI NAV")
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
                            // AMFI fields are not consistently "growth vs dividend" across the file; store both.
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
     * Fetch latest price from Yahoo quote API.
     * Symbol examples: TCS.NS, RELIANCE.NS, INFY.NS.
     */
    suspend fun fetchYahooQuote(yahooSymbol: String): FetchedPrice = withContext(Dispatchers.IO) {
        val symbol = yahooSymbol.trim().uppercase(Locale.US)
        require(symbol.isNotEmpty()) { "Yahoo symbol is empty" }

        val url = URL("https://query1.finance.yahoo.com/v7/finance/quote?symbols=$symbol")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "InvestTrack/1.0")
        }

        val json = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }

        val root = Gson().fromJson(json, YahooQuoteRoot::class.java)
        val quote = root?.quoteResponse?.result?.firstOrNull()
            ?: throw IllegalStateException("No Yahoo quote found for $symbol")

        val price = quote.regularMarketPrice
            ?: quote.postMarketPrice
            ?: quote.preMarketPrice
            ?: throw IllegalStateException("Yahoo quote returned no price for $symbol")

        val epochSeconds = quote.regularMarketTime
        val epochMillis = epochSeconds?.times(1000L)

        FetchedPrice(price = price, epochMillis = epochMillis, source = "Yahoo")
    }

    // --- Minimal JSON models for Yahoo quote ---
    private data class YahooQuoteRoot(
        val quoteResponse: YahooQuoteResponse?
    )

    private data class YahooQuoteResponse(
        val result: List<YahooQuote>?
    )

    private data class YahooQuote(
        val regularMarketPrice: Double?,
        val regularMarketTime: Long?,
        val preMarketPrice: Double?,
        val postMarketPrice: Double?
    )
}

