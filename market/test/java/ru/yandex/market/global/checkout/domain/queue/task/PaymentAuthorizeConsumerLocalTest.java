package ru.yandex.market.global.checkout.domain.queue.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.configuration.ConfigurationProperties;
import ru.yandex.market.global.checkout.domain.order.OrderCommandService;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentAuthorizeConsumer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentCancelConsumer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentClearConsumer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.checkout.util.PaymentUtil;
import ru.yandex.market.global.common.trust.client.TrustClient;
import ru.yandex.market.global.common.trust.client.dto.BoundPaymentMethod;
import ru.yandex.market.global.common.trust.client.dto.GetPaymentMethodsRequest;
import ru.yandex.market.global.common.trust.client.dto.GetPaymentMethodsResponse;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EPlusActionType;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PaymentAuthorizeConsumerLocalTest extends BaseLocalTest {

    private static final Long UID = 4092490744L;
    //сменить ордер ID ибо у нас в in-memory postgres и trust имеют разные state с разными orderID
    //поэтому trust ругается из-за дублирующихся order-id
    private static final Long ORDER_ID = 100055L;

    private final PaymentAuthorizeConsumer paymentAuthorizeConsumer;
    private final PaymentClearConsumer paymentClearConsumer;
    private final PaymentCancelConsumer paymentCancelConsumer;
    private final OrderCommandService orderCommandService;
    private final TestOrderFactory orderFactory;
    private final TrustClient trustClient;
    private final ConfigurationService configurationProvider;

    @Test
    void getPaymethods() {
        GetPaymentMethodsResponse response = trustClient.getPaymentMethods(GetPaymentMethodsRequest.builder()
                .uid(UID)
                .showBound(true)
                .showEnabled(true)
                .build());

        log.error("Paymethods: {}", response.getBoundPaymentMethods());
    }

    @Test
    void clearSuccessfully() throws InterruptedException {
        Order order = createOrder();

        authorizeOrder(order.getId());

        Thread.sleep(40_000);
        clearOrder(order.getId());
        Thread.sleep(40_000);
    }

    @Test
    void clearWithPlusSuccessfully() throws InterruptedException {

        configurationProvider.mergeValue(ConfigurationProperties.PLUS_MODE, "ON");

        Order order = createOrderWithPlus();

        authorizeOrder(order.getId());

        Thread.sleep(40_000);
        clearOrder(order.getId());
        Thread.sleep(40_000);
    }

    @Test
    void clearApplePaySuccessfully() {
        Order order = createOrderApplePay();

        authorizeOrder(order.getId());
        clearOrder(order.getId());
    }

    @Test
    void unholdSuccessfully() {
        Order order = createOrder();

        authorizeOrder(order.getId());
        cancelOrder(order.getId());
    }

    @Test
    void refundSuccessfully() {
        Order order = createOrder();

        authorizeOrder(order.getId());
        clearOrder(order.getId());
        cancelOrder(order.getId());
    }


    private Order createOrderWithPlus() {
        BoundPaymentMethod payMethod = getBoundPaymentMethod();

        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(d -> d
                        .setUid(UID)
                        .setId(ORDER_ID)
                        .setPlusAction(EPlusActionType.SPEND)
                        .setPlusEarned(0L)
                        .setPlusSpent(100_00L))
                .setupDelivery(d -> d
                        .setRecipientEmail("denr01@yandex.ru")
                        .setRecipientPhone("+78005553535"))
                .setupPayment(p -> p
                        .setTrustPaymethodId(payMethod.getId())
                        .setTrustRegionId(payMethod.getRegionId())
                        .setTrustProductId("1354411205")
                        .setTrustOrderId(null)
                        .setTrustPurchaseToken(null)
                        .setAuthorizeStartedAt(null)
                        .setAuthorizedAt(null)
                        .setClearedAt(null)
                        .setClearStartedAt(null))
                .build()).getOrder();
    }

    private BoundPaymentMethod getBoundPaymentMethod() {
        return trustClient.getPaymentMethods(GetPaymentMethodsRequest.builder()
                        .showEnabled(true)
                        .showBound(true)
                        .uid(UID)
                        .build())
                .getBoundPaymentMethods().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment methods not found!"));
    }

    private Order createOrder() {
        BoundPaymentMethod payMethod = getBoundPaymentMethod();

        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(d -> d
                        .setUid(UID)
                        .setId(ORDER_ID))
                .setupDelivery(d -> d
                        .setRecipientEmail("denr01@yandex.ru")
                        .setRecipientPhone("+78005553535"))
                .setupPayment(p -> p
                        .setTrustPaymethodId(payMethod.getId())
                        .setTrustRegionId(payMethod.getRegionId())
                        .setTrustProductId("1354411205")
                        .setTrustOrderId(null)
                        .setTrustPurchaseToken(null)
                        .setAuthorizeStartedAt(null)
                        .setAuthorizedAt(null)
                        .setClearedAt(null)
                        .setClearStartedAt(null))
                .build()).getOrder();
    }

    private Order createOrderApplePay() {

        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(d -> d
                        .setUid(UID)
                        .setId(ORDER_ID))
                .setupDelivery(d -> d
                        .setRecipientEmail("denr01@yandex.ru")
                        .setRecipientPhone("+78005553535"))
                .setupPayment(p -> p
                        .setTrustPaymethodId(PaymentUtil.APPLE_PAY_PAYMETHOD)
                        .setTrustRegionId(213)
                        .setTrustProductId("1354411205")
                        .setTrustOrderId(null)
                        .setTrustPurchaseToken(null)
                        .setAuthorizeStartedAt(null)
                        .setAuthorizedAt(null)
                        .setPaymentRedirectUrl("https://yandex.ru/")
                        .setAppleToken("12345678sad324fgt63f"))
                .build()).getOrder();
    }

    private void authorizeOrder(long orderId) {
        TestQueueTaskRunner.runTaskThrowOnFail(paymentAuthorizeConsumer, orderId);
    }

    private void clearOrder(long orderId) {
        TestQueueTaskRunner.runTaskThrowOnFail(paymentClearConsumer, orderId);
    }

    private void cancelOrder(long orderId) {
        TestQueueTaskRunner.runTaskThrowOnFail(paymentCancelConsumer, orderId);
    }

}
