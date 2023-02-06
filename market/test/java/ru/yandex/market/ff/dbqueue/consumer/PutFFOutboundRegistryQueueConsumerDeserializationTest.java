package ru.yandex.market.ff.dbqueue.consumer;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.PutFFOutboundRegistryPayload;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundRegistry;

public class PutFFOutboundRegistryQueueConsumerDeserializationTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private PutFFOutboundRegistryQueueConsumer putFFOutboundRegistryQueueConsumer;

    @Test
    public void testDeserializationWorks() throws IOException {
        PutFFOutboundRegistryPayload payload = putFFOutboundRegistryQueueConsumer.getPayloadTransformer()
                .toObject(FileContentUtils.getFileContent("consumer/put_ff_outbound_registry_payload.json"));
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(3L);

        OutboundRegistry outboundRegistry = payload.getOutboundRegistry();
        assertions.assertThat(outboundRegistry.getOutboundId().getYandexId()).isEqualTo("1");
        assertions.assertThat(outboundRegistry.getRegistryId().getYandexId()).isEqualTo("3");
        assertions.assertThat(outboundRegistry.getRegistryType()).isEqualTo(RegistryType.PLANNED);
        assertions.assertThat(outboundRegistry.getComment()).isEqualTo("Comment");
        assertions.assertThat(outboundRegistry.getBoxes().size()).isEqualTo(1);
        assertions.assertThat(outboundRegistry.getPallets().size()).isEqualTo(1);
        assertions.assertThat(outboundRegistry.getItems().size()).isEqualTo(1);
    }
}
