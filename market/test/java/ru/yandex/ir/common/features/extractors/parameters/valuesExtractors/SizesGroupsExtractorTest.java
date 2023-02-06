package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.List;

import static ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.SizesGroupExtractor.SizesGroup;
import static ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.NumberWithUnitExtractor.NumberWithUnit;

public class SizesGroupsExtractorTest extends AllSizesExtractorTest {
    @Override
    protected List<List> getExpectedValues() {
        return Arrays.asList(
                Arrays.asList(new SizesGroup(new NumberWithUnit(22, null), new NumberWithUnit(31f, null),
                        new NumberWithUnit(66f, null), new NumberWithUnit(42f, null))),
                Arrays.asList(new SizesGroup(new NumberWithUnit(22, null), new NumberWithUnit(31f, null),
                        new NumberWithUnit(66f, null), new NumberWithUnit(42f, null))),
                Arrays.asList(),
                Arrays.asList(new SizesGroup(new NumberWithUnit(320f, Unit.MILLIMETER), new NumberWithUnit(350f, Unit.MILLIMETER), new NumberWithUnit(36f, Unit.MILLIMETER))),
                Arrays.asList(new SizesGroup(new NumberWithUnit(320f, Unit.MILLIMETER), new NumberWithUnit(350f, Unit.MILLIMETER), new NumberWithUnit(48f, Unit.MILLIMETER))),
                Arrays.asList(),
                Arrays.asList(new SizesGroup(new NumberWithUnit(2f, Unit.MILLIMETER), new NumberWithUnit(40f, Unit.CENTIMETER))),
                Arrays.asList(),
                Arrays.asList(new SizesGroup(new NumberWithUnit(18f, Unit.CENTIMETER), new NumberWithUnit(13f, Unit.CENTIMETER))),
                Arrays.asList(),
                Arrays.asList());
    }

    @Override
    protected ValuesExtractor<SizesGroup> getValuesExtractor() {
        return new SizesGroupExtractor();
    }

    @ParameterizedTest()
    @ArgumentsSource(SizesGroupsExtractorTest.class)
    public void testExtractSizes(String title, List<NumberWithUnit> expectedValues) {
        super.testExtractValues(title, expectedValues);
    }

    @Override
    @Test
    public void testCalculateDelta() {
        Assertions.assertEquals(0, valuesExtractor.calculateDelta(new SizesGroup(), new SizesGroup()));
        Assertions.assertEquals(0, valuesExtractor.calculateDelta(
                new SizesGroup(new NumberWithUnit(15, Unit.CENTIMETER)),
                new SizesGroup()));
        Assertions.assertEquals(2 * 5f / 15, valuesExtractor.calculateDelta(
                new SizesGroup(new NumberWithUnit(5, Unit.CENTIMETER), new NumberWithUnit(20, Unit.CENTIMETER)),
                new SizesGroup(new NumberWithUnit(10, Unit.CENTIMETER))), 1e-5);
        Assertions.assertEquals(2 * 5f / 15 + 2 * 10f / 50, valuesExtractor.calculateDelta(
                new SizesGroup(new NumberWithUnit(5, Unit.CENTIMETER), new NumberWithUnit(30, Unit.CENTIMETER)),
                new SizesGroup(new NumberWithUnit(10, Unit.CENTIMETER), new NumberWithUnit(20, Unit.CENTIMETER))), 1e-5);
    }
}
