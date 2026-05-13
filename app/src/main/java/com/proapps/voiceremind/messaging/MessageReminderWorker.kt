package com.proapps.voiceremind.messaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.proapps.voiceremind.R

class MessageReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val triggerEpochMillis = inputData.getLong(KEY_TRIGGER_EPOCH_MILLIS, -1L)
        if (triggerEpochMillis <= 0L) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val recipient = inputData.getString(KEY_RECIPIENT)
        val messageText = inputData.getString(KEY_MESSAGE_TEXT).orEmpty().ifBlank {
            applicationContext.getString(R.string.message_default_text)
        }
        val preferredChannel = inputData.getString(KEY_PREFERRED_CHANNEL).orEmpty().ifBlank { "SMS" }

        val notificationBody = if (recipient.isNullOrBlank()) {
            applicationContext.getString(R.string.message_notification_body_no_recipient, messageText)
        } else {
            applicationContext.getString(R.string.message_notification_body, recipient, messageText)
        }

        MessageReminderNotifier.ensureChannel(applicationContext)
        MessageReminderNotifier.show(
            context = applicationContext,
            title = applicationContext.getString(R.string.message_notification_title),
            body = notificationBody,
            recipient = recipient,
            messageText = messageText,
            preferredChannel = preferredChannel,
            notificationId = buildNotificationId(triggerEpochMillis)
        )

        return Result.success()
    }

    private fun buildNotificationId(triggerEpochMillis: Long): Int {
        return (triggerEpochMillis % Int.MAX_VALUE).toInt().coerceAtLeast(1)
    }

    companion object {
        const val KEY_TRIGGER_EPOCH_MILLIS = "key_trigger_epoch_millis"
        const val KEY_RECIPIENT = "key_recipient"
        const val KEY_MESSAGE_TEXT = "key_message_text"
        const val KEY_PREFERRED_CHANNEL = "key_preferred_channel"
    }
}

