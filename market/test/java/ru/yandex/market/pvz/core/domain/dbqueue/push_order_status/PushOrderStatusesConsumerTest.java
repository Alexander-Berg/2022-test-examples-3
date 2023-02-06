package ru.yandex.market.pvz.core.domain.dbqueue.push_order_status;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.tpl.common.ds.client.DeliveryClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PushOrderStatusesConsumerTest {

    public static final String YANDEX_ID = "23748069";
    public static final String YANDEX_ID_2 = "23769423";
    private final PushOrderStatusesConsumer consumer;

    @Test
    void shouldTransform() {
        String taskFromDb = "{\n" +
                "  \"requestId\": \"1597146387921/d470157e2ccf53ebf829254152dfa8c5/5\",\n" +
                "  \"orderIds\": [\n" +
                "    {\n" +
                "      \"yandexId\": \"" + YANDEX_ID + "\",\n" +
                "      \"deliveryId\": \"1234\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"yandexId\": \"" + YANDEX_ID_2 + "\",\n" +
                "      \"deliveryId\": \"3602\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"token\": \"XXX\",\n" +
                "  \"entityId\": [\n" +
                "    23748069,\n" +
                "    23769423\n" +
                "  ]\n" +
                "}";
        PushOrdersStatusesPayload payload = consumer.getPayloadTransformer().toObject(taskFromDb);
        assertThat(payload.getOrderIds())
                .extracting(DeliveryClient.DsOrderID::getYandexId)
                .containsExactly(YANDEX_ID, YANDEX_ID_2);
    }
}
