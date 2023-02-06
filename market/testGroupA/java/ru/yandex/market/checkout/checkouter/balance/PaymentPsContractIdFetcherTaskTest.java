package ru.yandex.market.checkout.checkouter.balance;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentOperations;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.BalanceMockHelper;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkouter.jooq.Tables.PAYMENT;

/**
 * @author : poluektov
 * date: 02.04.2019.
 */
public class PaymentPsContractIdFetcherTaskTest extends AbstractWebTestBase {

    @Autowired
    private BalanceMockHelper balanceMockHelper;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentOperations trustPaymentOperations;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private DSLContext dsl;
    @Autowired
    private RefundService refundService;

    private Order order;
    private Payment payment;

    @BeforeEach
    public void before() {
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payment = payHelper.payForOrder(order);
    }

    @Test
    public void testContractIdInitialization() {
        balanceMockHelper.mockWholeBalance();
        trustMockConfigurer.mockCheckBasket(null);
        trustMockConfigurer.mockStatusBasket(null, null);
        trustPaymentOperations.updatePaymentStateFromPaymentSystem(payment, ClientInfo.SYSTEM);
        Payment updatedPayment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertThat(updatedPayment.getPsContractExternalId(), notNullValue());
        assertThat(updatedPayment.getProperties().getTerminalId(), notNullValue());
        assertThat(updatedPayment.getProperties().getTerminalContractId(), notNullValue());
    }

    @Test
    public void testGetTerminalQC() {
        balanceMockHelper.mockWholeBalance();
        trustMockConfigurer.mockCheckBasket(null);
        trustMockConfigurer.mockStatusBasket(null, null);
        queuedCallService.addQueuedCall(CheckouterQCType.FILL_TERMINAL_INFO, payment.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.FILL_TERMINAL_INFO, payment.getId());
        Payment updatedPayment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertThat(updatedPayment.getPsContractExternalId(), notNullValue());
        assertThat(updatedPayment.getProperties().getTerminalId(), notNullValue());
        assertThat(updatedPayment.getProperties().getTerminalContractId(), notNullValue());
    }

    @Test
    public void testQCCreationOnError() {
        trustMockConfigurer.resetAll();
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(
                BalanceMockHelper.BalanceXMLRPCMethod.GetContractInfoByTerminal,
                BalanceMockHelper.BalanceResponseVariant.FAIL);
        CheckBasketParams config = CheckBasketParams.buildPostAuth();
        config.setEmptyTerminal(true);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        payment.setProperties(null);
        transactionTemplate.execute(ts -> dsl.update(PAYMENT)
                .setNull(PAYMENT.PROPERTIES)
                .where(PAYMENT.ID.eq(payment.getId())).execute());
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);

        trustPaymentOperations.updatePaymentStateFromPaymentSystem(payment, ClientInfo.SYSTEM);
        Payment updatedPayment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);

        assertThat(updatedPayment.getProperties(), nullValue());

        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.FILL_TERMINAL_INFO, payment.getId()));
    }

    @Test
    public void testRetryOnQcError() {
        trustMockConfigurer.resetAll();
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(
                BalanceMockHelper.BalanceXMLRPCMethod.GetContractInfoByTerminal,
                BalanceMockHelper.BalanceResponseVariant.FAIL);
        CheckBasketParams config = CheckBasketParams.buildPostAuth();
        config.setEmptyTerminal(true);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        payment.setProperties(null);
        transactionTemplate.execute(ts -> dsl.update(PAYMENT)
                .setNull(PAYMENT.PROPERTIES)
                .where(PAYMENT.ID.eq(payment.getId())).execute());
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);

        queuedCallService.addQueuedCall(CheckouterQCType.FILL_TERMINAL_INFO, payment.getId());
        assertThrows(
                RuntimeException.class,
                () -> queuedCallService.
                        executeQueuedCallSynchronously(CheckouterQCType.FILL_TERMINAL_INFO, payment.getId())
        );
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.FILL_TERMINAL_INFO, payment.getId()));
    }

    @Test
    public void testRefundTerminal() {
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);

        balanceMockHelper.mockWholeBalance();
        trustMockConfigurer.mockCheckBasket(null);
        trustMockConfigurer.mockStatusBasket(null, null);
        queuedCallService.addQueuedCall(CheckouterQCType.FILL_TERMINAL_INFO, payment.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.FILL_TERMINAL_INFO, payment.getId());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        queuedCallService.addQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, order.getId());
        Refund refund =
                refundService.getRefunds(order.getId()).iterator().next();
        assertNotNull(refund.getProperties());
        assertNotNull(refund.getProperties().getTerminalContractId());
        assertNotNull(refund.getProperties().getTerminalId());
    }
}
