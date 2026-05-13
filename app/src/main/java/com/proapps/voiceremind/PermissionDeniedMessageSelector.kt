package com.proapps.voiceremind

enum class PermissionDeniedMessageType {
    MICROPHONE_TEMPORARY,
    CALENDAR_TEMPORARY,
    BOTH_TEMPORARY,
    MICROPHONE_PERMANENT,
    CALENDAR_PERMANENT,
    BOTH_PERMANENT
}

object PermissionDeniedMessageSelector {

    fun select(
        microphoneDenied: Boolean,
        calendarDenied: Boolean,
        microphonePermanentlyDenied: Boolean,
        calendarPermanentlyDenied: Boolean
    ): PermissionDeniedMessageType? {
        return when {
            !microphoneDenied && !calendarDenied -> null
            microphoneDenied && calendarDenied && microphonePermanentlyDenied && calendarPermanentlyDenied -> PermissionDeniedMessageType.BOTH_PERMANENT
            microphoneDenied && calendarDenied && (microphonePermanentlyDenied || calendarPermanentlyDenied) -> PermissionDeniedMessageType.BOTH_PERMANENT
            microphoneDenied && calendarDenied -> PermissionDeniedMessageType.BOTH_TEMPORARY
            microphoneDenied && microphonePermanentlyDenied -> PermissionDeniedMessageType.MICROPHONE_PERMANENT
            microphoneDenied -> PermissionDeniedMessageType.MICROPHONE_TEMPORARY
            calendarPermanentlyDenied -> PermissionDeniedMessageType.CALENDAR_PERMANENT
            else -> PermissionDeniedMessageType.CALENDAR_TEMPORARY
        }
    }
}

