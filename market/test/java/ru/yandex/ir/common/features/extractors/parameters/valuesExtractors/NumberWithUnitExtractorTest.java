package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.NumberWithUnitExtractor.NumberWithUnit;

import java.util.Arrays;
import java.util.List;

public class NumberWithUnitExtractorTest extends AbstractValuesExtractorTest<NumberWithUnit> {
    private static final double EPS = 1e-5;

    @Override
    protected String[] getTitles() {
        return new String[]{
                "Размеры: 320х350х36мм<br />\\nсредний<br />\\nвес - 500г",
                "Противень",
                "Спицы круговые 2мм/40см, Prym, 211200",
                "Характеристики: Высота, мм: 117.5; Длина, мм: 150; Ширина, мм: 150; Мощность, эквивалентная лампам накаливания, Вт: 50; Вес, кг: 0.73; Степень защиты, IP: 54;",
                "Чайник 2,7л ZILLINGER ZL-5001-27 зеленый",
                "Аккумуляторная батарея iBatt iB-B1-A366H 5200mAh для ноутбуков LG LB52113D, LB62115E, LB32111B",
                "Состав: 25% мохер, 24% шерсть, 51% акрилДлина: 200 мВес: 100 г"
        };
    }

    @Override
    protected ValuesExtractor<NumberWithUnit> getValuesExtractor() {
        // TODO add percent here
        return new NumberWithUnitExtractor(
                Arrays.asList(Unit.GRAM, Unit.MILLIMETER, Unit.CENTIMETER, Unit.WATT, Unit.KILOGRAM, Unit.LITER,
                        Unit.MILLIAMPER_HOURS, Unit.PERCENT, Unit.METER), false);
    }

    @Override
    public List<List<NumberWithUnit>> getExpectedValues() {
        return Arrays.asList(
                Arrays.asList(new NumberWithUnit(36, Unit.MILLIMETER), new NumberWithUnit(500, Unit.GRAM)),
                Arrays.asList(),
                Arrays.asList(new NumberWithUnit(2, Unit.MILLIMETER), new NumberWithUnit(40, Unit.CENTIMETER)),
                Arrays.asList(new NumberWithUnit(117.5f, Unit.MILLIMETER),
                        new NumberWithUnit(150, Unit.MILLIMETER),
                        new NumberWithUnit(150, Unit.MILLIMETER),
                        new NumberWithUnit(50, Unit.WATT),
                        new NumberWithUnit(0.73f, Unit.KILOGRAM)),
                Arrays.asList(new NumberWithUnit(2.7f, Unit.LITER)),
                Arrays.asList(new NumberWithUnit(5200, Unit.MILLIAMPER_HOURS)),
                Arrays.asList(new NumberWithUnit(25, Unit.PERCENT),
                        new NumberWithUnit(24, Unit.PERCENT),
                        new NumberWithUnit(51, Unit.PERCENT),
                        new NumberWithUnit(200, Unit.METER),
                        new NumberWithUnit(100, Unit.GRAM)));
    }

    @ParameterizedTest()
    @ArgumentsSource(NumberWithUnitExtractorTest.class)
    public void testExtractValues(String title, List<NumberWithUnit> expectedValues) {
        super.testExtractValues(title, expectedValues);
    }

    @Test
    public void testCalculateDelta() {
        Assertions.assertEquals(1, valuesExtractor.calculateDelta(
                new NumberWithUnit(3, Unit.GRAM),
                new NumberWithUnit(2, Unit.CENTIMETER)), EPS);
        Assertions.assertEquals(2f / 9, valuesExtractor.calculateDelta(
                new NumberWithUnit(8, Unit.CENTIMETER),
                new NumberWithUnit(10, Unit.CENTIMETER)), EPS);
        Assertions.assertEquals(0, valuesExtractor.calculateDelta(
                new NumberWithUnit(5, Unit.KILOGRAM),
                new NumberWithUnit(5, Unit.KILOGRAM)), EPS);
    }
}
