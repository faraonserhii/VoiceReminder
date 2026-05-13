package com.proapps.voiceremind

import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MessageReminderParserTest {

    @Test
    fun extract_ruCommand_parsesRecipientTimeAndBody() {
        val command = MessageReminderParser.extract(
            "Напомни отправить сообщение жене в 17:00, что я задержусь"
        )

        assertNotNull(command)
        assertEquals("жене", command?.recipient)
        assertEquals(LocalTime.of(17, 0), command?.explicitTime)
        assertEquals("я задержусь", command?.messageText)
        assertEquals(MessageChannel.SMS, command?.preferredChannel)
    }

    @Test
    fun extract_detectsWhatsappPreference() {
        val command = MessageReminderParser.extract(
            "Напомни отправить сообщение брату в WhatsApp в 19:30, что буду позже"
        )

        assertEquals(MessageChannel.WHATSAPP, command?.preferredChannel)
    }

    @Test
    fun extract_ruTomorrow_setsDayShiftAndKeepsRecipient() {
        val command = MessageReminderParser.extract(
            "Напомни отправить сообщение жене завтра в 18:10, что опоздаю"
        )

        assertNotNull(command)
        assertEquals(1L, command?.dayShift)
        assertEquals("жене", command?.recipient)
        assertEquals(LocalTime.of(18, 10), command?.explicitTime)
    }

    @Test
    fun extract_enCommand_parsesRecipientTimeAndBody() {
        val command = MessageReminderParser.extract(
            "Remind me to send a message to wife at 5:45 pm that I will be late"
        )

        assertNotNull(command)
        assertEquals("to wife", command?.recipient)
        assertEquals(LocalTime.of(17, 45), command?.explicitTime)
        assertEquals("I will be late", command?.messageText)
    }

    @Test
    fun extract_withoutWhatThat_usesFallbackMessageText() {
        val command = MessageReminderParser.extract(
            "Напомни отправить сообщение маме в 20:00"
        )

        assertNotNull(command)
        assertEquals("маме", command?.recipient)
        assertEquals("Сообщение для маме", command?.messageText)
    }

    @Test
    fun extract_withoutRecipient_setsRecipientNull() {
        val command = MessageReminderParser.extract(
            "Напомни отправить сообщение в 20:00, что задержусь"
        )

        assertNotNull(command)
        assertEquals(null, command?.recipient)
        assertEquals("задержусь", command?.messageText)
    }

    @Test
    fun extract_enWithoutRecipient_setsRecipientNull() {
        val command = MessageReminderParser.extract(
            "Remind me to send a message at 8:00 pm that I am running late"
        )

        assertNotNull(command)
        assertEquals(null, command?.recipient)
        assertEquals(LocalTime.of(20, 0), command?.explicitTime)
        assertEquals("I am running late", command?.messageText)
    }

    @Test
    fun resolveTriggerDateTime_movesPastTimeToNextDay() {
        val command = MessageReminderParser.extract(
            "Напомни отправить сообщение жене в 09:00, что я задержусь"
        )

        val resolved = command?.resolveTriggerDateTime(
            now = LocalDateTime.of(2026, 5, 13, 10, 0),
            defaultTime = LocalTime.of(12, 0)
        )

        assertEquals(LocalDateTime.of(2026, 5, 14, 9, 0), resolved)
    }

    @Test
    fun extract_returnsNullForRegularReminder() {
        val command = MessageReminderParser.extract("Встреча завтра в 10:00")

        assertEquals(null, command)
    }
}

