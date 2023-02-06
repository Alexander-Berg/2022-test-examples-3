package ru.yandex.market.logistic.gateway.service.converter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConverterHelperTest {
    private final Map<Integer, String> TEST_MAP = ImmutableMap.of(
        1, "one",
        2, "two",
        3, "three",
        4, "four"
    );

    @Test
    public void convertList_returnsNull_whenListIsNull() {
        assertThat(ConverterHelper.convertList(this::testConverter, null)).isNull();
    }

    @Test
    public void convertList_returnsNull_whenConverterIsNull() {
        assertThat(ConverterHelper.convertList(null, new ArrayList<>(TEST_MAP.keySet()))).isNull();
    }

    @Test
    public void convertList_returnsListGeneratedWithConversionFunction() {
        var output = ConverterHelper.convertList(this::testConverter, new ArrayList<>(TEST_MAP.keySet()));

        assertThat(output)
            .hasSameElementsAs(TEST_MAP.values())
            .hasSameSizeAs(TEST_MAP.values());
    }

    @Test
    public void convertList_returnsListWithoutEmptyConvertedValues() {
        var output = ConverterHelper.convertList(this::testConverter, List.of(1, 2, 5));

        List<String> expectedOutput = List.of("one", "two");
        assertThat(output)
            .hasSameElementsAs(expectedOutput)
            .hasSameSizeAs(expectedOutput);
    }

    private Optional<String> testConverter(Integer i) {
        return Optional.ofNullable(TEST_MAP.getOrDefault(i, null));
    }
}
