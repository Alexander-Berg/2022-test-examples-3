package ru.yandex.market.markup2.utils;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by alex-pekin on 31.01.2017.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class BatchProcessingTest extends TestCase {
    public void testBatch() {
        final AtomicInteger sum = new AtomicInteger(0);
        final AtomicInteger batches = new AtomicInteger(0);
        BatchProcessing<Integer> batchProcessing = new BatchProcessing<Integer>(15) {
            @Override
            public void process(List<Integer> items) {
                items.stream().forEach(i -> sum.addAndGet(i));
                batches.incrementAndGet();
            }
        };

        IntStream.range(1, 101).forEach(
            i -> batchProcessing.add(i));

        batchProcessing.finish();

        Assert.assertEquals(7, batches.get());
        Assert.assertEquals((1 + 100) * 100 / 2, sum.get());
    }
}
