package ru.yandex.market.checkout.checkouter.returns;

import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ReturnOnDeliveryTransmittedToRecipientTest extends AbstractReturnTestBase {
    @Autowired
    private ReturnHelper returnHelper;

    private Order order;

    @BeforeEach
    public void createOrder() {
        Parameters params = defaultBlueOrderParameters();
        params.getOrder().getItems().forEach(item -> item.setCount(10));
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED);
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
    }

    @Test
    void shouldAllowToCreateReturnOnDeliveryTransmittedToRecipient() {
        ReturnOptionsResponse returnOptions = getReturnOptions(order);

        ReturnDelivery delivery = convertOptionToDelivery(returnOptions.getDeliveryOptions().get(0));
        addCompensation(returnOptions);
        returnOptions.setDelivery(delivery);
        returnOptions.setComment("Comment");
        addBankDetails(returnOptions);

        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, returnOptions);

        returnResp = client.returns().getReturn(order.getId(),
                returnResp.getId(), false, ClientRole.SYSTEM, 3331L);

        assertThat(returnResp.getItems(), hasSize(order.getItems().size()));
        assertThat(
                returnResp.getItems().stream().map(ReturnItem::getItemId).collect(Collectors.toList()),
                containsInAnyOrder(order.getItems().stream().map(OrderItem::getId).toArray())
        );

        assertThat(returnResp.getDelivery().getDeliveryServiceId(), equalTo(delivery.getDeliveryServiceId()));
        assertThat(returnResp.getDelivery().getType(), equalTo(delivery.getType()));
        assertThat(returnResp.getStatus(), equalTo(ReturnStatus.STARTED_BY_USER));
        assertThat(returnResp.getComment(), equalTo("Comment"));
        compensationProcessed(returnResp, true);
    }
}
