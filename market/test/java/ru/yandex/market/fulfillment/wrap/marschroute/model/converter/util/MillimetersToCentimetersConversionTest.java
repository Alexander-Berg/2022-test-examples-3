package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util.WeightsAndDimensions.millimetersToCentimeters;

class MillimetersToCentimetersConversionTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(0, 0),
            Arguments.of(80, 8),
            Arguments.of(77, 8),
            Arguments.of(75, 8),
            Arguments.of(73, 8),
            Arguments.of(71, 8),
            Arguments.of(70, 7)
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testConversion(int millimeters, int expectedCentimeters) throws Exception {
        int actualCentimeters = millimetersToCentimeters(millimeters);

        assertThat(actualCentimeters)
            .as("Asserting that millimeters were converted to centimeters correctly")
            .isEqualTo(expectedCentimeters);
    }
}
