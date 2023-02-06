package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import steps.orderSteps.OrderSteps;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrderMilestoneTimingsTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.service.InternalVariableService;

import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class GetLabelsEventsTest {
    @Parameterized.Parameter
    public OrderHistoryEvent orderEvent;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{getLabelsEvent()});

        return parameters;
    }

    @Nonnull
    private static OrderHistoryEvent getLabelsEvent() {
        OrderHistoryEvent labelsEvent = new OrderHistoryEvent();

        Order orderAfter = OrderSteps.getNotFakeOrder();
        Order orderBefore = OrderSteps.getNotFakeOrder();

        orderBefore.getDelivery().getParcels().get(0).setStatus(ParcelStatus.NEW);
        orderAfter.getDelivery().getParcels().get(0).setStatus(ParcelStatus.CREATED);

        labelsEvent.setOrderBefore(orderBefore);
        labelsEvent.setOrderAfter(orderAfter);

        return labelsEvent;
    }

    @Test
    //TODO тест проверяет что выпиливание ярлыков в синем флоу сработало, выпилить, когда убедимся, что все ок
    public void getLabelsTest() {
        BackLogOrderMilestoneTimingsTskvLogger tskvLogger = new BackLogOrderMilestoneTimingsTskvLogger(
            new EventFlowParametersHolder(),
            new TestableClock()
        );
        OrderEventsRouter orderEventsRouter = new OrderEventsRouter(tskvLogger, mock(InternalVariableService.class));
        Assert.assertEquals(
            "Unexpected task channel",
            OrderEventsByTypeRouter.CHANNEL_DISCARDED,
            orderEventsRouter.route(orderEvent)
        );
    }
}
