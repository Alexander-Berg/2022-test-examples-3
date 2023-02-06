package ru.yandex.market.checkout.checkouter.returns;

import java.util.concurrent.atomic.AtomicLong;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.TRACK_RETURN_START_TRACKING;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueParametersWithDelivery;

public class ReturnTrackingWithQueuedCallTest extends AbstractReturnTestBase {

    private Return ret;
    private Long trackId;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private QueuedCallService queuedCallService;

    private static final AtomicLong TRACK_CODE_NUMBER = new AtomicLong(100);
    private Order order;

    @BeforeEach
    public void setUp() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);

        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        MockTrackerHelper.mockGetDeliveryServices(DELIVERY_SERVICE_ID, trackerMock);

        Parameters params = defaultBlueParametersWithDelivery(DELIVERY_SERVICE_ID);
        params.getOrder().getItems().forEach(item -> item.setCount(10));
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        ret = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        request = ReturnHelper.copy(ret);
        client.returns().resumeReturn(order.getId(), ret.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID,
                request);
        final long seed = TRACK_CODE_NUMBER.getAndIncrement();
        final String uniqueTrackCode = "TrackCode" + seed;

        client.returns().setReturnTrackCode(order.getId(), ret.getId(), ClientRole.SYSTEM, order.getBuyer().getUid(),
                uniqueTrackCode);
        ret = client.returns().getReturn(order.getId(), ret.getId(), false, ClientRole.SYSTEM, 123L);
        trackId = ret.getDelivery().getTrack().getId();
    }

    @AfterEach
    public void tearDown() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, false);
    }

    @Test
    public void returnTrackingStartCheckWithQC() {
        assertTrue(queuedCallService.existsQueuedCall(TRACK_RETURN_START_TRACKING, trackId));
        try {
            checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);

            assertTrackStatusEquals(order, ret, TrackStatus.NEW);
            queuedCallService.executeQueuedCallSynchronously(TRACK_RETURN_START_TRACKING, trackId);
            assertTrackStatusEquals(order, ret);
        } finally {
            checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, false);
        }
    }
}
