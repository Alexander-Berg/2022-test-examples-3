package ru.yandex.market.wms.servicebus.async.service.consumer;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.cte.client.FulfillmentCteClientApi;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemUpdateBatchRequestDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemUpdateRequestDTO;
import ru.yandex.market.logistics.iris.client.api.PushApiClient;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.shared.libs.async.jms.DestNamingUtils;

public class UpdateResupplyItemsConsumerTest extends IntegrationTest {

    @Autowired
    PushApiClient pushApiClient;

    @MockBean
    @Autowired
    private FulfillmentCteClientApi cteClient;

    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @Test
    public void receivePayload() {

        String queue = DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "resupplyItems";

        SupplyItemUpdateBatchRequestDTO queuedRequest = new SupplyItemUpdateBatchRequestDTO(123L,
                List.of(new SupplyItemUpdateRequestDTO("uuid", "uit")));

        defaultJmsTemplate.convertAndSend(queue, queuedRequest);

        ArgumentCaptor<SupplyItemUpdateBatchRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(SupplyItemUpdateBatchRequestDTO.class);

        Mockito.verify(cteClient, Mockito.timeout(1000).times(1))
                .updateResupplyItems(requestCaptor.capture());

        assertions.assertThat(requestCaptor.getValue().getSupplyItemUpdates()).isNotEmpty();
        assertions.assertThat(requestCaptor.getValue().getSupplyItemUpdates().get(0).getUit()).isEqualTo("uit");
    }
}
