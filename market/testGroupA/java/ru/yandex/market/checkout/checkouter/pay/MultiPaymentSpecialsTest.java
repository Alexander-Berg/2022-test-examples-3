package ru.yandex.market.checkout.checkouter.pay;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MultiPaymentSpecialsTest extends AbstractPaymentTestBase {

    @Autowired
    PaymentService paymentService;
    @Autowired
    RefundService refundService;
    private List<Order> orders = new ArrayList<>();
    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;

    @BeforeEach
    public void prepareOrders() {
        orders = new ArrayList<>();
        trustMockConfigurer.resetRequests();
    }

    @AfterEach
    public void cleanup() {
        clearFixed();
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что нормально можно оплатить FF и не-FF заказ.")
    @Test
    public void payFFandNonFFOrder() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 18);
        calendar.set(Calendar.MINUTE, 18);
        setFixedTime(calendar.toInstant());

        Order dropshipOrder = dropshipDeliveryHelper.createDropshipOrder();
        assertThat(dropshipOrder.isFulfilment(), equalTo(false));
        Order unpaidBlueOrder = orderServiceTestHelper.createUnpaidBlueOrder(null);
        assertThat(unpaidBlueOrder.isFulfilment(), equalTo(true));
        orders.add(dropshipOrder);
        orders.add(unpaidBlueOrder);

        if (dropshipOrder.getStatus() != OrderStatus.UNPAID) {
            orderStatusHelper.proceedOrderToStatus(dropshipOrder, OrderStatus.UNPAID);
        }

        // Инициируем платеж
        ordersPay((new PaymentParameters()).getReturnPath());

        // Убедимся, что платеж в нужном статусе
        Order createdOrder = orderService.getOrder(orders.get(0).getId());
        Payment createdPayment = createdOrder.getPayment();
        Assertions.assertEquals(PaymentStatus.INIT, createdPayment.getStatus());

        // Проверяем, что чек есть и в нужном статусе
        receiptTestHelper.checkReceiptForOrders(orders, ReceiptStatus.NEW);
    }

    private List<Long> asIds() {
        return orders.stream().map(Order::getId).collect(Collectors.toList());
    }

    private ResultActions ordersPay(String returnPath) throws Exception {
        ResultActions resultActions = ordersPay(asIds(), returnPath);
        orders = getOrdersFromDB();
        return resultActions;
    }

    private List<Order> getOrdersFromDB() {
        return new ArrayList<>(
                orderService.getOrders(
                        asIds()
                ).values()
        );
    }
}
