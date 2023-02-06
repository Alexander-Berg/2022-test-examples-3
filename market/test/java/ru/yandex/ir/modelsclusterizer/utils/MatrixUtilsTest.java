package ru.yandex.ir.modelsclusterizer.utils;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author mkrasnoperov
 */
public class MatrixUtilsTest {

    @Test
    public void inplaceDistanceToProbabilityTest() {
        double[][] distances = {{0.0, 1.0, 100}, {-1.0, -100, -1.0}};
        double[][] expected = {{0.5, 0.2689, 0.0}, {0.731, 1.0, 0.731}};
        MatrixUtils.inplaceDistanceToProbability(distances);
        for (int i = 0; i < distances.length; ++i) {
            for (int j = 0; j < distances[i].length; ++j) {
                assertTrue(Math.abs(distances[i][j] - expected[i][j]) < 1e-3);
            }
        }
    }

    @Test
    public void inplaceZeroDiagTest() {
        double[][] distances = {{1.0, 1.0}, {1.0, 1.0}};
        double[][] expected = {{0.0, 1.0}, {1.0, 0.0}};
        MatrixUtils.inplaceZeroDiag(distances);
        for (int i = 0; i < distances.length; ++i) {
            for (int j = 0; j < distances[i].length; ++j) {
                assertTrue(Math.abs(distances[i][j] - expected[i][j]) < 1e-9);
            }
        }
    }
}
