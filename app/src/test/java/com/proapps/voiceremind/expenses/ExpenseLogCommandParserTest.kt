package com.proapps.voiceremind.expenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseLogCommandParserTest {

    @Test
    fun extract_ruLunchExpense_parsesAmountAndCategory() {
        val command = ExpenseLogCommandParser.extract("Запиши: потратил 15 евро на обед")

        assertNotNull(command)
        val parsed = command!!
        assertEquals(15.0, parsed.amountEuro, 0.001)
        assertEquals(ExpenseCategory.RESTAURANT, parsed.category)
    }

    @Test
    fun extract_enRestaurantExpense_parsesAmountAndCategory() {
        val command = ExpenseLogCommandParser.extract("Record: spent 22.5 eur for restaurant")

        assertNotNull(command)
        val parsed = command!!
        assertEquals(22.5, parsed.amountEuro, 0.001)
        assertEquals(ExpenseCategory.RESTAURANT, parsed.category)
    }

    @Test
    fun extract_nonExpensePhrase_returnsNull() {
        val command = ExpenseLogCommandParser.extract("Напомни про встречу завтра")

        assertNull(command)
    }
}

