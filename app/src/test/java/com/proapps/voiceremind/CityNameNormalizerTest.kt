package com.proapps.voiceremind

import org.junit.Assert.assertEquals
import org.junit.Test

class CityNameNormalizerTest {

    @Test
    fun normalize_trimsAndCollapsesSpaces() {
        val normalized = CityNameNormalizer.normalize("   new    york   ")
        assertEquals("New York", normalized)
    }

    @Test
    fun normalize_normalizesMixedCaseInput() {
        val normalized = CityNameNormalizer.normalize("heLSinKi")
        assertEquals("Helsinki", normalized)
    }

    @Test
    fun normalize_handlesMultiWordInput() {
        val normalized = CityNameNormalizer.normalize("saint petersburg")
        assertEquals("Saint Petersburg", normalized)
    }

    @Test
    fun normalize_handlesMultiWordMixedCaseInput() {
        val normalized = CityNameNormalizer.normalize("  sAiNt    pETeRsBuRg ")
        assertEquals("Saint Petersburg", normalized)
    }
}

