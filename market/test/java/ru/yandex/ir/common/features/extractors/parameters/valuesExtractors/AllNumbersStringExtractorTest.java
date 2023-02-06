package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.List;

import static ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.NumberWithUnitExtractor.NumberWithUnit;

public class AllNumbersStringExtractorTest extends AllSizesExtractorTest {
    @Override
    protected List<List> getExpectedValues() {
        return Arrays.asList(
                Arrays.asList("22", "31", "66", "42"),
                Arrays.asList("22", "31", "66", "42", "24"),
                Arrays.asList("043"),
                Arrays.asList("320", "350", "36", "500"),
                Arrays.asList("320", "350", "48", "043"),
                Arrays.asList("8023809343"),
                Arrays.asList("2", "40", "211200"),
                Arrays.asList("117", "5", "150", "150", "50", "0", "73", "54"),
                Arrays.asList("18", "13", "65093"),
                Arrays.asList("45"),
                Arrays.asList("45"));
    }

    @Override
    protected ValuesExtractor<String> getValuesExtractor() {
        return new AllNumbersStringExtractor();
    }

    @ParameterizedTest()
    @ArgumentsSource(AllNumbersStringExtractorTest.class)
    public void testExtractSizes(String title, List<NumberWithUnit> expectedValues) {
        super.testExtractValues(title, expectedValues);
    }

    @Override
    @Test
    public void testCalculateDelta() {
        Assertions.assertEquals(0, valuesExtractor.calculateDelta("23", "23"));
        Assertions.assertEquals(0.5, valuesExtractor.calculateDelta("20", "30"), 1e-5);
        Assertions.assertEquals(0.4, valuesExtractor.calculateDelta("20", "020"), 1e-5);
    }
}
