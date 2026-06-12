package com.proapps.voiceremind.transport

import com.proapps.voiceremind.BusDeparture
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class DigitransitProvider(private val router: String = "hsl") : TransportProvider {
    private val client = OkHttpClient()

    override fun supportsRegion(countryCode: String?): Boolean {
        // Digitransit primarily supports Finland routers; simple heuristic: prefer FI
        return countryCode == null || countryCode.equals("FI", ignoreCase = true)
    }

    override fun findNextDeparture(routeNumber: String, lat: Double, lon: Double, radiusMeters: Int): BusDeparture? {
        val endpoint = "https://api.digitransit.fi/routing/v1/routers/$router/index/graphql"

        val query = """
            query(${"\$"}lat: Float!, ${"\$"}lon: Float!, ${"\$"}radius: Int!, ${"\$"}numDepartures: Int!) {
              stopsByRadius(lat: ${"\$"}lat, lon: ${"\$"}lon, radius: ${"\$"}radius) {
                edges {
                  node {
                    stop {
                      id
                      name
                      lat
                      lon
                      stoptimesWithoutPatterns(numberOfDepartures: ${"\$"}numDepartures) {
                        scheduledDeparture
                        realtimeDeparture
                        realtime
                        headsign
                        trip { route { shortName } }
                      }
                    }
                    distance
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("lat", lat)
            put("lon", lon)
            put("radius", radiusMeters)
            put("numDepartures", 6)
        }

        val bodyJson = JSONObject()
            .put("query", query)
            .put("variables", variables)
            .toString()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(endpoint)
            .post(bodyJson.toRequestBody(mediaType))
            .build()

        val respStr = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            resp.body?.string() ?: throw RuntimeException("Empty response")
        }

        return parseBestDeparture(respStr, routeNumber)
    }

    private fun parseBestDeparture(jsonText: String, routeNumber: String): BusDeparture? {
        val root = JSONObject(jsonText)
        val data = root.optJSONObject("data") ?: return null
        val stopsByRadius = data.optJSONObject("stopsByRadius") ?: return null

        val edges = stopsByRadius.optJSONArray("edges") ?: JSONArray()
        var best: BusDeparture? = null

        for (i in 0 until edges.length()) {
            val node = edges.getJSONObject(i).optJSONObject("node") ?: continue
            val stop = node.optJSONObject("stop") ?: continue
            val stopName = stop.optString("name")
            val stoptimes = stop.optJSONArray("stoptimesWithoutPatterns") ?: JSONArray()

            for (j in 0 until stoptimes.length()) {
                val st = stoptimes.getJSONObject(j)
                val trip = st.optJSONObject("trip")
                val route = trip?.optJSONObject("route")
                val shortName = route?.optString("shortName") ?: ""
                if (shortName == routeNumber) {
                    val realtime = st.optBoolean("realtime", false)
                    val depSeconds = if (realtime && st.has("realtimeDeparture")) st.optInt("realtimeDeparture") else st.optInt("scheduledDeparture")

                    val nowZ = Instant.now().atZone(ZoneId.systemDefault())
                    val midnight = nowZ.toLocalDate().atStartOfDay(nowZ.zone)
                    var depInstant = midnight.plusSeconds(depSeconds.toLong())
                    var minutesUntil = Duration.between(nowZ.toLocalDateTime(), depInstant.toLocalDateTime()).toMinutes().toInt()
                    if (minutesUntil < 0) minutesUntil += 24 * 60

                    val epochMillis = depInstant.toInstant().toEpochMilli()

                    val candidate = BusDeparture(
                        route = routeNumber,
                        stopName = stopName,
                        headsign = st.optString("headsign").ifBlank { null },
                        departureEpochMillis = epochMillis,
                        minutesUntil = minutesUntil,
                        realtime = realtime
                    )

                    if (best == null || candidate.minutesUntil < best.minutesUntil) {
                        best = candidate
                    }
                }
            }
        }

        return best
    }
}

