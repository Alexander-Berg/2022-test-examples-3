package ru.yandex.market.checkout.checkouter.track;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackCheckpointProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackMetaProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

public class CheckpointsWithDelayedActionsTest extends AbstractWebTestBase {

    @Autowired
    private CheckouterProperties checkouterProperties;
    @Autowired
    private OrderService orderService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;


    @ParameterizedTest
    @ValueSource(ints = {70, 80})
    public void shouldCreateQueuedCallAfterReturnArrivedTrackCheckpointForDeliveryType(int deliveryCheckpointStatus)
            throws Exception {
        checkouterProperties.setEnabledDelayedOrderStatusUpdateActions(true);
        var order = initOrder(OrderStatus.DELIVERY, DeliveryType.DELIVERY);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(deliveryCheckpointStatus)
        );

        // Отправляем трек
        notifyTracksHelper.notifyTracks(deliveryTrack);

        // Должен быть создан QC на отмену заказа
        var qc = queuedCallService.findQueuedCalls(CheckouterQCType.DELAYED_ORDER_CHANGE_ACTION, order.getId());
        Assertions.assertEquals(1, qc.size());

        // Заказ не отменяется сразу
        order = orderService.getOrder(order.getId());
        Assertions.assertEquals(OrderStatus.DELIVERY, order.getStatus());

        // Заказ должен отмениться после выполнения QC
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.DELAYED_ORDER_CHANGE_ACTION, order.getId());
        order = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @ParameterizedTest
    @ValueSource(ints = {70, 80})
    public void shouldCancelOrderAfterReturnArrivedTrackCheckpointForPickupTypeAndDeliveryStatus(
            int deliveryCheckpointStatus) throws Exception {
        checkouterProperties.setEnabledDelayedOrderStatusUpdateActions(true);
        var order = initOrder(OrderStatus.DELIVERY, DeliveryType.PICKUP);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(deliveryCheckpointStatus)
        );

        // Отправляем трек
        notifyTracksHelper.notifyTracks(deliveryTrack);

        // Должен быть создан QC на отмену заказа
        var qc = queuedCallService.findQueuedCalls(CheckouterQCType.DELAYED_ORDER_CHANGE_ACTION, order.getId());
        Assertions.assertEquals(1, qc.size());

        // Заказ не отменяется сразу
        order = orderService.getOrder(order.getId());
        Assertions.assertEquals(OrderStatus.DELIVERY, order.getStatus());

        // Заказ должен отмениться после выполнения QC
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.DELAYED_ORDER_CHANGE_ACTION, order.getId());
        order = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @ParameterizedTest
    @ValueSource(ints = {70, 80})
    public void shouldCancelOrderAfterReturnArrivedTrackCheckpointForPickupTypeAndPickupStatus(
            int deliveryCheckpointStatus) throws Exception {
        checkouterProperties.setEnabledDelayedOrderStatusUpdateActions(true);
        var order = initOrder(OrderStatus.PICKUP, DeliveryType.PICKUP);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(deliveryCheckpointStatus)
        );

        // Отправляем трек
        notifyTracksHelper.notifyTracks(deliveryTrack);

        // Должен быть создан QC на отмену заказа
        var qc = queuedCallService.findQueuedCalls(CheckouterQCType.DELAYED_ORDER_CHANGE_ACTION, order.getId());
        Assertions.assertEquals(1, qc.size());

        // Заказ не отменяется сразу
        order = orderService.getOrder(order.getId());
        Assertions.assertEquals(OrderStatus.PICKUP, order.getStatus());

        // Заказ должен отмениться после выполнения QC
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.DELAYED_ORDER_CHANGE_ACTION, order.getId());
        order = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    public void shouldCancelOrderAfterSortingCenterReturnTrackCheckpointForDeliveryType() throws Exception {
        checkouterProperties.setEnabledDelayedOrderStatusUpdateActions(true);
        var order = initOrder(OrderStatus.DELIVERY, DeliveryType.DELIVERY);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(
                        DeliveryCheckpointStatus.SORTING_CENTER_RETURN_RETURNED.getId())
        );

        // Отправляем трек
        notifyTracksHelper.notifyTracks(deliveryTrack);

        //Заказ должен отмениться синхронно
        var qc = queuedCallService.findQueuedCalls(
                CheckouterQCType.DELAYED_ORDER_CHANGE_ACTION, order.getId());
        Assertions.assertTrue(qc.isEmpty());

        order = orderService.getOrder(order.getId());
        Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    public void shouldCancelOrderAfterSortingCenterReturnTrackCheckpointForPickupType() throws Exception {
        checkouterProperties.setEnabledDelayedOrderStatusUpdateActions(true);
        var order = initOrder(OrderStatus.DELIVERY, DeliveryType.PICKUP);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(
                        DeliveryCheckpointStatus.SORTING_CENTER_RETURN_RETURNED.getId())
        );

        // Отправляем трек
        notifyTracksHelper.notifyTracks(deliveryTrack);

        //Заказ должен отмениться синхронно
        var qc = queuedCallService.findQueuedCalls(
                CheckouterQCType.DELAYED_ORDER_CHANGE_ACTION, order.getId());
        Assertions.assertTrue(qc.isEmpty());

        order = orderService.getOrder(order.getId());
        Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    private Order initOrder(OrderStatus status, DeliveryType deliveryType) throws Exception {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        params.setDeliveryType(deliveryType);
        Order order = orderCreateHelper.createOrder(params);

        order = orderStatusHelper.proceedOrderToStatus(order, status);

        Track track = new Track("trackCode", DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        track = orderDeliveryHelper.addTrack(order, track, ClientInfo.SYSTEM);
        orderUpdateService.updateTrackSetTrackerId(
                order.getId(),
                track.getBusinessId(),
                DeliveryTrackMetaProvider.getDeliveryTrackMeta(String.valueOf(order.getId())).getId()
        );
        return order;
    }
}
