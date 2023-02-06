package ru.yandex.market.loyalty.back.controller.discount;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
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
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
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
import static ru.yandex.market.loyalty.api.model.PaymentType.APPLE_PAY;
import static ru.yandex.market.loyalty.api.model.PaymentType.YANDEX;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.allowedPaymentTypes;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class DiscountControllerApplePayTest extends DiscountControllerGenerationSupportedTest {

    private static final BigDecimal INITIAL_CURRENT_BUDGET = BigDecimal.valueOf(1000000);
    private static final String COUPON_FOR_PROMO = "coupon_promo_2";
    private static final OrderWithBundlesRequest ALLOWED_APPLE_PAY_REQUEST = orderRequestWithBundlesBuilder()
            .withOrderItem(itemKey(ANOTHER_ITEM_KEY),
                    price(BigDecimal.valueOf(1000)),
                    allowedPaymentTypes(Set.of(APPLE_PAY, YANDEX))
            )
            .withPaymentType(YANDEX)
            .build();

    @Autowired
    private PromoManager promoManager;
    @Value("${market.loyalty.apple.pay.discount.promo.code}")
    private String applePayDiscountPromoCode;
    @Autowired
    private CoinService coinService;

    @Test
    public void shouldApplyApplePayDiscount() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY),
                                        price(BigDecimal.valueOf(1000)))
                                .withPaymentType(APPLE_PAY)
                                .build(),
                        ALLOWED_APPLE_PAY_REQUEST
                )
                        .build(),
                enableApplePayDiscountHeader()
        );

        assertThat(discountResponse, hasProperty(
                "orders",
                containsInAnyOrder(
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
                        ),
                        hasProperty(
                                "items",
                                contains(
                                        hasProperty(
                                                "promos",
                                                contains(
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.ZERO)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldHaveZeroApplePayDiscount() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        ALLOWED_APPLE_PAY_REQUEST
                )
                        .build(),
                enableApplePayDiscountHeader()
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
                                                                comparesEqualTo(BigDecimal.ZERO)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldNotApplyApplePayDiscountIfUsedAsRegularCouponCode() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY),
                                price(BigDecimal.valueOf(1000)))
                        .withPaymentType(null)
                        .build())
                        .withCoupon(applePayDiscountPromoCode)
                        .build(),
                enableApplePayDiscountHeader()
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
    public void shouldApplyApplePayAndCoupon() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
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

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY),
                                price(BigDecimal.valueOf(1000)))
                        .withPaymentType(APPLE_PAY)
                        .build(), ALLOWED_APPLE_PAY_REQUEST)
                        .withCoupon(COUPON_FOR_PROMO)
                        .build(),
                enableApplePayDiscountHeader()
        );

        assertThat(discountResponse, hasProperty(
                "orders",
                containsInAnyOrder(
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
                                                                comparesEqualTo(BigDecimal.valueOf(48L))
                                                        )
                                                )
                                        )
                                )
                        ),
                        hasProperty(
                                "items",
                                contains(
                                        hasProperty(
                                                "promos",
                                                containsInAnyOrder(
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.ZERO)
                                                        ),
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(52L))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldHaveZeroApplePayAndCoupon() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
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

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(ALLOWED_APPLE_PAY_REQUEST)
                        .withCoupon(COUPON_FOR_PROMO)
                        .build(),
                enableApplePayDiscountHeader()
        );

        assertThat(discountResponse, hasProperty(
                "orders",
                containsInAnyOrder(
                        hasProperty(
                                "items",
                                contains(
                                        hasProperty(
                                                "promos",
                                                containsInAnyOrder(
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.ZERO)
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
    public void shouldApplyApplePayAndCouponAndCoin() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
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

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY),
                                price(BigDecimal.valueOf(1000)))
                        .withPaymentType(APPLE_PAY)
                        .build(), ALLOWED_APPLE_PAY_REQUEST)
                        .withCoupon(COUPON_FOR_PROMO)
                        .withCoins(coin)
                        .build(),
                enableApplePayDiscountHeader()
        );

        assertThat(discountResponse, hasProperty(
                "orders",
                containsInAnyOrder(
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
                                                                comparesEqualTo(BigDecimal.valueOf(48L))
                                                        ),
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(48L))
                                                        )
                                                )
                                        )
                                )
                        ),
                        hasProperty(
                                "items",
                                contains(
                                        hasProperty(
                                                "promos",
                                                containsInAnyOrder(
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.ZERO)
                                                        ),
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(52L))
                                                        ),
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.valueOf(52L))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    public void shouldHaveZeroApplePayAndCouponAndCoin() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
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

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(ALLOWED_APPLE_PAY_REQUEST)
                        .withCoupon(COUPON_FOR_PROMO)
                        .withCoins(coin)
                        .build(),
                enableApplePayDiscountHeader()
        );

        assertThat(discountResponse, hasProperty(
                "orders",
                containsInAnyOrder(
                        hasProperty(
                                "items",
                                contains(
                                        hasProperty(
                                                "promos",
                                                containsInAnyOrder(
                                                        hasProperty(
                                                                "discount",
                                                                comparesEqualTo(BigDecimal.ZERO)
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
    public void shouldIgnoreApplePayPromoNotActive() {
        final Date yesterday = new Date(clock.millis() - TimeUnit.DAYS.toMillis(1));
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
                        .setBudget(INITIAL_CURRENT_BUDGET)
                        .setEndDate(yesterday)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY),
                                price(BigDecimal.valueOf(1000)))
                        .withPaymentType(APPLE_PAY)
                        .build(), ALLOWED_APPLE_PAY_REQUEST)
                        .build(),
                enableApplePayDiscountHeader()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                containsInAnyOrder(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                empty()
                                                        )
                                                )
                                        ),
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
    public void shouldIgnoreApplePayPromoNotExists() {
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(itemKey(DEFAULT_ITEM_KEY),
                                price(BigDecimal.valueOf(1000)))
                        .withPaymentType(APPLE_PAY)
                        .build(), ALLOWED_APPLE_PAY_REQUEST)
                        .build(),
                enableApplePayDiscountHeader()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                containsInAnyOrder(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                empty()
                                                        )
                                                )
                                        ),
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
    public void shouldIgnoreApplePayPromoPhoneAlreadyUsed() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(applePayDiscountPromoCode)
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
                                        .withPaymentType(APPLE_PAY)
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory
                                .withUidBuilder(DEFAULT_UID)
                                .withPhone(phone)
                                .buildOperationContext()
                        )
                        .build()
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId("2")
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withPaymentType(APPLE_PAY)
                                        .build(), ALLOWED_APPLE_PAY_REQUEST
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
                                containsInAnyOrder(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                empty()
                                                        )
                                                )
                                        ),
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

    private static HttpHeaders enableApplePayDiscountHeader() {
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "enable_applepay_discount=1");
        return header;
    }
}
