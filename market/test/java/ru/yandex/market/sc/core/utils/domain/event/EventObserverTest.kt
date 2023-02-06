package ru.yandex.market.sc.core.utils.domain.event

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.market.sc.core.utils.data.event.OnceEvent

class EventObserverTest {
    @Test
    fun `calls handler only once for same event`() {
        var times = 0

        val handler: (Boolean) -> Unit = { ++times }
        val eventObserver = EventObserver(handler)
        val event = OnceEvent(true)

        eventObserver.onChanged(event)
        assertEquals(times, 1)

        eventObserver.onChanged(event)
        assertEquals(times, 1)
    }
}