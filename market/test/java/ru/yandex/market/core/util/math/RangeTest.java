package ru.yandex.market.core.util.math;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
public class RangeTest {
    private static <T extends Comparable<? super T>> void assertIntersects(Range<T> range1, Range<T> range2) {
        Assert.assertTrue(range1.intersectsWith(range2));
        Assert.assertTrue(range2.intersectsWith(range1));
    }

    private static <T extends Comparable<? super T>> void assertNotIntersects(Range<T> range1, Range<T> range2) {
        Assert.assertFalse(range1.intersectsWith(range2));
        Assert.assertFalse(range2.intersectsWith(range1));
    }

    @Test
    public void testUnlimitedIntersection() {
        assertIntersects(Range.<Integer>unlimited(), Range.unlimited());
        assertIntersects(Range.unlimited(), Range.inclusive(1, 2));
        assertIntersects(Range.unlimited(), Range.upFrom(1));
        assertIntersects(Range.unlimited(), Range.upTo(5));
    }

    @Test
    public void testSimpleIntersection() {
        assertIntersects(Range.inclusive(1, 3), Range.inclusive(2, 4));
        assertNotIntersects(Range.inclusive(1, 3), Range.inclusive(4, 6));
    }

    @Test
    public void testDifferentDirectionIntersection() {
        assertIntersects(Range.upFrom(1), Range.upTo(4));
        assertNotIntersects(Range.upFrom(4), Range.upTo(1));
    }

    @Test
    public void testSameDirectionIntersection() {
        assertIntersects(Range.upFrom(1), Range.upFrom(4));
        assertIntersects(Range.upTo(1), Range.upTo(4));
    }

    @Test
    public void testDifferentDirectionAndSimpleIntersection() {
        assertIntersects(Range.upFrom(1), Range.inclusive(0, 2));
        assertIntersects(Range.upFrom(1), Range.inclusive(2, 4));
        assertNotIntersects(Range.upFrom(1), Range.inclusive(-1, 0));

        assertIntersects(Range.upTo(4), Range.inclusive(0, 1));
        assertIntersects(Range.upTo(4), Range.inclusive(3, 5));
        assertNotIntersects(Range.upTo(4), Range.inclusive(5, 7));
    }
}
