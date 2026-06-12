package com.proapps.voiceremind.transport.gtfs

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.zip.ZipInputStream
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

object GTFSParser {
    private val client = OkHttpClient()

    fun downloadAndParse(feedUrl: String): GTFSFeed? {
        try {
            val req = Request.Builder().url(feedUrl).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.byteStream() ?: return null
                ZipInputStream(body).use { zis ->
                    val stops = mutableListOf<GTFSStop>()
                    val routes = mutableMapOf<String, GTFSRoute>()
                    val trips = mutableMapOf<String, GTFSTrip>()
                    val stopTimes = mutableMapOf<String, MutableList<Pair<Int, String>>>()
                    val calendarEntries = mutableMapOf<String, MutableMap<String, String>>()
                    val calendarDates = mutableListOf<Triple<String, String, Int>>() // service_id, date(YYYYMMDD), exception_type

                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.lowercase()
                        if (name.endsWith("stops.txt")) {
                            val reader = BufferedReader(InputStreamReader(zis))
                            parseCsv(reader) { map ->
                                val id = map["stop_id"] ?: map["id"] ?: return@parseCsv
                                val nm = map["stop_name"] ?: map["name"] ?: ""
                                stops.add(GTFSStop(id, nm))
                            }
                        } else if (name.endsWith("routes.txt")) {
                            val reader = BufferedReader(InputStreamReader(zis))
                            parseCsv(reader) { map ->
                                val id = map["route_id"] ?: return@parseCsv
                                val shortName = map["route_short_name"] ?: map["short_name"]
                                routes[id] = GTFSRoute(id, shortName)
                            }
                        } else if (name.endsWith("trips.txt")) {
                            val reader = BufferedReader(InputStreamReader(zis))
                            parseCsv(reader) { map ->
                                val tripId = map["trip_id"] ?: return@parseCsv
                                val routeId = map["route_id"] ?: ""
                                val headsign = map["trip_headsign"] ?: map["headsign"]
                                val serviceId = map["service_id"] ?: map["service"]
                                trips[tripId] = GTFSTrip(tripId, routeId, headsign, serviceId)
                            }
                        } else if (name.endsWith("stop_times.txt")) {
                            val reader = BufferedReader(InputStreamReader(zis))
                            parseCsv(reader) { map ->
                                val tripId = map["trip_id"] ?: return@parseCsv
                                val stopId = map["stop_id"] ?: return@parseCsv
                                val dep = map["departure_time"] ?: map["arrival_time"] ?: return@parseCsv
                                val secs = hhMmSsToSeconds(dep)
                                if (secs >= 0) {
                                    val list = stopTimes.getOrPut(stopId) { mutableListOf() }
                                    list.add(Pair(secs, tripId))
                                }
                            }
                        } else if (name.endsWith("calendar.txt")) {
                            val reader = BufferedReader(InputStreamReader(zis))
                            parseCsv(reader) { map ->
                                val sid = map["service_id"] ?: return@parseCsv
                                calendarEntries[sid] = map.toMutableMap()
                            }
                        } else if (name.endsWith("calendar_dates.txt")) {
                            val reader = BufferedReader(InputStreamReader(zis))
                            parseCsv(reader) { map ->
                                val sid = map["service_id"] ?: return@parseCsv
                                val date = map["date"] ?: return@parseCsv
                                val ex = map["exception_type"]?.toIntOrNull() ?: map["exception"]?.toIntOrNull() ?: 1
                                calendarDates.add(Triple(sid, date, ex))
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }

                    // compute active service_ids for today using calendar.txt and calendar_dates.txt
                    val active = mutableSetOf<String>()
                    try {
                        val zone = ZoneId.systemDefault()
                        val today = LocalDate.now(zone)
                        val fmt = DateTimeFormatter.ofPattern("yyyyMMdd")

                        for ((sid, row) in calendarEntries) {
                            try {
                                val start = row["start_date"]?.let { LocalDate.parse(it, fmt) } ?: LocalDate.MIN
                                val end = row["end_date"]?.let { LocalDate.parse(it, fmt) } ?: LocalDate.MAX
                                if (today.isBefore(start) || today.isAfter(end)) continue
                                val dow = today.dayOfWeek // MONDAY..SUNDAY
                                val idx = when (dow) {
                                    DayOfWeek.MONDAY -> "monday"
                                    DayOfWeek.TUESDAY -> "tuesday"
                                    DayOfWeek.WEDNESDAY -> "wednesday"
                                    DayOfWeek.THURSDAY -> "thursday"
                                    DayOfWeek.FRIDAY -> "friday"
                                    DayOfWeek.SATURDAY -> "saturday"
                                    DayOfWeek.SUNDAY -> "sunday"
                                }
                                val flag = row[idx]?.trim() ?: "0"
                                if (flag == "1" || flag.equals("true", true)) active.add(sid)
                            } catch (_: Exception) { }
                        }

                        // apply calendar_dates exceptions
                        for ((sid, dateStr, ex) in calendarDates) {
                            try {
                                val d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
                                if (d == today) {
                                    if (ex == 1) active.add(sid) else if (ex == 2) active.remove(sid)
                                }
                            } catch (_: Exception) { }
                        }
                    } catch (_: Exception) { }

                    return GTFSFeed(feedUrl, stops, routes, trips, stopTimes, active)
                }
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseCsv(reader: BufferedReader, rowHandler: (Map<String, String>) -> Unit) {
        var header: List<String>? = null
        reader.useLines { seq ->
            seq.forEach { lineRaw ->
                val line = lineRaw.trim()
                if (line.isEmpty()) return@forEach
                val cols = splitCsvLine(line)
                if (header == null) {
                    header = cols
                } else {
                    val map = mutableMapOf<String, String>()
                    for (i in header!!.indices) {
                        val key = header!![i].trim().lowercase()
                        val value = if (i < cols.size) cols[i].trim() else ""
                        map[key] = value
                    }
                    rowHandler(map)
                }
            }
        }
    }

    // naive CSV splitter that handles quoted fields with commas
    private fun splitCsvLine(line: String): List<String> {
        val res = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    cur.append('"')
                    i += 1
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                res.add(cur.toString())
                cur = StringBuilder()
            } else {
                cur.append(c)
            }
            i += 1
        }
        res.add(cur.toString())
        return res
    }

    private fun hhMmSsToSeconds(t: String): Int {
        try {
            val parts = t.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                val s = if (parts.size >= 3) parts[2].toIntOrNull() ?: 0 else 0
                return h * 3600 + m * 60 + s
            }
        } catch (_: Exception) {}
        return -1
    }

    // Compute epoch millis for today's date + seconds since midnight, handling >24:00
    fun secondsTodayToEpochMillis(seconds: Int): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val daysToAdd = seconds / 86400
        val secs = seconds % 86400
        val dt = ZonedDateTime.of(today.plusDays(daysToAdd.toLong()).atStartOfDay(), zone).plusSeconds(secs.toLong())
        return dt.toInstant().toEpochMilli()
    }
}

