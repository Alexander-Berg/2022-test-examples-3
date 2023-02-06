package ru.yandex.market.olap2.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RetryTest {
    @Test(expected = RuntimeException.class)
    public void mustFail() {
        new Retry<Long>(2, 1).retry((ctx) -> { throw new RuntimeException("retry test must fail"); });
    }

    @Test
    public void mustReturn() {
        assertEquals(new Long(1L), new Retry<Long>(2, 1).retry((ctx) -> 1L));
    }

    @Test(expected = RuntimeException.class)
    public void mustNotReturnFail() {
        new Retry<Long>(2, 1).retryVoid((ctx) -> { throw new RuntimeException("retry test must fail"); });
    }

    @Test
    public void mustNotReturnRetry() {
        int times = 3;
        AtomicInteger i = new AtomicInteger(0);
        new Retry<Long>(times, 1).retryVoid((ctx) -> {
            if(times - i.incrementAndGet() > 1) {
                throw new RuntimeException("transient error");
            }
        });
    }

    @Test
    public void mustReturnExp() {
        assertEquals(new Long(1L), new ExponentialRetry<Long>(2, 1).retry((ctx) -> 1L));
    }

    @Test
    public void mustHaveProperCountsInContext() {
        Queue<Integer> tryCounts = new ConcurrentLinkedQueue<>();
        try {
            new Retry<Void>(3, 1).retryVoid((ctx) -> {
                tryCounts.add(ctx.getRetry());
                throw new RuntimeException();
            });
        } catch (RuntimeException e) {
            // ignore
        }

        assertEquals(1, (int) tryCounts.poll());
        assertEquals(2, (int) tryCounts.poll());
        assertEquals(3, (int) tryCounts.poll());
        assertNull(tryCounts.poll());
    }

    @Test
    public void mustHaveFirstRetry() {
        new Retry<Void>(3, 1).retryVoid((ctx) -> assertEquals(1, ctx.getRetry()));
    }
}
