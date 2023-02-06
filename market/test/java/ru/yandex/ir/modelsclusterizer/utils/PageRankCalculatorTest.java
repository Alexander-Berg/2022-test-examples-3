package ru.yandex.ir.modelsclusterizer.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author mkrasnoperov
 */
public class PageRankCalculatorTest {
    @Test
    public void calculateRanksTest() {
        double[][] matrix = {
                {0.0, 0.95, 0.15, 0.1},
                {0.9, 0.0, 0.9, 0.9},
                {0.15, 0.9, 0.0, 0.1},
                {0.1, 0.9, 0.1, 0.0}
        };
        int[] order = new PageRankCalculator().calculatePriorityOrder(matrix);
        int[] expected = {1, 0, 2, 3};
        assertArrayEquals(expected, order);
    }
}
