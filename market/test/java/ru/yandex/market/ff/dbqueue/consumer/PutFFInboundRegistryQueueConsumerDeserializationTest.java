package ru.yandex.market.ff.dbqueue.consumer;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.PutFFInboundRegistryPayload;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundRegistry;

public class PutFFInboundRegistryQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private PutFFInboundRegistryQueueConsumer putFFInboundRegistryQueueConsumer;

    @Test
    public void testDeserializationWorks() throws IOException {
        PutFFInboundRegistryPayload payload = putFFInboundRegistryQueueConsumer.getPayloadTransformer()
                .toObject(FileContentUtils.getFileContent("consumer/put_ff_inbound_registry_payload.json"));
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(3L);

        InboundRegistry inboundRegistry = payload.getInboundRegistry();
        assertions.assertThat(inboundRegistry.getInboundId().getYandexId()).isEqualTo("1");
        assertions.assertThat(inboundRegistry.getRegistryId().getYandexId()).isEqualTo("3");
        assertions.assertThat(inboundRegistry.getRegistryType()).isEqualTo(RegistryType.PLANNED);
        assertions.assertThat(inboundRegistry.getComment()).isEqualTo("Comment");
        assertions.assertThat(inboundRegistry.getBoxes().size()).isEqualTo(1);
        assertions.assertThat(inboundRegistry.getPallets().size()).isEqualTo(1);
        assertions.assertThat(inboundRegistry.getItems().size()).isEqualTo(1);
    }
}
