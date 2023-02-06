package ru.yandex.market.logistic.api.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

class DateTimeParseOffsetDateTimeTest {

    @MethodSource("data")
    @ParameterizedTest
    void itShouldHaveCorrectlyDateObject(OffsetDateTime incomingDateTime, String expectedString) {
        assertEquals(
            OffsetDateTime.parse(expectedString),
            DateTime.fromOffsetDateTime(incomingDateTime).getOffsetDateTime(),
            "Date Object is invalid"
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void itShouldHaveCorrectlyFormattedString(OffsetDateTime incomingDateTime, String expectedString) {
        assertEquals(
            expectedString,
            DateTime.fromOffsetDateTime(incomingDateTime).getFormattedDate(),
            "Date Formatted string is invalid"
        );
    }

    static Stream<Arguments> data() {
        return Stream.of(
            of(
                OffsetDateTime.of(
                    LocalDateTime.of(2019, 7, 12, 0, 0),
                    ZoneOffset.ofHours(0)
                ),
                "2019-07-12T00:00:00+00:00"
            ),
            of(
                OffsetDateTime.of(
                    LocalDateTime.of(2019, 7, 12, 10, 30),
                    ZoneOffset.ofHours(3)
                ),
                "2019-07-12T10:30:00+03:00"
            ),
            of(
                OffsetDateTime.of(
                    LocalDateTime.of(2019, 7, 12, 10, 30),
                    ZoneOffset.ofHoursMinutes(3, 30)
                ),
                "2019-07-12T10:30:00+03:30"
            ),
            of(
                OffsetDateTime.of(
                    LocalDateTime.of(2019, 7, 12, 10, 30, 18),
                    ZoneOffset.ofHours(5)
                ),
                "2019-07-12T10:30:18+05:00"
            ),
            of(
                OffsetDateTime.of(
                    LocalDateTime.of(2019, 7, 12, 10, 30, 18, 800000000),
                    ZoneOffset.ofHours(5)
                ),
                "2019-07-12T10:30:18+05:00"
            ),
            of(
                OffsetDateTime.of(
                    LocalDateTime.of(2019, 7, 12, 10, 30, 0, 168000000),
                    ZoneOffset.ofHours(5)
                ),
                "2019-07-12T10:30:00+05:00"
            )
        );
    }
}
