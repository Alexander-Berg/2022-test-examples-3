package ru.yandex.market.wms.servicebus.async.service.consumer;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.iris.client.api.PushApiClient;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.ShelfLifesDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.UnitIdDto;
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushMeasurementShelfLifesRequest;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.shared.libs.async.jms.DestNamingUtils;
import ru.yandex.market.wms.shared.libs.business.logger.BusinessLogger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

public class PushShelflifesConsumerTest extends IntegrationTest {

    @SpyBean
    @Autowired
    private PushApiClient pushApiClient;

    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @SpyBean
    @Autowired
    BusinessLogger businessLogger;

    @Test
    public void receivePayload() {

        String queue = DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "shelflifes";
        PushMeasurementShelfLifesRequest queuedRequest = PushMeasurementShelfLifesRequest.builder()
                .shelfLifes(
                        List.of(
                                ShelfLifesDto.builder()
                                        .unitId(UnitIdDto.builder()
                                                .article("MAN_0359")
                                                .vendorId(465852L)
                                                .id("MAN_0359")
                                                .build())
                                        .shelfLife(111)
                                        .operatorId("TEST")
                                        .build()))
                .build();

        doAnswer((Answer<Void>) invocation -> null).when(pushApiClient).pushMeasurementShelfLifes(any());

        defaultJmsTemplate.convertAndSend(queue, queuedRequest);

        ArgumentCaptor<ru.yandex.market.logistics.iris.client.model.request.PushMeasurementShelfLifesRequest>
                requestCaptor = ArgumentCaptor.forClass(
                ru.yandex.market.logistics.iris.client.model.request.PushMeasurementShelfLifesRequest.class);

        Mockito.verify(pushApiClient, Mockito.timeout(1000).times(1))
                .pushMeasurementShelfLifes(requestCaptor.capture());
        Mockito.verify(businessLogger, Mockito.timeout(1000).times(1))
                .info(anyString(), anyString(), anyString(), any());

        assertions.assertThat(requestCaptor.getValue().getWarehouseId()).isEqualTo(0);
        assertions.assertThat(requestCaptor.getValue().getShelfLifes()).isNotEmpty();
        assertions.assertThat(requestCaptor.getValue().getShelfLifes().get(0).getShelfLife()).isEqualTo(111);
    }
}
