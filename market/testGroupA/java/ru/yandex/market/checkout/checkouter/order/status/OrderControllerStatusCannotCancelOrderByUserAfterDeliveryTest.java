package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class OrderControllerStatusCannotCancelOrderByUserAfterDeliveryTest extends AbstractWebTestBase {

    private Order order;

    @BeforeEach
    public void prepareOrder() {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @ParameterizedTest
    @EnumSource(value = OrderSubstatus.class)
    public void checkCanNotSetStatusForOrderByUser(OrderSubstatus orderSubstatus) throws Exception {
        orderStatusHelper.updateOrderStatusForActions(
                order.getId(),
                new ClientInfo(ClientRole.USER, order.getBuyer().getUid()),
                OrderStatus.CANCELLED,
                orderSubstatus
        )
                .andExpect(status().is4xxClientError());

        Order order = orderService.getOrder(this.order.getId());
        assertThat(order.getStatus(), is(OrderStatus.DELIVERED));
    }
}
