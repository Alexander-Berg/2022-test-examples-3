package ru.yandex.market.monitoring.thread.pool;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.util.concurrent.Callables;
import org.junit.jupiter.api.Test;

import ru.yandex.market.monitoring.thread.pool.inspectors.PoolInspector;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


class PoolInspectorTest {

    private static <T> Future<T> submit(ExecutorService service,
                                        AtomicInteger futureCalled,
                                        Callable<T> callable) {
        final Future<T> future = service.submit(() -> {
            futureCalled.incrementAndGet();
            return callable.call();
        });
        return future;
    }

    @SuppressWarnings("squid:S2925")
    private static void awaitEquals(int expected, AtomicInteger actual, int iterations) {
        int count = 0;
        while (count < iterations && expected != actual.get()) {
            try {
                Thread.sleep(100);
                count++;
            } catch (InterruptedException ignored) {
            }
        }

        assertTrue(count < iterations,
                expected + " != " + actual + " (Failed after " + iterations + " iterations)");
    }

    @SuppressWarnings("PackageVisibleField")
    private static class CallStat implements PoolInspector {

        final AtomicInteger create = new AtomicInteger();
        final AtomicInteger before = new AtomicInteger();
        final AtomicInteger after = new AtomicInteger();
        final AtomicInteger terminated = new AtomicInteger();
        final AtomicInteger reject = new AtomicInteger();
        final AtomicInteger finalReject = new AtomicInteger();
        final AtomicInteger finalAccept = new AtomicInteger();

        @Override
        public void onCreated(ThreadPoolExecutor pool) {
            create.incrementAndGet();
        }

        @Override
        public void onTerminated(ThreadPoolExecutor pool) {
            terminated.incrementAndGet();
        }

        @Override
        public void onInitialRejection(ThreadPoolExecutor pool, Runnable task) {
            reject.incrementAndGet();
        }

        @Override
        public void onFinalAccept(ThreadPoolExecutor pool, Runnable task) {
            finalAccept.incrementAndGet();
        }

        @Override
        public void onFinalRejection(ThreadPoolExecutor pool, Runnable task) {
            finalReject.incrementAndGet();
        }

        @Override
        public void afterExecute(ThreadPoolExecutor executor, Runnable r, Throwable t) {
            after.incrementAndGet();
        }

        @Override
        public void beforeExecute(ThreadPoolExecutor executor, Thread t, Runnable r) {
            before.incrementAndGet();
        }
    }

    @Test
    void pool() {
        final CallStat callStat = new CallStat();
        final ExecutorService executor = InstrumentedBuilders
                .threadPool("test")
                .addInspector(callStat)
                .build();

        final AtomicInteger future1Called = new AtomicInteger();
        final AtomicInteger future2Called = new AtomicInteger();
        {
            final AtomicReference<Throwable> exception = new AtomicReference<>();
            final Future<Boolean> future = submit(executor, future1Called, Callables.returning(true));
            awaitEquals(1, future1Called, 100);
            try {
                assertTrue(future.get());
            } catch (InterruptedException | ExecutionException e) {
                exception.set(e);
            }
            assertNull(exception.get());
        }

        {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            final AtomicReference<Boolean> value = new AtomicReference<>();
            final AtomicReference<Throwable> exception = new AtomicReference<>();
            final Future<Boolean> future = submit(executor, future2Called, () -> {
                countDownLatch.await();
                return true;
            });
            awaitEquals(1, future2Called, 100);
            future.cancel(true);
            try {
                value.set(future.get());
                fail();
            } catch (CancellationException | ExecutionException | InterruptedException ignored) {
                exception.set(ignored);
            }
            assertTrue(exception.get() instanceof IllegalStateException);
            assertNull(value.get());
        }

        awaitEquals(2, callStat.before, 100);
        awaitEquals(2, callStat.after, 100);
        awaitEquals(1, callStat.create, 100);
        awaitEquals(1, future1Called, 100);

        awaitEquals(1, future2Called, 100);

        executor.shutdown();
        executor.shutdownNow();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        awaitEquals(1, callStat.terminated, 100);
    }

    @Test
    void scheduledPool() throws ExecutionException, InterruptedException {
        final CallStat callStat = new CallStat();
        final ExecutorService executor =
                InstrumentedBuilders
                        .scheduledThreadPool("test")
                        .addInspector(callStat)
                        .build();

        final AtomicInteger future1Called = new AtomicInteger();
        final AtomicInteger future2Called = new AtomicInteger();
        {
            final Future<Boolean> future = submit(executor, future1Called, Callables.returning(true));
            awaitEquals(1, future1Called, 100);
            assertTrue(future.get());
        }

        {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            final AtomicReference<Boolean> value = new AtomicReference<>();
            final AtomicReference<Throwable> exception = new AtomicReference<>();
            final Future<Boolean> future = submit(executor, future2Called, () -> {
                countDownLatch.await();
                return true;
            });
            awaitEquals(1, future2Called, 100);
            future.cancel(true);
            try {
                value.set(future.get());
                fail();
            } catch (CancellationException e) {
                exception.set(e);
            }
            assertTrue(exception.get() instanceof IllegalStateException);
            assertNull(value.get());
        }

        awaitEquals(2, callStat.before, 100);
        awaitEquals(2, callStat.after, 100);
        awaitEquals(1, callStat.create, 100);
        awaitEquals(1, future1Called, 100);
        awaitEquals(1, future2Called, 100);

        executor.shutdown();
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        awaitEquals(1, callStat.terminated, 100);
    }
}
