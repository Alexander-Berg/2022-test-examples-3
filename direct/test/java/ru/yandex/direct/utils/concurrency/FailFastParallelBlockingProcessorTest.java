package ru.yandex.direct.utils.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class FailFastParallelBlockingProcessorTest extends ParallelBlockingProcessorTest {
    @Override
    protected ParallelBlockingProcessor makeProcessor(int threads) {
        return new FailFastParallelBlockingProcessor(threads, testName.getMethodName() + ":processor:");
    }

    @Parameters({"2", "5"})
    @Test
    public void spawnWithError(int attempts) throws Exception {
        final int spawnThreads = 2;
        AtomicInteger executedTasks = new AtomicInteger();
        Runnable task = () -> {
            executedTasks.incrementAndGet();
            throw new Oops();
        };

        softly.assertThatCode(
                () -> {
                    try (ParallelBlockingProcessor processor = makeProcessor(spawnThreads)) {
                        List<CompletableFuture<Void>> futureHolder = new ArrayList<>();
                        softly.assertThatCode(() -> futureHolder.add(processor.spawn(task)))
                                .as("На этот момент ещё не было упавших задач - не должно быть ошибки")
                                .doesNotThrowAnyException();
                        softly.assertThatCode(() -> futureHolder.get(0).join())
                                .as("Задача действительно провалилась")
                                .isInstanceOf(CompletionException.class)
                                .hasCauseExactlyInstanceOf(Oops.class);
                        for (int i = 1; i < attempts; ++i) {
                            softly.assertThatCode(() -> processor.spawn(task))
                                    .as("Ошибка уже точно произошла - " +
                                            "она должна бросаться при попытке запустить новую задачу")
                                    .isInstanceOf(Oops.class);
                        }
                    }
                })
                .as("Если ошибка была выброшена хотя бы в одном spawn, то close не кидает ошибку")
                .doesNotThrowAnyException();

        softly.assertThat(executedTasks.get())
                .as("Всего задача запускалась лишь однажды")
                .isEqualTo(1);
    }

    @Test
    public void spawnWithErrorOnClose() throws Exception {
        Runnable task = () -> {
            throw new Oops();
        };

        softly.assertThatCode(
                () -> {
                    try (ParallelBlockingProcessor processor = makeProcessor(1)) {
                        List<CompletableFuture<Void>> futureHolder = new ArrayList<>();
                        softly.assertThatCode(() -> futureHolder.add(processor.spawn(task)))
                                .as("На этот момент ещё не было упавших задач - не должно быть ошибки")
                                .doesNotThrowAnyException();
                        softly.assertThatCode(() -> futureHolder.get(0).join())
                                .as("Задача действительно провалилась")
                                .isInstanceOf(CompletionException.class)
                                .hasCauseExactlyInstanceOf(Oops.class);
                    }
                })
                .as("Если ошибка не была выброшена хотя бы в одном spawn, то close кидает ошибку")
                .isInstanceOf(Oops.class);
    }

    private static class Oops extends RuntimeException {
    }
}
