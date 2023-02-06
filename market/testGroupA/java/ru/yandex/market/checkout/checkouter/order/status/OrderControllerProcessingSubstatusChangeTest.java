package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.service.TrackerNotificationService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.SORTING_CENTER_PACKAGING;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.SORTING_CENTER_READY_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED;
import static ru.yandex.market.checkout.providers.DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint;
import static ru.yandex.market.checkout.providers.DeliveryTrackProvider.getDeliveryTrack;

public class OrderControllerProcessingSubstatusChangeTest extends AbstractWebTestBase {

    @Autowired
    private CheckouterClient client;
    @Autowired
    private TrackerNotificationService trackerNotificationService;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Test
    public void shouldAllowToChangeProcessingSubstatuses() {
        Order order = orderCreateHelper.createOrder(new Parameters());
        assertThat(order.getStatus(), is(OrderStatus.PROCESSING));
        assertThat(order.getSubstatus(), is(OrderSubstatus.STARTED));
        updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.PACKAGING);
        updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.READY_TO_SHIP);
        updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.SHIPPED);
    }

    @Test
    public void shouldNotAllowToChangeProcessingSubstatusesBackwards() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Order order = orderCreateHelper.createOrder(new Parameters());
            assertThat(order.getStatus(), is(OrderStatus.PROCESSING));
            assertThat(order.getSubstatus(), is(OrderSubstatus.STARTED));
            updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.PACKAGING);
            updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.STARTED);
        });
    }

    @Test
    public void shouldNotAllowToSkipSubstatuses() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Order order = orderCreateHelper.createOrder(new Parameters());
            assertThat(order.getStatus(), is(OrderStatus.PROCESSING));
            assertThat(order.getSubstatus(), is(OrderSubstatus.STARTED));
            updateSubstatusAndCheckIt(order.getId(), OrderSubstatus.SHIPPED);
        });
    }

    @Test
    public void shouldChangeStatusesByCheckpoints() {
        Order order = createOrderWithTrack();
        assertThat(order.getStatus(), is(OrderStatus.PROCESSING));
        assertThat(order.getSubstatus(), is(OrderSubstatus.STARTED));

        notifyTrackWithCheckpoint(order, SORTING_CENTER_PACKAGING);
        checkOrderSubstatus(order.getId(), OrderSubstatus.PACKAGING);

        notifyTrackWithCheckpoint(order, SORTING_CENTER_READY_TO_SHIP);
        checkOrderSubstatus(order.getId(), OrderSubstatus.READY_TO_SHIP);

        notifyTrackWithCheckpoint(order, SORTING_CENTER_TRANSMITTED);
        checkOrderSubstatus(order.getId(), OrderSubstatus.SHIPPED);
    }

    private void notifyTrackWithCheckpoint(Order order, DeliveryCheckpointStatus deliveryCheckpointStatus) {
        trackerNotificationService.notifyTracks(
                Collections.singletonList(
                        getDeliveryTrack(
                                order.getId().toString(),
                                deliveryTrackCheckpoint(deliveryCheckpointStatus.getId())
                        )
                )
        );
    }

    private void updateSubstatusAndCheckIt(Long orderId, OrderSubstatus substatus) {
        Order orderBeforeUpdate = client.getOrder(orderId, ClientRole.SYSTEM, null);
        String statusUpdateDate = orderBeforeUpdate.getStatusUpdateDateTs();
        client.updateOrderStatus(
                orderId,
                ClientRole.REFEREE,
                123L,
                null,
                OrderStatus.PROCESSING,
                substatus
        );
        Order order = client.getOrder(orderId, ClientRole.SYSTEM, null);
        assertThat(order.getSubstatus(), is(substatus));
        assertEquals(statusUpdateDate, order.getStatusUpdateDateTs());
    }

    private void checkOrderSubstatus(Long orderId, OrderSubstatus substatus) {
        Order order = client.getOrder(orderId, ClientRole.SYSTEM, null);
        assertThat(order.getSubstatus(), is(substatus));
    }

    private Order createOrderWithTrack() {
        Order order = OrderProvider.getOrderWithTracking();
        FulfilmentProvider.fulfilmentize(order);
        order = orderServiceHelper.saveOrder(order);
        return order;
    }
}
