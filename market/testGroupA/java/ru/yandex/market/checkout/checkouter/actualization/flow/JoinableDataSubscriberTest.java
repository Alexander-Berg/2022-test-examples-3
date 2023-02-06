package ru.yandex.market.checkout.checkouter.actualization.flow;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.actualization.flow.DataSubscriber.JoinableDataSubscriber;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.isA;

public class JoinableDataSubscriberTest extends BaseReceivingFlowTest {

    private JoinableDataSubscriber<SimpleContext, Integer> subscriber;

    @BeforeEach
    void configure() {
        subscriber = new JoinableDataSubscriber<>();
    }

    @Test
    void shouldCallSubscriberOnSuccess() {
        AtomicInteger result = new AtomicInteger();
        subscriber.join(DataSubscriber.onSuccess((ctx, value) -> result.set(requireNonNull(value))));
        subscriber.succeeded(context(), 13);

        assertThat(result.get(), comparesEqualTo(13));
    }

    @Test
    void shouldCallSubscriberOnSuccess2() {
        AtomicInteger result = new AtomicInteger();
        subscriber.join(DataSubscriber.onSuccess(value -> result.set(requireNonNull(value))));
        subscriber.succeeded(context(), 13);

        assertThat(result.get(), comparesEqualTo(13));
    }

    @Test
    void shouldCallSubscriberAfterSuccess() {
        AtomicInteger result = new AtomicInteger();
        subscriber.succeeded(context(), 13);
        subscriber.join(DataSubscriber.onSuccess((ctx, value) -> result.set(requireNonNull(value))));

        assertThat(result.get(), comparesEqualTo(13));
    }

    @Test
    void shouldCallSubscriberAfterSuccess2() {
        AtomicInteger result = new AtomicInteger();
        subscriber.succeeded(context(), 13);
        subscriber.join(DataSubscriber.onSuccess(value -> result.set(requireNonNull(value))));

        assertThat(result.get(), comparesEqualTo(13));
    }

    @Test
    void shouldCallAllSubscribersOnSuccessOneTime() {
        AtomicInteger result1 = new AtomicInteger();
        AtomicInteger result2 = new AtomicInteger();
        subscriber.join(DataSubscriber.onSuccess((ctx, value) -> result1.addAndGet(requireNonNull(value))));
        subscriber.join(DataSubscriber.onSuccess((ctx, value) -> result2.addAndGet(requireNonNull(value))));
        subscriber.succeeded(context(), 13);

        assertThat(result1.get(), comparesEqualTo(13));
        assertThat(result2.get(), comparesEqualTo(13));
    }

    @Test
    void shouldCallAllSubscribersOneDifferentTime() {
        AtomicInteger result1 = new AtomicInteger();
        AtomicInteger result2 = new AtomicInteger();
        subscriber.join(DataSubscriber.onSuccess((ctx, value) -> result1.addAndGet(requireNonNull(value))));
        subscriber.succeeded(context(), 13);
        subscriber.join(DataSubscriber.onSuccess((ctx, value) -> result2.addAndGet(requireNonNull(value))));

        assertThat(result1.get(), comparesEqualTo(13));
        assertThat(result2.get(), comparesEqualTo(13));
    }

    @Test
    void shouldCallSubscribersOnError() {
        AtomicReference<Throwable> result = new AtomicReference<>();
        subscriber.join(DataSubscriber.onError((ctx, ex) -> result.set(requireNonNull(ex))));
        subscriber.failed(context(), new IllegalAccessException());

        assertThat(result.get(), isA(IllegalAccessException.class));
    }

    @Test
    void shouldCallSubscribersOnError2() {
        AtomicReference<Throwable> result = new AtomicReference<>();
        subscriber.join(DataSubscriber.onError((ctx, ex) -> result.set(requireNonNull(ex))));
        subscriber.failed(context(), new IllegalAccessException());

        assertThat(result.get(), isA(IllegalAccessException.class));
    }

    @Test
    void shouldCallErrorCallbackOnSubscriberError() {
        AtomicReference<Throwable> result = new AtomicReference<>();
        subscriber.join(DataSubscriber.onError((ctx, ex) -> result.set(requireNonNull(ex))));
        subscriber.join(DataSubscriber.onSuccess(v -> {
            throw new IllegalAccessError();
        }));
        subscriber.succeeded(context(), 1);

        assertThat(result.get(), isA(IllegalAccessError.class));
    }

    @Test
    void shouldCallErrorCallbackOnSubscriberError2() {
        AtomicReference<Throwable> result = new AtomicReference<>();
        subscriber.join(DataSubscriber.onSuccess(v -> {
            throw new IllegalAccessError();
        }));
        subscriber.succeeded(context(), 1);

        subscriber.join(DataSubscriber.onError((ctx, ex) -> result.set(requireNonNull(ex))));

        assertThat(result.get(), isA(IllegalAccessError.class));
    }
}
