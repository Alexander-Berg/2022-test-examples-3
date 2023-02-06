package ru.yandex.travel.commons.concurrent;

import org.junit.AfterClass;
import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TerminationSemaphoreTests {
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void testEmpty() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        assertFalse(semaphore.hasTerminated());
    }

    @Test
    public void testCanAcquireWhileNotTerminated() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        assertTrue(semaphore.acquire());
        assertTrue(semaphore.acquire());
        assertTrue(semaphore.acquire());
        assertFalse(semaphore.hasTerminated());
    }

    @Test
    public void testCanNotAcquireWhileTerminated() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        assertTrue(semaphore.acquire());
        semaphore.shutdown();
        assertFalse(semaphore.acquire());
    }

    @Test
    public void testDoesNotTerminateWhileReleasingIfShutdownNotCalled() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        semaphore.acquire();
        semaphore.release();
        assertFalse(semaphore.hasTerminated());
    }

    @Test
    public void testDoesNotTerminateWhileShutdownIfRunningTasksArePresent() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        semaphore.acquire();
        semaphore.shutdown();
        assertFalse(semaphore.hasTerminated());
    }

    @Test
    public void testTerminatesOnShutdownWhenEmpty() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        semaphore.shutdown();
        assertTrue(semaphore.hasTerminated());
    }

    @Test
    public void testTerminatesOnShutdownWhenReleased() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        assertTrue(semaphore.acquire());
        semaphore.release();
        assertFalse(semaphore.hasTerminated());
        semaphore.shutdown();
        assertTrue(semaphore.hasTerminated());
    }

    @Test
    public void testTerminatesOnFinalReleaseWhenShutDown() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        semaphore.acquire();
        semaphore.acquire();
        semaphore.shutdown();
        semaphore.release();
        assertFalse(semaphore.hasTerminated());
        semaphore.release();
        assertTrue(semaphore.hasTerminated());
    }

    @Test(expected = IllegalStateException.class)
    public void testExceptionIfReleasedMoreThanAcquired() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        semaphore.acquire();
        semaphore.release();
        semaphore.release();
    }

    @Test(expected = IllegalStateException.class)
    public void testFailedAcquiringDoesNotIncrementSemaphore() {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        semaphore.acquire();
        semaphore.shutdown();
        assertFalse(semaphore.acquire());
        semaphore.release();
        semaphore.release();
    }

    @Test(expected = TimeoutException.class)
    public void testNonterminatedAwaitBlocks() throws InterruptedException, ExecutionException, TimeoutException {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                semaphore.awaitTermination();
            } catch (InterruptedException e) {
            }
        }).get(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testTerminatedAwaitDoesNotBlock() throws InterruptedException, ExecutionException {
        TerminationSemaphore semaphore = new TerminationSemaphore();
        semaphore.shutdown();
        executor.submit(() -> {
            try {
                semaphore.awaitTermination();
            } catch (InterruptedException e) {
            }
        }).get();
    }

    @AfterClass
    public static void closeExecutor() {
        executor.shutdownNow();
    }
}
