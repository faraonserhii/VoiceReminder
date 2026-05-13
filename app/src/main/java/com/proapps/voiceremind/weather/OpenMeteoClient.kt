package com.proapps.voiceremind.weather

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class ForecastPayload(
    val hourlyEpochSeconds: List<Long>,
    val precipitationMm: List<Double>,
    val snowfallCm: List<Double>
)

data class TemperatureForecastPayload(
    val hourlyEpochSeconds: List<Long>,
    val temperatureC: List<Double>
)

class OpenMeteoClient {

    fun geocode(place: String): GeoPoint? {
        val encoded = URLEncoder.encode(place, StandardCharsets.UTF_8.toString())
        val url =
            "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=ru&format=json"

        val json = getJson(url) ?: return null
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val first = results.optJSONObject(0) ?: return null
        val lat = first.optDouble("latitude", Double.NaN)
        val lon = first.optDouble("longitude", Double.NaN)
        if (lat.isNaN() || lon.isNaN()) return null
        return GeoPoint(latitude = lat, longitude = lon)
    }

    fun loadForecast(
        latitude: Double,
        longitude: Double,
        timezoneId: String
    ): ForecastPayload? {
        val encodedTz = URLEncoder.encode(timezoneId, StandardCharsets.UTF_8.toString())
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&hourly=precipitation,snowfall&forecast_days=3&timezone=$encodedTz"

        val json = getJson(url) ?: return null
        val hourly = json.optJSONObject("hourly") ?: return null

        val timeArray = hourly.optJSONArray("time") ?: return null
        val precipArray = hourly.optJSONArray("precipitation") ?: return null
        val snowfallArray = hourly.optJSONArray("snowfall") ?: return null

        val hours = parseTimeToEpochSeconds(timeArray, timezoneId)
        val precip = parseDoubleArray(precipArray)
        val snow = parseDoubleArray(snowfallArray)

        return ForecastPayload(
            hourlyEpochSeconds = hours,
            precipitationMm = precip,
            snowfallCm = snow
        )
    }

    fun loadTemperatureForecast(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        forecastDays: Int = 16
    ): TemperatureForecastPayload? {
        val encodedTz = URLEncoder.encode(timezoneId, StandardCharsets.UTF_8.toString())
        val safeDays = forecastDays.coerceIn(1, 16)
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&hourly=temperature_2m&forecast_days=$safeDays&timezone=$encodedTz"

        val json = getJson(url) ?: return null
        val hourly = json.optJSONObject("hourly") ?: return null

        val timeArray = hourly.optJSONArray("time") ?: return null
        val tempArray = hourly.optJSONArray("temperature_2m") ?: return null

        return TemperatureForecastPayload(
            hourlyEpochSeconds = parseTimeToEpochSeconds(timeArray, timezoneId),
            temperatureC = parseDoubleArray(tempArray)
        )
    }

    private fun getJson(urlRaw: String): JSONObject? {
        val connection = (URL(urlRaw).openConnection() as? HttpURLConnection) ?: return null
        return runCatching {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            connection.inputStream.bufferedReader().use { reader ->
                JSONObject(reader.readText())
            }
        }.getOrNull().also {
            connection.disconnect()
        }
    }

    private fun parseTimeToEpochSeconds(array: JSONArray, timezoneId: String): List<Long> {
        val zone = runCatching { java.time.ZoneId.of(timezoneId) }.getOrDefault(java.time.ZoneId.systemDefault())
        val result = ArrayList<Long>(array.length())
        for (i in 0 until array.length()) {
            val value = array.optString(i)
            val epoch = runCatching {
                java.time.LocalDateTime.parse(value).atZone(zone).toEpochSecond()
            }.getOrNull() ?: continue
            result.add(epoch)
        }
        return result
    }

    private fun parseDoubleArray(array: JSONArray): List<Double> {
        val result = ArrayList<Double>(array.length())
        for (i in 0 until array.length()) {
            result.add(array.optDouble(i, 0.0))
        }
        return result
    }
}

