package ru.yandex.market.logistics.calendaring.util

import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.calendaring.base.SoftAssertionSupport
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeIntervalTest : SoftAssertionSupport() {

    @Test
    fun dateTimeIntervalFromLocalTimeWhenMidnightTest() {
        val interval = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(20, 0),
            LocalTime.of(0, 0), ZoneId.of("UTC")
        )

        softly.assertThat(interval.from).isEqualTo(ZonedDateTime.of(2021, 5, 17, 20, 0, 0, 0, ZoneId.of("UTC")))
        softly.assertThat(interval.to).isEqualTo(ZonedDateTime.of(2021, 5, 18, 0, 0, 0, 0, ZoneId.of("UTC")))
    }

    @Test
    fun dateTimeIntervalFromLocalTimeWhenTransitionDayTest() {
        val interval = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(20, 0),
            LocalTime.of(4, 0), ZoneId.of("UTC")
        )

        softly.assertThat(interval.from).isEqualTo(ZonedDateTime.of(2021, 5, 17, 20, 0, 0, 0, ZoneId.of("UTC")))
        softly.assertThat(interval.to).isEqualTo(ZonedDateTime.of(2021, 5, 18, 4, 0, 0, 0, ZoneId.of("UTC")))
    }

    @Test
    fun sameIntervalIntersects() {
        val interval1 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0), ZoneId.of("UTC")
        )

        val interval2 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0), ZoneId.of("UTC")
        )

        softly.assertThat(interval1.intersects(interval2)).isTrue
    }

    @Test
    fun intersectsCaseRightSideTest() {
        val interval1 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0), ZoneId.of("UTC")
        )

        val interval2 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(10, 0),
            LocalTime.of(19, 0), ZoneId.of("UTC")
        )

        softly.assertThat(interval1.intersects(interval2)).isTrue
    }

    @Test
    fun intersectsCaseFromLeftSideTest() {
        val interval1 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0), ZoneId.of("UTC")
        )

        val interval2 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(8, 0),
            LocalTime.of(17, 0), ZoneId.of("UTC")
        )

        softly.assertThat(interval1.intersects(interval2)).isTrue
    }

    @Test
    fun intersectsCaseIncludesTest() {
        val interval1 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0), ZoneId.of("UTC")
        )

        val interval2 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(7, 0),
            LocalTime.of(19, 0), ZoneId.of("UTC")
        )

        softly.assertThat(interval1.intersects(interval2)).isTrue
    }

    @Test
    fun intersectsCase2IncludesTest() {
        val interval1 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0), ZoneId.of("UTC")
        )

        val interval2 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(7, 0),
            LocalTime.of(19, 0), ZoneId.of("UTC")
        )

        softly.assertThat(interval2.intersects(interval1)).isTrue
    }


    @Test
    fun notIntersectsTest() {
        val interval1 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(11, 0), ZoneId.of("UTC")
        )

        val interval2 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0), ZoneId.of("UTC")
        )

        softly.assertThat(interval1.intersects(interval2)).isFalse
    }


    @Test
    fun sameIntervalIncludesTest() {
        val interval1 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0), ZoneId.of("UTC")
        )

        val interval2 = DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(18, 0), ZoneId.of("UTC")
        )
        softly.assertThat(interval1.includes(interval2)).isTrue
    }

    @Test
    fun spitIntervalByMinutes() {

        val interval = DateTimeInterval(
            ZonedDateTime.of(2021, 5, 17, 9, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 5, 18, 11, 0, 0, 0, ZoneId.of("UTC")),
        )

        val spitIntervalByMinutes = interval.spitIntervalByMinutes(30)

        softly.assertThat(spitIntervalByMinutes.size).isEqualTo(52)
        softly.assertThat(spitIntervalByMinutes.first()).isEqualTo(
            DateTimeInterval(
                ZonedDateTime.of(2021, 5, 17, 9, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2021, 5, 17, 9, 30, 0, 0, ZoneId.of("UTC")),
            )
        )

        softly.assertThat(spitIntervalByMinutes.last()).isEqualTo(
            DateTimeInterval(
                ZonedDateTime.of(2021, 5, 18, 10, 30, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2021, 5, 18, 11, 0, 0, 0, ZoneId.of("UTC")),
            )
        )
    }
}
