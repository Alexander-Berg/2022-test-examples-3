package ru.yandex.market.checkout.checkouter.pay;

import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.item.BalanceOrderIdService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_REFUND_STUB;

public class PartialRefundTest extends AbstractWebTestBase {

    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private BalanceOrderIdService balanceOrderIdService;
    @Autowired
    private TrustPaymentOperations trustPaymentOperationsMock;

    @Test
    void shouldPartialRefund() {
        var order1 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        var order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderPayHelper.payForOrders(List.of(order1, order2));


        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order1.getId()), OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order2.getId()), OrderStatus.CANCELLED);
        // создается QC на рефанд
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order2.getId()));
        Long order2Id = order2.getId();
        // эмулируем баг с пустым balance_order_id на доставке
        transactionTemplate.execute(tx -> {
            masterJdbcTemplate.update(
                    "update order_delivery set balance_order_id = null where order_id = " + order2Id);
            return null;
        });
        assertThat(orderService.getOrder(order2Id).getDelivery().getBalanceOrderId(), nullValue());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order2.getId());
        assertThat(orderService.getOrder(order2Id).getDelivery().getBalanceOrderId(), notNullValue());
        refundHelper.proceedAsyncRefunds(order1.getId(), order2.getId());

        order2 = orderService.getOrder(order2.getId());

        Collection<Refund> refunds = refundService.getRefunds(order2.getId());
        assertEquals(1, refunds.size());
        assertThat(refunds, everyItem(hasProperty("status", is(RefundStatus.ACCEPTED))));

        var createRefundRequest = trustMockConfigurer.servedEvents()
                .stream()
                .filter(event -> CREATE_REFUND_STUB.equals(event.getStubMapping().getName()))
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .findFirst()
                .orElseThrow();

        JsonTest.checkJsonMatcher(createRefundRequest,
                "$.orders.*.order_id",
                hasItem(order2Id + "-delivery"));
    }

    @Test
    public void testClearedEventToCancelledOrder() throws Exception {
        //prepare
        stockStorageConfigurer.mockOkForUnfreeze();
        stockStorageConfigurer.mockOkForForceUnfreeze();

        Order order1 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Order order3 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        List<Order> orders = List.of(order1, order2, order3);
        orders.forEach(balanceOrderIdService::fixOrderServiceId);

        trustMockConfigurer.mockWholeTrust();

        //do
        orderPayHelper.payForOrders(orders);

        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order1);
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(order2.getId()),
                OrderStatus.PROCESSING);
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(order2.getId()), OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order3);

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        // check
        Mockito.verify(trustPaymentOperationsMock).partialUnholdPayment(Mockito.any(), Mockito.any());
    }

    @Configuration
    public static class Config {

        @Bean
        @Primary
        public TrustPaymentOperations trustPaymentOperationsMock(TrustPaymentOperations trustPaymentOperations) {
            return Mockito.spy(trustPaymentOperations);
        }
    }
}
