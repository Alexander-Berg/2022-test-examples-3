package ru.yandex.market.arbiter.test.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.Value;

/**
 * @author moskovkin@yandex-team.ru
 * @since 24.05.2020
 */
public class TestUtil {
    /**
     * Do specified count of parallel calls of specified callable.
     * Wait on CyclicBarrier internally before method call to make calls more or less really in parallel.
     * Result getErrors() contain exceptions describing failed calls.
     * Result getResults() contain results of successful calls.
     */
    @SneakyThrows
    public static <T> ParallelCallResults<T> doParallelCalls(
            int count, Callable<T> callable
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(count,
                new ThreadFactoryBuilder().setNameFormat("doParallelCalls-%d").build()
        );

        CyclicBarrier barrier = new CyclicBarrier(count);
        List<Callable<T>> parallelTasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            parallelTasks.add(() -> {
                barrier.await();
                return callable.call();
            });
        }
        return doCallsAndCollectResults(executor, parallelTasks);
    }

    @SneakyThrows
    public static <T> ParallelCallResults<T> doParallelCalls(
            int count, List<Callable<T>> callables
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(count * callables.size(),
                new ThreadFactoryBuilder().setNameFormat("doParallelCalls-%d").build()
        );

        CyclicBarrier barrier = new CyclicBarrier(count);
        List<Callable<T>> parallelTasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            for (Callable<T> callable : callables) {
                parallelTasks.add(() -> {
                    barrier.await();
                    return callable.call();
                });
            }
        }
        return doCallsAndCollectResults(executor, parallelTasks);
    }

    @SneakyThrows
    public static <T> ParallelCallResults<T> doCallsAndCollectResults(
            ExecutorService executor, List<Callable<T>> parallelTasks
    ) {
        //Start all threads
        List<Future<T>> futures = executor.invokeAll(parallelTasks);

        //Wait for tasks to finish and collect results
        List<T> results = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        for (Future<T> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof InvocationTargetException) {
                    errors.add(e.getCause().getCause());
                }
                else {
                    errors.add(e.getCause());
                }
            }
        }

        return new ParallelCallResults<>(List.copyOf(results), List.copyOf(errors));
    }

    @Value
    public static class ParallelCallResults<T>  {
        List<T> results;
        List<Throwable> errors;
    }
}
