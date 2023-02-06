package ru.yandex.market.delivery.mdbapp.integration.router;

import org.junit.Test;
import steps.orderSteps.OrderSteps;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrderMilestoneTimingsTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.service.InternalVariableService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsByTypeRouter.CHANNEL_DISCARDED;

public class CanceledOrderTrackTest {

    BackLogOrderMilestoneTimingsTskvLogger tskvLogger = new BackLogOrderMilestoneTimingsTskvLogger(
        new EventFlowParametersHolder(),
        new TestableClock()
    );
    private final OrderEventsRouter router = new OrderEventsRouter(tskvLogger, mock(InternalVariableService.class));

    @Test
    public void routedToDiscardedAfterTrackWasAdded() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        orderEvent.setOrderBefore(new Order());
        orderEvent.setOrderAfter(OrderSteps.getNotFakeOrder());
        orderEvent.getOrderBefore().setStatus(OrderStatus.CANCELLED);
        orderEvent.getOrderAfter().setStatus(OrderStatus.CANCELLED);

        assertEquals("Wrong route", CHANNEL_DISCARDED, router.route(orderEvent));
    }

    @Test
    public void discardedCanceledOrderEventWithoutTrack() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        orderEvent.setOrderBefore(new Order());
        orderEvent.setOrderAfter(new Order());
        orderEvent.getOrderBefore().setStatus(OrderStatus.CANCELLED);
        orderEvent.getOrderAfter().setStatus(OrderStatus.CANCELLED);

        assertEquals("Wrong route", CHANNEL_DISCARDED, router.route(orderEvent));
    }

    @Test
    public void discardedCanceledOrderEventWithTrackAlreadySet() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        orderEvent.setOrderBefore(OrderSteps.getNotFakeOrder());
        orderEvent.setOrderAfter(OrderSteps.getNotFakeOrder());
        orderEvent.getOrderBefore().setStatus(OrderStatus.CANCELLED);
        orderEvent.getOrderAfter().setStatus(OrderStatus.CANCELLED);

        assertEquals("Wrong route", CHANNEL_DISCARDED, router.route(orderEvent));
    }
}
