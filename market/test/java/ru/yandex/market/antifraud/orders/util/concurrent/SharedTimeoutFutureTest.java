package ru.yandex.market.antifraud.orders.util.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class SharedTimeoutFutureTest {

    @Test
    public void get() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        Future<String> future = new SharedTimeoutFuture<>(executorService.submit(() -> {
            lock.lock();
            lock.unlock();
            return "ok";
        }));
        Long start1 = System.nanoTime();
        try {
            future.get(100L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // ignored
        }
        Long finish1 = System.nanoTime();
        assertThat(TimeUnit.NANOSECONDS.toMillis(finish1 - start1)).isGreaterThanOrEqualTo(100L);

        Long start2 = System.nanoTime();
        try {
            future.get(100L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // ignored
        } finally {
            lock.unlock();
        }
        Long finish2 = System.nanoTime();
        assertThat(TimeUnit.NANOSECONDS.toMillis(finish2 - start2)).isLessThan(100L);
        assertThat(future.get()).isEqualTo("ok");
    }

}
