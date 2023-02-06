package ru.yandex.direct.jobs.payment;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.direct.jobs.payment.TurnOnAutopayUtils.calcTryLater;

class CalcTryLaterTest {

    static Stream<Arguments> testData() {
        return Stream.of(
                arguments(0, 5),
                arguments(1, 5),
                arguments(2, 5),
                arguments(3, 5),
                arguments(4, 10),
                arguments(5, 10),
                arguments(6, 10),
                arguments(7, 10),
                arguments(8, 20),
                arguments(9, 20),
                arguments(10, 20),
                arguments(11, 20),
                arguments(12, 40),
                arguments(13, 40),
                arguments(14, 40),
                arguments(15, 40),
                arguments(16, 80)
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void test(int tryCount, int expectedSeconds) {
        Duration resultDuration = calcTryLater((long) tryCount);
        assertThat(resultDuration).isEqualTo(Duration.ofSeconds(expectedSeconds));
    }

}
