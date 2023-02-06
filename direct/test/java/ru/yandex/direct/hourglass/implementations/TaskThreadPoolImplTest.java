package ru.yandex.direct.hourglass.implementations;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class TaskThreadPoolImplTest {

    /**
     * Тест проверяет, что если несколько задач запущено, то количество доступных потоков будет равно изначальному
     * количеству минус количество запущенных задач
     * <p>
     * После того, как все задачи выполнятся, количество доступных потоков снова станет равно изначальному
     */
    @Test
    void availableThreadCountTest() throws InterruptedException {
        var threadPool = new TaskThreadPoolImpl(6, new ThreadsHierarchy());
        assertThat(threadPool.availableThreadCount()).isEqualTo(6);
        var checkAvailableThreadLatch = new CountDownLatch(1);

        AtomicReference<CountDownLatch> startFutureLatch = new AtomicReference<>();
        startFutureLatch.set(new CountDownLatch(1));

        Callable<Void> waitWhileCheckingEnd = () -> {
            startFutureLatch.get().countDown();
            checkAvailableThreadLatch.await();
            return null;
        };

        threadPool.start(waitWhileCheckingEnd);
        startFutureLatch.get().await();
        assertThat(threadPool.availableThreadCount()).isEqualTo(5);

        startFutureLatch.set(new CountDownLatch(1));
        threadPool.start(waitWhileCheckingEnd);
        startFutureLatch.get().await();
        assertThat(threadPool.availableThreadCount()).isEqualTo(4);

        checkAvailableThreadLatch.countDown();
        await().atMost(5, SECONDS).alias("Available threads count should become equal to 6 after all tasks in thread" +
                " pool have finished").until(() -> threadPool.availableThreadCount() == 6);
    }

    /**
     * Тест проверяет, что если тред пул уже закрыт, что новую задачу нельзя будет добавтить
     */
    @Test
    void rejectIfThreadPollShutdown() throws InterruptedException {
        var threadPool = new TaskThreadPoolImpl(1, new ThreadsHierarchy());
        threadPool.shutdown();
        var isTerminated = threadPool.awaitTermination(2, SECONDS);
        assertThat(isTerminated).isTrue();
        assertThatThrownBy(() -> threadPool.start(() -> null)).isInstanceOf(RejectedExecutionException.class);
    }

    /**
     * Тест проверяет, что возвращаемая future реагирует на прерывание
     */
    @Test
    void testInterruptableFuture() throws InterruptedException {
        var threadPool = new TaskThreadPoolImpl(1, new ThreadsHierarchy());
        var startFutureLatch = new CountDownLatch(1);
        var stopFutureLatch = new CountDownLatch(1);
        var isInterrupted = new AtomicBoolean();
        Callable<Void> interruptableCallable = () -> {
            startFutureLatch.countDown();
            while (!Thread.currentThread().isInterrupted()) {
            }

            isInterrupted.set(true);
            stopFutureLatch.countDown();
            return null;
        };

        var interruptableFuture = threadPool.start(interruptableCallable);
        startFutureLatch.await();
        interruptableFuture.cancel(true);
        assertTimeoutPreemptively(Duration.ofMillis(1000), (Executable) stopFutureLatch::await, "Interruptable future" +
                "expected to be stopped due to interruption, but it is not");
        assertThat(isInterrupted.get()).isTrue();
    }
}
