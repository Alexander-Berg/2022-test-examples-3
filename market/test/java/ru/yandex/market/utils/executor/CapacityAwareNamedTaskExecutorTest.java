package ru.yandex.market.utils.executor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Suppliers;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.concurrent.ThreadFactories;
import ru.yandex.market.mbi.util.concurrent.CompletableFutures;

import static org.assertj.core.api.Assertions.assertThat;

class CapacityAwareNamedTaskExecutorTest {
    private static final int MAX_CAPACITY = 1 << 3; // хотим степень двойки для удобства деления пополам
    private final Predicate<String> EXECUTED_ONLY = r -> !"fallback".equals(r) && !"rejected".equals(r);
    private final ThreadPoolExecutor originalExecutor = new ThreadPoolExecutor(
            MAX_CAPACITY, MAX_CAPACITY,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            ThreadFactories.namedDaemon("test-executor-")
    );
    NamedTaskExecutor namedTaskExecutor = CapacityAwareNamedTaskExecutor.makeWithRatioCapacityPerTask(
            originalExecutor,
            Suppliers.ofInstance(1),
            Suppliers.ofInstance(50) // хотим половину в %
    );

    @AfterEach
    void tearDown() {
        originalExecutor.shutdownNow();
    }

    @Test
    void singleTaskSaturation() {
        // given
        var ticker = new Ticker();
        var tasks = IntStream.range(0, MAX_CAPACITY << 1)
                .mapToObj(i -> ticker.makeTask(0, 2))
                .collect(Collectors.toList());

        // when
        var results = runTasks(ticker, tasks, namedTaskExecutor);

        // then
        assertThat(results)
                .as("все таски завершились так и или иначе")
                .hasSameSizeAs(tasks);
        assertThat(results.stream().filter(EXECUTED_ONLY))
                .as("все threads имели читаемый вид для удобства отладки по thread dump")
                .allSatisfy(r -> assertThat(r).matches("test-executor-\\d+-task-\\d+"))
                .as("реально отработали только таски, которые влезли в capacity")
                .hasSize(MAX_CAPACITY >> 1);
        assertThat(results.stream().filter(Predicate.not(EXECUTED_ONLY)))
                .containsOnly("fallback");
    }

    @Test
    void multipleTaskSaturation() {
        // given
        var maxCapacity = MAX_CAPACITY; // не хотим кричать
        var ticker = new Ticker();
        var tasks = Stream.of(
                // первыми будут медленные таски
                makeNTasks(maxCapacity, 0, 5, ticker),
                makeNTasks(maxCapacity, 1, 4, ticker),
                makeNTasks(maxCapacity, 2, 3, ticker),
                makeNTasks(maxCapacity, 3, 2, ticker)
        ).flatMap(Function.identity()).collect(Collectors.toList());

        // when
        var results = runTasks(ticker, tasks, namedTaskExecutor);

        // then
        assertThat(results)
                .as("все таски завершились так и или иначе")
                .hasSameSizeAs(tasks);
        // таймлайн теста должен быть примерно таким
        // 0ms = id0 batch started and remaining drained, max(1, 1/2 capacity)=4 running
        // 0ms = id1 batch started and remaining drained, max(1, 1/4 capacity)=2 running
        // 0ms = id2 batch started and remaining drained, max(1, 1/8 capacity)=1 running
        // 0ms = id3 batch started, max(1, 1/16 capacity)=1 running
        // 400ms = id3[0] completes, id3[1] started
        // 600ms = id2[0] completes, id3 remaining drained!
        // 800ms = id1 and id3[1] stopped
        // 1000ms = id0 and id[2] stopped
        assertThat(
                results.stream()
                        .filter(EXECUTED_ONLY)
                        .map(r -> StringUtils.substringAfter(r, "-task-"))
                        .collect(Collectors.groupingBy(
                                Function.identity(),
                                Collectors.summingInt(each -> 1)
                        ))
        )
                .hasEntrySatisfying("0", count -> assertThat(count)
                        .as("id0 должна занять 1/2 capacity")
                        .isEqualTo(maxCapacity >> 1))
                .hasEntrySatisfying("1", count -> assertThat(count)
                        .as("id1 должна занять 1/2 оставшейся capacity")
                        .isEqualTo(maxCapacity >> 2))
                .hasEntrySatisfying("2", count -> assertThat(count)
                        .as("id2 должна занять 1/2 оставшейся capacity")
                        .isEqualTo(maxCapacity >> 3))
                .hasEntrySatisfying("3", count -> assertThat(count)
                        .as("для id3 сперва останется всего 1/8 общей capacity," +
                                " после чего она успеет выполниться еще раз." +
                                " но когда закончится первая таска i2, то согласно параметру 50%" +
                                " все оставшиеся таски id3 пролетят вхолостую" +
                                " тк для них уже будет достигнуто ограничение")
                        .isEqualTo(2));
        assertThat(results.stream().filter(Predicate.not(EXECUTED_ONLY)))
                .containsOnly("fallback");
    }

    private static Stream<SupplierWithName<String>> makeNTasks(int N, int id, int ticksToCompletion, Ticker ticker) {
        return IntStream.range(0, N).mapToObj(i -> ticker.makeTask(id, ticksToCompletion));
    }

    private static List<String> runTasks(
            Ticker ticker,
            List<? extends SupplierWithName<String>> tasks,
            NamedTaskExecutor namedTaskExecutor
    ) {
        // добавляем по очереди, для предсказуемого результата
        var tasksFuture = CompletableFutures.allOf(tasks.stream()
                .map(task -> {
                    try {
                        return namedTaskExecutor.supplyAsync(task, "fallback");
                    } catch (RejectedExecutionException e) {
                        return CompletableFuture.completedFuture("rejected");
                    }
                })
                .collect(Collectors.toList()));
        try {
            while (ticker.tick() || !tasksFuture.isDone()) {
                TimeUnit.MILLISECONDS.sleep(100L);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return tasksFuture.join();
    }

    @ThreadSafe
    private static final class Ticker {
        private final Set<CountDownLatch> tasksRunning = new LinkedHashSet<>(); // order just simplifies debugging

        SupplierWithName<String> makeTask(int id, int ticksToCompletion) {
            var task = new CountDownLatch(ticksToCompletion);
            return new SupplierWithName<>() {
                @Override
                public String get() {
                    try {
                        synchronized (tasksRunning) {
                            tasksRunning.add(task);
                        }
                        task.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return Thread.currentThread().getName();
                }

                @Nonnull
                @Override
                public String getName() {
                    return "task-" + id;
                }
            };
        }

        boolean tick() throws InterruptedException {
            synchronized (tasksRunning) {
                for (var iterator = tasksRunning.iterator(); iterator.hasNext(); ) {
                    var task = iterator.next();
                    task.countDown();
                    if (task.getCount() <= 0L) {
                        iterator.remove();
                    }
                }
                return !tasksRunning.isEmpty();
            }
        }
    }
}
