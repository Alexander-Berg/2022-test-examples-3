package ru.yandex.canvas.service.color;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static ru.yandex.canvas.service.color.ColorRepresentation.RGBtoXYZ;
import static ru.yandex.canvas.service.color.ColorRepresentation.XYZtoLAB;
import static ru.yandex.canvas.service.color.ColorRepresentation.hexToRGB;

/**
 * @author skirsanov
 */
public class ColorRepresentationTest {
    private static final double DELTA = 0.01;

    @Test
    public void testRGBToXYZ() {

        assertArrayEquals(new double[]{12.112951, 12.7437680, 13.877963},
                RGBtoXYZ(new double[]{100, 100, 100}),
                DELTA);

        assertArrayEquals(new double[]{56.06, 50.49, 8.16},
                RGBtoXYZ(new double[]{255, 171, 32}),
                DELTA);
    }

    @Test
    public void testXYZtoLAB() {
        assertArrayEquals(new double[]{76.37, 21.18, 74.94},
                XYZtoLAB(new double[]{56.06, 50.49, 8.16}),
                DELTA);

        assertArrayEquals(new double[]{76.37, 21.18, 74.94},
                XYZtoLAB(new double[]{56.06, 50.49, 8.16}),
                DELTA);
    }

    @Test
    public void testHEXtoRGB() {
        assertArrayEquals(new double[]{1, 1, 1}, hexToRGB("#010101"), DELTA);

        assertArrayEquals(new double[]{0, 170, 187}, hexToRGB("#00AABB"), DELTA);
    }
}
