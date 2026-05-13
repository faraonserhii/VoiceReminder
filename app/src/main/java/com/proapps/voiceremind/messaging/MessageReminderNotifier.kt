package com.proapps.voiceremind.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.proapps.voiceremind.R

object MessageReminderNotifier {

    private const val CHANNEL_ID = "message_reminder_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.message_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.message_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        recipient: String?,
        messageText: String,
        preferredChannel: String,
        notificationId: Int
    ) {
        val actionIntent = Intent(context, MessageSendActionReceiver::class.java).apply {
            putExtra(MessageSendActionReceiver.EXTRA_RECIPIENT, recipient)
            putExtra(MessageSendActionReceiver.EXTRA_MESSAGE_TEXT, messageText)
            putExtra(MessageSendActionReceiver.EXTRA_PREFERRED_CHANNEL, preferredChannel)
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_info_24)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.message_action_send), actionPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

