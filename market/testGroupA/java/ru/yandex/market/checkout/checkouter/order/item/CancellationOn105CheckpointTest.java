package ru.yandex.market.checkout.checkouter.order.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.TRACK_START_TRACKING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

class CancellationOn105CheckpointTest extends AbstractWebTestBase {

    private static final long DS_ID = 19235676;

    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private QueuedCallService queuedCallService;

    @Nonnull
    private static Track extractFfTrack(@Nonnull Order order) {
        return order.getDelivery().getParcels().get(0).getTracks().get(0);
    }

    private static void checkStatus(@Nonnull Order order,
                                    @Nullable OrderStatus expectedStatus,
                                    @Nullable OrderSubstatus expectedSubstatus) {
        if (expectedStatus != null) {
            assertEquals(expectedStatus, order.getStatus());
        }
        if (expectedSubstatus != null) {
            assertEquals(expectedSubstatus, order.getSubstatus());
        }
    }

    @BeforeEach
    void setUp() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
    }

    @Test
    void testCancellation() throws Exception {
        Order order = createProcessingOrderWithFfTrack();

        order = notifyTracks(order, 105, 1);
        checkStatus(order, CANCELLED, null);
    }

    @Test
    void testNonCancellation() throws Exception {
        Order order = createProcessingOrderWithFfTrack();

        checkouterProperties.setCancellationOn105CheckpointDisabled(true);

        order = notifyTracks(order, 105, 1);
        checkStatus(order, PROCESSING, null);
    }

    @Nonnull
    private Order notifyTracks(@Nonnull Order order, int rawStatus, long trackerCheckpointId) throws Exception {
        long trackerId = extractFfTrack(order).getTrackerId();
        notifyTracksHelper.notifyTracks(
                DeliveryTrackProvider.getDeliveryTrack(trackerId, rawStatus, trackerCheckpointId));
        order = orderService.getOrder(order.getId());
        assertTrue(extractFfTrack(order).getCheckpoints().stream()
                .anyMatch(cp -> cp.getDeliveryCheckpointStatus() == rawStatus));
        return order;
    }

    @Nonnull
    private Order createProcessingOrderWithFfTrack() throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(DS_ID)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        Track track = new Track("QWE", MOCK_DELIVERY_SERVICE_ID);
        track.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        Long trackId = orderDeliveryHelper.addTrack(order, track, ClientInfo.SYSTEM).getId();

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        checkStatus(order, PROCESSING, null);

        return orderService.getOrder(order.getId());
    }
}
