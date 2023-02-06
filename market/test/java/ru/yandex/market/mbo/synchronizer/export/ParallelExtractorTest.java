package ru.yandex.market.mbo.synchronizer.export;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.04.2017
 */
@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class ParallelExtractorTest {

    public static final int N_THREADS = 3;
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Mock
    private Callable<Void> task1;
    @Mock
    private Callable<Void> task2;
    @Mock
    private Callable<Void> task3;

    private static ExecutorService pool;

    @BeforeClass
    public static void initClass() {
        pool = Executors.newFixedThreadPool(N_THREADS);
    }

    @Test
    public void testTasksCall() throws Exception {
        ExportRegistry registry = mock(ExportRegistry.class);

        ParallelExtractor<Void> pe = new ParallelExtractorWithTasks<>(task1, task2, task3);
        pe.setRegistry(registry);
        pe.perform("/test/dir");

        verify(task1).call();
        verify(task2).call();
        verify(task3).call();
        verify(registry, never()).registerFailure(any(), any());
    }

    @Test
    public void testTasksCallParallel() throws Exception {
        ExportRegistry registry = mock(ExportRegistry.class);

        ParallelExtractor<Void> pe = new ParallelExtractorWithTasks<>(task1, task2, task3);
        CompletableFuture<Void> completeFuture = pe.performParallel(pool, "/test/dir");

        completeFuture.join();

        verify(task1).call();
        verify(task2).call();
        verify(task3).call();
        verify(registry, never()).registerFailure(any(), any());
    }

    @Test
    public void testFinishedCalled() throws Exception {
        Object definedContext = new Object();
        AtomicInteger finishedCalled = new AtomicInteger();
        AtomicReference<Object> gotContext = new AtomicReference<>();

        ParallelExtractor<Object> pe = new ParallelExtractorWithContextAndTasks<Object>(definedContext) {
            @Override
            protected void extractionFinished(Object context) {
                gotContext.set(context);
                finishedCalled.incrementAndGet();
            }
        };
        pe.perform("/test/dir");

        assertThat("finished method called once", finishedCalled.get(), is(1));
        assertThat("got initialized context", gotContext.get(), sameInstance(definedContext));
    }

    @Test
    public void testFinishedNotCalledOnFail() throws Exception {
        AtomicInteger finishedCalled = new AtomicInteger();

        Callable<Void> failTask = when(mock(Callable.class).call()).thenThrow(new TestException()).getMock();

        ParallelExtractor<Void> pe = new ParallelExtractorWithTasks<Void>(task1, failTask) {
            @Override
            protected void extractionFinished(Void aVoid) {
                finishedCalled.incrementAndGet();
            }
        };
        try {
            pe.perform("/test/dir");
        } catch (Exception e) {
            // skip it
        }

        assertThat("finished method not called on fail in task", finishedCalled.get(), is(0));
    }

    @Test
    public void testFinishedCalledParallel() throws Exception {
        Object definedContext = new Object();
        AtomicInteger finishedCalled = new AtomicInteger();
        AtomicReference<Object> gotContext = new AtomicReference<>();


        ParallelExtractor<Object> pe = new ParallelExtractorWithContextAndTasks<Object>(
            definedContext, task1, task2, task3) {

            @Override
            protected void extractionFinished(Object context) {
                gotContext.set(context);
                finishedCalled.incrementAndGet();
            }
        };
        CompletableFuture<Void> completeFuture = pe.performParallel(pool, "/test/dir");
        completeFuture.join();

        assertThat("finished method called once with parallel", finishedCalled.get(), is(1));
        assertThat("got initialized context parallel", gotContext.get(), sameInstance(definedContext));
    }

    @Test
    public void testFinishedNotCalledOnFailParallel() throws Exception {
        AtomicInteger finishedCalled = new AtomicInteger();

        Callable<Void> failTask = when(mock(Callable.class).call()).thenThrow(new TestException()).getMock();

        ParallelExtractor<Void> pe = new ParallelExtractorWithTasks<Void>(task1, failTask) {
            @Override
            protected void extractionFinished(Void aVoid) {
                finishedCalled.incrementAndGet();
            }
        };

        CompletableFuture<Void> completeFuture = pe.performParallel(pool, "/test/dir");

        try {
            completeFuture.join();
        } catch (Exception e) {
            // skip expected exception
        }

        assertThat("finished method not called on fail in task parallel", finishedCalled.get(), is(0));
    }

    @Test
    public void testSkipTasksIfHasFailedBefore() throws Exception {
        Callable<Void> failTask = when(mock(Callable.class).call()).thenThrow(new TestException()).getMock();

        ParallelExtractor<Void> pe = new ParallelExtractorWithTasks<>(task1, failTask, task2, task3);

        ExecutorService singleThreadExecutor = Executors.newFixedThreadPool(1);
        CompletableFuture<Void> completeFuture = pe.performParallel(singleThreadExecutor, "/test/dir");

        try {
            completeFuture.join();
        } catch (CompletionException e) {
            // skip expected exception
        }

        verify(task2, never()).call();
        verify(task3, never()).call();
    }

    @Test(expected = TestException.class)
    public void testThrowExceptionFromFailedTask() throws Exception {
        TestException testException = new TestException();

        Callable<Void> failTask = when(mock(Callable.class).call()).thenThrow(testException).getMock();

        ParallelExtractor<Void> pe = new ParallelExtractorWithTasks<>(task1, failTask);
        pe.perform("/test/dir");
    }

    @Test
    public void testThrowExceptionFromFailedTaskParallel() throws Exception {
        TestException testException = new TestException();

        exception.expect(CompletionException.class);
        exception.expectCause(sameInstance(testException));

        Callable<Void> failTask = when(mock(Callable.class).call()).thenThrow(testException).getMock();

        ParallelExtractor<Void> pe = new ParallelExtractorWithTasks<>(task1, failTask);
        CompletableFuture<Void> completeFuture = pe.performParallel(pool, "/test/dir");

        completeFuture.join();
    }

    @Test
    public void testRegisterFailureCalledOnFail() throws Exception {
        TestException testException1 = new TestException();
        TestException testException2 = new TestException();
        ExportRegistry registry = mock(ExportRegistry.class);

        Callable<Void> failTask1 = when(mock(Callable.class).call()).thenThrow(testException1).getMock();
        Callable<Void> failTask2 = when(mock(Callable.class).call()).thenThrow(testException2).getMock();

        ParallelExtractor<Void> pe = new ParallelExtractorWithTasks<>(task1, failTask1, failTask2);
        pe.setRegistry(registry);
        pe.setName("test-extractor");

        CompletableFuture<Void> completeFuture = pe.performParallel(pool, "/test/dir");

        try {
            completeFuture.join();
        } catch (CompletionException e) {
            // skip expected exception
        }

        verify(registry, description("only single call registerFailure"))
            .registerFailure(eq("test-extractor"), or(eq(testException1), eq(testException2)));
    }

    private static class TestException extends Exception {

    }

    private static class ParallelExtractorWithTasks<T> extends ParallelExtractor<T> {
        private final Collection<Callable<Void>> tasks;

        private ParallelExtractorWithTasks(Callable<Void>... tasks) {
            this.tasks = Arrays.asList(tasks);
        }

        @Override
        protected Collection<Callable<Void>> getTasks(String dir) {
            return this.tasks;
        }
    }

    private static class ParallelExtractorWithContextAndTasks<T> extends ParallelExtractor<T> {
        private final Collection<Callable<Void>> tasks;
        private final T context;

        private ParallelExtractorWithContextAndTasks(T context, Callable<Void>... tasks) {
            this.context = context;
            this.tasks = Arrays.asList(tasks);
        }

        @Override
        protected ExecutionTask<T> initializeTasks(String dir) {
            return new ExecutionTask<>(context, tasks);
        }

        @Override
        protected Collection<? extends Callable<Void>> getTasks(String dir) {
            return initializeTasks(dir).getTasks();
        }
    }
}
