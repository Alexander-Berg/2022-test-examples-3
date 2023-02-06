package ru.yandex.market.vendors.analytics.core.service.strategies;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.vendors.analytics.core.model.common.StartEndDate;
import ru.yandex.market.vendors.analytics.core.service.sales.common.DBDatesInterval;

/**
 * Тесты для {@link DBDatesInterval#getHidingDates()}.
 *
 * @author ogonek
 */
public class GetDatesBetweenTest {

    @ParameterizedTest
    @MethodSource("getDatesBetweenTestArguments")
    @DisplayName("Возвращается лист *базных* дат в интервале")
    void getDatesBetween(List<String> expected, TimeDetailing timeDetailing, String startDate, String endDate) {
        DBDatesInterval interval = new DBDatesInterval(new StartEndDate(startDate, endDate), timeDetailing);
        List<String> dates = interval.getHidingDates();
        Assertions.assertEquals(expected, dates);
    }

    /**
     * Параметры для тестов получения "базных" дат в интервале.
     */
    private static Stream<Arguments> getDatesBetweenTestArguments() {
        return Stream.of(
                Arguments.of(List.of("2017-01-01"), TimeDetailing.DAY,
                        "2017-01-01", "2017-01-01"),
                Arguments.of(List.of("2017-01-01"), TimeDetailing.MONTH,
                        "2017-01-01", "2017-01-01"),
                Arguments.of(List.of("2017-01-01"), TimeDetailing.QUARTER,
                        "2017-01-01", "2017-01-01"),
                Arguments.of(List.of("2017-01-01"), TimeDetailing.YEAR,
                        "2017-01-01", "2017-01-01"),

                Arguments.of(List.of("2017-01-28", "2017-01-29", "2017-01-30", "2017-01-31", "2017-02-01"),
                        TimeDetailing.DAY, "2017-01-28", "2017-02-01"),
                Arguments.of(List.of("2017-01-01", "2017-02-01"),
                        TimeDetailing.MONTH, "2017-01-03", "2017-02-01"),
                Arguments.of(List.of("2017-01-01", "2017-04-01"),
                        TimeDetailing.QUARTER, "2017-01-03", "2017-04-01"),
                Arguments.of(List.of("2017-01-01", "2018-01-01"),
                        TimeDetailing.YEAR, "2017-01-03", "2018-01-01"),

                Arguments.of(List.of("2017-01-30", "2017-01-31", "2017-02-01", "2017-02-02"),
                        TimeDetailing.DAY, "2017-01-30", "2017-02-02"),
                Arguments.of(List.of("2017-01-01", "2017-02-01", "2017-03-01"),
                        TimeDetailing.MONTH, "2017-01-03", "2017-03-05"),
                Arguments.of(List.of("2017-01-01", "2017-04-01", "2017-07-01"),
                        TimeDetailing.QUARTER, "2017-01-03", "2017-07-05"),
                Arguments.of(List.of("2017-01-01", "2018-01-01", "2019-01-01"),
                        TimeDetailing.YEAR, "2017-01-03", "2019-11-05")
        );
    }
}
