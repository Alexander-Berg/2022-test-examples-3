package ru.yandex.canvas.service.color;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author skirsanov
 */
public class CIEDE2000ColorDistanceTest {

    private static final double DELTA = 0.00005;

    /**
     * @see <a href="http://www.ece.rochester.edu/~gsharma/ciede2000/ciede2000noteCRNA.pdf">The CIEDE2000 Color-Difference</a>
     */
    @Test
    public void testCIEDE2000Implementation() {
        //Pair #1
        assertEquals(2.0425,
                CIEDE2000ColorDistance.compute(
                        new double[]{50, 2.6772, -79.7751},
                        new double[]{50, 0, -82.7485}),
                DELTA);

        //Pair #7
        assertEquals(2.3669,
                CIEDE2000ColorDistance.compute(
                        new double[]{50, 0, 0},
                        new double[]{50, -1, 2}),
                DELTA);

        //Pair #17
        assertEquals(27.1492,
                CIEDE2000ColorDistance.compute(
                        new double[]{50, 2.5, 0},
                        new double[]{73, 25, -18}),
                DELTA);

        //Pair #21
        assertEquals(1,
                CIEDE2000ColorDistance.compute(
                        new double[]{50, 2.5, 0},
                        new double[]{50, 3.1736, 0.5854}),
                DELTA);

        //Pair #25
        assertEquals(1.2644,
                CIEDE2000ColorDistance.compute(
                        new double[]{60.2574, -34.0099, 36.2677},
                        new double[]{60.4626, -34.1751, 39.4387}),
                DELTA);
    }
}
