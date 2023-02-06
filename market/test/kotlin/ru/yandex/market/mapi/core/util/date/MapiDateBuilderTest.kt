package ru.yandex.market.mapi.core.util.date

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.BUILD_INSTANT_FUN
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.currentDate
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.currentTime
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.currentTimeAtShift
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.parseDateDMY
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.parseDateEpochMs
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.parseDateTimeIsoWithTZ
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.parseDateYMD
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.parseTimeHM
import ru.yandex.market.mapi.core.util.date.MapiDateBuilder.toMorning
import ru.yandex.market.mapi.core.util.mockTimezone
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFails

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.03.2022
 */
class MapiDateBuilderTest : AbstractNonSpringTest() {

    @Test
    fun testPromoDateParsing() {
        // ensure that parsing succeeds for valid pattern "yyyy-MM-dd"
        // ensure that returned date is at the start of this date, 00:00
        assertEquals(
            expected = Instant.parse("2022-02-18T00:00:00Z").atOffset(ZoneOffset.UTC).toLocalDate(),
            actual = parseDateYMD("2022-02-18")
        )

        // ensure that parsing fails for invalid date pattern "dd-MM-yyyy"
        assertFails { parseDateYMD("18-02-2022") }

        // ensure that parsing fails for invalid date and time pattern "yyyy-MM-dd'T'HH:mm:ss'Z'"
        assertFails { parseDateYMD("2016-12-31T10:00:00Z") }
    }

    @Test
    fun testCurrentTime() {
        assertEquals("2022-03-15T10:53:30Z", BUILD_INSTANT_FUN.invoke().toString())

        assertEquals("2022-03-15T13:53:30+03:00", currentTime().toString())
        assertEquals("2022-03-15", currentDate().toString())
    }

    @Test
    fun testCurrentTime2() {
        mockTimezone("+04:30")
        assertEquals("2022-03-15T15:23:30+04:30", currentTime().toString())
    }

    @Test
    fun testCurrentTime3() {
        mockTimezone("+14:30")
        assertEquals("2022-03-16T01:23:30+14:30", currentTime().toString())
        assertEquals("2022-03-16", currentDate().toString())
    }

    @Test
    fun testCurrentTimeShift() {
        assertEquals("2022-03-16T13:53:30+03:00", currentTimeAtShift(1, ChronoUnit.DAYS).toString())
    }

    @Test
    fun testParseEpoch() {
        assertEquals("2022-03-15T13:53:30+03:00", parseDateEpochMs(1647341610000L).toString())
        assertEquals("2022-07-01T18:26:48.590+03:00", parseDateEpochMs(1656689208590L).toString())
    }

    @Test
    fun testParseDateYMD() {
        assertEquals("2022-05-12", parseDateYMD("2022-05-12").toString())
    }

    @Test
    fun testParseDateDMY() {
        assertEquals("2022-02-18", parseDateDMY("18-02-2022").toString())
    }

    @Test
    fun testParseDateIso() {
        assertEquals("2022-07-06T16:00:17+03:00", parseDateTimeIsoWithTZ("2022-07-06T13:00:17Z").toString())
    }

    @Test
    fun testToMorning() {
        assertEquals("2022-05-12T08:00+03:00", toMorning(parseDateYMD("2022-05-12")).toString())

        mockTimezone("+12:30")
        assertEquals("2022-05-12T08:00+12:30", toMorning(parseDateYMD("2022-05-12")).toString())
    }

    @Test
    fun testParseTimeHM() {
        assertEquals("13:44", parseTimeHM("13:44").toString())
        assertEquals("09:00", parseTimeHM("09:00").toString())
    }
}
