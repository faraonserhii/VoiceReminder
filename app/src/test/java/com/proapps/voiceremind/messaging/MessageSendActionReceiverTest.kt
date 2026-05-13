package com.proapps.voiceremind.messaging

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@Suppress("DEPRECATION")
class MessageSendActionReceiverTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun onReceive_preferredSms_startsSmsIntent() {
        registerSmsHandler()
        registerWhatsappHandler()

        MessageSendActionReceiver().onReceive(
            app,
            buildInput(preferred = "SMS")
        )

        val started = shadowOf(app).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_SENDTO, started?.action)
        assertEquals("smsto:12345", started?.dataString)
        assertEquals("I will be late", started?.getStringExtra("sms_body"))
    }

    @Test
    fun onReceive_smsUnavailable_fallsBackToWhatsapp() {
        registerWhatsappHandler()

        MessageSendActionReceiver().onReceive(
            app,
            buildInput(preferred = "SMS")
        )

        val started = shadowOf(app).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_SEND, started?.action)
        assertEquals("com.whatsapp", started?.`package`)
        assertEquals("I will be late", started?.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun onReceive_preferredWhatsapp_startsWhatsappIntent() {
        registerSmsHandler()
        registerWhatsappHandler()

        MessageSendActionReceiver().onReceive(
            app,
            buildInput(preferred = "WHATSAPP")
        )

        val started = shadowOf(app).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_SEND, started?.action)
        assertEquals("com.whatsapp", started?.`package`)
        assertEquals("I will be late", started?.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun onReceive_noAppsAvailable_doesNotStartActivity() {
        MessageSendActionReceiver().onReceive(
            app,
            buildInput(preferred = "SMS")
        )

        val started = shadowOf(app).nextStartedActivity
        assertEquals(null, started)
    }

    private fun buildInput(preferred: String): Intent {
        return Intent().apply {
            putExtra(MessageSendActionReceiver.EXTRA_RECIPIENT, "12345")
            putExtra(MessageSendActionReceiver.EXTRA_MESSAGE_TEXT, "I will be late")
            putExtra(MessageSendActionReceiver.EXTRA_PREFERRED_CHANNEL, preferred)
        }
    }

    private fun registerSmsHandler() {
        val smsResolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.example.sms"
                name = "com.example.sms.ComposeActivity"
            }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("smsto:") },
            smsResolveInfo
        )
        shadowOf(app.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("smsto:12345") },
            smsResolveInfo
        )
    }

    private fun registerWhatsappHandler() {
        val whatsappResolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.whatsapp"
                name = "com.whatsapp.Main"
            }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
            },
            whatsappResolveInfo
        )
    }
}

