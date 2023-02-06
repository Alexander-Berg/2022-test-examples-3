package ru.yandex.market.checkout.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.TestHelper;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author : poluektov
 * date: 09.08.17.
 */
@TestHelper
public class OrderHistoryEventsTestHelper {

    private final EventService eventService;

    public OrderHistoryEventsTestHelper(EventService eventService) {
        this.eventService = eventService;
    }

    public List<OrderHistoryEvent> getAllEvents(long orderId) {
        Set<Long> orderIds = Stream.of(orderId).collect(Collectors.toSet());
        Collection<OrderHistoryEvent> events = eventService.getOrdersHistoryEventsByOrders(
                orderIds, null, null, false, false, false, null, ClientInfo.SYSTEM, null
        );
        return new ArrayList<>(events);
    }

    public List<OrderHistoryEvent> getEventsOfType(long orderId, HistoryEventType type) {

        Set<Long> orderIds = Stream.of(orderId).collect(Collectors.toSet());
        Collection<OrderHistoryEvent> events = eventService.getOrdersHistoryEventsByOrders(
                orderIds, null, EnumSet.of(type), false, false, false, null, ClientInfo.SYSTEM, null
        );
        return new ArrayList<>(events);
    }

    public List<OrderHistoryEvent> getEventsOfType(Set<Long> orderIds, HistoryEventType type) {

        Collection<OrderHistoryEvent> events = eventService.getOrdersHistoryEventsByOrders(
                orderIds, null, EnumSet.of(type), false, false, false, null, ClientInfo.SYSTEM, null
        );
        return new ArrayList<>(events);
    }

    public OrderHistoryEvent getOrderEventById(long orderId, long eventId) {
        return getAllEvents(orderId).stream()
                .filter(event -> event.getId() == eventId)
                .findAny()
                .orElseThrow(() -> new RuntimeException("event not found"));
    }

    public static OrderHistoryEvent findEvent(Collection<OrderHistoryEvent> eventCollection,
                                              HistoryEventType eventType) {
        return eventCollection.stream()
                .filter(event -> event.getType().equals(eventType))
                .max(Comparator.comparingLong(OrderHistoryEvent::getId))
                .orElse(null);
    }

    public static List<OrderHistoryEvent> findEvents(Collection<OrderHistoryEvent> eventCollection,
                                                     HistoryEventType eventType) {
        return eventCollection.stream()
                .filter(event -> event.getType().equals(eventType))
                .collect(Collectors.toList());
    }

    public void assertHasEventWithType(Order order, HistoryEventType eventType) {
        PagedEvents orderHistoryEvents = eventService.getPagedOrderHistoryEvents(
                order.getId(),
                Pager.atPage(1, 100),
                null,
                null,
                null,
                false,
                ClientInfo.SYSTEM,
                null
        );
        assertThat(
                orderHistoryEvents.getItems().stream().map(OrderHistoryEvent::getType).collect(Collectors.toList()),
                hasItem(eventType)
        );
    }

    //sorted descending
    public static List<OrderHistoryEvent> lastNEvents(Collection<OrderHistoryEvent> events, int n) {
        return new ArrayList<>(events).subList(0, n);
    }
}
