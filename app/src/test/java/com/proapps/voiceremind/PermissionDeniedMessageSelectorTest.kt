package com.proapps.voiceremind

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionDeniedMessageSelectorTest {

    @Test
    fun select_noneDenied_returnsNull() {
        val result = PermissionDeniedMessageSelector.select(
            microphoneDenied = false,
            calendarDenied = false,
            microphonePermanentlyDenied = false,
            calendarPermanentlyDenied = false
        )

        assertNull(result)
    }

    @Test
    fun select_microphoneTemporary() {
        val result = PermissionDeniedMessageSelector.select(
            microphoneDenied = true,
            calendarDenied = false,
            microphonePermanentlyDenied = false,
            calendarPermanentlyDenied = false
        )

        assertEquals(PermissionDeniedMessageType.MICROPHONE_TEMPORARY, result)
    }

    @Test
    fun select_microphonePermanent() {
        val result = PermissionDeniedMessageSelector.select(
            microphoneDenied = true,
            calendarDenied = false,
            microphonePermanentlyDenied = true,
            calendarPermanentlyDenied = false
        )

        assertEquals(PermissionDeniedMessageType.MICROPHONE_PERMANENT, result)
    }

    @Test
    fun select_calendarTemporary() {
        val result = PermissionDeniedMessageSelector.select(
            microphoneDenied = false,
            calendarDenied = true,
            microphonePermanentlyDenied = false,
            calendarPermanentlyDenied = false
        )

        assertEquals(PermissionDeniedMessageType.CALENDAR_TEMPORARY, result)
    }

    @Test
    fun select_calendarPermanent() {
        val result = PermissionDeniedMessageSelector.select(
            microphoneDenied = false,
            calendarDenied = true,
            microphonePermanentlyDenied = false,
            calendarPermanentlyDenied = true
        )

        assertEquals(PermissionDeniedMessageType.CALENDAR_PERMANENT, result)
    }

    @Test
    fun select_bothTemporary() {
        val result = PermissionDeniedMessageSelector.select(
            microphoneDenied = true,
            calendarDenied = true,
            microphonePermanentlyDenied = false,
            calendarPermanentlyDenied = false
        )

        assertEquals(PermissionDeniedMessageType.BOTH_TEMPORARY, result)
    }

    @Test
    fun select_bothMixedPermanent_treatedAsBothPermanent() {
        val result = PermissionDeniedMessageSelector.select(
            microphoneDenied = true,
            calendarDenied = true,
            microphonePermanentlyDenied = true,
            calendarPermanentlyDenied = false
        )

        assertEquals(PermissionDeniedMessageType.BOTH_PERMANENT, result)
    }
}

