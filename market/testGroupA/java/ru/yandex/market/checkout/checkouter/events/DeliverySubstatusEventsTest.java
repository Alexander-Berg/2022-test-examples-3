package ru.yandex.market.checkout.checkouter.events;

import java.util.Collection;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.EventsTestUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_DELIVERY_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_SUBSTATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.TRACK_CHECKPOINT_CHANGED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.TRACK_START_TRACKING;
import static ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper.findEvent;
import static ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper.lastNEvents;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

/**
 * @author mmetlov
 */
public class DeliverySubstatusEventsTest extends AbstractEventsControllerTestBase {

    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private ReturnHelper returnHelper;

    public static Stream<Arguments> parameterizedTestData() {
        return EventsTestUtils.parameters(Color.BLUE).stream().map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
    }

    @AfterEach
    public void cleanup() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, false);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void shouldChangeStatusesByCheckpoints(String caseName, EventsTestUtils.EventGetter eventGetter)
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        Long trackId = orderDeliveryHelper.addTrack(order, new Track("QWE", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM).getId();

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 49, 1L));
        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);

        assertThat(lastNEvents(events, 2), contains(
                hasProperty("type", is(ORDER_SUBSTATUS_UPDATED)),
                hasProperty("type", is(TRACK_CHECKPOINT_CHANGED))
        ));

        OrderHistoryEvent event = findEvent(events, ORDER_SUBSTATUS_UPDATED);
        assertThat(event.getOrderBefore().getStatus(), is(DELIVERY));
        assertThat(event.getOrderBefore().getSubstatus(), is(DELIVERY_SERVICE_RECEIVED));
        assertThat(event.getOrderAfter().getStatus(), is(DELIVERY));
        assertThat(event.getOrderAfter().getSubstatus(), is(USER_RECEIVED));

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 50, 2L));
        events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);

        assertThat(lastNEvents(events, 4), contains(
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(TRACK_CHECKPOINT_CHANGED)),
                hasProperty("type", is(ORDER_SUBSTATUS_UPDATED)),
                hasProperty("type", is(TRACK_CHECKPOINT_CHANGED))
        ));

        event = findEvent(events, ORDER_STATUS_UPDATED);
        assertThat(event.getOrderBefore().getStatus(), is(DELIVERY));
        assertThat(event.getOrderBefore().getSubstatus(), is(USER_RECEIVED));
        assertThat(event.getOrderAfter().getStatus(), is(DELIVERED));
        assertThat(event.getOrderAfter().getSubstatus(), is(DELIVERY_SERVICE_DELIVERED));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void shouldSkipUserReceivedByCheckpoint50(String caseName, EventsTestUtils.EventGetter eventGetter)
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        Long trackId = orderDeliveryHelper.addTrack(order, new Track("QWE", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM).getId();

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 50, 1L));
        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);

        assertThat(lastNEvents(events, 3), contains(
                hasProperty("type", is(ORDER_STATUS_UPDATED)),
                hasProperty("type", is(TRACK_CHECKPOINT_CHANGED)),
                hasProperty("type", is(ORDER_DELIVERY_UPDATED))
        ));

        OrderHistoryEvent event = findEvent(events, ORDER_STATUS_UPDATED);
        assertThat(event.getOrderBefore().getStatus(), is(DELIVERY));
        assertThat(event.getOrderBefore().getSubstatus(), is(DELIVERY_SERVICE_RECEIVED));
        assertThat(event.getOrderAfter().getStatus(), is(DELIVERED));
        assertThat(event.getOrderAfter().getSubstatus(), is(DELIVERY_SERVICE_DELIVERED));
    }

    @Test
    void shouldAllowUserToMoveOrderToUserReceived() {
        Order order = orderCreateHelper.createOrder(new Parameters());

        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        order = orderStatusHelper.updateOrderStatus(order.getId(), ClientHelper.userClientFor(order), DELIVERY,
                USER_RECEIVED);
        MatcherAssert.assertThat(order.getStatus(), CoreMatchers.is(DELIVERY));
        MatcherAssert.assertThat(order.getSubstatus(), CoreMatchers.is(USER_RECEIVED));
    }
}
