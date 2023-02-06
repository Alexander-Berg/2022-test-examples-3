package ru.yandex.market.mapi.core.util.date

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.04.2022
 */
class MapiDateUtilsTest : AbstractNonSpringTest() {
    @Test
    fun testDateFormatsSimple() {
        val day = MapiDateBuilder.parseDateYMD("2022-04-15")

        assertEquals("15", MapiDateUtils.toDayOfMonth(day))
        assertEquals("15 апреля", MapiDateUtils.toDayWithMonth(day))

        assertEquals(2, MapiDateUtils.getTodayDayOfWeek())
        assertEquals(4, MapiDateUtils.getMonth(day))
        assertEquals(5, MapiDateUtils.getDayOfWeek(day))
    }

    @Test
    fun testDateWithYear() {
        val daySameYear = MapiDateBuilder.parseDateYMD("2022-04-15")
        val dayOtherYear = MapiDateBuilder.parseDateYMD("2019-05-01")

        assertEquals("15 апреля", MapiDateUtils.toDateWithMonthYear(daySameYear))
        assertEquals("1 мая, 2019", MapiDateUtils.toDateWithMonthYear(dayOtherYear))
    }

    @Test
    fun testDetDayOfWeek() {
        for (i in 1..7) {
            // staring with monday 2022-06-20
            val day = MapiDateBuilder.parseDateYMD("2022-06-20").plusDays(i - 1L)
            assertEquals(i, MapiDateUtils.getDayOfWeek(day))
        }
    }

    @Test
    fun testFormatOutletWorkingDayOfWeek() {
        assertEquals("в понедельник", MapiDateUtils.getAtDayOfWeekName(1))
        assertEquals("во вторник", MapiDateUtils.getAtDayOfWeekName(2))
        assertEquals("в среду", MapiDateUtils.getAtDayOfWeekName(3))
        assertEquals("в четверг", MapiDateUtils.getAtDayOfWeekName(4))
        assertEquals("в пятницу", MapiDateUtils.getAtDayOfWeekName(5))
        assertEquals("в субботу", MapiDateUtils.getAtDayOfWeekName(6))
        assertEquals("в воскресенье", MapiDateUtils.getAtDayOfWeekName(7))
    }

    @Test
    fun testFormatOutletStorageLimitDate() {
        val startDay = MapiDateBuilder.parseDateYMD("2022-04-04")
        assertEquals("понедельника", MapiDateUtils.getUntilDayOfWeekName(startDay))
        assertEquals("вторника", MapiDateUtils.getUntilDayOfWeekName(startDay.plusDays(1)))
        assertEquals("среды", MapiDateUtils.getUntilDayOfWeekName(startDay.plusDays(2)))
        assertEquals("четверга", MapiDateUtils.getUntilDayOfWeekName(startDay.plusDays(3)))
        assertEquals("пятницы", MapiDateUtils.getUntilDayOfWeekName(startDay.plusDays(4)))
        assertEquals("субботы", MapiDateUtils.getUntilDayOfWeekName(startDay.plusDays(5)))
        assertEquals("воскресенья", MapiDateUtils.getUntilDayOfWeekName(startDay.plusDays(6)))
    }

    @Test
    fun testDayCountOfWeek() {
        assertEquals("в понедельник", MapiDateUtils.toDayOfWeekSmart(-1))
        assertEquals("сегодня", MapiDateUtils.toDayOfWeekSmart(0))
        assertEquals("завтра", MapiDateUtils.toDayOfWeekSmart(1))
        assertEquals("в четверг", MapiDateUtils.toDayOfWeekSmart(2))
        assertEquals("в пятницу", MapiDateUtils.toDayOfWeekSmart(3))
        assertEquals("в субботу", MapiDateUtils.toDayOfWeekSmart(4))
        assertEquals("в воскресенье", MapiDateUtils.toDayOfWeekSmart(5))

        // with day printer
        assertEquals(
            "сегодня", MapiDateUtils.toDayOfWeekSmart(0,
                dayPrinter = { x -> MapiDateUtils.toDayWithMonth(x) })
        )
        assertEquals(
            "завтра, 16 марта", MapiDateUtils.toDayOfWeekSmart(1,
                dayPrinter = { x -> MapiDateUtils.toDayWithMonth(x) })
        )
        assertEquals(
            "в четверг, 17 марта", MapiDateUtils.toDayOfWeekSmart(2,
                dayPrinter = { x -> MapiDateUtils.toDayWithMonth(x) })
        )

        // with day of week printer
        assertEquals(
            "сегодня", MapiDateUtils.toDayOfWeekSmart(0,
                dayOfWeekPrinter = { x -> MapiDateUtils.getUntilDayOfWeekName(x) },
                dayPrinter = { x -> MapiDateUtils.toDayWithMonth(x) })
        )
        assertEquals(
            "завтра, 16 марта", MapiDateUtils.toDayOfWeekSmart(1,
                dayOfWeekPrinter = { x -> MapiDateUtils.getUntilDayOfWeekName(x) },
                dayPrinter = { x -> MapiDateUtils.toDayWithMonth(x) })
        )
        assertEquals(
            "четверга, 17 марта", MapiDateUtils.toDayOfWeekSmart(2,
                dayOfWeekPrinter = { x -> MapiDateUtils.getUntilDayOfWeekName(x) },
                dayPrinter = { x -> MapiDateUtils.toDayWithMonth(x) })
        )
    }

    @Test
    fun testDayCount() {
        assertEquals("через -1 день", MapiDateUtils.toDayOrCount(-1))
        assertEquals("сегодня", MapiDateUtils.toDayOrCount(0))
        assertEquals("завтра", MapiDateUtils.toDayOrCount(1))
        assertEquals("через 2 дня", MapiDateUtils.toDayOrCount(2))
        assertEquals("через 3 дня", MapiDateUtils.toDayOrCount(3))
        assertEquals("через 4 дня", MapiDateUtils.toDayOrCount(4))
        assertEquals("через 5 дней", MapiDateUtils.toDayOrCount(5))
    }

    @Test
    fun testDateRange() {
        val startDay = MapiDateBuilder.parseDateYMD("2022-04-04")
        val endDaySameMonth = MapiDateBuilder.parseDateYMD("2022-04-15")
        val endDayOther = MapiDateBuilder.parseDateYMD("2022-05-01")

        assertEquals("4 — 15 апреля", MapiDateUtils.toDateRange(startDay, endDaySameMonth))
        assertEquals("4 апреля — 1 мая", MapiDateUtils.toDateRange(startDay, endDayOther))
    }

    @Test
    fun testGetShortDayOfWeek() {
        assertEquals("пн", MapiDateUtils.getShortDayOfWeek(1))
        assertEquals("вт", MapiDateUtils.getShortDayOfWeek(2))
        assertEquals("ср", MapiDateUtils.getShortDayOfWeek(3))
        assertEquals("чт", MapiDateUtils.getShortDayOfWeek(4))
        assertEquals("пт", MapiDateUtils.getShortDayOfWeek(5))
        assertEquals("сб", MapiDateUtils.getShortDayOfWeek(6))
        assertEquals("вс", MapiDateUtils.getShortDayOfWeek(7))
    }

    @Test
    fun testSimpleDateTimeFormat() {
        val date = MapiDateBuilder.parseDateDMY("03-04-2022")
        val time = MapiDateBuilder.parseTimeHM("16:00")

        assertEquals("03-04-2022 16:00:00", MapiDateUtils.toSimpleDateTimeFormat(date, time))
    }
}
