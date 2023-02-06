package ru.yandex.market.checkout.checkouter.promo.promocode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.Epic;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.OrderUtils;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.loyalty.model.PromocodeDiscountEntry;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.loyalty.api.model.CouponError;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.promocode.MarketLoyaltyPromocodeWarningCode;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;

public class PromocodeCheckoutControllerTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";
    private static final String PROMO_KEY = "some promo key";
    private static final String ANAPLAN_ID = "some anaplan id";

    private static final String ANOTHER_PROMO_CODE = "ANOTHER_PROMO_CODE";
    private static final String ANOTHER_PROMO_KEY = "ANOTHER_PROMO_KEY";

    private Parameters parameters;
    private OrderItem orderItem;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private LoyaltyConfigurer loyaltyConfigurer;

    @BeforeEach
    void configure() {
        parameters = defaultBlueOrderParameters();
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(PROMO_CODE));
        parameters.configuration().cart().multiCartMocks().setMockLoyalty(false);
        orderItem = parameters.getItems().iterator().next();
        orderItem.setPrice(BigDecimal.valueOf(1000));
        orderItem.setBuyerPrice(BigDecimal.valueOf(1000));
        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(orderItem).build()));
        parameters.getLoyaltyParameters().setExpectedPromoCode(PROMO_CODE);
    }

    @Test
    void shouldApplyPromocodeDiscount() {
        parameters.getLoyaltyParameters().expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                .discount(Map.of(orderItem.getOfferItemKey(), BigDecimal.valueOf(100))));

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getItems(), hasItem(allOf(
                hasProperty("offerId", is(orderItem.getOfferId())),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(900))),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoDefinition", allOf(
                                hasProperty("type", is(PromoType.MARKET_PROMOCODE)),
                                hasProperty("marketPromoId", is(PROMO_KEY))
                        )),
                        hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(100)))
                )))
        )));
    }

    @Test
    void shouldReturnOrderWithPromoClientId() {
        parameters.getLoyaltyParameters().expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                .discount(Map.of(orderItem.getOfferItemKey(), BigDecimal.valueOf(100)))
                .clientId(CLIENT_ID)
        );

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getItems(), hasItem(allOf(
                hasProperty("offerId", is(orderItem.getOfferId())),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(900))),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoDefinition", allOf(
                                hasProperty("type", is(PromoType.MARKET_PROMOCODE)),
                                hasProperty("clientId", is(CLIENT_ID)),
                                hasProperty("marketPromoId", is(PROMO_KEY))
                        ))
                )))
        )));
    }

    @Test
    void shouldReturnOrderWithAnaplanId() {
        parameters.getLoyaltyParameters().expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                .discount(Map.of(orderItem.getOfferItemKey(), BigDecimal.valueOf(100)))
                .anaplanId(ANAPLAN_ID)
                .clientId(CLIENT_ID)
        );

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getItems(), hasItem(allOf(
                hasProperty("offerId", is(orderItem.getOfferId())),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(900))),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoDefinition", allOf(
                                hasProperty("type", is(PromoType.MARKET_PROMOCODE)),
                                hasProperty("clientId", is(CLIENT_ID)),
                                hasProperty("marketPromoId", is(PROMO_KEY)),
                                hasProperty("anaplanId", is(ANAPLAN_ID))
                        ))
                )))
        )));
    }

    @Test
    void shouldReturnUnusedPromocode() {
        parameters.getLoyaltyParameters().expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                .unused(true));

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getPromocodeInfo(), notNullValue());
        assertThat(multiOrder.getPromocodeInfo().getUnusedPromocodes(), notNullValue());
        assertThat(multiOrder.getPromocodeInfo().getUnusedPromocodes(), contains(PROMO_CODE));

        Order order = OrderUtils.firstOrder(multiOrder);

        assertThat(order.getItems(), hasItem(allOf(
                hasProperty("offerId", is(orderItem.getOfferId())),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(1000))),
                hasProperty("promos", empty())
        )));
    }

    @Test
    void shouldReturnPromocodeErrors() {
        parameters.getLoyaltyParameters().expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                .promocodeError(new CouponError(new MarketLoyaltyError(MarketLoyaltyErrorCode.INVALID_COUPON_CODE))));

        parameters.configuration().cart().response().setCheckCartErrors(false);

        MultiCart multiOrder = orderCreateHelper.cart(parameters);

        assertThat(multiOrder.getPromocodeInfo(), notNullValue());
        assertThat(multiOrder.getPromocodeInfo().getPromocodeErrors(), notNullValue());
        assertThat(multiOrder.getPromocodeInfo().getPromocodeErrors(), hasItem(allOf(
                hasProperty(Names.PromocodeError.PROMOCODE, is(PROMO_CODE)),
                hasProperty(Names.PromocodeError.CODE, is(MarketLoyaltyErrorCode.INVALID_COUPON_CODE.name()))
        )));

        assertThat(multiOrder.getValidationErrors(), not(empty()));
        assertThat(multiOrder.getValidationErrors(), hasItems(allOf(
                hasProperty(Names.ValidationResult.SEVERITY, is(ValidationResult.Severity.ERROR)),
                hasProperty(Names.ValidationResult.PROMOCODE, is(PROMO_CODE)),
                hasProperty(Names.ValidationResult.CODE, is(MarketLoyaltyErrorCode.INVALID_COUPON_CODE.name()))
        )));

        Order order = OrderUtils.firstOrder(multiOrder);

        assertThat(order.getItems(), hasItem(allOf(
                hasProperty("offerId", is(orderItem.getOfferId())),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(1000))),
                hasProperty("promos", empty())
        )));
    }

    @Test
    void shouldReturnPromocodeWarnings() {
        parameters.getLoyaltyParameters().expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                .promocodeWarning(MarketLoyaltyPromocodeWarningCode.MAX_DISCOUNT_REACHED, "test"));

        MultiCart multiOrder = orderCreateHelper.cart(parameters);

        assertThat(multiOrder.getPromocodeInfo(), notNullValue());
        assertThat(multiOrder.getPromocodeInfo().getPromocodeWarnings(), notNullValue());
        assertThat(multiOrder.getPromocodeInfo().getPromocodeWarnings(), hasItem(allOf(
                hasProperty(Names.PromocodeWarning.PROMOCODE, is(PROMO_CODE)),
                hasProperty(Names.PromocodeWarning.CODE, is("MAX_DISCOUNT_REACHED")),
                hasProperty(Names.PromocodeWarning.MESSAGE, is("test"))
        )));

        assertThat(multiOrder.getValidationErrors(), nullValue());
    }


    @Test
    void shouldFailOrderCreationOnPromocodeErrors() {
        parameters.getLoyaltyParameters().expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                .promocodeError(new CouponError(new MarketLoyaltyError(MarketLoyaltyErrorCode.INVALID_COUPON_CODE))));

        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertThat(multiOrder.getValidationErrors(), not(empty()));
        assertThat(multiOrder.getValidationErrors(), hasItems(allOf(
                hasProperty(Names.ValidationResult.SEVERITY, is(ValidationResult.Severity.ERROR)),
                hasProperty(Names.ValidationResult.PROMOCODE, is(PROMO_CODE)),
                hasProperty(Names.ValidationResult.CODE, is(MarketLoyaltyErrorCode.INVALID_COUPON_CODE.name()))
        )));
    }

    @Test
    void shouldApplyMultiplePromocodes() {
        parameters.configureMultiCart(multiCart -> multiCart.useInternalPromoCode(true));
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(null));

        parameters.getLoyaltyParameters()
                .expectPromocodes(
                        PromocodeDiscountEntry
                                .promocode(PROMO_CODE, PROMO_KEY)
                                .discount(Map.of(orderItem.getOfferItemKey(), BigDecimal.valueOf(101))),
                        PromocodeDiscountEntry
                                .promocode(ANOTHER_PROMO_CODE, ANOTHER_PROMO_KEY)
                                .discount(Map.of(orderItem.getOfferItemKey(), BigDecimal.valueOf(102))));

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getPromos(), hasSize(2));
        assertThat(order.getItems(), hasItem(allOf(
                hasProperty("offerId", is(orderItem.getOfferId())),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(797))),
                hasProperty("promos", containsInAnyOrder(
                        allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(PromoType.MARKET_PROMOCODE)),
                                        hasProperty("marketPromoId", is(PROMO_KEY)),
                                        hasProperty("promoCode", is(PROMO_CODE))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(101)))

                        ),
                        allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(PromoType.MARKET_PROMOCODE)),
                                        hasProperty("marketPromoId", is(ANOTHER_PROMO_KEY)),
                                        hasProperty("promoCode", is(ANOTHER_PROMO_CODE))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(102)))
                        )
                        )
                ))));

        //Проверяем что было 2 запроса на активацию
        assertThat(loyaltyConfigurer.findAll(
                WireMock.postRequestedFor(urlPathEqualTo(LoyaltyConfigurer.URI_PROMOCODE_ACTIVATE_V1))
        ), hasSize(2));
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, что для 3p промокодов, созданных партнером, не отправляем субсидию")
    @Test
    public void test3pPartnerPromoSubsidies() {
        ReportGeneratorParameters reportParameters = parameters.getReportParameters();
        reportParameters.overrideItemInfo(orderItem.getFeedOfferId()).setSupplierType(SupplierType.THIRD_PARTY);
        parameters.getLoyaltyParameters().clearDiscounts();
        final BigDecimal discount = BigDecimal.valueOf(100);
        parameters.getLoyaltyParameters()
                .expectPromocode(PromocodeDiscountEntry.promocode(PROMO_CODE, PROMO_KEY)
                        .discount(Map.of(orderItem.getOfferItemKey(), discount)))
                .expectResponseItems(
                        itemResponseFor(orderItem)
                                .quantity(2)
                                .promo(new ItemPromoResponse(
                                        discount,
                                        ru.yandex.market.loyalty.api.model.PromoType.MARKET_PROMOCODE,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        null,
                                        null,
                                        true,
                                        PROMO_CODE,
                                        null,
                                        null
                                ))
                );

        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(PROMO_CODE));

        Order order = orderCreateHelper.createOrder(parameters);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        OrderItem item = orderService.getOrder(order.getId()).getItem(orderItem.getFeedOfferId());

        assertThat(item.getSupplierType(), Is.is(SupplierType.THIRD_PARTY));
        final BigDecimal error = new BigDecimal("0.01");
        assertThat(item.getPrices().getSubsidy(), closeTo(BigDecimal.ZERO, error));
        assertThat(item.getPrices().getBuyerSubsidy(), closeTo(BigDecimal.ZERO, error));

        trustMockConfigurer.trustMock().verify(
                anyRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL))
        );

        payHelper.refundAllOrderItems(order);
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);
        trustMockConfigurer.trustMock().verify(
                anyRequestedFor(urlEqualTo("/trust-payments/v2/refunds"))
                        .withRequestBody(notMatching(".*" +
                                discount.setScale(2, RoundingMode.HALF_UP).toString() + "" + ".*"))
        );
    }

    @Test
    void shouldApplyDeliveryExtraCharge() {
        parameters.configureMultiCart(multiCart -> multiCart.useInternalPromoCode(true));
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode(null));

        var extraChargeValueExpected = BigDecimal.valueOf(21);
        var loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters.setPromoOnlySelectedOption(true);
        loyaltyParameters.addDeliveryDiscount(LoyaltyDiscount.builder()
                .promoType(PromoType.MULTICART_DISCOUNT)
                .discount(BigDecimal.valueOf(40))
                .extraCharge(extraChargeValueExpected).build());

        Order order = orderCreateHelper.createOrder(parameters);

        var orderPromos = order.getPromos();
        assertThat(orderPromos, hasSize(1));
        assertThat(orderPromos.get(0).getExtraCharge(), equalTo(extraChargeValueExpected));
    }
}
