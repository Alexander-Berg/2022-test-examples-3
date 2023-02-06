package ru.yandex.market.loyalty.back.controller.discount;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.PaymentType.GOOGLE_PAY;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * @author maratik
 */
@TestFor(DiscountController.class)
public class DiscountControllerGooglePayTest extends DiscountControllerGenerationSupportedTest {

    private static final BigDecimal INITIAL_CURRENT_BUDGET = BigDecimal.valueOf(700);
    private static final String COUPON_FOR_PROMO = "coupon_promo_2";

    @Autowired
    private PromoManager promoManager;
    @Value("${market.loyalty.google.pay.promo.code}")
    private String googlePayPromoCode;
    @Autowired
    private CoinService coinService;

    @Test
    public void shouldApplyGooglePayDiscount() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(googlePayPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1000))
                )
                .withPaymentType(GOOGLE_PAY)
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order).build()
        );

        assertThat(discountResponse, hasProperty(
                "orders",
                contains(
                        hasProperty(
                                "items",
                                contains(
                                        hasProperty(
                                                "promos",
                                                contains(
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(50L))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldNotApplyGooglePayDiscountIfUsedAsRegularCouponCode() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(googlePayPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1000))
                )
                .withPaymentType(null)
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order)
                        .withCoupon(googlePayPromoCode)
                        .build()
        );

        assertThat(discountResponse, hasProperty(
                "couponError",
                hasProperty(
                        "error",
                        hasProperty(
                                "code",
                                equalTo("COUPON_NOT_EXISTS")
                        )
                )
        ));
    }


    @Test
    public void shouldApplyGooglePayAndCoupon() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(googlePayPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );

        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(COUPON_FOR_PROMO)
                        .setCouponValue(BigDecimal.valueOf(100), CoreCouponValueType.FIXED)
                        .setBudget(INITIAL_CURRENT_BUDGET)
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1000))
                )
                .withPaymentType(GOOGLE_PAY)
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order)
                        .withCoupon(COUPON_FOR_PROMO)
                        .build()
        );

        assertThat(discountResponse, hasProperty(
                "orders",
                contains(
                        hasProperty(
                                "items",
                                contains(
                                        hasProperty(
                                                "promos",
                                                containsInAnyOrder(
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(50L))
                                                        ),
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(100L))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldApplyGooglePayAndCouponAndCoin() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(googlePayPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );

        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(COUPON_FOR_PROMO)
                        .setCouponValue(BigDecimal.valueOf(100), CoreCouponValueType.FIXED)
                        .setBudget(INITIAL_CURRENT_BUDGET)
        );

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100))
        );

        final CoinKey coin = coinService.create.createCoin(
                smartShoppingPromo,
                CoinInsertRequest.authMarketBonus(DEFAULT_UID)
                        .setSourceKey("test")
                        .setStatus(ACTIVE)
                        .setReason(CoreCoinCreationReason.OTHER)
                        .build()
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1000))
                )
                .withPaymentType(GOOGLE_PAY)
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order)
                        .withCoupon(COUPON_FOR_PROMO)
                        .withCoins(coin)
                        .build()
        );

        assertThat(discountResponse, hasProperty(
                "orders",
                contains(
                        hasProperty(
                                "items",
                                contains(
                                        hasProperty(
                                                "promos",
                                                containsInAnyOrder(
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(50L))
                                                        ),
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(100L))
                                                        ),
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(100L))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldIgnoreGooglePayPromoNotActive() {
        final Date yesterday = new Date(clock.millis() - TimeUnit.DAYS.toMillis(1));
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(googlePayPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setEndDate(yesterday)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1000))
                )
                .withPaymentType(GOOGLE_PAY)
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order).build()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                contains(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                empty()
                                                        )
                                                )
                                        )
                                )
                        ),
                        hasProperty(
                                "couponError",
                                nullValue()
                        )
                )
        );
    }

    @Test
    public void shouldIgnoreGooglePayPromoNotExists() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1000))
                )
                .withPaymentType(GOOGLE_PAY)
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order).build()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                contains(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                empty()
                                                        )
                                                )
                                        )
                                )
                        ),
                        hasProperty(
                                "couponError",
                                nullValue()
                        )
                )
        );
    }

    @Test
    public void shouldIgnoreGooglePayPromoPhoneAlreadyUsed() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(googlePayPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .addPromoRule(
                                RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE,
                                RuleParameterName.MAX_ORDER_TOTAL,
                                Collections.singleton(BigDecimal.valueOf(500))
                        )
                        .addPromoRule(RuleType.ONCE_PER_PHONE_NUMBER_FILTER_RULE)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );

        final String phone = "87773334455";
        marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder
                        .builder(
                                orderRequestBuilder()
                                        .withOrderId("1")
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withPaymentType(GOOGLE_PAY)
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory
                                .withUidBuilder(DEFAULT_UID)
                                .withPhone(phone)
                                .buildOperationContext()
                        )
                        .build()
        );

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder
                        .builder(
                                orderRequestBuilder()
                                        .withOrderId("2")
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withPaymentType(GOOGLE_PAY)
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory
                                .withUidBuilder(DEFAULT_UID)
                                .withPhone(phone)
                                .buildOperationContext()
                        )
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                contains(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                empty()
                                                        )
                                                )
                                        )
                                )
                        ),
                        hasProperty(
                                "couponError",
                                nullValue()
                        )
                )
        );
    }
}
