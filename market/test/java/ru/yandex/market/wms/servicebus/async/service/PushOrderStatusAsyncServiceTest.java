package ru.yandex.market.wms.servicebus.async.service;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.OrderDto;
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushOrdersStatusesChangedRequest;
import ru.yandex.market.wms.servicebus.api.internal.wms.server.service.OrderStatusService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushOrderStatusAsyncServiceTest {

    private final OrderStatusService orderStatusService = mock(OrderStatusService.class);
    private final JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    private final DbConfigService dbConfigService = mock(DbConfigService.class);
    private final PushOrderStatusAsyncService pushService = new PushOrderStatusAsyncService(
            jmsTemplate,
            orderStatusService,
            dbConfigService
    );

    @AfterEach
    void tearDown() {
        Mockito.reset(orderStatusService, jmsTemplate, dbConfigService);
    }

    @Test
    void checkPushOrdersStatusesChangedWithoutHistoryWhenPushOrdersStatusesWithHistoryPropertyIsFalse() {
        when(dbConfigService.getConfigAsBoolean("YM_ENABLE_PSH_ORD_ST_WTH_HIST")).thenReturn(false);
        Iterable<OrderDto> orders = createOrders();
        PushOrdersStatusesChangedRequest request = createRequest(orders);
        pushService.receivePushOrderStatus(request, any());
        verify(orderStatusService, times(1)).pushOrdersStatusesChanged(orders);
        verify(orderStatusService, times(0)).pushOrdersStatusesChangedWithHistory(orders);
    }

    @Test
    void checkPushOrdersStatusesChangedWithHistoryWhenPushOrdersStatusesWithHistoryPropertyIsTrue() {
        when(dbConfigService.getConfigAsBoolean("YM_ENABLE_PSH_ORD_ST_WTH_HIST")).thenReturn(true);
        Iterable<OrderDto> orders = createOrders();
        PushOrdersStatusesChangedRequest request = createRequest(orders);
        pushService.receivePushOrderStatus(request, any());
        verify(orderStatusService, times(0)).pushOrdersStatusesChanged(orders);
        verify(orderStatusService, times(1)).pushOrdersStatusesChangedWithHistory(orders);
    }

    private static Iterable<OrderDto> createOrders() {
        return List.of(
            OrderDto.builder()
                .orderKey("test")
                .externOrderKey("test")
                .build()
        );
    }

    private static PushOrdersStatusesChangedRequest createRequest(Iterable<OrderDto> orders) {
        return PushOrdersStatusesChangedRequest.builder()
            .orders(orders)
            .build();
    }
}
