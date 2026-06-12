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
import com.proapps.voiceremind.transport.DigitransitProvider
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
                // First, detect country for the coordinates. If not Finland, fall back to a generic Overpass lookup
                val country = detectCountryCode(lat, lon)
                if (country == null) {
                    // couldn't detect country, proceed with digitransit for best effort
                } else if (!country.equals("FI", ignoreCase = true)) {
                    // Try to find nearest stop via Overpass and inform user that schedule provider is not available
                    val stopName = findNearestStopOverpass(lat, lon, radiusMeters)
                    val message = if (stopName != null) {
                        context.getString(R.string.bus_schedule_unsupported_country, country, stopName)
                    } else {
                        context.getString(R.string.bus_schedule_unsupported_no_stop, country)
                    }
                    // Notify UI with a synthetic BusDeparture marking unsupported schedule (minutesUntil = -1)
                    val synthetic = BusDeparture(
                        route = routeNumber,
                        stopName = stopName ?: context.getString(R.string.bus_no_stop_found_label),
                        headsign = null,
                        departureEpochMillis = 0L,
                        minutesUntil = -1,
                        realtime = false
                    )
                    resultCallback?.let { cb -> mainHandler.post { cb(synthetic) } }
                    speakOnMain(message)
                    return@Thread
                }

                // use Digitransit provider for supported regions (router param allows selecting router)
                val provider = DigitransitProvider(router)
                val departure = provider.findNextDeparture(routeNumber, lat, lon, radiusMeters)
                // notify caller (UI) on main thread with structured result
                resultCallback?.let { cb ->
                    mainHandler.post { cb(departure) }
                }
                // speak human-friendly sentence
                val speech = speechFromDeparture(departure, routeNumber)
                speakOnMain(speech)
            } catch (e: Exception) {
                Log.e("BusTimeAnnouncer", "failed", e)
                speakOnMain(context.getString(R.string.bus_time_error_generic, e.message ?: ""))
            }
        }.start()
    }

    private fun detectCountryCode(lat: Double, lon: Double): String? {
        return try {
            val url = "https://geocoding-api.open-meteo.com/v1/reverse?latitude=$lat&longitude=$lon&count=1"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val results = root.optJSONArray("results") ?: return null
                if (results.length() == 0) return null
                val obj = results.getJSONObject(0)
                obj.optString("country_code").ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findNearestStopOverpass(lat: Double, lon: Double, radius: Int): String? {
        return try {
            val q = """
                [out:json][timeout:25];
                (
                  node(around:$radius,$lat,$lon)["highway"="bus_stop"];
                  node(around:$radius,$lat,$lon)["public_transport"="platform"];
                );
                out body 1;
            """.trimIndent()

            val mediaType = "text/plain; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(q.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val elements = root.optJSONArray("elements") ?: return null
                if (elements.length() == 0) return null
                for (i in 0 until elements.length()) {
                    val el = elements.getJSONObject(i)
                    val tags = el.optJSONObject("tags")
                    if (tags != null) {
                        val name = tags.optString("name")
                        if (!name.isNullOrBlank()) return name
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.w("BusTimeAnnouncer", "overpass failed", e)
            null
        }
    }

    // parsing of Digitransit responses is handled inside provider implementations

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

