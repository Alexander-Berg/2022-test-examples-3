package ru.yandex.market.delivery.entities.common;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.entities.common.exeption.ParseException;

class TimeIntervalTestNegativeConstruct extends BaseTest {

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("12:00-13:00"),
            Arguments.of("12/13")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void test(String incomingString) {
        softly.assertThatThrownBy(() -> new TimeInterval(incomingString)).isInstanceOf(ParseException.class);
    }

}
