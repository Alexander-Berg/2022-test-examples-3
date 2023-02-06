package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CentimetersToMillimetersConversionTest {

    @Test
    void testConversion() throws Exception {
        assertThat(WeightsAndDimensions.centimetersToMillimeters(10))
                .as("Asserting that centimeters to millimeters conversion is correct")
                .isEqualTo(100);
    }
}
