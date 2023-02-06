package ru.yandex.utils;

import org.junit.Test;

import static it.unimi.dsi.fastutil.ints.IntIterators.unwrap;
import static it.unimi.dsi.fastutil.ints.IntIterators.wrap;
import static org.junit.Assert.assertArrayEquals;
import static ru.yandex.utils.IntUtils.*;


public class IntUtilsTest {
    @Test
    public void testFilterMore() {
        assertArrayEquals(iarr(3, 4), unwrap(filter(wrap(iarr(1, 2, 3, 4)), moreA(2))));
        assertArrayEquals(iarr(1, 2, 3, 4), unwrap(filter(wrap(iarr(1, 2, 3, 4)), moreA(0))));
        assertArrayEquals(new int[]{}, unwrap(filter(wrap(iarr(1, 2, 3, 4)), moreA(6))));

        assertArrayEquals(iarr(4, 6, 7, 10), unwrap(filter(wrap(iarr(4, 2, 6, 7, 1, 0, -1, 10)), moreA(3))));
    }

    @Test
    public void testFilterLess() {
        assertArrayEquals(iarr(1), unwrap(filter(wrap(iarr(1, 2, 3, 4)), lessA(2))));
        assertArrayEquals(iarr(1, 2, 3, 4), unwrap(filter(wrap(iarr(1, 2, 3, 4)), lessA(6))));
        assertArrayEquals(new int[]{}, unwrap(filter(wrap(iarr(1, 2, 3, 4)), lessA(0))));

        assertArrayEquals(iarr(2, 1, 0, -1), unwrap(filter(wrap(iarr(4, 2, 6, 7, 1, 0, -1, 10)), lessA(4))));
    }
}
