package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrderEventsFetchingManagerTest {

    private CheckouterServiceClient client = mock(CheckouterServiceClient.class);
    private FeatureProperties featureProperties = mock(FeatureProperties.class);
    private int batchSize = 5;

    private OrderEventsFetchingManager manager = new OrderEventsFetchingManager(client, featureProperties, batchSize);

    @Test
    public void testLastBatch() {
        OrderHistoryEvent event = createEvent();
        Long eventId = event.getId();
        when(client.getOrderHistoryEvents(eventId, batchSize, null))
                .thenReturn(new OrderHistoryEvents(emptyList()))
                .thenReturn(new OrderHistoryEvents(singleton(event)));

        OrderEventsFetchingResult result1 = manager.fetchEventsBatchAfterId(eventId, null);
        OrderEventsFetchingResult result2 = manager.fetchEventsBatchAfterId(eventId, null);

        assertTrue(result1.isLastBatch());
        assertTrue(result1.getEvents().isEmpty());
        assertTrue(result2.isLastBatch());
        assertEquals(1, result2.getEvents().size());
    }

    @Test
    public void testNotLastBatch() {
        OrderHistoryEvent event = createEvent();
        Long eventId = event.getId();
        when(client.getOrderHistoryEvents(eventId, batchSize, null))
                .thenReturn(new OrderHistoryEvents(
                        Arrays.asList(createEvent(), createEvent(), createEvent(), createEvent(), createEvent())))
                .thenReturn(new OrderHistoryEvents(singleton(event)));

        OrderEventsFetchingResult result1 = manager.fetchEventsBatchAfterId(eventId, null);
        OrderEventsFetchingResult result2 = manager.fetchEventsBatchAfterId(eventId, null);

        assertFalse(result1.isLastBatch());
        assertEquals(result1.getEvents().size(), 5);
        assertTrue(result2.isLastBatch());
        assertEquals(1, result2.getEvents().size());
    }

    @Test
    public void testSingleEventEnabled() {
        OrderHistoryEvent event = createEvent();
        Long eventId = event.getId();
        when(client.getHistoryEvent(eventId)).thenReturn(event);
        when(featureProperties.isUseSingleEventEndpointForFailover()).thenReturn(true);

        OrderHistoryEvent result = manager.fetchEventById(eventId);

        assertEquals(result, event);
    }

    @Test
    public void testSingleEventDisabled() {
        OrderHistoryEvent event = createEvent();
        Long eventId = event.getId();
        when(client.getOrderHistoryEvents(eventId - 1, 1))
            .thenReturn(new OrderHistoryEvents(List.of(event)));

        OrderHistoryEvent result = manager.fetchEventById(eventId);

        assertEquals(result, event);
    }

    private OrderHistoryEvent createEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(1L);
        return event;
    }

}
