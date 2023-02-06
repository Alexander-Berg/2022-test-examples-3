package ru.yandex.market.global.common.test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.TicketStatus;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * @author moskovkin@yandex-team.ru
 * @since 24.05.2020
 */
public class TestUtil {
    private TestUtil() {
    }

    /**
     * Do specified count of parallel calls of specified callable.
     * Wait on CyclicBarrier internally before method call to make calls more or less really in parallel.
     * Result getErrors() contain exceptions describing failed calls.
     * Result getResults() contain results of successful calls.
     */
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

    public static <T> ParallelCallResults<T> doCallsAndCollectResults(
            ExecutorService executor, List<Callable<T>> parallelTasks
    ) {
        try {
            //Start all threads
            List<Future<T>> futures;
                futures = executor.invokeAll(parallelTasks);

            //Wait for tasks to finish and collect results
            List<T> results = new ArrayList<>();
            List<Throwable> errors = new ArrayList<>();
            for (Future<T> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof InvocationTargetException) {
                        errors.add(e.getCause().getCause());
                    } else {
                        errors.add(e.getCause());
                    }
                }
            }

            return new ParallelCallResults<>(List.copyOf(results), List.copyOf(errors));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockRequestAttributes(Map<String, Object> values) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        values.forEach((key, value) -> attributes.setAttribute(key, value, SCOPE_REQUEST));
        RequestContextHolder.setRequestAttributes(attributes);
    }

    public static CheckedUserTicket createCheckedUserTicket(long uid) {
        return new CheckedUserTicket(
                TicketStatus.OK, "", new String[0], uid, new long[]{uid}
        );
    }

    public static class ParallelCallResults<T>  {
        private final List<T> results;
        private final List<Throwable> errors;

        public ParallelCallResults(List<T> results, List<Throwable> errors) {
            this.results = results;
            this.errors = errors;
        }

        public List<T> getResults() {
            return results;
        }

        public List<Throwable> getErrors() {
            return errors;
        }
    }
}
