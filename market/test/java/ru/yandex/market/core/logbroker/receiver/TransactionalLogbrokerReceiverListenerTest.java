package ru.yandex.market.core.logbroker.receiver;

import java.util.List;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.kikimr.persqueue.consumer.StreamListener.ReadResponder;
import ru.yandex.kikimr.persqueue.consumer.transport.message.CommitMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.logbroker.consumer.LogbrokerDataProcessor;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;

/**
 * Тесты для {@link TransactionalLogbrokerReceiverListener}
 */
@ExtendWith(MockitoExtension.class)
class TransactionalLogbrokerReceiverListenerTest extends FunctionalTest {

    private static final MessageBatch DUMMY_MSG = new MessageBatch(
            "some-test-topic",
            1,
            List.of()
    );

    @Mock
    private LogbrokerDataProcessor dataProcessorMock;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Mock
    private LogbrokerMonitorExceptionsService logbrokerMonitorExceptionsServiceMock;
    @Mock
    private ReceiverListener receiverMock;
    @Mock
    private ConsumerReadResponse readMock;
    @Mock
    private ReadResponder readResponderMock;
    @Mock
    private CommitMessage commitMessageMock;

    private TransactionalLogbrokerReceiverListener listener;

    @BeforeEach
    void setup() {
        Mockito.when(readMock.getBatches())
                .thenReturn(List.of(DUMMY_MSG));

        listener = new TransactionalLogbrokerReceiverListener(
                dataProcessorMock,
                transactionTemplate,
                logbrokerMonitorExceptionsServiceMock,
                receiverMock
        );
    }

    @Test
    void listenerTest() {
        // Проверяем успешное чтение сообщения
        listener.onRead(readMock, readResponderMock);

        Mockito.verify(dataProcessorMock)
                .process(Mockito.any());

        listener.stopReading();
        listener.onRead(readMock, readResponderMock);
        // Проверяем уведомление Receiver'а об окончании обработки сообщения
        Mockito.verify(receiverMock)
                .listenerReadingFinished();

        listener.onClose();
        // Проверяем уведомление Receiver'а об окончании сессии
        Mockito.verify(receiverMock)
                .listenerSessionClosed();
    }

    @Test
    void listenerProcessWithExceptionTest() {
        var exception = new RuntimeException();
        Mockito.doThrow(exception)
                .when(dataProcessorMock)
                .process(Mockito.any());

        Assertions.assertThrows(
                RuntimeException.class,
                () -> listener.onRead(readMock, readResponderMock)
        );

        Mockito.verify(dataProcessorMock)
                .process(Mockito.any());

        // Проверяем уведомление Receiver'а об ошибке при обработки сообщений
        Mockito.verify(receiverMock)
                .handleListenerException(
                        Mockito.argThat(arg -> exception.equals(arg.getCause()))
                );

        // Проверяем что больше не обрабатываем сообщения
        listener.onRead(readMock, readResponderMock);
        listener.onRead(readMock, readResponderMock);

        Mockito.verifyNoMoreInteractions(dataProcessorMock);
    }

    @Test
    void listenerOnErrorTest() {
        var exception = new StatusRuntimeException(Status.UNKNOWN);

        listener.onError(exception);

        // Проверяем уведомление Receiver'а об ошибке при обработки сообщений
        Mockito.verify(receiverMock)
                .handleListenerException(
                        Mockito.argThat(arg -> exception.equals(arg.getCause()))
                );

        // Проверяем уведомление Receiver'а об окончании сессии
        Mockito.verify(receiverMock)
                .listenerSessionClosed();

        // На всякий случай проверяем что больше не обрабатываем сообщения
        // (Как я понял из документации onError это последний вызов в сессии
        // но на всякий случай проверку оставил)
        listener.onRead(readMock, readResponderMock);
        listener.onRead(readMock, readResponderMock);

        Mockito.verifyNoMoreInteractions(dataProcessorMock);
    }
}
