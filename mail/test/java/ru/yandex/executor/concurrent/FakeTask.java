package ru.yandex.executor.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

import ru.yandex.util.timesource.TimeSource;

public class FakeTask {
    private final int id;
    private final int retriesCount;
    private final long retryInterval;
    private final long executionTime;
    private final long availabilityDelay;
    private final long availableAt;
    private AtomicInteger currentRetry;

    public FakeTask(
        final int id,
        final int retriesCount,
        final long retryInterval,
        final long executionTime,
        final long availabilityDelay)
    {
        this.id = id;
        this.retriesCount = retriesCount;
        this.retryInterval = retryInterval;
        this.executionTime = executionTime;
        this.availabilityDelay = availabilityDelay;
        availableAt =
            TimeSource.INSTANCE.currentTimeMillis() + availabilityDelay;
        currentRetry = new AtomicInteger(0);
    }

    public FakeTask(final FakeTask task) {
        id = task.id;
        retriesCount = task.retriesCount;
        retryInterval = task.retryInterval;
        executionTime = task.executionTime;
        availabilityDelay = task.availabilityDelay;
        availableAt =
            TimeSource.INSTANCE.currentTimeMillis() + availabilityDelay;
        currentRetry = task.currentRetry;
    }

    public int id() {
        return id;
    }

    public int retriesCount() {
        return retriesCount;
    }

    public long retryInterval() {
        return retryInterval;
    }

    public long executionTime() {
        return executionTime;
    }

    public long availabilityDelay() {
        return availabilityDelay;
    }

    public long availableAt() {
        return availableAt;
    }

    public int currentRetry() {
        return currentRetry.get();
    }

    public void incrementRetry() {
        currentRetry.incrementAndGet();
    }

    @Override
    public String toString() {
        return "FakeTask(" + id + ')';
    }
}

