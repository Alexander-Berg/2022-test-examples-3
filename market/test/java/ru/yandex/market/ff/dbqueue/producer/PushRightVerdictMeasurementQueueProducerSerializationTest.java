package ru.yandex.market.ff.dbqueue.producer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.PushRightVerdictMeasurementPayload;
import ru.yandex.market.ff.model.dto.dbqueue.ItemIdentifierDTO;

public class PushRightVerdictMeasurementQueueProducerSerializationTest extends IntegrationTest {
    private static final String PAYLOAD_STRING =
            "{\"requestId\":3,\"identifiers\":[{\"supplierId\":3,\"shopSku\":\"sku1\"}," +
                    "{\"supplierId\":3,\"shopSku\":\"sku2\"}]}";

    private static final long REQUEST_ID = 3L;

    @Autowired
    private PushRightVerdictMeasurementQueueProducer producer;

    @Test
    public void testSerializeWorks() {
        PushRightVerdictMeasurementPayload payload = new PushRightVerdictMeasurementPayload(REQUEST_ID,
                ImmutableList.of(
                        new ItemIdentifierDTO(3L, "sku1"),
                        new ItemIdentifierDTO(3L, "sku2")
                )
        );

        String payloadString = producer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }
}
