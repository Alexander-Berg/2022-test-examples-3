package ru.yandex.market.crm.lb.writer.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.ProducerStreamClosedException;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;

public class SelfRevivingAsyncProducerTest extends AbstractAsyncProducerTest {

    private AsyncProducer producer;

    @BeforeEach
    void setUp() {
        super.setUp();
        Mockito.when(delegate.write(DATA))
                .thenReturn(failedFuture(new ProducerStreamClosedException()))
                .thenReturn(completedFuture(null));
        producer = new SelfRevivingAsyncProducer(() -> delegate);
    }

    @Test
    void shouldReviveProducerOnStreamClosed() {
        producer.init();
        Mockito.verify(delegate, Mockito.times(1)).init();

        assertThat(producer.write(DATA)).hasFailed();
        Mockito.verify(delegate, Mockito.times(1)).write(DATA);
        Mockito.verify(delegate, Mockito.times(1)).close();
        Mockito.verify(delegate, Mockito.times(2)).init();

        assertThat(producer.write(DATA)).hasNotFailed();
        Mockito.verify(delegate, Mockito.times(2)).write(DATA);
        Mockito.verifyNoMoreInteractions(delegate);
    }
}
