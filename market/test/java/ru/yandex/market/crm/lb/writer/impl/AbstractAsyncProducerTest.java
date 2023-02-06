package ru.yandex.market.crm.lb.writer.impl;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;

abstract class AbstractAsyncProducerTest {

    protected static final byte[] DATA = new byte[0];

    protected AsyncProducer delegate;

    @BeforeEach
    void setUp() {
        delegate = Mockito.mock(AsyncProducer.class);
        Mockito.when(delegate.init()).thenReturn(CompletableFuture.completedFuture(null));
    }
}
