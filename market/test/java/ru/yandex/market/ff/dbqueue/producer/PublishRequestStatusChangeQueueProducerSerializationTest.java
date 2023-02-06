package ru.yandex.market.ff.dbqueue.producer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.model.dbqueue.PublishRequestStatusChangePayload;
import ru.yandex.market.ff.service.enums.LogbrokerTopic;

class PublishRequestStatusChangeQueueProducerSerializationTest extends IntegrationTest {
    private static final String EXPECTED_PAYLOAD_STRING =
        "{\"requestId\":1,\"requestType\":0," +
            "\"detailsLoaded\":true," +
            "\"preparedDetailsLoaded\":false," +
            "\"changedAt\":\"2020-09-09T09:09:09\"," +
            "\"oldStatus\":0,\"newStatus\":1,\"receivedChangeAt\":\"2020-09-09T09:09:09\"," +
            "\"topic\":\"REQUEST_STATUS_EVENTS\"}";

    private static final long REQUEST_ID = 1L;

    @Autowired
    private PublishRequestStatusChangeQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        var payload = new PublishRequestStatusChangePayload(
                REQUEST_ID, 0,
            true, false, LocalDateTime.of(2020, 9, 9, 9, 9, 9), LocalDateTime.of(2020, 9, 9, 9, 9, 9),
                RequestStatus.CREATED, RequestStatus.VALIDATED, LogbrokerTopic.REQUEST_STATUS_EVENTS
        );

        String actual = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(actual).isEqualTo(EXPECTED_PAYLOAD_STRING);
    }
}
