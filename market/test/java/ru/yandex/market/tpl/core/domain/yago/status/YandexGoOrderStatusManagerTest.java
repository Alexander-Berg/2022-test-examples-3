package ru.yandex.market.tpl.core.domain.yago.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType.COURIER_FOUND;
import static ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType.COURIER_SEARCH;

@ExtendWith(MockitoExtension.class)
class YandexGoOrderStatusManagerTest {

    public static final long YANDEXGO_ORDER_ID = 123L;

    @Test
    void shouldExecuteExpectedHandler_whenOnYandexGoOrderStatusUpdatedEvent() {
        // given
        YandexGoOrder order = mockYandexGoOrder(YANDEXGO_ORDER_ID, COURIER_SEARCH);
        YandexGoOrderStatusHandler handler = mockYandexGoOrderStatusHandler(COURIER_SEARCH);
        YandexGoOrderStatusManager manager = new YandexGoOrderStatusManager(List.of(handler));

        // when
        manager.handleNewOrderStatus(order);

        // then
        verify(handler).handle(order);
    }

    @Test
    void shouldNotExecuteHandler_whenOnYandexGoOrderStatusUpdatedEvent_ifNoStatusHandlerExist() {
        // given
        YandexGoOrder order = mockYandexGoOrder(YANDEXGO_ORDER_ID, COURIER_FOUND);
        YandexGoOrderStatusHandler handler = mockYandexGoOrderStatusHandler(COURIER_SEARCH);
        YandexGoOrderStatusManager manager = new YandexGoOrderStatusManager(List.of(handler));

        // when
        manager.handleNewOrderStatus(order);

        // then
        verify(handler).getSupportedStatus();
        verifyNoMoreInteractions(handler);
    }


    private YandexGoOrder mockYandexGoOrder(long yandexgoOrderId, OrderStatusType status) {
        YandexGoOrder order = mock(YandexGoOrder.class);
        when(order.getStatus()).thenReturn(status);
        return order;
    }

    private YandexGoOrderStatusHandler mockYandexGoOrderStatusHandler(OrderStatusType status) {
        YandexGoOrderStatusHandler handler = mock(YandexGoOrderStatusHandler.class);
        when(handler.getSupportedStatus()).thenReturn(status);
        return handler;
    }

}
