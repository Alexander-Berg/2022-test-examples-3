package ru.yandex.market.checkout.checkouter.pay;


import java.time.LocalTime;
import java.util.List;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.CLEARED;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.HOLD;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.INIT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.WAITING_BANK_DECISION;

public class TinkoffCreditCourierTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private YandexMarketDeliveryHelper marketDeliveryHelper;

    private Order order;

    @BeforeEach
    void initOrder() {
        Parameters parameters = marketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(BlueParametersProvider.DELIVERY_SERVICE_ID)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addDelivery(actualDelivery())
                        .withFreeDelivery()
                        .build())
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        order = orderCreateHelper.createOrder(parameters);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void paymentWaitingStatus(boolean skipHold) {
        Payment payment = orderPayHelper.pay(order.getId());
        assertThat(payment.getStatus(), is(INIT));

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket(), null);

        orderPayHelper.notifyPayment(payment);
        assertThat(paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus(),
                is(WAITING_BANK_DECISION));

        if (!skipHold) {
            trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildHoldCheckBasket());
            trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildHoldCheckBasket(), null);
            orderPayHelper.notifyPayment(payment);
            assertThat(paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus(), is(HOLD));
        }

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);

        orderPayHelper.notifyPayment(payment);
        assertThat(paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus(), is(CLEARED));
    }

    @DisplayName("Проверяем, что заказ перейдет в UNPAID(WAITING_TINKOFF_DECISION) после нотификации траста " +
            "о курьерском флоу")
    @Test
    void moveOrderToWaitingStatusOnNotifyReceived() {
        Payment payment = orderPayHelper.pay(order.getId());
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket(), null);

        orderPayHelper.notifyPayment(payment);

        Order orderAfterNotify = orderService.getOrder(order.getId());
        assertThat(orderAfterNotify.getSubstatus(), is(OrderSubstatus.WAITING_TINKOFF_DECISION));
    }

    @Test
    void shouldNotAllowNewPaymentsWhileWaitingTinkoffDecision() {
        Payment payment = orderPayHelper.pay(order.getId());
        assertThat(payment.getStatus(), is(INIT));

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket(), null);
        orderPayHelper.notifyPayment(payment);
        assertThat(paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus(),
                is(WAITING_BANK_DECISION));

        Assertions.assertThrows(ErrorCodeException.class,
                () -> client.payments().payOrder(order.getId(), order.getBuyer().getUid(),
                        "https://localhost/payment/status/", null, false, null));
    }

    @Test
    void shouldMarkOrderWithCourierFlowProperty() {
        Payment payment = orderPayHelper.pay(order.getId());
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildWaitingBankDecisionCheckBasket(), null);
        orderPayHelper.notifyPayment(payment);
        assertThat(paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus(),
                is(WAITING_BANK_DECISION));
        Order orderAfterNotify = orderService.getOrder(order.getId());
        assertThat(orderAfterNotify.getSubstatus(), is(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(orderAfterNotify.getProperty(OrderPropertyType.TINKOFF_CREDIT_COURIER_FLOW), is(true));
    }

    private ActualDeliveryOption actualDelivery() {
        ActualDeliveryOption option = new ActualDeliveryOption();
        option.setDayFrom(1);
        option.setDayTo(1);
        option.setTimeIntervals(List.of(
                new DeliveryTimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0)),
                new DeliveryTimeInterval(LocalTime.of(14, 0), LocalTime.of(18, 0))));
        option.setPaymentMethods(Sets.newHashSet("YANDEX", "CASH_ON_DELIVERY"));
        option.setDeliveryServiceId(BlueParametersProvider.DELIVERY_SERVICE_ID);
        option.setShipmentDay(0);
        return option;
    }
}
