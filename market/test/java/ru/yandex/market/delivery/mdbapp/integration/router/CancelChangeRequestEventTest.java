package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrderMilestoneTimingsTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.service.InternalVariableService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_CHANGE_REQUEST_CREATED;

@RunWith(Parameterized.class)
public class CancelChangeRequestEventTest {

    BackLogOrderMilestoneTimingsTskvLogger tskvLogger = new BackLogOrderMilestoneTimingsTskvLogger(
        new EventFlowParametersHolder(),
        new TestableClock()
    );
    private final OrderEventsRouter orderEventsRouter = new OrderEventsRouter(
        tskvLogger,
        mock(InternalVariableService.class)
    );

    @Parameterized.Parameter
    public OrderHistoryEvent orderHistoryEvent;

    @Parameterized.Parameter(1)
    public String channel;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
            {
                cancelChangeRequestCreatedEvent(),
                CHANNEL_CHANGE_REQUEST_CREATED
            }
        });
    }

    @Nonnull
    private static OrderHistoryEvent cancelChangeRequestCreatedEvent() {
        Order order = OrderEventSteps.buildBeruDropshipOrderWithCancelRequest(1L, 1L, 1L, 1L);
        order.setFulfilment(false);
        order.getDelivery().setTariffId(1L);
        order.setChangeRequests(List.of(OrderEventSteps.createCancelChangeRequest()));
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);
        event.setOrderAfter(order);
        return event;
    }

    @Test
    public void routeTest() {
        assertEquals("Routing is correct", channel, orderEventsRouter.route(orderHistoryEvent));
    }

}
