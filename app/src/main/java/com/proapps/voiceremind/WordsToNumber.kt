package com.proapps.voiceremind

// Простая утилита для преобразования русских словесных числительных в Int (ограничение до 9999).
object WordsToNumber {
    private val units = mapOf(
        "ноль" to 0, "нуль" to 0,
        "один" to 1, "одна" to 1, "одно" to 1,
        "два" to 2, "две" to 2,
        "три" to 3, "четыре" to 4, "пять" to 5,
        "шесть" to 6, "семь" to 7, "восемь" to 8, "девять" to 9
    )

    private val teens = mapOf(
        "десять" to 10, "одиннадцать" to 11, "двенадцать" to 12, "тринадцать" to 13,
        "четырнадцать" to 14, "пятнадцать" to 15, "шестнадцать" to 16, "семнадцать" to 17,
        "восемнадцать" to 18, "девятнадцать" to 19
    )

    private val tens = mapOf(
        "двадцать" to 20, "тридцать" to 30, "сорок" to 40, "пятьдесят" to 50,
        "шестьдесят" to 60, "семьдесят" to 70, "восемьдесят" to 80, "девяносто" to 90
    )

    private val hundreds = mapOf(
        "сто" to 100, "двести" to 200, "триста" to 300, "четыреста" to 400,
        "пятьсот" to 500, "шестьсот" to 600, "семьсот" to 700, "восемьсот" to 800, "девятьсот" to 900
    )

    private val thousands = mapOf(
        "тысяча" to 1000, "тысячи" to 1000, "тысяч" to 1000
    )

    fun parseNumber(text: String): Int? {
        val cleaned = text
            .lowercase()
            .replace('-', ' ')
            .replace(',', ' ')
            // keep letters from any script and digits
            .replace(Regex("[^\\p{L}0-9 ]+"), " ")
            .trim()
        // If contains digits, prefer digits
        Regex("\\d{1,4}").find(cleaned)?.let { return it.value.toIntOrNull() }

        if (cleaned.isBlank()) return null

        val tokens = cleaned.split(Regex("\\s+"))
        var total = 0
        var current = 0
        var seenAny = false

        for (t0 in tokens) {
            val t = t0.trim()
            if (t.isEmpty()) continue
            when {
                thousands.containsKey(t) -> {
                    val mult = if (current == 0) 1 else current
                    total += mult * 1000
                    current = 0
                    seenAny = true
                }

                hundreds.containsKey(t) -> {
                    current += hundreds[t] ?: 0
                    seenAny = true
                }

                tens.containsKey(t) -> {
                    current += tens[t] ?: 0
                    seenAny = true
                }

                teens.containsKey(t) -> {
                    current += teens[t] ?: 0
                    seenAny = true
                }

                units.containsKey(t) -> {
                    current += units[t] ?: 0
                    seenAny = true
                }

                // no-op for other covered tokens

                else -> {
                    // unknown token -> stop parsing further
                    // e.g. words like "маршрут" or filler words
                    // continue scanning but don't reset
                }
            }
        }

        // If Russian parsing succeeded, return
        val result = total + current
        if (seenAny && result in 0..9999) return result

        // Try Finnish parsing (basic)
        val fiClean = cleaned.replace('ä', 'a').replace('ö', 'o').replace('å', 'a').replace('š', 's').replace('ž', 'z')
        val fiTokens = fiClean.split(Regex("\\s+"))
        var fiTotal = 0
        var fiCurrent = 0
        var fiSeen = false

        val fiUnits = mapOf(
            "nolla" to 0,
            "yksi" to 1, "kaksi" to 2, "kolme" to 3, "nelja" to 4, "viisi" to 5,
            "kuusi" to 6, "seitseman" to 7, "kahdeksan" to 8, "yhdeksan" to 9
        )
        val fiTeens = mapOf(
            "kymmenen" to 10, "yksitoista" to 11, "kaksitoista" to 12
        )
        val fiTens = mapOf(
            "kaksikymmenta" to 20, "kolmekymmenta" to 30, "neljakymmenta" to 40,
            "viisikymmenta" to 50, "kuusikymmenta" to 60
        )
        // 'sata' family -> 100
        val fiHundredWords = setOf("sata", "sataa")
        val fiThousandWords = setOf("tuhat", "tuhatta")

        for (tk in fiTokens) {
            val t2 = tk.trim()
            if (t2.isEmpty()) continue

            when {
                fiThousandWords.contains(t2) -> {
                    val mult = if (fiCurrent == 0) 1 else fiCurrent
                    fiTotal += mult * 1000
                    fiCurrent = 0
                    fiSeen = true
                }

                fiHundredWords.contains(t2) -> {
                    val mult = if (fiCurrent == 0) 1 else fiCurrent
                    fiCurrent = mult * 100
                    fiSeen = true
                }

                fiTens.containsKey(t2) -> {
                    fiCurrent += fiTens[t2] ?: 0
                    fiSeen = true
                }

                fiTeens.containsKey(t2) -> {
                    fiCurrent += fiTeens[t2] ?: 0
                    fiSeen = true
                }

                fiUnits.containsKey(t2) -> {
                    fiCurrent += fiUnits[t2] ?: 0
                    fiSeen = true
                }

                // compound like 'kuusisataa' or 'kaksituhatta'
                t2.endsWith("sataa") || t2.endsWith("sata") -> {
                    val prefix = if (t2.endsWith("sataa")) t2.removeSuffix("sataa") else t2.removeSuffix("sata")
                    val pval = fiUnits[prefix]
                    if (pval != null) {
                        fiCurrent += pval * 100
                        fiSeen = true
                    }
                }

                t2.endsWith("tuhat") || t2.endsWith("tuhatta") -> {
                    val prefix = if (t2.endsWith("tuhatta")) t2.removeSuffix("tuhatta") else t2.removeSuffix("tuhat")
                    val pval = fiUnits[prefix]
                    if (pval != null) {
                        fiTotal += pval * 1000
                        fiSeen = true
                    }
                }

                else -> {
                    // ignore unknown tokens
                }
            }
        }

        val fiResult = fiTotal + fiCurrent
        return if (fiSeen && fiResult in 0..9999) fiResult else null
    }
}

