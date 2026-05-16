package com.proapps.voiceremind

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Небольшой helper для запроса ближайшего рейса через Digitransit GraphQL и проговаривания через TTS.
 * Минимальная, sync-реализация: сетевой вызов выполняется в фоновом потоке, результат проговаривается на UI-потоке.
 */
data class BusDeparture(
    val route: String,
    val stopName: String,
    val headsign: String?,
    val departureEpochMillis: Long,
    val minutesUntil: Int,
    val realtime: Boolean
)

class BusTimeAnnouncer(private val context: Context) : TextToSpeech.OnInitListener {

    private val client = OkHttpClient()
    private var tts: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // выбор языка — можно поменять на Locale("fi") для финского
            tts?.language = Locale.getDefault()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    /**
     * Асинхронно запрашивает ближайший рейс маршрута [routeNumber] около координат [lat],[lon]
     * и проговаривает результат через TTS.
     * router — Digitransit router (по умолчанию "hsl").
     */
    fun announceNextBus(
        routeNumber: String,
        lat: Double,
        lon: Double,
        router: String = "hsl",
        radiusMeters: Int = 400,
        resultCallback: ((BusDeparture?) -> Unit)? = null
    ) {
        Thread {
            try {
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

                val departure = parseBestDeparture(respStr, routeNumber)
                // notify caller (UI) on main thread with structured result
                resultCallback?.let { cb ->
                    mainHandler.post { cb(departure) }
                }
                // speak human-friendly sentence
                val speech = speechFromDeparture(departure, routeNumber)
                speakOnMain(speech)
            } catch (e: Exception) {
                Log.e("BusTimeAnnouncer", "failed", e)
                speakOnMain("Ошибка получения расписания: ${e.message}")
            }
        }.start()
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

    private fun speechFromDeparture(departure: BusDeparture?, routeNumber: String): String {
        if (departure == null) return context.getString(R.string.bus_time_not_found, routeNumber)

        val whenText = when (departure.minutesUntil) {
            0 -> context.getString(R.string.bus_time_now)
            1 -> context.getString(R.string.bus_time_one)
            else -> context.resources.getQuantityString(R.plurals.minutes_until, departure.minutesUntil, departure.minutesUntil)
        }

        val headsignPart = if (!departure.headsign.isNullOrBlank()) context.getString(R.string.bus_headsign_prefix, departure.headsign) else ""
        val realtimeNote = if (departure.realtime) context.getString(R.string.bus_realtime_note) else ""

        return context.getString(
            R.string.bus_time_template,
            departure.route,
            headsignPart,
            departure.stopName,
            whenText,
            realtimeNote
        )
    }

    private fun speakOnMain(text: String) {
        mainHandler.post {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bus_time_utterance")
        }
    }
}

