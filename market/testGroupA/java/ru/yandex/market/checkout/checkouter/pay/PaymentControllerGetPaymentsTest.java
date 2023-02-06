package ru.yandex.market.checkout.checkouter.pay;

import java.util.Objects;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.PaymentGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PaymentControllerGetPaymentsTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentGetHelper paymentGetHelper;

    private Order order;
    private Payment payment;

    @BeforeEach
    public void setUp() {
        if (order == null) {
            Parameters parameters = new Parameters();
            parameters.setPaymentMethod(PaymentMethod.YANDEX);

            order = orderCreateHelper.createOrder(parameters);
            payment = orderPayHelper.payForOrder(order);
        }
    }

    @Test
    public void shouldGetPaymentsWithSystemRole() throws Exception {
        PagedPayments orderPayments = paymentGetHelper.getOrderPayments(order.getId(), ClientInfo.SYSTEM);

        Assertions.assertTrue(orderPayments.getItems().stream().anyMatch(p -> Objects.equals(p.getId(),
                payment.getId())));
    }

    @Test
    public void shouldGetPaymentsWithCallCenterOperatorRole() throws Exception {
        PagedPayments orderPayments = paymentGetHelper.getOrderPayments(
                order.getId(),
                new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 123L)
        );

        Assertions.assertTrue(orderPayments.getItems().stream().anyMatch(p -> Objects.equals(p.getId(),
                payment.getId())));
    }

    @Test
    public void shouldGetPaymentsWithClientRole() throws Exception {
        PagedPayments orderPayments = paymentGetHelper.getOrderPayments(
                order.getId(),
                new ClientInfo(ClientRole.USER, order.getBuyer().getUid())
        );

        Assertions.assertTrue(orderPayments.getItems().stream().anyMatch(p -> Objects.equals(p.getId(),
                payment.getId())));
    }

    @Test
    public void shouldGetPaymentsWithRefereeRole() throws Exception {
        PagedPayments orderPayments = paymentGetHelper.getOrderPayments(
                order.getId(),
                new ClientInfo(ClientRole.REFEREE, order.getBuyer().getUid())
        );

        Assertions.assertTrue(orderPayments.getItems().stream().anyMatch(p -> Objects.equals(p.getId(),
                payment.getId())));
    }

    @Test
    public void shouldNotGetPaymentsWithShopRole() throws Exception {
        PagedPayments orderPayments = paymentGetHelper.getOrderPayments(
                order.getId(), new ClientInfo(ClientRole.SHOP, order.getShopId())
        );

        assertThat(orderPayments.getItems(), hasSize(0));
    }

    @Test
    public void shouldReturnNotFoundIfRequestedForNotExistingOrder() throws Exception {
        Random random = new Random();
        long orderId = random.nextLong() % 1_000_000;
        paymentGetHelper.getOrderPaymentsForActions(orderId, ClientInfo.SYSTEM)
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestIfRoleUserClientIdIsNull() throws Exception {
        paymentGetHelper.getOrderPaymentsForActions(order.getId(), new ClientInfo(ClientRole.USER, null))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestIfRoleRefereeClientIdIsNull() throws Exception {
        paymentGetHelper.getOrderPaymentsForActions(order.getId(), new ClientInfo(ClientRole.REFEREE, null))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void shouldReturnBadRequestIfClientIdIsWrong() throws Exception {
        paymentGetHelper.getOrderPaymentsForActions(order.getId(), new ClientInfo(ClientRole.USER,
                order.getBuyer().getUid() + 1))
                .andExpect(status().isNotFound());
    }


}
