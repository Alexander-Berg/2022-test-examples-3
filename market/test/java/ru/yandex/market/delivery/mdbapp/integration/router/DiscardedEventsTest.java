package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;

@RunWith(Parameterized.class)
public class DiscardedEventsTest {
    @Parameterized.Parameter
    public OrderHistoryEvent orderEvent;
    @Parameterized.Parameter(1)
    public boolean isAcceptFakeEvent;
    @Parameterized.Parameter(2)
    public String caseName;

    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{getEmptyEvent(), false, "empty event"});
        parameters.add(new Object[]{getFakeEvent(), true, "fake event first"});
        parameters.add(new Object[]{getFakeEvent(), false, "fake event second"});
        parameters.add(new Object[] {getEventWithShopDeliveryType(), true, "not Market partner delivery"});
        parameters.add(new Object[] {getEventWithUnknownDeliveryType(), true, "not Market partner delivery"});

        return parameters;
    }

    @Nonnull
    private static OrderHistoryEvent getEmptyEvent() {
        return new OrderHistoryEvent();
    }

    @Nonnull
    private static OrderHistoryEvent getFakeEvent() {
        OrderHistoryEvent fakeEvent = new OrderHistoryEvent();
        fakeEvent.setOrderAfter(new Order());
        fakeEvent.getOrderAfter().setFake(true);
        return fakeEvent;
    }

    @Nonnull
    private static OrderHistoryEvent getEventWithShopDeliveryType() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderAfter(new Order());
        orderHistoryEvent.getOrderAfter().setFake(false);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        orderHistoryEvent.getOrderAfter().setDelivery(delivery);

        return orderHistoryEvent;
    }

     @Nonnull
    private static OrderHistoryEvent getEventWithUnknownDeliveryType() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderAfter(new Order());
        orderHistoryEvent.getOrderAfter().setFake(false);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.UNKNOWN);
        orderHistoryEvent.getOrderAfter().setDelivery(delivery);

        return orderHistoryEvent;
    }

    @Test
    public void discardedNotFakeTest() {
        OrderEventsByTypeRouter orderEventsRouter = new OrderEventsByTypeRouter(
            new FeatureProperties().setAcceptFakeOrderEventsEnabled(true)
        );
        Assert.assertEquals(
            "Unexpected task channel" + caseName,
            OrderEventsByTypeRouter.CHANNEL_DISCARDED,
            orderEventsRouter.route(orderEvent)
        );
    }
}
