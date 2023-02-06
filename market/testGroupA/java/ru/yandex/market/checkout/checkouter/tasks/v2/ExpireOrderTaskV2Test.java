package ru.yandex.market.checkout.checkouter.tasks.v2;

import java.time.temporal.ChronoUnit;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ExpireOrderTaskV2Test extends AbstractPaymentTestBase {

    @Autowired
    private ExpireOrderTaskV2 expireOrderTaskV2;
    @Value("${checkouter.credit.payment.paymentExpireTimeoutInMinutes}")
    private Integer creditExpireTimeoutMinutes;
    @Value("${market.checkouter.order.status.unpaid.timeout.minutes}")
    private Integer prepaidExpireTimeoutMinutes;
    @Autowired
    private RefundHelper refundHelper;

    @Test
    public void expireUnpaidOrderStepByStep() {
        prepareOrder();
        prepareOrder();

        jumpToFuture(prepaidExpireTimeoutMinutes + 1, ChronoUnit.MINUTES);
        assertEquals(2, expireOrderTaskV2.countItemsToProcess());
        assertEquals(2, expireOrderTaskV2.prepareBatch().size());

        expireOrderTaskV2.prepareBatch().forEach(this::processAndCheckOrder);
    }

    private Order prepareOrder() {
        Parameters whiteParameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.UNPAID);
        order = orderService.getOrder(order.getId());
        assertNotNull(order.getStatusExpiryDate());

        return order;
    }

    private void processAndCheckOrder(long orderId) {
        try {
            expireOrderTaskV2.processItem(orderId);
            var order = orderService.getOrder(orderId);
            assertThat(order.getStatus(), equalTo(OrderStatus.CANCELLED));
            assertThat(order.getSubstatus(), equalTo(OrderSubstatus.USER_NOT_PAID));
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void expireUnpaidOrderEntireTaskRun() {
        var order = prepareOrder();

        jumpToFuture(prepaidExpireTimeoutMinutes + 1, ChronoUnit.MINUTES);
        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.CANCELLED));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.USER_NOT_PAID));
    }

    @Test
    public void testUnpaidCreditPaymentExpiring() {
        orderServiceTestHelper.createUnpaidBlueOrder(order -> order.setPaymentMethod(PaymentMethod.CREDIT));

        jumpToFuture(creditExpireTimeoutMinutes - 1, ChronoUnit.MINUTES);
        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        reloadOrder();
        assertEquals(OrderStatus.UNPAID, order().getStatus());

        orderServiceTestHelper.createUnpaidBlueOrder(order -> order.setPaymentMethod(PaymentMethod.CREDIT));

        jumpToFuture(creditExpireTimeoutMinutes + 1, ChronoUnit.MINUTES);
        result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        reloadOrder();
        assertEquals(OrderStatus.CANCELLED, order().getStatus());
        assertEquals(OrderSubstatus.USER_NOT_PAID, order().getSubstatus());
    }

    @Test
    public void testUnpaidBluePaymentExpiring() {
        orderServiceTestHelper.createUnpaidFFBlueOrder();

        jumpToFuture(prepaidExpireTimeoutMinutes - 1, ChronoUnit.MINUTES);
        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        reloadOrder();
        assertEquals(OrderStatus.UNPAID, order().getStatus());

        orderServiceTestHelper.createUnpaidFFBlueOrder();
        jumpToFuture(prepaidExpireTimeoutMinutes + 1, ChronoUnit.MINUTES);
        result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        reloadOrder();
        assertEquals(OrderStatus.CANCELLED, order().getStatus());
        assertEquals(OrderSubstatus.USER_NOT_PAID, order().getSubstatus());
    }

    @Test
    public void shouldNotExpireFFOrderFromProcessing() {
        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId());

        assertTrue(order.isFulfilment());
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
        assertNull(order.getStatusExpiryDate());

        jumpToFuture(30, ChronoUnit.DAYS);

        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
    }

    @Test
    public void shouldNotExpireMarketDeliveryOrderFromProcessing() {
        Order order = orderCreateHelper.createOrder(DropshipDeliveryHelper.getDropshipPrepaidParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId());

        assertFalse(order.isFulfilment());
        assertThat(order.getDelivery().getDeliveryPartnerType(), equalTo(DeliveryPartnerType.YANDEX_MARKET));
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
        assertNull(order.getStatusExpiryDate());

        jumpToFuture(30, ChronoUnit.DAYS);
        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
    }

    @Test
    public void shouldNotExpireNotFFOrderFromProcessing() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId());

        assertFalse(order.isFulfilment());
        assertThat(order.getDelivery().getDeliveryPartnerType(), equalTo(DeliveryPartnerType.SHOP));
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
        assertNull(order.getStatusExpiryDate());

        jumpToFuture(30, ChronoUnit.DAYS);

        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
    }

    @Test
    public void shouldNotExpireShopDeliveryOrderFromProcessing() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId());

        assertFalse(order.isFulfilment());
        assertThat(order.getDelivery().getDeliveryPartnerType(), equalTo(DeliveryPartnerType.SHOP));
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
        assertNull(order.getStatusExpiryDate());

        jumpToFuture(30, ChronoUnit.DAYS);
        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
    }

    @Test
    public void shouldNotExpireDigitalOrderFromProcessing() {

        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId());

        assertNull(order.getStatusExpiryDate());

        jumpToFuture(31, ChronoUnit.MINUTES);
        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
    }

    @Test
    public void shouldExpireTinkoffCreditOrderFromUnpaid() {
        Order order = orderServiceTestHelper.createUnpaidBlue1POrder(o -> {
            o.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        });
        Payment payment = paymentHelper.payForOrderWithoutNotification(order);
        paymentHelper.notifyWaitingBankDecision(payment);
        order = orderService.getOrder(order.getId());

        assertNotNull(order.getStatusExpiryDate());

        jumpToFuture(15, ChronoUnit.DAYS);
        var result = expireOrderTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        refundHelper.proceedAsyncRefunds(order.getId());
        //async call to cancel credit payment
        var creditUnholdCount = trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .filter(url -> url.contains(
                        "/credit/" + payment.getBasketKey().getPurchaseToken() + "/unhold"))
                .count();

        assertThat("Must be 1 unhold call in trust", creditUnholdCount, equalTo(1L));

        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.CANCELLED));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.USER_NOT_PAID));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        paymentHelper.notifyPaymentCancel(payment);
        assertThat(
                orderService.getOrder(order.getId()).getPayment().getStatus(),
                equalTo(PaymentStatus.CANCELLED));
    }
}
