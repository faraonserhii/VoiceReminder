package com.proapps.voiceremind.sahko

import java.util.Locale

data class SahkoVahtiCommand(
    val applianceLabel: String
)

object SahkoVahtiCommandParser {

    private val applianceMarkerRegex = Regex(
        """(?i)(включить|вруби|запусти|turn\s+on|start|run|kaynnista|käynnistä)\s+([\p{L}\p{N}\s-]{2,40})"""
    )

    fun extract(rawText: String): SahkoVahtiCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val lower = text.lowercase(Locale.ROOT)
        val mentionsElectricity = lower.contains("электрич") ||
            lower.contains("тариф") ||
            lower.contains("спот") ||
            lower.contains("porssisahko") ||
            lower.contains("porssi") ||
            lower.contains("pörssisähkö") ||
            lower.contains("sahko") ||
            lower.contains("sähkö") ||
            lower.contains("electricity")
        val mentionsCheap = lower.contains("дешев") ||
            lower.contains("дешёв") ||
            lower.contains("cheap") ||
            lower.contains("lower") ||
            lower.contains("low") ||
            lower.contains("halpa")
        val asksTurnOn = lower.contains("включ") ||
            lower.contains("вруби") ||
            lower.contains("запусти") ||
            lower.contains("turn on") ||
            lower.contains("start") ||
            lower.contains("run") ||
            lower.contains("kaynnista") ||
            lower.contains("kaynn") ||
            lower.contains("käynn")

        if (!(mentionsElectricity && mentionsCheap && asksTurnOn)) return null

        val label = extractApplianceLabel(text).ifBlank { "прибор" }
        return SahkoVahtiCommand(applianceLabel = label)
    }

    private fun extractApplianceLabel(text: String): String {
        val match = applianceMarkerRegex.find(text) ?: return ""
        val raw = match.groupValues[2]
        return raw
            .substringBefore(',')
            .substringBefore("когда")
            .substringBefore("when")
            .substringBefore("если")
            .substringBefore("if")
            .substringBefore("kun")
            .trim(' ', '.', '!', '?')
    }
}

