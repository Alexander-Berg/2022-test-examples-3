package ru.yandex.market.vendors.analytics.core.service.sales.hiding;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.vendors.analytics.core.model.common.StartEndDate;
import ru.yandex.market.vendors.analytics.core.service.sales.common.DBDatesInterval;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link HiddenDatesUtils}.
 *
 * @author ogonek
 */
public class HiddenDatesUtilsTest {

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("getHiddenDatesArguments")
    @DisplayName("Раскрытие списка скрытых дат")
    void getHiddenDates(
            String startDate, String endDate, TimeDetailing timeDetailing, List<StartEndDate> hiddenIntervals,
            Set<String> expected
    ) {
        var datesInterval = new DBDatesInterval(new StartEndDate(startDate, endDate), timeDetailing);
        assertEquals(expected, HiddenDatesUtils.getHiddenDates(datesInterval, hiddenIntervals));
    }

    private static Stream<Arguments> getHiddenDatesArguments() {
        return Stream.of(
                Arguments.of(
                        "2019-01-01", "2019-03-31", TimeDetailing.MONTH,
                        List.of(new StartEndDate("2019-01-01", "2019-02-29")),
                        Set.of("2019-01-01", "2019-02-01")
                ),
                Arguments.of(
                        "2018-12-30", "2019-01-03", TimeDetailing.DAY,
                        List.of(new StartEndDate("2019-01-01", "2019-01-31")),
                        Set.of("2019-01-01", "2019-01-02", "2019-01-03")
                ),
                Arguments.of(
                        "2018-12-31", "2019-04-01", TimeDetailing.WEEK,
                        List.of(
                                new StartEndDate("2018-01-01", "2018-12-31"),
                                new StartEndDate("2019-02-01", "2019-02-28"),
                                new StartEndDate("2019-04-01", "2019-06-30")
                        ),
                        Set.of("2018-12-31", "2019-02-04", "2019-02-11", "2019-02-18", "2019-02-25", "2019-04-01")
                )
        );
    }


    @ParameterizedTest
    @MethodSource("convertDatesArguments")
    @DisplayName("Конвертация скрытых дат в схлопнутые интервалы")
    void convertDates(List<String> dates, TimeDetailing timeDetailing, List<StartEndDate> expected) {
        assertEquals(expected, HiddenDatesUtils.convertDates(dates, timeDetailing));
    }

    private static Stream<Arguments> convertDatesArguments() {
        return Stream.of(
                Arguments.of(
                        List.of("2019-01-01", "2019-01-15", "2019-02-10", "2019-02-16"),
                        TimeDetailing.MONTH,
                        List.of(new StartEndDate("2019-01-01", "2019-02-28"))
                ),
                Arguments.of(
                        List.of("2019-01-01", "2019-02-01", "2019-03-01", "2019-05-01"),
                        TimeDetailing.MONTH,
                        List.of(
                                new StartEndDate("2019-01-01", "2019-03-31"),
                                new StartEndDate("2019-05-01", "2019-05-31")
                        )
                ),
                Arguments.of(
                        List.of("2019-02-01", "2019-03-01"),
                        TimeDetailing.MONTH,
                        List.of(new StartEndDate("2019-02-01", "2019-03-31"))
                ),
                Arguments.of(
                        List.of("2019-01-01", "2019-03-01", "2019-05-01"),
                        TimeDetailing.MONTH,
                        List.of(
                                new StartEndDate("2019-01-01", "2019-01-31"),
                                new StartEndDate("2019-03-01", "2019-03-31"),
                                new StartEndDate("2019-05-01", "2019-05-31")
                        )
                ),
                Arguments.of(
                        List.of("2019-01-01", "2019-03-01", "2019-05-01"),
                        TimeDetailing.QUARTER,
                        List.of(new StartEndDate("2019-01-01", "2019-06-30"))
                )
        );
    }
}
