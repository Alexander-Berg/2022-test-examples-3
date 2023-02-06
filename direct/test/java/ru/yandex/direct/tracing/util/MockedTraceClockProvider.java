package ru.yandex.direct.tracing.util;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation that may be used for mocking
 */
public final class MockedTraceClockProvider implements TraceClockProvider {
    private final long startEpochSecond;
    private final long startNanoTime;
    private final AtomicLong elapsed = new AtomicLong();

    public MockedTraceClockProvider() {
        this(System.currentTimeMillis() / 1000);
    }

    public MockedTraceClockProvider(long startEpochSeconds) {
        this.startEpochSecond = startEpochSeconds;
        this.startNanoTime = System.nanoTime();
    }

    @Override
    public long nanoTime() {
        return startNanoTime + elapsed.get();
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochSecond(startEpochSecond, elapsed.get());
    }

    public void advance(long duration, TimeUnit unit) {
        elapsed.getAndAdd(unit.toNanos(duration));
    }
}
