package ru.yandex.market.logistics.mqm.utils

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

internal class MqmUtilsKtTest {
    @Test
    fun shouldReturnTheSameDayWithAllTheDaysString() {
        assertSoftly {
            TIME_1ST_WEEK_SUNDAY_7.adjustToDayOfWeek("1,2,3,4,5,6,7") shouldBe TIME_1ST_WEEK_SUNDAY_7
            TIME_1ST_WEEK_MONDAY_1.adjustToDayOfWeek("1,2,3,4,5,6,7") shouldBe TIME_1ST_WEEK_MONDAY_1
            TIME_2ND_WEEK_TUESDAY_9.adjustToDayOfWeek("1,2,3,4,5,6,7") shouldBe TIME_2ND_WEEK_TUESDAY_9
        }
    }

    @Test
    fun shouldTreatEverydayEmptyAndWrongStringAsAllTheDays() {
        assertSoftly {
            TIME_1ST_WEEK_SUNDAY_7.adjustToDayOfWeek("everyday") shouldBe TIME_1ST_WEEK_SUNDAY_7
            TIME_1ST_WEEK_MONDAY_1.adjustToDayOfWeek("") shouldBe TIME_1ST_WEEK_MONDAY_1
            TIME_2ND_WEEK_TUESDAY_9.adjustToDayOfWeek("не пойми что") shouldBe TIME_2ND_WEEK_TUESDAY_9
        }
    }

    @Test
    fun shouldAdjustToTheNextDay() {
        assertSoftly {
            TIME_1ST_WEEK_MONDAY_1.adjustToDayOfWeek("2,3,4,5,6,7") shouldBe TIME_1ST_WEEK_TUESDAY_2
            TIME_1ST_WEEK_THURSDAY_4.adjustToDayOfWeek("2,3 , 5,6,7") shouldBe TIME_1ST_WEEK_FRIDAY_5
            TIME_2ND_WEEK_MONDAY_8.adjustToDayOfWeek(" 2, 6") shouldBe TIME_2ND_WEEK_TUESDAY_9
        }
    }

    @Test
    fun shouldAdjustToTheNextWeek() {
        assertSoftly {
            TIME_1ST_WEEK_TUESDAY_2.adjustToDayOfWeek("1") shouldBe TIME_2ND_WEEK_MONDAY_8
            TIME_1ST_WEEK_FRIDAY_5.adjustToDayOfWeek("1") shouldBe TIME_2ND_WEEK_MONDAY_8
            TIME_1ST_WEEK_TUESDAY_2.adjustToDayOfWeek(listOf(DayOfWeek.MONDAY)) shouldBe TIME_2ND_WEEK_MONDAY_8
            TIME_1ST_WEEK_FRIDAY_5.adjustToDayOfWeek(listOf(DayOfWeek.MONDAY)) shouldBe TIME_2ND_WEEK_MONDAY_8
        }
    }

    @Test
    fun shouldAdjustToFirstOddWeekAndDayOfWeek() {
        assertSoftly {
            TIME_1ST_WEEK_SUNDAY_7.adjustToFirstOddWeekAndDayOfWeek("1,2,3,4,5,6,7") shouldBe TIME_1ST_WEEK_SUNDAY_7
            TIME_1ST_WEEK_MONDAY_1.adjustToFirstOddWeekAndDayOfWeek("5,6,7") shouldBe TIME_1ST_WEEK_FRIDAY_5
            TIME_1ST_WEEK_TUESDAY_2.adjustToFirstOddWeekAndDayOfWeek("everyday") shouldBe TIME_1ST_WEEK_TUESDAY_2
            TIME_1ST_WEEK_FRIDAY_5.adjustToFirstOddWeekAndDayOfWeek("3,4") shouldBe TIME_3RD_WEEK_WEDNESDAY_17
            TIME_2ND_WEEK_MONDAY_8.adjustToFirstOddWeekAndDayOfWeek("3,5,6,7") shouldBe TIME_3RD_WEEK_WEDNESDAY_17
            TIME_2ND_WEEK_MONDAY_8.adjustToFirstOddWeekAndDayOfWeek("1,3,5,6,7") shouldBe TIME_3RD_WEEK_MONDAY_15
            TIME_3RD_WEEK_MONDAY_15.adjustToFirstOddWeekAndDayOfWeek("1,3,5,6,7") shouldBe TIME_3RD_WEEK_MONDAY_15
            TIME_3RD_WEEK_TUESDAY_16.adjustToFirstOddWeekAndDayOfWeek("everyday") shouldBe TIME_3RD_WEEK_TUESDAY_16
            TIME_3RD_WEEK_TUESDAY_16.adjustToFirstOddWeekAndDayOfWeek("1,3") shouldBe TIME_3RD_WEEK_WEDNESDAY_17
        }
    }

    @Test
    fun shouldParseNumericalDaysOfWeekString() {
        assertSoftly {
            parseNumericalDaysString("1,2,3,4,5,6,7") shouldContainExactly listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            )
            parseNumericalDaysString("1, 22, 3, skip unparsable, 6") shouldContainExactly listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.SATURDAY,
            )
        }
    }

    @Test
    fun shouldParseEmptyOrUnparsableStringAsEverydayString() {
        assertSoftly {
            parseNumericalDaysString("") shouldContainExactly listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            )
            parseNumericalDaysString("everyday") shouldContainExactly listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            )
        }
    }

    @Test
    fun isOddWeekOfMonthTest() {
        TIME_1ST_WEEK_MONDAY_1.isOddWeekOfMonth() shouldBe true
        TIME_1ST_WEEK_MONDAY_1.plusDays(8).isOddWeekOfMonth() shouldBe false
        TIME_1ST_WEEK_MONDAY_1.plusDays(16).isOddWeekOfMonth() shouldBe true
        TIME_1ST_WEEK_MONDAY_1.plusDays(26).isOddWeekOfMonth() shouldBe false
    }

    companion object {
        val TIME_1ST_WEEK_MONDAY_1: LocalDateTime = LocalDateTime.parse("2022-08-01T11:00:00.00")
        val TIME_1ST_WEEK_TUESDAY_2: LocalDateTime = LocalDateTime.parse("2022-08-02T11:00:00.00")
        val TIME_1ST_WEEK_THURSDAY_4: LocalDateTime = LocalDateTime.parse("2022-08-04T11:00:00.00")
        val TIME_1ST_WEEK_FRIDAY_5: LocalDateTime = LocalDateTime.parse("2022-08-05T11:00:00.00")
        val TIME_1ST_WEEK_SUNDAY_7: LocalDateTime = LocalDateTime.parse("2022-08-07T11:00:00.00")
        val TIME_2ND_WEEK_MONDAY_8: LocalDateTime = LocalDateTime.parse("2022-08-08T11:00:00.00")
        val TIME_2ND_WEEK_TUESDAY_9: LocalDateTime = LocalDateTime.parse("2022-08-09T11:00:00.00")
        val TIME_3RD_WEEK_MONDAY_15: LocalDateTime = LocalDateTime.parse("2022-08-15T11:00:00.00")
        val TIME_3RD_WEEK_TUESDAY_16: LocalDateTime = LocalDateTime.parse("2022-08-16T11:00:00.00")
        val TIME_3RD_WEEK_WEDNESDAY_17: LocalDateTime = LocalDateTime.parse("2022-08-17T11:00:00.00")
    }
}
