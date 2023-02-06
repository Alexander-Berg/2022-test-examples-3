package ru.yandex.market.logbroker;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logbroker.event.LogbrokerEvent;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class LogbrokerEventPublisherTest {
    @Test
    void syncPublishWrapsExceptionProperly() {
        // given
        Exception expectedException = new LogbrokerInteractionException("some exception");
        LogbrokerEventPublisher<LogbrokerEvent> publisher = new LogbrokerEventPublisher<LogbrokerEvent>() {
            @Override
            public CompletableFuture<LogbrokerEvent> publishEventAsync(LogbrokerEvent event) {
                CompletableFuture<LogbrokerEvent> exceptionFuture = new CompletableFuture<>();
                exceptionFuture.completeExceptionally(expectedException);
                return exceptionFuture;
            }

            @Override
            public void close() {
            }
        };

        // when-then
        assertThatExceptionOfType(LogbrokerInteractionException.class)
                .isThrownBy(() -> publisher.publishEvent(mock(LogbrokerEvent.class)))
                .as("top-level ExecutionException is removed from stack by default")
                .isSameAs(expectedException);
    }

}
