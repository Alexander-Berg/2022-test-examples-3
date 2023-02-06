package ru.yandex.mbo.tool.jira.MBO23611;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dergachevfv
 * @since 4/6/20
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ParallelExecutionStrategyTest {

    @Test(timeout = 10000)
    public void testParallelExecution() throws InterruptedException {
        Semaphore semaphoreAssertion = new Semaphore(0);
        Semaphore semaphoreWorker = new Semaphore(0);
        AtomicInteger counter = new AtomicInteger(0);

        Consumer<Integer> worker = i -> {
            try {
                counter.incrementAndGet();
                semaphoreAssertion.release();

                semaphoreWorker.acquire();
                counter.incrementAndGet();
                semaphoreAssertion.release();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Consumer<Integer> assertion = i -> {
            try {
                semaphoreAssertion.acquire(2);
                assertThat(counter.get()).isEqualTo(2);

                semaphoreWorker.release(2);

                semaphoreAssertion.acquire(2);
                assertThat(counter.get()).isEqualTo(4);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        List<Consumer<Integer>> tasks = List.of(worker, worker, assertion);

        ExecutorService threadPool = Executors.newFixedThreadPool(tasks.size(),
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .build());
        ExecutionStrategy executionStrategy = new ParallelExecutionStrategy(threadPool);

        executionStrategy.<Integer>apply(
            i -> tasks.get(i).accept(i),
            a -> Stream.iterate(0, i -> i + 1)
                .limit(tasks.size())
                .forEachOrdered(a));

        assertThat(counter.get()).isEqualTo(4);

        threadPool.shutdownNow();
        threadPool.awaitTermination(1L, TimeUnit.SECONDS);
    }
}
