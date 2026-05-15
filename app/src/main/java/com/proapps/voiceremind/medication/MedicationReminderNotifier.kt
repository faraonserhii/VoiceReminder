package com.proapps.voiceremind.medication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.proapps.voiceremind.R

object MedicationReminderNotifier {

    private const val CHANNEL_ID = "medication_reminder_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.medication_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.medication_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        logId: String,
        notificationId: Int
    ) {
        val takenIntent = Intent(context, MedicationTakenReceiver::class.java).apply {
            putExtra(MedicationTakenReceiver.EXTRA_LOG_ID, logId)
            putExtra(MedicationTakenReceiver.EXTRA_TITLE, title)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_info_24)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.medication_action_taken), takenPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

