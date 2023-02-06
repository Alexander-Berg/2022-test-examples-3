package ru.yandex.market.delivery.entities.common;

import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TimeIntervalTestNegativeDateParse extends BaseTest {

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("25:00:00+03:00/30:00:00+03:00"),
            Arguments.of("10:70:00+03:00/12:99:00+03:00"),
            Arguments.of("10:00:00+03:00/12:01:00+99:00")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void test(String incomingString) {
        softly.assertThatThrownBy(() -> new TimeInterval(incomingString)).isInstanceOf(DateTimeParseException.class);
    }

}
