package ru.yandex.market.mining.shared.models.measure;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 22.08.2013
 */
public class UnitTest {

    private Unit defaultUnit = new Unit("pixel", 1, 42, 1);
    private Unit unit = new Unit("Mp", 0.000001, 42, 2);
    private Measure measure = new Measure(42, 1);


    private Unit defaultUnit2 = new Unit("meter", 1, 42, 1);
    private Unit unit2 = new Unit("mm", 1000, 42, 2);
    private Measure measure2 = new Measure(42, 1);


    @Before
    public void setUp() throws Exception {
        defaultUnit.setMeasure(measure);
        unit.setMeasure(measure);
        measure.setUnits(Arrays.asList(unit, defaultUnit));
        measure.setDefaultUnit(defaultUnit);

        defaultUnit2.setMeasure(measure2);
        unit2.setMeasure(measure2);
        measure2.setUnits(Arrays.asList(unit2, defaultUnit2));
        measure2.setDefaultUnit(defaultUnit2);


    }

    @Test
    public void testConvertToDefault() throws Exception {
        Assert.assertEquals(560000, 0, unit.convertToDefault(0.56));
        Assert.assertEquals(800000, unit.convertToDefault(0.8), 0);

        Assert.assertEquals(0.011, unit2.convertToDefault(11.0), 0);

    }

    @Test
    public void testConvertFromDefault() throws Exception {
        Assert.assertEquals(0.56, unit.convertFromDefault(560000), 0);
        Assert.assertEquals(0.8, unit.convertFromDefault(800000), 0);

        Assert.assertEquals(11.0, unit2.convertFromDefault(0.011), 0);

    }

    @Test
    public void testRound() throws Exception {
        Assert.assertEquals(150000000, Unit.round(150000000), 0);
        Assert.assertEquals(2990000000000d, Unit.round(2990000000000d), 0);
        Assert.assertEquals(560000, Unit.round(560000.0000001), 0);
    }
}
