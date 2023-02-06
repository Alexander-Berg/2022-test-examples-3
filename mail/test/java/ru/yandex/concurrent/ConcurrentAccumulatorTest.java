package ru.yandex.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class ConcurrentAccumulatorTest extends TestBase {
    public ConcurrentAccumulatorTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        final int threadsCount = 32;
        final int countTo = 10000000;
        final Object lock = new Object();
        final AtomicBoolean started = new AtomicBoolean();
        long[] zero = new long[] {0L, 0L};
        final ConcurrentAccumulator<long[]> accum =
            new ConcurrentAccumulator<>(
                (x, y) -> new long[] {x[0] + y[0], x[1] + y[1]},
                zero);
        Assert.assertSame(zero, accum.get());

        Thread[] threads = new Thread[threadsCount];
        for (int i = 0; i < threadsCount; ++i) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    synchronized (lock) {
                        while (!started.get()) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    for (int i = 0; i < countTo; ++i) {
                        accum.accept(new long[] {1L, i});
                    }
                }
            };
            threads[i].setDaemon(true);
        }
        synchronized (lock) {
            for (Thread thread: threads) {
                thread.start();
            }
            Thread.sleep(100L);
            started.set(true);
            lock.notifyAll();
        }
        for (Thread thread: threads) {
            thread.join();
        }
        long[] result = accum.get();
        Assert.assertEquals(threadsCount * countTo, result[0]);
        Assert.assertEquals(
            ((long) threadsCount) * countTo * (countTo - 1) / 2,
            result[1]);
        Assert.assertEquals(0L, zero[0]);
    }
}

