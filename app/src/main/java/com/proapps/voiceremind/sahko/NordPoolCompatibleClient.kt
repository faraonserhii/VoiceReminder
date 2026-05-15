package com.proapps.voiceremind.sahko

import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONObject

class NordPoolCompatibleClient {

    fun loadUpcomingFiPrices(): List<ElectricityPriceSlot>? {
        val fromNordPool = loadFromNordPool()
        if (!fromNordPool.isNullOrEmpty()) return fromNordPool

        return loadFromPorssiSahko()
    }

    private fun loadFromNordPool(): List<ElectricityPriceSlot>? {
        val today = java.time.LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val url = "https://dataportal-api.nordpoolgroup.com/api/DayAheadPrices?currency=EUR&market=DayAhead&deliveryArea=FI&date=$today"
        val root = getJson(url) ?: return null

        val entries = root.optJSONArray("multiAreaEntries") ?: return null
        val result = ArrayList<ElectricityPriceSlot>()
        for (i in 0 until entries.length()) {
            val obj = entries.optJSONObject(i) ?: continue
            val start = obj.optString("deliveryStart")
            val end = obj.optString("deliveryEnd")
            val perArea = obj.optJSONObject("entryPerArea")
            val fi = perArea?.optDouble("FI", Double.NaN) ?: Double.NaN
            if (start.isBlank() || end.isBlank() || fi.isNaN()) continue

            val startMillis = parseIsoMillis(start) ?: continue
            val endMillis = parseIsoMillis(end) ?: continue
            // NordPool values are often EUR/MWh. Convert approximately to cents/kWh.
            val centsPerKwh = fi / 10.0
            result.add(ElectricityPriceSlot(startMillis, endMillis, centsPerKwh))
        }
        return result
    }

    private fun loadFromPorssiSahko(): List<ElectricityPriceSlot>? {
        val root = getJson("https://api.porssisahko.net/v1/latest-prices.json") ?: return null
        val prices = root.optJSONArray("prices") ?: return null

        val result = ArrayList<ElectricityPriceSlot>()
        for (i in 0 until prices.length()) {
            val obj = prices.optJSONObject(i) ?: continue
            val start = obj.optString("startDate")
            val end = obj.optString("endDate")
            val price = obj.optDouble("price", Double.NaN)
            if (start.isBlank() || end.isBlank() || price.isNaN()) continue

            val startMillis = parseIsoMillis(start) ?: continue
            val endMillis = parseIsoMillis(end) ?: continue
            result.add(ElectricityPriceSlot(startMillis, endMillis, price))
        }
        return result
    }

    private fun parseIsoMillis(value: String): Long? {
        return runCatching {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        }.getOrElse {
            runCatching { OffsetDateTime.parse(value + "Z").toInstant().toEpochMilli() }.getOrNull()
        }
    }

    private fun getJson(urlRaw: String): JSONObject? {
        val connection = (URL(urlRaw).openConnection() as? HttpURLConnection) ?: return null
        return runCatching {
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.requestMethod = "GET"
            connection.inputStream.bufferedReader().use { reader ->
                JSONObject(reader.readText())
            }
        }.getOrNull().also {
            connection.disconnect()
        }
    }
}

