package ru.yandex.direct.utils.concurrency;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import ru.yandex.direct.utils.Interrupts;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
@SuppressWarnings("squid:S2925")  // Сложно протестировать без sleep
public class ParallelBlockingProcessorTest {
    @Rule
    public TestName testName = new TestName();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Rule
    public Timeout timeout = Timeout.seconds(10);

    protected ParallelBlockingProcessor makeProcessor(int threads) {
        return new ParallelBlockingProcessor(threads, testName.getMethodName() + ":processor:");
    }

    @Parameters({"2", "3", "5"})
    @Test
    public void execute(int threads) throws Exception {
        int spawnThreads = 4;
        AtomicInteger currentExecutingThreadCount = new AtomicInteger();
        AtomicInteger maxExecutingThreadCount = new AtomicInteger();

        try (ParallelBlockingProcessor processor = makeProcessor(threads)) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < spawnThreads; ++i) {
                futures.add(processor.spawn(() -> {
                    Interrupts.failingRun(() -> Thread.sleep(100));
                    int t = currentExecutingThreadCount.incrementAndGet();
                    int oldMax;
                    int max;
                    do {
                        oldMax = maxExecutingThreadCount.get();
                        max = Math.max(oldMax, t);
                    } while (!maxExecutingThreadCount.compareAndSet(oldMax, max));
                    Interrupts.failingRun(() -> Thread.sleep(100));
                    currentExecutingThreadCount.decrementAndGet();
                }));
            }
            for (CompletableFuture<Void> future : futures) {
                future.join();
            }
        }

        softly.assertThat(maxExecutingThreadCount.get())
                .as("Количество одновременно выполняемых задач было равно"
                        + " меньшему из лимита на количество и самого количества")
                .isEqualTo(Math.min(spawnThreads, threads));
    }

    @Parameters({"2", "3", "5"})
    @Test
    public void tryExecute(int attempts) throws Exception {
        int spawnThreads = 2;
        AtomicInteger successfulTasks = new AtomicInteger();
        Runnable task = () -> {
            Interrupts.failingRun(() -> Thread.sleep(100));
            successfulTasks.incrementAndGet();
        };
        Duration tryTimeout = Duration.ofMillis(1);

        try (ParallelBlockingProcessor processor = makeProcessor(spawnThreads)) {
            Collection<CompletableFuture<Void>> futures = new ArrayList<>();
            int i = 0;
            for (; i < spawnThreads; ++i) {
                softly.assertThatCode(() -> futures.add(processor.trySpawn(task, tryTimeout)))
                        .as("Первые задачи должны успешно запуститься. Задача №" + i)
                        .doesNotThrowAnyException();
            }
            for (; i < attempts; ++i) {
                softly.assertThatCode(() -> futures.add(processor.trySpawn(task, tryTimeout)))
                        .as("Последующие задачи должны бросить ошибку по истечению таймаута. Задача №" + i)
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Timed out");
            }
            for (CompletableFuture<Void> future : futures) {
                future.join();
            }
        }

        softly.assertThat(successfulTasks.get())
                .as("В итоге отработало только " + spawnThreads + " задач")
                .isEqualTo(spawnThreads);
    }
}
