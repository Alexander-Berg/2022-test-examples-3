package ru.yandex.market.checkout.checkouter.order.status.actions;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для {@link CreateParcelBoxAction}.
 */
class CreateParcelBoxActionTest extends AbstractWebTestBase {

    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;

    @Autowired
    private OrderCreateHelper orderCreateHelper;

    @Test
    public void createParcelBox() {
        Order order = dropshipDeliveryHelper.createDropshipOrder();
        assertTrue(CollectionUtils.isEmpty(order.getDelivery().getParcels().get(0).getBoxes()));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        final Order orderAfter = client.getOrder(new RequestClientInfo(ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid()), OrderRequest.builder(order.getId()).build());

        final Parcel parcel = orderAfter.getDelivery().getParcels().get(0);
        final List<ParcelBox> boxes = parcel.getBoxes();
        assertEquals(1, boxes.size(), "Order must have 1 box");

        final ParcelBox parcelBox = boxes.get(0);
        assertEquals(orderAfter.getId() + "-1", parcelBox.getFulfilmentId());
    }

    @Test
    public void createParcelBoxDbs() {
        Order order = orderCreateHelper.createOrder(WhiteParametersProvider.defaultWhiteParameters());
        assertTrue(CollectionUtils.isEmpty(order.getDelivery().getParcels().get(0).getBoxes()));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        final Order orderAfter = client.getOrder(new RequestClientInfo(ClientInfo.SYSTEM.getRole(),
                ClientInfo.SYSTEM.getUid()), OrderRequest.builder(order.getId()).build());

        final Parcel parcel = orderAfter.getDelivery().getParcels().get(0);
        final List<ParcelBox> boxes = parcel.getBoxes();
        assertEquals(1, boxes.size());
    }
}
