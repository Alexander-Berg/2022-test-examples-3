package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class OrderControllerStatusChangeToPreviousStatusTest extends AbstractWebTestBase {

    private Order order;


    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{OrderStatus.PROCESSING, OrderStatus.UNPAID},
                new Object[]{OrderStatus.DELIVERY, OrderStatus.PROCESSING},
                new Object[]{OrderStatus.DELIVERED, OrderStatus.DELIVERY}
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldNotAllowToUpdateToPreviousStatus(OrderStatus orderStatus, OrderStatus previousStatus)
            throws Exception {
        //setup
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, orderStatus);

        //do
        orderStatusHelper.updateOrderStatusForActions(order.getId(), ClientInfo.SYSTEM, previousStatus, null)
                .andExpect(status().isBadRequest());
    }
}
