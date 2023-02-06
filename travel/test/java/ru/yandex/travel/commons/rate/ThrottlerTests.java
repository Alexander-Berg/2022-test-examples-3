package ru.yandex.travel.commons.rate;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;


public class ThrottlerTests {
    @Test
    public void testRateLimit() {
        Throttler throttler = new Throttler(3, Long.MAX_VALUE, Duration.ofMillis(1000), Duration.ofMillis(5000));
        long nowMs = 0;
        for (int step = 1; step <= 25; ++step) {
            for (int i = 0; i < step; ++i) {
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 10));
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 20));
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 30));
                assertEquals(Throttler.EDecision.RATE_LIMIT, throttler.acquire(nowMs + 40));
                throttler.release();
                assertEquals(Throttler.EDecision.RATE_LIMIT, throttler.acquire(nowMs + 50));
                throttler.release();
                throttler.release();
                nowMs += 1000 * step;
            }
        }
    }

    @Test
    public void testSemaphoreLimit() {
        Throttler throttler = new Throttler(Long.MAX_VALUE, 3, Duration.ofMillis(1000), Duration.ofMillis(5000));
        long nowMs = 0;
        for (int step = 1; step <= 25; ++step) {
            for (int i = 0; i < step; ++i) {
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 10));
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 20));
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 30));
                assertEquals(Throttler.EDecision.CONCURRENCY_LIMIT, throttler.acquire(nowMs + 40));
                throttler.release();
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 50));
                assertEquals(Throttler.EDecision.CONCURRENCY_LIMIT, throttler.acquire(nowMs + 60));
                throttler.release();
                throttler.release();
                throttler.release();
                nowMs += 1000 * step;
            }
        }
    }

    @Test
    public void testCombined() {
        Throttler throttler = new Throttler(3, 2, Duration.ofMillis(1000), Duration.ofMillis(5000));
        long nowMs = 0;
        for (int step = 1; step <= 25; ++step) {
            for (int i = 0; i < step; ++i) {
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 10));
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 20));
                assertEquals(Throttler.EDecision.CONCURRENCY_LIMIT, throttler.acquire(nowMs + 30));
                throttler.release();
                assertEquals(Throttler.EDecision.PASS, throttler.acquire(nowMs + 40));
                assertEquals(Throttler.EDecision.CONCURRENCY_LIMIT, throttler.acquire(nowMs + 50));
                throttler.release();
                assertEquals(Throttler.EDecision.RATE_LIMIT, throttler.acquire(nowMs + 60));
                throttler.release();
                nowMs += 1000 * step;
            }
        }
    }
}
