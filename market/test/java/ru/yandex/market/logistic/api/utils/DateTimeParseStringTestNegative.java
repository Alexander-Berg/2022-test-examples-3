package ru.yandex.market.logistic.api.utils;

import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;

class DateTimeParseStringTestNegative {

    @MethodSource("data")
    @ParameterizedTest
    void itShouldThrowException(String incomingString) {
        assertThrows(DateTimeParseException.class, () -> new DateTime(incomingString));
    }

    static Stream<Arguments> data() {
        return Stream.of(
            of("09-12-2017"),
            of("09/12/2017"),
            of("2017/09/10"),
            of("2017-09-10T"),
            of("2017-09-10T05:56:10:566"),
            of("2017-09-10T99:16:00+03:00"),
            of("2017-09-10 99:16:00+03:00"),
            of("2017-09-10 T99:16:00+03:00"),
            of("2017-09-10T11:16:0+03:00"),
            of("2017-09-10T03:16:0"),
            of("2017-09-10T3:16:00"),
            of("2017-09-10T15:16:"),
            of("14:02:01+05:00"),
            of("14:02:01"),
            of("0000-00-00T21:59:59+03:00"),
            of("2019-07-12T15:27:15.605+0000")
        );
    }
}
