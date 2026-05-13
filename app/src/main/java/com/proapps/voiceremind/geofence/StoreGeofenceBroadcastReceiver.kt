package com.proapps.voiceremind.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.proapps.voiceremind.R

class StoreGeofenceBroadcastReceiver : BroadcastReceiver() {

    internal data class ParsedGeofenceEvent(
        val hasError: Boolean,
        val transition: Int
    )

    internal companion object {
        internal var eventExtractor: (Intent) -> ParsedGeofenceEvent? = { intent ->
            GeofencingEvent.fromIntent(intent)?.let { event ->
                ParsedGeofenceEvent(
                    hasError = event.hasError(),
                    transition = event.geofenceTransition
                )
            }
        }

        internal fun resetEventExtractorForTests() {
            eventExtractor = { intent ->
                GeofencingEvent.fromIntent(intent)?.let { event ->
                    ParsedGeofenceEvent(
                        hasError = event.hasError(),
                        transition = event.geofenceTransition
                    )
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val event = eventExtractor(intent) ?: return
        if (event.hasError) return
        handleTransition(
            context = context,
            transition = event.transition,
            rawPlaceName = intent.getStringExtra(StoreGeofenceManager.EXTRA_PLACE_NAME)
        )
    }

    internal fun handleTransition(context: Context, transition: Int, rawPlaceName: String?) {
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        handleEnterTransition(context, rawPlaceName)
    }

    internal fun handleEnterTransition(context: Context, rawPlaceName: String?) {
        val placeName = rawPlaceName
            .orEmpty()
            .ifBlank { context.getString(R.string.store_geo_generic_place) }

        val body = context.getString(R.string.store_geo_notification_body, placeName)

        StoreGeoNotifier.ensureChannel(context)
        StoreGeoNotifier.show(
            context = context,
            title = context.getString(R.string.store_geo_notification_title),
            body = body,
            notificationId = placeName.hashCode().coerceAtLeast(1)
        )
    }
}

