package ru.yandex.market.mbi.orderservice.api.controller.summary

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.mbi.orderservice.api.service.summary.getDatePeriod
import ru.yandex.market.mbi.orderservice.api.service.summary.getDaysList
import ru.yandex.market.mbi.orderservice.api.service.summary.getPreviousDatePeriod
import ru.yandex.market.mbi.orderservice.api.service.summary.getTimeList
import ru.yandex.market.mbi.orderservice.api.service.summary.getPrevTimePeriod
import ru.yandex.market.mbi.orderservice.api.service.summary.percentChange
import ru.yandex.market.mbi.orderservice.api.service.summary.percentage
import ru.yandex.market.mbi.orderservice.model.SummaryPeriod
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.stream.Stream

/**
 * Тесты для [SummaryOrderUtils]
 */
class SummaryOrderUtilsTest {

    @DisplayName("Тест на получение списка дат из ренджа")
    @Test
    fun `test get list of dates from range`() {
        val expected = listOf(
            "2019-12-28",
            "2019-12-29",
            "2019-12-30",
            "2019-12-31",
            "2020-01-01",
            "2020-01-02",
            "2020-01-03"
        )
        val actual = getDaysList(Pair(LocalDate.parse("2019-12-28"), LocalDate.parse("2020-01-03")))
        assertThat(actual).isEqualTo(expected)
    }

    @DisplayName("Тест на получение списка времён из ренджа")
    @Test
    fun `test get list of times from range`() {
        val expected = listOf(
            OffsetDateTime.parse("2020-01-01T20:00:00+02:00"),
            OffsetDateTime.parse("2020-01-01T21:00:00+02:00"),
            OffsetDateTime.parse("2020-01-01T22:00:00+02:00"),
            OffsetDateTime.parse("2020-01-01T23:00:00+02:00"),
            OffsetDateTime.parse("2020-01-02T00:00:00+02:00"),
            OffsetDateTime.parse("2020-01-02T01:00:00+02:00"),
            OffsetDateTime.parse("2020-01-02T02:00:00+02:00"),
            OffsetDateTime.parse("2020-01-02T03:00:00+02:00"),
            OffsetDateTime.parse("2020-01-02T04:00:00+02:00"),
            OffsetDateTime.parse("2020-01-02T05:00:00+02:00"),
        )
        val actual = getTimeList(
            OffsetDateTime.parse("2020-01-01T20:00:00+02:00"),
            OffsetDateTime.parse("2020-01-02T05:00:00+02:00")
        )
        assertThat(actual).isEqualTo(expected)
    }

    @DisplayName("Тест на создание ренджа из периода")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getRangeSource")
    fun `test range creation from period`(
        date: LocalDate,
        period: SummaryPeriod,
        expectedStart: LocalDate,
        expectedEnd: LocalDate
    ) {
        val expected = Pair(expectedStart, expectedEnd)
        val actual = getDatePeriod(date, period)
        assertThat(actual).isEqualTo(expected)
    }

    @DisplayName("Тест на создание ренджа из предыдущего периода")
    @ParameterizedTest(name = "{1}")
    @MethodSource("getPrevRangeSource")
    fun `test range creation from prev period`(
        date: LocalDate,
        period: SummaryPeriod,
        expectedStart: LocalDate,
        expectedEnd: LocalDate
    ) {
        val expected = Pair(expectedStart, expectedEnd)
        val currentDatePeriod = getDatePeriod(date, period)
        val actualPrevDatePeriod = getPreviousDatePeriod(currentDatePeriod, period)
        assertThat(actualPrevDatePeriod).isEqualTo(expected)
    }

    @DisplayName("Тест на создание ренджа из периода с таймзоной")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getZonedRangeSource")
    fun `test zoned range creation from period`(
        begin: OffsetDateTime,
        end: OffsetDateTime,
        period: SummaryPeriod,
        expectedStart: OffsetDateTime,
        expectedEnd: OffsetDateTime
    ) {
        val expected = Pair(expectedStart, expectedEnd)
        val actual = getPrevTimePeriod(begin, end, period, true)
        assertThat(actual).isEqualTo(expected)
    }

    @DisplayName("Тест на создание процентного соотношения")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getPercentageSource")
    fun `test percentage creation`(
        selected: BigDecimal,
        total: BigDecimal,
        expected: BigDecimal
    ) {
        val actual = percentage(selected, total)
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(expected)
    }

    @DisplayName("Тест на создание диффа процентов")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getDiffSource")
    fun `test diff creation`(
        prev: BigDecimal,
        curr: BigDecimal,
        expected: BigDecimal
    ) {
        val actual = percentChange(prev, curr)
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(expected)
    }

    companion object {

        @JvmStatic
        fun getDiffSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(BigDecimal(0), BigDecimal(0), BigDecimal(0)),
                Arguments.of(BigDecimal(0), BigDecimal(123), BigDecimal(100)),
                Arguments.of(BigDecimal(123), BigDecimal(0), BigDecimal(-100)),
                //строка, тк дабл не точный
                Arguments.of(BigDecimal(350), BigDecimal(50), BigDecimal("-85.7")),
                Arguments.of(BigDecimal(50), BigDecimal(350), BigDecimal(600)),
            )
        }

        @JvmStatic
        fun getPercentageSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(BigDecimal(0), BigDecimal(0), BigDecimal(0)),
                Arguments.of(BigDecimal(0), BigDecimal(11), BigDecimal(0)),
                Arguments.of(BigDecimal(11), BigDecimal(11), BigDecimal(100)),
                Arguments.of(BigDecimal(5), BigDecimal(11), BigDecimal(45.5)),
            )
        }

        @JvmStatic
        fun getZonedRangeSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    OffsetDateTime.parse("2020-01-01T00:00:00+02:00"),
                    OffsetDateTime.parse("2020-01-04T06:00:00+02:00"),
                    SummaryPeriod.MONTH,
                    OffsetDateTime.parse("2019-12-01T00:00:00+02:00"),
                    OffsetDateTime.parse("2019-12-04T06:00:00+02:00")
                ),
                Arguments.of(
                    OffsetDateTime.parse("2020-01-01T00:00:00+02:00"),
                    OffsetDateTime.parse("2020-01-31T00:00:00+02:00"),
                    SummaryPeriod.PREV_MONTH,
                    OffsetDateTime.parse("2019-12-01T00:00:00+02:00"),
                    OffsetDateTime.parse("2019-12-31T23:00:00+02:00")
                ),
                Arguments.of(
                    OffsetDateTime.parse("2020-02-04T00:00:00+02:00"),
                    OffsetDateTime.parse("2020-02-18T00:00:00+02:00"),
                    SummaryPeriod.TWO_WEEKS,
                    OffsetDateTime.parse("2020-01-22T00:00:00+02:00"),
                    OffsetDateTime.parse("2020-02-03T23:00:00+02:00")
                ),
                Arguments.of(
                    OffsetDateTime.parse("2020-02-04T00:00:00+02:00"),
                    OffsetDateTime.parse("2020-02-11T00:00:00+02:00"),
                    SummaryPeriod.WEEK,
                    OffsetDateTime.parse("2020-01-29T00:00:00+02:00"),
                    OffsetDateTime.parse("2020-02-03T23:00:00+02:00")
                )
            )
        }

        @JvmStatic
        fun getRangeSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    LocalDate.parse("2020-02-03"),
                    SummaryPeriod.MONTH,
                    LocalDate.parse("2020-02-01"),
                    LocalDate.parse("2020-02-03")
                ),
                Arguments.of(
                    LocalDate.parse("2020-02-03"),
                    SummaryPeriod.PREV_MONTH,
                    LocalDate.parse("2020-01-01"),
                    LocalDate.parse("2020-01-31")
                ),
                Arguments.of(
                    LocalDate.parse("2020-02-03"),
                    SummaryPeriod.TWO_WEEKS,
                    LocalDate.parse("2020-01-21"),
                    LocalDate.parse("2020-02-03")
                ),
                Arguments.of(
                    LocalDate.parse("2020-02-03"),
                    SummaryPeriod.WEEK,
                    LocalDate.parse("2020-01-28"),
                    LocalDate.parse("2020-02-03")
                )
            )
        }

        @JvmStatic
        fun getPrevRangeSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    LocalDate.parse("2020-02-03"),
                    SummaryPeriod.MONTH,
                    LocalDate.parse("2020-01-01"),
                    LocalDate.parse("2020-01-03")
                ),
                Arguments.of(
                    LocalDate.parse("2020-02-03"),
                    SummaryPeriod.PREV_MONTH,
                    LocalDate.parse("2019-12-01"),
                    LocalDate.parse("2019-12-31")
                ),
                Arguments.of(
                    LocalDate.parse("2020-02-03"),
                    SummaryPeriod.TWO_WEEKS,
                    LocalDate.parse("2020-01-07"),
                    LocalDate.parse("2020-01-20")
                ),
                Arguments.of(
                    LocalDate.parse("2020-02-03"),
                    SummaryPeriod.WEEK,
                    LocalDate.parse("2020-01-21"),
                    LocalDate.parse("2020-01-27")
                )
            )
        }
    }
}
