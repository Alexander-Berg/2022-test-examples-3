package ru.yandex.market.vendors.analytics.core.service.strategies;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Тесты для {@link QuarterUtils}.
 *
 * @author ogonek
 */
public class QuarterOfYearTest {
    @ParameterizedTest
    @MethodSource("dateToQuarterDateTestArguments")
    @DisplayName("Дата конвертируется из yyyy-MM(-dd) в yyyy-QQ")
    void dateToQuarterDateTest(String expected, String date) {
        Assertions.assertEquals(expected, QuarterUtils.dateToQuarterFirstDate(date));
    }
    @ParameterizedTest
    @MethodSource("getEndDateTestArguments")
    @DisplayName("Верно находится конечная дата")
    void getEndDateTest(String expected, String quarter) {
        Assertions.assertEquals(expected, QuarterUtils.dateToQuarterLastDate(quarter));
    }
    /**
     * Параметры для тестов конвертации из yyyy-MM(-dd) в yyyy-QQ.
     */
    private static Stream<Arguments> dateToQuarterDateTestArguments() {
        return Stream.of(
                Arguments.of("2017-01-01", "2017-01-01"),
                Arguments.of("2017-01-01", "2017-01"),
                Arguments.of("2017-01-01", "2017-02-28"),
                Arguments.of("2017-04-01", "2017-04-01"),
                Arguments.of("2016-01-01", "2016-02-29"),
                Arguments.of("2018-07-01", "2018-09-01")
        );
    }
    /**
     * Параметры для тестов нахождения конечной даты.
     */
    private static Stream<Arguments> getEndDateTestArguments() {
        return Stream.of(
                Arguments.of("2017-03-31", "2017-01-01"),
                Arguments.of("2017-06-30", "2017-04-01"),
                Arguments.of("2017-09-30", "2017-07-01"),
                Arguments.of("2017-12-31", "2017-10-01")
        );
    }
}