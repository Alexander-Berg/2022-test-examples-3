package ru.yandex.direct.asynchttp;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.asynchttpclient.ListenableFuture;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class RequestDataTest {
    private static final Duration TEST_DURATION = Duration.ofSeconds(5);

    @Test
    public void noExceptionOnConcurrentAccess() throws Exception {
        // запускаем две задачи - одна активно модифицирует RequestData,
        // другая читает его (делает abort)
        // цель теста - убедиться, что исключения не бросаются
        RequestData<String> rd = new RequestData<>(null);

        CountDownLatch latch = new CountDownLatch(2);
        Runnable waitLatch = () -> {
            latch.countDown();
            try {
                latch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        };


        AtomicBoolean mutatorFinished = new AtomicBoolean(false);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture<Void> aborter = CompletableFuture.runAsync(() -> {
            waitLatch.run();
            while (!mutatorFinished.get()) {
                rd.abort();
            }
        }, executorService);

        CompletableFuture<Void> mutator = CompletableFuture.runAsync(() -> {
            waitLatch.run();
            Queue<ListenableFuture> queue = new ArrayDeque<>();
            long finishTime = System.nanoTime() + TEST_DURATION.toNanos();
            while (System.nanoTime() < finishTime && !aborter.isDone()) {
                ListenableFuture fut = mock(ListenableFuture.class);
                queue.add(fut);
                rd.addCall(fut);
                if (queue.size() > 10) {
                    rd.removeCall(queue.poll());
                }
            }
            while (!queue.isEmpty()) {
                rd.removeCall(queue.poll());
            }
            mutatorFinished.set(true);
        }, executorService);

        // не должно быть исключений
        CompletableFuture.allOf(mutator, aborter).get();
    }
}
