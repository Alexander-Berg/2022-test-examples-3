package ru.yandex.common.util.collections;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Comparator;

/**
 * Test for {@link ru.yandex.common.util.functional.Comparators}.
 * @author maxkar
 *
 */
public class ComparatorsTest {

    /**
     * Enum to test comparator.
     * @author maxkar
     *
     */
    static enum TerribleEnum {
        /**
         * Fist element of the enum.
         */
        FIRST,

        /**
         * Second element of the enum.
         */
        SECOND,
    }

    static class AwfulComparator implements Comparator<TerribleEnum> {
        public int compare(TerribleEnum o1, TerribleEnum o2) {
            if (o1 == null || o2 == null)
                throw new NullPointerException();
            if (o1 == o2)
                return 0;
            return o1 == TerribleEnum.FIRST ? Integer.MAX_VALUE : Integer.MIN_VALUE ;
        }
    }

    /**
     * Tests, that reverse comparator works correctly.
     */
    @Test
    public void testReverseComparator() {
        final Comparator<TerribleEnum> orig = new AwfulComparator();
        Assert.assertEquals(0, orig.compare(TerribleEnum.FIRST, TerribleEnum.FIRST));
        Assert.assertEquals(0, orig.compare(TerribleEnum.SECOND, TerribleEnum.SECOND));
        Assert.assertTrue(orig.compare(TerribleEnum.FIRST, TerribleEnum.SECOND) > 0);
        Assert.assertTrue(orig.compare(TerribleEnum.SECOND, TerribleEnum.FIRST) < 0);
    }
}
