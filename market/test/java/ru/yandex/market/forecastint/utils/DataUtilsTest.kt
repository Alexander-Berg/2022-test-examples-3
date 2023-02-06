package ru.yandex.market.forecastint.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.yandex.market.forecastint.utils.DataUtils.toEndOfInterval
import ru.yandex.mj.generated.server.model.TimeIntervalType.*
import java.time.LocalDate.of

class DataUtilsTest {

    @Test
    fun testToEndOfInterval() {
        assertEquals(of(2021, 12, 13),
            toEndOfInterval(of(2021, 12, 13), DAY))

        assertEquals(of(2021, 12, 19),
            toEndOfInterval(of(2021, 12, 13), WEEK))

        assertEquals(of(2021, 12, 19),
            toEndOfInterval(of(2021, 12, 19), WEEK))

        assertEquals(of(2021, 12, 19),
            toEndOfInterval(of(2021, 12, 15), WEEK))

        assertEquals(of(2021, 12, 31),
            toEndOfInterval(of(2021, 12, 15), MONTH))

        assertEquals(of(2021, 12, 31),
            toEndOfInterval(of(2021, 12, 1), MONTH))

        assertEquals(of(2021, 12, 31),
            toEndOfInterval(of(2021, 12, 31), MONTH))

        assertEquals(of(2021, 12, 31),
            toEndOfInterval(of(2021, 12, 31), YEAR))

        assertEquals(of(2021, 12, 31),
            toEndOfInterval(of(2021, 12, 12), YEAR))

        assertEquals(of(2021, 12, 31),
            toEndOfInterval(of(2021, 5, 12), YEAR))

        assertEquals(of(2021, 12, 31),
            toEndOfInterval(of(2021, 1, 1), YEAR))
    }

}
