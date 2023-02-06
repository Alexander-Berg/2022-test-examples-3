package ru.yandex.market.checkout.checkouter.tasks.v2.paymentstatusinspector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskResult;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.mediabilling.MediabillingMockConfigurer;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.common.util.date.DateUtil.truncDay;
import static ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder.PLACEHOLDER_PAYMENT_ID;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.getDefaultMeta;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildFailCheckBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

public class AbstractInspectExpiredPaymentTaskV2Test extends AbstractPaymentTestBase {

    private static final PaymentStatus[] STATUSES_TO_CHECK = new PaymentStatus[]{PaymentStatus.INIT};

    @Autowired
    private OrderCompletionService orderCompletionService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private MediabillingMockConfigurer mediabillingMockConfigurer;

    @Autowired
    InspectExpiredPaymentPrepayTaskV2 inspectExpiredPaymentPrepayTaskV2;
    @Autowired
    InspectExpiredPaymentTinkoffCreditTaskV2 inspectExpiredPaymentTinkoffCreditTaskV2;
    @Autowired
    InspectExpiredPaymentBnplTaskV2 inspectExpiredPaymentBnplTaskV2;
    @Autowired
    InspectExpiredPaymentStationSubscriptionTaskV2 inspectExpiredPaymentStationSubscriptionTaskV2;

    @BeforeEach
    public void setUp() {
        trustMockConfigurer.mockWholeTrust();
        mediabillingMockConfigurer.mockWholeMediabilling();
        shopService.updateMeta(OrderProvider.SHOP_ID, getDefaultMeta());
    }

    @Test
    public void prepaidPaymentExpired() {
        ZonedDateTime fakeCreateDateTime = LocalDateTime.of(2018, Month.JANUARY, 30, 23, 30, 0)
                .atZone(ZoneId.systemDefault());
        Instant fakeNow = fakeCreateDateTime.toInstant();
        setFixedTime(fakeNow);
        long orderId = OrderServiceHelper.createPrepaidOrder(orderCreateService, orderUpdateService,
                orderCompletionService);
        Order order = orderService.getOrder(orderId);
        Payment payment = client.payments().payOrder(orderId,
                order.getBuyer().getUid(),
                "https://localhost/payment/status/" + PLACEHOLDER_PAYMENT_ID + "/",
                null, false, null);
        assertEquals(PaymentStatus.INIT, payment.getStatus());
        assertEquals(truncDay(Date.from(fakeNow)), truncDay(payment.getStatusExpiryDate()));

        Instant fakeCheckMoment = fakeCreateDateTime.plusDays(7).toInstant();
        setFixedTime(fakeCheckMoment);
        Collection<Payment> expiredPayments = paymentService.loadPaymentsWithExpiredStatus(PaymentGoal.ORDER_PREPAY,
                STATUSES_TO_CHECK);
        assertThat(expiredPayments, hasSize(1));
        trustMockConfigurer.mockCheckBasket(buildFailCheckBasket());
        trustMockConfigurer.mockStatusBasket(buildFailCheckBasket(), null);

        var result = inspectExpiredPaymentPrepayTaskV2.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        expiredPayments = paymentService.loadPaymentsWithExpiredStatus(PaymentGoal.ORDER_PREPAY, STATUSES_TO_CHECK);
        assertThat(expiredPayments, empty());
    }

    @Test
    public void tinkoffCreditPaymentExpired() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getStatus(), equalTo(OrderStatus.UNPAID));
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));

        orderPayHelper.payForOrder(createdOrder);
        Order order = orderService.getOrder(createdOrder.getId());
        assertEquals(PaymentStatus.HOLD, order.getPayment().getStatus());
        Collection<Payment> expiredPayments =
                paymentService.loadPaymentsByGoal(PaymentGoal.TINKOFF_CREDIT, new PaymentStatus[]{PaymentStatus.HOLD},
                        90, null, null);
        assertThat(expiredPayments, hasSize(1));
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);

        var result = inspectExpiredPaymentTinkoffCreditTaskV2.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());

        Payment payment = paymentService.getPayment(order.getPayment().getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());
        expiredPayments = paymentService.loadPaymentsByGoal(PaymentGoal.TINKOFF_CREDIT,
                new PaymentStatus[]{PaymentStatus.HOLD}, 90, null, null);
        assertThat(expiredPayments, empty());
    }

    @Test
    public void stationSubscriptionPaymentExpired() {
        checkouterProperties.setEnableStationSubscription(true);
        var parameters = BlueParametersProvider
                .defaultBlueOrderParametersWithItems(OrderItemProvider.defaultOrderItem());
        parameters.getReportParameters().setYaSubscriptionOffer(true);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getStatus(), equalTo(OrderStatus.UNPAID));
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.YANDEX));
        assertThat(createdOrder.getPaymentSubmethod(), equalTo(PaymentSubmethod.STATION_SUBSCRIPTION));

        orderPayHelper.payForOrder(createdOrder, false);
        Order savedOrder = orderService.getOrder(createdOrder.getId());
        assertEquals(PaymentStatus.IN_PROGRESS, savedOrder.getPayment().getStatus());
        Collection<Payment> expiredPayments = paymentService.loadPaymentsByGoal(
                PaymentGoal.YA_PLUS_SUBSCRIPTION,
                new PaymentStatus[]{PaymentStatus.IN_PROGRESS},
                90,
                null,
                null
        );
        assertThat(expiredPayments, hasSize(1));

        TaskResult result = inspectExpiredPaymentStationSubscriptionTaskV2.run(TaskRunType.ONCE);
        assertEquals(TaskStageType.SUCCESS, result.getStage());

        Payment payment = paymentService.getPayment(savedOrder.getPayment().getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());
        expiredPayments = paymentService.loadPaymentsByGoal(
                PaymentGoal.YA_PLUS_SUBSCRIPTION,
                new PaymentStatus[]{PaymentStatus.IN_PROGRESS},
                90,
                null,
                null
        );
        assertThat(expiredPayments, empty());
    }
}
