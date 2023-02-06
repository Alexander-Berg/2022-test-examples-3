package ru.yandex.market.crm.lb.writer.impl;

import java.util.function.Predicate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

class RetryableAsyncProducerTest extends AbstractAsyncProducerTest {

    private static final int ATTEMPTS = 3;

    @Test
    public void shouldRetryByPredicate() {
        AsyncProducer producer = createProducer(new IllegalStateException(),
                e -> e instanceof IllegalStateException);
        Assertions.assertThat(producer.write(DATA)).hasNotFailed();
        Mockito.verify(delegate, Mockito.times(ATTEMPTS)).write(DATA);
    }

    @Test
    public void shouldNotRetryOnFailedPredicate() {
        AsyncProducer producer = createProducer(new NullPointerException(),
                e -> e instanceof IllegalStateException);
        Assertions.assertThat(producer.write(DATA)).hasFailed();
        Mockito.verify(delegate, Mockito.times(1)).write(DATA);
    }

    private AsyncProducer createProducer(Throwable throwable, Predicate<Throwable> retryPredicate) {
        Mockito.when(delegate.write(DATA))
                .thenReturn(failedFuture(throwable))
                .thenReturn(failedFuture(throwable))
                .thenReturn(completedFuture(null));
        AsyncProducer producer = new RetryableAsyncProducer(delegate, retryPredicate, ATTEMPTS);
        producer.init();
        return producer;
    }
}
