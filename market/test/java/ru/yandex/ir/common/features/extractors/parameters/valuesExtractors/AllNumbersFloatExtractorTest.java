package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.List;

import static ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.NumberWithUnitExtractor.NumberWithUnit;

public class AllNumbersFloatExtractorTest extends AllSizesExtractorTest {
    @Override
    protected List<List> getExpectedValues() {
        return Arrays.asList(
                Arrays.asList(22f, 31f, 66f, 42f),
                Arrays.asList(22f, 31f, 66f, 42f, 24f),
                Arrays.asList(43f),
                Arrays.asList(320f, 350f, 36f, 500f),
                Arrays.asList(320f, 350f, 48f, 43f),
                Arrays.asList(8023809343f),
                Arrays.asList(2f, 40f, 211200f),
                Arrays.asList(117.5f, 150f, 150f, 50f, 0.73f, 54f),
                Arrays.asList(18f, 13f, 65093f),
                Arrays.asList(45f),
                Arrays.asList(45f));
    }

    @Override
    protected ValuesExtractor<Float> getValuesExtractor() {
        return new AllNumbersFloatExtractor();
    }

    @ParameterizedTest()
    @ArgumentsSource(AllNumbersFloatExtractorTest.class)
    public void testExtractSizes(String title, List<NumberWithUnit> expectedValues) {
        super.testExtractValues(title, expectedValues);
    }

    @Override
    @Test
    public void testCalculateDelta() {
        Assertions.assertEquals(0, valuesExtractor.calculateDelta(23f, 23f));
        Assertions.assertEquals(0.4, valuesExtractor.calculateDelta(20f, 30f), 1e-5);
    }
}
