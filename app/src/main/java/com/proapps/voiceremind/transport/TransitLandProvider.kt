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
            // 1) Find nearby stops
            val stopsUrl = "https://transit.land/api/v2/rest/stops?lat=$lat&lon=$lon&r=$radiusMeters&per_page=10"
            val stopsReq = Request.Builder().url(stopsUrl).get().build()
            client.newCall(stopsReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val stops = root.optJSONArray("stops") ?: return null
                if (stops.length() == 0) return null

                // iterate stops (prefer closest = first) and try to fetch schedules for each
                for (i in 0 until stops.length()) {
                    val stop = stops.getJSONObject(i)
                    val name = stop.optString("name")
                    val onestopId = stop.optString("onestop_id", null)

                    if (!onestopId.isNullOrBlank()) {
                        // Try stop_schedules endpoint (transit.land v2)
                        try {
                            val schedulesUrl = "https://transit.land/api/v2/rest/stop_schedules?stop_onestop_id=$onestopId&per_page=20"
                            val schReq = Request.Builder().url(schedulesUrl).get().build()
                            client.newCall(schReq).execute().use { sresp ->
                                if (!sresp.isSuccessful) {
                                    // fallthrough to next stop
                                } else {
                                    val sbody = sresp.body?.string() ?: ""
                                    if (sbody.isNotBlank()) {
                                        val sroot = JSONObject(sbody)
                                        // try common array names
                                        val candidates = mutableListOf<org.json.JSONObject>()
                                        val arrNames = listOf("stop_schedules", "schedules", "data", "stop_times")
                                        for (nameKey in arrNames) {
                                            val arr = sroot.optJSONArray(nameKey)
                                            if (arr != null) {
                                                for (j in 0 until arr.length()) candidates.add(arr.getJSONObject(j))
                                            }
                                        }

                                        // If no array found, but root has one stop_schedule object
                                        if (candidates.isEmpty() && sroot.has("stop_schedule")) {
                                            candidates.add(sroot.getJSONObject("stop_schedule"))
                                        }

                                        // Also accept top-level array if present
                                        if (candidates.isEmpty()) {
                                            val topArr = sroot.optJSONArray("results") ?: sroot.optJSONArray("items")
                                            if (topArr != null) for (j in 0 until topArr.length()) candidates.add(topArr.getJSONObject(j))
                                        }

                                        // Parse timestamps and pick the earliest future departure matching route (best-effort)
                                        var bestEpoch: Long? = null
                                        var bestHeadsign: String? = null
                                        var bestRealtime = false
                                        for (obj in candidates) {
                                            try {
                                                // route matching heuristics: try route_onestop_id or route_id or route
                                                val matchesRoute = when {
                                                    obj.has("route_onestop_id") -> {
                                                        val r = obj.optString("route_onestop_id")
                                                        r.contains(routeNumber, ignoreCase = true) || r.endsWith(routeNumber)
                                                    }
                                                    obj.has("route_id") -> obj.optString("route_id").contains(routeNumber, ignoreCase = true)
                                                    obj.has("route") -> obj.optString("route").contains(routeNumber, ignoreCase = true)
                                                    else -> true // if we can't check, don't filter too much
                                                }

                                                if (!matchesRoute) continue

                                                val epoch = parseFlexibleEpochMillis(obj)
                                                if (epoch != null) {
                                                    val now = Instant.now().toEpochMilli()
                                                    if (epoch >= now) {
                                                        if (bestEpoch == null || epoch < bestEpoch) {
                                                            bestEpoch = epoch
                                                            bestHeadsign = obj.optString("headsign", obj.optString("trip_headsign", null))
                                                            bestRealtime = obj.optBoolean("realtime", obj.optBoolean("predicted", false))
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                // ignore per-item parsing errors
                                            }
                                        }

                                        if (bestEpoch != null) {
                                            val minutes = ((bestEpoch - Instant.now().toEpochMilli()) / 60000).toInt()
                                            return BusDeparture(
                                                route = routeNumber,
                                                stopName = if (name.isNullOrBlank()) onestopId else name,
                                                headsign = bestHeadsign,
                                                departureEpochMillis = bestEpoch,
                                                minutesUntil = minutes,
                                                realtime = bestRealtime
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // try next stop
                        }
                    }

                    // If no onestop_id or no schedules, fall back to synthetic for this stop if it's the first
                    if (i == 0) {
                        return BusDeparture(
                            route = routeNumber,
                            stopName = if (name.isNullOrBlank()) "stop" else name,
                            headsign = null,
                            departureEpochMillis = 0L,
                            minutesUntil = -1,
                            realtime = false
                        )
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }

    // Attempt to parse various timestamp representations from transit.land stop schedule items
    private fun parseFlexibleEpochMillis(obj: org.json.JSONObject): Long? {
        try {
            if (obj.has("departure_utc")) {
                val s = obj.optString("departure_utc")
                if (s.isNotBlank()) return Instant.parse(s).toEpochMilli()
            }
            if (obj.has("predicted_departure_utc")) {
                val s = obj.optString("predicted_departure_utc")
                if (s.isNotBlank()) return Instant.parse(s).toEpochMilli()
            }
            if (obj.has("departure_timestamp")) {
                val v = obj.optLong("departure_timestamp", -1L)
                if (v > 0) return v * 1000L
            }
            if (obj.has("arrival_timestamp")) {
                val v = obj.optLong("arrival_timestamp", -1L)
                if (v > 0) return v * 1000L
            }
            // GTFS-style: service_day (seconds since epoch) + departure_time "HH:MM:SS"
            if (obj.has("service_day") && (obj.has("departure_time") || obj.has("arrival_time"))) {
                val sd = obj.optLong("service_day", -1L)
                if (sd > 0) {
                    val t = obj.optString("departure_time", obj.optString("arrival_time", ""))
                    val secs = parseHhMmSsToSeconds(t)
                    if (secs >= 0) return (sd + secs) * 1000L
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun parseHhMmSsToSeconds(time: String): Long {
        try {
            val parts = time.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toLongOrNull() ?: 0L
                val m = parts[1].toLongOrNull() ?: 0L
                val s = if (parts.size >= 3) parts[2].toLongOrNull() ?: 0L else 0L
                return h * 3600 + m * 60 + s
            }
        } catch (_: Exception) {
        }
        return -1L
    }
}

