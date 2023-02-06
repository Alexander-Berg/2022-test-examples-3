package ru.yandex.market.ff4shops.api;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.request.OrdersEventsRequest;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public final class OrderHistoryUtil {

    private static final OrderStatus[] ORDER_STATUSES = {OrderStatus.PROCESSING};

    private static final HistoryEventType[] EVENT_TYPES = {
        HistoryEventType.ORDER_STATUS_UPDATED,
        HistoryEventType.ORDER_SUBSTATUS_UPDATED,
        HistoryEventType.ITEMS_UPDATED,
        HistoryEventType.PARCEL_BOXES_CHANGED
    };

    public static final long ORDER_ID = 100500;
    public static final long YANDEX_ID = 1;
    public static final LocalDateTime DATE = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
    public static final List<OrderHistoryEvent> ORDER_HISTORY_EVENTS = List.of(
        OrderHistoryUtil.eventSubstatusUpdated(1, ORDER_ID, DATE, OrderSubstatus.STARTED),
        OrderHistoryUtil.eventSubstatusUpdated(2, ORDER_ID, DATE.plusSeconds(1), OrderSubstatus.PACKAGING),
        OrderHistoryUtil.itemNotFoundEvent(3, ORDER_ID, DATE.plusSeconds(2), OrderSubstatus.PACKAGING),
        OrderHistoryUtil.eventParcelBoxesChanged(4, ORDER_ID, DATE.plusSeconds(3), OrderSubstatus.PACKAGING),
        OrderHistoryUtil.eventSubstatusUpdated(4, ORDER_ID, DATE.plusSeconds(4), OrderSubstatus.READY_TO_SHIP),
        OrderHistoryUtil.itemInstancesUpdated(5, ORDER_ID, DATE.plusSeconds(5), OrderSubstatus.READY_TO_SHIP),
        OrderHistoryUtil.itemNotFoundEvent(6, ORDER_ID, DATE.plusSeconds(6), OrderSubstatus.READY_TO_SHIP),
        OrderHistoryUtil.eventParcelBoxesChanged(7, ORDER_ID, DATE.plusSeconds(7), OrderSubstatus.READY_TO_SHIP),
        OrderHistoryUtil.eventParcelBoxesChanged(8, ORDER_ID, DATE.plusSeconds(8), OrderSubstatus.READY_TO_SHIP),
        OrderHistoryUtil.eventSubstatusUpdated(9, ORDER_ID, DATE.plusSeconds(9), OrderSubstatus.SHIPPED)
    );

    private OrderHistoryUtil() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static CheckouterOrderHistoryEventsApi prepareMock(CheckouterAPI checkouterAPI) {
        CheckouterOrderHistoryEventsApi historyEventsApi = mock(CheckouterOrderHistoryEventsApi.class);
        when(checkouterAPI.orderHistoryEvents()).thenReturn(historyEventsApi);
        return historyEventsApi;
    }

    @Nonnull
    public static OrderHistoryEvent eventParcelBoxesChanged(
        long id,
        long orderId,
        LocalDateTime dateTime,
        OrderSubstatus orderSubstatus
    ) {
        return event(
            id,
            orderId,
            dateTime,
            orderSubstatus,
            HistoryEventType.PARCEL_BOXES_CHANGED,
            null
        );
    }

    @Nonnull
    public static OrderHistoryEvent eventSubstatusUpdated(
        long id,
        long orderId,
        LocalDateTime dateTime,
        OrderSubstatus orderSubstatus
    ) {
        return event(
            id,
            orderId,
            dateTime,
            orderSubstatus,
            HistoryEventType.ORDER_SUBSTATUS_UPDATED,
            null
        );
    }

    @Nonnull
    public static OrderHistoryEvent itemInstancesUpdated(
        long id,
        long orderId,
        LocalDateTime dateTime,
        OrderSubstatus orderSubstatus
    ) {
        return event(
            id,
            orderId,
            dateTime,
            orderSubstatus,
            HistoryEventType.ITEMS_UPDATED,
            HistoryEventReason.ITEM_INSTANCES_UPDATED
        );
    }

    @Nonnull
    public static OrderHistoryEvent itemNotFoundEvent(
        long id,
        long orderId,
        LocalDateTime dateTime,
        OrderSubstatus orderSubstatus
    ) {
        return event(
            id,
            orderId,
            dateTime,
            orderSubstatus,
            HistoryEventType.ITEMS_UPDATED,
            HistoryEventReason.ITEMS_NOT_FOUND
        );
    }

    @Nonnull
    private static OrderHistoryEvent event(
        long id,
        long orderId,
        LocalDateTime dateTime,
        OrderSubstatus orderSubstatus,
        HistoryEventType eventType,
        @Nullable HistoryEventReason eventReason
    ) {
        OrderHistoryEvent result = new OrderHistoryEvent();
        Order order = new Order();
        order.setId(orderId);
        order.setSubstatus(orderSubstatus);
        result.setId(id);
        result.setType(eventType);
        result.setReason(eventReason);
        result.setOrderAfter(order);
        result.setFromDate(Date.from(dateTime.toInstant(ZoneOffset.of("+03:00"))));
        return result;
    }

    public static void mockHistoryEvents(
        CheckouterOrderHistoryEventsApi historyEventsApi,
        Collection<OrderHistoryEvent> events,
        long... orderIds
    ) {
        doReturn(new OrderHistoryEvents(events)).when(historyEventsApi)
            .getOrdersHistoryEvents(refEq(
                OrdersEventsRequest.builder(orderIds)
                    .withOrderStatuses(ORDER_STATUSES)
                    .withEventTypes(EVENT_TYPES)
                    .build()
            ));
    }
}
