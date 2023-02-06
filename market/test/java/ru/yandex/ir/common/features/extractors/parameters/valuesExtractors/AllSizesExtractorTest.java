package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.NumberWithUnitExtractor.NumberWithUnit;

import java.util.Arrays;
import java.util.List;

public class AllSizesExtractorTest extends AbstractValuesExtractorTest {
    private static final double EPS = 1e-5;

    @Override
    protected String[] getTitles() {
        return new String[]{
                "22*31 / 66x 42",
                "22*31 / 66x 42, высота 24см",
                "Противень Демидово МТ043 средний алюминиевый\"",
                "Размеры: 320х350х36мм<br />\\nсредний<br />\\nвес - 500г",
                "Противень 320*350*48мм средний МТ-043",
                "Противень 8023809343",
                "Спицы круговые 2мм/40см, Prym, 211200",
                "Характеристики: Высота, мм: 117.5; Длина, мм: 150; Ширина, мм: 150; Мощность, эквивалентная лампам накаливания, Вт: 50; Вес, кг: 0,73; Степень защиты, IP: 54;",
                "Набор для вышивания крестом Dimensions \"Вид на кафе\", 18x13 см, арт. 65093",
                "45 смлово на см",
                "словом: 45"
        };
    }

    @Override
    protected List<List> getExpectedValues() {
        return Arrays.asList(
                Arrays.asList(new NumberWithUnit(22, null), new NumberWithUnit(31f, null),
                        new NumberWithUnit(66f, null), new NumberWithUnit(42f, null)),
                Arrays.asList(new NumberWithUnit(22, null), new NumberWithUnit(31f, null),
                        new NumberWithUnit(66f, null), new NumberWithUnit(42f, null), new NumberWithUnit(24, Unit.CENTIMETER)),
                Arrays.asList(),
                Arrays.asList(new NumberWithUnit(320f, Unit.MILLIMETER), new NumberWithUnit(350f, Unit.MILLIMETER), new NumberWithUnit(36f, Unit.MILLIMETER)),
                Arrays.asList(new NumberWithUnit(320f, Unit.MILLIMETER), new NumberWithUnit(350f, Unit.MILLIMETER), new NumberWithUnit(48f, Unit.MILLIMETER)),
                Arrays.asList(),
                Arrays.asList(new NumberWithUnit(2f, Unit.MILLIMETER), new NumberWithUnit(40f, Unit.CENTIMETER)),
                Arrays.asList(new NumberWithUnit(117.5f, Unit.MILLIMETER), new NumberWithUnit(150f, Unit.MILLIMETER), new NumberWithUnit(150f, Unit.MILLIMETER)),
                Arrays.asList(new NumberWithUnit(18f, Unit.CENTIMETER), new NumberWithUnit(13f, Unit.CENTIMETER)),
                Arrays.asList(),
                Arrays.asList());
    }

    @Override
    protected ValuesExtractor getValuesExtractor() {
        return new AllSizesExtractor();
    }

    @ParameterizedTest()
    @ArgumentsSource(AllSizesExtractorTest.class)
    public void testExtractSizes(String title, List<NumberWithUnit> expectedValues) {
        super.testExtractValues(title, expectedValues);
    }

    @Test
    public void testCalculateDelta() {
        Assertions.assertEquals(1 / 2.5f, valuesExtractor.calculateDelta(new NumberWithUnit(3, null), new NumberWithUnit(2, Unit.CENTIMETER)), EPS);
        Assertions.assertEquals(2f / 9, valuesExtractor.calculateDelta(new NumberWithUnit(8, Unit.CENTIMETER), new NumberWithUnit(10, Unit.CENTIMETER)), EPS);
        Assertions.assertEquals(0, valuesExtractor.calculateDelta(new NumberWithUnit(5, Unit.METER), new NumberWithUnit(5, Unit.METER)), EPS);
        Assertions.assertEquals(0, valuesExtractor.calculateDelta(new NumberWithUnit(500, Unit.MILLIMETER), new NumberWithUnit(50, Unit.CENTIMETER)), EPS);
    }
}
