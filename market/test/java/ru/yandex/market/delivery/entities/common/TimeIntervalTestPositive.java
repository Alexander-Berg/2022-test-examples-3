package ru.yandex.market.delivery.entities.common;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TimeIntervalTestPositive extends BaseTest {

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("12:00/13:00", "12:00:00+03:00/13:00:00+03:00"),
            Arguments.of("12:00:00/13:00", "12:00:00+03:00/13:00:00+03:00"),
            Arguments.of("12:00:00/13:00:00", "12:00:00+03:00/13:00:00+03:00"),
            Arguments.of("00:00/24:00", "00:00:00+03:00/23:59:59+03:00"),
            Arguments.of("00:00/23:56", "00:00:00+03:00/23:56:00+03:00"),
            Arguments.of("00:00+04:00/23:56", "00:00:00+04:00/23:56:00+03:00"),
            Arguments.of("00:00+04:00/23:56+04:00", "00:00:00+04:00/23:56:00+04:00"),
            Arguments.of("00:00+04:30/23:56+04:00", "00:00:00+04:30/23:56:00+04:00"),
            Arguments.of("00:00:03+04:30/23:56:10+04:00", "00:00:03+04:30/23:56:10+04:00"),
            //хорошо бы решить, валидая ли это ситуация с перескоком суток (пока считаем, что да)
            Arguments.of("23:50/01:05", "23:50:00+03:00/01:05:00+03:00"),
            //рельно это значит, что начало работы в 01:00 и завершение в 23:00 по мск, пока считаем что это валидно
            Arguments.of("22:00+00:00/23:00", "22:00:00+00:00/23:00:00+03:00")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void test(String incomingString, String outputString) {
        softly.assertThat(new TimeInterval(incomingString).getFormattedTimeInterval()).isEqualTo(outputString);
    }

}
