package ru.yandex.common.mining.aliaser.correlate;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.mining.property.PropertyInfo;
import ru.yandex.common.mining.propkinds.Converter;
import ru.yandex.common.mining.propkinds.PropertyKind;
import ru.yandex.common.mining.propkinds.Unit;
import ru.yandex.common.util.collections.nullable.NotNullPair;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.common.mining.aliaser.correlate.NumberCorrelator.findBestFactor;

/**
 * Created on 17:07:28 27.06.2008
 *
 * @author jkff
 */
public class NumberCorrelatorTest extends Assert {
    final static double DELTA = 0.00001;
    @Test
    public void testFindBestFactor() {
        assertEquals(1, findBestFactor(
            ar(2, 3, 4, 5, 6), ar(2, 3, 5, 5, 6)), DELTA);

        assertEquals(0.1, findBestFactor(
            ar(20, 30, 50, 50, 60), ar(2, 3, 4, 5, 6)), DELTA);
        assertEquals(10, findBestFactor(
            ar(2, 3, 4, 5, 6), ar(20, 30, 50, 50, 60)), DELTA);

        assertEquals(0.01, findBestFactor(
            ar(200, 300, 500, 500, 600), ar(2, 3, 4, 5, 6)), DELTA);
        assertEquals(0.01, findBestFactor(
            ar(200, 300, 5, 500, 600), ar(2, 3, 4, 5, 6)), DELTA);
        assertEquals(100, findBestFactor(
            ar(2, 3, 4, 5, 6), ar(200, 300, 5, 500, 600)), DELTA);

        assertEquals(0, findBestFactor(
            ar(200, 300, 5, 500, 600), ar(1, 2, 3, 4, 5)), DELTA);

        assertEquals(0, findBestFactor(
            ar(3, 7, 9, 2, 5, 6, 2, 5, 3), ar(1, 2, 3, 4, 5, 2, 3, 2, 1)), DELTA);

        assertEquals(1, findBestFactor(
            ar(64, 128, 1024, 2048, 65536), ar(64, 128, 16, 2048, 65536)), DELTA);
    }

    @Test
    public void testMakeConverter() {
        NumberCorrelator nc = new NumberCorrelator();
        PropertyInfo pi = new PropertyInfo("prop", PropertyKind.numeric(Unit.MASS));
        Converter c;

        c = nc.tryMakeConverter(vals(ar(1, 2, 3, 4, 5), ar(1, 2, 3, 40, 5)), pi, pi);
        assertNotNull(c);
        assertEquals(5, Double.parseDouble(c.convert("5")), DELTA);

        c = nc.tryMakeConverter(vals(ar(1, 20, 3, 4, 5), ar(10, 21, 30, 40, 50)), pi, pi);
        assertNotNull(c);
        assertEquals(30, Double.parseDouble(c.convert("3")), DELTA);


        assertNull(nc.tryMakeConverter(vals(ar(1, 20, 3, 4, 5), ar(3, 11, 20, 9, 43)), pi, pi));
        assertNull(nc.tryMakeConverter(vals(ar(3, 11, 20, 9, 43), ar(1, 20, 3, 4, 5)), pi, pi));
        assertNotNull(nc.tryMakeConverter(vals(ar(64, 128, 1024, 2048, 65536), ar(64, 128, 16, 2048, 65536)), pi, pi));
    }

    @Test
    public void testSimilarity() {
        NumberCorrelator nc = new NumberCorrelator();
        PropertyInfo pi = new PropertyInfo("prop", PropertyKind.numeric(Unit.MASS));

        assertTrue(0.9 < nc.valueSimilarity(vals(ar(1, 2, 3, 4, 5), ar(1, 2, 3, 4, 5)), pi));
        assertTrue(0.9 < nc.valueSimilarity(vals(ar(1, 2, 3, 4, 5), ar(1, 2, 3, 40, 5)), pi));
        assertTrue(0.1 > nc.valueSimilarity(vals(ar(10, 20, 30, 40, 50), ar(1, 2, 3, 40, 5)), pi));
        assertTrue(0.5 < nc.valueSimilarity(vals(ar(64, 128, 1024, 2048, 65536), ar(64, 128, 16, 2048, 65536)), pi));
    }

    private static List<NotNullPair<String, String>> vals(double[] xs, double[] ys) {
        List<NotNullPair<String, String>> res = new ArrayList<>();
        for (int i = 0; i < xs.length; ++i)
            res.add(new NotNullPair<String, String>(String.valueOf(xs[i]), String.valueOf(ys[i])));
        return res;
    }

    private static double[] ar(double... xs) {
        return xs;
    }
}
