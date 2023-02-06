package ru.yandex.market.ir.tms.logs.saasoffers;

import java.util.Collections;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author apluhin
 * @created 9/14/21
 */
public class SampleHelperTest {

    long countInCategory;
    long threshold = 100_000L;
    SampleHelper sampleHelper;
    Random random = new Random();

    @Before
    public void setUp() throws Exception {
        countInCategory = 1_000_000L;
        sampleHelper = new SampleHelper(Collections.singletonMap(1L, countInCategory), threshold);
    }

    @Test
    public void testSampling() {
        long countInCategory = 1_000_000L;
        long threshold = 100_000L;
        long count = IntStream.range(0, (int) countInCategory)
            .boxed()
            .filter(it -> !sampleHelper.skipModel(1L, random))
            .count();
        Assert.assertTrue(Math.abs(count - threshold) < threshold * 0.1);
    }

    @Test
    public void testIgnoreCategory() {
        sampleHelper.ignoreCategory(1L);
        long count = IntStream.range(0, (int) countInCategory)
            .boxed()
            .filter(it -> !sampleHelper.skipModel(1L, random))
            .count();
        Assert.assertEquals(countInCategory, count);
    }

    @Test
    public void testCustomFactor() {
        long expectedCount = 500_000L;
        sampleHelper.setExpectedCountForCategory(1L, 500_000L);
        long count = IntStream.range(0, (int) countInCategory)
            .boxed()
            .filter(it -> sampleHelper.skipModel(1L, random))
            .count();
        Assert.assertTrue(Math.abs(count - expectedCount) < expectedCount * 0.1);
    }

}
