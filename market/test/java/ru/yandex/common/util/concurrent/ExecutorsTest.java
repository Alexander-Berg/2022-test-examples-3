package ru.yandex.common.util.concurrent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.util.concurrent.Futures;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorsTest {
    private static final int SIZE = 3;

    @Test
    public void testToStringCallable() {
        ExecutorService service = Executors.newFixedThreadPool(SIZE);
        List<Future<String>> futures = IntStream.range(0, SIZE)
                .mapToObj(i -> "Task number " + i)
                .map(name -> new Callable<String>() {
                    public String call() {
                        return Thread.currentThread().getName();
                    }

                    public String toString() {
                        return name;
                    }
                })
                .map(service::submit)
                .collect(Collectors.toList());
        service.shutdown();
        assertThat(futures.stream().map(Futures::getUnchecked)).containsExactlyInAnyOrder(
                "Task number 0",
                "Task number 1",
                "Task number 2"
        );
    }

    @Test
    public void testToStringCallableInvokeAll() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(SIZE);
        List<Future<String>> futures = service.invokeAll(IntStream.range(0, SIZE)
                .mapToObj(i -> "Task number " + i)
                .map(name -> new Callable<String>() {
                    public String call() {
                        return Thread.currentThread().getName();
                    }

                    public String toString() {
                        return name;
                    }
                })
                .collect(Collectors.toList())
        );
        service.shutdown();
        assertThat(futures.stream().map(Futures::getUnchecked)).containsExactlyInAnyOrder(
                "Task number 0",
                "Task number 1",
                "Task number 2"
        );
    }

    @Test
    public void testToStringRunnable() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(SIZE);
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch countDownLatch = new CountDownLatch(SIZE);
        IntStream.range(0, SIZE)
                .mapToObj(i -> "Task number " + i)
                .map(name -> new Runnable() {
                    public void run() {
                        threadNames.add(Thread.currentThread().getName());
                        countDownLatch.countDown();
                    }

                    public String toString() {
                        return name;
                    }
                })
                .forEach(service::submit);
        service.shutdown();
        countDownLatch.await(1L, TimeUnit.SECONDS);
        assertThat(threadNames).containsExactlyInAnyOrder(
                "Task number 0",
                "Task number 1",
                "Task number 2"
        );
    }

    /**
     * Tests that rejected execution is correctly notified in the blocking pool.
     */
    @Test(expected = RejectedExecutionException.class)
    public void testRejectedExecutionForBlockingFixedThreadPool() {
        ExecutorService es = Executors.newBlockingFixedThreadPool(1, 1, ThreadFactories.daemon(), 1);
        try {
            Runnable elt = () -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore
                }
            };
            es.submit(elt);
            es.submit(elt);
            es.submit(elt);
        } finally {
            es.shutdownNow();
            try {
                es.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ingore
            }
        }
    }
}
