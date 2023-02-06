package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import steps.orderSteps.OrderSteps;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrderMilestoneTimingsTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.service.InternalVariableService;

import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class CanceledOrderEventTest {

    @Parameterized.Parameter
    public OrderStatus status;
    OrderHistoryEvent orderEvent;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{OrderStatus.DELIVERED});
        parameters.add(new Object[]{OrderStatus.DELIVERY});
        parameters.add(new Object[]{OrderStatus.PENDING});
        parameters.add(new Object[]{OrderStatus.PICKUP});
        parameters.add(new Object[]{OrderStatus.PLACING});
        parameters.add(new Object[]{OrderStatus.PROCESSING});
        parameters.add(new Object[]{OrderStatus.RESERVED});
        parameters.add(new Object[]{OrderStatus.UNPAID});
        parameters.add(new Object[]{OrderStatus.UNKNOWN});

        return parameters;
    }

    @Before
    public void getOrder() {
        orderEvent = new OrderHistoryEvent();

        orderEvent.setOrderBefore(OrderSteps.getNotFakeOrder());
        orderEvent.setOrderAfter(OrderSteps.getNotFakeOrder());

        orderEvent.getOrderBefore().setStatus(status);
        orderEvent.getOrderAfter().setStatus(OrderStatus.CANCELLED);
    }

    @Test
    public void cancelOrderEventTest() {
        BackLogOrderMilestoneTimingsTskvLogger tskvLogger = new BackLogOrderMilestoneTimingsTskvLogger(
            new EventFlowParametersHolder(),
            new TestableClock()
        );
        OrderEventsRouter orderEventsRouter = new OrderEventsRouter(tskvLogger, mock(InternalVariableService.class));
        Assert.assertEquals(
            "Unexpected OrderEventsRouter",
            OrderEventsByTypeRouter.CHANNEL_DISCARDED,
            orderEventsRouter.route(orderEvent)
        );
    }
}
