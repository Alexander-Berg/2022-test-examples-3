package ru.yandex.market.ff.dbqueue.consumer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.model.dbqueue.PublishRequestStatusChangePayload;
import ru.yandex.market.ff.service.enums.LogbrokerTopic;

public class PublishRequestStatusChangeQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {
    private static final String GIVEN_PAYLOAD_STRING =
        "{\"requestId\":1,\"requestType\":0," +
            "\"detailsLoaded\":true," +
            "\"changedAt\":\"2020-09-09T09:09:09\"," +
            "\"oldStatus\":0,\"newStatus\":1,\"receivedChangeAt\":\"2020-09-09T09:09:09\"," +
            "\"topic\":\"REQUEST_STATUS_EVENTS\"}";

    @Autowired
    private PublishRequestStatusChangeQueueConsumer consumer;

    @Test
    void testDeserializationWorks() {
        var expected = new PublishRequestStatusChangePayload(
            1L,
            0,
            true,
            false,
            LocalDateTime.of(2020, 9, 9, 9, 9, 9),
            LocalDateTime.of(2020, 9, 9, 9, 9, 9),
            RequestStatus.CREATED,
            RequestStatus.VALIDATED,
            LogbrokerTopic.REQUEST_STATUS_EVENTS
        );

        var actual = consumer.getPayloadTransformer().toObject(GIVEN_PAYLOAD_STRING);

        assertions.assertThat(actual).isEqualTo(expected);
    }
}
