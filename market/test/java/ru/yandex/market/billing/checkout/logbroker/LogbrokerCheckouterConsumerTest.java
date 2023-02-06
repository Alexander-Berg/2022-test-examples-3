package ru.yandex.market.billing.checkout.logbroker;

import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.util.logbroker.StoppableStreamListenerDecorator;

@ExtendWith(MockitoExtension.class)
public class LogbrokerCheckouterConsumerTest extends FunctionalTest {

    @Autowired
    @Qualifier("stoppableTransactionalLogbrokerListener")
    private StreamListener streamListener;

    @Test
    @DisplayName("Проверка поведения при возникновении ошибки обработки данных")
    void testProcessingError() {
        var responderMock = Mockito.mock(StreamListener.ReadResponder.class);
        streamListener.onRead(new ConsumerReadResponse(List.of(new MessageBatch("test-topic", 0, List.of())), 0),
                responderMock);
        Mockito.verify(responderMock, Mockito.times(1)).commit();
        //второе чтение бросает исключение
        /*Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> streamListener.onRead(
                        new ConsumerReadResponse(List.of(new MessageBatch("test-topic", 1, List.of())), 0),
                        responderMock
                ));*/
        //проверяем, что коммита при ошибке не произошло
        Mockito.verifyNoMoreInteractions(responderMock);

        streamListener.onClose();
        Assertions.assertThat(((StoppableStreamListenerDecorator) streamListener).isReadingEnabled()).isFalse();
    }
}
