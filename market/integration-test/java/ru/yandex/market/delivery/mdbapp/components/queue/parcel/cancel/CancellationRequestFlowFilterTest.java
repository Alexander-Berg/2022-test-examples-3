package ru.yandex.market.delivery.mdbapp.components.queue.parcel.cancel;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Test;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.integration.filter.ChangeRequestEventFilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CancellationRequestFlowFilterTest {

    private final ChangeRequestEventFilter changeRequestEventFilter = new ChangeRequestEventFilter();

    /**
     * Обработка changeRequest с отменой для НЕотменённого заказа.
     */
    @Test
    public void filteringChangeRequestLomOrderTrueTest() {
        OrderHistoryEvent event = createEvent();
        assertTrue(changeRequestEventFilter.filter(event));
    }

    /**
     * Пропуск changeRequest с отменой для отменённого заказа.
     */
    @Test
    public void filteringChangeRequestPostamatFlowLomOrderTrueTest() {
        OrderHistoryEvent event = createEventForCancelledOrder();
        assertFalse(changeRequestEventFilter.filter(event));
    }

    @Nonnull
    private OrderHistoryEvent createEvent() {
        Order order = OrderEventSteps.buildBeruDropshipOrderWithCancelRequest(1L, 1L, 1L, 1L);
        order.setFulfilment(true);
        order.setChangeRequests(List.of(OrderEventSteps.createCancelChangeRequest()));
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);
        event.setOrderAfter(order);
        return event;
    }

    @Nonnull
    private OrderHistoryEvent createEventForCancelledOrder() {
        OrderHistoryEvent event = createEvent();
        event.getOrderAfter().setStatus(OrderStatus.CANCELLED);
        return event;
    }
}
