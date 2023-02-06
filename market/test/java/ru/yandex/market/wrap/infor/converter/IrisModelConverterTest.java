package ru.yandex.market.wrap.infor.converter;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class IrisModelConverterTest {

    @Test
    void gramsToKilograms() {
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(IrisModelConverter.gramsToKilograms(null))
                .as("Result of conversion null gram to kg should be null")
                .isNull();

            assertions.assertThat(IrisModelConverter.gramsToKilograms(BigDecimal.valueOf(1000)))
                .as("Result of conversion 1000 gram to kg should be 1 kg")
                .isEqualByComparingTo(BigDecimal.valueOf(1));

            assertions.assertThat(IrisModelConverter.gramsToKilograms(BigDecimal.valueOf(1234)))
                .as("Result of conversion 1234 gram to kg should be 1.234 kg")
                .isEqualByComparingTo(new BigDecimal("1.234"));

            assertions.assertThat(IrisModelConverter.gramsToKilograms(BigDecimal.valueOf(1)))
                .as("Result of conversion 1 gram to kg should be 0.001 kg")
                .isEqualByComparingTo(new BigDecimal("0.001"));

            assertions.assertThat(IrisModelConverter.gramsToKilograms(new BigDecimal("0.071")))
                .as("Result of conversion 0.071 gram to kg should be 0.00008 (scale 5, roundMode.up)")
                .isEqualByComparingTo(new BigDecimal("0.00008"));
        });
    }

    @Test
    void millimetersToCentimeters() {
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(IrisModelConverter.millimetersToCentimeters(null))
                .as("Result of conversion null mm to cm should be null")
                .isNull();

            assertions.assertThat(IrisModelConverter.millimetersToCentimeters(BigDecimal.valueOf(111)))
                .as("Result of conversion 111 mm to cm should be 11.1 cm")
                .isEqualByComparingTo(new BigDecimal("11.1"));

            assertions.assertThat(IrisModelConverter.millimetersToCentimeters(BigDecimal.valueOf(1)))
                .as("Result of conversion 1 mm to cm should be 0.1 cm")
                .isEqualByComparingTo(new BigDecimal("0.1"));

            assertions.assertThat(IrisModelConverter.millimetersToCentimeters(new BigDecimal("0.00011")))
                .as("Result of conversion 0.00011 mm to cm should be 0.2 cm (scale 5, roundMode.up)")
                .isEqualByComparingTo(new BigDecimal("0.00002"));
        });
    }
}
