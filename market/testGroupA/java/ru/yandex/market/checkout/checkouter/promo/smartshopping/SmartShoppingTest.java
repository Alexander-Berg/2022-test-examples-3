package ru.yandex.market.checkout.checkouter.promo.smartshopping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.google.common.collect.Lists;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.collections.CollectionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.refund.ItemsRefundStrategy;
import ru.yandex.market.checkout.checkouter.pay.refund.SubsidyRefundStrategy;
import ru.yandex.market.checkout.checkouter.promo.AbstractPromoTestBase;
import ru.yandex.market.checkout.checkouter.promo.util.Utils;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.checkout.util.loyalty.model.CoinDiscountEntry;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.math.BigDecimal.ZERO;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCoinPromo;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCouponPromo;
import static ru.yandex.market.checkout.checkouter.promo.util.Utils.addCartErrorsChecks;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.AddressProvider.getAddress;
import static ru.yandex.market.checkout.test.providers.AddressProvider.getAnotherAddress;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildWithBasketKeyConfig;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount.createCoin;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyParameters.DeliveryDiscountsMode.FORCE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COIN_ALREADY_USED;

/**
 * @author sergeykoles
 * Created on: 06.06.18
 */
public class SmartShoppingTest extends AbstractPromoTestBase {

    private static final String REAL_PROMO_CODE = "REAL-PROMO-CODE";
    private static final BigDecimal PRECISION = new BigDecimal("0.01");

    private static final BigDecimal COUPON_DISCOUNT_1P = BigDecimal.valueOf(13.45);
    private static final BigDecimal COUPON_DISCOUNT_3P = BigDecimal.valueOf(23.10);
    private static final BigDecimal COUPON_DISCOUNT = COUPON_DISCOUNT_1P.add(COUPON_DISCOUNT_3P);

    private static final BigDecimal COIN1_DISCOUNT_1P = BigDecimal.valueOf(3.45);
    private static final BigDecimal COIN1_DISCOUNT_3P = BigDecimal.valueOf(2.45);
    private static final BigDecimal COIN1_DISCOUNT = COIN1_DISCOUNT_1P.add(COIN1_DISCOUNT_3P);
    private static final BigDecimal COIN2_DISCOUNT_3P = COIN1_DISCOUNT_3P.multiply(BigDecimal.valueOf(2));
    private static final BigDecimal COIN3_DISCOUNT_3P = BigDecimal.valueOf(6.99);

    private static final BigDecimal TOTAL_DISCOUNT_1P = COUPON_DISCOUNT_1P.add(COIN1_DISCOUNT_1P);
    private static final BigDecimal TOTAL_DISCOUNT_3P = COUPON_DISCOUNT_3P
            .add(COIN1_DISCOUNT_3P)
            .add(COIN2_DISCOUNT_3P)
            .add(COIN3_DISCOUNT_3P);

    private static final String COIN1_3_PROMO_KEY = "COIN1_3_PROMO_KEY";
    private static final String COIN2_PROMO_KEY = "COIN2_PROMO_KEY";
    private static final long COIN1_ID = 113442L; //random enough :-)
    private static final long COIN2_ID = 244311L;
    private static final long COIN3_ID = 311244L;

    @Autowired
    private CipherService reportCipherService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private CuratorFramework curator;
    @Autowired
    private ItemsRefundStrategy itemsRefundStrategy;
    @Autowired
    private SubsidyRefundStrategy subsidyRefundStrategy;

    private Parameters parameters;
    private OrderItem orderItem1p;
    private OrderItem orderItem3p;

    @BeforeEach
    public void init() throws Exception {
        parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        orderItem1p = OrderItemProvider.getOrderItem();
        orderItem3p = OrderItemProvider.getOrderItem();
        orderItem3p.setWareMd5(orderItem1p.getWareMd5() + "_item2");
        OrderItemProvider.patchShowInfo(orderItem3p, reportCipherService);

        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(REAL_PROMO_CODE));
        parameters.getOrder().setItems(asList(orderItem1p, orderItem3p));

        trustMockConfigurer.mockWholeTrust();

        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters
                // 1P item's promos
                .addLoyaltyDiscount(orderItem1p, LoyaltyDiscount.builder()
                        .promoType(PromoType.MARKET_COUPON)
                        .promoKey(LoyaltyDiscount.PROMOCODE_PROMO_KEY)
                        .discount(COUPON_DISCOUNT_1P)
                        .promocode(REAL_PROMO_CODE)
                        .build())
                .addLoyaltyDiscount(
                        orderItem1p, createCoin(COIN1_DISCOUNT_1P, COIN1_3_PROMO_KEY, COIN1_ID)
                )

                // 3P item's promos
                .addLoyaltyDiscount(orderItem3p, LoyaltyDiscount.builder()
                        .promoType(PromoType.MARKET_COUPON)
                        .promoKey(LoyaltyDiscount.PROMOCODE_PROMO_KEY)
                        .discount(COUPON_DISCOUNT_3P)
                        .build())
                .addLoyaltyDiscount(
                        orderItem3p, createCoin(COIN1_DISCOUNT_3P, COIN1_3_PROMO_KEY, COIN1_ID)
                )
                .addLoyaltyDiscount(
                        orderItem3p,
                        createCoin(COIN2_DISCOUNT_3P, COIN2_PROMO_KEY, COIN2_ID)
                )
                .addLoyaltyDiscount(
                        orderItem3p, createCoin(COIN3_DISCOUNT_3P, COIN1_3_PROMO_KEY, COIN3_ID)
                );

        parameters.getOrder().setCoinIdsToUse(Lists.newArrayList(COIN1_ID, COIN2_ID, COIN3_ID));

        parameters.setMockLoyalty(true);
        parameters.getReportParameters().setShopSupportsSubsidies(false);
        fulfillmentConfigurer.configure(parameters);
        parameters.setWeight(BigDecimal.valueOf(1));
        parameters.setDimensions("10", "10", "10");
        ReportGeneratorParameters reportParameters = parameters.getReportParameters();
        reportParameters.overrideItemInfo(orderItem1p.getFeedOfferId()).setSupplierType(SupplierType.FIRST_PARTY);
        reportParameters.overrideItemInfo(orderItem3p.getFeedOfferId()).setSupplierType(SupplierType.THIRD_PARTY);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Отмена мультиоплаченных заказов с субсидией из DELIVERED. Проверяем что субсидии рефандятся.")
    @Test
    public void cancelSubsidiaryDeliveredOrders() throws Exception {
        Order order1 = orderCreateHelper.createOrder(parameters);
        Order order2 = orderCreateHelper.createOrder(parameters);
        List<Long> orderIds = Arrays.asList(order1.getId(), order2.getId());

        String returnPath = (new PaymentParameters()).getReturnPath();
        ordersPay(orderIds, returnPath, order1.getBuyer().getUid());

        //эмулируем проверку статуса субсидийных платежей
        setFixedTime(getClock().instant().plus(1, ChronoUnit.HOURS));
        runInspectorTask();
        checkSubsidyPaymentsCleared(order1.getId());
        checkSubsidyPaymentsCleared(order2.getId());

        Order canceledOrder = orderUpdateService.updateOrderStatus(
                order1.getId(),
                OrderStatus.CANCELLED,
                OrderSubstatus.CUSTOM);
        Assertions.assertEquals(OrderStatus.CANCELLED, canceledOrder.getStatus());
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, canceledOrder.getId());
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        Collection<Refund> refunds = refundService.getRefunds(canceledOrder.getId());
        Assertions.assertEquals(2, refunds.size());
        Set<PaymentGoal> refundTypes = refunds.stream().map(r -> r.getPayment().getType()).collect(Collectors.toSet());
        Assertions.assertTrue(refundTypes.containsAll(Arrays.asList(PaymentGoal.ORDER_PREPAY, PaymentGoal.SUBSIDY)));

    }

    private void checkSubsidyPaymentsCleared(long orderId) {
        List<Payment> subsidyPaymentsForOrder1 = paymentService.getPayments(orderId, ClientInfo.SYSTEM, PaymentGoal
                .SUBSIDY);
        Assertions.assertEquals(1, subsidyPaymentsForOrder1.size());
        Assertions.assertEquals(PaymentStatus.CLEARED, subsidyPaymentsForOrder1.get(0).getStatus());
    }

    private void ordersPay(List<Long> orderIds, String returnPath, Long uid) throws Exception {
        //init payment
        MockHttpServletRequestBuilder builder = post("/orders/payment/")
                .contentType(MediaType.APPLICATION_JSON)
                .content((new JSONArray(orderIds)).toString())
                .param("uid", uid.toString());
        if (returnPath != null) {
            builder.param("returnPath", returnPath);
        }
        mockMvc.perform(builder).andExpect(status().is2xxSuccessful());

        Payment createdPayment = orderService.getOrder(orderIds.get(0)).getPayment();

        //hold payment
        mockMvc.perform(
                post("/payments/" + createdPayment.getId() + "/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("status", BasketStatus.success.name())
                        .content((new JSONArray(orderIds)).toString())
                        .param("uid", uid.toString()))
                .andExpect(status().isOk());

        orderIds.forEach(
                oid -> {
                    orderUpdateService.updateOrderStatus(oid, OrderStatus.DELIVERY);
                }
        );

        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        Collection<Order> orders = orderService.getOrders(orderIds).values();

        orders.forEach(
                o -> {
                    assertThat(o.getStatus(), is(OrderStatus.DELIVERY));
                    assertThat(o.getPayment().getStatus(), is(PaymentStatus.CLEARED));
                }
        );
    }


    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, что для 3p отправляем субсидию, а для 1p - нет в случае, если есть скидки SmartShopping")
    @Test
    public void test1pAnd3pSubsidies() {
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        Order dbOrder = orderService.getOrder(order.getId());

        OrderItem item1p = dbOrder.getItem(orderItem1p.getFeedOfferId());
        OrderItem item3p = dbOrder.getItem(orderItem3p.getFeedOfferId());

        assertThat(item1p.getSupplierType(), is(SupplierType.FIRST_PARTY));
        assertThat(item1p.getPrices().getSubsidy(), closeTo(ZERO, PRECISION));
        assertThat(item1p.getPrices().getBuyerSubsidy(), closeTo(ZERO, PRECISION));

        assertThat(item3p.getSupplierType(), is(SupplierType.THIRD_PARTY));

        assertThat(item3p.getPrices().getSubsidy(), closeTo(TOTAL_DISCOUNT_3P, PRECISION));
        assertThat(item3p.getPrices().getBuyerSubsidy(), closeTo(TOTAL_DISCOUNT_3P, PRECISION));

        //но не передаем в баланс в случае 1p
        trustMockConfigurer.trustMock().verify(
                anyRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL))
                        .withRequestBody(
                                matching(".*" + TOTAL_DISCOUNT_3P.setScale(2,
                                        RoundingMode.HALF_UP) + ".*"))
                        .withRequestBody(
                                notMatching(".*" + TOTAL_DISCOUNT_1P.setScale(2,
                                        RoundingMode.HALF_UP).toString() + ".*"))
        );

        payHelper.refundAllOrderItems(order);
        trustMockConfigurer.mockCheckBasket(buildPostAuth(), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("First check passed");
        });
        trustMockConfigurer.mockCheckBasket(buildWithBasketKeyConfig(null), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs("First check passed")
                    .willSetStateTo(Scenario.STARTED);
        });
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND);
        trustMockConfigurer.trustMock().verify(
                anyRequestedFor(urlEqualTo("/trust-payments/v2/refunds"))
                        .withRequestBody(notMatching(".*" + TOTAL_DISCOUNT_1P.setScale(2,
                                RoundingMode.HALF_UP).toString()
                                + ".*"))
                        .withRequestBody(matching(".*" + TOTAL_DISCOUNT_3P.setScale(2,
                                RoundingMode.HALF_UP).toString() + ".*"))
        );
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем корректность формирования промо-данных (OrderPromos) заказа по SmartShopping")
    @Test
    public void testAppliedCoinOrderPromos() {
        LoggingComparatorHelper logging = new LoggingComparatorHelper();


        Comparator<PromoDefinition> dbPromoDefinitionComparator = comparing(PromoDefinition::getType,
                logging.natural("type"))
                .thenComparing(PromoDefinition::getMarketPromoId, logging.natural("marketPromoId"))
                .thenComparing(PromoDefinition::getCoinId, logging.natural("coinId"))
                .thenComparing(PromoDefinition::getPromoCode, logging.natural("promoCode"));

        Comparator<OrderPromo> dbOrderPromoComparator =
                comparing(OrderPromo::getPromoDefinition, dbPromoDefinitionComparator)
                        .thenComparing(OrderPromo::getBuyerItemsDiscount, logging.natural("buyerItemsDiscount"))
                        .thenComparing(OrderPromo::getDeliveryDiscount, logging.natural("deliveryDiscount"))
                        .thenComparing(OrderPromo::getBuyerSubsidy, logging.natural("buyerSubsidy"))
//              когда появятся скидки на доставку для монеток - добавить
                        .thenComparing(OrderPromo::getSubsidy, logging.natural("subsidy"));


        Comparator<PromoDefinition> apiPromoDefinitionComparator = comparing(PromoDefinition::getType, logging
                .natural("type"))
                .thenComparing(PromoDefinition::getMarketPromoId, logging.natural("marketPromoId"))
                .thenComparing(PromoDefinition::getCoinId, logging.natural("coinId"));
        Comparator<OrderPromo> apiOrderPromoComparator = comparing(OrderPromo::getPromoDefinition,
                apiPromoDefinitionComparator)
                .thenComparing(OrderPromo::getBuyerItemsDiscount, logging.natural("buyerItemsDiscount"))
                .thenComparing(OrderPromo::getDeliveryDiscount, logging.natural("deliveryDiscount"));

        List<OrderPromo> expectedOrderPromos =
                Lists.newArrayList(
                        coupon(REAL_PROMO_CODE, COUPON_DISCOUNT, COUPON_DISCOUNT_3P, COUPON_DISCOUNT_3P),
                        marketCoin(COIN1_3_PROMO_KEY, COIN1_ID, COIN1_DISCOUNT, COIN1_DISCOUNT_3P,
                                COIN1_DISCOUNT_3P),
                        marketCoin(COIN2_PROMO_KEY, COIN2_ID, COIN2_DISCOUNT_3P, COIN2_DISCOUNT_3P, COIN2_DISCOUNT_3P),
                        marketCoin(COIN1_3_PROMO_KEY, COIN3_ID, COIN3_DISCOUNT_3P, COIN3_DISCOUNT_3P, COIN3_DISCOUNT_3P)
                );

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        Order dbOrder = orderService.getOrder(createdOrder.getId());

        // сравниваем апишным компаратором
        assertCollectionEqual(
                createdOrder.getPromos(),
                expectedOrderPromos,
                apiOrderPromoComparator,
                logging
        );

        // сравниваем компаратором для данных из DB
        assertCollectionEqual(
                dbOrder.getPromos(),
                expectedOrderPromos,
                dbOrderPromoComparator,
                logging
        );
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем корректность формирования промо-данных (ItemPromos) заказа по SmartShopping")
    @Test
    public void testAppliedCoinItemPromos() {
        List<ItemPromo> expectedItem1pPromos = Arrays.asList(
                new ItemPromo(PromoDefinition.marketCoinPromo(COIN1_3_PROMO_KEY, null, null, COIN1_ID, null, null),
                        COIN1_DISCOUNT_1P, ZERO, ZERO),
                new ItemPromo(
                        PromoDefinition.marketCouponPromo(
                                LoyaltyDiscount.PROMOCODE_PROMO_KEY, null, null, REAL_PROMO_CODE, "LOYALTY"
                        ),
                        COUPON_DISCOUNT_1P, ZERO, ZERO)
        );

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        Order dbOrder = orderService.getOrder(createdOrder.getId());

        // сравниваем ItemPromos с ожидаемыми
        assertThat(
                dbOrder.getItem(orderItem1p.getOfferItemKey()).getPromos(),
                containsInAnyOrder(expectedItem1pPromos.toArray(new ItemPromo[0]))
        );

        assertThat(
                createdOrder.getItem(orderItem1p.getFeedOfferId()).getPromos(),
                containsInAnyOrder(expectedItem1pPromos.toArray(new ItemPromo[0]))
        );

    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем передачу доставки в лоялти, и применение монеток на доставку")
    @Test
    public void testCoinsOnDelivery() {
        BigDecimal deliveryDiscount = new BigDecimal(100);
        parameters.getOrder().getCoinIdsToUse().add(1111L);
        LoyaltyDiscount pickupCoin = createCoin(deliveryDiscount, "pickupPromoKey", 1111L);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.PICKUP, pickupCoin);
        parameters.setDeliveryType(PICKUP);
        parameters.getOrder().getDelivery().setType(null);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertOrderHasCoinPromoForFreeDelivery(createdOrder, pickupCoin);
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем что будет если лоялти вернет на доставку 'новый' тип промо " +
            "(которого не предполагается получать по доставке), для которого НЕ настроен мапинг типов промо на типы " +
            "лоялти")
    @Test
    public void testAbsolutelyNewPromoTypeOnDelivery() throws Exception {
        BigDecimal deliveryDiscount = new BigDecimal(100);
        LoyaltyDiscount pickupCoin = new LoyaltyDiscount(deliveryDiscount, PromoType.MARKET_BLUE);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.PICKUP, pickupCoin);
        parameters.setDeliveryType(PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);

        MultiCart multicart = orderCreateHelper.cart(parameters);
        assertThat(multicart.getCarts(), hasSize(1));
        Order cart = multicart.getCarts().get(0);
        cart.getDeliveryOptions()
                .forEach(opt -> {
                    log.info("checking {}", opt);
                    assertTrue(CollectionUtils.isEmpty(opt.getPromos()));
                });
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем что будет если лоялти вернет на доставку 'новый' тип промо " +
            "(которого не предполагается получать по доставке), для которого УЖЕ настроен мапинг типов промо на типы " +
            "лоялти")
    @Test
    public void testNewPromoTypeOnDelivery() throws Exception {
        BigDecimal deliveryDiscount = new BigDecimal(1);
        LoyaltyDiscount pickupPromo = new LoyaltyDiscount(deliveryDiscount, PromoType.MARKET_COUPON);
        parameters.getLoyaltyParameters().addDeliveryDiscount(DeliveryType.PICKUP, pickupPromo);
        parameters.setDeliveryType(PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);

        MultiCart multicart = orderCreateHelper.cart(parameters);
        assertThat(multicart.getCarts(), hasSize(1));
        Order cart = multicart.getCarts().get(0);

        cart.getDeliveryOptions().stream()
                .filter(opt -> opt.getType() == PICKUP)
                .forEach(opt -> {
                    log.info("checking {}", opt);
                    List<ItemPromo> coinPromos = opt.getPromos().stream()
                            .filter(promo -> promo.getType() == PromoType.MARKET_COUPON).collect(Collectors.toList());
                    assertThat(coinPromos, hasSize(1));
                    ItemPromo promo = coinPromos.get(0);
                    assertThat(promo.getPromoDefinition().getMarketPromoId(), is(pickupPromo.getPromoKey()));
                    assertThat(promo.getPromoDefinition().getCoinId(), is(pickupPromo.getCoinId()));
                    assertThat(promo.getBuyerDiscount(), is(pickupPromo.getDiscount()));
                    assertThat(opt.getPrices().getBuyerDiscount(), is(pickupPromo.getDiscount()));
                });

        cart.getDeliveryOptions().stream()
                .filter(opt -> opt.getType() != PICKUP)
                .forEach(opt -> {
                    log.info("checking {}", opt);
                    assertTrue(CollectionUtils.isEmpty(opt.getPromos()));
                });

    }


    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем что будет если лоялти посчитает скидку больше чем цена доставки")
    @Test
    public void testInvalidDeliveryDiscountCalculation() throws Exception {
        BigDecimal deliveryDiscount = new BigDecimal(1000);
        LoyaltyDiscount pickupCoin = createCoin(deliveryDiscount, "pickupPromoKey", 1111L);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.PICKUP, pickupCoin);
        parameters.getLoyaltyParameters().setDeliveryDiscountsMode(FORCE);
        parameters.setDeliveryType(PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);


        parameters.setCheckCartErrors(false);
        addCartErrorsChecks(parameters.cartResultActions(), "basic", "wrong_loyalty_discount", "ERROR", null);

        orderCreateHelper.cart(parameters);
    }


    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем передачу применения монеток на доставку к опциям доставки")
    @Test
    public void testCoinsOnDeliveryOptions() {
        BigDecimal deliveryDiscount = new BigDecimal(1);
        LoyaltyDiscount pickupCoin = createCoin(deliveryDiscount, "pickupPromoKey", 1111L);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.PICKUP, pickupCoin);
        parameters.setDeliveryType(PICKUP);
        parameters.getOrder().getDelivery().setType(null);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);

        MultiCart multicartResponse = orderCreateHelper.cart(parameters);
        validateDeliveryOptions(multicartResponse, pickupCoin);

        //второй вызов для проверки как посчитается, если указать опцию доставки
        parameters.configureMultiCart(multiCart -> orderCreateHelper.mapDeliveryOptions(multicartResponse,
                parameters, multiCart));
        MultiCart multicartResponseWithSelectedDeliveryOption = orderCreateHelper.cart(parameters);
        validateDeliveryOptions(multicartResponseWithSelectedDeliveryOption, pickupCoin);
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем передачу применения монеток на доставку к опциям доставки, если были изменения в опциях " +
            "доставки")
    @Issue("https://st.yandex-team.ru/MARKETCHECKOUT-8516")
    @Test
    public void testCoinsOnDeliveryOptionsWithChangesInDelivery() {
        BigDecimal deliveryDiscount = new BigDecimal(100);
        LoyaltyDiscount deliveryCoin = createCoin(deliveryDiscount, "promoKey", 1111L);

        parameters = createParameters();
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().addDeliveryDiscount(DeliveryType.PICKUP, deliveryCoin);
        parameters.getLoyaltyParameters().addDeliveryDiscount(DeliveryType.COURIER,
                new LoyaltyDiscount(deliveryDiscount, PromoType.YANDEX_EMPLOYEE));
        parameters.getOrder().setCoinIdsToUse(Lists.newArrayList(deliveryCoin.getCoinId()));

        //настраиваем бесплатную доставку в офис
        parameters.getOrder().getDelivery().setBuyerAddress(getAnotherAddress());
        parameters.getOrder().getDelivery().setShopAddress(getAnotherAddress());
        parameters.getOrder().getDelivery().setType(null); //опция не выбрана
        patchAddressForFreeDelivery(parameters);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3)
                        .addPickup(100501L, 2, Collections.singletonList(12312303L))
                        .build()
        );

        MultiCart multicartResponse = orderCreateHelper.cart(parameters);
        //смотрим что для курьерки бесплатная доставка на адрес, а для остального по монете
        assertThat(multicartResponse.getCarts(), hasSize(1));
        Order cart = multicartResponse.getCarts().get(0);
        cart.getDeliveryOptions()
                .forEach(opt -> {
                    log.info("checking {}", opt);
                    List<ItemPromo> coinPromos = opt.getPromos().stream()
                            .filter(promo -> promo.getType() == PromoType.MARKET_COIN).collect(Collectors.toList());
                    List<ItemPromo> freeDeliveryAddressPromos = opt.getPromos().stream()
                            .filter(promo -> promo.getType() == PromoType.YANDEX_EMPLOYEE).collect(Collectors.toList());

                    ItemPromo promo;
                    if (opt.getType() == DELIVERY) {
                        assertThat(freeDeliveryAddressPromos, hasSize(1));
                        assertThat(coinPromos, empty());
                        promo = freeDeliveryAddressPromos.get(0);
                    } else {
                        assertThat(freeDeliveryAddressPromos, empty());
                        assertThat(coinPromos, hasSize(1));
                        promo = coinPromos.get(0);

                        assertThat(promo.getPromoDefinition().getMarketPromoId(), is(deliveryCoin.getPromoKey()));
                        assertThat(promo.getPromoDefinition().getCoinId(), is(deliveryCoin.getCoinId()));
                    }

                    assertThat(promo.getBuyerDiscount(), is(opt.getPrices().getBuyerPriceBeforeDiscount()));
                    assertThat(opt.getBuyerPrice(), is(BigDecimal.ZERO));
                    assertThat(opt.getPrices().getBuyerDiscount(), is(promo.getBuyerDiscount()));
                });

        //находим айдишку курьерки
        Optional<? extends Delivery> delivery = cart.getDeliveryOptions().stream()
                .filter(opt -> opt.getType() == DELIVERY).findFirst();
        assertTrue(delivery.isPresent());
        String optionId = delivery.get().getHash();
        //меняем адрес, но отправляем эту опцию как выбранную
        parameters.getOrder().getDelivery().setBuyerAddress(getAddress());
        parameters.getOrder().getDelivery().setShopAddress(getAddress());
        parameters.getGeocoderParameters().setDefault();
        parameters.getOrder().getDelivery().setHash(optionId);

        parameters.getLoyaltyParameters().clearDiscounts();
        parameters.getLoyaltyParameters().addDeliveryDiscount(null, deliveryCoin);

        // отправляем /cart с выбранной опцией доставки
        parameters.setCheckCartErrors(false); // ожидаем что будут изменения в доставке
        multicartResponse = orderCreateHelper.cart(parameters);

        //проверяем что монета применилась ко всем опциям доставки
        cart = multicartResponse.getCarts().get(0);
        cart.getDeliveryOptions()
                .forEach(opt -> {
                    log.info("checking {}", opt);
                    List<ItemPromo> coinPromos = opt.getPromos().stream()
                            .filter(promo -> promo.getType() == PromoType.MARKET_COIN).collect(Collectors.toList());

                    assertThat(coinPromos, hasSize(1));
                    ItemPromo promo = coinPromos.get(0);

                    assertThat(promo.getPromoDefinition().getMarketPromoId(), is(deliveryCoin.getPromoKey()));
                    assertThat(promo.getPromoDefinition().getCoinId(), is(deliveryCoin.getCoinId()));


                    assertThat(promo.getBuyerDiscount(), is(opt.getPrices().getBuyerPriceBeforeDiscount()));
                    assertThat(opt.getBuyerPrice(), is(BigDecimal.ZERO));
                    assertThat(opt.getPrices().getBuyerDiscount(), is(promo.getBuyerDiscount()));
                });
        assertThat(cart.getCoinInfo().getUnusedCoinIds(), anyOf(empty(), nullValue()));
    }


    private void validateDeliveryOptions(MultiCart multicart, LoyaltyDiscount pickupCoin) {
        assertThat(multicart.getCarts(), hasSize(1));
        Order cart = multicart.getCarts().get(0);

        cart.getDeliveryOptions().stream()
                .filter(opt -> opt.getType() == PICKUP)
                .forEach(opt -> {
                    log.info("checking {}", opt);
                    List<ItemPromo> coinPromos = opt.getPromos().stream()
                            .filter(promo -> promo.getType() == PromoType.MARKET_COIN).collect(Collectors.toList());
                    assertThat(coinPromos, hasSize(1));
                    ItemPromo promo = coinPromos.get(0);
                    assertThat(promo.getPromoDefinition().getMarketPromoId(), is(pickupCoin.getPromoKey()));
                    assertThat(promo.getPromoDefinition().getCoinId(), is(pickupCoin.getCoinId()));
                    assertThat(promo.getBuyerDiscount(), is(pickupCoin.getDiscount()));
                    assertThat(opt.getPrices().getBuyerDiscount(), is(pickupCoin.getDiscount()));
                    assertThat(opt.getPrices().getBuyerPriceBeforeDiscount().subtract(opt.getPrices()
                            .getBuyerDiscount()), is(opt.getPrice()));
                });

        cart.getDeliveryOptions().stream()
                .filter(opt -> opt.getType() != PICKUP)
                .forEach(opt -> {
                    log.info("checking {}", opt);
                    assertTrue(CollectionUtils.isEmpty(opt.getPromos()));
                });
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем что при опциях доставки, отличающихся только методами оплаты, в лоялти передаются опции с" +
            " разными айдишками")
    @Test
    public void testDuplicateDeliveryRequestIds() {
        BigDecimal deliveryDiscount = new BigDecimal(100);
        LoyaltyDiscount pickupCoin = createCoin(deliveryDiscount, "pickupPromoKey", 1111L);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.PICKUP, pickupCoin);
        parameters.setDeliveryType(PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);


        //добавляем опции доставки, отличающиеся только методами оплаты. все должно работать
        //один такой уже добавляется при создании параметров
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID, 2, Collections.singletonList(12312303L))
                        .addPickup(MOCK_DELIVERY_SERVICE_ID, 2, Collections.singletonList(12312303L))
                        .withPaymentMethods(Set.of(PaymentMethod.YANDEX))
                        .build()
        );

        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertOrderHasCoinPromoForFreeDelivery(createdOrder, pickupCoin);
    }


    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем передачу UserCoinResponse от лоялти до клиента")
    @Test
    public void testAllCoins() {
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(1, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(2, "key 2")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(3, "key 3")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));

        var coins = parameters.getLoyaltyParameters().getCoinDiscountEntries().stream()
                .map(CoinDiscountEntry::toCoinResponse)
                .collect(Collectors.toUnmodifiableList());

        parameters.setMultiCartAction(
                mc -> Utils.checkCoins(
                        mc.getCarts().iterator().next().getCoinInfo().getAllCoins(),
                        coins
                )
        );

        var createdOrder = orderCreateHelper.createOrder(parameters);
        Utils.checkCoins(createdOrder.getCoinInfo().getAllCoins(), coins);
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, что неиспользованные монетки вернулись в предупреждениях валидации")
    @Test
    public void shouldReturnWarningsCausedByUnusedCoinsOnCart() {
        long unusedCoinId = 99L;
        parameters.getBuiltMultiCart().setCoinIdsToUse(Arrays.asList(COIN1_ID, COIN2_ID, COIN3_ID, unusedCoinId));
        parameters.cartResultActions()
                .andExpect(jsonPath("$.validationWarnings.length()").value(1))
                .andExpect(jsonPath("$.validationWarnings[0].code").value("UNUSED_COIN"))
                .andExpect(jsonPath("$.validationWarnings[0].type").value("MARKET_COIN_ERROR"))
                .andExpect(jsonPath("$.validationWarnings[0].coinId").value(unusedCoinId))
                .andExpect(jsonPath("$.validationWarnings[0].userMessage").doesNotExist()
                );

        orderCreateHelper.cart(parameters);
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, что происходит чекаут, если есть неиспользованные монетки")
    @Test
    public void shouldCheckoutWithUnusedCoins() {
        long unusedCoinId = 99L;
        parameters.getBuiltMultiCart().setCoinIdsToUse(Arrays.asList(COIN1_ID, COIN2_ID, COIN3_ID, unusedCoinId));
        orderCreateHelper.createOrder(parameters);
    }


    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, что coinInfo передается в случае ошибки, произошедшей на этапе spend'а")
    @Test
    public void testCoinInfoOnSpendDiscountError() {
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(1, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(2, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(3, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));

        var coins = parameters.getLoyaltyParameters().getCoinDiscountEntries().stream()
                .map(CoinDiscountEntry::toCoinResponse)
                .collect(Collectors.toUnmodifiableList());

        parameters.setMultiCartAction(
                mc -> {
                    Utils.checkCoins(
                            mc.getCarts().iterator().next().getCoinInfo().getAllCoins(),
                            coins
                    );
                    loyaltyConfigurer.mockSpendError(
                            new MarketLoyaltyError(MarketLoyaltyErrorCode.OTHER_ERROR),
                            HttpStatus.UNPROCESSABLE_ENTITY.value());
                });

        parameters.setCheckOrderCreateErrors(false);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(multiOrder.getCartFailures(), hasSize(1));
        OrderFailure failure = multiOrder.getCartFailures().get(0);
        Utils.checkCoins(failure.getOrder().getCoinInfo().getAllCoins(), coins);
    }


    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, передачу ошибок монеток")
    @Test
    public void testCoinErrors() {
        long unusedCoinId = 99L;

        MarketLoyaltyError marketLoyaltyError = EnhancedRandom.random(MarketLoyaltyError.class);
        marketLoyaltyError.setCode(MarketLoyaltyErrorCode.COUPON_NOT_EXISTS.name());

        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(1, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE)
                .coinError(marketLoyaltyError));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(2, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(3, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));

        parameters.getOrder().setCoinIdsToUse(Arrays.asList(unusedCoinId, 1L, 2L, 3L));
        parameters.setCheckCartErrors(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        Order cart = multiCart.getCarts().iterator().next();
        assertThat(cart.getCoinInfo(), notNullValue());
        List<ru.yandex.market.checkout.checkouter.order.CoinError> coinErrors = cart.getCoinInfo().getCoinErrors();
        assertThat(coinErrors, hasSize(1));

        ru.yandex.market.checkout.checkouter.order.CoinError returnedCoinError = coinErrors.iterator().next();
        assertThat(returnedCoinError.getCoinId(), equalTo(1L));
        assertThat(returnedCoinError.getMessage(), equalTo(marketLoyaltyError.getUserMessage()));
        assertThat(returnedCoinError.getCode(),
                equalTo(
                        MarketLoyaltyErrorCode.findErrorCodeByName(marketLoyaltyError.getCode()).getCodeForFront()
                                .name()
                )
        );
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем обратную совместимость")
    @Test
    public void testCoinForwardCompatibility() {
        long unusedCoinId = 99L;
        parameters.getOrder().setCoinIdsToUse(null);
        parameters.configureMultiCart(mc -> mc.setPromoCode(null));

        MarketLoyaltyError marketLoyaltyError = EnhancedRandom.random(MarketLoyaltyError.class);
        marketLoyaltyError.setCode(MarketLoyaltyErrorCode.COUPON_NOT_EXISTS.name());

        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(1, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE)
                .coinError(marketLoyaltyError));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(2, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(3, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));

        parameters.getBuiltMultiCart().setCoinIdsToUse(Arrays.asList(unusedCoinId, 1L, 2L, 3L));
        parameters.setCheckCartErrors(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getCoinInfo(), notNullValue());
        List<ru.yandex.market.checkout.checkouter.order.CoinError> coinErrors = multiCart.getCoinInfo().getCoinErrors();
        assertThat(coinErrors, hasSize(1));

        ru.yandex.market.checkout.checkouter.order.CoinError returnedCoinError = coinErrors.iterator().next();
        assertThat(returnedCoinError.getCoinId(), equalTo(1L));
        assertThat(returnedCoinError.getMessage(), equalTo(marketLoyaltyError.getUserMessage()));
        assertThat(returnedCoinError.getCode(),
                equalTo(
                        MarketLoyaltyErrorCode.findErrorCodeByName(marketLoyaltyError.getCode()).getCodeForFront()
                                .name()
                )
        );
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, передачу ошибок монеток, если нет userMessage")
    @Test
    public void testCoinErrorsWithEmptyUserMessage() throws Exception {
        MarketLoyaltyError marketLoyaltyError = new MarketLoyaltyError(COIN_ALREADY_USED);

        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(1, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE)
                .coinError(marketLoyaltyError));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(2, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));
        parameters.getLoyaltyParameters().expectCoin(CoinDiscountEntry.coin(3, "key 1")
                .discount(orderItem1p.getOfferItemKey(), BigDecimal.ONE));

        addCartErrorsChecks(parameters.cartResultActions(), "MARKET_COIN_ERROR",
                //берется codeForFront
                COIN_ALREADY_USED.getCodeForFront().name(), "ERROR",
                null);
        parameters.setCheckCartErrors(false);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        Order cart = multiCart.getCarts().iterator().next();
        assertThat(cart.getCoinInfo(), notNullValue());
        List<ru.yandex.market.checkout.checkouter.order.CoinError> coinErrors = cart.getCoinInfo().getCoinErrors();
        assertThat(coinErrors, hasSize(1));

        ru.yandex.market.checkout.checkouter.order.CoinError returnedCoinError = coinErrors.iterator().next();
        assertThat(returnedCoinError.getCoinId(), equalTo(1L));
        assertThat(returnedCoinError.getMessage(), equalTo(marketLoyaltyError.getUserMessage()));
        assertThat(returnedCoinError.getCode(),
                equalTo(
                        MarketLoyaltyErrorCode.findErrorCodeByName(marketLoyaltyError.getCode()).getCodeForFront()
                                .name()
                )
        );
    }

    private <T> void assertCollectionEqual(Collection<T> actualCol,
                                           Collection<T> expectedCol,
                                           Comparator<T> comparator,
                                           LoggingComparatorHelper helper) {
        assertThat(actualCol, hasSize(expectedCol.size()));
        Iterator<T> actualIt = actualCol.stream()
                .sorted(comparator)
                .iterator();
        Iterator<T> expectedIt = expectedCol.stream()
                .sorted(comparator)
                .iterator();

        while (actualIt.hasNext()) {
            T actual = actualIt.next();
            T expected = expectedIt.next();
            boolean oldLoggingState = false;

            if (helper != null) {
                oldLoggingState = helper.enableLogging();
            }
            boolean result = comparator.compare(actual, expected) == 0;
            String message = "Expected: " + expected + "\nActual: " + actual;
            if (!result && helper != null) {
                message += "\nFailures: \n\t" + helper.failures.stream()
                        .collect(Collectors.joining("\n\t"));
            }
            assertThat(message, result);
            if (helper != null) {
                helper.setLoggingEnabled(oldLoggingState);
            }
        }
    }

    /**
     * Обёрточка над компаратором, позволяющая фиксировать, где свалились.
     */
    private static class LoggingComparatorHelper {

        private boolean loggingEnabled;
        private List<String> failures;

        private <T> Comparator<T> wrap(String fieldName, Comparator<T> downstream) {
            return (v1, v2) -> {
                int result = downstream.compare(v1, v2);
                if (loggingEnabled && result != 0) {
                    failures.add("Field '" + fieldName + "' mismatched: " + v1 + " not equal to " + v2);
                }
                return result;
            };
        }

        private <T> Comparator<T> natural(String fieldName) {
            return wrap(fieldName, (Comparator<T>) nullsFirst(naturalOrder()));
        }

        private boolean enableLogging() {
            boolean oldState = loggingEnabled;
            loggingEnabled = true;
            failures = new ArrayList<>();
            return oldState;
        }

        private void setLoggingEnabled(boolean enabled) {
            loggingEnabled = enabled;
        }
    }

    private static OrderPromo marketCoin(String promoId, Long coinId, BigDecimal buyerItemsDiscount,
                                         BigDecimal buyerSubsidy, BigDecimal subsidy) {
        OrderPromo orderPromo = new OrderPromo(marketCoinPromo(promoId, null, null, coinId, null, null));
        orderPromo.setBuyerItemsDiscount(buyerItemsDiscount);
        orderPromo.setBuyerSubsidy(buyerSubsidy);
        orderPromo.setSubsidy(subsidy);
        orderPromo.setDeliveryDiscount(BigDecimal.ZERO);
        return orderPromo;
    }

    private static OrderPromo coupon(String promoCode, BigDecimal buyerItemsDiscount,
                                     BigDecimal buyerSubsidy, BigDecimal subsidy) {
        OrderPromo orderPromo = new OrderPromo(
                marketCouponPromo(LoyaltyDiscount.PROMOCODE_PROMO_KEY, null, null, promoCode, "LOYALTY")
        );
        orderPromo.setBuyerItemsDiscount(buyerItemsDiscount);
        orderPromo.setBuyerSubsidy(buyerSubsidy);
        orderPromo.setSubsidy(subsidy);
        orderPromo.setDeliveryDiscount(BigDecimal.ZERO);
        return orderPromo;
    }

    private void assertOrderHasCoinPromoForFreeDelivery(Order order, LoyaltyDiscount coin) {

        assertEquals(BigDecimal.ZERO, order.getDelivery().getPrice());
        assertTrue(order.getDelivery().isFree());
        assertThat(order.getDelivery().getPromos(), hasSize(1));

        ItemPromo actualDeliveryItemPromo = order.getDelivery().getPromos().iterator().next();
        assertThat(actualDeliveryItemPromo.getType(), is(PromoType.MARKET_COIN));
        assertThat(actualDeliveryItemPromo.getPromoDefinition().getMarketPromoId(), is(coin.getPromoKey()));
        assertThat(actualDeliveryItemPromo.getPromoDefinition().getCoinId(), is(coin.getCoinId()));
        assertThat(actualDeliveryItemPromo.getBuyerDiscount(), is(coin.getDiscount()));

        Optional<OrderPromo> orderPromo = order.getOrderPromo(
                PromoDefinition.marketCoinPromo(coin.getPromoKey(), null, null, coin.getCoinId(), null, null));
        assertTrue(orderPromo.isPresent());
        assertThat(orderPromo.get().getDeliveryDiscount(), is(coin.getDiscount()));
    }

    private void runInspectorTask() {
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);

        tmsTaskHelper.runInspectExpiredPaymentTaskV2();
    }
}
