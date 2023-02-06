package ru.yandex.travel.commons.concurrent;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class PausableTerminationSemaphoreTests {

    @Test
    public void testCanNotAcquireWhilePaused() {
        PausableTerminationSemaphore semaphore = new PausableTerminationSemaphore();
        assertThat(semaphore.acquire()).isTrue();
        semaphore.pause();
        assertThat(semaphore.acquire()).isFalse();
    }

    @Test
    public void testCanAcquireAfterResume() {
        PausableTerminationSemaphore semaphore = new PausableTerminationSemaphore();
        semaphore.pause();
        assertThat(semaphore.acquire()).isFalse();
        semaphore.resume();
        assertThat(semaphore.acquire()).isTrue();
    }

    @Test
    public void testIsActive() {
        PausableTerminationSemaphore semaphore = new PausableTerminationSemaphore();
        assertThat(semaphore.isActive()).isTrue();
        semaphore.pause();
        assertThat(semaphore.isActive()).isFalse();
        semaphore.resume();
        assertThat(semaphore.isActive()).isTrue();
        semaphore.pause();
        semaphore.shutdown();
        assertThat(semaphore.isActive()).isFalse();
    }
}
