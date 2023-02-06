package ru.yandex.market.bidding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformSnapshot;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UpdateTest {
    private static final int MAX_SLEEP_MILLIS = 5;
    private static final int TEST_DURATION_SECONDS = 5;
    private static final int TEST_READ_COUNT = 5_000;

    @Test
    public void testSingleThread() throws Exception {
        final long sleepMillis = 1500;
        System.out.printf("This test will take about %d seconds.. Be patient!\n",
                TimeUnit.MILLISECONDS.toSeconds(sleepMillis * 2));
        Update.begin(true);
        assertEquals(1, Update.size());
        Update.end();
        assertEquals(0, Update.size());
        Update.begin(true);
        assertEquals(1, Update.size());
        final int eldest = Update.eldest();
        assertEquals(eldest, Update.eldest());
        Update.begin(true);
        assertEquals(eldest, Update.eldest());
        assertEquals(1, Update.size());
        ExecutorService es = Executors.newSingleThreadExecutor();
        final AtomicInteger tick = new AtomicInteger();
        Future<?> task = es.submit(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(sleepMillis);
                Update.begin(true);
                tick.set(Update.tick());
                assertEquals(eldest, Update.eldest());
                assertEquals(2, Update.size());
                //Update is intentionally not finished by Update.end
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted");
            }
        });
        task.get();
        TimeUnit.MILLISECONDS.sleep(sleepMillis);
        assertEquals(eldest, Update.eldest());
        assertTrue(Update.tick() < tick.get());
        Update.end();
        assertTrue(Update.tick() > tick.get());
        assertTrue(eldest < Update.eldest());
        assertEquals(1, Update.size());
        task = es.submit(() -> {
            assertEquals(Update.tick(), tick.get());
            Update.end();
            assertTrue(Update.tick() > tick.get());
            assertEquals(0, Update.size());
        });
        task.get();
        assertEquals(0, Update.size());
    }

    @Test
    public void testDoubleThread() throws Exception {
        testMultipleThread(2);
    }

    @Test
    public void testMultipleThread() throws Exception {
        testMultipleThread(Runtime.getRuntime().availableProcessors() * 2 - 1);
    }

    private void testMultipleThread(int nThreads) throws Exception {
        System.out.printf("This test will take about %d seconds.. Be patient!\n", TEST_READ_COUNT);
        Semaphore semaphore = new Semaphore(nThreads);
        semaphore.acquire(nThreads);
        ExecutorService es = Executors.newFixedThreadPool(nThreads, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        for (int i = 0; i < nThreads; i++) {
            es.submit(new Updater(semaphore));
        }
        Random random = new Random();
        Histogram histogram = new Histogram(new Reservoir() {
            List<Long> values = new ArrayList<>();

            @Override
            public int size() {
                return values.size();
            }

            @Override
            public void update(long value) {
                values.add(value);
            }

            @Override
            public Snapshot getSnapshot() {
                return new UniformSnapshot(values);
            }
        });
        int count = TEST_READ_COUNT;
        long micros = TimeUnit.SECONDS.toMicros(TEST_DURATION_SECONDS);
        int maxMicros = (int) micros / count;
        semaphore.acquire();
        while (count-- > 0) {
            histogram.update(Update.size());
            TimeUnit.MICROSECONDS.sleep(random.nextInt(maxMicros));
        }
        es.shutdownNow();
        es.awaitTermination(1, TimeUnit.SECONDS);
        int tick = Update.eldest();
        Snapshot snapshot = histogram.getSnapshot();
        System.out.printf("Threads=%d Mean=%.3f StdDev=%.3f Max=%d Min=%d\n",
                nThreads, snapshot.getMean(), snapshot.getStdDev(), snapshot.getMax(), snapshot.getMin());
        System.out.println("Last tick = " + tick);
        System.out.println("Ticks = " + Update.dump(new StringBuilder("[")).append("]").toString());
        assertEquals(0, Update.size());
    }

    private static class Updater implements Runnable {
        private final Random random = new Random();
        private final Semaphore semaphore;

        Updater(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void run() {
            semaphore.release();
            while (!Thread.currentThread().isInterrupted()) {
                Update.begin(true);
                try {
                    TimeUnit.MILLISECONDS.sleep(random.nextInt(MAX_SLEEP_MILLIS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    Update.end();
                }
            }
        }
    }
}