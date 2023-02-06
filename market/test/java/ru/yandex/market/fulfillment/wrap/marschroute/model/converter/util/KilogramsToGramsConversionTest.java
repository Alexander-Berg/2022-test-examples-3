package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class KilogramsToGramsConversionTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(BigDecimal.valueOf(0), 0),
            Arguments.of(BigDecimal.valueOf(1), 1000),
            Arguments.of(BigDecimal.valueOf(2), 2000),
            Arguments.of(BigDecimal.valueOf(1.0), 1000),
            Arguments.of(BigDecimal.valueOf(1.1), 1100),
            Arguments.of(BigDecimal.valueOf(1.2), 1200),
            Arguments.of(BigDecimal.valueOf(2.0), 2000)
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testKilogramsToGramsConversion(BigDecimal kilograms, int expectedGrams) throws Exception {
        int actualGrams = WeightsAndDimensions.kilogramsToGrams(kilograms);

        assertThat(actualGrams)
            .as("Assert that kg to g conversion was correct")
            .isEqualTo(expectedGrams);
    }
}
