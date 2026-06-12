package com.proapps.voiceremind

import org.junit.Assert.assertEquals
import org.junit.Test

class WordsToNumberTest {

    @Test
    fun testRussianDigits() {
        assertEquals(600, WordsToNumber.parseNumber("600"))
        assertEquals(123, WordsToNumber.parseNumber("сто двадцать три"))
        assertEquals(5, WordsToNumber.parseNumber("пять"))
    }

    @Test
    fun testEnglishWords() {
        assertEquals(123, WordsToNumber.parseNumber("one hundred twenty three"))
        assertEquals(45, WordsToNumber.parseNumber("forty five"))
        assertEquals(1000, WordsToNumber.parseNumber("one thousand"))
    }

    @Test
    fun testFinnishBasics() {
        assertEquals(600, WordsToNumber.parseNumber("kuusi sata"))
        assertEquals(600, WordsToNumber.parseNumber("kuusisataa"))
        assertEquals(21, WordsToNumber.parseNumber("kaksikymmentayksi"))
    }

    @Test
    fun testAlphanumericRoute() {
        // parseNumber should prefer digits; for alphanumeric, MainActivity regex picks it up
        assertEquals(600, WordsToNumber.parseNumber("600A") )
    }
}

