package ru.yandex.market.logistic.api.utils;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

class DateTimeParseStringTestPositive {

    @MethodSource("data")
    @ParameterizedTest
    void itShouldHaveCorrectlyIncomingString(String incomingString, String expectedString) {
        assertEquals(
            incomingString,
            new DateTime(incomingString).getInputString(),
            "Date Time incoming string is invalid"
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void itShouldHaveCorrectlyDateObject(String incomingString, String expectedString) {
        assertEquals(
            OffsetDateTime.parse(expectedString),
            new DateTime(incomingString).getOffsetDateTime(),
            "Date Object is invalid"
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void itShouldHaveCorrectlyFormattedString(String incomingString, String expectedString) {
        assertEquals(
            expectedString,
            new DateTime(incomingString).getFormattedDate(),
            "Date Formatted string is invalid"
        );
    }


    static Stream<Arguments> data() {
        return Stream.of(
            of("2017-09-10", "2017-09-10T00:00:00+03:00"),
            of("2019-07-12+03:00", "2019-07-12T00:00:00+03:00"),
            of("2019-07-12+05:00", "2019-07-12T00:00:00+05:00"),
            of("2017-09-10T10:30", "2017-09-10T10:30:00+03:00"),
            of("2017-09-10T05:56:10", "2017-09-10T05:56:10+03:00"),
            of("2017-09-10T10:16:00+03:00", "2017-09-10T10:16:00+03:00"),
            of("2017-09-10T14:02:01+05:00", "2017-09-10T14:02:01+05:00"),
            of("2017-09-10T12:04:11+05:30", "2017-09-10T12:04:11+05:30"),
            of("2017-09-10T12:04:11+00:00", "2017-09-10T12:04:11+00:00"),
            of("2019-07-12T13:23:27.8", "2019-07-12T13:23:27+03:00"),
            of("2019-07-12T13:23:27.82", "2019-07-12T13:23:27+03:00"),
            of("2019-07-12T13:23:27.864", "2019-07-12T13:23:27+03:00"),
            of("2019-07-12T13:23:27.86413", "2019-07-12T13:23:27+03:00"),
            of("2019-07-12T13:23:27.864137", "2019-07-12T13:23:27+03:00"),
            of("2019-07-12T13:23:27.8641375", "2019-07-12T13:23:27+03:00"),
            of("2019-07-12T13:23:27.86413754", "2019-07-12T13:23:27+03:00"),
            of("2019-07-12T13:23:27.864137548", "2019-07-12T13:23:27+03:00"),
            of("2019-07-12T13:23:27.8+05:00", "2019-07-12T13:23:27+05:00"),
            of("2019-07-12T13:23:27.82+05:00", "2019-07-12T13:23:27+05:00"),
            of("2019-07-12T13:23:27.864+05:00", "2019-07-12T13:23:27+05:00"),
            of("2019-07-12T13:23:27.86413+05:00", "2019-07-12T13:23:27+05:00"),
            of("2019-07-12T13:23:27.864137+05:00", "2019-07-12T13:23:27+05:00"),
            of("2019-07-12T13:23:27.8641375+05:00", "2019-07-12T13:23:27+05:00"),
            of("2019-07-12T13:23:27.86413754+05:00", "2019-07-12T13:23:27+05:00"),
            of("2019-07-12T13:23:27.864137548+05:00", "2019-07-12T13:23:27+05:00")
        );
    }
}
