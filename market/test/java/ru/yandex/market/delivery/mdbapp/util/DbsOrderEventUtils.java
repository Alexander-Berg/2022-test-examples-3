package ru.yandex.market.delivery.mdbapp.util;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;

@ParametersAreNonnullByDefault
public class DbsOrderEventUtils {

    private static final Date TRAN_DATE = Date.from(Instant.parse("2019-07-20T00:00:00Z"));
    private static final long DELIVERY_SERVICE_ID = 100L;

    private DbsOrderEventUtils() {
    }

    @Nonnull
    public static OrderHistoryEvent event(
        HistoryEventType historyEventType,
        @Nullable Long deliveryServiceIdBefore,
        @Nullable Long deliveryServiceIdAfter
    ) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(historyEventType);

        event.setOrderBefore(orderBefore(deliveryServiceIdBefore));
        event.setOrderAfter(orderAfter(deliveryServiceIdAfter));
        event.setTranDate(TRAN_DATE);

        return event;
    }

    @Nonnull
    public static OrderHistoryEvent eventDbsWithRoute(
        HistoryEventType historyEventType,
        OrderStatus orderStatusAfter,
        OrderSubstatus orderSubstatusAfter
    ) {
        OrderHistoryEvent event = event(historyEventType, orderStatusAfter, orderSubstatusAfter);
        Order orderAfter = event.getOrderAfter();
        orderAfter.getDelivery().setMarketBranded(false);
        orderAfter.getDelivery().setFeatures(Set.of(DeliveryFeature.DBS_WITH_ROUTE));
        event.setTranDate(TRAN_DATE);

        return event;
    }

    @Nonnull
    public static OrderHistoryEvent event(HistoryEventType historyEventType, Order orderAfter) {
        return event(historyEventType, orderBefore(), orderAfter);
    }

    @Nonnull
    public static OrderHistoryEvent event(HistoryEventType historyEventType, Order orderBefore, Order orderAfter) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(historyEventType);

        event.setOrderBefore(orderBefore);
        event.setOrderAfter(orderAfter);
        event.setTranDate(TRAN_DATE);

        return event;
    }

    @Nonnull
    public static OrderHistoryEvent eventWithNotBrandedPvz(HistoryEventType historyEventType) {
        Order orderAfter = orderAfter(100L);
        orderAfter.getDelivery().setMarketBranded(false);

        return event(historyEventType, orderAfter);
    }

    @Nonnull
    public static OrderHistoryEvent event(HistoryEventType historyEventType, OrderSubstatus orderSubstatusAfter) {
        Order orderBefore = orderBefore(null);
        orderBefore.setSubstatus(OrderSubstatus.UNKNOWN);
        Order orderAfter = orderAfter(100L);
        orderAfter.setSubstatus(orderSubstatusAfter);

        return event(historyEventType, orderBefore, orderAfter);
    }

    @Nonnull
    public static OrderHistoryEvent event(
        HistoryEventType historyEventType,
        OrderStatus orderStatusBefore,
        OrderStatus orderStatusAfter,
        List<ChangeRequest> changeRequests
    ) {
        OrderHistoryEvent event = event(historyEventType, orderStatusBefore, orderStatusAfter);
        event.getOrderAfter().setChangeRequests(changeRequests);
        event.setTranDate(TRAN_DATE);

        return event;
    }

    @Nonnull
    public static OrderHistoryEvent event(
        HistoryEventType historyEventType,
        OrderStatus orderStatusBefore,
        OrderStatus orderStatusAfter
    ) {
        Order orderBefore = orderBefore();
        orderBefore.setStatus(orderStatusBefore);

        Order orderAfter = orderAfter(DELIVERY_SERVICE_ID);
        orderAfter.setStatus(orderStatusAfter);

        return event(historyEventType, orderBefore, orderAfter);
    }

    @Nonnull
    public static OrderHistoryEvent event(HistoryEventType historyEventType, OrderStatus orderStatusAfter) {
        return event(historyEventType, OrderStatus.UNKNOWN, orderStatusAfter);
    }

    @Nonnull
    public static OrderHistoryEvent event(
        HistoryEventType historyEventType,
        OrderStatus orderStatusAfter,
        OrderSubstatus orderSubstatusAfter
    ) {
        Order orderBefore = orderBefore(null);
        orderBefore.setSubstatus(OrderSubstatus.ANTIFRAUD);
        orderBefore.setStatus(OrderStatus.UNKNOWN);

        Order orderAfter = orderAfter(100L);
        orderAfter.setSubstatus(orderSubstatusAfter);
        orderAfter.setStatus(orderStatusAfter);

        return event(historyEventType, orderBefore, orderAfter);
    }

    @Nonnull
    public static Order orderAfter(Color color) {
        return order(
            DELIVERY_SERVICE_ID,
            color,
            OrderStatus.PENDING,
            OrderSubstatus.AWAIT_CONFIRMATION,
            DeliveryPartnerType.SHOP
        );
    }

    @Nonnull
    public static Order orderAfter(DeliveryPartnerType partnerType) {
        return order(
            DELIVERY_SERVICE_ID,
            Color.WHITE,
            OrderStatus.PENDING,
            OrderSubstatus.AWAIT_CONFIRMATION,
            partnerType
        );
    }

    @Nonnull
    public static Order orderBefore() {
        return orderBefore(null);
    }

    @Nonnull
    public static Order orderBefore(@Nullable Long deliveryServiceId) {
        return order(
            deliveryServiceId,
            Color.BLUE,
            OrderStatus.PICKUP,
            OrderSubstatus.UNKNOWN,
            DeliveryPartnerType.YANDEX_MARKET
        );
    }

    @Nonnull
    public static Order orderAfter(@Nullable Long deliveryServiceId) {
        return order(
            deliveryServiceId,
            Color.WHITE,
            OrderStatus.PENDING,
            OrderSubstatus.AWAIT_CONFIRMATION,
            DeliveryPartnerType.SHOP
        );
    }

    @Nonnull
    public static Order order(
        @Nullable Long deliveryServiceId,
        Color color,
        OrderStatus orderStatus,
        OrderSubstatus orderSubstatus,
        DeliveryPartnerType deliveryPartnerType
    ) {
        Order order = OrderSteps.getFilledOrder();

        order.setRgb(color);
        order.setFulfilment(false);
        order.setStatus(orderStatus);
        order.setAcceptMethod(OrderAcceptMethod.DEFAULT);
        order.setSubstatus(orderSubstatus);

        Delivery delivery = order.getDelivery();
        delivery.setDeliveryPartnerType(deliveryPartnerType);
        delivery.setParcels(Collections.emptyList());
        delivery.setDeliveryServiceId(deliveryServiceId);
        delivery.setMarketBranded(true);

        return order;
    }
}
