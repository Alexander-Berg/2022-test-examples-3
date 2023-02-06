package ru.yandex.market.mbo.gwt.client.utils.promise;

import org.junit.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 14.04.2017
 */
@SuppressWarnings({"unchecked", "checkstyle:magicNumber"})
public class PromiseTest {

    @Test
    public void testDone() throws Exception {
        Deferred<String> deferred = Deferred.create();
        Promise<String> promise = deferred.promise();

        Consumer<String> callback1 = mock(Consumer.class);
        Consumer<String> callback2 = mock(Consumer.class);
        Consumer<String> callbackAfter = mock(Consumer.class);
        promise.done(callback1).done(callback2);

        deferred.resolve("resolved value");

        verify(callback1).accept("resolved value");
        verify(callback2).accept("resolved value");

        promise.done(callbackAfter);

        verify(callbackAfter).accept("resolved value");

    }

    @Test
    public void testReject() throws Exception {
        Deferred<String> deferred = Deferred.create();
        Promise<String> promise = deferred.promise();

        Consumer<String> callback = mock(Consumer.class);
        Consumer<Throwable> failed = mock(Consumer.class);
        Consumer<Throwable> failedAfter = mock(Consumer.class);
        promise.done(callback).fail(failed);

        Exception ex = new Exception("exception");

        deferred.reject(ex);
        deferred.resolve("value1");
        deferred.resolve("value2");
        promise.fail(failedAfter);

        verify(callback, never()).accept(any());
        verify(failed).accept(same(ex));
        verify(failedAfter).accept(same(ex));
    }

    @Test
    public void testAlways() throws Exception {
        Deferred<String> deferred = Deferred.create();
        Promise<String> promise = deferred.promise();

        BiConsumer<String, Throwable> callback = mock(BiConsumer.class);
        promise.always(callback);

        Exception ex = new Exception("exception");
        deferred.resolve("ok value");
        deferred.reject(ex);

        verify(callback).accept(eq("ok value"), nullable(Throwable.class));
    }

    @Test
    public void testAlwaysRejected() throws Exception {
        Deferred<String> deferred = Deferred.create();
        Promise<String> promise = deferred.promise();

        BiConsumer<String, Throwable> callback = mock(BiConsumer.class);
        promise.always(callback);

        Exception ex = new Exception("exception");

        deferred.reject(ex);
        deferred.resolve("value");

        verify(callback).accept(nullable(String.class), same(ex));

    }

    @Test
    public void testThen() throws Exception {
        Deferred<Integer> deferred = Deferred.create();

        Promise<Integer> promise = deferred.promise();
        Promise<String> secondPromise = promise.then(n -> n * 3).then(n -> "Number is " + n + ";");

        Consumer<String> consumer = mock(Consumer.class);
        Consumer<String> consumer2 = mock(Consumer.class);

        secondPromise.done(consumer);

        deferred.resolve(42);

        secondPromise.done(consumer2);

        verify(consumer).accept("Number is 126;");
        verify(consumer2).accept("Number is 126;");
    }

    @Test
    public void testThenFailed() throws Exception {
        Deferred<Integer> deferred = Deferred.create();

        Promise<Integer> promise = deferred.promise();
        Promise<String> secondPromise = promise.then(n -> n * 3).then(n -> "Number is " + n + ";");
        Consumer<String> consumer = mock(Consumer.class);
        Consumer<Throwable> failHandler = mock(Consumer.class);

        Exception ex = new Exception();

        deferred.reject(ex);
        secondPromise.done(consumer).fail(failHandler);

        verify(consumer, never()).accept(any());
        verify(failHandler).accept(same(ex));
    }

    @Test
    public void testMultipleHandler() throws Exception {
        Deferred<Integer> deferred = Deferred.create();
        Promise<Integer> promise = deferred.promise();

        Consumer<Number> consumer = mock(Consumer.class);

        promise.done(consumer).done(consumer);
        deferred.resolve(42);
        promise.done(consumer);

        verify(consumer, times(3)).accept(42);
    }

    @Test
    public void testMultipleFailHandler() throws Exception {
        Deferred<Integer> deferred = Deferred.create();
        Promise<Integer> promise = deferred.promise();

        Consumer<Throwable> consumer = mock(Consumer.class);
        Exception ex = new Exception();
        promise.fail(consumer).fail(consumer);
        deferred.reject(ex);
        promise.fail(consumer);

        verify(consumer, times(3)).accept(same(ex));
    }

    @Test
    public void testStates() throws Exception {
        Deferred<Integer> deferred = Deferred.create();
        Promise<Integer> promise = deferred.promise();

        assertThat(promise.state(), is(State.PENDING));
        assertThat(promise.isPending(), is(true));
        assertThat(promise.isRejected(), is(false));
        assertThat(promise.isResolved(), is(false));

        deferred.resolve(42);

        assertThat(promise.state(), is(State.RESOLVED));
        assertThat(promise.isPending(), is(false));
        assertThat(promise.isRejected(), is(false));
        assertThat(promise.isResolved(), is(true));

        deferred.reject(new Exception()); // do not change anything

        assertThat(promise.state(), is(State.RESOLVED));
        assertThat(promise.isPending(), is(false));
        assertThat(promise.isRejected(), is(false));
        assertThat(promise.isResolved(), is(true));
    }

    @Test
    public void testStatesRejected() throws Exception {
        Deferred<Integer> deferred = Deferred.create();
        Promise<Integer> promise = deferred.promise();

        assertThat(promise.state(), is(State.PENDING));
        assertThat(promise.isPending(), is(true));
        assertThat(promise.isRejected(), is(false));
        assertThat(promise.isResolved(), is(false));

        deferred.reject(new Exception());

        assertThat(promise.state(), is(State.REJECTED));
        assertThat(promise.isPending(), is(false));
        assertThat(promise.isRejected(), is(true));
        assertThat(promise.isResolved(), is(false));

        deferred.resolve(42);

        assertThat(promise.state(), is(State.REJECTED));
        assertThat(promise.isPending(), is(false));
        assertThat(promise.isRejected(), is(true));
        assertThat(promise.isResolved(), is(false));
    }

    @Test
    public void testProgress() throws Exception {
        Deferred<String> deferred = Deferred.create();
        Promise<String> promise = deferred.promise();

        Consumer<String> progressHandler = mock(Consumer.class);
        Consumer<String> completeHandler = mock(Consumer.class);

        promise.done(completeHandler).progress(progressHandler);

        deferred.notify("slicing...");
        deferred.notify("eating brain...");
        deferred.resolve("zombie");

        InOrder inOrder = inOrder(progressHandler, completeHandler);
        inOrder.verify(progressHandler).accept("slicing...");
        inOrder.verify(progressHandler).accept("eating brain...");
        inOrder.verify(completeHandler).accept("zombie");
    }

    @Test
    public void testWhen() throws Exception {
        Deferred<String> deferred1 = Deferred.create();
        Deferred<String> deferred2 = Deferred.create();
        Promise<String> promise1 = deferred1.promise();
        Promise<String> promise2 = deferred2.promise();

        Consumer<List<Object>> doneHandler = mock(Consumer.class);

        Promise<List<Object>> all = Promise.when(promise1, promise2);
        all.done(doneHandler);

        deferred2.resolve("second value");
        deferred1.resolve("first value");

        verify(doneHandler).accept(Arrays.asList("first value", "second value"));
    }

    @Test
    public void testWhenFailedFirst() throws Exception {
        Deferred<String> deferred1 = Deferred.create();
        Deferred<String> deferred2 = Deferred.create();
        Promise<String> promise1 = deferred1.promise();
        Promise<String> promise2 = deferred2.promise();

        Consumer<List<Object>> doneHandler = mock(Consumer.class);
        Consumer<Throwable> failHandler = mock(Consumer.class);

        Exception ex = new Exception();

        Promise<List<Object>> all = Promise.when(promise1, promise2);
        all.done(doneHandler).fail(failHandler);

        deferred1.reject(ex);
        deferred2.resolve("first value");

        verify(doneHandler, never()).accept(any());
        verify(failHandler).accept(same(ex));
    }

    @Test
    public void testWhenFailedSecond() throws Exception {
        Deferred<String> deferred1 = Deferred.create();
        Deferred<String> deferred2 = Deferred.create();
        Promise<String> promise1 = deferred1.promise();
        Promise<String> promise2 = deferred2.promise();

        Consumer<List<Object>> doneHandler = mock(Consumer.class);
        Consumer<Throwable> failHandler = mock(Consumer.class);

        Exception ex = new Exception();

        Promise<List<Object>> all = Promise.when(promise1, promise2);
        all.done(doneHandler).fail(failHandler);

        deferred1.resolve("first value");
        deferred2.reject(ex);

        verify(doneHandler, never()).accept(any());
        verify(failHandler).accept(same(ex));
    }

    @Test
    public void testWhenAndThen() throws Exception {
        Deferred<Long> deferred1 = Deferred.create();
        Deferred<Integer> deferred2 = Deferred.create();
        Promise<Long> promise1 = deferred1.promise();
        Promise<Integer> promise2 = deferred2.promise();

        Consumer<Number> sumHandler = mock(Consumer.class);
        Consumer<String> finalHandler = mock(Consumer.class);

        Promise.when(promise1, promise2)
                .then(numbers -> numbers.stream().mapToLong(e -> ((Number) e).longValue()).sum())
                .done(sumHandler)
                .then(n -> "Result = " + n)
                .done(finalHandler);

        deferred1.resolve(13L);
        deferred2.resolve(39 - 13);

        verify(sumHandler).accept(39L);
        verify(finalHandler).accept("Result = 39");
    }

    @Test
    public void testWhenSimple() throws Exception {
        Deferred<Set<String>> deferred1 = Deferred.create();
        Deferred<BigDecimal> deferred2 = Deferred.create();
        Deferred<Date> deferred3 = Deferred.create();
        Promise<Set<String>> promise1 = deferred1.promise();
        Promise<BigDecimal> promise2 = deferred2.promise();
        Promise<Date> promise3 = deferred3.promise();

        Consumer<Object> complexCallback = mock(Consumer.class);
        Promise.when(promise1, promise2, promise3)
                .done(complexCallback);

        verify(complexCallback, never()).accept(any());

        deferred2.resolve(BigDecimal.TEN);

        verify(complexCallback, never()).accept(any());

        deferred1.resolve(Collections.singleton("OK"));

        verify(complexCallback, never()).accept(any());

        deferred3.resolve(new Date(123456789));

        verify(complexCallback).accept(Arrays.asList(
                Collections.singleton("OK"),
                BigDecimal.TEN,
                new Date(123456789)
        ));
    }
}
