package ru.yandex.market.checkout.checkouter.delivery.parcel;

import java.util.Collections;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderCreateService;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author mmetlov
 */
public class ParcelDaoTest extends AbstractServicesTestBase {

    @Autowired
    private OrderCreateService orderCreateService;

    @Test
    public void createParcelWithWaybill() {
        Order order = OrderProvider.getBlueOrder();
        Parcel parcel = ParcelProvider.createParcel();
        parcel.setRoute(JsonNodeFactory.instance.objectNode());
        parcel.setCombinatorRouteId(UUID.randomUUID());
        order.getDelivery().setParcels(Collections.singletonList(parcel));
        long id = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order createdOrder = orderService.getOrder(id);
        assertEquals(parcel.getRoute(), createdOrder.getDelivery().getParcels().get(0).getRoute());
        assertEquals(parcel.getCombinatorRouteId(),
                createdOrder.getDelivery().getParcels().get(0).getCombinatorRouteId());
    }
}
