package ru.yandex.market.checkout.checkouter.events;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.EventsQueueGetHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.event.HistorySortingField.DATE;
import static ru.yandex.market.checkout.checkouter.events.EventOrderControllerTest.checkOrderStatusUpdated;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

public class EventsControllerQueueTest extends AbstractWebTestBase {

    private static final int BUCKET_COUNT = 60;

    @Autowired
    private EventsQueueGetHelper eventsQueueGetHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    /**
     * Считает сумму элементов мапы, не равных et
     */
    private static long sumNotMatching(Map<HistoryEventType, Long> entries, HistoryEventType eventType) {
        return entries.entrySet().stream()
                .filter(it -> it.getKey() != eventType)
                .map(Map.Entry::getValue)
                .mapToLong(Long::longValue).sum();
    }

    @Test
    public void shouldGetEventById() throws Exception {
        orderServiceHelper.createOrder(Color.BLUE);

        OrderHistoryEvents events = eventsQueueGetHelper.getOrderHistoryEvents(null, null, null, getBlueOrderFilter());
        assertThat(events.getContent(), not(empty()));

        OrderHistoryEvent event = Iterables.get(events.getContent(), 0);

        mockMvc.perform(get("/orders/events/{eventId}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", numberEqualsTo(event.getId())));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_EVENTS)
    @DisplayName("Проверяем, что ручка /order/events?lastEventId фильтрует по бакету.")
    @Test
    public void shouldFilterByBucket() throws Exception {
        Order order = orderServiceHelper.createOrder(Color.BLUE);
        int bucketId = (int) (order.getId() % BUCKET_COUNT);
        OrderHistoryEvents events = eventsQueueGetHelper.getOrderHistoryEvents(Collections.singleton(bucketId), null,
                null, getBlueOrderFilter());

        assertThat(events.getContent(), not(empty()));

        int anotherBucketId = (bucketId + 1) % 60;
        OrderHistoryEvents emptyEvents =
                eventsQueueGetHelper.getOrderHistoryEvents(Collections.singleton(anotherBucketId), null, null,
                        getBlueOrderFilter());

        assertThat(emptyEvents.getContent(), empty());
    }

    private OrderFilter getBlueOrderFilter() {
        return OrderFilter.builder().setRgb(Color.BLUE, Color.BLUE).build();
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_EVENTS)
    @DisplayName("Проверяем, что ручка /order/events?lastEventId фильтрует по lastEventId.")
    @Test
    public void shouldFilterByLastEventId() throws Exception {
        orderServiceHelper.createOrder(Color.BLUE);

        OrderHistoryEvents events = eventsQueueGetHelper.getOrderHistoryEvents(null, null, null,
                getBlueOrderFilter());
        Collection<OrderHistoryEvent> content = events.getContent();
        OrderHistoryEvent first = Iterables.get(content, 0);
        long firstEventId = first.getId();

        OrderHistoryEvents events2 = eventsQueueGetHelper.getOrderHistoryEvents(ClientRole.SYSTEM, firstEventId,
                null, null, null, null, getBlueOrderFilter(), null);
        assertThat(events2.getContent(), hasSize(content.size() - 1));
    }

    /**
     * Проверяем, что ручка /order/events?lastEventId фильтурет по eventTypes
     */
    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_EVENTS)
    @DisplayName("Проверяем, что ручка /order/events?lastEventId фильтрует по eventTypes.")
    @Test
    public void shouldFilterByEvents() throws Exception {
        orderServiceHelper.createOrder(Color.BLUE);

        OrderHistoryEvents events = eventsQueueGetHelper.getOrderHistoryEvents(null, null, null,
                getBlueOrderFilter());
        Map<HistoryEventType, Long> entries = events.getContent().stream()
                .collect(Collectors.groupingBy(OrderHistoryEvent::getType, Collectors.counting()));

        for (Map.Entry<HistoryEventType, Long> entry : entries.entrySet()) {
            HistoryEventType historyEventType = entry.getKey();
            Long count = entry.getValue();

            OrderHistoryEvents eventsByType = eventsQueueGetHelper.getOrderHistoryEvents(null,
                    Collections.singleton(historyEventType), false, getBlueOrderFilter());
            assertThat(eventsByType.getContent(), hasSize(count.intValue()));
        }

        Map<HistoryEventType, Long> entries2 = entries.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), et -> sumNotMatching(entries, et)));

        for (Map.Entry<HistoryEventType, Long> entry : entries2.entrySet()) {
            HistoryEventType historyEventType = entry.getKey();
            Long count = entry.getValue();

            OrderHistoryEvents eventsByType = eventsQueueGetHelper.getOrderHistoryEvents(null,
                    Collections.singleton(historyEventType), true, getBlueOrderFilter());
            assertThat(eventsByType.getContent(), hasSize(count.intValue()));
        }
    }

    /**
     * Проверяем, что ручка /order?events?lastEventId возвращает не больше batchSize элементов
     */
    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_EVENTS)
    @DisplayName("Проверяем, что ручка /order/events?lastEventId возвращает не больше batchSize элементов")
    @Test
    public void shouldReturnNoMoreThanBatchSizeElements() throws Exception {
        orderServiceHelper.createOrder(Color.BLUE);

        OrderHistoryEvents events = eventsQueueGetHelper.getOrderHistoryEvents(null, null, null,
                getBlueOrderFilter());
        assertThat(events.getContent(), hasSize(greaterThan(1)));

        OrderHistoryEvents events2 = eventsQueueGetHelper.getOrderHistoryEvents(ClientRole.SYSTEM, 0, 1, null, null,
                null, getBlueOrderFilter(), null);
        assertThat(events2.getContent(), hasSize(1));
    }

    @Test
    public void shouldCreateEventDirectlyInOrderEventTable() throws Exception {
        orderServiceHelper.createOrder(Color.BLUE);

        List<OrderHistoryEvent> events = new ArrayList<>(eventsQueueGetHelper.getOrderHistoryEvents(ClientRole.SYSTEM,
                0L, null, null, null, null, getBlueOrderFilter(), null)
                .getContent());
        assertThat(events, hasSize(3));

        assertThat(events.get(0).getType(), Is.is(HistoryEventType.NEW_ORDER));
        assertThat(events.get(0).getOrderBefore(), nullValue());
        assertThat(events.get(0).getOrderAfter(), notNullValue());
        checkOrderStatusUpdated(events.get(1), OrderStatus.PLACING, OrderStatus.RESERVED);
        checkOrderStatusUpdated(events.get(2), OrderStatus.RESERVED, OrderStatus.PENDING);
    }

    @Test
    public void shouldReturnFirstEventIdAfterDate() throws Exception {
        Date date = new Date();
        Order firstOrder = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        Long minEventId = Iterables.getFirst(
                eventService.getPagedOrderHistoryEvents(
                        firstOrder.getId(),
                        Pager.atPage(1, 1),
                        new SortingInfo<>(DATE, SortingOrder.ASC),
                        null,
                        null,
                        false,
                        ClientInfo.SYSTEM,
                        null
                ).getItems(),
                null
        ).getId();
        Long eventId = eventsQueueGetHelper.getFirstEventIdAfterDate(date);

        assertThat(eventId, notNullValue());
        assertThat(eventId, equalTo(minEventId));
    }

    @Test
    public void shouldReturnNullForFutureDate() throws Exception {
        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Date date = new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        @Nullable Long eventId = eventsQueueGetHelper.getFirstEventIdAfterDate(date);

        assertThat(eventId, is(nullValue()));
    }
}
