package ru.yandex.market.billing.checkout.logbroker;

import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.core.logbroker.receiver.StoppableTransactionalLogbrokerListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class LogbrokerCheckouterConsumerTest extends FunctionalTest {
    @Autowired
    @Qualifier("stoppableTransactionalLogbrokerListener")
    private StreamListener streamListener;

    @Test
    @DisplayName("Проверка поведения при возникновении ошибки обработки данных")
    void testProcessingError() {
        var responderMock = mock(StreamListener.ReadResponder.class);
        streamListener.onRead(new ConsumerReadResponse(List.of(new MessageBatch("test-topic", 0, List.of())), 0), responderMock);
        verify(responderMock, times(1)).commit();
        //второе чтение бросает исключение
        assertThatIllegalArgumentException()
                .isThrownBy(() -> streamListener.onRead(
                        new ConsumerReadResponse(List.of(new MessageBatch("test-topic", 1, List.of())), 0),
                        responderMock
                ));
        //проверяем, что коммита при ошибке не произошло
        verifyNoMoreInteractions(responderMock);

        streamListener.onClose();
        assertThat(((StoppableTransactionalLogbrokerListener) streamListener).isStopped()).isTrue();
    }


}
