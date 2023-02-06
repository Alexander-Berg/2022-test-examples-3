package ru.yandex.market.delivery.rupostintegrationapp.model.sequence.generator;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;

class TrackCodeCheckDigitGeneratorTest extends BaseTest {

    private static Stream<Arguments> getParameters() {
        return Stream.of(
            Arguments.of(1974409, 3),
            Arguments.of(798124, 5),
            Arguments.of(1974427, 0),
            Arguments.of(0, 5)
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testCheckDigitGenerating(long serialNumber, int expectedCheckDigit) {
        softly.assertThat(TrackCodeCheckDigitGenerator.getCheckDigit(serialNumber))
            .as("Asserting that the generated check digit is valid")
            .isEqualTo(expectedCheckDigit);
    }
}
