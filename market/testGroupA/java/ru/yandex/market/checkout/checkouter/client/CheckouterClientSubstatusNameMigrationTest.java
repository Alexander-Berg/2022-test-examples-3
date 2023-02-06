package ru.yandex.market.checkout.checkouter.client;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;

public class CheckouterClientSubstatusNameMigrationTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    private long orderId;

    @BeforeEach
    public void setUp() {
        Order order = orderServiceHelper.prepareOrder();
        order.getDelivery().setParcels(Collections.singletonList(createParcel()));
        order = orderServiceHelper.saveOrder(order);

        orderId = order.getId();

        Order updatedOrder = client.updateOrderStatus(
                order.getId(),
                ClientRole.SYSTEM,
                null,
                null,
                PROCESSING,
                READY_TO_SHIP
        );
        assertEquals(Long.valueOf(orderId), updatedOrder.getId());
    }

    @Test
    public void setAndExpectsDeprecatedSubstatus() {
        OrderStatus targetStatus = OrderStatus.CANCELLED;
        OrderSubstatus targetSubstatus = DELIVERY_SERVICE_UNDELIVERED;

        Order updatedOrder = client.updateOrderStatus(
                orderId,
                ClientRole.SYSTEM,
                null,
                null,
                targetStatus,
                targetSubstatus
        );

        assertEquals(Long.valueOf(orderId), updatedOrder.getId());
        assertEquals(targetStatus, updatedOrder.getStatus());
        assertEquals(targetSubstatus, updatedOrder.getSubstatus());
    }

    @Test
    public void setNewAndExpectsDeprecatedSubstatus() {
        OrderStatus targetStatus = OrderStatus.CANCELLED;

        Order updatedOrder = client.updateOrderStatus(
                orderId,
                ClientRole.SYSTEM,
                null,
                null,
                targetStatus,
                DELIVERY_SERVICE_UNDELIVERED
        );

        assertEquals(Long.valueOf(orderId), updatedOrder.getId());
        assertEquals(targetStatus, updatedOrder.getStatus());
        assertEquals(DELIVERY_SERVICE_UNDELIVERED, updatedOrder.getSubstatus());
    }

    private Parcel createParcel() {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setWeight(1L);
        parcelBox.setWidth(2L);
        parcelBox.setHeight(3L);
        parcelBox.setDepth(4L);

        Parcel parcel = new Parcel();
        parcel.setBoxes(Collections.singletonList(parcelBox));
        return parcel;
    }
}
