package com.proapps.voiceremind

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderCategoryEmojiTest {

    @Test
    fun apply_shopKeyword_addsCartEmoji() {
        val title = ReminderCategoryEmoji.apply("Магазин купить молоко")
        assertEquals("🛒 Магазин купить молоко", title)
    }

    @Test
    fun apply_saunaKeyword_addsSaunaEmoji() {
        val title = ReminderCategoryEmoji.apply("Сауна в 18:00")
        assertEquals("🧖‍♂️ Сауна в 18:00", title)
    }

    @Test
    fun apply_workKeyword_addsBriefcaseEmoji() {
        val title = ReminderCategoryEmoji.apply("Работа: созвон")
        assertEquals("💼 Работа: созвон", title)
    }

    @Test
    fun apply_whenEmojiAlreadyPresent_keepsSinglePrefix() {
        val title = ReminderCategoryEmoji.apply("🛒 Магазин")
        assertEquals("🛒 Магазин", title)
    }
}

