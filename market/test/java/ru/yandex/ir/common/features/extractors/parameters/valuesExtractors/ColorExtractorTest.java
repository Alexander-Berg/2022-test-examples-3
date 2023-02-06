package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.ColorExtractor.Color;

public class ColorExtractorTest extends AbstractValuesExtractorTest<Color> {
    private static final Color TURQUOISE = new Color(48f / 255, 213f / 255, 200f / 255);
    private static final Color CYAN = new Color(0, 1, 1);

    @Override
    protected ValuesExtractor<Color> getValuesExtractor() {
        Map<String, Color> colorMap = new HashMap<>();
        colorMap.put("бирюзовый", TURQUOISE);
        colorMap.put("cyan", CYAN);
        return new ColorExtractor(colorMap);
    }

    @Override
    protected String[] getTitles() {
        return new String[] {
                "Вакуумный контейнер Zoku Neat Stack 295 мл бирюзовый",
                "Термобелье шорты женские Sprint cyan 46"
        };
    }

    @Override
    protected List<List<Color>> getExpectedValues() {
        return Arrays.asList(
                Arrays.asList(TURQUOISE),
                Arrays.asList(CYAN)
        );
    }

    @ParameterizedTest()
    @ArgumentsSource(ColorExtractorTest.class)
    public void testExtractColors(String title, List<Color> expectedValues) {
        super.testExtractValues(title, expectedValues);
    }

    @Test
    public void testReadColorMap() {
        Map<String, Color> colorMap = ColorExtractor.readColorMap();
        Assertions.assertEquals(colorMap.get("cyan"), CYAN);
        Assertions.assertEquals(colorMap.get("бирюзовый"), TURQUOISE);
    }
}
