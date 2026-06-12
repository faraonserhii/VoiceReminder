package com.proapps.voiceremind.transport

import com.proapps.voiceremind.BusDeparture
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Minimal TransitLand provider: attempts to find nearby stops via transit.land REST API.
 * NOTE: This is a lightweight implementation and currently only discovers stops; full schedule
 * integration (GTFS/GTFS-RT) is a planned follow-up.
 */
class TransitLandProvider : TransportProvider {
    private val client = OkHttpClient()

    override fun supportsRegion(countryCode: String?): Boolean {
        // transit.land is global; accept all
        return true
    }

    override fun findNextDeparture(routeNumber: String, lat: Double, lon: Double, radiusMeters: Int): BusDeparture? {
        try {
            val url = "https://transit.land/api/v2/rest/stops?lat=$lat&lon=$lon&r=$radiusMeters"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val stops = root.optJSONArray("stops") ?: return null
                if (stops.length() == 0) return null
                val stop = stops.getJSONObject(0)
                val name = stop.optString("name")
                // synthetic BusDeparture: provider does not yet fetch next departures
                return BusDeparture(
                    route = routeNumber,
                    stopName = if (name.isNullOrBlank()) "stop" else name,
                    headsign = null,
                    departureEpochMillis = 0L,
                    minutesUntil = -1,
                    realtime = false
                )
            }
        } catch (_: Exception) {
            return null
        }
    }
}

