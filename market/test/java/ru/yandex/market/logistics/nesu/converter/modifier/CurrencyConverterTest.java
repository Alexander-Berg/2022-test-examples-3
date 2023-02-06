package ru.yandex.market.logistics.nesu.converter.modifier;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.AbstractTest;

public class CurrencyConverterTest extends AbstractTest {

    private final CurrencyConverter currencyConverter = new CurrencyConverter();

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("rublesToKopecksArguments")
    @DisplayName("Проверка конвертации рубли -> копейки")
    void rublesToKopecks(BigDecimal input, BigDecimal output) {
        softly.assertThat(currencyConverter.rublesToKopecks(input)).isEqualByComparingTo(output);
    }

    public static Stream<Arguments> rublesToKopecksArguments() {
        return Stream.of(
            Arguments.of(BigDecimal.valueOf(123.4512), BigDecimal.valueOf(12345)),
            Arguments.of(BigDecimal.valueOf(123.4567), BigDecimal.valueOf(12346)),
            Arguments.of(BigDecimal.valueOf(123), BigDecimal.valueOf(12300)),
            Arguments.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(100000)),
            Arguments.of(BigDecimal.valueOf(1999), BigDecimal.valueOf(199900))
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("kopecksToRublesArguments")
    @DisplayName("Проверка конвертации копейки -> рубли")
    void kopecksToRubles(BigDecimal input, BigDecimal output) {
        softly.assertThat(currencyConverter.kopecksToRubles(input)).isEqualByComparingTo(output);
    }

    public static Stream<Arguments> kopecksToRublesArguments() {
        return Stream.of(
            Arguments.of(BigDecimal.valueOf(12345.12), BigDecimal.valueOf(123.45)),
            Arguments.of(BigDecimal.valueOf(12345.67), BigDecimal.valueOf(123.46)),
            Arguments.of(BigDecimal.valueOf(12300), BigDecimal.valueOf(123.00)),
            Arguments.of(BigDecimal.valueOf(100000), BigDecimal.valueOf(1000)),
            Arguments.of(BigDecimal.valueOf(199999), BigDecimal.valueOf(1999.99))
        );
    }

}
