package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.config.CheckouterAnnotationJsonConfig;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.service.OrderEventsService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderEventsFailoverCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.FailCauseType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.TicketCreationStatus;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FailedOrderEventsPollerTest {

    private static long eventId = 0;

    private final OrderEventsFetchingManager fetchingManager = mock(OrderEventsFetchingManager.class);
    private final OrderEventFailoverableService failoverService = mock(OrderEventFailoverableService.class);
    private final OrderEventsProcessor eventsProcessor = mock(OrderEventsProcessor.class);
    private final OrderEventsService eventsService = mock(OrderEventsService.class);
    private final ObjectMapper objectMapper = CheckouterAnnotationJsonConfig.objectMapperPrototype(
        new SimpleFilterProvider()
            .setFailOnUnknownId(false)
    );

    private final OrderHistoryEvent firstEvent = newEvent(1L);
    private final OrderHistoryEvent secondEvent = newEvent(1L);
    private final OrderHistoryEvent thirdEvent = newEvent(21L);
    private final OrderHistoryEvent fourthEvent = newEvent(41L);
    private final OrderHistoryEvent fifthEvent = newEvent(61L);
    private final OrderHistoryEvent sixthEvent = newEvent(61L);
    private final OrderHistoryEvent sevenEvent = newEvent(71L);

    private final OrderEventsFailoverCounter first = newCounter(firstEvent, false);
    private final OrderEventsFailoverCounter second = newCounter(secondEvent, false);
    private final OrderEventsFailoverCounter third = newCounter(thirdEvent, false);
    private final OrderEventsFailoverCounter fourth = newCounter(fourthEvent, false);
    private final OrderEventsFailoverCounter fifth = newCounter(fifthEvent, false);
    private final OrderEventsFailoverCounter sixth = newCounter(sixthEvent, false);
    private final OrderEventsFailoverCounter seven = newCounter(sevenEvent, true);

    private final EventFlowParametersHolder eventFlowParametersHolder = mock(EventFlowParametersHolder.class);
    private final FailedOrderEventsPoller poller = new FailedOrderEventsPoller(
        fetchingManager,
        failoverService,
        eventsProcessor,
        eventFlowParametersHolder,
        eventsService,
        1
    );

    @Before
    public void setUp() {
        when(failoverService.findFailedOrdersIds(1)).thenReturn(allOrders());

        when(failoverService.getCountersByOrder(1L)).thenReturn(asList(first, second));
        when(failoverService.getCountersByOrder(21L)).thenReturn(singletonList(third));
        when(failoverService.getCountersByOrder(41L)).thenReturn(asList(fourth, fifth, sixth));

        when(fetchingManager.fetchEventById(first.getEventId())).thenReturn(firstEvent);
        when(fetchingManager.fetchEventById(second.getEventId())).thenReturn(secondEvent);
        when(fetchingManager.fetchEventById(third.getEventId())).thenReturn(thirdEvent);
        when(fetchingManager.fetchEventById(fourth.getEventId())).thenReturn(fourthEvent);
        when(fetchingManager.fetchEventById(fifth.getEventId())).thenReturn(fifthEvent);
        when(fetchingManager.fetchEventById(sixth.getEventId())).thenReturn(sixthEvent);
    }

    @AfterEach
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(eventsProcessor, fetchingManager, failoverService);
    }

    @Test
    public void testAllCountersCanBeProcessed() {
        when(failoverService.canBeProcessed(any(OrderEventsFailoverCounter.class))).thenReturn(true);
        when(eventsProcessor.processEvent(any(OrderHistoryEvent.class))).thenReturn(true);

        poller.poll();

        checkEvents(firstEvent, secondEvent, thirdEvent, fourthEvent, fifthEvent, sixthEvent);
        verify(fetchingManager).fetchEventById(first.getEventId());
        verify(fetchingManager).fetchEventById(second.getEventId());
        verify(fetchingManager).fetchEventById(third.getEventId());
        verify(fetchingManager).fetchEventById(fourth.getEventId());
        verify(fetchingManager).fetchEventById(fifth.getEventId());
        verify(fetchingManager).fetchEventById(sixth.getEventId());
    }

    @Test
    public void testSomeCountersCanBeProcessed() {
        when(failoverService.canBeProcessed(any(OrderEventsFailoverCounter.class))).thenReturn(true);
        when(failoverService.canBeProcessed(first)).thenReturn(false);
        when(eventsProcessor.processEvent(any(OrderHistoryEvent.class))).thenReturn(true);

        poller.poll();

        checkEvents(thirdEvent, fourthEvent, fifthEvent, sixthEvent);
        verify(fetchingManager).fetchEventById(third.getEventId());
        verify(fetchingManager).fetchEventById(fourth.getEventId());
        verify(fetchingManager).fetchEventById(fifth.getEventId());
        verify(fetchingManager).fetchEventById(sixth.getEventId());
    }

    @Test
    public void testAllEarliestCountersCantBeProcessed() {
        when(failoverService.canBeProcessed(any(OrderEventsFailoverCounter.class))).thenReturn(true);
        when(failoverService.canBeProcessed(first)).thenReturn(false);
        when(failoverService.canBeProcessed(third)).thenReturn(false);
        when(failoverService.canBeProcessed(fourth)).thenReturn(false);

        poller.poll();

        verify(eventsProcessor, never()).processEvent(any(OrderHistoryEvent.class));
    }

    @Test
    public void testCancelled() {
        fifthEvent.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);
        ChangeRequest changeRequest = new ChangeRequest(
            1L,
            1L,
            new AbstractChangeRequestPayload(ChangeRequestType.PARCEL_CANCELLATION) {
            },
            ChangeRequestStatus.PROCESSING,
            Instant.MIN,
            "",
            ClientRole.SYSTEM
        );
        fifthEvent.getOrderAfter().setChangeRequests(List.of(
            changeRequest
        ));
        // should cancel directly on last attempt only
        when(failoverService.isLastAttempt(fourth)).thenReturn(true);
        when(failoverService.canBeProcessed(any(OrderEventsFailoverCounter.class))).thenReturn(true);
        when(eventsProcessor.processEvent(any(OrderHistoryEvent.class))).thenReturn(true);

        poller.poll();

        checkEvents(firstEvent, secondEvent, thirdEvent, fifthEvent);
        Mockito.verifyNoMoreInteractions(eventsProcessor);
        verify(fetchingManager).fetchEventById(first.getEventId());
        verify(fetchingManager).fetchEventById(second.getEventId());
        verify(fetchingManager).fetchEventById(third.getEventId());
        verify(fetchingManager).fetchEventById(fourth.getEventId());
        verify(fetchingManager).fetchEventById(fifth.getEventId());
        verify(fetchingManager).fetchEventById(sixth.getEventId());
    }

    @Test
    public void testCancelledNotLastAttempt() {
        fifthEvent.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);
        ChangeRequest changeRequest = Mockito.mock(ChangeRequest.class);
        when(changeRequest.getType()).thenReturn(ChangeRequestType.PARCEL_CANCELLATION);
        fifthEvent.getOrderAfter().setChangeRequests(List.of(
            changeRequest
        ));

        sixthEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        sixthEvent.getOrderAfter().setStatus(OrderStatus.CANCELLED);

        when(failoverService.canBeProcessed(any(OrderEventsFailoverCounter.class))).thenReturn(true);
        when(eventsProcessor.processEvent(any(OrderHistoryEvent.class))).thenReturn(true);

        poller.poll();

        checkEvents(firstEvent, secondEvent, thirdEvent, fourthEvent, fifthEvent, sixthEvent);
        Mockito.verifyNoMoreInteractions(eventsProcessor);
        verify(fetchingManager).fetchEventById(first.getEventId());
        verify(fetchingManager).fetchEventById(second.getEventId());
        verify(fetchingManager).fetchEventById(third.getEventId());
        verify(fetchingManager).fetchEventById(fourth.getEventId());
        verify(fetchingManager).fetchEventById(fifth.getEventId());
        verify(fetchingManager).fetchEventById(sixth.getEventId());
    }

    @Test
    public void testSavedEvent() {
        when(failoverService.findFailedOrdersIds(1)).thenReturn(Set.of(71L));
        when(failoverService.getCountersByOrder(71L)).thenReturn(singletonList(seven));
        when(failoverService.canBeProcessed(any(OrderEventsFailoverCounter.class))).thenReturn(true);
        when(eventsService.convertEvent(anyString())).thenReturn(sevenEvent);
        when(eventsProcessor.processEvent(any(OrderHistoryEvent.class))).thenReturn(true);
        sevenEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);

        poller.poll();

        checkEvents(sevenEvent);
    }

    @Nonnull
    private Set<Long> allOrders() {
        return Set.of(1L, 21L, 41L);
    }

    @Nonnull
    private OrderHistoryEvent newEvent(Long orderId) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(eventId++);
        Order orderAfter = new Order();
        orderAfter.setId(orderId);
        event.setOrderAfter(orderAfter);
        Order orderBefore = new Order();
        orderBefore.setId(orderId);
        event.setOrderBefore(orderBefore);
        return event;
    }

    @Nonnull
    @SneakyThrows
    private OrderEventsFailoverCounter newCounter(OrderHistoryEvent event, boolean withEvent) {
        OrderEventsFailoverCounter counter = new OrderEventsFailoverCounter(
            event.getId(),
            event.getOrderAfter().getId(),
            "message",
            null,
            TicketCreationStatus.NOT_CREATED,
            FailCauseType.UNKNOWN
        );
        if (withEvent) {
            counter.setEventPayload(objectMapper.writeValueAsString(event));
        }
        return counter;
    }

    private void checkEvents(OrderHistoryEvent... events) {
        for (OrderHistoryEvent event : events) {
            verify(eventsProcessor).processEvent(event);
            verify(eventFlowParametersHolder, times(events.length)).setFailoverFlowActive(eq(true));
        }
        Mockito.verifyNoMoreInteractions(eventsProcessor);
        Mockito.verifyNoMoreInteractions(eventFlowParametersHolder);
    }
}
