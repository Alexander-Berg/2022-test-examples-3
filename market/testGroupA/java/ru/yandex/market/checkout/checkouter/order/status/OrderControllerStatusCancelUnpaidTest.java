package ru.yandex.market.checkout.checkouter.order.status;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * На замену CancelUnpaidStatusTest.
 */

public class OrderControllerStatusCancelUnpaidTest extends AbstractWebTestBase {

    private Order order;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(OrderSubstatus.values())
                .filter(substatus -> substatus.getStatus() == OrderStatus.CANCELLED)
                .map(substatus -> new Object[]{substatus.getStatus(), substatus})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @BeforeEach
    public void prepareOrder() {
        if (order != null) {
            return;
        }
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), is(OrderStatus.UNPAID));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testCancelByShop(OrderStatus status, OrderSubstatus substatus) throws Exception {
        orderStatusHelper.updateOrderStatusForActions(
                order.getId(), new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID), status, substatus
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(
                        "$.message",
                        is("No permission to set status CANCELLED for order " + order.getId() +
                                " with status UNPAID")
                ));
    }
}
