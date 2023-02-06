package ru.yandex.market.logistics.management.util;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;

class CoordinateUtilTest extends AbstractTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("coordinates")
    @DisplayName("Сравнение координат")
    void test(BigDecimal oldCoordinate, BigDecimal newCoordinate, boolean expected) {
        softly.assertThat(CoordinateUtil.isCoordinateNotChanged(oldCoordinate, newCoordinate)).isEqualTo(expected);
    }

    @Nonnull
    private static Stream<Arguments> coordinates() {
        return Stream.of(
            Arguments.of(new BigDecimal("1"), new BigDecimal("1"), true),
            Arguments.of(new BigDecimal("2"), new BigDecimal("2.00"), true),
            Arguments.of(new BigDecimal("3.000"), new BigDecimal("3.0"), true),
            Arguments.of(null, null, true),
            Arguments.of(new BigDecimal("5"), new BigDecimal("500"), false),
            Arguments.of(null, new BigDecimal("6"), false)
        );
    }
}
