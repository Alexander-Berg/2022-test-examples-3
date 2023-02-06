package ru.yandex.market.deepmind.tracker_approver.utils;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link java.util.concurrent.ExecutorService} which runs in current thread.
 * Useful for unit tests.
 */
public class CurrentThreadExecutorService extends AbstractExecutorService implements ScheduledExecutorService {

    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @Override
    public void shutdown() {
        Thread.currentThread().interrupt();
    }

    @Override
    public List<Runnable> shutdownNow() {
        Thread.currentThread().interrupt();
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return Thread.currentThread().isInterrupted();
    }

    @Override
    public boolean isTerminated() {
        return Thread.currentThread().isInterrupted();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        command.run();
        return getScheduledFuture(null);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        V call;
        try {
            call = callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return getScheduledFuture(call);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        command.run();
        return getScheduledFuture(null);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        command.run();
        return getScheduledFuture(null);
    }

    private static <V> ScheduledFuture<V> getScheduledFuture(V result) {
        return new ScheduledFuture<V>() {
            @Override
            public long getDelay(TimeUnit unit) {
                return 0;
            }

            @Override
            public int compareTo(Delayed o) {
                return 0;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {
                return result;
            }

            @Override
            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                TimeoutException {
                return result;
            }
        };
    }
}
