package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.OrderItemResponse;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.util.BundleAdapterUtils;
import ru.yandex.market.loyalty.api.model.cart.CartFlag;
import ru.yandex.market.loyalty.api.model.coin.CoinError;
import ru.yandex.market.loyalty.api.model.coin.OrdersUpdatedCoinsForFront;
import ru.yandex.market.loyalty.api.model.coin.creation.DeviceInfoRequest;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryFeature;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.EmailQueueDao;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.coin.UserInfo;
import ru.yandex.market.loyalty.core.model.email.JobType;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.ExcludedOffersType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinPromoCalculator;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.constants.SupplierFlagRestrictionType;
import ru.yandex.market.loyalty.core.service.mail.AlertNotificationService;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.test.BREMockUtils;
import ru.yandex.market.loyalty.core.test.CheckouterMockUtils;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.CoinRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.OrderResponseUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static java.sql.Timestamp.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.BUDGET_EXCEEDED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.COIN_ALREADY_USED;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.EXPIRED_COIN;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.INVALID_COIN_STATUS;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_SUITABLE_COIN;
import static ru.yandex.market.loyalty.api.model.UsageClientDeviceType.APPLICATION;
import static ru.yandex.market.loyalty.api.model.UsageClientDeviceType.DESKTOP;
import static ru.yandex.market.loyalty.back.controller.discount.DiscountControllerGenerationSupportedTest.createKey;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.USED;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.B2B_USERS;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CLIENT_PLATFORM;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.EXCLUDED_OFFERS_TYPE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_PROMO_NOMINAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.NOT_DBS_SUPPLIER_FLAG_RESTRICTION;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_FLAG_RESTRICTION_TYPE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.CLIENT_PLATFORM_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.DBS_SUPPLIER_FLAG_RESTRICTION_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.EXCLUDED_OFFERS_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.FOR_B2B_USERS_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_PROMO_NOMINAL_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.PURCHASE_BY_LIST_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.SUPPLIER_FLAG_RESTRICTION_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.EXCLUSIONS_CONFIG_ENABLED;
import static ru.yandex.market.loyalty.core.service.discount.ItemPromoCalculation.calculateTotalDiscount;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.SUPPLIER_EXCLUSION_ID;
import static ru.yandex.market.loyalty.core.trigger.restrictions.SetRelation.ALL_FROM_SET_SHOULD_BE_INCLUDED;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.generateWith;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.same;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.DEFAULT_ACTIVATION_TOKEN;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withFeatures;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_QUANTITY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.OrderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.THIRD_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.cpa;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.keyOf;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderItemBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.platform;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.totalDiscount;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.vendor;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_CODE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_VALUE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFreeDelivery;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultPercent;
import static ru.yandex.market.loyalty.core.utils.SequenceCustomizer.compose;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.userSegmentsRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_PHONE;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_PHONE_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_USER_FULL_NAME_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UUID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;
import static ru.yandex.market.loyalty.test.SameCollection.sameCollectionByPropertyValuesAs;

@TestFor(DiscountController.class)
public class DiscountControllerCoinsTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final Logger logger = LogManager.getLogger(DiscountControllerCoinsTest.class);
    public static final String SOURCE_KEY = "1";
    public static final Long OTHER_SUPPLIER_ID = 10390399L;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private YabacksMailer yabacksMailer;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private AlertNotificationService alertNotificationService;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private CheckouterMockUtils checkouterMockUtils;
    @Autowired
    private BREMockUtils breMockUtils;
    @Autowired
    private CoinPromoCalculator coinPromoCalculator;
    @Autowired
    private EmailQueueDao emailQueueDao;
    @Autowired
    private DiscountUtils discountUtils;

    private static final ItemKey OFFER_KEY = ItemKey.ofFeedOffer(368604L, "6230");
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void shouldRoundEndDateToLastSecondOfTheDay() {
        clock.setDate(valueOf("2019-01-01 00:00:00"));

        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setStartDate(valueOf("2019-01-10 00:00:00"))
                        .setEndDate(valueOf("2019-01-17 15:00:00"))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        clock.setDate(valueOf("2019-01-17 23:00:00"));
        assertThat(
                marketLoyaltyClient.calculateDiscount(
                        builder(orderRequestBuilder().withOrderItem().build()).withCoins(coinKey).build()).getCoinErrors(),
                is(empty())
        );

        clock.setDate(valueOf("2019-01-18 00:00:01"));
        assertThat(
                marketLoyaltyClient.calculateDiscount(
                        builder(orderRequestBuilder().withOrderItem().build()).withCoins(coinKey).build()).getCoinErrors(),
                is(not(empty()))
        );
        alertNotificationService.processEmailQueue(100);
    }

    @Test
    public void shouldUseForwardingCoinProps() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(
                        MIN_ORDER_TOTAL_CUTTING_RULE,
                        MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(1000)
                )
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        promoManager.updateCoinPromo(defaultFixed().setId(promo.getId()).addCoinRule(
                MIN_ORDER_TOTAL_CUTTING_RULE,
                MIN_ORDER_TOTAL,
                BigDecimal.valueOf(3000)
        ));

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM coin_props WHERE promo_id = ? ORDER BY id",
                Long.class,
                promo.getId()
        );

        final long newCoinPropsId = ids.get(1);
        final long oldCoinPropsId = ids.get(0);

        jdbcTemplate.update("UPDATE coin_props SET forwarding_coin_props_id = ? WHERE id = ?", newCoinPropsId, oldCoinPropsId);

        coinService.search.invalidateCaches();

        final Coin coin = coinService.search.getCoin(coinKey).orElseThrow();
        final CoinProps coinProps = coinService.search.getCoinProps(coinKey);

        assertEquals(coin.getCoinPropsId(), oldCoinPropsId);
        assertThat(coinProps.getForwardingCoinPropsId(), is(nullValue()));
        assertEquals((long)coinProps.getId(), oldCoinPropsId);
        //noinspection ConstantConditions
        assertEquals(
                coinProps.getRulesContainer()
                        .get(MIN_ORDER_TOTAL_CUTTING_RULE).getParam(MIN_ORDER_TOTAL).orElseThrow(),
                BigDecimal.valueOf(3000)
        );
    }

    @Test
    public void testCalcFixedCoin() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setCanBeRestoredFromReserveBudget(true)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
         comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
    }

    @Test
    public void testCalcFixedCoinWithZeroDiscountByOptionalRule() {
        configurationService.set(EXCLUSIONS_CONFIG_ENABLED, true);
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(PURCHASE_BY_LIST_RULE, SUPPLIER_ID, Set.of((long)SUPPLIER_EXCLUSION_ID))
                        .setCanBeRestoredFromReserveBudget(true)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        supplier(SUPPLIER_EXCLUSION_ID)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                BundleAdapterUtils.adaptFrom(
                        builder(order).withCoins(coinKey)
                                .withOptionalRulesEnabled(true)
                                .build()
                )
        );
        assertEquals(1, discountResponse.getOrders().get(0).getItems().get(0).getPromos().size());
        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    public void testCalcFixedCoinWithOptionalRulesEnabled() {
        configurationService.set(EXCLUSIONS_CONFIG_ENABLED, true);
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        // Правило не должно сработать из-за несовпадения supplierId
                        .addCoinRule(PURCHASE_BY_LIST_RULE, SUPPLIER_ID, Set.of((long)OTHER_SUPPLIER_ID))
                        .setCanBeRestoredFromReserveBudget(true)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        supplier(SUPPLIER_EXCLUSION_ID)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                BundleAdapterUtils.adaptFrom(
                        builder(order).withCoins(coinKey)
                                .withOptionalRulesEnabled(true)
                                .build()
                )
        );
        assertEquals(0, discountResponse.getOrders().get(0).getItems().get(0).getPromos().size());
    }

    @Test
    public void testSpendFixedCoinWithOptionalRulesEnabled() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(PURCHASE_BY_LIST_RULE, SUPPLIER_ID, Set.of((long)SUPPLIER_EXCLUSION_ID))
                        .setCanBeRestoredFromReserveBudget(true)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        supplier(SUPPLIER_EXCLUSION_ID)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                BundleAdapterUtils.adaptFrom(
                        builder(order).withCoins(coinKey).withOptionalRulesEnabled(true).build()
                )
        );
        // Ожидается расчёт скидки без учёта необязательных правил.
        // т.к. в /spend необязательные правила не поддерживаются.
        assertEquals(0, discountResponse.getOrders().get(0).getItems().get(0).getPromos().size());
    }

    @Test
    public void shouldNotCalcFixedCoinWithExcludedSupplier() {
        configurationService.set(EXCLUSIONS_CONFIG_ENABLED, true);
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setCanBeRestoredFromReserveBudget(true)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem(supplier(SUPPLIER_EXCLUSION_ID)).build();
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(discountResponse.getCoinErrors(), not(empty()));
    }

    @Test
    public void shouldReturnExternalIdsOnItemPromo() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100))
                        .setAnaplanId("some anaplan id")
                        .setPromoStorageId("some external id")
                        .setCanBeRestoredFromReserveBudget(true)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        promoService.reloadActiveSmartShoppingPromoIdsCache();

        var order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1000L))
                ).build();

        var discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order).withCoins(coinKey).build()
        );

        assertThat(discountResponse.getCoinErrors(), empty());
        assertThat(discountResponse.getOrders().get(0).getItems(), hasItem(
                hasProperty("promos", hasItem(allOf(
                        hasProperty("shopPromoId", is("some external id")),
                        hasProperty("anaplanId", is("some anaplan id"))
                )))
        ));
    }

    @Test
    public void shouldCalcFixedCoinWithExcludedSupplierIfExcludedCategoriesIgnored() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100)).
                        addCoinRule(EXCLUDED_OFFERS_FILTER_RULE, EXCLUDED_OFFERS_TYPE, ExcludedOffersType.NONE)
        );

        CoinKey coinKey = coinService.create.createCoin(
                smartShoppingPromo,
                defaultAuth().build()
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(supplier(SUPPLIER_EXCLUSION_ID)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(BigDecimal.valueOf(100))
        );
    }

    @Test
    public void shouldFailCalcDeliveryCoinUsed() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        coinDao.updateCoinStatus(coinKey, ACTIVE, USED);

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(discountResponse.getCoinErrors(), hasSize(1));
        assertThat(discountResponse.getCoinErrors(), contains(
                hasProperty("error",
                        hasProperty("code", equalTo(MarketLoyaltyErrorCode.COIN_ALREADY_USED.name()))
                )
        ));
    }

    @Test
    public void testCalcPercentCoinForRoundPrice() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1000L))
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(BigDecimal.valueOf(100))
        );
    }

    @Test
    public void testCalcPercentCoin() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(1234L))
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(BigDecimal.valueOf(124))
        );
    }

    @Test
    public void testCalcTwoPercentCoins() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
        );

        Promo secondPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.valueOf(20))
        );

        CoinKey coinKey1 = coinService.create.createCoin(firstPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(secondPromo, defaultAuth().build());

        BigDecimal price = BigDecimal.valueOf(1000L);
        BigDecimal expectedDiscount = BigDecimal.valueOf(280L);
        assertThat(
                price.subtract(expectedDiscount),
                comparesEqualTo(price.multiply(new BigDecimal("0.9").multiply(new BigDecimal("0.8"))))
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(price)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey1, coinKey2).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(expectedDiscount)
        );
    }

    @Test
    public void testCalcTwoPercentCoinsWithQuantityTwo() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
        );

        Promo secondPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.valueOf(20))
        );

        CoinKey coinKey1 = coinService.create.createCoin(firstPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(secondPromo, defaultAuth().build());

        BigDecimal price = BigDecimal.valueOf(220L);
        BigDecimal expectedDiscount = BigDecimal.valueOf(62L);
        BigDecimal quantity = BigDecimal.valueOf(2);
        assertThat(
                price.subtract(expectedDiscount),
                comparesEqualTo(
                        price
                                .multiply(new BigDecimal("0.9"))
                                .multiply(new BigDecimal("0.8"))
                                .setScale(0, RoundingMode.FLOOR)
                )
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(price),
                        quantity(quantity)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey1, coinKey2).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(expectedDiscount.multiply(quantity))
        );
    }

    @Test
    public void testCalcTwoPercentCoinsAndCoupon() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
        );

        Promo secondPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.valueOf(20))
        );

        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );

        CoinKey coinKey1 = coinService.create.createCoin(firstPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(secondPromo, defaultAuth().build());

        String couponCode = couponService.createOrGetCoupon(
                CouponCreationRequest
                        .builder(createKey(), couponPromo.getId())
                        .forceActivation(true)
                        .build(), discountUtils.getRulesPayload()
        ).getCode();

        BigDecimal price = BigDecimal.valueOf(200L);
        BigDecimal expectedDiscount = BigDecimal.valueOf(328L);


        OrderWithDeliveriesRequest order = orderRequestBuilder()
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
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey1, coinKey2).withCoupon(couponCode).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(expectedDiscount)
        );
    }

    @Test
    public void testCalcPercentAndFixedCoins() {
        Promo percentPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
        );

        BigDecimal fixedCoinNominal = BigDecimal.valueOf(150L);
        Promo fixedPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(fixedCoinNominal)
        );

        CoinKey coinKey1 = coinService.create.createCoin(percentPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(fixedPromo, defaultAuth().build());

        BigDecimal price = BigDecimal.valueOf(1000L);
        BigDecimal expectedDiscount = BigDecimal.valueOf(235L);
        assertThat(
                price.subtract(expectedDiscount),
                comparesEqualTo(price.subtract(fixedCoinNominal).multiply(new BigDecimal("0.9")))
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(price)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey1, coinKey2).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(expectedDiscount)
        );
    }

    @Test
    public void testComparator() {
        Coin coinWithNull1 = PromoUtils.SmartShopping.buildCoin(new CoinKey(1L), null, CoreCoinType.FREE_DELIVERY);
        Coin coinWithZero = PromoUtils.SmartShopping.buildCoin(new CoinKey(2L), BigDecimal.ZERO,
         CoreCoinType.FREE_DELIVERY);
        Coin coinWithNull2 = PromoUtils.SmartShopping.buildCoin(new CoinKey(3L), null, CoreCoinType.FREE_DELIVERY);
        Coin coinWithValue = PromoUtils.SmartShopping.buildCoin(new CoinKey(4L), BigDecimal.ONE, CoreCoinType.FIXED);
        Coin percentCoinWithValue = PromoUtils.SmartShopping.buildCoin(new CoinKey(4L), BigDecimal.ONE,
         CoreCoinType.PERCENT);

        List<Coin> coins = Arrays.asList(percentCoinWithValue, coinWithNull1, coinWithZero, coinWithNull2,
         coinWithValue);
        coins.sort(CoinPromoCalculator.COIN_COMPARATOR);

        assertThat(coins, contains(sameCollectionByPropertyValuesAs(
                Arrays.asList(coinWithValue, percentCoinWithValue, coinWithZero, coinWithNull1, coinWithNull2)
        )));
    }

    /**
     * Real case. MARKETDISCOUNT-712
     */
    @Test
    public void shouldSpendCoinsOnTwoItemsWithPartialApplication() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponValue(BigDecimal.valueOf(500), CoreCouponValueType.FIXED)
        );

        BigDecimal million = BigDecimal.valueOf(1_000_000);
        Promo promo100 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100))
                        .setBudget(million)
                        .setEmissionBudget(million)
        );
        Promo promo1000 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(1000))
                        .setBudget(million)
                        .setEmissionBudget(million)
        );
        Promo promo400 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(400))
                        .setBudget(million)
                        .setEmissionBudget(million)
        );
        Promo promo10 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultPercent(BigDecimal.TEN)
                        .setBudget(million)
                        .setEmissionBudget(million)
                        .addCoinRule(MSKU_FILTER_RULE, MSKU_ID, "100244445801")
        );

        CoinKey coin100_50369 = coinService.create.createCoin(promo100, defaultAuth().build());
        CoinKey coin1000_197376 = coinService.create.createCoin(promo1000, defaultAuth().build());
        CoinKey coin400_197380 = coinService.create.createCoin(promo400, defaultAuth().build());
        CoinKey coin1000_197386 = coinService.create.createCoin(promo1000, defaultAuth().build());
        CoinKey coin400_197390 = coinService.create.createCoin(promo400, defaultAuth().build());
        CoinKey coin1000_197396 = coinService.create.createCoin(promo1000, defaultAuth().build());
        CoinKey coin1000_197407 = coinService.create.createCoin(promo1000, defaultAuth().build());
        CoinKey coin10_197385 = coinService.create.createCoin(promo10, defaultAuth().build());

        BigDecimal firstPrice = BigDecimal.valueOf(3478);
        BigDecimal secondPrice = BigDecimal.valueOf(1533);
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(475690L, "200348026.100131944800")),
                        price(firstPrice)
                )
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(475690L, "200344277.100244445801")),
                        price(secondPrice)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(
                        coin100_50369,
                        coin1000_197376,
                        coin400_197380,
                        coin1000_197386,
                        coin400_197390,
                        coin1000_197396,
                        coin1000_197407,
                        coin10_197385
                ).withCoupon(DEFAULT_COUPON_CODE).build()
        );

        assertThat(
                discountResponse.getOrders().get(0).getItems().stream()
                        .map(OrderRequestUtils::totalDiscount)
                        .collect(ImmutableList.toImmutableList()),
                containsInAnyOrder(
                        comparesEqualTo(firstPrice.subtract(BigDecimal.ONE)),
                        comparesEqualTo(secondPrice.subtract(BigDecimal.ONE))
                )
        );
    }

    @Test
    public void shouldFailCalcFixedCoinOnExceededBudget() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(DEFAULT_COIN_FIXED_NOMINAL)
                        .setBudget(DEFAULT_COIN_FIXED_NOMINAL)
        );
        CoinKey firstCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(firstCoinKey).build()
        );

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(secondCoinKey).build()
        );
        assertThat(discountResponse.getCoinErrors(), contains(coinError(secondCoinKey, BUDGET_EXCEEDED)));
        assertThat(
                totalDiscount(discountResponse.getOrders().get(0).getItems().get(0)), comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    public void testSpendFixedCoin() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
         comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)
        );

        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.USED));
    }

    @Test
    public void shouldFailSpendFixedCoinOnExceededBudget() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(DEFAULT_COIN_FIXED_NOMINAL)
                        .setBudget(DEFAULT_COIN_FIXED_NOMINAL)
        );
        CoinKey firstCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(firstCoinKey).build()
        );

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(secondCoinKey).build()
        );
        assertThat(discountResponse.getCoinErrors(), contains(coinError(secondCoinKey, BUDGET_EXCEEDED)));
    }

    @Test
    public void shouldFailCalcTwoFixedCoinsOnExceededBudget() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(DEFAULT_COIN_FIXED_NOMINAL)
                        .setBudget(DEFAULT_COIN_FIXED_NOMINAL)
        );
        CoinKey firstCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(firstCoinKey, secondCoinKey).build()
        );

        assertThat(discountResponse.getCoinErrors(), contains(coinError(secondCoinKey, BUDGET_EXCEEDED)));
    }

    @Test
    public void shouldFailSpendAlreadyUsedCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(DEFAULT_COIN_FIXED_NOMINAL)
                        .setBudget(DEFAULT_COIN_FIXED_NOMINAL)
        );
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey).build()
        );

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey).build()
        );
        assertThat(discountResponse.getCoinErrors(), contains(coinError(coinKey, COIN_ALREADY_USED)));
    }

    @Test
    public void shouldSpendCoinCreatedOnSameDeviceId() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(
                        DEFAULT_COIN_FIXED_NOMINAL
                )
                        .addCoinRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, APPLICATION)
                        .setBudget(DEFAULT_COIN_FIXED_NOMINAL)
        );

        CoinKey coinKey = coinService.create.createCoin(promo, CoinInsertRequest.noAuthMarketBonus(UserInfo.builder()
                .putAllDeviceId(Collections.singletonMap("deviceId", "1"))
                .setUuid(DEFAULT_UUID)
                .setMuid(DEFAULT_MUID)
                .setEmail(DEFAULT_EMAIL)
                .setPhone(DEFAULT_PHONE)
                .build(), DEFAULT_ACTIVATION_TOKEN)
                .setSourceKey(SOURCE_KEY)
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(ACTIVE).build());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN);

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem()
                .withClientDeviceType(APPLICATION)
                .build();

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey)
                        .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID)
                                .withClientDevice(APPLICATION)
                                .buildOperationContextDto()
                        ).withDeviceInfoRequest(
                        new DeviceInfoRequest(DEFAULT_UUID, Collections.singletonMap("deviceId", "1"), DEFAULT_MUID,
                                DEFAULT_EMAIL, DEFAULT_PHONE, DEFAULT_EMAIL_ID,
                                DEFAULT_USER_FULL_NAME_ID, DEFAULT_PHONE_ID)
                ).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.USED));
    }

    @Test
    public void shouldSpendCoinCreatedWithoutDeviceId() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(
                        DEFAULT_COIN_FIXED_NOMINAL
                )
                        .addCoinRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, APPLICATION)
                        .setBudget(DEFAULT_COIN_FIXED_NOMINAL)
        );

        CoinKey coinKey = coinService.create.createCoin(promo, CoinInsertRequest.noAuthMarketBonus(UserInfo.builder()
                .setUuid(DEFAULT_UUID)
                .setMuid(DEFAULT_MUID)
                .setEmail(DEFAULT_EMAIL)
                .setPhone(DEFAULT_PHONE)
                .build(), DEFAULT_ACTIVATION_TOKEN)
                .setSourceKey(SOURCE_KEY)
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(ACTIVE).build());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN);

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem()
                .withClientDeviceType(APPLICATION)
                .build();

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey)
                        .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID)
                                .withClientDevice(APPLICATION)
                                .buildOperationContextDto()
                        ).withDeviceInfoRequest(
                        new DeviceInfoRequest(DEFAULT_UUID, Collections.singletonMap("deviceId", "1"), DEFAULT_MUID,
                                DEFAULT_EMAIL, DEFAULT_PHONE, DEFAULT_EMAIL_ID,
                                DEFAULT_USER_FULL_NAME_ID, DEFAULT_PHONE_ID)
                ).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.USED));
    }

    @Test
    public void shouldFailSpendCoinCreatedOnAnotherDeviceId() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(
                        DEFAULT_COIN_FIXED_NOMINAL
                )
                        .addCoinRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, APPLICATION)
                        .setBudget(DEFAULT_COIN_FIXED_NOMINAL)
        );

        CoinKey coinKey = coinService.create.createCoin(promo, CoinInsertRequest.noAuthMarketBonus(UserInfo.builder()
                .putAllDeviceId(Collections.singletonMap("deviceId", "1"))
                .setUuid(DEFAULT_UUID)
                .setMuid(DEFAULT_MUID)
                .setEmail(DEFAULT_EMAIL)
                .setPhone(DEFAULT_PHONE)
                .build(), DEFAULT_ACTIVATION_TOKEN)
                .setStatus(ACTIVE)
                .setReason(CoreCoinCreationReason.OTHER)
                .setSourceKey(SOURCE_KEY)
                .build());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN);

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem()
                .withClientDeviceType(APPLICATION)
                .build();

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey)
                        .withDeviceInfoRequest(new DeviceInfoRequest(
                                DEFAULT_UUID, Collections.singletonMap("deviceId", "2"), DEFAULT_MUID, DEFAULT_EMAIL,
                                 DEFAULT_PHONE, DEFAULT_EMAIL_ID, DEFAULT_USER_FULL_NAME_ID, DEFAULT_PHONE_ID
                        ))
                        .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID)
                                .withClientDevice(APPLICATION)
                                .buildOperationContextDto()
                        )
                        .build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(coin.getStatus(), equalTo(ACTIVE));
    }

    @Repeat(5)
    @Test
    public void shouldAllowOnlyOneSpendOfCoinOnParallel() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setBudget(BigDecimal.valueOf(1_000_000))
        );
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        LongAdder counter = new LongAdder();
        testConcurrency(() -> () -> {
            MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                    builder(order).withCoins(coinKey).build()
            );
            if (!discountResponse.getCoinErrors().isEmpty()) {
                assertThat(discountResponse.getCoinErrors(), anyOf(
                        contains(coinError(coinKey, COIN_ALREADY_USED)),
                        contains(coinError(coinKey, INVALID_COIN_STATUS))
                ));
                assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                 comparesEqualTo(BigDecimal.ZERO));
            } else {
                counter.increment();
                assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                 comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
            }
        });

        assertEquals(1, counter.intValue());
        assertThat(promoService.getPromo(promo.getId()).getSpentBudget(), comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
    }

    @Repeat(5)
    @Test
    public void shouldAllowSpendOneCoinIfBudgetExceededInParallel() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(DEFAULT_COIN_FIXED_NOMINAL)
                        .setEmissionBudget(BigDecimal.valueOf(1_000_000))
                        .setBudget(DEFAULT_COIN_FIXED_NOMINAL)
        );

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        LongAdder counter = new LongAdder();
        testConcurrency(() -> {
            CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());
            return () -> {
                MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                        builder(order).withCoins(coinKey).build()
                );
                if (!discountResponse.getCoinErrors().isEmpty()) {
                    assertThat(discountResponse.getCoinErrors(), contains(coinError(coinKey, BUDGET_EXCEEDED)));
                    assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                     comparesEqualTo(BigDecimal.ZERO));
                } else {
                    counter.increment();
                    assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                     comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
                }
            };
        });

        assertEquals(1, counter.intValue());
        assertThat(promoService.getPromo(promo.getId()).getCurrentBudget(), comparesEqualTo(BigDecimal.ZERO));
        assertThat(promoService.getPromo(promo.getId()).getSpentBudget(), comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
    }

    @Test
    public void testSpendFixedCoinAndCoupon() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        Promo couponPromo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        String couponCode = couponService.createOrGetCoupon(
                CouponCreationRequest
                        .builder(createKey(), couponPromo.getId())
                        .forceActivation(true)
                        .build(), discountUtils.getRulesPayload()
        ).getCode();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoupon(couponCode).withCoins(coinKey).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
         comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL.add(DEFAULT_COUPON_VALUE)));

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)
        );

        assertThat(promoService.getPromo(couponPromo.getId()).getSpentBudget(), comparesEqualTo(DEFAULT_COUPON_VALUE));

        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.USED));
    }

    @Test
    public void shouldFailOnNotFoundCoin() {
        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.spendDiscount(
                        builder(order).withCoins(new CoinKey(123L)).build()
                )
        );
        assertEquals(MarketLoyaltyErrorCode.OTHER_ERROR, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void testSpendFixedCoinAndRevert() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey).build()

        );

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)
        );

        assertThat(coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus(),
         equalTo(CoreCoinStatus.USED));

        marketLoyaltyClient.revertDiscount(getDiscountTokens(discountResponse.getOrders().get(0)));

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(BigDecimal.ZERO)
        );

        assertThat(coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus(),
         equalTo(CoreCoinStatus.ACTIVE));
    }

    @Test
    public void shouldNotSpendFixedCoinByNotOwner() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultNoAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withOperationContext(uidOperationContextDto(ANOTHER_UID)).withCoins(coinKey).build()
        );
        assertThat(discountResponse.getCoinErrors(), contains(coinError(coinKey, INVALID_COIN_STATUS)));
        assertThat(discountResponse.getCoins(), contains(allOf(
                hasProperty("id", equalTo(coinKey.getId())),
                hasProperty("activationToken", is(emptyString())))
        ));
    }

    @Test
    public void shouldFilterCoinsByRules() {
        int categoryId = 123141412;
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categoryId)
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        categoryId(categoryId)
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        categoryId(categoryId)
                )
                .withOrderItem(
                        itemKey(THIRD_ITEM_KEY))
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        Map<ItemKey, OrderItemResponse> discountItems =
         OrderResponseUtils.itemsAsMap(discountResponse.getOrders().get(0));

        assertThat(discountItems.get(DEFAULT_ITEM_KEY).getPromos(), contains(
                hasProperty("usedCoin", hasProperty("id", equalTo(coinKey.getId())))
        ));
        assertThat(discountItems.get(ANOTHER_ITEM_KEY).getPromos(), contains(
                hasProperty("usedCoin", hasProperty("id", equalTo(coinKey.getId())))
        ));
        assertThat(discountItems.get(THIRD_ITEM_KEY).getPromos(), is(empty()));
    }

    @Test
    public void shouldFilterCoinsByMinOrderTotal() {
        BigDecimal minOrderTotal = BigDecimal.valueOf(2000);
        BigDecimal chipItemPrice = BigDecimal.valueOf(100);
        BigDecimal expensiveItemPrice = BigDecimal.valueOf(3000);

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, minOrderTotal)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        MultiCartDiscountRequest requestForChipItem = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(chipItemPrice)
                )
                        .build()
        ).withCoins(coinKey).build();

        MultiCartDiscountRequest requestForExpensiveItem = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(expensiveItemPrice)
                )
                        .build()
        ).withCoins(coinKey).build();

        assertThat(
                marketLoyaltyClient.calculateDiscount(requestForExpensiveItem).getCoinErrors(),
                is(empty())
        );

        assertThat(
                marketLoyaltyClient.calculateDiscount(requestForChipItem).getCoinErrors(),
                contains(coinError(coinKey, NOT_SUITABLE_COIN))
        );
    }

    @Test
    public void shouldLimitMaxOrderTotal() {
        BigDecimal maxOrderTotal = BigDecimal.valueOf(2000);
        BigDecimal chipItemPrice = BigDecimal.valueOf(100);
        BigDecimal expensiveItemPrice = BigDecimal.valueOf(3000);

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultPercent(BigDecimal.TEN)
                        .addCoinRule(UPPER_BOUND_DISCOUNT_BASE_RULE, MAX_ORDER_TOTAL, maxOrderTotal)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        MultiCartDiscountRequest requestForChipItem = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(chipItemPrice)
                )
                        .build()
        ).withCoins(coinKey).build();

        MultiCartDiscountRequest requestForExpensiveItem = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(expensiveItemPrice)
                )
                        .build()
        ).withCoins(coinKey).build();

        assertThat(
                marketLoyaltyClient.calculateDiscount(requestForChipItem),
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(empty())
                        ),
                        hasProperty(
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
                                                                                comparesEqualTo(BigDecimal.TEN)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        assertThat(
                marketLoyaltyClient.calculateDiscount(requestForExpensiveItem),
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(empty())
                        ),
                        hasProperty(
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
                                                                                comparesEqualTo(BigDecimal.valueOf(200))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldFailToUseNotApplicableCoin() {
        int categoryId = 123141412;
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categoryId)
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(discountResponse.getCoinErrors(), contains(coinError(coinKey, NOT_SUITABLE_COIN)));
    }

    @Test
    public void shouldCalcPartialAppliedCoin() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        BigDecimal itemPrice = DEFAULT_COIN_FIXED_NOMINAL.divide(BigDecimal.valueOf(2), 0, RoundingMode.CEILING);
        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem(
                itemKey(DEFAULT_ITEM_KEY),
                price(itemPrice)
        ).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
                comparesEqualTo(itemPrice.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldFailToUseExpiredCoin() {
        int daysToExpire = 20;
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setExpiration(ExpirationPolicy.expireByDays(daysToExpire))
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        clock.spendTime(daysToExpire + 1, ChronoUnit.DAYS);

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(discountResponse.getCoinErrors(), contains(coinError(coinKey, EXPIRED_COIN)));
    }

    @Test
    public void shouldDistributeDiscountOfFixedCoin() {
        BigDecimal priceDifference = BigDecimal.valueOf(9);

        BigDecimal coinNominal = BigDecimal.valueOf(100);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(coinNominal)
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        BigDecimal smallPrice = BigDecimal.valueOf(1000);
        BigDecimal bigPrice = smallPrice.multiply(priceDifference);
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(bigPrice)
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        price(smallPrice)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        Map<ItemKey, OrderItemResponse> discountItems =
         OrderResponseUtils.itemsAsMap(discountResponse.getOrders().get(0));

        BigDecimal expectedBigDiscount = BigDecimal.valueOf(90);
        BigDecimal actualDiscount = coinNominal.divide(priceDifference.add(BigDecimal.ONE), RoundingMode.UNNECESSARY);
        assertThat(actualDiscount.multiply(priceDifference), comparesEqualTo(expectedBigDiscount));
        assertThat(discountItems.get(DEFAULT_ITEM_KEY).getPromos(), contains(
                hasProperty("discount", comparesEqualTo(expectedBigDiscount))
        ));

        BigDecimal expectedSmallDiscount = BigDecimal.TEN;
        assertThat(actualDiscount, comparesEqualTo(expectedSmallDiscount));
        assertThat(discountItems.get(ANOTHER_ITEM_KEY).getPromos(), contains(
                hasProperty("discount", comparesEqualTo(expectedSmallDiscount))
        ));
    }

    @Test
    public void shouldUseSameCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey firstCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .withOrderItem(itemKey(ANOTHER_ITEM_KEY))
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(firstCoinKey, secondCoinKey).build()
        );

        Map<ItemKey, OrderItemResponse> discountItems =
         OrderResponseUtils.itemsAsMap(discountResponse.getOrders().get(0));
        assertThat(discountItems.get(DEFAULT_ITEM_KEY).getPromos(), containsInAnyOrder(
                hasProperty("usedCoin", hasProperty("id", equalTo(firstCoinKey.getId()))),
                hasProperty("usedCoin", hasProperty("id", equalTo(secondCoinKey.getId())))
        ));
        assertThat(discountItems.get(ANOTHER_ITEM_KEY).getPromos(), containsInAnyOrder(
                hasProperty("usedCoin", hasProperty("id", equalTo(firstCoinKey.getId()))),
                hasProperty("usedCoin", hasProperty("id", equalTo(secondCoinKey.getId())))
        ));
    }

    @Test
    public void shouldReturnUnusedCoinWithCategories() {
        final int COINS_CNT = 10;
        final int ITEMS_CNT = 10;
        CoinKey[] coins = IntStream.generate(new FibonacciSupplier())
                .limit(COINS_CNT)
                .mapToObj(t -> {
                    BigDecimal coinNominal = BigDecimal.valueOf(t * 100L);
                    SmartShoppingPromoBuilder<?> builder = PromoUtils.SmartShopping.defaultFixed(coinNominal);
                    if (t % 3 == 0) {
                        builder.addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL,
                         BigDecimal.valueOf(t * 1000L));
                    }
                    if (t % 5 == 0) {
                        builder.addCoinRule(MSKU_FILTER_RULE, MSKU_ID, String.valueOf(t));
                    }
                    BigDecimal budget = coinNominal.multiply(BigDecimal.valueOf(2));
                    Promo promo = promoManager.createSmartShoppingPromo(builder
                            .setBudget(budget)
                            .setBudgetThreshold(budget)
                            .setEmissionBudget(budget)
                    );
                    return coinService.create.createCoin(promo, defaultAuth().build());
                })
                .toArray(CoinKey[]::new);

        OrderRequestBuilder orderBuilder = orderRequestBuilder();
        IntStream.generate(new FibonacciSupplier())
                .skip(1)
                .limit(ITEMS_CNT)
                .forEach(t -> orderBuilder.withOrderItem(
                        itemKey(ItemKey.ofFeedOffer((long) t, String.valueOf(t))),
                        price(BigDecimal.valueOf(100)),
                        msku(String.valueOf(t)),
                        categoryId(t == 2 ? t : t / 2)
                ));
        logger.info("Calculation start");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(orderBuilder.build()).withCoins(coins).build()
        );
        logger.info("Calculation end");

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)), greaterThan(BigDecimal.ZERO));
        assertThat(discountResponse.getUnusedCoins(), not(empty()));
    }

    @Test
    public void shouldReturnUnusedCoin() {
        BigDecimal coinNominal = BigDecimal.valueOf(300);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(coinNominal)
        );

        CoinKey firstCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey thirdCoinKey = coinService.create.createCoin(promo, defaultAuth().build());

        //пока в ноль не уходим
        BigDecimal itemPrice = coinNominal.multiply(BigDecimal.valueOf(2)).add(BigDecimal.ONE);
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(itemPrice)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(firstCoinKey, secondCoinKey, thirdCoinKey).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
         comparesEqualTo(coinNominal.multiply(BigDecimal.valueOf(2))));
        assertThat(discountResponse.getUnusedCoins(), contains(
                hasProperty("id", equalTo(Stream.of(firstCoinKey, secondCoinKey, thirdCoinKey)
                        .max(Comparator.comparingLong(CoinKey::getId))
                        .get()
                        .getId()
                ))
        ));
    }

    @Test
    public void shouldReturnWorstUnusedCoin() {
        Promo promo100 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100))
        );
        Promo promo200 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(200))
        );
        Promo promo400 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(400))
        );

        CoinKey coin100 = coinService.create.createCoin(promo100, defaultAuth().build());
        CoinKey coin200 = coinService.create.createCoin(promo200, defaultAuth().build());
        CoinKey coin400 = coinService.create.createCoin(promo400, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(300))
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coin100, coin200, coin400).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
         comparesEqualTo(BigDecimal.valueOf(299)));
        assertThat(discountResponse.getUnusedCoins(), contains(
                hasProperty("id", equalTo(coin400.getId()))
        ));
    }

    @Test
    public void shouldReturnTwoUnusedCoins() {
        Promo promo100 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100))
        );
        Promo promo200 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(200))
        );
        Promo promo300 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(300))
        );

        CoinKey coin100 = coinService.create.createCoin(promo100, defaultAuth().build());
        CoinKey coin200 = coinService.create.createCoin(promo200, defaultAuth().build());
        CoinKey coin300 = coinService.create.createCoin(promo300, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(300))
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coin100, coin200, coin300).build()
        );

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
         comparesEqualTo(BigDecimal.valueOf(299)));
        assertThat(discountResponse.getUnusedCoins(), containsInAnyOrder(
                hasProperty("id", equalTo(coin100.getId())),
                hasProperty("id", equalTo(coin200.getId()))
        ));
    }

    @Test
    public void shouldNotReturnReturnDiscountMoreThanPriceOnCoinsCrossing() {
        BigDecimal coinNominal = BigDecimal.valueOf(300);

        BigDecimal firstItemPrice = BigDecimal.valueOf(400);
        BigDecimal secondItemPrice = BigDecimal.valueOf(450);

        BigDecimal expectedDiscountForFirstItemByFirstCoin = BigDecimal.valueOf(300);
        BigDecimal expectedDiscountForFirstItemBySecondCoin = BigDecimal.valueOf(54);
        BigDecimal expectedDiscountForSecondItemBySecondCoin = BigDecimal.valueOf(246);


        String mskuToFilter = "123123123";
        Promo firstPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(coinNominal).addCoinRule(MSKU_FILTER_RULE, MSKU_ID, mskuToFilter)
        );
        Promo secondPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(coinNominal)
        );

        CoinKey firstCoinKey = coinService.create.createCoin(firstPromo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(secondPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        msku(mskuToFilter),
                        price(firstItemPrice)
                ).withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        quantity(BigDecimal.ONE),
                        price(secondItemPrice)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(firstCoinKey, secondCoinKey).build()
        );

        Map<ItemKey, OrderItemResponse> discountItems =
         OrderResponseUtils.itemsAsMap(discountResponse.getOrders().get(0));

        assertThat(discountItems.get(DEFAULT_ITEM_KEY).getPromos(), containsInAnyOrder(
                allOf(
                        hasProperty("discount", comparesEqualTo(expectedDiscountForFirstItemByFirstCoin)),
                        hasProperty("usedCoin", hasProperty("id", equalTo(firstCoinKey.getId())))
                ),
                allOf(
                        hasProperty("discount", comparesEqualTo(expectedDiscountForFirstItemBySecondCoin)),
                        hasProperty("usedCoin", hasProperty("id", equalTo(secondCoinKey.getId())))
                )
        ));

        assertThat(discountItems.get(ANOTHER_ITEM_KEY).getPromos(), contains(allOf(
                hasProperty("discount", comparesEqualTo(expectedDiscountForSecondItemBySecondCoin)),
                hasProperty("usedCoin", hasProperty("id", equalTo(secondCoinKey.getId())))
        )));

        assertThat(calculateTotalDiscount(discountResponse.getOrders().get(0)),
         comparesEqualTo(coinNominal.multiply(BigDecimal.valueOf(2))));
    }

    @Test
    // TODO by almakarov57: добавить исключение для брендов в OffersFilter MARKETDISCOUNT-6365
    public void shouldExcludeDysonVendor() {
        BigDecimal coinNominal = BigDecimal.valueOf(300);
        BigDecimal firstItemPrice = BigDecimal.valueOf(400);

        Promo firstPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(coinNominal)
        );

        CoinKey coinKey = coinService.create.createCoin(firstPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        vendor(206928L),
                        price(firstItemPrice)
                ).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey).build()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "coinErrors",
                                contains(allOf(
                                        hasProperty("coin", equalTo(new IdObject(coinKey.getId()))),
                                        hasProperty("error",
                                                hasProperty("code", equalTo(NOT_SUITABLE_COIN.name()))
                                        )
                                ))
                        ))
        );
    }

    @Test
    public void shouldSendAlertOnBudgetThreshold() {
        BigDecimal coinNominal = BigDecimal.valueOf(300);
        BigDecimal budgetThreshold = BigDecimal.valueOf(1000);

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(coinNominal)
                        .setBudget(budgetThreshold.add(BigDecimal.valueOf(50)))
                        .setBudgetThreshold(budgetThreshold)
        );

        CoinKey firstCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();
        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(firstCoinKey).build()
        );


        alertNotificationService.processEmailQueue(100);
        verify(yabacksMailer).sendMail(anyString(), isNull(), anyString(), anyString());

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(secondCoinKey).build()
        );

        alertNotificationService.processEmailQueue(100);
        verifyZeroInteractions(yabacksMailer);
    }

    @Test
    public void testSpendBudgetExceededWithReserve() {
        supplementaryDataLoader.createReserveIfNotExists(BigDecimal.valueOf(1_000_000));

        BigDecimal coinNominal = BigDecimal.valueOf(300);

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(coinNominal)
                        .setBudget(coinNominal.multiply(BigDecimal.valueOf(2)).subtract(BigDecimal.valueOf(20)))
                        .setCanBeRestoredFromReserveBudget(true)
                        .setBudgetThreshold(BigDecimal.valueOf(100_000))
        );

        CoinKey firstCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey thirdCoinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 7, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder
                .build();

        MultiCartDiscountResponse orderCalc1 = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(firstCoinKey).build()
        );
        MultiCartDiscountResponse orderCalc2 = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(secondCoinKey).build()
        );

        // достаточно, чтобы потратить 1, но недостаточно, чтобы потратить 2
        assertThat(calculateTotalDiscount(orderCalc1.getOrders().get(0)), lessThan(promo.getCurrentBudget()));
        assertThat(calculateTotalDiscount(orderCalc1.getOrders().get(0)).add(calculateTotalDiscount(orderCalc2.getOrders().get(0))), greaterThan(promo.getCurrentBudget()));

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(firstCoinKey).build()
        );
        budgetService.awaitExecutor();
        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(secondCoinKey).build()
        );
        budgetService.awaitExecutor();
        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(thirdCoinKey).build()
        );
        budgetService.awaitExecutor();
        assertThat(promoService.getPromo(promo.getId()).getCurrentBudget(), greaterThan(BigDecimal.ZERO));
        alertNotificationService.processEmailQueue(100);
        verify(yabacksMailer).sendMail(anyString(), isNull(), anyString(), anyString());
    }

    @Test
    public void testSpendBudgetExceededWithInsufficientReserve() throws InterruptedException {
        supplementaryDataLoader.createReserveIfNotExists(BigDecimal.valueOf(100));

        BigDecimal coinNominal = BigDecimal.valueOf(300);

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(coinNominal)
                        .setBudget(coinNominal.multiply(BigDecimal.valueOf(2)).subtract(BigDecimal.valueOf(20)))
                        .setCanBeRestoredFromReserveBudget(true)
                        .setBudgetThreshold(BigDecimal.valueOf(100_000))
        );

        CoinKey firstCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey thirdCoinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
        generateWith(same(orderItemBuilder(
                quantity(DEFAULT_QUANTITY),
                price(BigDecimal.valueOf(1000))
        )), 7, compose(keyOf(), OrderRequestUtils::itemKey))
                .forEach(orderRequestBuilder::withOrderItem);
        OrderWithDeliveriesRequest order = orderRequestBuilder
                .build();

        MultiCartDiscountResponse orderCalc1 = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(firstCoinKey).build()
        );
        MultiCartDiscountResponse orderCalc2 = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(secondCoinKey).build()
        );

        // достаточно, чтобы потратить 1, но недостаточно, чтобы потратить 2
        assertThat(calculateTotalDiscount(orderCalc1.getOrders().get(0)), lessThan(promo.getCurrentBudget()));
        assertThat(calculateTotalDiscount(orderCalc1.getOrders().get(0)).add(calculateTotalDiscount(orderCalc2.getOrders().get(0))), greaterThan(promo.getCurrentBudget()));

        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(firstCoinKey).build()
        );
        assertThat(promoService.getPromo(promo.getId()).getCurrentBudget(), greaterThan(BigDecimal.ZERO));
        marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(secondCoinKey).build()
        );
        assertThat(promoService.getPromo(promo.getId()).getCurrentBudget(), lessThan(BigDecimal.ZERO));

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(thirdCoinKey).build()
        );
        assertThat(discountResponse.getCoinErrors(), contains(coinError(thirdCoinKey,
         MarketLoyaltyErrorCode.BUDGET_EXCEEDED)));
        budgetService.waitForReserveBudget();
        alertNotificationService.processEmailQueue(100);
        verify(yabacksMailer).sendMail(anyString(), isNull(), eq("Ошибка списания с резервного бюджета"), anyString());
    }

    @Test
    public void coinShouldNotApplyIfClientDeviceNotMatch() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, APPLICATION)
        );
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem()
                .build();

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(order)
                .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID)
                        .withClientDevice(DESKTOP)
                        .buildOperationContext())
                .withCoins(coinKey)
                .build();


        final MultiCartDiscountResponse calcResponse = marketLoyaltyClient.calculateDiscount(request);
        assertThat(
                calcResponse,
                hasProperty(
                        "coinErrors",
                        contains(allOf(
                                hasProperty("coin", equalTo(new IdObject(coinKey.getId()))),
                                hasProperty("error", hasProperty(
                                        "code", equalTo(MarketLoyaltyErrorCode.INVALID_CLIENT_DEVICE.name())
                                        )
                                )
                        ))
                )
        );

        final MultiCartDiscountResponse spendResponse = marketLoyaltyClient.spendDiscount(request);
        assertThat(spendResponse,
                hasProperty("coinErrors",
                        contains(allOf(
                                hasProperty("coin", equalTo(new IdObject(coinKey.getId()))),
                                hasProperty("error", hasProperty(
                                        "code", equalTo(MarketLoyaltyErrorCode.INVALID_CLIENT_DEVICE.name())
                                        )
                                )
                        ))
                )
        );
    }

    @Test
    public void coinShouldNotApplyIfClientDeviceMatch() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(CLIENT_PLATFORM_CUTTING_RULE, CLIENT_PLATFORM, APPLICATION)
        );
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem()
                .build();

        MultiCartDiscountRequest request = DiscountRequestBuilder.builder(order)
                .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID)
                        .withClientDevice(APPLICATION)
                        .buildOperationContext())
                .withCoins(coinKey)
                .build();


        final MultiCartDiscountResponse calcResponse = marketLoyaltyClient.calculateDiscount(request);
        assertThat(
                calcResponse,
                hasProperty(
                        "coinErrors",
                        empty()
                )
        );

        final MultiCartDiscountResponse spendResponse = marketLoyaltyClient.spendDiscount(request);
        assertThat(spendResponse,
                hasProperty("coinErrors",
                        empty()
                )
        );
    }

    @Test
    public void shouldEmitCoinIfUserSegmentRestrictionMatches() {
        checkouterMockUtils.mockCheckoutGetOrdersResponse(
                CheckouterUtils.DEFAULT_ORDER_ID,
                DEFAULT_UID,
                OrderStatus.PROCESSING
        );

        breMockUtils.mockBREGetUserSegmentsResponse(
                "segment1"
        );

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                userSegmentsRestriction(
                        ALL_FROM_SET_SHOULD_BE_INCLUDED,
                        "segment1"
                )
        );

        OrdersUpdatedCoinsForFront coinsForFront = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(
                        CheckouterUtils.defaultStatusUpdatedRequest(CheckouterUtils.DEFAULT_ORDER_ID)
                );

        assertThat(
                coinsForFront,
                hasProperty("newCoins", hasSize(1))
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldNotFailOnSegmentatorError() {
        checkouterMockUtils.mockCheckoutGetOrdersResponse(
                CheckouterUtils.DEFAULT_ORDER_ID,
                DEFAULT_UID,
                OrderStatus.DELIVERED
        );

        breMockUtils.mockBREGetUserSegmentsError();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                userSegmentsRestriction(
                        ALL_FROM_SET_SHOULD_BE_INCLUDED,
                        "segment1"
                )
        );

        OrdersUpdatedCoinsForFront coinsForFront = marketLoyaltyClient
                .sendOrderStatusUpdatedEvent(
                        CheckouterUtils.defaultStatusUpdatedRequest(CheckouterUtils.DEFAULT_ORDER_ID)
                );

        assertThat(
                coinsForFront,
                hasProperty("newCoins", is(empty()))
        );
    }

    @Test
    public void shouldPutBudgetExceededNotificationOnCalc() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setBudget(BigDecimal.ZERO)
        );
        CoinKey coinKey = coinService.create.createCoin(promo, CoinRequestUtils.defaultAuth(DEFAULT_UID).build());

        MultiCartDiscountRequest request = DiscountRequestBuilder
                .builder(
                        orderRequestBuilder().withOrderItem().build()
                )
                .withCoins(coinKey)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                .build();

        marketLoyaltyClient.calculateDiscount(request);

        coinPromoCalculator.awaitExecutor();

        assertThat(emailQueueDao.fetchReadyJobs(5), contains(
                hasProperty("jobType", equalTo(JobType.BUDGET_EXCEEDED))
        ));
    }

    @Test
    public void shouldPutBudgetExceededNotificationOnCalcOnlyOnce() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setBudget(BigDecimal.ZERO)
        );
        CoinKey coinKey = coinService.create.createCoin(promo, CoinRequestUtils.defaultAuth(DEFAULT_UID).build());

        MultiCartDiscountRequest request = DiscountRequestBuilder
                .builder(
                        orderRequestBuilder().withOrderItem().build()
                )
                .withCoins(coinKey)
                .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                .build();

        marketLoyaltyClient.calculateDiscount(request);

        clock.spendTime(1, ChronoUnit.SECONDS);

        marketLoyaltyClient.calculateDiscount(request);

        coinPromoCalculator.awaitExecutor();

        assertThat(emailQueueDao.fetchReadyJobs(5), hasSize(1));
    }

    private static Matcher<CoinError> coinError(CoinKey coinKey, MarketLoyaltyErrorCode errorCode) {
        return allOf(
                hasProperty("coin", hasProperty("id", equalTo(coinKey.getId()))),
                hasProperty("error", hasProperty("code", equalTo(errorCode.name())))
        );
    }

    @Test
    public void shouldThrowExceptionInCaseOfTooManyCoins() {
        int tooBigAmountOfCoins = 101;
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(100))
                        .setEmissionBudget(BigDecimal.valueOf(tooBigAmountOfCoins))
        );

        Set<CoinKey> coinKeySet = new HashSet<>();
        for (int i = 0; i < tooBigAmountOfCoins; i++) {
            coinKeySet.add(coinService.create.createCoin(promo, defaultAuth().build()));
        }

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(OFFER_KEY),
                        quantity(BigDecimal.valueOf(2)),
                        price(150)
                ).build();
        //MethodArgumentNotValidException is expected
        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order).withCoins(coinKeySet).build()
                )
        ).getModel();
        assertEquals(MarketLoyaltyErrorCode.OTHER_ERROR.name(), error.getCode());
        assertTrue(error.getMessage().contains("size must be between"));
    }

    @Test
    public void fibTest() {
        final int LIMIT = 20;
        int[] fibs = IntStream.generate(new FibonacciSupplier())
                .limit(LIMIT)
                .toArray();
        assertEquals(1, fibs[0]);
        assertEquals(1, fibs[1]);

        for (int i = 2; i < LIMIT; ++i) {
            assertEquals(fibs[i - 2] + fibs[i - 1], fibs[i]);
        }
    }


    @Test
    public void testSuccessfulCalculateDiscountAndCheckSecretSalePerkWithForceRearrFlag() {
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_START_DATE, LocalDateTime.now(clock).plusDays(1));
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_END_DATE, LocalDateTime.now(clock).plusDays(2));
        configurationService.set(ConfigurationService.BRAND_DAY_SALE_FORCE_DATES_REARR, "market_force_secret_sale=1");
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-Market-Rearrfactors", "market_force_secret_sale=1;new_spread_algorithm");
        MultiCartWithBundlesDiscountResponse orderResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build(),
                httpHeaders
        );

        assertEquals(CartFlag.YANDEX_PLUS_SALE_ENABLE, orderResponse.getYandexPlusSale());
    }

    private static class FibonacciSupplier implements IntSupplier {
        int prev1 = 0;
        int prev2 = 0;

        @Override
        public int getAsInt() {
            if (prev2 == 0) {
                if (prev1 == 0) {
                    prev1 = 1;
                    return 1;
                }
                prev2 = 1;
                return 1;
            }
            int result = prev2 + prev1;
            prev2 = prev1;
            prev1 = result;
            return result;
        }
    }


    @Test
    public void coinInformationDoesNotBelongToTheUserShouldNotBeReturned() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKeyFirst = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        CoinKey coinKeySecond = coinService.create.createCoin(smartShoppingPromo, defaultAuth(345L).build());

        MultiCartDiscountRequest requestForChipItem = builder(
                orderRequestBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY)
                        ).build()
        )
                .withOperationContext(
                        OperationContextFactory.withUidBuilder(DEFAULT_UID)
                                .buildOperationContextDto())
                .withCoins(coinKeyFirst, coinKeySecond)
                .build();
        MultiCartDiscountResponse multiCartDiscountResponse = marketLoyaltyClient.calculateDiscount(requestForChipItem);
        assertEquals(1, multiCartDiscountResponse.getCoins().size());
    }

    @Test
    public void shouldLimitMaxPromoNominal() {
        BigDecimal limitMaxPromoNominal = BigDecimal.valueOf(2000);
        BigDecimal chipItemPrice = BigDecimal.valueOf(100);
        BigDecimal expensiveItemPrice = BigDecimal.valueOf(50000);

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultPercent(BigDecimal.TEN)
                        .setBudget(BigDecimal.valueOf(100000))
                        .setCanBeRestoredFromReserveBudget(true)
                        .setBudgetThreshold(BigDecimal.valueOf(100000))
                        .addCoinRule(MAX_PROMO_NOMINAL_FILTER_RULE, MAX_PROMO_NOMINAL, limitMaxPromoNominal)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        MultiCartDiscountRequest requestForChipItem = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(chipItemPrice)
                )
                        .build()
        ).withCoins(coinKey).build();

        MultiCartDiscountRequest requestForExpensiveItem = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(expensiveItemPrice)
                )
                        .build()
        ).withCoins(coinKey).build();

        assertThat(
                marketLoyaltyClient.calculateDiscount(requestForChipItem),
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(empty())
                        ),
                        hasProperty(
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
                                                                                comparesEqualTo(BigDecimal.TEN)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        assertThat(
                marketLoyaltyClient.calculateDiscount(requestForExpensiveItem),
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(empty())
                        ),
                        hasProperty(
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
                                                                                comparesEqualTo(limitMaxPromoNominal)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldFilterExpressDeliveryWhenPromoForEveryThingExceptExpress() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultPercent(BigDecimal.TEN)
                        .addCoinRule(SUPPLIER_FLAG_RESTRICTION_FILTER_RULE, SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EVERYTHING_EXCEPT_EXPRESS)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );

        String couponCode = couponService.createOrGetCoupon(
                CouponCreationRequest
                        .builder(createKey(), couponPromo.getId())
                        .forceActivation(true)
                        .build(), discountUtils.getRulesPayload()
        ).getCode();

        MultiCartDiscountRequest request = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(400))
                ).withDeliveries(courierDelivery(
                        withFeatures(Set.of(DeliveryFeature.EXPRESS)),
                        withPrice(BigDecimal.valueOf(350))))
                        .build()
        ).withCoins(coinKey).withCoupon(couponCode).build();

        assertThat(
                marketLoyaltyClient.calculateDiscount(request),
                allOf(
                        hasProperty(
                                "coinErrors",
                                contains(allOf(
                                        hasProperty("coin", equalTo(new IdObject(coinKey.getId()))),
                                        hasProperty("error",
                                                hasProperty("code", equalTo(NOT_SUITABLE_COIN.name()))
                                        )
                                ))
                        ))
        );
    }

    @Test
    public void shouldFilterNotExpressDeliveryWhenPromoForExpress() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultPercent(BigDecimal.TEN)
                        .addCoinRule(SUPPLIER_FLAG_RESTRICTION_FILTER_RULE, SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );

        String couponCode = couponService.createOrGetCoupon(
                CouponCreationRequest
                        .builder(createKey(), couponPromo.getId())
                        .forceActivation(true)
                        .build(), discountUtils.getRulesPayload()
        ).getCode();

        MultiCartDiscountRequest request = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(400))
                ).withDeliveries(courierDelivery(
                        withPrice(BigDecimal.valueOf(350))))
                        .build()
        ).withCoins(coinKey).withCoupon(couponCode).build();

        assertThat(
                marketLoyaltyClient.calculateDiscount(request),
                allOf(
                        hasProperty(
                                "coinErrors",
                                contains(allOf(
                                        hasProperty("coin", equalTo(new IdObject(coinKey.getId()))),
                                        hasProperty("error",
                                                hasProperty("code", equalTo(NOT_SUITABLE_COIN.name()))
                                        )
                                ))
                        ))
        );
    }

    @Test
    public void shouldCalcFreeDeliveryForExpressCoinOnly() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
                        .addCoinRule(SUPPLIER_FLAG_RESTRICTION_FILTER_RULE, SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withCartId("first")
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(BigDecimal.valueOf(400))
                                ).withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350))))
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withCartId("second")
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(BigDecimal.valueOf(400))
                                ).withDeliveries(courierDelivery(
                                        withFeatures(Set.of(DeliveryFeature.EXPRESS)),
                                        withPrice(BigDecimal.valueOf(350))))
                                .build()
                )
                .withCoins(coinKey)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(request);
        assertThat(discountResponse.getOrders(), hasSize(2));
        assertThat(discountResponse.getOrders(), hasItems(
                allOf(
                        hasProperty("cartId", is("first")),
                        hasProperty("deliveries", hasItem(
                                hasProperty("promos", empty())
                        ))
                ),
                allOf(
                        hasProperty("cartId", is("second")),
                        hasProperty("deliveries", hasItem(
                                hasProperty("promos", hasItem(allOf(
                                        hasProperty("usedCoin",
                                                hasProperty("id", equalTo(coinKey.getId()))
                                        ),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(350)))
                                )))
                        ))
                )
        ));
        assertThat(discountResponse.getCoinErrors(), empty());
    }

    @Test
    public void shouldNotApplyFreeDeliveryForExpressCoinOnly() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultFreeDelivery()
                        .addCoinRule(SUPPLIER_FLAG_RESTRICTION_FILTER_RULE, SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EXPRESS_WAREHOUSE)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(BigDecimal.valueOf(400))
                                ).withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350))))
                                .build()
                )
                .withCoins(coinKey)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(request);
        assertThat(discountResponse.getOrders(), hasSize(1));
        assertThat(discountResponse.getOrders(), hasItem(
                allOf(
                        hasProperty("deliveries", hasItem(
                                hasProperty("promos", empty())
                        ))
                )
        ));
        assertThat(discountResponse.getCoinErrors(), contains(coinError(coinKey, NOT_SUITABLE_COIN)));
    }

    @Test
    public void shouldNotFilterNotExpressDeliveryWhenPromoForExpress() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultPercent(BigDecimal.TEN)
                        .addCoinRule(SUPPLIER_FLAG_RESTRICTION_FILTER_RULE, SUPPLIER_FLAG_RESTRICTION_TYPE,
                                SupplierFlagRestrictionType.EVERYTHING)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );

        String couponCode = couponService.createOrGetCoupon(
                CouponCreationRequest
                        .builder(createKey(), couponPromo.getId())
                        .forceActivation(true)
                        .build(), discountUtils.getRulesPayload()
        ).getCode();

        MultiCartDiscountRequest request = builder(
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(400))
                ).withDeliveries(courierDelivery(
                        withPrice(BigDecimal.valueOf(350))))
                        .build(),
                orderRequestBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(BigDecimal.valueOf(400))
                ).withDeliveries(courierDelivery(
                        withFeatures(Set.of(DeliveryFeature.EXPRESS)),
                        withPrice(BigDecimal.valueOf(350))))
                        .build()
        ).withCoins(coinKey).withCoupon(couponCode).build();

        assertThat(
                marketLoyaltyClient.calculateDiscount(request),
                allOf(hasProperty("coinErrors", is(empty())))
        );
    }

    @Test
    public void shouldFilterDbsOffersWithNoDbsRule() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultPercent(BigDecimal.TEN)
                        .addCoinRule(DBS_SUPPLIER_FLAG_RESTRICTION_FILTER_RULE,
                                NOT_DBS_SUPPLIER_FLAG_RESTRICTION, true)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(100),
                        platform(MarketPlatform.WHITE),
                        cpa("real")
                )
                .build();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(order)
                .withCoins(coinKey)
                .build();

        MultiCartWithBundlesDiscountResponse response = marketLoyaltyClient.calculateDiscount(request);

        assertThat(response.getCoinErrors(), not(empty()));
        assertThat(response.getCoinErrors().get(0).getError().getCode(), is(NOT_SUITABLE_COIN.name()));
    }

    @Test
    public void shouldNotFilterNotDbsOffersWithNoDbsRule() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                defaultPercent(BigDecimal.TEN)
                        .addCoinRule(DBS_SUPPLIER_FLAG_RESTRICTION_FILTER_RULE,
                                NOT_DBS_SUPPLIER_FLAG_RESTRICTION, true)
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        price(100),
                        platform(MarketPlatform.BLUE)
                )
                .build();

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(order)
                .withCoins(coinKey)
                .build();

        MultiCartWithBundlesDiscountResponse response = marketLoyaltyClient.calculateDiscount(request);

        assertThat(response.getCoinErrors(), empty());
    }

    @Test
    public void shouldApplyB2BCoinToB2BUser() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(FOR_B2B_USERS_CUTTING_RULE, B2B_USERS, true)  // B2B promo
        );
        configurationService.set(ConfigurationService.ENABLE_B2B_CUTTING_RULE, true);  // enabled rule

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(1000)
                )
                .build();

        OperationContextDto operationContext = OperationContextFactory.withUidBuilder(DEFAULT_UID)
                .buildOperationContextDto();
        operationContext.setIsB2B(true);  // B2B user

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withOperationContext(operationContext)
                        .withCoins(coinKey).build()
        );

        assertFalse(discountResponse.getCoins().isEmpty());
        assertTrue(discountResponse.getCoinErrors().isEmpty());
        assertTrue(discountResponse.getUnusedCoins().isEmpty());
    }

    @Test
    public void shouldNotApplyB2BCoinToNotB2BUser() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(FOR_B2B_USERS_CUTTING_RULE, B2B_USERS, true)  // B2B promo
        );
        configurationService.set(ConfigurationService.ENABLE_B2B_CUTTING_RULE, true);  // enabled rule

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(1000)
                )
                .build();

        OperationContextDto operationContext = OperationContextFactory.withUidBuilder(DEFAULT_UID)
                .buildOperationContextDto();
        operationContext.setIsB2B(false);  // not B2B user

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withOperationContext(operationContext)
                        .withCoins(coinKey).build()
        );

        assertFalse(discountResponse.getCoins().isEmpty());
        assertFalse(discountResponse.getCoinErrors().isEmpty());
        assertEquals(
                MarketLoyaltyErrorCode.USER_IS_NOT_B2B.createError().getCode(),
                discountResponse.getCoinErrors().get(0).getError().getCode());
        assertTrue(discountResponse.getUnusedCoins().isEmpty());
    }

    @Test
    public void shouldNotApplyNotB2BCoinToB2BUser() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        configurationService.set(ConfigurationService.ENABLE_B2B_CUTTING_RULE, true);  // enabled rule

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(1000)
                )
                .build();

        OperationContextDto operationContext = OperationContextFactory.withUidBuilder(DEFAULT_UID)
                .buildOperationContextDto();
        operationContext.setIsB2B(true);  // B2B user

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withOperationContext(operationContext)
                        .withCoins(coinKey).build()
        );

        assertFalse(discountResponse.getCoins().isEmpty());
        assertFalse(discountResponse.getCoinErrors().isEmpty());
        assertEquals(
                MarketLoyaltyErrorCode.PROMO_IS_NOT_B2B.createError().getCode(),
                discountResponse.getCoinErrors().get(0).getError().getCode());
        assertTrue(discountResponse.getUnusedCoins().isEmpty());
    }

    @Test
    public void shouldNotWorkOnNotB2BCoinToNotB2BUser() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        configurationService.set(ConfigurationService.ENABLE_B2B_CUTTING_RULE, true); // enabled rule

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(1000)
                )
                .build();

        OperationContextDto operationContext = OperationContextFactory.withUidBuilder(DEFAULT_UID)
                .buildOperationContextDto();
        operationContext.setIsB2B(false);  // not B2B user

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withOperationContext(operationContext)
                        .withCoins(coinKey).build()
        );

        assertFalse(discountResponse.getCoins().isEmpty());
        assertTrue(discountResponse.getCoinErrors().isEmpty());
        assertTrue(discountResponse.getUnusedCoins().isEmpty());
    }

    @Test
    public void shouldNotWorkIfB2bRuleDisabled() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        configurationService.set(ConfigurationService.ENABLE_B2B_CUTTING_RULE, false);  // disabled rule

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(1000)
                )
                .build();

        OperationContextDto operationContext = OperationContextFactory.withUidBuilder(DEFAULT_UID)
                .buildOperationContextDto();
        operationContext.setIsB2B(true);  // B2B user

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withOperationContext(operationContext)
                        .withCoins(coinKey).build()
        );

        assertFalse(discountResponse.getCoins().isEmpty());
        assertTrue(discountResponse.getCoinErrors().isEmpty());
        assertTrue(discountResponse.getUnusedCoins().isEmpty());
    }
}
