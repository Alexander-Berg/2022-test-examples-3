package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.jobs.model.SendNotificationPayload;
import ru.yandex.market.logistics.nesu.jobs.processor.SendNotificationProcessor;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Отправка уведомления консьюмером SendNotificationConsumer через mbiApiClient")
class SendNotificationConsumerTest extends AbstractTest {
    @Mock
    private MbiApiClient mbiApiClient;

    @InjectMocks
    private SendNotificationProcessor sendNotificationProcessor;

    private SendNotificationConsumer sendNotificationConsumer;

    @BeforeEach
    void setUp() {
        sendNotificationConsumer = new SendNotificationConsumer(sendNotificationProcessor);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mbiApiClient);
    }

    @Test
    @DisplayName("Успешная отправка уведомления")
    void sendNotificationSuccess() {
        MessageRecipients messageRecipients = new MessageRecipients();
        sendNotificationConsumer.execute(new Task<>(
            new QueueShardId("queueShardId"),
            new SendNotificationPayload("requestId", 1, 2L, null, "data", messageRecipients),
            3,
            ZonedDateTime.now(),
            "traceInfo",
            "actor"
        ));
        verify(mbiApiClient).sendNotificationToRecipients(messageRecipients, 1, "data");
    }
}
