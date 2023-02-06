package ru.yandex.market.metrics;

import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.failover.FailoverTestUtils.between;

public class MinMaxAvgTest {
    @Test
    public void simpleTest() {
        MinMaxAvg minMaxAvg = new MinMaxAvg();

        minMaxAvg.add(1);
        minMaxAvg.add(2);
        minMaxAvg.add(3);

        assertEquals(3, minMaxAvg.getCount());
        assertEquals(1, minMaxAvg.getMin());
        assertEquals(2, minMaxAvg.getAvg());
        assertEquals(3, minMaxAvg.getMax());
    }

    @Test
    public void testBigValues() {
        MinMaxAvg minMaxAvg = new MinMaxAvg();

        minMaxAvg.add(Integer.MAX_VALUE / 2);
        minMaxAvg.add(Integer.MAX_VALUE / 2);
        minMaxAvg.add(0);

        assertEquals(3, minMaxAvg.getCount());
        assertEquals(0, minMaxAvg.getMin());
        assertTrue(format("%d is not in range [%d;%d]",
                minMaxAvg.getAvg(), Integer.MAX_VALUE / 3 - 1, Integer.MAX_VALUE / 3 + 1),
                between(minMaxAvg.getAvg(), Integer.MAX_VALUE / 3 - 1, Integer.MAX_VALUE / 3 + 1));
        assertEquals(Integer.MAX_VALUE / 2, minMaxAvg.getMax());
    }


    @Test
    public void testManyValues() {
        MinMaxAvg minMaxAvg = new MinMaxAvg();
        for (int i = 0; i <= 100; i++) {
            minMaxAvg.add(i);
        }

        assertEquals(101, minMaxAvg.getCount());
        assertEquals(0, minMaxAvg.getMin());
        assertEquals(50, minMaxAvg.getAvg());
        assertEquals(100, minMaxAvg.getMax());
    }

}