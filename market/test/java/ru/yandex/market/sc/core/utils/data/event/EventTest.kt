package ru.yandex.market.sc.core.utils.data.event

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EventTest {
    @Test
    fun `is able to be handled only once`() {
        val event = OnceEvent(true)

        assertNotNull(event.get())
        assertNull(event.get())
        assertNull(event.get())
    }
}