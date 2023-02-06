package ru.yandex.calendar.logic.update;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.calendar.test.generic.AbstractConfTest;

import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

public class DistributedSemaphoreTest extends AbstractConfTest {
    @Autowired
    private PgDistributedSemaphore distributedSemaphore;
    @Autowired
    private SemaphoreSettingsProvider semaphoreSettingsProvider;

    @Test
    public void shouldIncrementCounterWhileAcquire() {
        assertThat(distributedSemaphore.getCount("key")).isZero();
        try (var lock1 = distributedSemaphore.acquire("key", 5000, true)) {
            assertThat(distributedSemaphore.getCount("key")).isEqualTo(1);
            try (var lock2 = distributedSemaphore.acquire("key", 5000, true)) {
                assertThat(distributedSemaphore.getCount("key")).isEqualTo(2);
            }
        }
        assertThat(distributedSemaphore.getCount("key")).isZero();
    }

    @Test
    public void shouldCollectExpiredLocks() {
        distributedSemaphore.acquire("key", 0, false);
        assertThat(distributedSemaphore.getCount("key")).isZero();
    }

    @Test
    public void shouldIgnoreExpiredLockAfterRelease() {
        try (var lock = distributedSemaphore.acquire("key", 0, true)) {
            assertThat(distributedSemaphore.getCount("key")).isZero();
        }
        assertThat(distributedSemaphore.getCount("key")).isZero();
    }

    @Test
    public void shouldForceTryAcquireIfNoLimitPresent() {
        try (var lock = distributedSemaphore.forceTryAcquire("key")) {
            assertThat(semaphoreSettingsProvider.getSettings("key")).isEmpty();
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfForceTryAcquireUnderLimit() {
        semaphoreSettingsProvider.setSettings("key", false, 1, OptionalLong.of(0), 5000);
        distributedSemaphore.tryAcquire("key");
        distributedSemaphore.forceTryAcquire("key");
    }

    @Test
    public void shouldAcquireIfNoLimit() {
        semaphoreSettingsProvider.setNoLimit("key");
        try (var lock = distributedSemaphore.forceTryAcquire("key")) {
        }
    }

    @Test
    public void shouldAcquireAndReleaseLock() {
        semaphoreSettingsProvider.setSettings("key2", false, 1, OptionalLong.of(0), 1000);
        try (var lock1 = distributedSemaphore.forceTryAcquire("key2")) {
        }
        try (var lock2 = distributedSemaphore.forceTryAcquire("key2")) {
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfLockIsBusy() {
        semaphoreSettingsProvider.setSettings("key", false, 0, OptionalLong.of(0), 1000);
        distributedSemaphore.forceTryAcquire("key");
    }

    @Test
    public void shouldNotLockMoreThanLimit() {
        semaphoreSettingsProvider.setSettings("key", false, 2, OptionalLong.of(100), 5000);
        try (var lock1 = distributedSemaphore.forceTryAcquire("key")) {
            try (var lock2 = distributedSemaphore.forceTryAcquire("key")) {
                assertThat(distributedSemaphore.tryAcquire("key")).isEmpty();
            }
        }
    }

    @Test
    public void shouldInfiniteWaitIfNoWaitTimeout() throws InterruptedException {
        semaphoreSettingsProvider.setSettings("inf", false, 0, OptionalLong.empty(), 3000);
        var t = new Thread(() -> distributedSemaphore.tryAcquire("inf"));
        t.start();
        Thread.sleep(1000);
        assertThat(t.isAlive()).isTrue();
        t.interrupt();
    }

    @Test
    public void shouldNotIncrementCountIfNoLimit() {
        semaphoreSettingsProvider.setNoLimit("absent");
        distributedSemaphore.forceTryAcquire("absent");
        assertThat(distributedSemaphore.getCount("absent")).isZero();
    }

    @Test
    public void shouldNotReleaseInRpsLimiterMode() throws InterruptedException {
        semaphoreSettingsProvider.setSettings("rps", true, 2, OptionalLong.of(0), 3000);
        try (var lock = distributedSemaphore.forceTryAcquire("rps")) {
        }
        try (var lock = distributedSemaphore.forceTryAcquire("rps")) {
        }
        assertThat(distributedSemaphore.getCount("rps")).isEqualTo(2);
        Thread.sleep(3000);
        assertThat(distributedSemaphore.getCount("rps")).isEqualTo(0);
    }
}
