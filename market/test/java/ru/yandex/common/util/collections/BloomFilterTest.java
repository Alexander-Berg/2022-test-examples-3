package ru.yandex.common.util.collections;

import org.junit.Assert;
import org.junit.Test;

/**
 * Simple test of Bloom Filter
 *
 * @author Dima Schitinin <dimas@yandex-team.ru>
 */
public class BloomFilterTest {

    @Test
    public void simpleFilterTest() {
        final BloomFilter<Integer> filter =
                BloomFilter.newFilter(200, 2);
        for (Integer i = 0; i < 100; i++) {
            filter.add(i);
            Assert.assertTrue("Filter doesn't contain {" + i + "} after adding",
                    filter.contains(i));
        }
    }
}
