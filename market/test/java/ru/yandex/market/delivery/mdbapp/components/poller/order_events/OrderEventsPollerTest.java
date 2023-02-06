package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.parcel.ParcelCancelChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.components.curator.managers.OrderEventManager;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.service.InternalVariableService;
import ru.yandex.market.delivery.mdbapp.components.service.OrderEventsFailoverService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.InternalVariableType;
import ru.yandex.market.delivery.mdbapp.util.OrderEventUtils;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CHANGE_REQUEST_CREATED;

public class OrderEventsPollerTest extends AbstractTest {

    private final OrderEventsProcessor eventsProcessor = mock(OrderEventsProcessor.class);
    private final OrderEventsFetchingManager fetchingManager = mock(OrderEventsFetchingManager.class);
    private final OrderEventManager eventManager = mock(OrderEventManager.class);
    private final OrderEventFailoverableService failoverService = mock(OrderEventFailoverableService.class);
    private final OrderEventsFailoverService failoverRetryService = mock(OrderEventsFailoverService.class);
    private final InternalVariableService internalVariableService = mock(InternalVariableService.class);
    private final Set<Integer> buckets = ImmutableSet.of(0, 1, 2);
    private final Appender traceAppender = mock(Appender.class);

    private final OrderEventsPoller pollerWithDisabledLogbroker = new OrderEventsPoller(
        fetchingManager,
        eventManager,
        failoverService,
        failoverRetryService,
        eventsProcessor,
        buckets,
        internalVariableService
    );

    private final OrderEventsPoller poller = new OrderEventsPoller(
        fetchingManager,
        eventManager,
        failoverService,
        failoverRetryService,
        eventsProcessor,
        buckets,
        internalVariableService
    );

    @BeforeEach
    public void setUp() {
        Logger logger = (((Logger) LoggerFactory.getLogger("requestTrace")));
        logger.addAppender(traceAppender);
        logger.setLevel(Level.TRACE);
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(failoverService, eventsProcessor);
    }

    @Test
    public void testSuccessfulEventProcessing() throws Exception {
        OrderHistoryEvent event = createEvent();
        HashSet<Long> failedOrders = new HashSet<>();
        when(eventsProcessor.processEvent(event)).thenReturn(true);
        poller.processEvent(event, failedOrders, null, null);
        assertTrue(failedOrders.isEmpty());
        verify(eventManager).setId(event.getId());
        verify(eventsProcessor).processEvent(event);

        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        verify(traceAppender).doAppend(argumentCaptor.capture());

        softly.assertThat(1).isEqualTo(argumentCaptor.getAllValues().size());
        String message = ((LoggingEvent) argumentCaptor.getAllValues().get(0)).getMessage();
        softly.assertThat(message).contains("type=IN\t");
        softly.assertThat(message).contains("request_method=CheckouterEvent_processing\t");
        softly.assertThat(message).endsWith("kv.eventId=10\tkv.orderId=123");
    }

    @Test
    public void testStopForLogbrokerEventProcessing() throws Exception {
        OrderHistoryEvent event = createEventForLogbroker();
        HashSet<Long> failedOrders = new HashSet<>();
        when(eventsProcessor.processEvent(event)).thenReturn(true);
        when(internalVariableService.getValue(eq(InternalVariableType.CHECKOUTER_EVENT_POLLER_STOP_ID)))
            .thenReturn(Optional.of("0"));
        poller.processEvent(event, failedOrders, 0L, null);
        assertTrue(failedOrders.isEmpty());
        verify(eventManager, never()).setId(event.getId());
    }

    @Test
    public void testSkipForLogbrokerEventProcessing() throws Exception {
        OrderHistoryEvent event = createEventForLogbroker();
        HashSet<Long> failedOrders = new HashSet<>();
        when(eventsProcessor.processEvent(event)).thenReturn(true);
        when(internalVariableService.getValue(eq(InternalVariableType.CHECKOUTER_EVENT_POLLER_STOP_ID)))
            .thenReturn(Optional.of("0"));
        poller.processEvent(event, failedOrders, null, 100L);
        assertTrue(failedOrders.isEmpty());
        verify(eventManager).setId(eq(100L));
    }

    @Test
    public void testSuccessfulEventProcessingWithDisabledLogbroker() throws Exception {
        OrderHistoryEvent event = createEventForLogbroker();
        HashSet<Long> failedOrders = new HashSet<>();
        when(eventsProcessor.processEvent(event)).thenReturn(true);
        pollerWithDisabledLogbroker.processEvent(event, failedOrders, null, null);
        assertTrue(failedOrders.isEmpty());
        verify(eventManager).setId(event.getId());
        verify(eventsProcessor).processEvent(event);

        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        verify(traceAppender).doAppend(argumentCaptor.capture());

        softly.assertThat(1).isEqualTo(argumentCaptor.getAllValues().size());
        String message = ((LoggingEvent) argumentCaptor.getAllValues().get(0)).getMessage();
        softly.assertThat(message).contains("type=IN\t");
        softly.assertThat(message).contains("request_method=CheckouterEvent_processing\t");
        softly.assertThat(message).endsWith("kv.eventId=20\tkv.orderId=123");
    }

    @Test
    public void testEventProcessingFailed() throws Exception {
        OrderHistoryEvent event = createEvent();
        HashSet<Long> failedOrders = new HashSet<>();
        when(eventsProcessor.processEvent(event)).thenReturn(false);
        poller.processEvent(event, failedOrders, null, null);
        softly.assertThat(failedOrders.size()).isEqualTo(1);
        softly.assertThat(OrderEventUtils.getOrderId(event)).isEqualTo(failedOrders.iterator().next());
        verify(eventManager).setId(event.getId());
        verify(eventsProcessor).processEvent(event);
    }

    @Test
    public void testPreviousEventForSameOrderWasFailed() throws Exception {
        OrderHistoryEvent event = createEvent();
        HashSet<Long> failedOrders = new HashSet<>(singletonList(OrderEventUtils.getOrderId(event)));
        poller.processEvent(event, failedOrders, null, null);
        softly.assertThat(failedOrders.size()).isEqualTo(1);
        softly.assertThat(OrderEventUtils.getOrderId(event)).isEqualTo(failedOrders.iterator().next());
        verify(failoverService).queueEvent(event);
        verify(eventManager).setId(event.getId());
    }

    @Test
    public void testCancellaionRequestForFailover() throws Exception {
        OrderHistoryEvent event = createEvent();
        Long orderId = OrderEventUtils.getOrderId(event);

        event.setType(ORDER_CHANGE_REQUEST_CREATED);
        event.getOrderAfter().setChangeRequests(Collections.singletonList(
            new ChangeRequest(
                1L,
                orderId,
                new ParcelCancelChangeRequestPayload(1L, OrderSubstatus.CUSTOM, "", Collections.emptyList()),
                ChangeRequestStatus.NEW,
                Instant.MIN,
                "",
                ClientRole.UNKNOWN
            )
        ));
        HashSet<Long> failedOrders = new HashSet<>(singletonList(orderId));
        poller.processEvent(event, failedOrders, null, null);
        verify(failoverService).queueEvent(event);
        verify(eventManager).setId(event.getId());
        verify(failoverRetryService).retryOrder(orderId);
    }

    private OrderHistoryEvent createEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(10L);
        Order orderAfter = new Order();
        orderAfter.setId(123L);
        event.setOrderAfter(orderAfter);
        return event;
    }

    private OrderHistoryEvent createEventForLogbroker() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(20L);
        Order orderAfter = new Order();
        orderAfter.setId(123L);
        event.setOrderAfter(orderAfter);
        return event;
    }
}
