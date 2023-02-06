package ru.yandex.market.logistics.logistics4shops.logbroker;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Обработка событий")
class NotifyingStreamListenerTest extends AbstractIntegrationTest {

    @Autowired
    StreamListener.ReadResponder readResponder;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(readResponder);
    }

    @Test
    @DisplayName("Успех")
    void success() {
        NotifyingStreamListener<Long> notifyingStreamListener = new NotifyingStreamListener<>(
            new LogbrokerMessageHandler<>() {
                @Override
                public Long parse(@Nonnull byte[] data) {
                    return 1L;
                }

                @Override
                public void handle(@Nonnull List<Long> messages) {
                    //do nothing
                }
            }
        );
        notifyingStreamListener.onRead(new ConsumerReadResponse(List.of(), 100), readResponder);
        verify(readResponder).commit();
    }

    @Test
    @DisplayName("Ошибка обработки")
    void fail() {
        NotifyingStreamListener<Long> notifyingStreamListener = new NotifyingStreamListener<>(
            new LogbrokerMessageHandler<>() {
                @Override
                public Long parse(@Nonnull byte[] data) {
                    return 1L;
                }

                @Override
                public void handle(@Nonnull List<Long> messages) {
                    throw new IllegalStateException();
                }
            }
        );
        notifyingStreamListener.onRead(new ConsumerReadResponse(List.of(), 100), readResponder);
    }
}
