package ru.yandex.market.logbroker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.logbroker.event.BaseLogbrokerEvent;
import ru.yandex.market.logbroker.event.LogbrokerEvent;
import ru.yandex.market.logbroker.producer.SimpleAsyncProducer;
import ru.yandex.market.logbroker.producer.SimpleAsyncProducerProvider;
import ru.yandex.market.logbroker.utils.RetryTemplateCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class TestConfig {
    int maxRetries = 3;
    @Bean(name = "logbrokerServiceRetryTemplate")
    public RetryTemplate retryTemplate() {
        return RetryTemplateCreator.getRetryTemplate(maxRetries);
    }
}

@SpringJUnitConfig(classes = {
        TestConfig.class,
})
class LogbrokerEventPublisherImplTest {
    private static final LogbrokerEvent<String> EVENT = new TestEvent("test");
    private static final byte[] BYTE_DATA = EVENT.getBytes();
    private LogbrokerEventPublisherImpl<LogbrokerEvent<?>> publisher;
    private SimpleAsyncProducerProvider provider;
    private SimpleAsyncProducer simpleAsyncProducer;

    @Autowired
    @Qualifier("logbrokerServiceRetryTemplate")
    private RetryTemplate retryTemplate;

    @BeforeEach
    void setUp() throws Exception {
        retryTemplate = spy(retryTemplate);
        simpleAsyncProducer = mock(SimpleAsyncProducer.class);
        provider = mock(SimpleAsyncProducerProvider.class);
        when(provider.asyncProducer()).thenReturn(simpleAsyncProducer);

        publisher = new LogbrokerEventPublisherImpl<>(provider, retryTemplate);
    }

    @Test
    void syncPublishWrapsExceptionProperly() {
        // given
        Exception expectedException = new RuntimeException("some exception");
        when(simpleAsyncProducer.write(BYTE_DATA)).thenReturn(makeFutureFail(expectedException));

        // when
        assertThatExceptionOfType(LogbrokerInteractionException.class)
                .isThrownBy(() -> publisher.publishEvent(EVENT))
                .havingCause().isInstanceOf(ExecutionException.class)
                .havingCause().isSameAs(expectedException);

        // then
        verify(simpleAsyncProducer, times(3)).write(BYTE_DATA);
        verify(retryTemplate).execute(any(), any(), any());
    }

    @Test
    void waitSuccessOnFirstPublish() {
        // given
        when(simpleAsyncProducer.write(BYTE_DATA)).thenReturn(makeFutureOk());

        // when
        CompletableFuture<LogbrokerEvent<?>> ack = publisher.publishEventAsync(EVENT);

        // then
        assertThat(ack).isCompletedWithValue(EVENT);
        verify(simpleAsyncProducer).write(BYTE_DATA);
        verify(retryTemplate).execute(any(), any(), any());
    }

    @Test
    void waitRetriesOnProducerCreationError() throws Exception {
        // given
        Exception expectedException = new Exception("some exception");
        when(provider.asyncProducer()).thenThrow(expectedException);

        // when
        CompletableFuture<LogbrokerEvent<?>> ack = publisher.publishEventAsync(EVENT);

        // then
        assertThat(ack)
                .failsWithin(1L, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .havingCause().isInstanceOf(LogbrokerInteractionException.class)
                .havingCause().as("no redundant ExecutionException in stack").isSameAs(expectedException);
        verifyZeroInteractions(simpleAsyncProducer);
        verify(retryTemplate).execute(any(), any(), any());
    }

    @Test
    void waitRetriesOnWriteError() {
        // given
        Exception expectedException = new RuntimeException("some exception");
        when(simpleAsyncProducer.write(BYTE_DATA)).thenReturn(makeFutureFail(expectedException));

        // when
        CompletableFuture<LogbrokerEvent<?>> ack = publisher.publishEventAsync(EVENT);

        // then
        assertThat(ack)
                .failsWithin(1L, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .havingCause().isInstanceOf(LogbrokerInteractionException.class)
                .havingCause().isInstanceOf(ExecutionException.class)
                .havingCause().isSameAs(expectedException);
        verify(simpleAsyncProducer, times(3)).write(BYTE_DATA);
        verify(retryTemplate).execute(any(), any(), any());
    }

    @Test
    void waitRetriesOnAckError() {
        // given
        when(simpleAsyncProducer.write(BYTE_DATA)).thenReturn(
                makeFutureFail(new RuntimeException("some exception")),
                makeFutureFail(new TimeoutException()),
                makeFutureOk()
        );

        // when
        CompletableFuture<LogbrokerEvent<?>> ack = publisher.publishEventAsync(EVENT);

        // then
        assertThat(ack).isCompletedWithValue(EVENT);
        verify(simpleAsyncProducer, times(3)).write(BYTE_DATA);
        verify(retryTemplate).execute(any(), any(), any());
    }

    @Test
    void noWaitSuccessOnFirstPublish() {
        // given
        publisher.setWaitForAck(false);
        when(simpleAsyncProducer.write(BYTE_DATA)).thenReturn(new CompletableFuture<>()); // not completed

        // when
        CompletableFuture<LogbrokerEvent<?>> ack = publisher.publishEventAsync(EVENT);

        // then
        assertThat(ack).as("should not wait for future if setWaitForAck is set to false").isNotCompleted();
        verify(simpleAsyncProducer).write(BYTE_DATA);
        verifyZeroInteractions(retryTemplate);
    }

    @Test
    void noWaitDoesNotRetryOnFirstProducerCreationError() throws Exception {
        // given
        publisher.setWaitForAck(false);
        Exception expectedException = new Exception("some exception");
        when(provider.asyncProducer()).thenThrow(expectedException);

        // when
        CompletableFuture<LogbrokerEvent<?>> ack = publisher.publishEventAsync(EVENT);

        // then
        assertThat(ack)
                .failsWithin(1L, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .havingCause().isInstanceOf(LogbrokerInteractionException.class)
                .havingCause().as("no redundant ExecutionException in stack").isSameAs(expectedException);
        verifyZeroInteractions(simpleAsyncProducer);
        verifyZeroInteractions(retryTemplate);
    }

    @Test
    void noWaitDoesNotRetryOnFirstWriteError() {
        // given
        publisher.setWaitForAck(false);
        Exception expectedException = new RuntimeException("some exception");
        when(simpleAsyncProducer.write(BYTE_DATA)).thenReturn(makeFutureFail(expectedException));

        // when
        // when
        CompletableFuture<LogbrokerEvent<?>> ack = publisher.publishEventAsync(EVENT);

        // then
        assertThat(ack)
                .failsWithin(1L, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .havingCause().isInstanceOf(LogbrokerInteractionException.class)
                .havingCause().isInstanceOf(CompletionException.class)
                .havingCause().isSameAs(expectedException);

        // then
        verify(simpleAsyncProducer).write(BYTE_DATA);
        verifyZeroInteractions(retryTemplate);
    }

    @Test
    void noWaitRetriesOnAckErrorInBackground() {
        // given
        publisher.setWaitForAck(false);
        CompletableFuture<ProducerWriteResponse> ackFirst = new CompletableFuture<>();
        when(simpleAsyncProducer.write(BYTE_DATA)).thenReturn(
                ackFirst,
                makeFutureFail(new TimeoutException()),
                makeFutureOk()
        );

        // when
        CompletableFuture<LogbrokerEvent<?>> ack = publisher.publishEventAsync(EVENT);
        assertThat(ack).isNotCompleted();
        ackFirst.completeExceptionally(new RuntimeException("some exception"));

        // then
        assertThat(ack).as("first failed ACK should cause retries").isCompletedWithValue(EVENT);
        verify(simpleAsyncProducer, times(3)).write(BYTE_DATA);
        verify(retryTemplate).execute(any(), any(), any());
    }

    private static CompletableFuture<ProducerWriteResponse> makeFutureOk() {
        return CompletableFuture.completedFuture(mock(ProducerWriteResponse.class));
    }

    private static CompletableFuture<ProducerWriteResponse> makeFutureFail(Exception exception) {
        CompletableFuture<ProducerWriteResponse> exceptionFuture = new CompletableFuture<>();
        exceptionFuture.completeExceptionally(exception);
        return exceptionFuture;
    }

    private static class TestEvent extends BaseLogbrokerEvent<String> {
        TestEvent(String data) {
            super(data);
        }

        @Nonnull
        @Override
        public byte[] getBytes() {
            return payload.getBytes();
        }
    }
}
