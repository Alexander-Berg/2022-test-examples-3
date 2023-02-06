package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class AbstractValuesExtractorTest<T> implements ArgumentsProvider {
    public ValuesExtractor<T> valuesExtractor;

    protected abstract ValuesExtractor<T> getValuesExtractor();

    protected abstract String[] getTitles();

    protected abstract List<List<T>> getExpectedValues();

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        String[] titles = getTitles();
        List<List<T>> expectedValues = getExpectedValues();
        return IntStream.range(0, titles.length).mapToObj(i -> Arguments.of(titles[i], expectedValues.get(i)));
    }

    @BeforeEach
    public void init() {
        valuesExtractor = getValuesExtractor();
    }

    protected void testExtractValues(String title, List<T> expectedValues) {
        Assertions.assertEquals(expectedValues, valuesExtractor.extractParamValues(title));
    }
}
