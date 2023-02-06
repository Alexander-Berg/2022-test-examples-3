package ru.yandex.market.ff.dbqueue.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.PushRightVerdictMeasurementPayload;
import ru.yandex.market.ff.model.dto.dbqueue.ItemIdentifierDTO;

public class PushRightVerdictMeasurementQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING =
            "{\"identifiers\":[{\"supplierId\":3,\"shopSku\":\"sku1\"}," +
                    "{\"supplierId\":3,\"shopSku\":\"sku2\"}],\"requestId\":3}";

    @Autowired
    private PushRightVerdictMeasurementQueueConsumer consumer;

    @Test
    public void testDeserializationWorks() {
        PushRightVerdictMeasurementPayload payload = consumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(3L);

        assertions.assertThat(payload.getIdentifiers().size()).isEqualTo(2);

        ItemIdentifierDTO firstActualIdentifier = payload.getIdentifiers().get(0);
        assertions.assertThat(firstActualIdentifier).isEqualTo(new ItemIdentifierDTO(3L, "sku1"));

        ItemIdentifierDTO secondActualIdentifier = payload.getIdentifiers().get(1);
        assertions.assertThat(secondActualIdentifier).isEqualTo(new ItemIdentifierDTO(3L, "sku2"));
    }
}
