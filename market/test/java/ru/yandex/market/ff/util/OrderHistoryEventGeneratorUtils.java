package ru.yandex.market.ff.util;

import java.time.LocalDateTime;
import java.util.Date;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;

public final class OrderHistoryEventGeneratorUtils {

    public static final long ORDER_ID = 1;
    public static final long EVENT_ID = 2;
    public static final LocalDateTime PUBLISH_DATE_TIME =
        LocalDateTime.of(2019, 9, 30, 13, 18, 20);
    public static final Date TRANSACTION_DATE = Date.from(PUBLISH_DATE_TIME.toInstant(TimeZoneUtil.DEFAULT_OFFSET));

    private OrderHistoryEventGeneratorUtils() {
        throw new AssertionError();
    }

    @Nonnull
    public static OrderHistoryEvent createOrderHistoryEventForCreatedOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PROCESSING);
        return createOrderStatusUpdatedEvent(order);
    }

    @Nonnull
    public static OrderHistoryEvent createOrderHistoryEventForCancelOrderFirstType() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.CANCELLED);
        return createOrderStatusUpdatedEvent(order);
    }

    @Nonnull
    public static OrderHistoryEvent createOrderHistoryEventForCancelOrderSecondType() {
        Order order = new Order();
        order.setId(ORDER_ID);
        return createOrderCancellationRequestedEvent(order);
    }

    @Nonnull
    public static OrderHistoryEvent createOrderHistoryEventForUpdateOrderShipmentDate() {
        Order order = new Order();
        order.setId(ORDER_ID);
        return createOrderDeliveryUpdatedEvent(order);
    }

    @Nonnull
    public static OrderHistoryEvent createUnknownOrderHistoryEvent() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setType(HistoryEventType.UNKNOWN);
        Order order = new Order();
        order.setId(ORDER_ID);
        orderHistoryEvent.setOrderAfter(order);
        return orderHistoryEvent;
    }

    @Nonnull
    public static OrderHistoryEvent createOrderStatusUpdatedEvent(@Nonnull Order order) {
        return createEvent(order, HistoryEventType.ORDER_STATUS_UPDATED);
    }

    @Nonnull
    public static OrderHistoryEvent createOrderCancellationRequestedEvent(@Nonnull Order order) {
        return createEvent(order, HistoryEventType.ORDER_CANCELLATION_REQUESTED);
    }

    @Nonnull
    public static OrderHistoryEvent createOrderDeliveryUpdatedEvent(@Nonnull Order order) {
        return createEvent(order, HistoryEventType.ORDER_DELIVERY_UPDATED);
    }

    @Nonnull
    private static OrderHistoryEvent createEvent(@Nonnull Order order, @Nonnull HistoryEventType type) {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(EVENT_ID);
        orderHistoryEvent.setType(type);
        orderHistoryEvent.setOrderAfter(order);
        orderHistoryEvent.setTranDate(TRANSACTION_DATE);
        return orderHistoryEvent;
    }
}
