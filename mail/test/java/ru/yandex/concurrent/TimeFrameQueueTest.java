package ru.yandex.concurrent;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.collection.Iterators;
import ru.yandex.test.util.TestBase;

public class TimeFrameQueueTest extends TestBase {
    private static final long TIMEOUT = 4000L;

    public TimeFrameQueueTest() {
        super(false, 0L);
    }

    // CSOFF: MagicNumber
    @Test
    public void test() throws Exception {
        TimeFrameQueue<Integer> queue = new TimeFrameQueue<>(TIMEOUT);
        queue.accept(5);
        queue.accept(3);
        Assert.assertEquals(
            Arrays.asList(5, 3),
            Iterators.toList(queue.iterator()));
        Thread.sleep(TIMEOUT >> 2L);
        queue.accept(4);
        queue.accept(2);
        Assert.assertEquals(
            Arrays.asList(5, 3, 4, 2),
            Iterators.toList(queue.iterator()));
        Thread.sleep(TIMEOUT >> 2L);
        queue.accept(1);
        Assert.assertEquals(
            Arrays.asList(5, 3, 4, 2, 1),
            Iterators.toList(queue.iterator()));
        Thread.sleep((TIMEOUT >> 1L) + (TIMEOUT >> 3L));
        Assert.assertEquals(
            Arrays.asList(4, 2, 1),
            Iterators.toList(queue.iterator()));
        Thread.sleep(TIMEOUT >> 2L);
        Assert.assertEquals(
            Arrays.asList(1),
            Iterators.toList(queue.iterator()));
        queue.accept(5);
        queue.accept(6);
        queue.accept(7);
        Thread.sleep(TIMEOUT >> 2L);
        Assert.assertEquals(
            Arrays.asList(5, 6, 7),
            Iterators.toList(queue.iterator()));
        Thread.sleep(TIMEOUT);
        queue.accept(8);
        Assert.assertEquals(
            Arrays.asList(8),
            Iterators.toList(queue.iterator()));
    }
    // CSON: MagicNumber

    @Test
    public void testMultiThreading() throws Exception {
        final TimeFrameQueue<Integer> queue = new TimeFrameQueue<>(TIMEOUT);
        queue.accept(1);
        Thread thread = new Thread() {
            @Override
            public void run() {
                queue.accept(2);
            }
        };
        thread.start();
        thread.join();
        Assert.assertEquals(
            Arrays.asList(1, 2),
            Iterators.toList(queue.iterator()));
        Thread.sleep(TIMEOUT + (TIMEOUT >> 2L));
        Assert.assertEquals(
            Collections.emptyList(),
            Iterators.toList(queue.iterator()));
        Thread thread2 = new Thread() {
            @Override
            public void run() {
                queue.accept(1);
            }
        };
        thread2.start();
        thread2.join();
        queue.accept(2);
        Assert.assertEquals(
            Arrays.asList(2, 1),
            Iterators.toList(queue.iterator()));
    }
}

