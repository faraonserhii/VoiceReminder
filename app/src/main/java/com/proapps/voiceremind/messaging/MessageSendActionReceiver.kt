package com.proapps.voiceremind.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.proapps.voiceremind.R

class MessageSendActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val recipient = intent.getStringExtra(EXTRA_RECIPIENT).orEmpty().trim()
        val messageText = intent.getStringExtra(EXTRA_MESSAGE_TEXT).orEmpty().trim()
        val preferredChannel = intent.getStringExtra(EXTRA_PREFERRED_CHANNEL).orEmpty().trim()

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(recipient)}")
            putExtra("sms_body", messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val packageManager = context.packageManager
        val canSms = smsIntent.resolveActivity(packageManager) != null
        val canWhatsapp = whatsappIntent.resolveActivity(packageManager) != null

        val targetIntent = when {
            preferredChannel.equals("WHATSAPP", ignoreCase = true) && canWhatsapp -> whatsappIntent
            preferredChannel.equals("SMS", ignoreCase = true) && canSms -> smsIntent
            canSms -> smsIntent
            canWhatsapp -> whatsappIntent
            else -> null
        }

        if (targetIntent == null) {
            Toast.makeText(context, context.getString(R.string.message_no_app_found), Toast.LENGTH_LONG).show()
            return
        }

        context.startActivity(targetIntent)
    }

    companion object {
        const val EXTRA_RECIPIENT = "extra_recipient"
        const val EXTRA_MESSAGE_TEXT = "extra_message_text"
        const val EXTRA_PREFERRED_CHANNEL = "extra_preferred_channel"
    }
}

