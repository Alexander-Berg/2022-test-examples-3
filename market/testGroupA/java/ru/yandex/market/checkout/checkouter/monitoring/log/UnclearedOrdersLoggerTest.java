package ru.yandex.market.checkout.checkouter.monitoring.log;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrdersCountingService;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.providers.CashParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;

public class UnclearedOrdersLoggerTest extends AbstractWebTestBase {

    @Autowired
    private ZooTask unclearedOrdersTask;
    @Autowired
    private OrdersCountingService ordersCountingService;
    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    public void shouldExecuteCorrectly() {
        unclearedOrdersTask.runOnce();
    }

    @Test
    public void onEmptyDatabase() {
        final int count = ordersCountingService.countUnclearedPostpaidOrders(LocalDateTime.now(getClock()));
        assertEquals(0, count);
    }

    @Test
    @Disabled
    public void withCashPayment() {
        // Arrange
        final var parameters = CashParametersProvider.createOrderWithTwoItems(true);
        final var createdOrder = orderCreateHelper.createOrder(parameters);
        assertTrue(createdOrder.isFulfilment());
        orderStatusHelper.proceedOrderToStatusWithoutTask(createdOrder, OrderStatus.DELIVERED);
        trustMockConfigurer.mockWholeTrust();
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, createdOrder.getId());

        final var paidOrder = orderService.getOrder(createdOrder.getId());
        assertNotNull(paidOrder.getPaymentId());
        assertEquals(PaymentGoal.ORDER_POSTPAY, paidOrder.getPayment().getType());
        assertEquals(PaymentStatus.IN_PROGRESS, paidOrder.getPayment().getStatus());

        // Испортим платёж и заклирим его
        transactionTemplate.execute(ts -> {
            masterJdbcTemplate.update("update payment set total_amount = 1, status = ? where id = ?",
                    PaymentStatus.CLEARED.getId(), paidOrder.getPaymentId());
            return null;
        });

        // Act
        setFixedTime(getClock().instant().plus(1L, ChronoUnit.DAYS));
        final int countFor10Days = ordersCountingService.countUnclearedPostpaidOrders(
                LocalDateTime.now(getClock()).minus(10L, ChronoUnit.DAYS));
        final int countFor1Days = ordersCountingService.countUnclearedPostpaidOrders(
                LocalDateTime.now(getClock()).minus(1L, ChronoUnit.DAYS));

        // Assert
        assertEquals(0, countFor10Days);
        assertEquals(0, countFor1Days);
    }
}
