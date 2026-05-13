package com.proapps.voiceremind.messaging

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MessageReminderScheduler {

    fun schedule(
        context: Context,
        reminderId: String,
        triggerEpochMillis: Long,
        recipient: String?,
        messageText: String,
        preferredChannel: String
    ) {
        val delayMillis = (triggerEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val input = Data.Builder()
            .putLong(MessageReminderWorker.KEY_TRIGGER_EPOCH_MILLIS, triggerEpochMillis)
            .putString(MessageReminderWorker.KEY_RECIPIENT, recipient)
            .putString(MessageReminderWorker.KEY_MESSAGE_TEXT, messageText)
            .putString(MessageReminderWorker.KEY_PREFERRED_CHANNEL, preferredChannel)
            .build()

        val request = OneTimeWorkRequestBuilder<MessageReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "message_reminder_$reminderId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

