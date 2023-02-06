package ru.yandex.travel.task_processor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.AfterClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PausableTerminationSemaphoreTest {
    private static ExecutorService executor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .build()
    );

    private static final String SEMAPHORE_NAME = "TestSemaphore";

    @Test
    public void testEmpty() {
        PausableTerminationSemaphore semaphore = new PausableTerminationSemaphore(SEMAPHORE_NAME);
        assertThat(semaphore.isActive()).isFalse();
    }

    @Test
    public void testCanAcquireWhileNotTerminated() {
        PausableTerminationSemaphore semaphore = new PausableTerminationSemaphore(SEMAPHORE_NAME);
        assertThat(semaphore.acquire()).isFalse();
        semaphore.resume();
        assertThat(semaphore.acquire()).isTrue();
        assertThat(semaphore.isActive()).isTrue();
    }

    @Test
    public void testCanNotAcquireWhileTerminated() {
        PausableTerminationSemaphore semaphore = new PausableTerminationSemaphore(SEMAPHORE_NAME);
        semaphore.resume();
        assertThat(semaphore.acquire()).isTrue();
        semaphore.shutdown();
        assertThat(semaphore.acquire()).isFalse();
    }

    @Test
    public void testPausesSyncBlocks() throws InterruptedException, ExecutionException, TimeoutException {
        PausableTerminationSemaphore semaphore = new PausableTerminationSemaphore(SEMAPHORE_NAME);
        semaphore.resume();
        semaphore.acquire();
        CountDownLatch cdl = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                semaphore.pause();
                cdl.countDown();
            } catch (InterruptedException e) {
            }
        });
        boolean released = cdl.await(100L, TimeUnit.MILLISECONDS);
        assertThat(released).isFalse();
        semaphore.release();
        released = cdl.await(100L, TimeUnit.MILLISECONDS);
        assertThat(released).isTrue();
    }

    @AfterClass
    public static void closeExecutor() {
        executor.shutdownNow();
    }
}
