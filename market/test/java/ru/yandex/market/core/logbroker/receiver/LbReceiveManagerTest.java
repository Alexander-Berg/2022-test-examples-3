package ru.yandex.market.core.logbroker.receiver;

import java.util.concurrent.TimeUnit;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.core.FunctionalTest;

/**
 * Тесты для {@link LbReceiveManager}
 */
@ExtendWith(MockitoExtension.class)
class LbReceiveManagerTest extends FunctionalTest {

    private static final ReceiveConfig CONFIG = ReceiveConfig.builder()
            .setTopicName("some-test-topic")
            .setNumberOfReaders(1)
            // 2s for execution
            .setExecutionTimeLimit(2L)
            .setExecutionTimeUnit(TimeUnit.SECONDS)
            // 1s for waiting
            .setShutdownWaitingTime(1L)
            .setShutdownWaitingTimeUnit(TimeUnit.SECONDS)
            // 0s for processing waiting
            .setReceiverSleepTimeLimit(0L)
            .setReceiverSleepTimeUnit(TimeUnit.SECONDS)
            .build();

    @Mock
    private Receiver receiverMock;

    private LbReceiveManager receiveManager;

    @BeforeEach
    void setup() {
        receiveManager = new LbReceiveManager(
                (ignoredExCollector, ignoredConfig) -> receiverMock,
                CONFIG
        );
    }

    @Test
    void receiveManagerTest() {
        receiveManager.receive(new ExceptionCollector());

        InOrder inOrderReceiver = Mockito.inOrder(receiverMock);
        inOrderReceiver.verify(receiverMock)
                .run();
        inOrderReceiver.verify(receiverMock)
                .stopReceive();
        inOrderReceiver.verifyNoMoreInteractions();
    }
}
