package ru.yandex.market.checkout.checkouter.delivery.dropship;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DropshipOrderStatusTest extends AbstractWebTestBase {

    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;

    private Order order;

    @BeforeEach
    public void setUp() throws Exception {
        order = dropshipDeliveryHelper.createDropshipOrder();
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
    }

    @Test
    public void shouldAllowToUpdateToProcessingSubstatus() throws Exception {
        Order processing = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        assertThat(processing.getSubstatus(), is(OrderSubstatus.STARTED));

        ClientInfo clientInfo = new ClientInfo(ClientRole.SHOP, order.getShopId());

        ParcelBox parcelBox = parcelBoxHelper.provideOneBoxForOrder(processing);

        parcelBoxHelper.putBoxes(
                processing.getId(),
                processing.getDelivery().getParcels().get(0).getId(),
                Collections.singletonList(parcelBox),
                clientInfo
        );

        Order readyToShip = orderStatusHelper.updateOrderStatus(
                order.getId(), clientInfo,
                OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP
        );
        assertThat(readyToShip.getStatus(), is(OrderStatus.PROCESSING));
        assertThat(readyToShip.getSubstatus(), is(OrderSubstatus.READY_TO_SHIP));

        Order shipped = orderStatusHelper.updateOrderStatus(
                order.getId(), clientInfo,
                OrderStatus.PROCESSING, OrderSubstatus.SHIPPED
        );
        assertThat(shipped.getStatus(), is(OrderStatus.PROCESSING));
        assertThat(shipped.getSubstatus(), is(OrderSubstatus.SHIPPED));
    }

    @Test
    public void shouldNotAllowToUpdateDirectlyToShipped() throws Exception {
        Order processing = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        assertThat(processing.getStatus(), is(OrderStatus.PROCESSING));
        assertThat(processing.getSubstatus(), is(OrderSubstatus.STARTED));

        orderStatusHelper.updateOrderStatusForActions(
                order.getId(),
                new ClientInfo(ClientRole.SHOP, order.getShopId()),
                OrderStatus.PROCESSING,
                OrderSubstatus.SHIPPED
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(OrderStatusNotAllowedException.NOT_ALLOWED_CODE))
                .andExpect(jsonPath("$.message").value(
                        "No permission to set substatus SHIPPED for order " + order.getId()
                                + " with status PROCESSING and substatus STARTED"
                ));
    }
}
