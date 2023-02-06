package ru.yandex.market.checkout.checkouter.order.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.TRACK_START_TRACKING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

/**
 * @author mmetlov
 */
public class OrderControllerDeliverySubstatusChangeTest extends AbstractWebTestBase {

    @Autowired
    private ReturnHelper returnHelper;
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
    private OrderGetHelper orderGetHelper;

    @AfterEach
    public void cleanup() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, false);
    }

    @Test
    public void shouldAllowToChangeDeliverySubstatuses() {
        Order order = orderCreateHelper.createOrder(new Parameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));
        updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.USER_RECEIVED);
    }

    @Test
    public void shouldNotAllowToChangeDeliverySubstatusesBackwards() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Order order = orderCreateHelper.createOrder(new Parameters());
            order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
            assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
            assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
            assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));
            updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.USER_RECEIVED);
            updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.DELIVERY_SERVICE_RECEIVED);
        });
    }

    @Test
    public void shouldNotAllowUserReceivedForPickupOrder() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Parameters parameters = new Parameters();
            parameters.setDeliveryType(DeliveryType.PICKUP);
            Order order = orderCreateHelper.createOrder(parameters);
            order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
            assertThat(order.getDelivery().getType(), is(DeliveryType.PICKUP));
            assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
            assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));
            updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.USER_RECEIVED);
        });
    }

    @Test
    public void shouldChangeStatusesByCheckpoints() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Long trackId = orderDeliveryHelper.addTrack(order, new Track("QWE", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM).getId();

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 49, 1L));
        checkOrderSubstatus(order.getId(), OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED);

        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 50, 2L));
        checkOrderSubstatus(order.getId(), OrderStatus.DELIVERED, OrderSubstatus.DELIVERY_SERVICE_DELIVERED);
    }

    @Test
    public void shouldChangeToDeliveredWithout49() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Long trackId = orderDeliveryHelper.addTrack(order, new Track("QWE", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM).getId();

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 50, 2L));
        checkOrderSubstatus(order.getId(), OrderStatus.DELIVERED, OrderSubstatus.DELIVERY_SERVICE_DELIVERED);
    }

    @Test
    public void shouldNotAllowUserReceivedForPickupOrderByCheckpoint() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Long trackId = orderDeliveryHelper.addTrack(order, new Track("QWE", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM).getId();

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

        assertThat(order.getDelivery().getType(), is(DeliveryType.PICKUP));
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 49, 1L));
        checkOrderSubstatus(order.getId(), OrderStatus.DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 50, 2L));
        checkOrderSubstatus(order.getId(), OrderStatus.DELIVERED, OrderSubstatus.DELIVERY_SERVICE_DELIVERED);
    }

    @Test
    public void shopShouldSeeDeliverySubstatus() throws Exception {
        Order order = orderCreateHelper.createOrder(new Parameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        assertThat(client.getOrder(order.getId(), ClientRole.SHOP, order.getShopId()).getSubstatus(),
                is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));
        assertThat(orderGetHelper.getOrder(order.getId(), new ClientInfo(ClientRole.SHOP_USER, 123L,
                order.getShopId()))
                .getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.USER_RECEIVED);

        assertThat(client.getOrder(order.getId(), ClientRole.SHOP, order.getShopId()).getSubstatus(),
                is(OrderSubstatus.USER_RECEIVED));
        assertThat(orderGetHelper.getOrder(order.getId(), new ClientInfo(ClientRole.SHOP_USER, 123L,
                order.getShopId()))
                .getSubstatus(), is(OrderSubstatus.USER_RECEIVED));
    }

    @Test
    @DisplayName("Можно перевести статус заказа из DELIVERY/DELIVERY_SERVICE_RECEIVED " +
            "в DELIVERY/DELIVERY_TO_STORE_STARTED")
    public void shouldAllowChangeSubstatusToDeliveryToStoreStarted() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(OrderStatus.DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.DELIVERY_TO_STORE_STARTED);
    }

    private void updateSubstatusAndCheckIt(Long orderId, OrderSubstatus substatus) {
        Order orderBeforeUpdate = client.getOrder(orderId, ClientRole.SYSTEM, null);
        String statusUpdateDate = orderBeforeUpdate.getStatusUpdateDateTs();
        client.updateOrderStatus(
                orderId,
                ClientRole.SYSTEM,
                1L,
                null,
                OrderStatus.DELIVERY,
                substatus
        );
        Order order = client.getOrder(orderId, ClientRole.SYSTEM, null);
        assertThat(order.getSubstatus(), is(substatus));
        assertEquals(statusUpdateDate, order.getStatusUpdateDateTs());
    }

    private void checkOrderSubstatus(Long orderId, OrderStatus status, OrderSubstatus substatus) {
        Order order = client.getOrder(orderId, ClientRole.SYSTEM, null);
        assertThat(order.getStatus(), is(status));
        assertThat(order.getSubstatus(), is(substatus));
    }
}
