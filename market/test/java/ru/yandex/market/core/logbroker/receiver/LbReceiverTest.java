package ru.yandex.market.core.logbroker.receiver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.market.core.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link LbReceiver}
 */
@ExtendWith(MockitoExtension.class)
class LbReceiverTest extends FunctionalTest {

    @Mock
    private StreamConsumer consumer;
    @Mock
    private StreamReceiverListener listener;

    private ExceptionCollector exceptionCollector;

    private LbReceiver lbReceiver;

    private ExecutorService executorService;

    private final ReceiveConfig config = ReceiveConfig.builder()
            .setTopicName("some-test-topic")
            // 5s for execution
            .setExecutionTimeLimit(5L)
            .setExecutionTimeUnit(TimeUnit.SECONDS)
            // 1s for waiting
            .setShutdownWaitingTime(1L)
            .setShutdownWaitingTimeUnit(TimeUnit.SECONDS)
            // 0s for processing waiting
            .setReceiverSleepTimeLimit(0L)
            .setReceiverSleepTimeUnit(TimeUnit.SECONDS)
            .setNumberOfReaders(1)
            .build();

    @BeforeEach
    void setup() {
        exceptionCollector = new ExceptionCollector();
        executorService = Executors.newSingleThreadExecutor();

        lbReceiver = new LbReceiver(
                exceptionCollector,
                config,
                () -> consumer,
                receiver -> listener
        );
    }

    @AfterEach
    void afterEach() {
        executorService.shutdown();
        exceptionCollector.close();
    }

    @Test
    void receiverTest() throws InterruptedException {
        // Начинаем обрабатывать сообщения в другом потоке
        executorService.execute(lbReceiver);

        // Ждем пока сообщение пойдет до другого потока
        TimeUnit.SECONDS.sleep(1L);

        // Проверяем начало обработки сообщения
        Mockito.verify(consumer)
                .startConsume(Mockito.argThat(arg -> arg.equals(listener)));

        // Сообщаем об окончании работы
        lbReceiver.stopReceive();

        // Ждем пока сообщение пойдет до другого потока
        TimeUnit.SECONDS.sleep(1L);

        Mockito.verify(listener)
                .stopReading();

        lbReceiver.listenerReadingFinished();

        TimeUnit.SECONDS.sleep(1L);

        // Проверяет окончание обработки сообщения
        Mockito.verify(consumer)
                .stopConsume();

        // Подделываем сообщение от listener'а для завершения теста без ожидания
        lbReceiver.listenerSessionClosed();

        assertTrue(lbReceiver.isStopped());
    }

    @Test
    void receiverListenerExceptionTest() throws InterruptedException {
        ExceptionCollector exceptionCollector = new ExceptionCollector();

        LbReceiver lbReceiver = new LbReceiver(
                exceptionCollector,
                config,
                () -> consumer,
                receiver -> listener
        );

        // Начинаем обрабатывать сообщения в другом потоке
        executorService.execute(lbReceiver);

        var exception = new RuntimeException("Test exception");
        // Подделываем ошибку
        lbReceiver.handleListenerException(exception);

        // Ждем пока сообщение пойдет до другого потока
        TimeUnit.SECONDS.sleep(1L);

        // Ждем пока сообщение пойдет до другого потока
        TimeUnit.SECONDS.sleep(1L);

        lbReceiver.listenerReadingFinished();

        // Проверяет окончание обработки сообщения
        Mockito.verify(consumer)
                .stopConsume();
        // Подделываем остановку listener'а
        lbReceiver.listenerSessionClosed();

        assertTrue(lbReceiver.isStopped());

        var handledException = assertThrows(
                RuntimeException.class,
                exceptionCollector::close
        );
        assertEquals(exception, handledException);
    }

    @Test
    void listenerFinishedReadingTest() throws InterruptedException {
        var config = ReceiveConfig.builder()
                .setTopicName("some-test-topic")
                // 5s for execution
                .setExecutionTimeLimit(5L)
                .setExecutionTimeUnit(TimeUnit.SECONDS)
                // 3s for waiting
                .setShutdownWaitingTime(3L)
                .setShutdownWaitingTimeUnit(TimeUnit.SECONDS)
                // 0s for processing waiting
                .setReceiverSleepTimeLimit(0L)
                .setReceiverSleepTimeUnit(TimeUnit.SECONDS)
                .setNumberOfReaders(1)
                .build();

        lbReceiver = new LbReceiver(
                exceptionCollector,
                config,
                () -> consumer,
                receiver -> listener
        );

        // Начинаем обрабатывать сообщения в другом потоке
        executorService.execute(lbReceiver);

        lbReceiver.stopReceive();

        TimeUnit.SECONDS.sleep(1L);

        Mockito.verify(listener)
                .stopReading();

        lbReceiver.listenerReadingFinished();
        lbReceiver.listenerReadingFinished();

        TimeUnit.SECONDS.sleep(1L);

        // Проверяет окончание обработки сообщения
        Mockito.verify(consumer)
                .stopConsume();

        // Подделываем остановку listener'а
        lbReceiver.listenerSessionClosed();
        assertTrue(lbReceiver.isStopped());
    }
}
