package ru.yandex.market.metrics;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LogarithmicDistributionTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testRecord() throws Exception {
        LogarithmicDistribution distribution = new LogarithmicDistribution();
        long base = 1;
        for (int i = 0; i <= LogarithmicDistribution.DEFAULT_MAX_POWER; i++) {
            long number = base * (long) Math.pow(10, i);
            int k = i + 1;
            while (k-- > 0) {
                distribution.record(number);
            }
        }
        distribution.record(Long.MAX_VALUE);
        for (int i = 0; i < LogarithmicDistribution.DEFAULT_MAX_POWER; i++) {
            assertEquals(i + 1, distribution.count(i));
        }
        assertEquals(LogarithmicDistribution.DEFAULT_MAX_POWER + 2, distribution.count(LogarithmicDistribution.DEFAULT_MAX_POWER));
        System.out.println(distribution.print(new StringBuilder()));
    }

    @Test
    public void testSum() throws Exception {
        LogarithmicDistribution first = new LogarithmicDistribution();
        first.record(200);
        first.record(2);
        LogarithmicDistribution second = new LogarithmicDistribution();
        second.record(300);
        second.record(30_000);
        LogarithmicDistribution sum = first.sum(second);
        assertEquals(1, sum.count(0));
        assertEquals(0, sum.count(1));
        assertEquals(2, sum.count(2));
        assertEquals(0, sum.count(3));
        assertEquals(1, sum.count(4));
        for (int i = 5; i <= LogarithmicDistribution.DEFAULT_MAX_POWER; i++) {
            assertEquals(0, sum.count(i));
        }
    }

    @Test
    public void testEmptyPrint() throws Exception {
        assertEquals("<empty>", new LogarithmicDistribution().print(new StringBuilder()).toString());
    }

    @Test
    public void testPrint() throws Exception {
        LogarithmicDistribution distribution = new LogarithmicDistribution();
        distribution.record(717);
        distribution.record(7007);
        distribution.record(Long.MAX_VALUE);
        assertEquals("3>d/100", distribution.print(new StringBuilder(), Arrays.asList("d")).toString());
        assertEquals("1<k/33|1<m/66|1>m/34",
                distribution.print(new StringBuilder(), Arrays.asList("d", "h", "k", "m")).toString());
    }

    @Test
    public void testPrintLogBin() throws Exception {
        LogarithmicDistribution distribution = new LogarithmicDistribution(2, 0, 5);
        int[] data = {0, 0, 0, 1, 3, 2, 4, 5, 7};
        for (int i : data) {
            distribution.record(i);
        }
        assertEquals(4, distribution.count(0));
        assertEquals(2, distribution.count(1));
        assertEquals(3, distribution.count(2));
        assertEquals(0, distribution.count(3));
        assertEquals(0, distribution.count(4));
        assertEquals(0, distribution.count(5));
        distribution.record(Long.MAX_VALUE);
        assertEquals(1, distribution.count(5));
    }
}