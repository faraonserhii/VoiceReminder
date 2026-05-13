package com.proapps.voiceremind

import java.util.Locale

object CityNameNormalizer {

    fun normalize(rawCity: String): String {
        val locale = Locale.getDefault()
        val compact = rawCity.trim().replace(Regex("""\s+"""), " ")
        return compact
            .split(" ")
            .joinToString(" ") { token ->
                token
                    .lowercase(locale)
                    .replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                    }
                }
    }
}

