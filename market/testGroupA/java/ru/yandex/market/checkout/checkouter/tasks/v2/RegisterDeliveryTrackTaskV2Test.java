package ru.yandex.market.checkout.checkouter.tasks.v2;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RegisterDeliveryTrackTaskV2Test extends AbstractWebTestBase {
    @Autowired
    private RegisterDeliveryTrackTaskV2 registerDeliveryTrackTaskV2;


    private static final long DELIVERY_SERVICE_ID = 1L;

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;

    private long orderId;

    @BeforeEach
    public void setUp() throws Exception {
        Parameters blueOrder = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        Order order = orderCreateHelper.createOrder(blueOrder);
        assertEquals(Boolean.FALSE, order.isFulfilment());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        this.orderId = order.getId();

        orderDeliveryHelper.updateOrderDelivery(order.getId(), ClientInfo.SYSTEM,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithTracks(
                                TrackProvider.createTrack(DELIVERY_SERVICE_ID)
                        )
                )
        );

        MockTrackerHelper.mockGetDeliveryServices(DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
    }

    @Test
    public void shouldPushNonCancelledOrder() {
        var result = registerDeliveryTrackTaskV2.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        final var order = orderService.getOrder(orderId);
        assertNotNull(order);
        assertNotNull(order.getDelivery().getParcels().get(0).getTracks().get(0).getTrackerId());
    }
}
