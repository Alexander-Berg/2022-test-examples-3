package ru.yandex.market.delivery.entities.common;

import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DateTimeTestNegative extends BaseTest {

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("09-12-2017"),
            Arguments.of("09/12/2017"),
            Arguments.of("2017/09/10"),
            Arguments.of("2017-09-10T"),
            Arguments.of("2017-09-10T05:56:10:566"),
            Arguments.of("2017-09-10T99:16:00+03:00"),
            Arguments.of("2017-09-10 99:16:00+03:00"),
            Arguments.of("2017-09-10 T99:16:00+03:00"),
            Arguments.of("2017-09-10T11:16:0+03:00"),
            Arguments.of("2017-09-10T03:16:0"),
            Arguments.of("2017-09-10T3:16:00"),
            Arguments.of("2017-09-10T15:16:"),
            Arguments.of("14:02:01+05:00"),
            Arguments.of("14:02:01")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void itShouldThrowException(String incomingString) {
        softly.assertThatThrownBy(() -> new DateTime(incomingString)).isInstanceOf(DateTimeParseException.class);
    }
}
