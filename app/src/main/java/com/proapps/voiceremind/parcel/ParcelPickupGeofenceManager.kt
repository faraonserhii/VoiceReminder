package com.proapps.voiceremind.parcel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object ParcelPickupGeofenceManager {

    const val EXTRA_ITEM_LABEL = "extra_item_label"
    const val EXTRA_PICKUP_PLACE = "extra_pickup_place"
    const val EXTRA_DEADLINE_EPOCH_MILLIS = "extra_deadline_epoch_millis"

    private const val GEOFENCE_RADIUS_METERS = 250f

    @Suppress("MissingPermission")
    fun registerParcelEnterGeofence(
        context: Context,
        requestId: String,
        latitude: Double,
        longitude: Double,
        itemLabel: String,
        pickupPlace: String,
        deadlineEpochMillis: Long
    ) {
        val expirationDuration = (deadlineEpochMillis - System.currentTimeMillis()).coerceAtLeast(60_000L)

        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_METERS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(expirationDuration)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
        geofencingClient.addGeofences(
            request,
            pendingIntent(
                context = context,
                itemLabel = itemLabel,
                pickupPlace = pickupPlace,
                deadlineEpochMillis = deadlineEpochMillis
            )
        )
    }

    private fun pendingIntent(
        context: Context,
        itemLabel: String,
        pickupPlace: String,
        deadlineEpochMillis: Long
    ): PendingIntent {
        val intent = Intent(context, ParcelPickupGeofenceBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_LABEL, itemLabel)
            putExtra(EXTRA_PICKUP_PLACE, pickupPlace)
            putExtra(EXTRA_DEADLINE_EPOCH_MILLIS, deadlineEpochMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            (itemLabel + pickupPlace).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

