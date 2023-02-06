package ru.yandex.market.checkout.checkouter.refund;


import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.storage.payment.RefundWritingDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCObjectType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.queuedcalls.QueuedCallService;
import ru.yandex.market.queuedcalls.QueuedCallType;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SubsidyAsyncRefundTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "promo";
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private RefundWritingDao refundDao;

    private Refund subsidyRefund;

    @BeforeEach
    public void prepareDraftSubsidy() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        //чтобы были субсидии за офферы и за доставку
        parameters.setupPromo(PROMO_CODE);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.PICKUP, LoyaltyDiscount.builder()
                        .discount(BigDecimal.valueOf(50L))
                        .promoKey(PROMO_CODE)
                        .promoType(PromoType.MARKET_COUPON).build());
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);

        Payment subsidyPayment =
                paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY).get(0);
        subsidyRefund = refundService.getRefunds(order.getId()).stream()
                .filter(refund -> Objects.equals(subsidyPayment.getId(), refund.getPaymentId()))
                .findFirst().orElse(null);

        assertNotNull(subsidyRefund);
        assertEquals(subsidyPayment.getTotalAmount(), subsidyRefund.getAmount());
        assertThat(subsidyRefund.getStatus(), equalTo(RefundStatus.DRAFT));
    }


    @Test
    @DisplayName("Queued call проводит рефанд до статуса ACCEPTED")
    public void refundShouldBeProcessedToAcceptedStatusByQC() {
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        validateTrustCallRefundLines();
        Refund refundInDb = refundService.getRefund(subsidyRefund.getId());

        assertThat(refundInDb.getStatus(), equalTo(RefundStatus.ACCEPTED));
    }


    @Test
    @DisplayName("Рефанд остается в DRAFT статусе, если не смогли создать в трасте")
    public void refundShouldStayDraftIfCreateFailed() throws IOException {
        trustMockConfigurer.mockBadRequestRefund();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        validateTrustCallRefundLines();
        Refund refundInDb = refundService.getRefund(subsidyRefund.getId());

        assertThat(refundInDb.getStatus(), equalTo(RefundStatus.DRAFT));
        assertThat(refundInDb.getTrustRefundKey(), allOf(
                hasProperty("trustRefundId", nullValue())
        ));
    }

    @Test
    @DisplayName("Рефанд остается в IN_PROGRESS статусе, если упала операция doRefund")
    public void refundShouldStayInProgressIfDoRefundFailed() {
        trustMockConfigurer.mockBadRequestDoRefund();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        validateTrustCallRefundLines();
        Refund refundInDb = refundService.getRefund(subsidyRefund.getId());

        assertThat(refundInDb.getStatus(), equalTo(RefundStatus.IN_PROGRESS));
        assertThat(refundInDb.getTrustRefundKey(), allOf(
                hasProperty("trustRefundId", notNullValue())
        ));
    }

    @Test
    @DisplayName("Рефанд проталкивается по статусам после первоначального фейла")
    public void retryProcessRefundShouldWorkCorrectly() throws IOException {
        //не смогли создать в трасте
        trustMockConfigurer.mockBadRequestRefund();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        //спустя время, ретрай создал рефанд в трасте, но зафейлился на doRefund
        jumpToFuture(1, HOURS);
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCreateRefund(null);
        trustMockConfigurer.mockBadRequestDoRefund();
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        validateTrustCallRefundLines();
        Refund refundFirstRetry = refundService.getRefund(subsidyRefund.getId());
        assertThat(refundFirstRetry.getStatus(), equalTo(RefundStatus.IN_PROGRESS));
        assertThat(refundFirstRetry.getTrustRefundKey(), allOf(
                hasProperty("trustRefundId", notNullValue())
        ));

        //спустя еще  время, финальный успешный ретрай
        jumpToFuture(1, HOURS);
        trustMockConfigurer.mockDoRefund();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        Refund refundSecondRefund = refundService.getRefund(subsidyRefund.getId());
        assertThat(refundSecondRefund.getStatus(), equalTo(RefundStatus.ACCEPTED));
    }

    @Test
    public void shouldNotRetryFailedRefunds() {
        subsidyRefund.setStatus(RefundStatus.FAILED);
        transactionTemplate.execute(tx -> {
            refundDao.updateStatus(subsidyRefund, ClientInfo.SYSTEM);
            return null;
        });
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        Refund failedRefund = refundService.getRefund(subsidyRefund.getId());
        assertThat(failedRefund.getStatus(), equalTo(RefundStatus.FAILED));
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PROCESS_REFUND, failedRefund.getId()));
    }

    @Test
    public void shouldResolveSubsidyRefundsRace() {
        //todo выпилить после MARKETCHECKOUT-17116
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo(PROMO_CODE);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        Order cancelledOrder = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND);

        //заказ отменен в доставке, будет попытка создать ORDER_REFUND_SUBSIDY_PAYMENT
        //но если уже есть ORDER_REFUND, его не нужно создавать, иначе рефанды задублируются
        Set<QueuedCallType> existingQC = queuedCallService.existingCallsForObjId(
                CheckouterQCObjectType.ORDER, cancelledOrder.getId());
        assertThat(existingQC, hasSize(1));
        assertThat(existingQC.iterator().next(), equalTo(CheckouterQCType.ORDER_REFUND));
    }

    @Test
    void refundShouldWorkProperlyWhenSubsidyBalanceOrderIdIsMissing() {
        // Устанавливаем ffSubsidyBalanceOrderId в null для всех OrderItem'ов заказа.
        Order order = orderService.getOrder(subsidyRefund.getOrderId());
        dropSubsidyBalanceOrderIdForAllItems(order);
        order = orderService.getOrder(subsidyRefund.getOrderId());
        order.getItems().forEach(orderItem -> assertNull(orderItem.getFfSubsidyBalanceOrderId()));

        // Отсутствующие ffSubsidyBalanceOrderId должны быть восстановлены, и в трасте должен быть создан
        // корректный рефанд.
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        validateTrustCallRefundLines();
        Refund refundInDb = refundService.getRefund(subsidyRefund.getId());

        assertThat(refundInDb.getStatus(), equalTo(RefundStatus.ACCEPTED));
    }

    private void dropSubsidyBalanceOrderIdForAllItems(Order order) {
        jdbcTemplate.update("update order_item set ff_subsidy_balance_order_id = null where order_id = ?",
                order.getId());
    }

    private void validateTrustCallRefundLines() {
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        trustMockConfigurer.trustMock().verify(anyRequestedFor(urlEqualTo("/trust-payments/v2/refunds"))
                .withRequestBody(
                        matching(".*" + subsidyRefund.getAmount().setScale(2, RoundingMode.HALF_UP).toString() + ".*"))
        );
    }

}
