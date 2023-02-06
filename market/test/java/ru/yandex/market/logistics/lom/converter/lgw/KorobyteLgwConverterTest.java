package ru.yandex.market.logistics.lom.converter.lgw;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.entity.embedded.Korobyte;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

public class KorobyteLgwConverterTest {
    static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    private KorobyteLgwConverter converter;

    @BeforeEach
    void setup() {
        converter = new KorobyteLgwConverter();
    }

    @DisplayName("Конвертация объема: ")
    @MethodSource("volumeTestSource")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void toVolumeCmToM(String caseName, Triple<Integer, Integer, Integer> values, Float expected) {
        Korobyte korobyte = new Korobyte();
        korobyte.setHeight(values.getLeft())
                .setWidth(values.getMiddle())
                .setLength(values.getRight());

        Float result = converter.toVolumeM(korobyte);
        Assertions.assertThat(result).isGreaterThanOrEqualTo(expected);
    }

    @DisplayName("Null korobyte")
    @Test
    void toVolumeMNull() {
        Float result = converter.toVolumeM(null);
        Assertions.assertThat(result).isNull();
    }

    @DisplayName("Конвертация веса с округлением до десятых")
    @Test
    void toWeightKg() {
        Korobyte korobyte = new Korobyte()
            .setWeightGross(BigDecimal.valueOf(1.00001));
        Assertions.assertThat(converter.toWeightKg(korobyte)).isGreaterThanOrEqualTo(1.1f);
    }

    private static Stream<Arguments> volumeTestSource() {
        return Stream.of(
                Triple.of(
                        "маленький объем, округляем до десятых",
                        Triple.of(15, 3, 10),
                        0.1f
                ),
                Triple.of(
                        "большой",
                        Triple.of(50, 50, 1000),
                        2.5f
                ),
                Triple.of(
                        "кубометр",
                        Triple.of(100, 100, 100),
                        1f
                )
        ).map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }
}
