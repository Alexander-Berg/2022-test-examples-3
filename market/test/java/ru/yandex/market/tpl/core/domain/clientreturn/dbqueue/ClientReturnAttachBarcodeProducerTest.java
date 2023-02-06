package ru.yandex.market.tpl.core.domain.clientreturn.dbqueue;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.dbqueue.model.QueueType.CLIENT_RETURN_DELIVERED_TO_SC;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SEND_MESSAGE_UPDATE_LOCKER_ORDER_STATUS;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ClientReturnAttachBarcodeProducerTest extends TplAbstractTest {

    private final ClientReturnDeliveredToScProducer producer;
    private final ObjectMapper objectMapper;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;


    @Test
    void produce() throws IOException {
        when(configurationProviderAdapter.isBooleanEnabled(SEND_MESSAGE_UPDATE_LOCKER_ORDER_STATUS)).thenReturn(true);
        producer.produce(123L, 1651667813, "321");
        String stringPayload = (String) dbQueueTestUtil.getTasks(CLIENT_RETURN_DELIVERED_TO_SC).get(0)
                .getPayloadOrThrow();

        ClientReturnDeliveredToScPayload payload =
                objectMapper.readValue(stringPayload, ClientReturnDeliveredToScPayload.class);
        assertThat(payload.getReturnId()).isEqualTo(123L);
        assertThat(payload.getTimestamp()).isEqualTo(1651667813);
        assertThat(payload.getEventId()).isEqualTo("321");
        assertThat(payload.getEntityId()).isEqualTo("123");
    }
}
