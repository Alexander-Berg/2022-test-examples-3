package ru.yandex.market.ff.util.dateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.ff.service.util.dateTime.DateTimeUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsEqual.equalTo;

public class DateTimeUtilsTest {


    @ParameterizedTest
    @MethodSource("params")
    void testRoundUpDateTime(LocalDateTime dateTime, int minutesOffset, LocalDateTime expected) {
        LocalDateTime actual = DateTimeUtils.roundUpDateTime(dateTime, minutesOffset);

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testDaysBetween() {
        LocalDate from = LocalDate.parse("2020-06-20");
        LocalDate to = LocalDate.parse("2020-06-22");

        var result = DateTimeUtils.daysBetween(from, to).collect(Collectors.toList());
        assertThat(result, contains(from, LocalDate.parse("2020-06-21"), to));
    }

    @Test
    public void testDaysBetweenTwoLocalDateTimes() {
        LocalDateTime from = LocalDateTime.parse("2020-06-20T23:24");
        LocalDateTime  to = LocalDateTime.parse("2020-06-22T23:23");

        var result = DateTimeUtils.daysBetween(from, to).collect(Collectors.toList());
        assertThat(result, contains(from, from.plusDays(1)));
    }

    private static Stream<Arguments> params() {
        LocalDateTime zeroMinutes = LocalDateTime.of(2020, 3, 3, 19, 0, 0);
        LocalDateTime nextHourZeroMinutes = zeroMinutes.plusHours(1);

        LocalDateTime oneMinutes = zeroMinutes.withMinute(1);
        LocalDateTime twentyThreeMinutes = zeroMinutes.withMinute(23);
        LocalDateTime fiftyOneMinutes = zeroMinutes.withMinute(51);

        return Stream.of(
            Arguments.of(zeroMinutes, 30, zeroMinutes),
            Arguments.of(zeroMinutes, 2, zeroMinutes),

            Arguments.of(oneMinutes, 30, zeroMinutes.withMinute(30)),
            Arguments.of(oneMinutes, 2, zeroMinutes.withMinute(2)),

            Arguments.of(twentyThreeMinutes, 30, zeroMinutes.withMinute(30)),
            Arguments.of(twentyThreeMinutes, 25, zeroMinutes.withMinute(25)),
            Arguments.of(twentyThreeMinutes, 20, zeroMinutes.withMinute(40)),
            Arguments.of(twentyThreeMinutes, 17, zeroMinutes.withMinute(34)),
            Arguments.of(twentyThreeMinutes, 15, zeroMinutes.withMinute(30)),
            Arguments.of(twentyThreeMinutes, 10, zeroMinutes.withMinute(30)),
            Arguments.of(twentyThreeMinutes, 8, zeroMinutes.withMinute(24)),

            Arguments.of(fiftyOneMinutes, 30, nextHourZeroMinutes),
            Arguments.of(fiftyOneMinutes, 25, nextHourZeroMinutes.withMinute(15)),
            Arguments.of(fiftyOneMinutes, 20, nextHourZeroMinutes),
            Arguments.of(fiftyOneMinutes, 17, zeroMinutes.withMinute(51)),
            Arguments.of(fiftyOneMinutes, 15, nextHourZeroMinutes),
            Arguments.of(fiftyOneMinutes, 10, nextHourZeroMinutes),
            Arguments.of(fiftyOneMinutes, 8, zeroMinutes.withMinute(56))
        );
    }
}
