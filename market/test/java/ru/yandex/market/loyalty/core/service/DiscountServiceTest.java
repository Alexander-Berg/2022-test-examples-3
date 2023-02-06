package ru.yandex.market.loyalty.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.cart.CartFlag;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.api.model.promocode.MarketLoyaltyPromocodeWarningCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeError;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeWarning;
import ru.yandex.market.loyalty.api.model.red.RedOrder;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserReferralPromocodeDao;
import ru.yandex.market.loyalty.core.logbroker.DiscountEvent;
import ru.yandex.market.loyalty.core.logbroker.EventType;
import ru.yandex.market.loyalty.core.logbroker.OrderEvent;
import ru.yandex.market.loyalty.core.logbroker.PromocodeEvent;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoCodeGeneratorType;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.model.ydb.UserReferralPromocode;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.discount.ItemPromoCalculation;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationResult;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;
import ru.yandex.market.loyalty.core.utils.CoinRequestUtils;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountServiceTestingUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.BUDGET_EXCEEDED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_APPLICABLE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COUPON_NOT_EXISTS;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.DISCOUNT_NOT_ACTIVE;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.INSUFFICIENT_TOTAL;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.MIN_ORDER_TOTAL_VIOLATED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_EXISTS_PROMO_KEYS;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_SUITABLE_COIN;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_USE_PROMO_KEYS;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.PROMOCODE_IS_EXPIRED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.PROMOCODE_NOT_EXIST;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.PROMO_NOT_ACTIVE;
import static ru.yandex.market.loyalty.api.model.PromoType.BLUE_SET;
import static ru.yandex.market.loyalty.api.model.PromoType.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.api.model.PromoType.EXTERNAL;
import static ru.yandex.market.loyalty.api.model.PromoType.GENERIC_BUNDLE;
import static ru.yandex.market.loyalty.api.model.PromoType.MARKET_COUPON;
import static ru.yandex.market.loyalty.api.model.PromoType.MARKET_PROMOCODE;
import static ru.yandex.market.loyalty.api.model.PromoType.SMART_SHOPPING;
import static ru.yandex.market.loyalty.api.model.PromoType.UNKNOWN;
import static ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode.NO_AVAILABLE_PROMOCODES;
import static ru.yandex.market.loyalty.core.logbroker.EventType.BLUE_SET_DISCOUNT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CHEAPEST_AS_GIFT_DISCOUNT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COIN_DISCOUNT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COIN_ERROR;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COUPON_DISCOUNT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.COUPON_ERROR;
import static ru.yandex.market.loyalty.core.logbroker.EventType.GENERIC_BUNDLE_DISCOUNT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.NOT_SUPPORTED_PROMO_TYPE;
import static ru.yandex.market.loyalty.core.logbroker.EventType.ORDER_REQUEST;
import static ru.yandex.market.loyalty.core.logbroker.EventType.PROMOCODE_DISCOUNT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.PROMOCODE_ERROR;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinType.FIXED;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PICKUP_DISABLED_REAR;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PICKUP_PROMO_ENABLED;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMOCODE_ENABLE_NO_AVAILABLE_PROMOCODES_ERROR;
import static ru.yandex.market.loyalty.core.service.discount.ItemPromoCalculation.calculateTotalDiscount;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PHARMA_BUD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.DiscountResponseUtil.hasCouponError;
import static ru.yandex.market.loyalty.core.utils.DiscountServiceTestingUtils.ACTIVE_FROM_DATE;
import static ru.yandex.market.loyalty.core.utils.DiscountServiceTestingUtils.ACTIVE_TO_DATE;
import static ru.yandex.market.loyalty.core.utils.DiscountServiceTestingUtils.discountRequest;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.hasItemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.bundle;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.discount;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.dropship;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.totalPrice;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.anaplanId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.blueSet;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.cheapestAsGift;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.fixedPrice;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.quantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.withQuantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_CODE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_VALUE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixedPromocode;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

/**
 * @author dinyat
 * 05/06/2017
 */
public class DiscountServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final String PROMOCODE = "some promocode";
    private static final long USER_ID = 2133L;
    private static final long FEED_ID = 123;
    private static final String PROMO_KEY = "some promo";
    private static final String SHOP_PROMO_ID = "shop promo id";
    private static final String ANAPLAN_ID = "anaplan id";
    private static final String OFFER_1 = "offer 1";
    private static final String OFFER_2 = "offer 2";
    private static final String OFFER_3 = "offer 3";
    private final static String DEFAULT_ACCRUAL_PROMO_KEY = "accrual_promo";
    private final static long BUSINESS_ID = 987L;
    private final static String PICKUP_SEGMENT_REAR = "market.loyalty.config.pickup.segment.rearr";
    private final static String PICKUP_PROMO = "PICKUP_TEST";

    @Autowired
    private DiscountService discountService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private PromoBundleService bundleService;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    UserReferralPromocodeDao userReferralPromocodeDao;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private DiscountServiceTestingUtils discountServiceTestingUtils;

    @Test
    public void canNotSpendDiscountIfPromoNotActive() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setStartDate(ACTIVE_FROM_DATE)
                .setEndDate(ACTIVE_TO_DATE)
        );
        Date emmitButNotActiveDate = DateUtils.addDays(ACTIVE_FROM_DATE, -1);
        clock.setDate(emmitButNotActiveDate);
        promoService.update(promo);
        MultiCartWithBundlesDiscountResponse discountResponse = discountServiceTestingUtils.spendDiscount(
                discountRequest().withCoupon(DEFAULT_COUPON_CODE)
                        .build());
        assertThat(discountResponse, hasCouponError(PROMO_NOT_ACTIVE));
    }

    @Test
    public void testSuccessfulCalculateDiscount() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        MultiCartWithBundlesDiscountResponse orderResponse = discountServiceTestingUtils.calculateDiscounts(
                discountRequest().withCoupon(DEFAULT_COUPON_CODE)
                        .build());

        assertThat(calculateTotalDiscount(orderResponse.getOrders().get(0)),
                comparesEqualTo(PromoUtils.DEFAULT_COUPON_VALUE.add(BigDecimal.ONE)));
    }

    @Test
    public void testInsufficientTotalCalculateDiscountWithZeroItemWeights() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true)
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ZERO),
                                price(BigDecimal.ZERO)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                quantity(BigDecimal.ZERO),
                                price(BigDecimal.ZERO)
                        )
                        .build()
        ).withCoupon(DEFAULT_COUPON_CODE).build();

        MultiCartWithBundlesDiscountResponse discountResponse = discountServiceTestingUtils.calculateDiscounts(request);
        assertThat(discountResponse, hasCouponError(INSUFFICIENT_TOTAL));
    }

    @Test
    public void testInsufficientTotalWithOrderTotalLessThanCouponValue() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).
                        withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                price(PromoUtils.DEFAULT_COUPON_VALUE.subtract(BigDecimal.TEN))
                        )
                        .build()
        ).withCoupon(DEFAULT_COUPON_CODE).build();

        MultiCartWithBundlesDiscountResponse discountResponse = discountServiceTestingUtils.calculateDiscounts(request);
        assertThat(discountResponse, hasCouponError(INSUFFICIENT_TOTAL));
    }

    @Test
    public void testInsufficientTotalWithOrderTotalEqualToCouponValue() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(PromoUtils.DEFAULT_COUPON_VALUE)
                ).build()
        ).withCoupon(DEFAULT_COUPON_CODE).build();

        MultiCartWithBundlesDiscountResponse discountResponse = discountServiceTestingUtils.calculateDiscounts(request);
        assertThat(discountResponse, hasCouponError(INSUFFICIENT_TOTAL));
    }

    @Test
    public void testInsufficientTotalWithOrderTotalGreaterThanCouponValue() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(PromoUtils.DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                ).build()
        ).withCoupon(DEFAULT_COUPON_CODE).build();

        discountServiceTestingUtils.calculateDiscounts(request);
    }

    @Test
    @Ignore("Disabled after ticket MARKETDISCOUNT-8487 was done")
    public void testCouponNotAppliedForDropship() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true)
                        .withCartId("some cart")
                        .withOrderItem()
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                dropship()
                        )
                        .build(),
                orderRequestBuilder(true)
                        .withCartId("another cart")
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                dropship()
                        )
                        .build()
        )
                .withCoupon(DEFAULT_COUPON_CODE)
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.spendDiscount(request);

        assertThat(response.getOrders(), hasItems(
                allOf(
                        hasProperty("cartId", equalTo("some cart")),
                        hasProperty("items", hasItems(
                                allOf(
                                        hasItemKey(DEFAULT_ITEM_KEY),
                                        hasProperty("promos", hasSize(1))
                                ),
                                allOf(
                                        hasItemKey(ANOTHER_ITEM_KEY),
                                        hasProperty("promos", empty())
                                )
                        ))
                ),
                allOf(
                        hasProperty("cartId", equalTo("another cart")),
                        hasProperty("items", contains(allOf(
                                hasItemKey(DEFAULT_ITEM_KEY),
                                hasProperty("promos", empty())
                        )))
                )
        ));
    }

    @Test
    public void testPercentCouponApplied() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT));

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(false).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(100))
                ).build())
                .withCoupon(DEFAULT_COUPON_CODE)
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.spendDiscount(request);

        OrderWithBundlesResponse defaultOrder = response.getOrders().get(0);
        assertThat(defaultOrder.getItems().get(0).getPromos(), hasSize(1));
        assertThat(
                defaultOrder.getItems().get(0).getPromos(),
                contains(
                        allOf(
                                hasProperty("promoType", equalTo(MARKET_COUPON)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(5)))
                        )
                )
        );
    }

    @Test
    public void testPercentCoinAppliedWithExcludedCategory() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setPlatform(
                CoreMarketPlatform.RED));
        CoinKey coinKey = createCoin(promo);

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                OrderRequestUtils.orderRequestBuilder()
                        .withOrderItem()
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                categoryId(PHARMA_BUD_CATEGORY_ID)
                        )
                        .build()
        )
                .withCoins(coinKey)
                .withPlatform(MarketPlatform.RED)
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.spendDiscount(request);

        OrderWithBundlesResponse defaultOrder = response.getOrders().get(0);
        assertThat(defaultOrder, hasProperty("items", hasItem(allOf(
                hasItemKey(DEFAULT_ITEM_KEY),
                hasProperty("promos", contains(
                        allOf(
                                hasProperty("promoType", equalTo(SMART_SHOPPING)),
                                hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                        )
                ))
        ))));
    }

    @Test
    public void testCouponDiscountEvent() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setBudget(BigDecimal.valueOf(3000)));
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                )
                        .build()
        )
                .withCoupon(DEFAULT_COUPON_CODE)
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(DiscountEvent
                .builder()
                .setEventType(COUPON_DISCOUNT)
                .setPromoId(promo.getId())
                .setCouponCode(DEFAULT_COUPON_CODE.toUpperCase())
                .setOrderIds(Set.of())
                .setPlatform(CoreMarketPlatform.BLUE)
                .setUid(DEFAULT_UID)
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setEmail(DEFAULT_EMAIL)
                .setPromoType(MARKET_COUPON)
                .setHttpMethod("calc")
                .setDiscount(DEFAULT_COUPON_VALUE.setScale(
                        2, RoundingMode.FLOOR))
                .build(), "requestId", "promoKey")));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(OrderEvent
                .builder()
                .setHttpMethod("calc")
                .setPlatform(CoreMarketPlatform.BLUE)
                .setOrderIds(Collections.emptySet())
                .setEventType(ORDER_REQUEST)
                .setCoinsCount(0)
                .setOrderTotalDiscount(response
                        .getOrders()
                        .stream()
                        .map(ItemPromoCalculation::calculateTotalDiscount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .setOrderTotalPrice(totalPrice(request))
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setUid(DEFAULT_UID)
                .setEmail(DEFAULT_EMAIL)
                .build(), "requestId")));
    }

    @Test
    public void testCouponErrorEvent() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse().setBudget(BigDecimal.ONE));
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(PromoUtils.DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                ).build()
        ).withCoupon(DEFAULT_COUPON_CODE).build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(DiscountEvent
                .builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setHttpMethod("calc")
                .setOrderIds(null)
                .setEventType(COUPON_ERROR)
                .setCouponCode(DEFAULT_COUPON_CODE.toUpperCase())
                .setErrorType(BUDGET_EXCEEDED.name())
                .setPromoId(promo.getPromoId().getId())
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setUid(DEFAULT_UID)
                .setEmail(DEFAULT_EMAIL)
                .setPromoType(MARKET_COUPON)
                .setPromoKey(promo.getPromoKey())
                .setIsError(true)
                .build(), "requestId", "promoKey")));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(OrderEvent
                .builder()
                .setHttpMethod("calc")
                .setPlatform(CoreMarketPlatform.BLUE)
                .setOrderIds(Collections.emptySet())
                .setEventType(ORDER_REQUEST)
                .setCoinsCount(0)
                .setOrderTotalDiscount(response
                        .getOrders()
                        .stream()
                        .map(ItemPromoCalculation::calculateTotalDiscount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .setOrderTotalPrice(totalPrice(request))
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setUid(DEFAULT_UID)
                .setEmail(DEFAULT_EMAIL)
                .build(), "requestId")));
    }

    @Test
    public void testCoinDiscountEvent() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
        );

        deferredMetaTransactionService.consumeBatchOfTransactions(1);

        CoinKey coinKey = coinService.create.createCoin(promo, CoinInsertRequest.authMarketBonus(USER_ID)
                .setSourceKey("coin1")
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(ACTIVE)
                .build());

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COIN_FIXED_NOMINAL.add(BigDecimal.TEN))
                )
                        .build())
                .withCoins(coinKey)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(DiscountEvent
                .builder()
                .setEventType(COIN_DISCOUNT)
                .setPromoId(promo.getPromoId().getId())
                .setCoinType(FIXED)
                .setOrderIds(Set.of())
                .setPlatform(CoreMarketPlatform.BLUE)
                .setUid(USER_ID)
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setEmail(DEFAULT_EMAIL)
                .setPromoType(SMART_SHOPPING)
                .setPromoKey(promo.getPromoKey())
                .setHttpMethod("calc")
                .setDiscount(DEFAULT_COIN_FIXED_NOMINAL.setScale(
                        2, RoundingMode.FLOOR))
                .setCoinPropsId(promo.getCoinPropsId())
                .build(), "requestId")));


        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(OrderEvent
                .builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setHttpMethod("calc")
                .setOrderIds(Collections.emptySet())
                .setEventType(ORDER_REQUEST)
                .setCoinsCount(1)
                .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                        .get(0)))
                .setOrderTotalPrice(totalPrice(request))
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setUid(USER_ID)
                .setEmail(DEFAULT_EMAIL)
                .build(), "requestId")));
    }

    @NotNull
    private static OrderRequestUtils.OrderRequestBuilder orderRequestBuilder(boolean calc) {
        OrderRequestUtils.OrderRequestBuilder orderRequestBuilder = OrderRequestUtils.orderRequestBuilder();
        if (calc) {
            orderRequestBuilder.withOrderId((String)null);
        }
        return orderRequestBuilder;
    }

    @Test
    public void testCoinErrorEvent() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
                .setBudget(BigDecimal.ONE)
        );

        CoinKey coinKey = coinService.create.createCoin(promo, CoinInsertRequest.authMarketBonus(0L)
                .setSourceKey("coin1")
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(ACTIVE)
                .build());

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(PromoUtils.DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                ).build())
                .withCoins(coinKey)
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(DiscountEvent
                .builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setHttpMethod("calc")
                .setEventType(COIN_ERROR)
                .setCoinType(FIXED)
                .setErrorType(BUDGET_EXCEEDED.name())
                .setPromoId(promo.getPromoId().getId())
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setUid(0L)
                .setEmail(DEFAULT_EMAIL)
                .setPromoType(SMART_SHOPPING)
                .setPromoKey(promo.getPromoKey())
                .setIsError(true)
                .setCoinPropsId(promo.getCoinPropsId())
                .build(), "requestId")));


        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(OrderEvent
                .builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setHttpMethod("calc")
                .setOrderIds(Collections.emptySet())
                .setEventType(ORDER_REQUEST)
                .setCoinsCount(1)
                .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders().get(0)))
                .setOrderTotalPrice(totalPrice(request))
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setUid(0L)
                .setEmail(DEFAULT_EMAIL)
                .build(), "requestId")));
    }

    @Test
    public void testPromocodeDiscountEvent() {
        PromocodeActivationResult activationResult = createPromocodeFor(USER_ID);
        Promo promo = promoService.getPromo(activationResult.getPromoId());

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                )
                .build())
                .withCoupon(PROMOCODE)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getPromocodeErrors(), empty());

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                PromocodeEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setEventType(EventType.PROMOCODE_ACTIVATED)
                        .setPromoId(activationResult.getPromoId())
                        .setPromoKey(activationResult.getPromoKey())
                        .setShopPromoId(activationResult.getShopPromoId())
                        .setClientId(activationResult.getClientId())
                        .setCouponCode(activationResult.getCode())
                        .setUid(USER_ID)
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(PROMOCODE_DISCOUNT)
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setOrderIds(Set.of())
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(MARKET_PROMOCODE)
                        .setPromoKey(activationResult.getPromoKey())
                        .setCouponCode(PROMOCODE.toUpperCase())
                        .setHttpMethod("calc")
                        .setCoinType(FIXED)
                        .setPromoId(activationResult.getPromoId())
                        .setDiscount(DEFAULT_COUPON_VALUE.setScale(
                                2, RoundingMode.FLOOR))
                        .setShopPromoId(promo.getShopPromoId())
                        .setAnaplanId(ANAPLAN_ID)
                        .setBusinessId(BUSINESS_ID)
                        .setSourceType(NMarket.Common.Promo.Promo.ESourceType.LOYALTY.name())
                        .setCoinPropsId(promo.getCoinPropsId())
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(request))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void testPromocodeIsExpiredDiscountEvent() {
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode("PROMOCODE")
                        .setStartDate(toDate(LocalDateTime.now().minus(1, ChronoUnit.HOURS)))
                        .setEndDate(toDate(LocalDateTime.now().plus(1, ChronoUnit.HOURS)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setAnaplanId(ANAPLAN_ID)
                        .setBusinessId(BUSINESS_ID)
        );

        promoService.reloadActiveSmartShoppingPromoIdsCache();

        PromocodeActivationResult activationResult =
         promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(
                        Set.of("PROMOCODE"))
                .build())
                .getActivationResults()
                .get(0);

        clock.setDate(toDate(LocalDateTime.now().plus(2, ChronoUnit.HOURS)));

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                )
                .build())
                .withCoupon("PROMOCODE")
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getPromocodeErrors(), hasSize(1));

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                PromocodeEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setEventType(EventType.PROMOCODE_ACTIVATED)
                        .setPromoId(activationResult.getPromoId())
                        .setPromoKey(activationResult.getPromoKey())
                        .setShopPromoId(activationResult.getShopPromoId())
                        .setClientId(activationResult.getClientId())
                        .setCouponCode(activationResult.getCode())
                        .setUid(USER_ID)
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(PROMOCODE_ERROR)
                        .setErrorType(PROMOCODE_IS_EXPIRED.name())
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(MARKET_PROMOCODE)
                        .setCouponCode("PROMOCODE")
                        .setHttpMethod("calc")
                        .setIsError(true)
                        .setPromoId(0L)
                        .setPromoKey("unknown")
                        .build(),
                "requestId", "feedOffers"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(COUPON_ERROR)
                        .setErrorType(COUPON_NOT_EXISTS.name())
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(MARKET_COUPON)
                        .setCouponCode("PROMOCODE")
                        .setHttpMethod("calc")
                        .setIsError(true)
                        .setPromoId(0L)
                        .setPromoKey("unknown")
                        .build(),
                "requestId", "feedOffers"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(request))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void shouldBeDiscountNotActiveDiscountEvent() {
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode("PROMOCODE")
                        .setStartDate(toDate(LocalDateTime.now().minus(1, ChronoUnit.HOURS)))
                        .setEndDate(toDate(LocalDateTime.now().plus(1, ChronoUnit.HOURS)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setAnaplanId(ANAPLAN_ID)
                        .setBusinessId(BUSINESS_ID)
                        .setBindOnlyOnce(true)
        );

        promoService.reloadActiveSmartShoppingPromoIdsCache();

        PromocodeActivationResult activationResult =
         promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(
                        Set.of("PROMOCODE"))
                .build())
                .getActivationResults()
                .get(0);

        MultiCartDiscountRequest spendRequest = DiscountRequestBuilder.builder(orderRequestBuilder(false)
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                )
                .build())
                .withCoupon("PROMOCODE")
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartDiscountRequest calcRequest = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                )
                .build())
                .withCoupon("PROMOCODE")
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        // Потратим монетку
        discountServiceTestingUtils.spendDiscount(spendRequest);
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(calcRequest);

        assertThat(response.getPromocodeErrors(), hasSize(1));

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                PromocodeEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setEventType(EventType.PROMOCODE_ACTIVATED)
                        .setPromoId(activationResult.getPromoId())
                        .setPromoKey(activationResult.getPromoKey())
                        .setShopPromoId(activationResult.getShopPromoId())
                        .setClientId(activationResult.getClientId())
                        .setCouponCode(activationResult.getCode())
                        .setUid(USER_ID)
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(PROMOCODE_ERROR)
                        .setErrorType(DISCOUNT_NOT_ACTIVE.name())
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(MARKET_PROMOCODE)
                        .setCouponCode("PROMOCODE")
                        .setHttpMethod("calc")
                        .setIsError(true)
                        .setPromoId(0L)
                        .setPromoKey("unknown")
                        .build(),
                "requestId", "feedOffers"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(COUPON_ERROR)
                        .setErrorType(COUPON_NOT_EXISTS.name())
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(MARKET_COUPON)
                        .setCouponCode("PROMOCODE")
                        .setHttpMethod("calc")
                        .setIsError(true)
                        .setPromoId(0L)
                        .setPromoKey("unknown")
                        .build(),
                "requestId", "feedOffers"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(spendRequest))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void shouldNotBePromocodeNotExistsDiscountEventWhenCouponExists() {
        Promo couponPromo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode("PROMOCODE")
        );

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                )
                .build())
                .withCoupon("PROMOCODE")
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getPromocodeErrors(), empty());

        discountService.awaitLogBroker();

        verify(logBrokerClient, never()).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(PROMOCODE_ERROR)
                        .setErrorType(PROMOCODE_NOT_EXIST.name())
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(MARKET_PROMOCODE)
                        .setHttpMethod("calc")
                        .setIsError(true)
                        .setPromoId(0L)
                        .setPromoKey("unknown")
                        .build(),
                "requestId", "feedOffers", "couponCode"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(COUPON_DISCOUNT)
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(MARKET_COUPON)
                        .setCouponCode("PROMOCODE")
                        .setHttpMethod("calc")
                        .setPromoId(couponPromo.getPromoId().getId())
                        .setDiscount(DEFAULT_COUPON_VALUE.setScale(
                                2, RoundingMode.FLOOR))
                        .setPromoKey(couponPromo.getPromoKey())
                        .setOrderIds(Collections.emptySet())
                        .build(),
                "requestId", "feedOffers", "objectType"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(request))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void testPromocodeAnonymousActivationErrorEvent() {
        configurationService.set(PROMOCODE_ENABLE_NO_AVAILABLE_PROMOCODES_ERROR, true);

        PromocodeActivationResult activationResult =
                promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build())
                        .getActivationResults()
                        .get(0);

        assertThat(activationResult.getActivationResultCode(), is(NO_AVAILABLE_PROMOCODES));

        promocodeService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                PromocodeEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setEventType(EventType.PROMOCODE_ERROR)
                        .setErrorType(NO_AVAILABLE_PROMOCODES.getCode())
                        .setCouponCode(activationResult.getCode())
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void testPromocodeApplyingErrorEvent() {
        Promo promocodePromo = promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setBudget(BigDecimal.ONE)
                        .setCode(PROMOCODE)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now()
                                .plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setAnaplanId(ANAPLAN_ID)
        );

        promoService.reloadActiveSmartShoppingPromoIdsCache();

        PromocodeActivationResult activationResult = promocodeService.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(
                                Set.of(PROMOCODE))
                        .build())
                .getActivationResults()
                .get(0);

        assertThat(activationResult.getActivationResultCode(), is(PromocodeActivationResultCode.SUCCESS));

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                )
                .build())
                .withCoupon(PROMOCODE)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getPromocodeErrors(), not(empty()));

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                PromocodeEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setEventType(EventType.PROMOCODE_ACTIVATED)
                        .setPromoId(activationResult.getPromoId())
                        .setPromoKey(activationResult.getPromoKey())
                        .setShopPromoId(activationResult.getShopPromoId())
                        .setClientId(activationResult.getClientId())
                        .setCouponCode(activationResult.getCode())
                        .setUid(USER_ID)
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEventType(PROMOCODE_ERROR)
                        .setPromoType(MARKET_PROMOCODE)
                        .setErrorType(BUDGET_EXCEEDED.name())
                        .setUid(USER_ID)
                        .setPromoId(activationResult.getPromoId())
                        .setPromoKey(activationResult.getPromoKey())
                        .setEmail(DEFAULT_EMAIL)
                        .setHttpMethod("calc")
                        .setCouponCode(activationResult.getCode())
                        .setShopPromoId(promocodePromo.getShopPromoId())
                        .setAnaplanId(ANAPLAN_ID)
                        .setFeedOffers(Set.of("200321470#65"))
                        .setIsError(true)
                        .setCoinPropsId(promocodePromo.getCoinPropsId())
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(request))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void testGenericBundleDiscountEvent() {
        PromoBundleDescription bundleDescription = createBundleDescription();
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_1),
                        ssku(OFFER_1),
                        bundle("some bundle"),
                        promoKeys(PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_2),
                        ssku(OFFER_2),
                        bundle("some bundle"),
                        price(15000)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(order)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);
        OrderWithBundlesResponse orderWithBundlesResponse = response.getOrders().get(0);

        assertThat(orderWithBundlesResponse.getBundlesToDestroy(), not(hasItem(
                hasProperty("reason", is(BundleDestroyReason.ReasonType.ERROR))
        )));

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(GENERIC_BUNDLE_DISCOUNT)
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setOrderIds(Set.of())
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(GENERIC_BUNDLE)
                        .setPromoKey(bundleDescription.getPromoKey())
                        .setShopPromoId(bundleDescription.getShopPromoId())
                        .setAnaplanId(bundleDescription.getAnaplanId())
                        .setPromoId(bundleDescription.getPromoId())
                        .setHttpMethod("calc")
                        .setDiscount(BigDecimal.valueOf(15000).setScale(
                                2, RoundingMode.FLOOR))
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(request))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void testCheapestAsGiftDiscountEvent() {
        PromoBundleDescription bundleDescription = createCheapestAsGiftDescription();
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_1),
                        ssku(OFFER_1),
                        quantity(2),
                        promoKeys(PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_2),
                        ssku(OFFER_2),
                        promoKeys(PROMO_KEY),
                        price(15000)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(order)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);
        OrderWithBundlesResponse orderWithBundlesResponse = response.getOrders().get(0);

        assertThat(orderWithBundlesResponse.getBundlesToDestroy(), not(hasItem(
                hasProperty("reason", is(BundleDestroyReason.ReasonType.ERROR))
        )));

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(CHEAPEST_AS_GIFT_DISCOUNT)
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setOrderIds(Set.of())
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(CHEAPEST_AS_GIFT)
                        .setPromoKey(bundleDescription.getPromoKey())
                        .setShopPromoId(bundleDescription.getShopPromoId())
                        .setAnaplanId(bundleDescription.getAnaplanId())
                        .setPromoId(bundleDescription.getPromoId())
                        .setHttpMethod("calc")
                        .setDiscount(BigDecimal.valueOf(15000).setScale(
                                2, RoundingMode.FLOOR))
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(request))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void testBlueSetDiscountEvent() {
        PromoBundleDescription bundleDescription = createBlueSetDescription();
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_1),
                        ssku(OFFER_1),
                        promoKeys(PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_2),
                        ssku(OFFER_2),
                        promoKeys(PROMO_KEY),
                        price(15000)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(order)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);
        OrderWithBundlesResponse orderWithBundlesResponse = response.getOrders().get(0);

        assertThat(orderWithBundlesResponse.getBundlesToDestroy(), not(hasItem(
                hasProperty("reason", is(BundleDestroyReason.ReasonType.ERROR))
        )));

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(BLUE_SET_DISCOUNT)
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setOrderIds(Set.of())
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(BLUE_SET)
                        .setPromoKey(bundleDescription.getPromoKey())
                        .setShopPromoId(bundleDescription.getShopPromoId())
                        .setAnaplanId(bundleDescription.getAnaplanId())
                        .setPromoId(bundleDescription.getPromoId())
                        .setHttpMethod("calc")
                        .setDiscount(BigDecimal.valueOf(34500).setScale(
                                2, RoundingMode.FLOOR))
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(request))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void testUnsupportedPromoEvent() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_1),
                        ssku(OFFER_1),
                        discount(PROMO_KEY, UNKNOWN.getCode(), BigDecimal.valueOf(100)),
                        price(10000)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(order)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);
        OrderWithBundlesResponse orderWithBundlesResponse = response.getOrders().get(0);

        assertThat(orderWithBundlesResponse.getExternalItemDiscountFaults(), empty());

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                DiscountEvent.builder()
                        .setEventType(NOT_SUPPORTED_PROMO_TYPE)
                        .setIsError(true)
                        .setErrorType(UNKNOWN.getCode())
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setUid(USER_ID)
                        .setOrderIds(Set.of())
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setEmail(DEFAULT_EMAIL)
                        .setPromoType(EXTERNAL)
                        .setPromoKey(PROMO_KEY)
                        .setPromoId(0L)
                        .setHttpMethod("calc")
                        .setDiscount(BigDecimal.valueOf(100).setScale(
                                1, RoundingMode.FLOOR))
                        .build(),
                "requestId"
        )));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(
                OrderEvent.builder()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setHttpMethod("calc")
                        .setOrderIds(Collections.emptySet())
                        .setEventType(ORDER_REQUEST)
                        .setCoinsCount(0)
                        .setOrderTotalDiscount(calculateTotalDiscount(response.getOrders()
                                .get(0)))
                        .setOrderTotalPrice(totalPrice(request))
                        .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                        .setUid(USER_ID)
                        .setEmail(DEFAULT_EMAIL)
                        .build(),
                "requestId"
        )));
    }

    @Test
    public void testIsPickupPromocode() {
        String pickupPromocode = "test_pickup_promocode";
        configurationService.set(ConfigurationService.PICKUP_PROMO, pickupPromocode);

        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode(pickupPromocode)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now()
                                .plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setAnaplanId(ANAPLAN_ID)
                        .setBusinessId(BUSINESS_ID));


        promoService.reloadActiveSmartShoppingPromoIdsCache();

        promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(USER_ID)
                .externalPromocodes(
                        Set.of(pickupPromocode))
                .build())
                .getActivationResults()
                .get(0);

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                )
                .build())
                .withCoupon(pickupPromocode)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);
        assertTrue(response.getOrders().get(0).getItems().get(0).getPromos().get(0).getPickupPromocode());
    }

    @Test
    public void testOrderEvent() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
        );

        CoinKey coinKey = coinService.create.createCoin(promo, CoinInsertRequest.authMarketBonus(0L)
                .setSourceKey("coin1")
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(ACTIVE)
                .build());

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(false).withOrderItem().build()
        )
                .withCoins(coinKey)
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.spendDiscount(request);

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(DiscountEvent
                .builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setHttpMethod("spend")
                .setEventType(COIN_DISCOUNT)
                .setCoinType(FIXED)
                .setDiscount(DEFAULT_COIN_FIXED_NOMINAL)
                .setPromoId(promo.getPromoId().getId())
                .setOrderIds(request.getOrders()
                        .stream()
                        .map(o -> Long.parseLong(o.getOrderId()))
                        .collect(Collectors.toSet()))
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setUid(0L)
                .setEmail(DEFAULT_EMAIL)
                .setPromoType(SMART_SHOPPING)
                .setPromoKey(promo.getPromoKey())
                .setCoinPropsId(promo.getCoinPropsId())
                .build(), "discount", "requestId", "promoKey")));


        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(OrderEvent
                .builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setHttpMethod("spend")
                .setOrderIds(request.getOrders()
                        .stream()
                        .map(o -> Long.parseLong(o.getOrderId()))
                        .collect(Collectors.toSet()))
                .setEventType(ORDER_REQUEST)
                .setCoinsCount(1)
                .setOrderTotalDiscount(response.getOrders()
                        .stream()
                        .map(ItemPromoCalculation::calculateTotalDiscount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .setOrderTotalPrice(totalPrice(request))
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setUid(0L)
                .setEmail(DEFAULT_EMAIL)
                .build(), "orderTotalPrice", "orderTotalDiscount", "requestId")));
    }

    @Test
    public void testNotUseAndNotExists() {
        createBlueSetDescription();
        Promo otherPromo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
                .defaultFixed()
        );
        String notExistsPromoKey = randomString();
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_1),
                        ssku(OFFER_1),
                        promoKeys(PROMO_KEY, otherPromo.getPromoKey()),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_2),
                        ssku(OFFER_2),
                        promoKeys(PROMO_KEY, notExistsPromoKey),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(order)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();

        deferredMetaTransactionService.consumeBatchOfTransactions(1);

        discountServiceTestingUtils.calculateDiscounts(request);

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(DiscountEvent
                .builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setUid(USER_ID)
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setEmail(DEFAULT_EMAIL)
                .setPromoType(UNKNOWN)
                .setPromoKey(notExistsPromoKey)
                .setHttpMethod("calc")
                .setEventType(NOT_SUPPORTED_PROMO_TYPE)
                .setErrorType(NOT_EXISTS_PROMO_KEYS.name())
                .setIsError(true)
                .build(), "requestId")));

        verify(logBrokerClient).pushEvent(argThat(samePropertyValuesAs(DiscountEvent
                .builder()
                .setPlatform(CoreMarketPlatform.BLUE)
                .setUid(USER_ID)
                .setClientDeviceType(UsageClientDeviceType.DESKTOP)
                .setEmail(DEFAULT_EMAIL)
                .setPromoType(UNKNOWN)
                .setPromoKey(otherPromo.getPromoKey())
                .setHttpMethod("calc")
                .setEventType(NOT_SUPPORTED_PROMO_TYPE)
                .setErrorType(NOT_USE_PROMO_KEYS.name())
                .setIsError(true)
                .build(), "requestId")));
    }

    @Test
    public void testEmptyCartWorksCorrectly() {
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).build()
        )
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response,
                allOf(
                        hasProperty("orders", contains(hasProperty("items", empty()))),
                        hasProperty("coins", empty()),
                        hasProperty("unusedCoins", empty()),
                        hasProperty("coinErrors", empty()),
                        hasProperty("couponError", is(nullValue()))
                )
        );
    }

    @Test
    public void testEmptyCartWorksCorrectlyWithCoupon() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode(DEFAULT_COUPON_CODE)
        );

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).build()
        )
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .withCoupon(DEFAULT_COUPON_CODE)
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response,
                allOf(
                        hasProperty("orders", contains(hasProperty("items", empty()))),
                        hasProperty("coins", empty()),
                        hasProperty("unusedCoins", empty()),
                        hasProperty("coinErrors", empty()),
                        hasProperty("couponError", hasProperty("error", hasProperty("code",
                         equalTo(COUPON_NOT_APPLICABLE.name()))))
                )
        );
    }


    @Test
    public void testEmptyCartWorksCorrectlyWithCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coin = coinService.create.createCoin(promo, CoinInsertRequest.authMarketBonus(0L)
                .setSourceKey("coin_1")
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(ACTIVE)
                .build()
        );

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).build()
        )
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .withCoins(coin)
                .build();

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response,
                allOf(
                        hasProperty("orders", contains(hasProperty("items", empty()))),
                        hasProperty("coins", contains(hasProperty("id", equalTo(coin.getId())))),
                        hasProperty("unusedCoins", empty()),
                        hasProperty("coinErrors", contains(
                                allOf(
                                        hasProperty("coin", hasProperty("id", equalTo(coin.getId()))),
                                        hasProperty("error", hasProperty("code", equalTo(NOT_SUITABLE_COIN.name())))
                                )
                        )),
                        hasProperty("couponError", is(nullValue()))
                )
        );
    }

    @Test
    @Repeat(5)
    public void shouldSpendInfiniteUseCouponInParallel() throws InterruptedException {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setBudget(BigDecimal.valueOf(1_000_000L))
                .setCouponValue(BigDecimal.valueOf(100L), CoreCouponValueType.FIXED)
        );

        MultiCartDiscountRequest discountRequest = discountRequest().withCoupon(DEFAULT_COUPON_CODE).build();

        testConcurrency(() -> () -> {
            MultiCartWithBundlesDiscountResponse orderResponse =
             discountServiceTestingUtils.spendDiscount(discountRequest);

            assertThat(calculateTotalDiscount(orderResponse.getOrders().get(0)), greaterThan(BigDecimal.ZERO));
        });
    }

    @Test
    public void shouldCalcDiscountWithCoinCountMoreThenBatch() {
        BigDecimal coinNominal = BigDecimal.valueOf(50);
        int coinCountToApply = 15; // количество выбрано так чтобы оно было больше
        // CoinPromoCalculator#COINS_PER_BATCH_MAX


        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed(coinNominal));
        CoinKey[] coinKeys = IntStream.range(0, coinCountToApply)
                .boxed()
                .map(i -> coinService.create.createCoin(promo, CoinRequestUtils.defaultAuth(DEFAULT_UID).build()))
                .toArray(CoinKey[]::new);

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(600))
                ).build())
                .withCoins(coinKeys)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = discountServiceTestingUtils.calculateDiscounts(request);


        assertThat(discountResponse.getOrders().get(0).getItems().get(0).getPromos(), containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(49)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                )
        ));
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldThrowExceptionWhenGivenCouponForRedMarket() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true)
                        .build()
        )
                .withCoupon(DEFAULT_COUPON_CODE)
                .withPlatform(MarketPlatform.RED)
                .build();
        discountServiceTestingUtils.calculateDiscounts(request);
    }

    @Test
    public void shouldApplyCoinsWhenGivenFixedPriceCoins() {
        BigDecimal itemPrice = BigDecimal.valueOf(1000);
        Promo promo =
         promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setPlatform(CoreMarketPlatform.RED));
        CoinKey coinKey = createCoin(promo);

        MultiCartDiscountRequest request = getRedMarketMultiCartDiscountRequest(null, coinKey,
                Collections.singletonList(itemPrice));

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getOrders().get(0).getItems().get(0).getPromos(), contains(
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                )
        ));
    }

    @Test
    public void shouldApplyCoinsWhenGivenDeliveryCoins() {
        BigDecimal itemPrice = BigDecimal.valueOf(100);
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFreeDelivery()
                .setPlatform(
                        CoreMarketPlatform.RED));
        CoinKey coinKey = createCoin(promo);

        MultiCartDiscountRequest request = getRedMarketMultiCartDiscountRequest(null, coinKey,
                Collections.singletonList(itemPrice));

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getOrders().get(0).getDeliveries().get(0).getPromos(), contains(
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING))
                )
        ));
    }

    @Test
    public void shouldApplyCoinsWhenGivenPercentCoins() {
        BigDecimal itemPrice = BigDecimal.valueOf(1000);
        BigDecimal percentNominal = BigDecimal.TEN;
        BigDecimal expectedDiscount = BigDecimal.valueOf(100);

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultPercent(
                percentNominal).setPlatform(CoreMarketPlatform.RED));
        CoinKey coinKey = createCoin(promo);
        MultiCartDiscountRequest request = getRedMarketMultiCartDiscountRequest(null, coinKey,
                Collections.singletonList(itemPrice));

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getOrders().get(0).getItems().get(0).getPromos(), contains(
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(expectedDiscount))
                )
        ));
    }

    @Test
    public void shouldApplyBonusesWithCoinsWhenGivenBoth() {
        Promo promo =
         promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setPlatform(CoreMarketPlatform.RED));
        BigDecimal itemPrice = BigDecimal.valueOf(1000);
        CoinKey coinKey = createCoin(promo);

        Long redPromoId = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse(PromoSubType.RED_ORDER)
                        .setBudget(BigDecimal.valueOf(700))
                        .setPlatform(CoreMarketPlatform.RED)
        ).getId();
        BigDecimal fullDiscount = BigDecimal.valueOf(300);
        RedOrder redOrder = new RedOrder(redPromoId, fullDiscount);

        MultiCartDiscountRequest request = getRedMarketMultiCartDiscountRequest(
                redOrder, coinKey, Collections.singletonList(itemPrice));

        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getOrders().get(0).getItems().get(0).getPromos(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("promoType", equalTo(SMART_SHOPPING)),
                                hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                        ),
                        allOf(
                                hasProperty("promoType", equalTo(MARKET_COUPON)),
                                hasProperty("discount", comparesEqualTo(fullDiscount))
                        )
                )
        );
    }

    @NotNull
    private CoinKey createCoin(Promo promo) {
        return coinService.create.createCoin(
                promo,
                CoinRequestUtils.defaultAuth(DEFAULT_UID)
                        .setStatus(ACTIVE)
                        .build()
        );
    }

    private static MultiCartDiscountRequest getRedMarketMultiCartDiscountRequest(
            RedOrder redOrder, CoinKey coinKey, Collection<BigDecimal> itemPrices
    ) {
        List<OrderWithDeliveriesRequest> orders = itemPrices.stream().map(item -> orderRequestBuilder(true)
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(item)
                ).build()).collect(Collectors.toList());
        return getRedMarketMultiCartDiscountRequest(redOrder, coinKey, orders);
    }

    private static MultiCartDiscountRequest getRedMarketMultiCartDiscountRequest(
            RedOrder redOrder, CoinKey coinKey, List<OrderWithDeliveriesRequest> orders
    ) {
        return DiscountRequestBuilder.builder(orders)
                .withCoins(coinKey)
                .withRedOrder(redOrder)
                .withPlatform(MarketPlatform.RED)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                .build();
    }

    @Test
    public void shouldApplyPromoForAllOrdersWhenGivenMultiOrderRedMarketRequest() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setPlatform(CoreMarketPlatform.RED));
        CoinKey coinKey = createCoin(promo);
        BigDecimal price = BigDecimal.valueOf(1000);
        List<OrderWithDeliveriesRequest> orders = Arrays.asList(
                orderRequestBuilder(true)
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(price)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(price)
                        )
                        .build(),
                orderRequestBuilder(true)
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(price)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(price)
                        )
                        .build()
        );
        MultiCartDiscountRequest request = getRedMarketMultiCartDiscountRequest(null, coinKey, orders);

        MultiCartWithBundlesDiscountResponse discountResponse = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(discountResponse.getOrders(), hasSize(2));
        BigDecimal discount = DEFAULT_COIN_FIXED_NOMINAL.divide(BigDecimal.valueOf(4), RoundingMode.HALF_DOWN);
        assertThat(discountResponse.getOrders().get(0).getItems().stream().flatMap(item -> item.getPromos().stream()).collect(Collectors.toList()), containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(discount))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(discount))
                )
        ));
        assertThat(discountResponse.getOrders().get(1).getItems().stream().flatMap(item -> item.getPromos().stream()).collect(Collectors.toList()), containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(discount))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(discount))
                )
        ));
    }

    @Test
    public void shouldKeepOneRubblePriceWhenGivenPromoAndCouponGreaterThanPrice() {
        Long redPromoId = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse(PromoSubType.RED_ORDER)
                        .setBudget(BigDecimal.valueOf(700))
                        .setPlatform(CoreMarketPlatform.RED)
        ).getId();
        BigDecimal price = BigDecimal.valueOf(80);
        RedOrder redOrder = new RedOrder(redPromoId, price);
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setPlatform(CoreMarketPlatform.RED));
        CoinKey coinKey = createCoin(promo);
        List<OrderWithDeliveriesRequest> orders = Arrays.asList(
                orderRequestBuilder(true)
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(price)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(price)
                        )
                        .build(),
                orderRequestBuilder(true)
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(price)
                        )
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(price)
                        )
                        .build()
        );

        MultiCartDiscountRequest request = getRedMarketMultiCartDiscountRequest(redOrder, coinKey, orders);

        MultiCartWithBundlesDiscountResponse discountResponse = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(discountResponse.getOrders(), hasSize(2));
        BigDecimal coinDiscount = BigDecimal.valueOf(59);
        BigDecimal bringlyBonusDiscount = BigDecimal.valueOf(20);
        assertThat(discountResponse.getOrders().get(0).getItems().stream().flatMap(item -> item.getPromos().stream()).collect(Collectors.toList()), containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinDiscount))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinDiscount))
                ),
                allOf(
                        hasProperty("promoType", equalTo(MARKET_COUPON)),
                        hasProperty("discount", comparesEqualTo(bringlyBonusDiscount))
                ),
                allOf(
                        hasProperty("promoType", equalTo(MARKET_COUPON)),
                        hasProperty("discount", comparesEqualTo(bringlyBonusDiscount))
                )
        ));
        assertThat(discountResponse.getOrders().get(1).getItems().stream().flatMap(item -> item.getPromos().stream()).collect(Collectors.toList()), containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinDiscount))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinDiscount))
                ),
                allOf(
                        hasProperty("promoType", equalTo(MARKET_COUPON)),
                        hasProperty("discount", comparesEqualTo(bringlyBonusDiscount))
                ),
                allOf(
                        hasProperty("promoType", equalTo(MARKET_COUPON)),
                        hasProperty("discount", comparesEqualTo(bringlyBonusDiscount))
                )
        ));
    }

    @Test
    public void shouldCalcDiscountWithCouponAndCoinCountMoreThenBatch() {
        BigDecimal couponValue = BigDecimal.valueOf(500);
        BigDecimal coinNominal = BigDecimal.valueOf(50);
        BigDecimal itemPrice = BigDecimal.valueOf(1000);
        int coinCountToApply = 15; // количество выбрано так чтобы оно было больше
        // CoinPromoCalculator#COINS_PER_BATCH_MAX

        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode(DEFAULT_COUPON_CODE)
                .setCouponValue(couponValue, CoreCouponValueType.FIXED)
        );
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed(coinNominal));
        CoinKey[] coinKeys = IntStream.range(0, coinCountToApply)
                .boxed()
                .map(i -> coinService.create.createCoin(promo, CoinRequestUtils.defaultAuth(DEFAULT_UID).build()))
                .toArray(CoinKey[]::new);

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(itemPrice)
                ).build())
                .withCoupon(DEFAULT_COUPON_CODE)
                .withCoins(coinKeys)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(discountResponse.getOrders().get(0).getItems().get(0).getPromos(), containsInAnyOrder(
                allOf(
                        hasProperty("promoType", equalTo(MARKET_COUPON)),
                        hasProperty("discount", comparesEqualTo(couponValue))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(49)))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                ),
                allOf(
                        hasProperty("promoType", equalTo(SMART_SHOPPING)),
                        hasProperty("discount", comparesEqualTo(coinNominal))
                )
        ));
    }

//    [TODO]

//    @Test
//    public void testCoinNotAppliedToDropship() {
//        Promo promoFixed = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
//            .defaultFixed()
//        );
//
//        CoinKey fixedCoinKey = coinService.create.createCoin(promoFixed, CoinInsertRequest.authMarketBonus(0L)
//            .setSourceKey("coinFixed")
//            .setReason(CoreCoinCreationReason.OTHER)
//            .setStatus(ACTIVE)
//            .build());
//
//        Promo promoDelivery = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping
//            .defaultFreeDelivery()
//        );
//
//        CoinKey deliveryCoinKey = coinService.create.createCoin(promoDelivery, CoinInsertRequest.authMarketBonus(0L)
//            .setSourceKey("coinDelivery")
//            .setReason(CoreCoinCreationReason.OTHER)
//            .setStatus(ACTIVE)
//            .build());
//
//        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
//            orderRequestBuilder()
//                .withCartId("some cart")
//                .withOrderItem()
//                .build(),
//            orderRequestBuilder()
//                .withCartId("another cart")
//                .withOrderItem(
//                    itemKey(DEFAULT_ITEM_KEY),
//                    dropship()
//                )
//                .build()
//        )
//            .withCoins(fixedCoinKey)
//            .withCoins(deliveryCoinKey)
//            .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
//            .build();
//
//        MultiCartWithBundlesDiscountResponse response = discountServiceUtils.spendDiscount(request);
//
//        assertThat(response.getOrders(), hasItems(
//            allOf(
//                hasProperty("cartId", equalTo("some cart")),
//                hasProperty("items", hasItems(
//                    allOf(
//                        hasItemKey(DEFAULT_ITEM_KEY),
//                        hasProperty("promos", hasSize(1))
//                    )
//                )),
//                hasProperty("deliveries", contains(
//                    hasProperty("promos", contains(
//                        hasSmartShopingPromo(promoDelivery.getPromoKey(), deliveryCoinKey)
//                    ))
//                ))
//            ),
//            allOf(
//                hasProperty("cartId", equalTo("another cart")),
//                hasProperty("items", contains(allOf(
//                    hasItemKey(DEFAULT_ITEM_KEY),
//                    hasProperty("promos", empty())
//                ))),
//                hasProperty("deliveries", not(contains(hasProperty("promos", not(empty())))))
//            )
//        ));
//    }

    @Test
    public void testMaxPromoNominalRuleForPromoCode() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .addPromoRule(RuleType.MAX_PROMO_NOMINAL_FILTER_RULE, RuleParameterName.MAX_PROMO_NOMINAL,
                 Set.of(BigDecimal.valueOf(3)))
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT));

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(100))
                ).build())
                .withCoupon(DEFAULT_COUPON_CODE)
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();
        deferredMetaTransactionService.consumeBatchOfTransactions(1);
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        OrderWithBundlesResponse defaultOrder = response.getOrders().get(0);
        assertThat(defaultOrder.getItems().get(0).getPromos(), hasSize(1));
        assertThat(
                defaultOrder.getItems().get(0).getPromos(),
                contains(
                        allOf(
                                hasProperty("promoType", equalTo(MARKET_COUPON)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(3)))
                        )
                )
        );
    }

    @Test
    public void checkWarningForMaxPromoNominal() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .addPromoRule(RuleType.MAX_PROMO_NOMINAL_FILTER_RULE, RuleParameterName.MAX_PROMO_NOMINAL,
                 Set.of(BigDecimal.valueOf(3)))
                .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT));
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(100))
                ).build())
                .withCoupon(DEFAULT_COUPON_CODE)
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();
        deferredMetaTransactionService.consumeBatchOfTransactions(1);
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);

        assertThat(response.getPromocodesWarnings(),
                contains(PromocodeWarning.of(DEFAULT_COUPON_CODE.toUpperCase(),
                        MarketLoyaltyPromocodeWarningCode.PROMOCODE_MAX_NOMINAL_NOTIFICATION.codeFormat(3))));
    }

    @Test
    public void checkErrorForRefererPromo() {
        Promo accrualPromo = promoManager.createAccrualPromo(PromoUtils.WalletAccrual.defaultModelAccrual()
                .setName(DEFAULT_ACCRUAL_PROMO_KEY)
                .setPromoKey(DEFAULT_ACCRUAL_PROMO_KEY)
                .setStartDate(java.sql.Date.from(clock.instant()))
                .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("REFERRAL_CODE")
                        .addPromoRule(RuleType.MAX_PROMO_NOMINAL_FILTER_RULE, RuleParameterName.MAX_PROMO_NOMINAL,
                         Set.of(BigDecimal.valueOf(3)))
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                        .setBudget(BigDecimal.valueOf(1000))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );

        promoService.setPromoParam(couponPromo.getId(), PromoParameterName.GENERATOR_TYPE,
         PromoCodeGeneratorType.REFERRAL);

        var userReferralPromocode = UserReferralPromocode.builder()
                .setUid(0L)
                .setPromocode("REFERRAL_CODE")
                .setAssignTime(clock.instant())
                .setExpireTime(accrualPromo.getEndDate().toInstant())
                .build();

        userReferralPromocodeDao.insertNewEntry(userReferralPromocode);
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(100))
                ).build())
                .withCoupon("REFERRAL_CODE")
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        deferredMetaTransactionService.consumeBatchOfTransactions(1);
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);
        Set<PromocodeError> promocodeErrors = response.getPromocodeErrors();
        assertFalse(promocodeErrors.isEmpty());
        assertThat(promocodeErrors,
                contains(PromocodeError.of(
                                "REFERRAL_CODE",
                                new CouponError(new MarketLoyaltyError(MarketLoyaltyErrorCode.PROMOCODE_JUST_FOR_FRIENDS))
                        )
                )
        );
    }

    @Test
    public void testRefferalPromocodeOnlyInApp() {
        Promo accrualPromo = promoManager.createAccrualPromo(PromoUtils.WalletAccrual.defaultModelAccrual()
                .setName(DEFAULT_ACCRUAL_PROMO_KEY)
                .setPromoKey(DEFAULT_ACCRUAL_PROMO_KEY)
                .setStartDate(java.sql.Date.from(clock.instant()))
                .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("REFERRAL_CODE")
                        .addPromoRule(RuleType.MAX_PROMO_NOMINAL_FILTER_RULE, RuleParameterName.MAX_PROMO_NOMINAL,
                                Set.of(BigDecimal.valueOf(3)))
                        .addPromoRule(RuleType.CLIENT_PLATFORM_CUTTING_RULE, RuleParameterName.CLIENT_PLATFORM,
                                Set.of(UsageClientDeviceType.APPLICATION))
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                        .setBudget(BigDecimal.valueOf(1000))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );

        promoService.setPromoParam(couponPromo.getId(), PromoParameterName.GENERATOR_TYPE,
                PromoCodeGeneratorType.REFERRAL);

        var userReferralPromocode = UserReferralPromocode.builder()
                .setUid(0L)
                .setPromocode("REFERRAL_CODE")
                .setAssignTime(clock.instant())
                .setExpireTime(accrualPromo.getEndDate().toInstant())
                .build();

        userReferralPromocodeDao.insertNewEntry(userReferralPromocode);
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                        orderRequestBuilder(true).withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(BigDecimal.valueOf(100))
                        ).build())
                .withCoupon("REFERRAL_CODE")
                .withOperationContext(OperationContextFactory
                        .withUidBuilder(1L)
                        .withClientDevice(UsageClientDeviceType.DESKTOP)
                        .buildOperationContext())
                .build();

        deferredMetaTransactionService.consumeBatchOfTransactions(1);
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);
        Set<PromocodeError> promocodeErrors = response.getPromocodeErrors();
        assertFalse(promocodeErrors.isEmpty());
        assertThat(promocodeErrors,
                contains(PromocodeError.of(
                                "REFERRAL_CODE",
                                new CouponError(new MarketLoyaltyError(MarketLoyaltyErrorCode.PROMOCODE_ONLY_IN_APP))
                        )
                )
        );
    }

    @Test
    public void checkErrorForRefererPromoFirstOrderRule() {
        Promo accrualPromo = promoManager.createAccrualPromo(PromoUtils.WalletAccrual.defaultModelAccrual()
                .setName(DEFAULT_ACCRUAL_PROMO_KEY)
                .setPromoKey(DEFAULT_ACCRUAL_PROMO_KEY)
                .setStartDate(java.sql.Date.from(clock.instant()))
                .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("REFERRAL_CODE")
                        .addPromoRule(RuleType.MAX_PROMO_NOMINAL_FILTER_RULE, RuleParameterName.MAX_PROMO_NOMINAL,
                         Set.of(BigDecimal.valueOf(3)))
                        .addPromoRule(RuleType.FIRST_ORDER_CUTTING_RULE)
                        .setCouponValue(BigDecimal.valueOf(5), CoreCouponValueType.PERCENT)
                        .setBudget(BigDecimal.valueOf(1000))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );

        promoService.setPromoParam(couponPromo.getId(), PromoParameterName.GENERATOR_TYPE,
         PromoCodeGeneratorType.REFERRAL);

        var userReferralPromocode = UserReferralPromocode.builder()
                .setUid(0L)
                .setPromocode("REFERRAL_CODE")
                .setAssignTime(clock.instant())
                .setExpireTime(accrualPromo.getEndDate().toInstant())
                .build();

        userReferralPromocodeDao.insertNewEntry(userReferralPromocode);
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(true).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(100))
                ).build())
                .withCoupon("REFERRAL_CODE")
                .withOperationContext(OperationContextFactory.withUidBuilder(234L).buildOperationContext())
                .build();

        deferredMetaTransactionService.consumeBatchOfTransactions(1);
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request);
        Set<PromocodeError> promocodeErrors = response.getPromocodeErrors();
        assertFalse(promocodeErrors.isEmpty());
        assertThat(promocodeErrors,
                contains(PromocodeError.of(
                        "REFERRAL_CODE",
                        new CouponError(new MarketLoyaltyError(MarketLoyaltyErrorCode.ALLOWED_FOR_FIRST_ORDER_ONLY))
                        )
                )
        );
    }

    @Test //MARKETDISCOUNT-8237
    public void checkAccrualNotCreatedAfterSpend() {
        Promo accrualPromo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(300))
                .setName(DEFAULT_ACCRUAL_PROMO_KEY)
                .setPromoKey(DEFAULT_ACCRUAL_PROMO_KEY)
                .setStartDate(java.sql.Date.from(clock.instant()))
                .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_ACCRUAL, accrualPromo.getPromoKey());
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("REFERRAL_CODE")
                        .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
                        .setBudget(BigDecimal.valueOf(1000))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_PROMOCODE, couponPromo.getPromoKey());
        promoService.setPromoParam(couponPromo.getId(), PromoParameterName.GENERATOR_TYPE,
         PromoCodeGeneratorType.REFERRAL);
        deferredMetaTransactionService.consumeBatchOfTransactions(10);
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_MAX_REFERRER_REWARD, 5000);

        var userReferralPromocode = UserReferralPromocode.builder()
                .setUid(0L)
                .setPromocode("REFERRAL_CODE")
                .setAssignTime(clock.instant())
                .setExpireTime(accrualPromo.getEndDate().toInstant())
                .build();

        userReferralPromocodeDao.insertNewEntry(userReferralPromocode);
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                orderRequestBuilder(false).withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(BigDecimal.valueOf(6000))
                ).build())
                .withCoupon("REFERRAL_CODE")
                .withOperationContext(OperationContextFactory.withUidBuilder(1L).buildOperationContext())
                .build();

        deferredMetaTransactionService.consumeBatchOfTransactions(1);
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.spendDiscount(request);

        List<YandexWalletTransaction> referralAccruals = yandexWalletTransactionDao.findByOrderId(
                Long.parseLong(Objects.requireNonNull(response.getOrders().get(0).getOrderId())),
                YandexWalletTransactionStatus.PENDING);
        assertThat(referralAccruals, hasSize(0));
    }

    @Test
    public void testSuccessfulCalculateDiscountAndCheckSecretSalePerk() {
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_START_DATE, LocalDateTime.now(clock).minusDays(1));
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_END_DATE, LocalDateTime.now(clock).plusDays(1));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        MultiCartWithBundlesDiscountResponse orderResponse = discountServiceTestingUtils.calculateDiscounts(
                discountRequest().withCoupon(DEFAULT_COUPON_CODE)
                        .build());

        assertEquals(CartFlag.YANDEX_PLUS_SALE_ENABLE, orderResponse.getYandexPlusSale());

        configurationService.set(ConfigurationService.BRAND_DAY_SALE_START_DATE, LocalDateTime.now(clock).minusDays(2));
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_END_DATE, LocalDateTime.now(clock).minusDays(1));

        orderResponse = discountServiceTestingUtils.calculateDiscounts(
                discountRequest().withCoupon(DEFAULT_COUPON_CODE)
                        .build());

        assertEquals(CartFlag.YANDEX_PLUS_SALE_DISABLE, orderResponse.getYandexPlusSale());
    }

    @Nonnull
    private PromocodeActivationResult createPromocodeFor(long userId) {
        promoManager.createPromocodePromo(
                defaultFixedPromocode()
                        .setCode(PROMOCODE)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now()
                                .plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setAnaplanId(ANAPLAN_ID)
                        .setBusinessId(BUSINESS_ID)
        );

        promoService.reloadActiveSmartShoppingPromoIdsCache();

        return promocodeService.activatePromocodes(PromocodeActivationRequest.builder()
                .userId(userId)
                .externalPromocodes(
                        Set.of(PROMOCODE))
                .build())
                .getActivationResults()
                .get(0);
    }

    private PromoBundleDescription createBundleDescription() {
        return bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(PromoBundleStrategy.GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime()
                        .plusYears(10)),
                primaryItem(FEED_ID, OFFER_1),
                giftItem(FEED_ID, directionalMapping(
                        when(OFFER_1),
                        then(OFFER_2),
                        fixedPrice(100)
                ))
        ));
    }

    private PromoBundleDescription createCheapestAsGiftDescription() {
        return bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                strategy(PromoBundleStrategy.CHEAPEST_AS_GIFT),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                withQuantityInBundle(3),
                item(
                        condition(
                                cheapestAsGift(
                                        FeedSskuSet.of(FEED_ID, List.of(OFFER_1, OFFER_2, OFFER_3)))),
                        quantityInBundle(3),
                        primary()
                )
        ));
    }

    private PromoBundleDescription createBlueSetDescription() {
        return bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                strategy(PromoBundleStrategy.BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(OFFER_1, 30),
                                proportion(OFFER_2, 30)
                        )),
                        primary()
                )
        ));
    }

    @Test
    public void testPickupPromoSuitableFalse() {
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                        orderRequestBuilder(false).withOrderItem().build()
                )
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse response =
         discountServiceTestingUtils.spendDiscountWithExperiments(request,
                 "pickup_segment_flag");

        assertEquals(false, response.getIsPickupPromocodeSuitable());
    }

    @Test
    public void testPickupPromoDisabledRear() {
        configurationService.set(PICKUP_DISABLED_REAR, "disable_pickup_promocode");
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                        orderRequestBuilder(false)
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ZERO),
                                        price(BigDecimal.ZERO)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build()
                )
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse response =
                discountServiceTestingUtils.spendDiscountWithExperiments(request,
                        "pickup_segment_flag;disable_pickup_promocode");

        assertEquals(false, response.getIsPickupPromocodeSuitable());
    }

    @Test
    public void testPickupPromoFlagDisabled() {
        configurationService.set(PICKUP_PROMO_ENABLED, false);
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                        orderRequestBuilder(false)
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ZERO),
                                        price(BigDecimal.ZERO)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build()
                )
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        MultiCartWithBundlesDiscountResponse response =
                discountServiceTestingUtils.spendDiscount(request);

        assertEquals(false, response.getIsPickupPromocodeSuitable());
    }

    @Test
    public void testPickupPromoSuitableTrue() {
        configurationService.set(PICKUP_SEGMENT_REAR, "pickup_segment_flag");
        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(
                        orderRequestBuilder(false)
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ZERO),
                                        price(BigDecimal.ZERO)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build()
                )
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();
        MultiCartWithBundlesDiscountResponse response =
         discountServiceTestingUtils.spendDiscountWithExperiments(request,
                 "pickup_segment_flag");

        assertEquals(true, response.getIsPickupPromocodeSuitable());
    }

    private void setupPickupPromocode() {
        configurationService.set(ConfigurationService.PICKUP_PROMO, PICKUP_PROMO);
        promoService.reloadActiveSmartShoppingPromoIdsCache();

        PromocodeActivationResult activationResult = promocodeService.activatePromocodes(
                        PromocodeActivationRequest.builder()
                                .userId(USER_ID)
                                .externalPromocodes(
                                        Set.of(PICKUP_PROMO))
                                .build())
                .getActivationResults()
                .get(0);

        assertThat(activationResult.getActivationResultCode(), is(PromocodeActivationResultCode.SUCCESS));
    }

    @Test
    public void testPickupPromocodeBrandedPickupNoError() {
        Promo promocodePromo = promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.pickupPromoBrandedPickupStickyFilterRule()
                        .setBudget(BigDecimal.ONE)
                        .setCode(PICKUP_PROMO)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now()
                                .plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setAnaplanId(ANAPLAN_ID)
        );
        setupPickupPromocode();

        MultiCartDiscountRequest request1 = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                        )
                        .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                        .build())
                .withCoupon(PICKUP_PROMO)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request1);
        assertThat(response.getPromocodeErrors(), empty());
    }

    @Test
    public void testPickupPromocodeBrandedPickupError() {
        Promo promocodePromo = promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.pickupPromoBrandedPickupStickyFilterRule()
                        .setBudget(BigDecimal.ONE)
                        .setCode(PICKUP_PROMO)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now()
                                .plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setAnaplanId(ANAPLAN_ID)
        );
        setupPickupPromocode();
        MultiCartDiscountRequest request1 = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                        )
                        .withDeliveries(DeliveryRequestUtils.courierDelivery())
                        .build())
                .withCoupon(PICKUP_PROMO)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request1);
        Set<PromocodeError> errors = response.getPromocodeErrors();
        assertThat(errors, not(empty()));
        assertTrue(errors.stream().anyMatch(e -> e.getError().getError().getCode().equals(MarketLoyaltyErrorCode.NOT_SUITABLE_PICKUP_RULE.name())));
    }

    @Test
    public void testPickupPromocodeInsufficientTotalDeliveryNotSelected() {
        Promo promocodePromo = promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.pickupPromoBrandedPickupAndInsufficientTotalRule()
                        .setBudget(BigDecimal.ONE)
                        .setCode(PICKUP_PROMO)
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now()
                                .plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setAnaplanId(ANAPLAN_ID)
        );
        setupPickupPromocode();
        MultiCartDiscountRequest request1 = DiscountRequestBuilder.builder(orderRequestBuilder(true)
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                price(DEFAULT_COUPON_VALUE.add(BigDecimal.TEN))
                        )
                         .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDeliveryNotSelected())
                        .build())
                .withCoupon(PICKUP_PROMO)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(USER_ID))
                .build();
        MultiCartWithBundlesDiscountResponse response = discountServiceTestingUtils.calculateDiscounts(request1);
        Set<PromocodeError> errors = response.getPromocodeErrors();
        assertThat(errors, not(empty()));
        assertTrue(errors.stream().anyMatch(e -> e.getError().getError().getCode().equals(MIN_ORDER_TOTAL_VIOLATED.name())));
    }

}
