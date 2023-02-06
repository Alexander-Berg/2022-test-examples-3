package ru.yandex.market.mbo.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Interval Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>08/19/2008</pre>
 */
@SuppressWarnings("checkstyle:magicNumber")
public class IntervalTest {

    @Test
    public void testSize() {
        Interval i1 = Interval.interval(0, -1);
        Interval i2 = Interval.interval(3423, -243);
        assertEquals(0, i1.size());
        assertEquals(0, i2.size());

        Interval i3 = Interval.interval(0, 0);
        assertEquals(1, i3.size());

        Interval i4 = Interval.interval(1, 10);
        assertEquals(10, i4.size());

        Interval i5 = Interval.interval(-10, 10);
        assertEquals(21, i5.size());
    }

    @Test
    public void testIsEmpty() {
        Interval i = Interval.interval(0, -1);
        assertTrue(i.isEmpty());
        assertEquals(0, i.size());
    }

    @Test
    public void testSplit() {
        Interval i = Interval.interval(1, 10);
        List<Interval> intervals = i.split(3);
        assertEquals(4, intervals.size());
        assertEquals(3, intervals.get(0).size());
        assertEquals(3, intervals.get(1).size());
        assertEquals(3, intervals.get(2).size());
        assertEquals(1, intervals.get(3).size());

        Interval i1 = Interval.interval(-1, 1);
        assertEquals(3, i1.split(1).size());

        Interval i2 = Interval.interval(1, 10);
        List<Interval> split = i2.split(1000);
        assertEquals(1, split.size());
        assertEquals(11, split.get(0).size());
    }

    @Test
    public void testIterate() {
        Interval e = Interval.interval(0, -1);
        for (int l : e) {
            fail("Unexpected");
        }

        List<Integer> it = new ArrayList<Integer>();
        Interval i = Interval.interval(1, 10);
        for (int l : i) {
            it.add(l);
        }

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), it);
    }
}
