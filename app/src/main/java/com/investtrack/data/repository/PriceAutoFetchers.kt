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

object PriceAutoFetchers {

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

