package ru.yandex.market.logistic.api.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

class DateTimeParseLocalDateTimeTest {

    @MethodSource("data")
    @ParameterizedTest
    void itShouldHaveCorrectlyDateObject(LocalDateTime incomingDateTime, String expectedString) {
        assertEquals(
            OffsetDateTime.parse(expectedString),
            DateTime.fromLocalDateTime(incomingDateTime).getOffsetDateTime(),
            "Date Object is invalid"
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void itShouldHaveCorrectlyFormattedString(LocalDateTime incomingDateTime, String expectedString) {
        assertEquals(
            expectedString,
            DateTime.fromLocalDateTime(incomingDateTime).getFormattedDate(),
            "Date Formatted string is invalid"
        );
    }

    static Stream<Arguments> data() {
        return Stream.of(
            of(
                LocalDateTime.of(2019, 7, 12, 0, 0),
                "2019-07-12T00:00:00+03:00"
            ),
            of(
                LocalDateTime.of(2019, 7, 12, 10, 30),
                "2019-07-12T10:30:00+03:00"
            ),
            of(
                LocalDateTime.of(2019, 7, 12, 10, 30, 10),
                "2019-07-12T10:30:10+03:00"
            ),
            of(
                LocalDateTime.of(2019, 7, 12, 10, 30, 10, 800000000),
                "2019-07-12T10:30:10+03:00"
            ),
            of(
                LocalDateTime.of(2019, 7, 12, 10, 30, 0, 168000000),
                "2019-07-12T10:30:00+03:00"
            ),
            of(
                LocalDateTime.of(2019, 7, 12, 10, 30, 0, 168000000),
                "2019-07-12T10:30:00+03:00"
            ));
    }

}
