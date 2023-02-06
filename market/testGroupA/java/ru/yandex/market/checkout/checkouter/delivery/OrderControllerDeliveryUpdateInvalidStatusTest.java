package ru.yandex.market.checkout.checkouter.delivery;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;

public class OrderControllerDeliveryUpdateInvalidStatusTest extends AbstractWebTestBase {

    private static final String ERROR_MESSAGE_TEMPLATE = "Action is not allowed for order %d with status %s";
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;

    @Test
    @Epic(Epics.DELIVERY)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя изменять информацию о доставке доставленного заказа")
    public void shouldNotAllowToUpdateDeliveryForDeliveredOrder() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERED);

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(
                d -> d.setAddress(AddressProvider.getAnotherAddress()));

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STATUS_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message")
                        .value(String.format(ERROR_MESSAGE_TEMPLATE, order.getId(), DELIVERED)));
    }
}
