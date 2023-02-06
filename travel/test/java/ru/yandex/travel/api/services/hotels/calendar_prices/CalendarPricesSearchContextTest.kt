package ru.yandex.travel.api.services.hotels.calendar_prices

import org.junit.jupiter.api.Test
import ru.yandex.travel.api.proto.hotels_portal.TCalendarPricesSearchContext

class CalendarPricesSearchContextTest {
    @Test
    fun `TestSerialize-Deserialize`() {
        val cpsc =
            CalendarPricesSearchContext(TCalendarPricesSearchContext.newBuilder().apply { pollingIteration = 123 })
        val stringContext = cpsc.serialize()
        val cpscRecovered = CalendarPricesSearchContext.deserialize(stringContext)
        assert(cpsc == cpscRecovered)
    }
}
