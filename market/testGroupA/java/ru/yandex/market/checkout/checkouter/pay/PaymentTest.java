package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;

/**
 * @author mkasumov
 */

public class PaymentTest extends AbstractPaymentTestBase {

    public boolean divideBasketByItemsInBalance;

    @Autowired
    private PaymentService paymentService;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void holdAndClear(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void holdAndCancel(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.cancelPayment();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void payWithWalletAndCancel(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();
        //do
        trustMockConfigurer.resetRequests();

        paymentTestHelper.initAndHoldPayment(true);
        paymentTestHelper.cancelPayment(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void payWithWalletAndClear(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        paymentTestHelper.initAndHoldPayment(true);
        paymentTestHelper.clearPayment();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void firstFailThenClear(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.notifyPaymentFailed(receiptId);

        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void firstFailThenCancel(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.notifyPaymentFailed(receiptId);

        paymentTestHelper.cancelPayment();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void cancelPaymentInInit(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        paymentTestHelper.initPayment();
        paymentTestHelper.cancelPayment();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void getColorForNonexistentPayment(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        Payment payment = new Payment();
        payment.setId(Long.MAX_VALUE);
        payment.setType(PaymentGoal.UNKNOWN);
        try {
            paymentService.getColor(payment);
            Assertions.fail("IllegalStateException wasn't thrown");
        } catch (IllegalStateException ex) {
            Assertions.assertTrue(ex.getMessage().contains("is associated with incorrect orders"), ex.getMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void getColorForExistingPayment(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        paymentTestHelper.initPayment();
        final Color color = paymentService.getColor(order().getPayment());
        Assertions.assertEquals(Color.BLUE, color);
    }

    @DisplayName(value = "Первый платеж по любому из созданных платежей должен обновлять статус")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void payFirstAndSecondAndNotifyFirst(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        Long receiptId = paymentTestHelper.initPayment();
        long paymentId = order().getPaymentId();
        Payment payment = order().getPayment();
        String balanceOrderId = order().getBalanceOrderId();

        paymentTestHelper.initPayment();

        order().setPaymentId(paymentId);
        order().setPayment(payment);

        paymentTestHelper.notifyPaymentSucceeded(receiptId, false, false);

        Assertions.assertEquals(OrderStatus.PROCESSING, order().getStatus());

        order.set(orderService.getOrder(order.get().getId()));

        Assertions.assertEquals(paymentId, order().getPaymentId().longValue());
        Assertions.assertEquals(balanceOrderId, order().getBalanceOrderId());
    }

    @DisplayName(value = "Второй успешный платеж не должен прицепляться к заказу и должен быть отменен.")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void payFirstAndSecondAndNotifyFirstAndSecond(boolean divideBasketByItemsInBalance) throws Exception {
        this.divideBasketByItemsInBalance = divideBasketByItemsInBalance;
        //prepare
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        createUnpaidOrder();
        //do
        Long firstReceiptId = paymentTestHelper.initPayment();
        long firstPaymentId = order().getPaymentId();
        Payment firstPayment = order().getPayment();

        Long secondReceiptId = paymentTestHelper.initPayment();
        long secondPaymentId = order().getPaymentId();
        Payment secondPayment = order().getPayment();
        String secondBalanceOrderId = order().getBalanceOrderId();

        paymentTestHelper.notifyPaymentSucceeded(secondReceiptId, false, false);

        order.set(orderService.getOrder(order().getId()));

        Assertions.assertEquals(OrderStatus.PROCESSING, order().getStatus());
        Assertions.assertEquals(secondPaymentId, order().getPaymentId().longValue());
        Assertions.assertEquals(secondBalanceOrderId, order().getBalanceOrderId());

        order().setPaymentId(firstPaymentId);
        order().setPayment(firstPayment);

        paymentTestHelper.notifyPaymentSucceeded(firstReceiptId, false, false, PaymentStatus.CANCELLED);

        order.set(orderService.getOrder(order().getId()));

        Assertions.assertEquals(secondPaymentId, order().getPaymentId().longValue());
        Assertions.assertEquals(secondBalanceOrderId, order().getBalanceOrderId());

        firstPayment = paymentService.getPayment(firstPaymentId, ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.CANCELLED, firstPayment.getStatus());

        secondPayment = paymentService.getPayment(secondPaymentId, ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.HOLD, secondPayment.getStatus());
    }
}
