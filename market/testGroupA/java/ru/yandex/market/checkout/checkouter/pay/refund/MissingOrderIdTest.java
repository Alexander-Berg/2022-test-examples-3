package ru.yandex.market.checkout.checkouter.pay.refund;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.item.BalanceOrderIdService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


public class MissingOrderIdTest extends AbstractServicesTestBase {

    @Autowired
    private BalanceOrderIdService balanceOrderIdService;

    @Test
    public void testCreateRefundRequest() {
        Parameters parameters = BlueParametersProvider.prepaidBlueOrderParameters();
        Order order = parameters.getOrder();
        order.setId(1L);
        OrderItem first = order.getItems().iterator().next();

        assertNull(first.getBalanceOrderId());

        balanceOrderIdService.fixOrderServiceId(order);

        assertNotNull(first.getBalanceOrderId());
    }
}
