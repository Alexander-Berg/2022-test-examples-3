package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NumberWithCompatibleUnitExtractorTest {
    private static final float EPS = 1e-5f;

    @Test
    public void testCalculateDelta() {
        NumberWithCompatibleUnitsExtractor extractor = new NumberWithCompatibleUnitsExtractor(CompatibleUnitsMultipliers.volumes());
        Assertions.assertEquals(0, extractor.calculateDelta(
                new NumberWithUnitExtractor.NumberWithUnit(10, Unit.MILLILITER),
                new NumberWithUnitExtractor.NumberWithUnit(10, Unit.MILLILITER)), EPS);
        Assertions.assertEquals(0, extractor.calculateDelta(
                new NumberWithUnitExtractor.NumberWithUnit(0.5f, Unit.LITER),
                new NumberWithUnitExtractor.NumberWithUnit(500, Unit.MILLILITER)), EPS);
        Assertions.assertEquals(2 * 200f / 800, extractor.calculateDelta(
                new NumberWithUnitExtractor.NumberWithUnit(0.5f, Unit.LITER),
                new NumberWithUnitExtractor.NumberWithUnit(300, Unit.MILLILITER)), EPS);
    }
}
