package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;

public class SendMbiNotificationQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {
    private static final String PAYLOAD_STRING = "{\"supplierId\":1,\"notificationType\":1,\"data\":\"1\"}";
    @Autowired
    private SendMbiNotificationQueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        SendMbiNotificationPayload payload = consumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getData()).isEqualTo("1");
        assertions.assertThat(payload.getSupplierId()).isEqualTo(1L);
        assertions.assertThat(payload.getNotificationType()).isEqualTo(1);
    }
}
