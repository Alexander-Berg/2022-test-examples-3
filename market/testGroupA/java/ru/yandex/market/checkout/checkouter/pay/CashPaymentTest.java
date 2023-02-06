package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.cashier.CashierService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.CashParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class CashPaymentTest extends AbstractPaymentTestBase {

    @Autowired
    private ShopService shopService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private CashierService cashierService;
    @Autowired
    private RefundHelper refundHelper;

    private Order createOrder(boolean freeDelivery) {
        Parameters parameters = CashParametersProvider.createCashParameters(freeDelivery);
        return orderCreateHelper.createOrder(parameters);
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Чекаутер должен создавать компенсационный платеж на сумму заказа.")
    @Test
    public void shouldCreateCashPayment() {
        Order order = createOrder(true);
        ShopMetaData shopMetaData = ShopSettingsHelper.createCustomNewPrepayMeta(FulfilmentProvider.FF_SHOP_ID
                .intValue());
        shopService.updateMeta(FulfilmentProvider.FF_SHOP_ID, shopMetaData);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, order.getId()));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Чекаутер должен создавать компенсационный платеж на стоимость доставки, если она платная")
    @Test
    public void shouldCreateCashPaymentForDelivery() {
        Order order = createOrder(false);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, order.getId()));
    }

    @Test
    public void shouldReturnItemsOfCashPaymentOrder() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();

        Order order = createOrderWithTwoItems();
        updateShopMeta();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());

        Return ret = returnHelper.createReturn(order.getId(), ReturnProvider
                .generateReturnWithDelivery(order, order.getDelivery().getDeliveryServiceId()));
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();

        refundHelper.proceedAsyncRefunds(order.getId());

        Collection<Refund> refunds = refundService.getReturnRefunds(ret);
        assertThat(refunds.isEmpty(), equalTo(false));
        assertTrue(refunds.stream().allMatch(r -> r.getStatus() == RefundStatus.ACCEPTED));
        assertThat(refunds.size(), equalTo(1));
        BigDecimal totalRefundAmount = refunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalRefundAmount, comparesEqualTo(order.getBuyerTotal()));

        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, order.getId()));
    }

    @Test
    public void shouldReturnPartialItemsOfCashPaymentOrder() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();

        Order order = createOrderWithItemsCount(10);

        updateShopMeta();

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());

        Return ret = returnHelper.createReturn(order.getId(), ReturnProvider
                .generatePartialReturnWithDelivery(order, order.getDelivery().getDeliveryServiceId(), 1));
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();

        Collection<Refund> refunds = refundService.getReturnRefunds(ret);
        assertThat(refunds, both(not(empty())).and(not(nullValue())));
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, order.getId()));
    }

    @Test
    public void shouldNotPayCashPaymentTwice() throws Exception {
        Order order = createOrder(false);
        updateShopMeta();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());

        PagedPayments pagedPayments = paymentTestHelper.getPagedPayments(order.getId(), PaymentGoal.ORDER_POSTPAY);
        assertThat(pagedPayments.getItems(), hasSize(1));
        Payment payment = Iterables.getOnlyElement(pagedPayments.getItems());
        List<Receipt> receipts = receiptService.findByPayment(payment, ReceiptType.INCOME);
        assertThat(receipts, hasSize(1));

        cashierService.createAndBindCashPayment(order.getId());

        pagedPayments = paymentTestHelper.getPagedPayments(order.getId(), PaymentGoal.ORDER_POSTPAY);
        assertThat(pagedPayments.getItems(), hasSize(1));
        payment = Iterables.getOnlyElement(pagedPayments.getItems());
        receipts = receiptService.findByPayment(payment, ReceiptType.INCOME);
        assertThat(receipts, hasSize(1));
    }

    @Test
    public void shouldNotUpdateAlreadyFinishedPayment() throws Exception {
        Order order = createOrder(false);
        updateShopMeta();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());

        PagedPayments pagedPayments = paymentTestHelper.getPagedPayments(order.getId(), PaymentGoal.ORDER_POSTPAY);
        assertThat(pagedPayments.getItems(), hasSize(1));
        Payment payment = Iterables.getOnlyElement(pagedPayments.getItems());
        assertThat(payment.getStatus(), equalTo(PaymentStatus.IN_PROGRESS));

        payHelper.notifyPaymentClear(payment);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertThat(payment.getStatus(), equalTo(PaymentStatus.CLEARED));

        cashierService.createAndBindCashPayment(order.getId());
        Payment paymentAfter = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertThat(paymentAfter.getStatus(), equalTo(PaymentStatus.CLEARED));
    }


    private void updateShopMeta() {
        ShopMetaData shopMetaData = ShopSettingsHelper.createCustomNewPrepayMeta(123);
        shopService.updateMeta(123, shopMetaData);
        shopMetaData = ShopSettingsHelper.createCustomNewPrepayMeta(
                FulfilmentProvider.FF_SHOP_ID.intValue()
        );
        shopService.updateMeta(FulfilmentProvider.FF_SHOP_ID, shopMetaData);
    }

    private Order createOrderWithItemsCount(int count) {
        Parameters parameters = defaultBlueOrderParameters(true);
        parameters.getOrder().getItems().forEach(i -> i.setCount(count));
        parameters.setFreeDelivery(true);
        parameters.setDeliveryServiceId(null);
        parameters.setColor(Color.BLUE);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setShopId(123L);

        return orderCreateHelper.createOrder(parameters);
    }

    private Order createOrderWithTwoItems() {
        Parameters parameters = defaultBlueOrderParameters();
        OrderItem anotherItem = OrderItemProvider.getAnotherOrderItem();
        anotherItem.setWeight(1000L);
        FulfilmentProvider.addFulfilmentFields(
                anotherItem,
                FulfilmentProvider.ANOTHER_TEST_SKU,
                FulfilmentProvider.ANOTHER_TEST_SHOP_SKU,
                FulfilmentProvider.ANOTHER_FF_SHOP_ID
        );
        parameters.addShopMetaData(FulfilmentProvider.ANOTHER_FF_SHOP_ID, ShopSettingsHelper.getDefaultMeta());
        parameters.getOrder().addItem(anotherItem);
        parameters.setFreeDelivery(false);
        parameters.setDeliveryServiceId(null);
        parameters.setColor(Color.BLUE);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setShopId(123L);
        Order newOrder = orderCreateHelper.createOrder(parameters);
        return orderService.getOrder(newOrder.getId());
    }
}
