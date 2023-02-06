package ru.yandex.ir.modelsclusterizer.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author mkrasnoperov
 */
public class SecondDoubleMinAccumulatorTest {

    @Test
    public void test() throws Exception {
        int length = 10;
        for (int seed = 0; seed < 200; ++seed) {
            Random rnd = new Random(seed);
            double[] numbers = new double[length];
            SecondDoubleMinAccumulator accumulator = new SecondDoubleMinAccumulator();
            assertTrue(Double.isNaN(accumulator.firstOrElse(() -> Double.NaN)));
            assertTrue(Double.isNaN(accumulator.secondOrElse(() -> Double.NaN)));
            for (int i = 0; i < length; ++i) {
                numbers[i] = rnd.nextDouble();
                accumulator.accept(numbers[i]);
                if (i == 0) {
                    assertEquals(accumulator.firstOrElse(() -> Double.NaN), numbers[0], 0);
                    assertEquals(numbers[0], accumulator.calculateAverage(() -> Double.NaN), 0);
                }
            }
            Arrays.sort(numbers);
            assertEquals(numbers[0], accumulator.firstOrElse(() -> Double.NaN), 0);
            assertEquals(numbers[1], accumulator.secondOrElse(() -> Double.NaN), 0);
            assertEquals((numbers[0] + numbers[1]) / 2, accumulator.calculateAverage(() -> Double.NaN), 0);
        }
    }
}
