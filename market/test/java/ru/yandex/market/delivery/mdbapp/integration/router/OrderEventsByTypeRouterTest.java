package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.components.util.DeliveryServices;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class OrderEventsByTypeRouterTest {
    private static final Long DSBS_DELIVERY_SERVICE_ID = 9999L;

    @Parameter
    public OrderHistoryEvent orderEvent;

    @Parameter(1)
    public String expectedChannel;

    private final OrderEventsByTypeRouter router = new OrderEventsByTypeRouter(new FeatureProperties());

    @Parameters
    public static Collection<Object[]> data() {

        return Arrays.asList(new Object[][]{
            {
                createEvent(true, DeliveryPartnerType.SHOP),
                OrderEventsByTypeRouter.CHANNEL_STANDARD
            },
            {
                createEvent(true, DeliveryPartnerType.YANDEX_MARKET),
                OrderEventsByTypeRouter.CHANNEL_STANDARD
            },
            {
                createEvent(false, DeliveryPartnerType.YANDEX_MARKET),
                OrderEventsByTypeRouter.CHANNEL_CROSS_DOCK
            },
            {
                createEvent(false, DeliveryPartnerType.YANDEX_MARKET),
                OrderEventsByTypeRouter.CHANNEL_CROSS_DOCK
            },
            {
                createEvent(
                    createGlobalOrder(
                        createOrder(true, DeliveryPartnerType.YANDEX_MARKET)
                    )
                ),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                createEvent(Color.BLUE, true, DeliveryPartnerType.YANDEX_MARKET, DeliveryType.POST),
                OrderEventsByTypeRouter.CHANNEL_STANDARD
            },
            {
                createPostEvent(),
                OrderEventsByTypeRouter.CHANNEL_POST
            },
            {
                createEvent(createGoldPartnerOrder()),
                OrderEventsByTypeRouter.CHANNEL_GOLD_PARTNER
            },
            {
                createEvent(Color.BLUE, false, DeliveryPartnerType.YANDEX_MARKET, DeliveryType.DELIVERY),
                OrderEventsByTypeRouter.CHANNEL_BLUE_DROPSHIP
            },
            {
                createEvent(HistoryEventType.ORDER_RETURN_CREATED),
                OrderEventsByTypeRouter.CHANNEL_RETURN_CREATED
            },
            {
                createEvent(HistoryEventType.ORDER_RETURN_STATUS_UPDATED),
                OrderEventsByTypeRouter.CHANNEL_RETURN_STATUS_UPDATED
            },
            {
                createEvent(HistoryEventType.ORDER_RETURN_DELIVERY_UPDATED),
                OrderEventsByTypeRouter.CHANNEL_RETURN_DELIVERY_UPDATED
            },
            {
                createDropshipBySellerOrderEvent(1L, DSBS_DELIVERY_SERVICE_ID),
                OrderEventsByTypeRouter.CHANNEL_DROPSHIP_BY_SELLER
            },
            {
                createEvent(Color.WHITE, false, DeliveryPartnerType.YANDEX_MARKET, DeliveryType.DELIVERY),
                OrderEventsByTypeRouter.CHANNEL_DROPSHIP_BY_SELLER
            }
        });
    }

    @Nonnull
    private static OrderHistoryEvent createDropshipBySellerOrderEvent(
        @Nullable Long deliveryServiceIdBefore,
        @Nullable Long deliveryServiceIdAfter
    ) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_DELIVERY_UPDATED);

        Order orderBefore = OrderSteps.getNotFakeOrder();
        orderBefore.getDelivery().setDeliveryServiceId(deliveryServiceIdBefore);
        orderBefore.setRgb(Color.WHITE);
        event.setOrderBefore(orderBefore);

        Order orderAfter = OrderSteps.getNotFakeOrder();
        orderAfter.getDelivery().setDeliveryServiceId(deliveryServiceIdAfter);
        orderAfter.setRgb(Color.WHITE);
        event.setOrderAfter(orderAfter);

        return event;
    }

    private static Order createGoldPartnerOrder() {
        final Order order = OrderSteps.getNotFakeOrder();
        order.setFulfilment(false);
        order.setRgb(Color.BLUE);
        Delivery delivery = new Delivery();
        order.setStatus(OrderStatus.PROCESSING);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setDeliveryServiceId(OrderEventsByTypeRouter.SELF_DELIVERY_SERVICE);
        order.setDelivery(delivery);

        return order;
    }

    @Test
    public void testRouting() {
        assertEquals(
            "Result channel is not correct",
            expectedChannel,
            router.route(orderEvent)
        );
    }

    @Nonnull
    private static OrderHistoryEvent createEvent(boolean isFulfillment, DeliveryPartnerType deliveryPartnerType) {
        final OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        orderEvent.setOrderBefore(createOrder(isFulfillment, deliveryPartnerType));
        orderEvent.setOrderAfter(createOrder(isFulfillment, deliveryPartnerType));
        return orderEvent;
    }

    @Nonnull
    private static OrderHistoryEvent createEvent(Order order) {
        final OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        orderEvent.setOrderBefore(order);
        orderEvent.setOrderAfter(order);
        return orderEvent;
    }

    @Nonnull
    private static OrderHistoryEvent createEvent(
        Color color,
        boolean isFulfillment,
        DeliveryPartnerType deliveryPartnerType,
        DeliveryType type
    ) {
        OrderHistoryEvent orderEvent = createEvent(isFulfillment, deliveryPartnerType);

        orderEvent.setOrderBefore(createOrder(color, isFulfillment, deliveryPartnerType, type));
        orderEvent.setOrderAfter(createOrder(color, isFulfillment, deliveryPartnerType, type));
        return orderEvent;
    }

    @Nonnull
    private static Order createGlobalOrder(@Nonnull Order order) {
        order.setGlobal(true);
        return order;
    }

    @Nonnull
    private static Order createOrder(boolean isFulfillment, DeliveryPartnerType deliveryPartnerType) {
        final Order order = OrderSteps.getNotFakeOrder();
        order.setFulfilment(isFulfillment);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(deliveryPartnerType);

        order.setDelivery(delivery);

        return order;
    }

    @Nonnull
    private static Order createOrder(
        Color color,
        Boolean isFulfillment,
        DeliveryPartnerType deliveryPartnerType,
        DeliveryType type
    ) {
        Order order = createOrder(isFulfillment, deliveryPartnerType);
        order.setRgb(color);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(deliveryPartnerType);
        delivery.setType(type);
        order.setDelivery(delivery);

        return order;
    }

    @Nonnull
    private static OrderHistoryEvent createPostEvent() {
        OrderHistoryEvent orderEvent = createEvent(true, DeliveryPartnerType.YANDEX_MARKET);
        orderEvent.setOrderBefore(createPostOrder());
        orderEvent.setOrderAfter(createPostOrder());
        return orderEvent;
    }

    @Nonnull
    private static Order createPostOrder() {
        Order order = createOrder(Color.BLUE, true, DeliveryPartnerType.YANDEX_MARKET, DeliveryType.POST);
        order.getDelivery().setDeliveryServiceId(DeliveryServices.MARSCHROUTE_POST_DELIVERY_SERVICE_ID);
        return order;
    }

    @Nonnull
    private static OrderHistoryEvent createEvent(final HistoryEventType eventType) {
        final OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        orderEvent.setType(eventType);
        return orderEvent;
    }

}
