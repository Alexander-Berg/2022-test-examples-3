package ru.yandex.market.crm.operatorwindow.checkout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.crm.operatorwindow.checkout.events.CheckouterEventFeedType;
import ru.yandex.market.crm.operatorwindow.checkout.events.CheckouterEventFeedsReader;
import ru.yandex.market.crm.operatorwindow.checkout.events.CheckouterEventsHandler;
import ru.yandex.market.crm.operatorwindow.checkout.snapshot.CheckouterEventFeedSnapshot;
import ru.yandex.market.crm.operatorwindow.checkout.snapshot.CheckouterEventFeedSnapshots;
import ru.yandex.market.crm.operatorwindow.domain.order.OrderEventsIgnoreRules;
import ru.yandex.market.crm.operatorwindow.log.trace.CheckouterEventTraceLogger;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.configuration.api.Property;
import ru.yandex.market.request.trace.Module;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class CheckouterEventFeedsReaderTest {

    private static final CheckouterEventFeedType TYPE = CheckouterEventFeedType.ORDER;
    private static final long LAST_EVENT_ID = 555L;
    @Mock
    private OrderEventsIgnoreRules rules;

    private CheckouterEventTraceLogger traceLogger;

    @Mock
    private CheckouterEventFeedSnapshots snapshot;

    private CheckouterEventFeedsReader eventsProcessor;

    private CheckouterEventsHandler handler;

    @Mock
    private ConfigurationService configurationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(snapshot.get(TYPE)).thenReturn(new CheckouterEventFeedSnapshot(LAST_EVENT_ID, TYPE));
        when(rules.shouldOrderEventBeIgnored(any(Order.class))).thenReturn(false);
        traceLogger = new CheckouterEventTraceLogger(Module.MARKET_OPERATOR_WINDOW);
        eventsProcessor = new CheckouterEventFeedsReader(snapshot, rules, traceLogger, configurationService);
        handler = new TestHandler();
    }

    @Test
    public void testAllNewEvents() {
        List<OrderHistoryEvent> events = getEventsStartingFrom(5, LAST_EVENT_ID + 1);
        when(configurationService.getValue(any(Property.class), any(Boolean.class))).thenReturn(false);
        GetBatchResult result = eventsProcessor.processEvents(events, handler, LAST_EVENT_ID);
        MatcherAssert.assertThat(((TestHandler) handler).getResult(), is(Arrays.asList(556L, 557L, 558L, 559L, 560L)));
        Assertions.assertTrue(
                result instanceof ContinueFetchData,
                "Should continue reading, but got " + result.getClass().getSimpleName());
        MatcherAssert.assertThat(((ContinueFetchData) result).getNextEventId(), is(560L));
    }

    @Test
    public void testOldAndNewEvents_skip() {
        List<OrderHistoryEvent> events = getEventsStartingFrom(5, LAST_EVENT_ID - 2); //553-557
        when(configurationService.getValue(any(Property.class), any(Boolean.class))).thenReturn(true);
        GetBatchResult result = eventsProcessor.processEvents(events, handler, LAST_EVENT_ID);
        MatcherAssert.assertThat(((TestHandler) handler).getResult(), is(Arrays.asList(556L, 557L)));
        Assertions.assertTrue(
                result instanceof ContinueFetchData,
                "Should continue reading, but got " + result.getClass().getSimpleName());
        MatcherAssert.assertThat(((ContinueFetchData) result).getNextEventId(), is(557L));
    }

    @Test
    public void testOnlyOldEvents_skip() {
        List<OrderHistoryEvent> events = getEventsStartingFrom(5, LAST_EVENT_ID - 5); //550-554
        when(configurationService.getValue(any(Property.class), any(Boolean.class))).thenReturn(true);
        GetBatchResult result = eventsProcessor.processEvents(events, handler, LAST_EVENT_ID);
        MatcherAssert.assertThat(((TestHandler) handler).getResult(), empty());
        Assertions.assertTrue(
                result instanceof ContinueFetchData,
                "Should continue reading, but got " + result.getClass().getSimpleName());
        MatcherAssert.assertThat(((ContinueFetchData) result).getNextEventId(), is(555L));
    }

    @Test
    public void testOldAndNewEvents_dontSkip() {
        List<OrderHistoryEvent> events = getEventsStartingFrom(5, LAST_EVENT_ID - 2); //553-557
        when(configurationService.getValue(any(Property.class), any(Boolean.class))).thenReturn(false);
        GetBatchResult result = eventsProcessor.processEvents(events, handler, LAST_EVENT_ID);
        MatcherAssert.assertThat(((TestHandler) handler).getResult(), is(Arrays.asList(553L, 554L, 555L, 556L, 557L)));
        Assertions.assertTrue(
                result instanceof ContinueFetchData,
                "Should continue reading, but got " + result.getClass().getSimpleName());
        MatcherAssert.assertThat(((ContinueFetchData) result).getNextEventId(), is(557L));
    }

    @Test
    public void testOnlyOldEvents_dontSkip() {
        List<OrderHistoryEvent> events = getEventsStartingFrom(5, LAST_EVENT_ID - 5); //550-554
        when(configurationService.getValue(any(Property.class), any(Boolean.class))).thenReturn(false);
        GetBatchResult result = eventsProcessor.processEvents(events, handler, LAST_EVENT_ID);
        MatcherAssert.assertThat(((TestHandler) handler).getResult(), is(Arrays.asList(550L, 551L, 552L, 553L, 554L)));
        Assertions.assertTrue(
                result instanceof ContinueFetchData,
                "Should continue reading, but got " + result.getClass().getSimpleName());
        MatcherAssert.assertThat(((ContinueFetchData) result).getNextEventId(), is(554L));
    }

    @Test
    public void testNoEvents() {
        when(configurationService.getValue(any(Property.class), any(Boolean.class))).thenReturn(false);
        GetBatchResult result = eventsProcessor.processEvents(new ArrayList<>(), handler, LAST_EVENT_ID);
        MatcherAssert.assertThat(((TestHandler) handler).getResult(), empty());
        Assertions.assertTrue(
                result instanceof StopFetchData, "Should stop reading, but got " + result.getClass().getSimpleName());
    }

    @Test
    public void testIgnoredEventsShiftLastId() {
        List<OrderHistoryEvent> events = getEventsStartingFrom(5, LAST_EVENT_ID - 3); //552-556
        when(rules.shouldOrderEventBeIgnored(any(Order.class))).thenReturn(true);
        when(configurationService.getValue(any(Property.class), any(Boolean.class))).thenReturn(false);
        GetBatchResult result = eventsProcessor.processEvents(events, handler, LAST_EVENT_ID);
        Assertions.assertTrue(
                result instanceof ContinueFetchData,
                "Should continue reading, but got " + result.getClass().getSimpleName());
        MatcherAssert.assertThat(((ContinueFetchData) result).getNextEventId(), is(556L));
    }

    @Test
    public void testInvalidEventsCauseError() {
        List<OrderHistoryEvent> events = Arrays.asList(null, null);
        when(configurationService.getValue(any(Property.class), any(Boolean.class))).thenReturn(false);
        Assertions.assertThrows(RuntimeException.class, () -> eventsProcessor.processEvents(events, handler,
                LAST_EVENT_ID));
    }

    private List<OrderHistoryEvent> getEventsStartingFrom(int count, long firstId) {
        return IntStream.range(0, count).mapToObj(i -> {
            OrderHistoryEvent event = new OrderHistoryEvent();
            Order order = new Order();
            order.setId(100L + i);
            event.setId(firstId + i);
            event.setOrderAfter(order);
            return event;
        }).collect(Collectors.toList());
    }


    private class TestHandler implements CheckouterEventsHandler {
        private final List<Long> handledIds = new ArrayList<>();

        @Override
        public CheckouterEventFeedType getType() {
            return TYPE;
        }

        @Override
        public void handle(List<OrderHistoryEvent> events) {
            handledIds.addAll(events.stream().map(OrderHistoryEvent::getId).collect(Collectors.toList()));
        }

        public List<Long> getResult() {
            return handledIds;
        }
    }

}
