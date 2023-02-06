package ru.yandex.direct.core.entity.bs.common.service;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class BsBannerIdCalculatorTest {

    static Stream<Arguments> positiveCases() {
        return Stream.of(
                arguments(1L, 0x100000000000001L),
                arguments(123456789L, 72057594161384725L),
                arguments((1L << 56) - 1, 0x1ffffffffffffffL)
        );
    }

    static Stream<Arguments> negativeCases() {
        return Stream.of(
                arguments("Отрицательный directBannerId", -1L),
                arguments("Слишком большой directBannerId", 1L << 56)
        );
    }

    @ParameterizedTest
    @MethodSource("positiveCases")
    void checkCalculateBsBannerId(Long directBannerId, Long expectedBsBannerId) {
        assertThat(BsBannerIdCalculator.calculateBsBannerId(directBannerId))
                .isEqualTo(expectedBsBannerId);
    }

    @ParameterizedTest
    @MethodSource("negativeCases")
    void calculateBsBannerIdReturnsErrorWhenArgumentsAreInvalid(
            String description, Long directBannerId) {
        //noinspection ResultOfMethodCallIgnored
        assertThatThrownBy(() -> BsBannerIdCalculator.calculateBsBannerId(directBannerId))
                .as(description)
                .isInstanceOf(IllegalArgumentException.class);
    }
}
