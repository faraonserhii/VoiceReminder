package com.proapps.voiceremind.parcel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.proapps.voiceremind.R

class ParcelPickupGeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val itemLabel = intent.getStringExtra(ParcelPickupGeofenceManager.EXTRA_ITEM_LABEL)
            .orEmpty()
            .ifBlank { context.getString(R.string.parcel_default_item) }
        val pickupPlace = intent.getStringExtra(ParcelPickupGeofenceManager.EXTRA_PICKUP_PLACE)
            .orEmpty()
            .ifBlank { context.getString(R.string.parcel_default_place) }

        ParcelPickupNotifier.ensureChannel(context)
        ParcelPickupNotifier.show(
            context = context,
            title = context.getString(R.string.parcel_notification_title),
            body = context.getString(R.string.parcel_notification_body, itemLabel, pickupPlace),
            notificationId = (itemLabel + pickupPlace).hashCode().coerceAtLeast(1)
        )
    }
}

