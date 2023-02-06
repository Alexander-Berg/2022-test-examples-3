package ru.yandex.market.wms.servicebus.async.service;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.MessageHeaders;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushCarrierStateRequest;
import ru.yandex.market.wms.servicebus.api.internal.wms.server.service.CarrierStatePusher;
import ru.yandex.market.wms.shared.libs.async.jms.DestNamingUtils;

import static org.mockito.Mockito.mock;

class PushCarrierStateAsyncServiceTest {
    private final JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    private final DbConfigService dbConfigService = mock(DbConfigService.class);
    private final CarrierStatePusher carrierStatePusher = mock(CarrierStatePusher.class);

    private final PushCarrierStateAsyncService service = new PushCarrierStateAsyncService(
        jmsTemplate,
        dbConfigService,
        carrierStatePusher
    );

    @Test
    void testRequestProcessing() {
        Mockito.when(dbConfigService.getConfigAsBoolean("ENABLE_PUSH_CARRIER_STATE"))
            .thenReturn(true);

        service.processRequest(
            PushCarrierStateRequest.builder().carrierCode("OLOLO").build(),
            new MessageHeaders(Collections.emptyMap())
        );

        Mockito.verify(carrierStatePusher).push("OLOLO");
    }

    @Test
    void testSendStatus() {
        service.sendPushOrderStatus(PushCarrierStateRequest.builder().carrierCode("OLOLO2").build());
        Mockito.verify(jmsTemplate).convertAndSend(
            Mockito.eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "pushCarrierState"),
            Mockito.eq(PushCarrierStateRequest.builder().carrierCode("OLOLO2").build()),
            Mockito.any()
        );
    }
}
