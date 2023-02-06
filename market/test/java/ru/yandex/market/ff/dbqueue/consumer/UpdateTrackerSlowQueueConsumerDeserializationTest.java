package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.UpdateTrackerStatusPayload;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class UpdateTrackerSlowQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING_VAR_1 = extractFileContent(
            "db-queue/producer/update-tracker/payload1.json").trim();

    @Autowired
    private UpdateTrackerSlowQueueConsumer updateTrackQueueSlowConsumer;

    @Test
    public void testSerializeWorks() {

        UpdateTrackerStatusPayload updateTrackerStatusPayload = updateTrackQueueSlowConsumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING_VAR_1);

        assertions.assertThat(updateTrackerStatusPayload).isNotNull();
        assertions.assertThat(updateTrackerStatusPayload.getEntityId()).isEqualTo(1);
    }
}
