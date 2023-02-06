package ru.yandex.ir.predictors.base;


import java.util.Collections;

import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LongFloatMaxKTrackerTest {
    @Test
    public void test() {
        LongFloatMaxKTracker tracker = new LongFloatMaxKTracker(3);
        for (int i = 0; i < 10; ++i) {
            tracker.add(i, i);
        }
        LongList result = tracker.getMaxK();
        Collections.sort(result);
        assertEquals(result.get(0), 7);
        assertEquals(result.get(1), 8);
        assertEquals(result.get(2), 9);
        System.out.println(result);
    }

    @Test
    public void testReversed() {
        LongFloatMaxKTracker tracker = new LongFloatMaxKTracker(3);
        for (int i = 9; i >= 0; --i) {
            tracker.add(i, i);
        }
        LongList result = tracker.getMaxK();
        Collections.sort(result);
        assertEquals(result.get(0), 7);
        assertEquals(result.get(1), 8);
        assertEquals(result.get(2), 9);
        System.out.println(result);
    }
}
