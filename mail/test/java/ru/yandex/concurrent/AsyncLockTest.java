package ru.yandex.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AsyncLockTest extends TestBase {
    private static final long SLEEP = 2000L;
    private static final String LOCK_NAME = "lock";

    public AsyncLockTest() {
        super(false, 0L);
    }

    private static void sleep() {
        // CSOFF: EmptyBlock
        try {
            Thread.sleep(SLEEP);
        } catch (InterruptedException e) {
        }
        // CSON: EmptyBlock
    }

    @Test
    public void test() throws Exception {
        final LockStorage<String, AsyncLock> storage = new LockStorage<>();
        final Queue<Integer> queue = new ConcurrentLinkedQueue<>();
        Thread first = new Thread() {
            @Override
            public void run() {
                final AsyncLock lock =
                    storage.acquire(LOCK_NAME, new AsyncLock());
                lock.lock(
                    new Runnable() {
                        @Override
                        public void run() {
                            queue.add(1);
                            AsyncLockTest.sleep();
                            queue.add(2);
                            lock.unlock();
                        }
                    });
                storage.release(LOCK_NAME);
            }
        };
        final AtomicLong timeElapsed = new AtomicLong();
        Thread second = new Thread() {
            @Override
            public void run() {
                final AsyncLock lock =
                    storage.acquire(LOCK_NAME, new AsyncLock());
                long now = System.currentTimeMillis();
                lock.lock(
                    new Runnable() {
                        @Override
                        public void run() {
                            queue.add(2 + 1);
                            AsyncLockTest.sleep();
                            queue.add(2 + 2);
                            lock.unlock();
                        }
                    });
                lock.lock(
                    new Runnable() {
                        @Override
                        public void run() {
                            queue.add(2 + 2 + 1);
                            lock.unlock();
                        }
                    });
                timeElapsed.set(System.currentTimeMillis() - now);
                storage.release(LOCK_NAME);
            }
        };
        first.start();
        Thread.sleep(SLEEP >> 1);
        second.start();
        Assert.assertEquals(
            Collections.singletonList(1),
            new ArrayList<>(queue));
        Thread.sleep(SLEEP);
        Assert.assertEquals(
            Arrays.asList(1, 2, 2 + 1),
            new ArrayList<>(queue));
        Thread.sleep(SLEEP);
        Assert.assertEquals(
            Arrays.asList(1, 2, 2 + 1, 2 + 2, 2 + 2 + 1),
            new ArrayList<>(queue));
        YandexAssert.assertLess(SLEEP >> 2, timeElapsed.get());
    }
}

