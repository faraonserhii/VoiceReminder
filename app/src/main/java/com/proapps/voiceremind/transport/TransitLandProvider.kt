package com.proapps.voiceremind.transport

import com.proapps.voiceremind.BusDeparture
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import com.proapps.voiceremind.transport.gtfs.GTFSParser
import com.proapps.voiceremind.transport.gtfs.GTFSFeed
import android.content.Context

import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal TransitLand provider: attempts to find nearby stops via transit.land REST API.
 * NOTE: This is a lightweight implementation and currently only discovers stops; full schedule
 * integration (GTFS/GTFS-RT) is a planned follow-up.
 */
class TransitLandProvider(private val context: Context? = null) : TransportProvider {
    private val client = OkHttpClient()
    private val feedCache: MutableMap<String, Pair<GTFSFeed, Long>> = ConcurrentHashMap()
    private val feedTtlMs: Long = 1000L * 60L * 60L * 6L // 6 hours

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

                    // HYBRID: attempt to discover GTFS feed URLs from stop JSON and use GTFS for precise departures
                    try {
                        val feedUrls = mutableListOf<String>()
                        val feedFields = listOf("feeds", "served_feeds", "associated_feeds", "feed_urls", "feeds_onestop_ids")
                        for (f in feedFields) {
                            val arr = stop.optJSONArray(f)
                            if (arr != null) {
                                for (k in 0 until arr.length()) {
                                    val el = arr.get(k)
                                    if (el is String) feedUrls.add(el)
                                    else if (el is JSONObject) {
                                        val u = el.optString("url", el.optString("feed_url", el.optString("download_url", "")))
                                        if (!u.isNullOrBlank()) feedUrls.add(u)
                                    }
                                }
                            }
                        }

                        // sometimes stop object contains a nested 'feeds' object with metadata
                        if (feedUrls.isEmpty()) {
                            val maybeFeeds = stop.optJSONArray("feed_onestop_ids")
                            if (maybeFeeds != null) for (k in 0 until maybeFeeds.length()) feedUrls.add(maybeFeeds.optString(k))
                        }

                        // Try parsing each feed URL via GTFSParser (cached)
                        for (fuRaw in feedUrls) {
                            try {
                                val fu = resolveFeedUrlIfNeeded(fuRaw) ?: fuRaw
                                val now = System.currentTimeMillis()
                                val cached = feedCache[fu]
                                var feed: GTFSFeed? = null
                                var parsedUsedCacheFlag: Boolean? = null
                                if (cached != null && now - cached.second < feedTtlMs) {
                                    feed = cached.first
                                    parsedUsedCacheFlag = true
                                } else {
                                    val parsedPair = GTFSParser.downloadAndParse(fu, context?.cacheDir)
                                    if (parsedPair != null) {
                                        val parsedFeed = parsedPair.first
                                        val usedCache = parsedPair.second
                                        feedCache[fu] = Pair(parsedFeed, now)
                                        feed = parsedFeed
                                        parsedUsedCacheFlag = usedCache
                                    }
                                }

                                if (feed != null) {
                                    // try exact GTFS stop_id mapping first
                                    val gtfsStopId = extractGtfsStopId(stop)
                                    if (!gtfsStopId.isNullOrBlank()) {
                                        val times = feed.stopTimes[gtfsStopId] ?: emptyList()
                                        if (times.isNotEmpty()) {
                                            val nowMs = System.currentTimeMillis()
                                            var bestEpoch: Long? = null
                                            var bestHeadsign: String? = null
                                            for ((secs, tripId) in times) {
                                                val epoch = GTFSParser.secondsTodayToEpochMillis(secs)
                                                if (epoch >= nowMs) {
                                                        val trip = feed.trips[tripId]
                                                            // ensure trip's service is active today (if known)
                                                            if (trip?.serviceId != null && !feed.activeServiceIds.contains(trip.serviceId)) continue
                                                            val route = trip?.routeId?.let { feed.routes[it] }
                                                    val routeMatches = when {
                                                        route?.shortName != null -> route.shortName.equals(routeNumber, ignoreCase = true) || route.shortName.contains(routeNumber, ignoreCase = true)
                                                        trip?.headsign != null -> trip.headsign.contains(routeNumber, ignoreCase = true)
                                                        else -> true
                                                    }
                                                    if (!routeMatches) continue
                                                    if (bestEpoch == null || epoch < bestEpoch) {
                                                        bestEpoch = epoch
                                                        bestHeadsign = trip?.headsign
                                                    }
                                                }
                                            }
                                            if (bestEpoch != null) {
                                                val minutes = ((bestEpoch - System.currentTimeMillis()) / 60000).toInt()
                                                val sourceLabel = when {
                                                    cached != null -> "GTFS-disk"
                                                    parsedUsedCacheFlag == true -> "GTFS-disk"
                                                    parsedUsedCacheFlag == false -> "GTFS-network"
                                                    else -> "GTFS"
                                                }
                                                return BusDeparture(
                                                    route = routeNumber,
                                                    stopName = feed.stops.firstOrNull { it.stopId == gtfsStopId }?.name ?: name,
                                                    headsign = bestHeadsign,
                                                    departureEpochMillis = bestEpoch,
                                                    minutesUntil = minutes,
                                                    realtime = false,
                                                    source = sourceLabel
                                                )
                                            }
                                        }
                                    }
                                    // fallback: try find matching GTFS stop by name (best-effort)
                                    val stopNameLc = name?.lowercase() ?: ""
                                    val matched = feed.stops.firstOrNull { it.name.lowercase().contains(stopNameLc) || stopNameLc.contains(it.name.lowercase()) }
                                    if (matched != null) {
                                        val times = feed.stopTimes[matched.stopId] ?: emptyList()
                                        val nowMs = System.currentTimeMillis()
                                        var bestEpoch: Long? = null
                                        var bestHeadsign: String? = null
                                        var bestRealtime = false
                                        for ((secs, tripId) in times) {
                                            val epoch = GTFSParser.secondsTodayToEpochMillis(secs)
                                            if (epoch >= nowMs) {
                                                // route matching: check route short name or trip headsign
                                                val trip = feed.trips[tripId]
                                                if (trip?.serviceId != null && !feed.activeServiceIds.contains(trip.serviceId)) continue
                                                val route = trip?.routeId?.let { feed.routes[it] }
                                                val routeMatches = when {
                                                    route?.shortName != null -> route.shortName.equals(routeNumber, ignoreCase = true) || route.shortName.contains(routeNumber, ignoreCase = true)
                                                    trip?.headsign != null -> trip.headsign.contains(routeNumber, ignoreCase = true)
                                                    else -> true
                                                }
                                                if (!routeMatches) continue
                                                if (bestEpoch == null || epoch < bestEpoch) {
                                                    bestEpoch = epoch
                                                    bestHeadsign = trip?.headsign
                                                }
                                            }
                                        }
                                        if (bestEpoch != null) {
                                            val minutes = ((bestEpoch - System.currentTimeMillis()) / 60000).toInt()
                                            val sourceLabel = when {
                                                cached != null -> "GTFS-disk"
                                                parsedUsedCacheFlag == true -> "GTFS-disk"
                                                parsedUsedCacheFlag == false -> "GTFS-network"
                                                else -> "GTFS"
                                            }
                                            return BusDeparture(
                                                route = routeNumber,
                                                stopName = matched.name,
                                                headsign = bestHeadsign,
                                                departureEpochMillis = bestEpoch,
                                                minutesUntil = minutes,
                                                realtime = false,
                                                source = sourceLabel
                                            )
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                // try next feed URL
                            }
                        }
                    } catch (_: Exception) {
                        // ignore hybrid attempt errors
                    }

                    // hybrid helpers are implemented as class-level functions

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
                                                realtime = bestRealtime,
                                                source = "stop_schedules"
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
                            realtime = false,
                            source = "synthetic"
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

    // Resolve feed identifier (possibly onestop_id) to a download URL via transit.land feeds endpoint
    private fun resolveFeedUrlIfNeeded(raw: String): String? {
        try {
            if (raw.startsWith("http")) return raw
            val url = "https://transit.land/api/v2/rest/feeds?onestop_id=$raw&per_page=1"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return null
                val b = r.body?.string() ?: return null
                val jr = JSONObject(b)
                val arr = jr.optJSONArray("feeds") ?: jr.optJSONArray("results") ?: jr.optJSONArray("data")
                if (arr != null && arr.length() > 0) {
                    val f = arr.getJSONObject(0)
                    val download = f.optString("download_url", f.optString("url", f.optString("feed_url", "")))
                    if (!download.isNullOrBlank()) return download
                }
            }
        } catch (_: Exception) { }
        return null
    }

    // Try to extract GTFS stop_id from transit.land stop JSON (identifiers, codes etc.)
    private fun extractGtfsStopId(stopJson: org.json.JSONObject): String? {
        try {
            val candidates = listOf("gtfs_stop_id", "gtfs:stop_id", "stop_id", "feed_stop_id")
            for (c in candidates) {
                val v = stopJson.optString(c, "")
                if (!v.isNullOrBlank()) return v
            }
            if (stopJson.has("identifiers")) {
                val ids = stopJson.getJSONArray("identifiers")
                for (i in 0 until ids.length()) {
                    val idObj = ids.getJSONObject(i)
                    val idType = idObj.optString("type", "").lowercase()
                    val idVal = idObj.optString("identifier", idObj.optString("value", ""))
                    if (idType.contains("gtfs") || idType.contains("stop_id") || idType.contains("feed:stop")) return idVal
                }
            }
            if (stopJson.has("codes")) {
                val codes = stopJson.getJSONObject("codes")
                val gtfs = codes.optString("gtfs", "")
                if (!gtfs.isNullOrBlank()) return gtfs
            }
        } catch (_: Exception) { }
        return null
    }
}

