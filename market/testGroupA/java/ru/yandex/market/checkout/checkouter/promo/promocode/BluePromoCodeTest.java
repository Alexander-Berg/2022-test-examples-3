package ru.yandex.market.checkout.checkouter.promo.promocode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.qameta.allure.Epic;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.refund.ItemsRefundStrategy;
import ru.yandex.market.checkout.checkouter.pay.refund.SubsidyRefundStrategy;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

/**
 * @author Nikolai Iusiumbeli
 * date: 26/01/2018
 */
public class BluePromoCodeTest extends AbstractWebTestBase {

    private static final String REAL_PROMO_CODE = "REAL-PROMO-CODE";
    private static final BigDecimal PRECISION = new BigDecimal("0.01");

    private static final BigDecimal FF_SUBSIDY1 = BigDecimal.valueOf(123.45);
    private static final BigDecimal FF_SUBSIDY2 = BigDecimal.valueOf(789.10);

    @Autowired
    private FulfillmentConfigurer fulfillmentConfigurer;
    @Autowired
    private CipherService reportCipherService;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private RefundService refundService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ItemsRefundStrategy itemsRefundStrategy;
    @Autowired
    private SubsidyRefundStrategy subsidyRefundStrategy;

    private Parameters parameters;
    private OrderItem orderItem1;
    private OrderItem orderItem2;

    @BeforeEach
    public void setUp() throws Exception {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();

        parameters = defaultBlueOrderParameters();

        orderItem1 = OrderItemProvider.getOrderItem();
        orderItem2 = OrderItemProvider.getOrderItem();
        orderItem2.setWareMd5(orderItem1.getWareMd5() + "_item2");
        OrderItemProvider.patchShowInfo(orderItem2, reportCipherService);

        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(REAL_PROMO_CODE));
        parameters.getOrder().setItems(asList(orderItem1, orderItem2));

        trustMockConfigurer.mockWholeTrust();

        parameters.getLoyaltyParameters()
                .addLoyaltyDiscount(orderItem1, new LoyaltyDiscount(FF_SUBSIDY1, PromoType.MARKET_COUPON))
                .addLoyaltyDiscount(orderItem2, new LoyaltyDiscount(FF_SUBSIDY2, PromoType.MARKET_COUPON));

        parameters.setMockLoyalty(true);
        parameters.getReportParameters().setShopSupportsSubsidies(false);
        fulfillmentConfigurer.configure(parameters);
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, что промокоды применяются к синим заказам, даже если был выбран постоплатный способ")
    @Test
    public void testPostpaidCheckout() {
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        trustMockConfigurer.trustMock().verify(
                anyRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL))
                        .withRequestBody(
                                matching(".*" + FF_SUBSIDY1.setScale(2, RoundingMode.HALF_UP) + ".*"))
                        .withRequestBody(
                                matching(".*" + FF_SUBSIDY2.setScale(2, RoundingMode.HALF_UP) + ".*"))
        );
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, что для 3p отправляем субсидию, а для 1p - нет")
    @Test
    public void test1pAnd3pSubsidies() {
        ReportGeneratorParameters reportParameters = parameters.getReportParameters();
        reportParameters.overrideItemInfo(orderItem1.getFeedOfferId()).setSupplierType(SupplierType.FIRST_PARTY);
        reportParameters.overrideItemInfo(orderItem2.getFeedOfferId()).setSupplierType(SupplierType.THIRD_PARTY);

        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        Order dbOrder = orderService.getOrder(order.getId());

        OrderItem item1 = dbOrder.getItem(orderItem1.getFeedOfferId());
        OrderItem item2 = dbOrder.getItem(orderItem2.getFeedOfferId());

        assertThat(item1.getSupplierType(), is(SupplierType.FIRST_PARTY));
        assertThat(item1.getPrices().getSubsidy(), closeTo(BigDecimal.ZERO, PRECISION));
        assertThat(item1.getPrices().getBuyerSubsidy(), closeTo(BigDecimal.ZERO, PRECISION));

        assertThat(item2.getSupplierType(), is(SupplierType.THIRD_PARTY));
        assertThat(item2.getPrices().getSubsidy(), closeTo(FF_SUBSIDY2, PRECISION));
        assertThat(item2.getPrices().getBuyerSubsidy(), closeTo(FF_SUBSIDY2, PRECISION));

        //но не передаем в баланс в случае 1p
        trustMockConfigurer.trustMock().verify(
                anyRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL))
                        .withRequestBody(
                                matching(".*" + FF_SUBSIDY2.setScale(2, RoundingMode.HALF_UP).toString() + ".*"))
                        .withRequestBody(
                                notMatching(".*" + FF_SUBSIDY1.setScale(2, RoundingMode.HALF_UP).toString() + ".*"))
        );

        payHelper.refundAllOrderItems(order);
        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND);
        trustMockConfigurer.mockCheckBasket(buildPostAuth(), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("First check passed");
        });
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        trustMockConfigurer.trustMock().verify(
                anyRequestedFor(urlEqualTo("/trust-payments/v2/refunds"))
                        .withRequestBody(notMatching(".*" + FF_SUBSIDY1.setScale(2, RoundingMode.HALF_UP).toString()
                                + ".*"))
                        .withRequestBody(matching(".*" + FF_SUBSIDY2.setScale(2, RoundingMode.HALF_UP).toString() + "" +
                                ".*"))
        );
    }

    @Test
    public void testFreeItemAndFreeDelivery() {
        parameters.getLoyaltyParameters().clearDiscounts();
        parameters.getLoyaltyParameters()
                .addLoyaltyDiscount(
                        orderItem1,
                        new LoyaltyDiscount(orderItem1.getBuyerPrice(), PromoType.MARKET_COUPON)
                )
                .addLoyaltyDiscount(
                        orderItem2,
                        new LoyaltyDiscount(orderItem2.getBuyerPrice(), PromoType.MARKET_COUPON)
                )
                .addDeliveryDiscount(
                        null,
                        new LoyaltyDiscount(new BigDecimal(Integer.MAX_VALUE), PromoType.YANDEX_EMPLOYEE)
                );
        parameters.setFreeDelivery(true);

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getItemsTotal(), numberEqualsTo(0L));
        assertThat(order.getTotal(), numberEqualsTo(0L));

    }

    @Test
    @DisplayName("Если заказ уже отменен, то QC на создание субсидии успешно завершается")
    public void testIgnoreSubsidyCreationForCancelledOrder() {
        orderItem1.setCount(5);
        orderItem2.setCount(5);
        parameters.getLoyaltyParameters().addDeliveryDiscount(
                null,
                new LoyaltyDiscount(new BigDecimal(Integer.MAX_VALUE), PromoType.YANDEX_EMPLOYEE)
        );
        Order order = orderCreateHelper.createOrder(parameters);
        transactionTemplate.execute(ts -> {
            queuedCallService.addQueuedCall(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
            return null;
        });
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
    }

    private Refund findRefundByGoal(Order order, PaymentGoal goal) {
        Collection<Payment> payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, goal);
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        Payment payment = payments.stream().filter(p -> p.getType() == goal)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Продолбали " + goal.name() + " платеж"));
        return refunds.stream().filter(r -> r.getPaymentId().longValue() == payment.getId().longValue())
                .findAny()
                .orElseThrow(() -> new RuntimeException("Продолбали " + goal.name() + " рефанд"));
    }
}
