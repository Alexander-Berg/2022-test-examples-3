package ru.yandex.market.antifraud.runner;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by oroboros on 07.06.17.
 */
public class EternallyRetryingCallableTest {
    @Test
    public void mustRetry() throws InterruptedException {
        CountTask task = new CountTask();
        EternallyRetryingCallable rc = new EternallyRetryingCallable(
                task,
                "name",
                1,
                50
        );

        Executors.newSingleThreadExecutor().submit(rc);
        Thread.sleep(200);
        assertThat(task.runsCounter.get(), greaterThanOrEqualTo(2L));
    }

    @Test(timeout=5000) //ms
    public void mustBeInterruptable() throws InterruptedException, TimeoutException, ExecutionException {
        EternallyRetryingCallable rc = create(() -> {});

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Void> f = executorService.submit(rc);
        executorService.shutdownNow();
        executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
        assertTrue(executorService.isTerminated());
    }

    class CountTask implements Runnable {
        public AtomicLong runsCounter = new AtomicLong(0);

        @Override
        public void run() {
            runsCounter.incrementAndGet();
            throw new RuntimeException();
        }
    }

    private static EternallyRetryingCallable create(Runnable task) {
        return new EternallyRetryingCallable(
                task,
                "name",
                1,
                50
        );
    }
}
