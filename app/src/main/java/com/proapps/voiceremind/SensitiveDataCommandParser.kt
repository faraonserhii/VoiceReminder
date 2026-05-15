package com.proapps.voiceremind

import java.util.Locale

enum class SensitiveDataType(val labelRes: Int) {
    TAX_NUMBER(R.string.sensitive_data_type_tax),
    PERSONAL_ID(R.string.sensitive_data_type_personal_id),
    INSURANCE_NUMBER(R.string.sensitive_data_type_insurance)
}

sealed class SensitiveDataCommand {
    data class Save(val type: SensitiveDataType, val value: String) : SensitiveDataCommand()
    data class Query(val type: SensitiveDataType) : SensitiveDataCommand()
}

object SensitiveDataCommandParser {

    fun parse(rawText: String): SensitiveDataCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val lower = text.lowercase(Locale.ROOT)
        val type = detectType(lower) ?: return null

        val isSave = lower.contains("запомни") || lower.contains("сохрани") ||
            lower.contains("remember") || lower.contains("save")
        val isQuery = lower.contains("какой") || lower.contains("покажи") ||
            lower.contains("what is") || lower.contains("show")

        if (isSave) {
            val value = extractValue(text)
            if (value.length < 3) return null
            return SensitiveDataCommand.Save(type = type, value = value)
        }

        if (isQuery) {
            return SensitiveDataCommand.Query(type = type)
        }

        return null
    }

    private fun detectType(lower: String): SensitiveDataType? {
        return when {
            lower.contains("налог") || lower.contains("veronumero") || lower.contains("tax number") -> {
                SensitiveDataType.TAX_NUMBER
            }
            lower.contains("henkil") || lower.contains("социаль") || lower.contains("личн") || lower.contains("personal id") -> {
                SensitiveDataType.PERSONAL_ID
            }
            lower.contains("kela") || lower.contains("страхов") || lower.contains("insurance") -> {
                SensitiveDataType.INSURANCE_NUMBER
            }
            else -> null
        }
    }

    private fun extractValue(text: String): String {
        val colonSplit = text.split(':', limit = 2)
        if (colonSplit.size == 2) {
            return colonSplit[1].trim()
        }

        val dashSplit = text.split('-', limit = 2)
        if (dashSplit.size == 2) {
            return dashSplit[1].trim()
        }

        return text.substringAfterLast(' ').trim()
    }
}

