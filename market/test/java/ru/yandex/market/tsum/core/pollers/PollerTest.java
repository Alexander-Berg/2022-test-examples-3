package ru.yandex.market.tsum.core.pollers;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.tsum.clients.pollers.Poller;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 06.04.17
 */
public class PollerTest {
    @Test
    public void testRun() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);
        Mockito.when(action.call()).thenReturn(1).thenReturn(2).thenReturn(3);

        Poller.poll(action)
            .canStopWhen(t -> t == 3 )
            .interval(1, TimeUnit.MILLISECONDS)
            .allowIntervalLessThenOneSecond(true)
            .run();

        Mockito.verify(action, Mockito.times(3)).call();
    }

    @Test
    public void testIgnore() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);
        Mockito.when(action.call())
            .thenReturn(1)
            .thenThrow(new IllegalArgumentException("testing ignore exception in builder"))
            .thenReturn(3);

        Poller.poll(action)
            .canStopWhen(t -> t == 3)
            .ignoring(IllegalArgumentException.class)
            .interval(1, TimeUnit.MILLISECONDS)
            .allowIntervalLessThenOneSecond(true)
            .run();

        Mockito.verify(action, Mockito.times(3)).call();
    }

    @Test(expected = TimeoutException.class)
    public void testTimeout() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);
        Mockito.when(action.call()).thenReturn(1).thenReturn(2).thenReturn(3);

        Poller.poll(action)
            .canStopWhen(t -> t == 3)
            .timeout(100, TimeUnit.MILLISECONDS)
            .interval(500, TimeUnit.MILLISECONDS)
            .allowIntervalLessThenOneSecond(true)
            .run();
    }

    @Test(expected = RuntimeException.class)
    public void testRetry() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);
        Mockito.when(action.call())
            .thenThrow(new IllegalArgumentException("testing retry after exception in builder"))
            .thenReturn(2)
            .thenThrow(new IllegalArgumentException("testing retry after exception in builder"))
            .thenThrow(new IllegalArgumentException("testing retry after exception in builder"))
            .thenThrow(new IllegalArgumentException("testing retry after exception in builder"))
            .thenReturn(3);

        Poller.poll(action)
            .canStopWhen(t -> t == 3)
            .retryOnExceptionCount(2)
            .interval(1, TimeUnit.MILLISECONDS)
            .allowIntervalLessThenOneSecond(true)
            .run();
    }

    @Test
    public void testRetryReset() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);
        Mockito.when(action.call())
            .thenThrow(new IllegalArgumentException("testing retry reset after exception in builder"))
            .thenReturn(2)
            .thenThrow(new IllegalArgumentException("testing retry reset after exception in builder"))
            .thenReturn(3)
            .thenThrow(new IllegalArgumentException("testing retry reset after exception in builder"))
            .thenReturn(4)
            .thenThrow(new IllegalArgumentException("testing retry reset after exception in builder"))
            .thenReturn(5);

        Poller.poll(action)
            .canStopWhen(t -> t >= 5)
            .interval(1, TimeUnit.MILLISECONDS)
            .allowIntervalLessThenOneSecond(true)
            .retryOnExceptionCount(2)
            .run();
    }

    @Test(expected = RuntimeException.class)
    public void testNotIgnoring() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);
        Mockito.when(action.call())
            .thenThrow(new IllegalArgumentException("testing ignoring exception in builder"))
            .thenThrow(new IllegalStateException("testing not ignoring exception in builder"))
            .thenReturn(3);

        Poller.poll(action)
            .canStopWhen(t -> t > 2)
            .retryOnExceptionCount(0)
            .interval(1, TimeUnit.MILLISECONDS)
            .allowIntervalLessThenOneSecond(true)
            .ignoring(IllegalArgumentException.class)
            .run();
    }

    @Test
    public void testSleep() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);
        IntFunction<Long> interval = (IntFunction<Long>) Mockito.mock(IntFunction.class);
        Mockito.when(action.call()).thenReturn(1).thenReturn(2).thenReturn(3);
        Mockito.when(interval.apply(Mockito.anyInt())).thenReturn(1L);

        Poller.poll(action)
            .canStopWhen(t -> t.equals(3))
            .interval(interval, TimeUnit.MILLISECONDS)
            .allowIntervalLessThenOneSecond(true)
            .run();

        Mockito.verify(action, Mockito.times(3)).call();
        Mockito.verify(interval, Mockito.times(2)).apply(Mockito.anyInt());
    }

    @Test
    public void testOnThrow_SuccessfulRetry() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);

        Mockito.when(action.call())
            .thenThrow(RuntimeException.class)
            .thenThrow(RuntimeException.class)
            .thenThrow(RuntimeException.class)
            .thenReturn(42);

        Integer result = Poller
            .poll(action)
            .onThrow(ex -> ex instanceof RuntimeException)
            .interval(retryCount -> 1L << retryCount, TimeUnit.NANOSECONDS)
            .allowIntervalLessThenOneSecond(true)
            .retryOnExceptionCount(5)
            .run();

        Assert.assertEquals(Integer.valueOf(42), result);
        Mockito.verify(action, Mockito.times(4)).call();
    }

    @Test(expected = RuntimeException.class)
    public void testOnThrow_UnsuccessfulRetry() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);

        Mockito.when(action.call())
            .thenThrow(RuntimeException.class);

        Poller
            .poll(action)
            .onThrow(ex -> false)
            .interval(retryCount -> 1L << retryCount, TimeUnit.NANOSECONDS)
            .allowIntervalLessThenOneSecond(true)
            .retryOnExceptionCount(5)
            .run();
    }

    @Test(expected = IllegalStateException.class)
    public void testOnThrow_UnretryableException() throws Throwable {
        Callable<Integer> action = Mockito.mock(Callable.class);

        Mockito.when(action.call())
            .thenThrow(IllegalArgumentException.class)
            .thenThrow(IllegalStateException.class);

        Poller
            .poll(action)
            .onThrow(ex -> ex instanceof IllegalArgumentException)
            .interval(retryCount -> 1L << retryCount, TimeUnit.NANOSECONDS)
            .allowIntervalLessThenOneSecond(true)
            .retryOnExceptionCount(5)
            .run();
    }
}
