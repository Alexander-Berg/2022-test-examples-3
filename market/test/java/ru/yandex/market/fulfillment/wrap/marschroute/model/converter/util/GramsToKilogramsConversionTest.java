package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class GramsToKilogramsConversionTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(0, BigDecimal.valueOf(0)),
            Arguments.of(3, BigDecimal.valueOf(0.1)),
            Arguments.of(100, BigDecimal.valueOf(0.1)),
            Arguments.of(101, BigDecimal.valueOf(0.2)),
            Arguments.of(111, BigDecimal.valueOf(0.2)),
            Arguments.of(150, BigDecimal.valueOf(0.2)),
            Arguments.of(153, BigDecimal.valueOf(0.2)),
            Arguments.of(1000, BigDecimal.valueOf(1.0)),
            Arguments.of(1200, BigDecimal.valueOf(1.2))
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testGramsToKilogramsConversion(int grams, BigDecimal expectedKilograms) throws Exception {
        BigDecimal actualKilograms = WeightsAndDimensions.gramsToKilograms(grams);

        assertThat(actualKilograms)
            .as("Asserting that conversion from grams to kilograms went fine")
            .isEqualTo(actualKilograms);
    }
}
