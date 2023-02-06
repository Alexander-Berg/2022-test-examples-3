package ru.yandex.market.loyalty.back.controller.discount;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.coin.CoinsForCart;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryFeature;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryPromoResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryResponse;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderItemsRequest;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponse;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;
import ru.yandex.market.loyalty.api.model.web.LoyaltyTag;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackRegionSettingsTest;
import ru.yandex.market.loyalty.core.model.PropertyStateType;
import ru.yandex.market.loyalty.core.model.RegionSettings;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.constants.DeliveryPartnerType;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.RegionSettingsUtils;
import ru.yandex.market.loyalty.core.utils.ReportMockUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static java.math.BigDecimal.ZERO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.loyalty.api.model.PromoType.FREE_DELIVERY_ADDRESS;
import static ru.yandex.market.loyalty.api.model.PromoType.FREE_DELIVERY_THRESHOLD;
import static ru.yandex.market.loyalty.api.model.PromoType.MULTICART_DISCOUNT;
import static ru.yandex.market.loyalty.api.model.coin.CoinApplicationRestrictionType.FILTER_RULE_RESTRICTION;
import static ru.yandex.market.loyalty.api.model.coin.CoinApplicationRestrictionType.FREE_DELIVERY_THRESHOLD_RESTRICTION;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.ALREADY_ZERO_PRICE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.COIN_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.COIN_THRESHOLD_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.DROPSHIP_ONLY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.LARGE_SIZED_CART;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.REGION_WITHOUT_THRESHOLD;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.THRESHOLD_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.YA_PLUS_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.ALREADY_FREE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.NO_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.NOT_LOGIC;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.REGION_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.DELIVERY_REGION_CART_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.AMURSKI_DISTRICT;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.FAR_EASTERN_FEDERAL_DISTRICT;
import static ru.yandex.market.loyalty.core.service.discount.delivery.DeliveryDiscountCalculator.RUSSIA;
import static ru.yandex.market.loyalty.core.test.BlackboxUtils.mockBlackbox;
import static ru.yandex.market.loyalty.core.test.RegionSettingsLoader.YANDEX_PLUS_TEST_THRESHOLD;
import static ru.yandex.market.loyalty.core.test.RegionSettingsLoader.YANDEX_PLUS_TEST_THRESHOLD_REGION;
import static ru.yandex.market.loyalty.core.test.RegionSettingsLoader.YANDEX_PLUS_TEST_THRESHOLD_REGION_WO_DEFAULT_THRESHOLD;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.ALCOHOL_CATEGORY;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.ALCOHOL_CHILD_CATEGORY;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PHARMA_CHILD_CATEGORY;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PHARMA_LIST_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.STICK_CATEGORY;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.SUPPLIER_EXCLUSION_ID;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.ANOTHER_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.COURIER_DELIVERY_ID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.DEFAULT_DELIVERY_PRICE;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.DEFAULT_DELIVERY_PRICE_HALF;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.DEFAULT_DELIVERY_REGION;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.PICKUP_DELIVERY_ID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.POST_DELIVERY_ID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.SELECTED;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.ZERO_PRICE;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.freePickupDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.pickupDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.postDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withFeatures;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withFreeDeliveryAddress;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withId;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withRegion;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.DEFAULT_REGION;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.THIRD_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.atSupplierWarehouse;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.downloadable;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.dropship;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.excludedAlcoholCategoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.excludedStickCategoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ofSpecs;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.specs;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFreeDelivery;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFreeDeliveryWithMinOrderTotalRule;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFreeDeliveryWithNoDbsRule;
import static ru.yandex.market.loyalty.core.utils.RegionSettingsUtils.DEFAULT_THRESHOLD;
import static ru.yandex.market.loyalty.core.utils.ReportMockUtils.makeDefaultItem;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(DiscountController.class)
public class DiscountControllerDeliveryDiscountCalculationTest extends MarketLoyaltyBackRegionSettingsTest {
    private static final long MOSCOW_REGION = 213L;
    private static final long KERCH_REGION = 11464L;
    static final long VORONEZH_REGION_LONG = 193L;
    static final int VORONEZH_REGION_INT = 193;
    static final int CHITA_REGION = 68;
    private static final int DEFAULT_ITEM_PRICE = 200;
    private static final String ANOTHER_PROMO_KEY = "another promo";

    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoUtils promoUtils;
    @Autowired
    private ReportMockUtils reportMockUtils;
    @Autowired
    private PromoService promoService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private RegionSettingsService regionSettingsService;
    @Autowired
    private RegionSettingsUtils regionSettingsUtils;
    @Autowired
    private RegionService regionService;

    @Before
    public void setUp() throws Exception {
        configurationService.set(ConfigurationService.DELIVERY_EXTRA_CHARGE_CALCULATION_ENABLED, true);
        configurationService.set(ConfigurationService.CONSIDER_EXTRA_CHARGE_IN_DELIVERY_ZERO_PRICE_CALCULATION, true);
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnUnusedCoinIfDeliveryCoinRegionMismatch() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .addCoinRule(
                                RuleType.DELIVERY_REGION_CART_CUTTING_RULE,
                                RuleParameterName.REGION_ID,
                                Collections.singleton(121045)
                        )
        );
        Coin deliveryCoin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(courierDelivery(withRegion(213L)))
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(deliveryCoin.getCoinKey())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty("freeDeliveryReason", equalTo(THRESHOLD_FREE_DELIVERY)),
                        hasProperty("freeDeliveryStatus", equalTo(WILL_BE_FREE_WITH_MORE_ITEMS)),
                        hasProperty("priceLeftForFreeDelivery", comparesEqualTo(BigDecimal.valueOf(1499))),
                        hasProperty("threshold", comparesEqualTo(DEFAULT_THRESHOLD))
                )
        );

        assertThat(discountResponse.getOrders().get(0).getDeliveries(), contains(
                emptyDeliveryDiscounts(COURIER_DELIVERY_ID)));

        assertThat(discountResponse.getUnusedCoins(), empty());
        assertThat(discountResponse.getCoinErrors(), contains(
                allOf(
                        hasProperty("coin", equalTo(new IdObject(deliveryCoin.getCoinKey().getId()))),
                        hasProperty("error", hasProperty("code",
                                equalTo(MarketLoyaltyErrorCode.REGION_MISMATCH_ERROR.name())))
                )
        ));
    }

    @Test
    public void shouldApplyDeliveryCoinIfAtLeastOneCartRegionMatches() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .addCoinRule(
                                RuleType.DELIVERY_REGION_CART_CUTTING_RULE,
                                RuleParameterName.REGION_ID,
                                Collections.singleton(213)
                        )
        );
        Coin deliveryCoin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(b -> b.setId("1").setRegion(121045L))
                )
                .build();

        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(b -> b.setId("2").setRegion(213L))
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order1, order2)
                        .withCoins(deliveryCoin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo("1")),
                                hasProperty("promos", empty())
                        ))),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo("2")),
                                hasProperty("promos", contains(deliveryPromo(deliveryCoin, DEFAULT_DELIVERY_PRICE)))
                        )
                ))
        ));

        assertThat(discountResponse.getCoinErrors(), empty());
    }


    @Test
    public void shouldApplyDeliveryCoinIfRegionMatchesByNotLogic() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .addCoinRule(
                                RuleContainer.builder(DELIVERY_REGION_CART_CUTTING_RULE)
                                        .withParams(REGION_ID, Collections.singleton(213))
                                        .withParams(NOT_LOGIC, Collections.singleton(true))
                        )
        );
        Coin deliveryCoin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(b -> b.setId("1").setRegion(121045L))
                )
                .build();

        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(b -> b.setId("2").setRegion(213L))
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order1, order2)
                        .withCoins(deliveryCoin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo("1")),
                                hasProperty("promos", contains(deliveryPromo(deliveryCoin, DEFAULT_DELIVERY_PRICE)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo("2")),
                                hasProperty("promos", empty())
                        )))
        ));

        assertThat(discountResponse.getCoinErrors(), empty());
    }

    @Test
    public void shouldNotGiveFreeDeliveryIfAlreadyZeroPrice() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(ZERO_PRICE),
                        pickupDelivery()
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders().get(0).getDeliveries(), containsInAnyOrder(
                emptyDeliveryDiscounts(COURIER_DELIVERY_ID),
                hasSingleDeliveryDiscount(PICKUP_DELIVERY_ID, coin, DEFAULT_DELIVERY_PRICE)
        ));

        assertThat(discountResponse.getUnusedCoins(), is(empty()));
    }

    @Test
    public void shouldReturnUnusedFreeDeliveryCoinIfAllDeliveriesAlreadyZeroPrice() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(ZERO_PRICE),
                        pickupDelivery(ZERO_PRICE)
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders().get(0).getDeliveries(), containsInAnyOrder(
                emptyDeliveryDiscounts(COURIER_DELIVERY_ID),
                emptyDeliveryDiscounts(PICKUP_DELIVERY_ID)
        ));

        assertThat(discountResponse.getUnusedCoins(), contains(
                hasProperty("id", equalTo(coin.getCoinKey().getId()))
        ));
    }

    @Test
    public void shouldNowAllowUseDeliveryCoinForDigitalItem() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        downloadable(true),
                        price(100)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order)
                                .withCoins(coin.getCoinKey())
                                .build()
                );

        assertThat(discountResponse.getUnusedCoins(), hasSize(1));
    }

    @Test
    public void shouldNotAllowFreeDeliveryCoinIfFilterRuleMismatch() throws IOException, InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .addCoinRule(RuleType.CATEGORY_FILTER_RULE, CATEGORY_ID, DEFAULT_CATEGORY_ID)
        );
        Coin coin = createCoin(promo, defaultAuth());

        mockReport(DEFAULT_ITEM_KEY, ANOTHER_CATEGORY_ID, BigDecimal.valueOf(100));

        final CoinsForCart response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                DEFAULT_DELIVERY_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 1))
                )
        );
        assertEquals(0, response.getApplicableCoins().size());
        assertEquals(1, response.getDisabledCoins().size());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        categoryId(ANOTHER_CATEGORY_ID),
                        quantity(1),
                        price(100)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order)
                                .withCoins(coin.getCoinKey())
                                .build()
                );

        assertThat(discountResponse.getCoinErrors(), contains(allOf(
                hasProperty("coin", equalTo(new IdObject(coin.getCoinKey().getId()))),
                hasProperty(
                        "error", hasProperty("code", equalTo(MarketLoyaltyErrorCode.NOT_SUITABLE_COIN.name()))
                )
        )));
    }

    @Test
    public void shouldAllowFreeDeliveryCoinIfFilterRuleMatch() throws IOException, InterruptedException {
        reportMockUtils.mockReportService(
                makeDefaultItem(DEFAULT_ITEM_KEY, BigDecimal.valueOf(250)),
                makeDefaultItem(ANOTHER_ITEM_KEY, BigDecimal.valueOf(2500))
        );

        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        Coin coin = createCoin(
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFreeDelivery()
                        .addCoinRule(RuleType.CATEGORY_FILTER_RULE, CATEGORY_ID, DEFAULT_CATEGORY_ID)),
                defaultAuth()
        );

        var response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                DEFAULT_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Arrays.asList(makeItem(DEFAULT_ITEM_KEY, 1), makeItem(ANOTHER_ITEM_KEY, 1))
                )
        );

        assertEquals("Applicable coins map size != 1", 1, response.getApplicableCoins().size());
        assertEquals("Disabled coins map size != 0", 0, response.getDisabledCoins().size());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        categoryId(DEFAULT_CATEGORY_ID),
                        price(250)
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        categoryId(ANOTHER_CATEGORY_ID),
                        price(2500)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order)
                                .withCoins(coin.getCoinKey())
                                .build()
                );

        assertThat(discountResponse.getUnusedCoins(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());
        assertThat(discountResponse, allOf(
                hasProperty("orders", contains(
                        hasProperty("deliveries", containsInAnyOrder(
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", hasSize(1))
                                )
                        ))
                )))
        );
    }

    @Test
    public void shouldAllowFreeDeliveryCoinIfTotalCostMismatchByFilter() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .addCoinRule(RuleType.CATEGORY_FILTER_RULE, CATEGORY_ID, DEFAULT_CATEGORY_ID)
                        .addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(1000))
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        categoryId(DEFAULT_CATEGORY_ID),
                        price(100)
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        categoryId(ANOTHER_CATEGORY_ID),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order)
                                .withCoins(coin.getCoinKey())
                                .build()
                );


        assertThat(discountResponse.getCoinErrors(), contains(allOf(
                hasProperty("coin", equalTo(new IdObject(coin.getCoinKey().getId()))),
                hasProperty(
                        "error", hasProperty("code", equalTo(MarketLoyaltyErrorCode.NOT_SUITABLE_COIN.name()))
                )
        )));
    }

    @Test
    public void shouldAllowFreeDeliveryCoinIfTotalCostMatchByFilter() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .addCoinRule(RuleType.CATEGORY_FILTER_RULE, CATEGORY_ID, DEFAULT_CATEGORY_ID)
                        .addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(1000))
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        categoryId(DEFAULT_CATEGORY_ID),
                        price(1000)
                )
                .withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        categoryId(ANOTHER_CATEGORY_ID),
                        price(100)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order)
                                .withCoins(coin.getCoinKey())
                                .build()
                );


        assertThat(discountResponse.getUnusedCoins(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());
        assertThat(discountResponse, allOf(
                hasProperty("orders", contains(
                        hasProperty("deliveries", containsInAnyOrder(
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", hasSize(1))
                                )
                        ))
                ))));
    }

    @Test
    public void shouldReturnUnusedFreeDeliveryCoinButCalculateOtherDeliveriesIfSelectedDeliveryAlreadyZeroPrice() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        pickupDelivery(),
                        courierDelivery(SELECTED.andThen(ZERO_PRICE))
                )
                .build();

        MultiCartDiscountRequest discountRequest = builder(order)
                .withCoins(coin.getCoinKey())
                .build();

        assertThat(marketLoyaltyClient.calculateDiscount(discountRequest), allOf(
                hasProperty("orders", contains(
                        hasProperty("deliveries", containsInAnyOrder(
                                allOf(
                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                        hasProperty("promos", contains(deliveryPromo(coin, DEFAULT_DELIVERY_PRICE)))
                                ),
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", is(empty()))
                                )
                        ))
                )),
                hasProperty("unusedCoins", contains(
                        hasProperty("id", equalTo(coin.getCoinKey().getId()))
                ))
        ));

        MultiCartDiscountResponse actual = marketLoyaltyClient.spendDiscount(discountRequest);
        assertThat(actual, allOf(
                hasProperty("orders", contains(
                        hasProperty("deliveries", containsInAnyOrder(
                                allOf(
                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                        hasProperty("promos", is(empty()))
                                ),
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", is(empty()))
                                )
                        ))
                )),
                hasProperty("unusedCoins", contains(
                        hasProperty("id", equalTo(coin.getCoinKey().getId()))
                ))
        ));
    }

    @Test
    public void shouldReturnUnusedFreeDeliveryCoinIfUsedFixedCoin() {
        Promo freeDeliveryPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        Coin freeDeliveryCoin = createCoin(freeDeliveryPromo, defaultAuth());

        Promo fixedCoinPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        Coin fixedCoin = createCoin(fixedCoinPromo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        pickupDelivery(),
                        courierDelivery(SELECTED.andThen(ZERO_PRICE))
                )
                .build();

        MultiCartDiscountRequest discountRequest = builder(order)
                .withCoins(freeDeliveryCoin.getCoinKey(), fixedCoin.getCoinKey())
                .build();

        assertThat(marketLoyaltyClient.calculateDiscount(discountRequest), allOf(
                hasProperty("orders", contains(
                        hasProperty("deliveries", containsInAnyOrder(
                                allOf(
                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                        hasProperty("promos", contains(deliveryPromo(freeDeliveryCoin,
                                                DEFAULT_DELIVERY_PRICE)))
                                ),
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", is(empty()))
                                )
                        ))
                )),
                hasProperty("unusedCoins", contains(
                        hasProperty("id", equalTo(freeDeliveryCoin.getCoinKey().getId()))
                ))
        ));

        MultiCartDiscountResponse actual = marketLoyaltyClient.spendDiscount(discountRequest);
        assertThat(actual, allOf(
                hasProperty("orders", contains(
                        hasProperty("deliveries", containsInAnyOrder(
                                allOf(
                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                        hasProperty("promos", is(empty()))
                                ),
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", is(empty()))
                                )
                        ))
                )),
                hasProperty("unusedCoins", contains(
                        hasProperty("id", equalTo(freeDeliveryCoin.getCoinKey().getId()))
                ))
        ));
    }

    @Test
    public void shouldUseFreeDeliveryForTwoOrdersIfOneHasSelectedDeliveryWithZeroPrice() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        String selectedZeroPriceCartId = "selectedZeroPrice";
        String selectedNonZeroPriceCartId = "selectedNonZeroPrice";
        MultiCartDiscountRequest discountRequest = builder(
                orderRequestBuilder()
                        .withCartId(selectedZeroPriceCartId).withOrderItem()
                        .withDeliveries(
                                pickupDelivery(),
                                courierDelivery(SELECTED.andThen(ZERO_PRICE))
                        )
                        .build(),
                orderRequestBuilder()
                        .withCartId(selectedNonZeroPriceCartId)
                        .withOrderItem(itemKey(ANOTHER_ITEM_KEY))
                        .withDeliveries(
                                pickupDelivery(),
                                courierDelivery(SELECTED)
                        )
                        .build()
        )
                .withCoins(coin.getCoinKey())
                .build();

        assertThat(marketLoyaltyClient.calculateDiscount(discountRequest), allOf(
                hasProperty("orders", containsInAnyOrder(
                        allOf(
                                hasProperty("cartId", equalTo(selectedZeroPriceCartId)),
                                hasProperty("deliveries", containsInAnyOrder(
                                                allOf(
                                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                                        hasProperty("promos", is(empty()))
                                                ),
                                                allOf(
                                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                                        hasProperty("promos", contains(deliveryPromo(coin,
                                                                DEFAULT_DELIVERY_PRICE)))
                                                )
                                        )
                                )
                        ),
                        allOf(
                                hasProperty("cartId", equalTo(selectedNonZeroPriceCartId)),
                                hasProperty("deliveries", containsInAnyOrder(
                                                allOf(
                                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                                        hasProperty("promos", contains(deliveryPromo(coin,
                                                                DEFAULT_DELIVERY_PRICE)))
                                                ),
                                                allOf(
                                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                                        hasProperty("promos", contains(deliveryPromo(coin,
                                                                DEFAULT_DELIVERY_PRICE)))
                                                )
                                        )
                                )
                        )
                )),
                hasProperty("unusedCoins", is(empty()))
        ));

        assertThat(marketLoyaltyClient.spendDiscount(discountRequest), allOf(
                hasProperty("orders", containsInAnyOrder(
                        allOf(
                                hasProperty("cartId", equalTo(selectedZeroPriceCartId)),
                                hasProperty("deliveries", containsInAnyOrder(
                                                allOf(
                                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                                        hasProperty("promos", is(empty()))
                                                ),
                                                allOf(
                                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                                        hasProperty("promos", is(empty()))
                                                )
                                        )
                                )
                        ),
                        allOf(
                                hasProperty("cartId", equalTo(selectedNonZeroPriceCartId)),
                                hasProperty("deliveries", containsInAnyOrder(
                                                allOf(
                                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                                        hasProperty("promos", contains(deliveryPromo(coin,
                                                                DEFAULT_DELIVERY_PRICE)))
                                                ),
                                                allOf(
                                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                                        hasProperty("promos", is(empty()))
                                                )
                                        )
                                )
                        )
                )),
                hasProperty("unusedCoins", is(empty()))
        ));

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentBudget(),
                comparesEqualTo(initialBudget.subtract(DEFAULT_DELIVERY_PRICE))
        );
    }

    @NotNull
    private static Matcher<DeliveryResponse> hasSingleDeliveryDiscount(
            String pickupDeliveryId, Coin coin, BigDecimal defaultDeliveryPrice
    ) {
        return allOf(
                hasProperty("id", equalTo(pickupDeliveryId)),
                hasProperty("promos", contains(deliveryPromo(coin, defaultDeliveryPrice)))
        );
    }


    @Test
    public void shouldGiveFreeDelivery() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(courierDelivery(SELECTED))
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders().get(0).getDeliveries(), contains(
                hasSingleDeliveryDiscount(COURIER_DELIVERY_ID, coin, DEFAULT_DELIVERY_PRICE))
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentBudget(),
                comparesEqualTo(initialBudget.subtract(DEFAULT_DELIVERY_PRICE))
        );
    }

    @Test
    public void shouldGiveFreeDeliveryWithFreeDeliveryCoinAndFreeDeliveryAddressOnCalc() {
        Promo promo = createFreeDeliveryCoinPromo();
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(
                                withPrice(BigDecimal.valueOf(249L)),
                                withId("1"),
                                withFreeDeliveryAddress(),
                                withRegion(54L)
                        ),
                        pickupDelivery(
                                withPrice(BigDecimal.valueOf(99L)),
                                withId("2"),
                                withRegion(54L)
                        )
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                contains(
                        allOf(
                                hasProperty("id", equalTo("1")),
                                hasProperty("promos", contains(freeDeliveryAddressPromo(BigDecimal.valueOf(249L))))
                        ),
                        hasSingleDeliveryDiscount("2", coin, BigDecimal.valueOf(99L))
                )
        );
    }

    @Test
    public void shouldSetFreeDeliveryCoinUnusedIfFreeDeliveryAddressSelected() {
        Promo promo = createFreeDeliveryCoinPromo();
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(
                                SELECTED,
                                withPrice(BigDecimal.valueOf(249L)),
                                withId("1"),
                                withFreeDeliveryAddress(),
                                withRegion(54L)
                        ),
                        pickupDelivery(
                                withPrice(BigDecimal.valueOf(99L)),
                                withId("2"),
                                withRegion(54L)
                        )
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                contains(
                        allOf(
                                hasProperty("id", equalTo("1")),
                                hasProperty("promos", contains(freeDeliveryAddressPromo(BigDecimal.valueOf(249L))))
                        ),
                        hasSingleDeliveryDiscount("2", coin, BigDecimal.valueOf(99L))
                )
        );

        assertThat(discountResponse.getUnusedCoins(), contains(equalTo(new IdObject(coin.getCoinKey().getId()))));
    }

    @Test
    public void shouldUseFreeDeliveryCoinIfFreeDeliveryAddressNotSelected() {
        Promo promo = createFreeDeliveryCoinPromo();
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(
                                withPrice(BigDecimal.valueOf(249L)),
                                withId("1"),
                                withFreeDeliveryAddress(),
                                withRegion(54L)
                        ),
                        pickupDelivery(
                                SELECTED,
                                withPrice(BigDecimal.valueOf(99L)),
                                withId("2"),
                                withRegion(54L)
                        )
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                contains(
                        allOf(
                                hasProperty("id", equalTo("1")),
                                hasProperty("promos", contains(freeDeliveryAddressPromo(BigDecimal.valueOf(249L))))
                        ),
                        hasSingleDeliveryDiscount("2", coin, BigDecimal.valueOf(99L))
                )
        );

        assertThat(discountResponse.getUnusedCoins(), is(empty()));
    }

    @Test
    public void shouldSetFreeDeliveryCoinUnusedIfFreeDeliveryAddress() {
        Promo promo = createFreeDeliveryCoinPromo();
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(
                                withPrice(BigDecimal.valueOf(249L)),
                                withId("1"),
                                withFreeDeliveryAddress(),
                                withRegion(54L)
                        )
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                contains(
                        allOf(
                                hasProperty("id", equalTo("1")),
                                hasProperty("promos", contains(freeDeliveryAddressPromo(BigDecimal.valueOf(249L))))
                        )
                )
        );

        assertThat(discountResponse.getUnusedCoins(), contains(coinIdObject(coin)));
    }

    @Test
    public void shouldGiveFreeDeliveryByFreeDeliveryCoinForExcludedStickHids() {
        Promo promo = createFreeDeliveryCoinPromo();
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        excludedStickCategoryId()
                )
                .withDeliveries(
                        courierDelivery(
                                withPrice(BigDecimal.valueOf(249L)),
                                withId("1"),
                                withRegion(54L)
                        )
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                contains(
                        hasSingleDeliveryDiscount("1", coin, BigDecimal.valueOf(249L))
                )
        );

        assertThat(discountResponse.getUnusedCoins(), is(empty()));
    }

    @Test
    public void shouldCalculateDeliveryForDsbsAndKgtWithFlag() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(5000))
                                )
                                .withDeliveries(courierDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(BigDecimal.valueOf(999))
                                )
                                .withDeliveries(pickupDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(THIRD_ITEM_KEY),
                                        price(BigDecimal.valueOf(1200))
                                )
                                .withDeliveries(courierDelivery())
                                .build()
                ).build()
        );

        BigDecimal deliveryDiscount =
                DEFAULT_DELIVERY_PRICE.divide(BigDecimal.valueOf(3)).multiply(BigDecimal.valueOf(2));

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(deliveryDiscount, MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(deliveryDiscount, MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(deliveryDiscount, MULTICART_DISCOUNT)))
                        )
                ))
        ));
    }

    //Далее блок кейсов для унифицированной тарифной сетки.
    //Сами кейсы описаны в п.5 здесь https://wiki.yandex-team
    // .ru/users/ostrovskiy/edinye-tarify-na-dostavku--podem-tovarov-na-jetazh/#5
    // .opisaniekorner-kejjsovdljaslozhnyxkorzin
    // ---------------------------------------------------------------------------------------------
    private static final BigDecimal KGT_DELIVERY_PRICE_1 = BigDecimal.valueOf(549);

    @Test
    public void unifiedTariffsCase1() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(1000))
                                )
                                .withDeliveries(courierDelivery(
                                        withRegion(MOSCOW_REGION),
                                        withPrice(BigDecimal.valueOf(49))
                                ))
                                .build()
                ).build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", not(hasProperty("promos")))
        ));
    }

    @Test
    public void unifiedTariffsCase2() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(1000))
                                )
                                .withDeliveries(courierDelivery(
                                        withRegion(MOSCOW_REGION),
                                        withPrice(ZERO)
                                ))
                                .build()
                ).build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", not(hasProperty("promos")))
        ));
    }

    @Test
    public void unifiedTariffsCase3() {
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(1500)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(BigDecimal.valueOf(49))
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(1000),
                                                dropship()
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(BigDecimal.valueOf(49))
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(THIRD_ITEM_KEY),
                                                price(500),
                                                dropship()
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(BigDecimal.valueOf(49))
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        BigDecimal discount = BigDecimal.valueOf(33);

        assertThat(discountResponse.getOrders(), everyItem(
                hasProperty("deliveries", contains(
                                hasProperty("promos", contains(deliveryPromo(discount, MULTICART_DISCOUNT)))
                        )
                ))
        );
    }

    @Test
    public void unifiedTariffsCase4() {
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(1500)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.pickupDelivery(
                                                withPrice(BigDecimal.valueOf(0))
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(1000),
                                                dropship()
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(BigDecimal.valueOf(49))
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", hasSize(0))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", hasSize(0))
                        )
                ))
        ));
    }

    @Test
    public void unifiedTariffsCase5() {
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(500)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(BigDecimal.valueOf(99))
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(KGT_DELIVERY_PRICE_1)
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        BigDecimal discount = BigDecimal.valueOf(49);
        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(discount, MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(discount, MULTICART_DISCOUNT)))
                        )
                ))
        ));
    }

    @Test
    public void unifiedTariffsCase6() {
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(500),
                                                dropship()
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(BigDecimal.valueOf(99))
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(KGT_DELIVERY_PRICE_1)
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        BigDecimal discount = BigDecimal.valueOf(49);
        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(discount, MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(discount, MULTICART_DISCOUNT)))
                        )
                ))
        ));
    }

    @Test
    public void unifiedTariffsCase7() {
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(500),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(KGT_DELIVERY_PRICE_1)
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(KGT_DELIVERY_PRICE_1)
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        BigDecimal discount = BigDecimal.valueOf(274);
        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(discount, MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(discount, MULTICART_DISCOUNT)))
                        )
                ))
        ));
    }

    @Test
    public void unifiedTariffsCase8() {
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(1000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(KGT_DELIVERY_PRICE_1)
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(1000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(KGT_DELIVERY_PRICE_1)
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(THIRD_ITEM_KEY),
                                                price(1000),
                                                dropship()
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(BigDecimal.valueOf(99))
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        Map<Long, Long> map =
                discountResponse.getOrders()
                        .stream()
                        .collect(Collectors.toMap(
                                o -> o.getItems()
                                        .get(0)
                                        .getFeedId(),
                                o -> o.getDeliveries()
                                        .get(0)
                                        .getPromos()
                                        .stream()
                                        .filter(p -> p.getPromoType()
                                                .equals(MULTICART_DISCOUNT))
                                        .map(DeliveryPromoResponse::getDiscount)
                                        .map(BigDecimal::longValue)
                                        .findFirst()
                                        .orElse(0L)
                        ));
        List<Long> discountValues = new ArrayList<>(List.of(452L, 98L, 98L));
        assertThat(map, hasValue(discountValues.get(0)));
        discountValues.remove(0);
        assertThat(map, hasValue(discountValues.get(0)));
        discountValues.remove(0);
        assertThat(map, hasValue(discountValues.get(0)));
        discountValues.remove(0);
    }

    @Test
    public void unifiedTariffsCase9() {
        BigDecimal DELIVERY_PRICE = BigDecimal.valueOf(999);
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(DELIVERY_PRICE)
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(DELIVERY_PRICE)
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(THIRD_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(DELIVERY_PRICE)
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        Map<Long, Long> map =
                discountResponse.getOrders()
                        .stream()
                        .collect(Collectors.toMap(
                                o -> o.getItems()
                                        .get(0)
                                        .getFeedId(),
                                o -> o.getDeliveries()
                                        .get(0)
                                        .getPromos()
                                        .stream()
                                        .filter(p -> p.getPromoType()
                                                .equals(MULTICART_DISCOUNT))
                                        .map(DeliveryPromoResponse::getDiscount)
                                        .map(BigDecimal::longValue)
                                        .findFirst()
                                        .orElse(0L)
                        ));
        long discount = 666L;
        assertThat(map, hasEntry(2131L, discount));
        assertThat(map, hasEntry(200321470L, discount));
        assertThat(map, hasEntry(1312312L, discount));
    }

    @Test
    public void unifiedTariffsCase10() {
        BigDecimal DELIVERY_PRICE = BigDecimal.valueOf(849);
        long item4thFeedId = 123123123L;
        long item5thFeedId = 141414141L;
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withOrderItem(
                                                itemKey(THIRD_ITEM_KEY),
                                                price(10000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(DELIVERY_PRICE)
                                        ))
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(item4thFeedId, "0"),
                                                price(10000),
                                                dropship()
                                        )
                                        .withOrderItem(
                                                itemKey(item5thFeedId, "0"),
                                                price(10000),
                                                dropship()
                                        )
                                        .withLargeSize(true)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(DELIVERY_PRICE)
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        BigDecimal discountVal = BigDecimal.valueOf(424);
        assertTrue(discountResponse.getOrders().stream()
                .flatMap(o -> o.getDeliveries().stream())
                .flatMap(o -> o.getPromos().stream())
                .filter(p -> p.getPromoType().equals(MULTICART_DISCOUNT))
                .map(DeliveryPromoResponse::getDiscount)
                .allMatch(discount -> discount.compareTo(discountVal) == 0));
    }


    // ---------------------------------------------------------------------------------------------

    @Test
    public void shouldDeliverDbsStaffForFreeToFreeDeliveryAddress() {
        BigDecimal DELIVERY_COST = BigDecimal.valueOf(256);
        final MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(1500),
                                                dropship()
                                        )
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery(
                                                withPrice(DELIVERY_COST),
                                                withFreeDeliveryAddress()
                                        ))
                                        .build()
                        )
                        .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DELIVERY_COST, FREE_DELIVERY_ADDRESS)))
                        )
                ))
        ));
    }

    @Test
    public void shouldGetCoinForCartFreeDeliveryCoinForExcludedStickHids() throws IOException, InterruptedException {
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();
        mockReport(DEFAULT_ITEM_KEY, STICK_CATEGORY, DEFAULT_THRESHOLD);

        createCoin(createFreeDeliveryCoinPromo(), defaultAuth(DEFAULT_UID));

        var response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                DEFAULT_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 1))
                )
        );

        assertThat(
                response,
                hasProperty("applicableCoins", hasSize(1))
        );
    }

    @Test
    public void shouldGetCoinForCartFreeDeliveryCoinForDigitalItems() throws IOException, InterruptedException {
        Promo promo = createFreeDeliveryCoinPromo();
        createCoin(promo, defaultAuth(DEFAULT_UID));

        mockReport(DEFAULT_ITEM_KEY, true);

        final CoinsForCart response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                213L,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 1))
                )
        );

        assertThat(
                response,
                allOf(
                        hasProperty(
                                "applicableCoins",
                                empty()
                        ),
                        hasProperty(
                                "disabledCoins",
                                hasEntry(equalTo(FILTER_RULE_RESTRICTION), contains(any(UserCoinResponse.class)))
                        )
                )
        );
    }

    @Test
    public void shouldNotGetCoinForCartFreeDeliveryCoinForExcludedAlcoholHids() throws IOException,
            InterruptedException {
        Promo promo = createFreeDeliveryCoinPromo();
        createCoin(promo, defaultAuth(DEFAULT_UID));

        mockReport(DEFAULT_ITEM_KEY, ALCOHOL_CATEGORY);

        final CoinsForCart response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                213L,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 1))
                )
        );

        assertThat(
                response,
                allOf(
                        hasProperty(
                                "applicableCoins",
                                empty()
                        )
                )
        );
    }

    private void mockReport(ItemKey itemKey, int category) throws IOException, InterruptedException {
        mockReport(itemKey, category, BigDecimal.valueOf(100));
    }

    private void mockReport(ItemKey itemKey, int category, BigDecimal price) throws IOException, InterruptedException {
        FoundOffer fo = createFoundOffer(itemKey, category, price);

        reportMockUtils.mockReportService(fo);
    }

    private FoundOffer mockReportWithDbs(ItemKey itemKey, int category, BigDecimal price) throws IOException,
            InterruptedException {
        FoundOffer fo = createFoundOffer(itemKey, category, price);
        fo.setCpa("real");
        fo.setRgb(Color.WHITE);

        reportMockUtils.mockReportService(fo);
        return fo;
    }

    @NotNull
    private FoundOffer createFoundOffer(ItemKey itemKey, int category, BigDecimal price) {
        FoundOffer fo = new FoundOffer();
        fo.setFeedId(itemKey.getFeedId());
        fo.setShopOfferId(itemKey.getOfferId());
        fo.setPrice(price);
        fo.setHyperCategoryId(category);
        fo.setCargoTypes(Collections.emptySet());
        fo.setWeight(BigDecimal.ONE);
        return fo;
    }

    private void mockReport(ItemKey itemKey, boolean isDownloadable) throws IOException, InterruptedException {
        FoundOffer fo = new FoundOffer();
        fo.setFeedId(itemKey.getFeedId());
        fo.setShopOfferId(itemKey.getOfferId());
        fo.setPrice(BigDecimal.valueOf(100));
        fo.setHyperCategoryId(DEFAULT_CATEGORY_ID);
        fo.setDownloadable(isDownloadable);
        fo.setCargoTypes(Collections.emptySet());
        fo.setWeight(BigDecimal.ONE);

        reportMockUtils.mockReportService(fo);
    }

    private static OrderItemsRequest.Item makeItem(ItemKey defaultItemKey, int count) {
        return OrderItemsRequest.Item.builder()
                .setFeedId(defaultItemKey.getFeedId())
                .setOfferId(defaultItemKey.getOfferId())
                .setCount(count)
                .build();
    }

    @Test
    public void shouldNotGiveFreeDeliveryByFreeDeliveryCoinForExcludedAlcoholHids() {
        Promo promo = createFreeDeliveryCoinPromo();
        Coin coin = createCoin(promo, defaultAuth(DEFAULT_UID));

        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        excludedAlcoholCategoryId()
                )
                .withDeliveries(
                        courierDelivery(
                                withPrice(BigDecimal.valueOf(249L)),
                                withId("1"),
                                withRegion(54L)
                        )
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                contains(
                        emptyDeliveryDiscounts("1")
                )
        );

        assertThat(discountResponse.getUnusedCoins(), contains(coinIdObject(coin)));
    }


    @Test
    public void shouldGiveFreeDeliveryForTwoOrders() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(
                        orderRequestBuilder().withOrderItem()
                                .withDeliveries(courierDelivery(SELECTED))
                                .build(),
                        orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY))
                                .withDeliveries(courierDelivery(SELECTED))
                                .build()
                )
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", contains(deliveryPromo(coin, DEFAULT_DELIVERY_PRICE)))
                                )
                        )
                ),
                hasProperty("deliveries", contains(
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", contains(deliveryPromo(coin, DEFAULT_DELIVERY_PRICE)))
                                )
                        )
                )
        ));

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentBudget(),
                comparesEqualTo(initialBudget.subtract(DEFAULT_DELIVERY_PRICE.multiply(BigDecimal.valueOf(2))))
        );
    }

    @Test
    public void shouldGiveFreeDeliveryForOneOrderIfItemsFromSecondsInExcludeList() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        String normalCartId = "normalCart";
        String cartWithoutDiscountsId = "cartWithoutDiscounts";
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(
                        orderRequestBuilder()
                                .withCartId(normalCartId).withOrderItem()
                                .withDeliveries(courierDelivery(SELECTED))
                                .build(),
                        orderRequestBuilder()
                                .withCartId(cartWithoutDiscountsId)
                                .withOrderItem(
                                        categoryId(ALCOHOL_CATEGORY),
                                        itemKey(ANOTHER_ITEM_KEY)
                                )
                                .withDeliveries(courierDelivery(SELECTED))
                                .build()
                )
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("cartId", equalTo(normalCartId)),
                        hasProperty("deliveries", contains(
                                        allOf(
                                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                                hasProperty("promos", contains(deliveryPromo(coin, DEFAULT_DELIVERY_PRICE)))
                                        )
                                )
                        )
                ),
                allOf(
                        hasProperty("cartId", equalTo(cartWithoutDiscountsId)),
                        hasProperty("deliveries", contains(
                                        allOf(
                                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                                hasProperty("promos", is(empty()))
                                        )
                                )
                        )
                )
        ));

        assertThat(discountResponse.getUnusedCoins(), is(empty()));

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentBudget(),
                comparesEqualTo(initialBudget.subtract(DEFAULT_DELIVERY_PRICE))
        );
    }

    @Test
    public void shouldReturnUnusedFreeDeliveryCoinForOrderInExcludeList() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        MultiCartDiscountRequest orderRequest = builder(
                orderRequestBuilder()
                        .withOrderItem(categoryId(ALCOHOL_CATEGORY))
                        .withDeliveries(courierDelivery(SELECTED))
                        .build()
        )
                .withCoins(coin.getCoinKey())
                .build();

        assertThat(marketLoyaltyClient.calculateDiscount(orderRequest), allOf(
                hasProperty("orders", contains(
                        hasProperty("deliveries", contains(
                                        allOf(
                                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                                hasProperty("promos", is(empty()))
                                        )
                                )
                        )
                )),
                hasProperty("unusedCoins", contains(
                        hasProperty("id", equalTo(coin.getCoinKey().getId()))
                ))
        ));

        assertThat(marketLoyaltyClient.spendDiscount(orderRequest), allOf(
                hasProperty("orders", contains(
                        hasProperty("deliveries", contains(
                                        allOf(
                                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                                hasProperty("promos", is(empty()))
                                        )
                                )
                        )
                )),
                hasProperty("unusedCoins", contains(
                        hasProperty("id", equalTo(coin.getCoinKey().getId()))
                ))
        ));

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentBudget(),
                comparesEqualTo(initialBudget)
        );
    }

    @Test
    public void shouldUseFreeDeliveryCoinForOrderInExcludeListButSupplier() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        MultiCartDiscountRequest orderRequest = builder(
                orderRequestBuilder()
                        .withOrderItem(categoryId(PHARMA_LIST_CATEGORY_ID), supplier(SUPPLIER_EXCLUSION_ID))
                        .withDeliveries(courierDelivery(SELECTED))
                        .build()
        )
                .withCoins(coin.getCoinKey())
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(orderRequest);

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                contains(
                        hasSingleDeliveryDiscount(COURIER_DELIVERY_ID, coin, DEFAULT_DELIVERY_PRICE)
                )
        );
    }

    @Test
    public void shouldUseFreeDeliveryCoinForOrderInExcludeListButOurDelivery() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        MultiCartDiscountRequest orderRequest = builder(
                orderRequestBuilder()
                        .withOrderItem(categoryId(PHARMA_LIST_CATEGORY_ID), dropship(false))
                        .withDeliveries(courierDelivery(SELECTED))
                        .build()
        )
                .withCoins(coin.getCoinKey())
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(orderRequest);

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                contains(
                        hasSingleDeliveryDiscount(COURIER_DELIVERY_ID, coin, DEFAULT_DELIVERY_PRICE)
                )
        );
    }

    @Test
    public void shouldReturnUnusedCoinIfTwoFreeDeliveryCoinWasGiven() {
        Promo promo = createFreeDeliveryCoinPromo();
        Coin firstCoin = createCoin(promo, defaultAuth());
        Coin secondCoin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(courierDelivery())
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(firstCoin.getCoinKey(), secondCoin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders().get(0).getDeliveries(), contains(allOf(
                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                hasProperty("promos", contains(anyOf(
                        deliveryPromo(firstCoin, DEFAULT_DELIVERY_PRICE),
                        deliveryPromo(secondCoin, DEFAULT_DELIVERY_PRICE)
                )))
        )));

        assertThat(discountResponse.getUnusedCoins(), containsInAnyOrder(
                anyOf(
                        hasProperty("id", equalTo(firstCoin.getCoinKey().getId())),
                        hasProperty("id", equalTo(secondCoin.getCoinKey().getId()))
                )));
    }


    @Test
    public void shouldNotFailOnNullRegionGiveFreeDeliveryForSelectedDelivery() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(),
                        pickupDelivery(SELECTED.andThen(b -> b.setRegion(null))),
                        postDelivery()
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(discountResponse.getOrders().get(0).getDeliveries(), containsInAnyOrder(
                        emptyDeliveryDiscounts(COURIER_DELIVERY_ID),
                        hasSingleDeliveryDiscount(PICKUP_DELIVERY_ID, coin, DEFAULT_DELIVERY_PRICE),
                        emptyDeliveryDiscounts(POST_DELIVERY_ID)
                )
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentBudget(),
                comparesEqualTo(initialBudget.subtract(DEFAULT_DELIVERY_PRICE))
        );
    }

    @Test
    public void shouldNotFailIfNoSelectedDeliveryOnCalc() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(),
                        pickupDelivery(),
                        postDelivery()
                )
                .build();

        marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(coinKey)
                        .build()
        );
    }

    @Test
    public void shouldFailIfNoSelectedDeliveryOnSpend() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(),
                        pickupDelivery(),
                        postDelivery()
                )
                .build();

        assertThrows(MarketLoyaltyException.class, () -> marketLoyaltyClient.spendDiscount(
                builder(order)
                        .withCoins(coinKey)
                        .build()
        ));
    }

    @Ignore("MARKETCHECKOUT-7597")
    @Test
    public void shouldFailIfMoreThenOneSelectedDeliveryOnSpendOrCalc() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(SELECTED),
                        pickupDelivery(SELECTED),
                        postDelivery(SELECTED)
                )
                .build();

        MultiCartDiscountRequest request = builder(order)
                .withCoins(coinKey)
                .build();

        assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calculateDiscount(request)
        );
        assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.spendDiscount(request)
        );
    }

    @Test
    public void shouldGiveFreeDeliveryForSelectedDelivery() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
        Coin coin = createCoin(promo, defaultAuth());

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem()
                .withDeliveries(
                        courierDelivery(),
                        pickupDelivery(SELECTED),
                        postDelivery()
                )
                .build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order)
                        .withCoins(coin.getCoinKey())
                        .build()
        );

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                containsInAnyOrder(
                        emptyDeliveryDiscounts(COURIER_DELIVERY_ID),
                        hasSingleDeliveryDiscount(PICKUP_DELIVERY_ID, coin, DEFAULT_DELIVERY_PRICE),
                        emptyDeliveryDiscounts(POST_DELIVERY_ID)
                )
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentBudget(),
                comparesEqualTo(initialBudget.subtract(DEFAULT_DELIVERY_PRICE))
        );
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldGiveDiscountIfSumOfItemPricesMoreThanThreshold() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder().withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withDeliveries(courierDelivery())
                                .build(),
                        orderRequestBuilder().withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withDeliveries(pickupDelivery())
                                .build()
                ).build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                ))
        ));

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldGiveDiscountIfOnlyYandexPlusThresholdMatches() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        MultiCartDiscountResponse discountResponse =
                calculateDiscountForYandexPlusThresholdTest(YANDEX_PLUS_TEST_THRESHOLD_REGION_WO_DEFAULT_THRESHOLD,
                        1000, 800);

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                ))
        ));

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(discountResponse.getThreshold(), comparesEqualTo(BigDecimal.valueOf(YANDEX_PLUS_TEST_THRESHOLD)));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldGiveYandexPlusDiscountIfBothThresholdsMatch() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        long yandexPlusTestThresholdRegionId = YANDEX_PLUS_TEST_THRESHOLD_REGION;
        MultiCartDiscountResponse discountResponse =
                calculateDiscountForYandexPlusThresholdTest(yandexPlusTestThresholdRegionId, 4000, 3800);

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                ))
        ));

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(discountResponse.getThreshold(), comparesEqualTo(BigDecimal.valueOf(YANDEX_PLUS_TEST_THRESHOLD)));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldGiveDefaultDiscountIfNotYandexPlusUser() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, false, blackboxRestTemplate);
        MultiCartDiscountResponse discountResponse =
                calculateDiscountForYandexPlusThresholdTest(YANDEX_PLUS_TEST_THRESHOLD_REGION, 4000, 3800);

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                ))
        ));

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(discountResponse.getThreshold(), comparesEqualTo(BigDecimal.valueOf(5000)));
    }

    private MultiCartDiscountResponse calculateDiscountForYandexPlusThresholdTest(
            long yandexPlusTestThresholdRegionId, int firstItemPrice, int secondItemPrice
    ) {
        return marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder().withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(BigDecimal.valueOf(firstItemPrice))
                                )
                                .withDeliveries(courierDelivery(withRegion(yandexPlusTestThresholdRegionId)))
                                .build(),
                        orderRequestBuilder().withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(BigDecimal.valueOf(secondItemPrice))
                                )
                                .withDeliveries(pickupDelivery(withRegion(yandexPlusTestThresholdRegionId)))
                                .build()
                ).build()
        );
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNotGiveDiscountIfAllItemsAreDropship() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withDeliveries(courierDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withDeliveries(pickupDelivery())
                                .build()
                ).build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", is(empty()))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", is(empty()))
                        )
                ))
        ));

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), nullValue());
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(DROPSHIP_ONLY));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNotGiveMulticartDiscountForPartnerItem() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(5000))
                                )
                                .withDeliveries(courierDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(BigDecimal.valueOf(999))
                                )
                                .withDeliveries(pickupDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(THIRD_ITEM_KEY),
                                        price(BigDecimal.valueOf(1200))
                                )
                                .withDeliveries(courierDelivery())
                                .build()
                ).build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE_HALF,
                                        MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE_HALF,
                                        MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", empty())
                        )
                ))
        ));

    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNotGiveThresholdDiscountForPartnerItem() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(5000))
                                )
                                .withDeliveries(courierDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(BigDecimal.valueOf(3999))
                                )
                                .withDeliveries(pickupDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(THIRD_ITEM_KEY),
                                        price(BigDecimal.valueOf(1200))
                                )
                                .withDeliveries(courierDelivery())
                                .build()
                ).build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE,
                                        FREE_DELIVERY_THRESHOLD)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", empty())
                        )
                ))
        ));
    }


    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldExcludeDropshipItemsFromThreshold() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withDeliveries(courierDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(BigDecimal.valueOf(99))
                                )
                                .withDeliveries(pickupDelivery())
                                .build()
                ).build()
        );

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(2400)));
    }

    @Test
    public void shouldNotGiveDiscountForThresholdIfAlreadyZeroPrice() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder().withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(4000))
                                )
                                .withDeliveries(courierDelivery(ZERO_PRICE))
                                .build(),
                        orderRequestBuilder().withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(4000))
                                )
                                .withDeliveries(pickupDelivery(ZERO_PRICE))
                                .build()
                ).build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", is(empty()))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", is(empty()))
                        )
                ))
        ));

        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(ALREADY_ZERO_PRICE));
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
    }


    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnPriceLeftForDelivery() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder().withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(1000))
                                )
                                .withDeliveries(courierDelivery())
                                .build(),
                        orderRequestBuilder().withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(1000))
                                )
                                .withDeliveries(pickupDelivery())
                                .build()
                ).build()
        );

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(499)));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNotReturnFreeDeliveryForYaPlusUser() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder().withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withDeliveries(pickupDelivery())
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(1499)));
        assertThat(discountResponse.getOrders(), hasSize(1));
        assertThat(
                discountResponse.getOrders().get(0).getDeliveries().get(0).getPromos(), is(empty())
        );
    }

    @Test
    public void shouldDisableFreeDeliveryCoinForYaPlusUserInCaseOfFreeDelivery() throws IOException,
            InterruptedException {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        mockReport(DEFAULT_THRESHOLD);
        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        coinService.create.createCoin(
                promoManager.createSmartShoppingPromo(defaultFreeDelivery()),
                defaultAuth().build()
        );

        var response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                DEFAULT_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 1))
                )
        );

        assertThat(
                response,
                hasProperty(
                        "disabledCoins",
                        hasEntry(equalTo(FREE_DELIVERY_THRESHOLD_RESTRICTION), contains(any(UserCoinResponse.class)))
                )
        );
    }
// [TODO] MARKETDISCOUNT-5480

//    @Test
//    public void shouldHideFreeDeliveryCoinInCaseOf3PCart() throws IOException, InterruptedException {
//        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
//        FoundOffer foundOffer = mockReport(BigDecimal.valueOf(1000));
//        foundOffer.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.SHOP.getCode()));
//
//        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());
//        coinService.create.createCoin(promo, defaultAuth().build());
//
//        final CoinsForCart response = marketLoyaltyClient.getCoinsForCartV2(
//            DEFAULT_UID,
//            YANDEX_PLUS_TEST_THRESHOLD_REGION,
//            false,
//            MarketPlatform.BLUE,
//            UsageClientDeviceType.DESKTOP,
//            new OrderItemsRequest(
//                Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 1))
//            )
//        );
//
//        assertThat(
//            response,
//            hasProperty(
//                "disabledCoins",
//                hasEntry(equalTo(DROPSHIP_RESTRICTION), contains(any(UserCoinResponse.class)))
//            )
//        );
//    }

    @Test
    public void shouldNotReturnThresholdOnFreeDeliveryCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withOrderItem()
                                        .withDeliveries(
                                                courierDelivery(),
                                                pickupDelivery()
                                        )
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .withCoins(coinKey)
                        .build()
        );

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(discountResponse.getThreshold(), comparesEqualTo(ZERO));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_FREE_DELIVERY));
    }

    @Test
    public void shouldNotReturnFreeDeliveryForNotYaPlusUser() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, false, blackboxRestTemplate);

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder().withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withDeliveries(courierDelivery())
                                        .build(),
                                orderRequestBuilder().withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withDeliveries(pickupDelivery())
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getOrders(), hasSize(2));
        assertFalse(
                discountResponse.getOrders().get(0).getDeliveries().get(0).getPromos().stream()
                        .anyMatch(deliveryPromoResponse -> deliveryPromoResponse.getPromoType() != PromoType.MULTICART_DISCOUNT)
        );
        assertFalse(
                discountResponse.getOrders().get(1).getDeliveries().get(0).getPromos().stream()
                        .anyMatch(deliveryPromoResponse -> deliveryPromoResponse.getPromoType() != PromoType.MULTICART_DISCOUNT)
        );
    }

    @Test
    public void shouldNotReturnFreeDeliveryForYaPlusUserWhenLessThanThreshold() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder().withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(499))
                                        )
                                        .withDeliveries(courierDelivery())
                                        .build(),
                                orderRequestBuilder().withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(499))
                                        )
                                        .withDeliveries(pickupDelivery())
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getOrders(), hasSize(2));
        assertFalse(
                discountResponse.getOrders().get(0).getDeliveries().get(0).getPromos().stream()
                        .anyMatch(deliveryPromoResponse -> deliveryPromoResponse.getPromoType() != PromoType.MULTICART_DISCOUNT)
        );
        assertFalse(
                discountResponse.getOrders().get(1).getDeliveries().get(0).getPromos().stream()
                        .anyMatch(deliveryPromoResponse -> deliveryPromoResponse.getPromoType() != PromoType.MULTICART_DISCOUNT)
        );
    }

    @Test
    public void shouldNotReturnFreeDeliveryForYaPlus() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        String firstCartId = "firstCartId";
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId(firstCartId).withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );


        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("cartId", equalTo(firstCartId)),
                        hasProperty("deliveries", contains(
                                hasProperty("promos", is(empty())
                                )))
                )

        ));
    }

    @Test
    public void shouldGiveDiscountForMultiCartDeliveries() {
        String firstCartId = "firstCartId";
        String secondCartId = "secondCartId";
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId(firstCartId).withOrderItem()
                                        .withDeliveries(
                                                courierDelivery(withPrice(BigDecimal.valueOf(249))),
                                                pickupDelivery(withPrice(BigDecimal.valueOf(99)))
                                        )
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId(secondCartId).withOrderItem()
                                        .withDeliveries(
                                                courierDelivery(withPrice(BigDecimal.valueOf(249)))
                                        )
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );


        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("cartId", equalTo(firstCartId)),
                        hasProperty("deliveries", containsInAnyOrder(
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", contains(allOf(
                                                hasProperty("promoType", equalTo(MULTICART_DISCOUNT)),
                                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(150)))
                                        )))
                                ),
                                allOf(
                                        hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                        hasProperty("promos", is(empty()))
                                )
                        ))
                ),
                allOf(
                        hasProperty("cartId", equalTo(secondCartId)),
                        hasProperty("deliveries", contains(
                                allOf(
                                        hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                        hasProperty("promos", contains(allOf(
                                                hasProperty("promoType", equalTo(MULTICART_DISCOUNT)),
                                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(99)))
                                        )))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldNotReturnFreeDeliveryForYaPlusIfCartWithYaPlusRegionHasItemsLessThanTreshold() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        String firstCartId = "firstCartId";
        String secondCartId = "secondCartId";
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId(firstCartId).withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(499))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId(secondCartId).withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(KERCH_REGION)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );


        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("cartId", equalTo(firstCartId)),
                        hasProperty("deliveries", contains(
                                hasProperty("promos", contains(
                                        hasProperty("promoType", equalTo(MULTICART_DISCOUNT))
                                ))
                        ))
                ),
                allOf(
                        hasProperty("cartId", equalTo(secondCartId)),
                        hasProperty("deliveries", contains(
                                hasProperty("promos", contains(
                                        hasProperty("promoType", equalTo(MULTICART_DISCOUNT))
                                ))
                        ))
                )
        ));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnPriceLeftForDeliveryForMaxThresholdRegion() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder().withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withDeliveries(courierDelivery())
                                        .build(),
                                orderRequestBuilder().withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(2000))
                                        )
                                        .withDeliveries(pickupDelivery(withRegion((long) FAR_EASTERN_FEDERAL_DISTRICT)))
                                        .build()
                        )
                        .build()
        );

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(4000)));
        assertThat(discountResponse.getThreshold(), comparesEqualTo(BigDecimal.valueOf(7000)));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnPriceLeftForFreeDeliveryNoYaPlusV2() throws IOException, InterruptedException {
        mockReportWithDefaultAndDropshipItem();

        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.emptySet(),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemV2()))
        );

        assertThat(priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(2299)));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(THRESHOLD_FREE_DELIVERY));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldCalculatePriceLeftForFreeDeliveryV2WithQuantitySumming() throws IOException,
            InterruptedException {
        mockReportWithDefaultAndDropshipItem();

        PriceLeftForFreeDeliveryResponse response = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                (long) RUSSIA,
                Collections.emptySet(),
                MarketPlatform.BLUE,
                UsageClientDeviceType.APPLICATION,
                new OrderItemsRequest(Arrays.asList(
                        defaultItemV2(),
                        OrderItemsRequest.Item.builder()
                                .setFeedId(DEFAULT_ITEM_KEY.getFeedId())
                                .setOfferId(DEFAULT_ITEM_KEY.getOfferId())
                                .setBundleId("some bundle")
                                .setCount(1)
                                .build()
                ))
        );

        assertThat(response, hasProperty("priceLeftForFreeDelivery",
                comparesEqualTo(BigDecimal.valueOf(2099))));
        assertThat(response, hasProperty("threshold", comparesEqualTo(DEFAULT_THRESHOLD)));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldPreferThresholdToYaPlusV2() throws IOException, InterruptedException {
        mockReport(BigDecimal.valueOf(3000));

        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.singleton(YANDEX_PLUS),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemV2()))
        );

        assertThat(priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(THRESHOLD_FREE_DELIVERY));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldPreferThresholdToYaPlusWhenNoFreeDeliveryOnCheckout() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        String firstCartId = "firstCartId";
        String secondCartId = "secondCartId";
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId(firstCartId).withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(499))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId(secondCartId).withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(499))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );


        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(1501)));
        assertThat(discountResponse.getThreshold(), comparesEqualTo(DEFAULT_THRESHOLD));
        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("cartId", equalTo(firstCartId)),
                        hasProperty("deliveries", contains(
                                hasProperty("promos", contains(
                                        hasProperty("promoType", equalTo(MULTICART_DISCOUNT))
                                ))
                        ))
                ),
                allOf(
                        hasProperty("cartId", equalTo(secondCartId)),
                        hasProperty("deliveries", contains(
                                hasProperty("promos", contains(
                                        hasProperty("promoType", equalTo(MULTICART_DISCOUNT))
                                ))
                        ))
                )
        ));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldPreferThresholdToYaPlusWhenBothYaPlusAndThresholdFreeDeliveryOnCheckout() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        String firstCartId = "firstCartId";
        String secondCartId = "secondCartId";
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId(firstCartId).withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(2000))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId(secondCartId).withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(1000))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );


        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("cartId", equalTo(firstCartId)),
                        hasProperty(
                                "deliveries",
                                contains(
                                        hasProperty("promos", contains(
                                                allOf(
                                                        hasProperty("discount",
                                                                comparesEqualTo(DEFAULT_DELIVERY_PRICE)),
                                                        hasProperty("promoType", equalTo(FREE_DELIVERY_THRESHOLD))
                                                ))
                                        )
                                )
                        )
                ),
                allOf(
                        hasProperty("cartId", equalTo(secondCartId)),
                        hasProperty(
                                "deliveries",
                                contains(
                                        hasProperty("promos", contains(
                                                allOf(
                                                        hasProperty("discount",
                                                                comparesEqualTo(DEFAULT_DELIVERY_PRICE)),
                                                        hasProperty("promoType", equalTo(FREE_DELIVERY_THRESHOLD))
                                                ))
                                        )
                                )
                        )
                )
        ));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNotReturnPriceLeftForFreeDeliveryWithYaPlusV2() throws IOException, InterruptedException {
        mockReport(BigDecimal.valueOf(500));

        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.singleton(YANDEX_PLUS),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemV2()))
        );

        assertThat(priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(1999)));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(THRESHOLD_FREE_DELIVERY));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNotReturnPriceLeftForFreeDeliveryWithYaPlusV3() throws IOException, InterruptedException {
        mockReport(BigDecimal.valueOf(499));

        Map<FreeDeliveryReason, PriceLeftForFreeDeliveryResponseV3> priceLeftForFreeDelivery =
                marketLoyaltyClient.calcPriceLeftForFreeDeliveryV3(
                        MOSCOW_REGION,
                        Collections.singleton(YANDEX_PLUS),
                        MarketPlatform.BLUE,
                        UsageClientDeviceType.DESKTOP,
                        new OrderItemsRequest(Collections.singletonList(defaultItemV2()))
                );

        assertThat(priceLeftForFreeDelivery.entrySet(), contains(
                allOf(
                        hasProperty("key", equalTo(THRESHOLD_FREE_DELIVERY)),
                        hasProperty("value", hasProperty("priceLeftForFreeDelivery",
                                comparesEqualTo(BigDecimal.valueOf(2000))))
                )
        ));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnPriceLeftForFreeDeliveryWithYaPlusWhenDropshipV2() throws IOException,
            InterruptedException {
        mockReportWithDefaultAndDropshipItem();

        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.singleton(YANDEX_PLUS),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Arrays.asList(defaultItemV2(), dropshipItemV2()))
        );

        assertThat(
                priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(),
                comparesEqualTo(DEFAULT_THRESHOLD.subtract(BigDecimal.valueOf(DEFAULT_ITEM_PRICE)))
        );
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(THRESHOLD_FREE_DELIVERY));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnZeroPriceLeftForFreeDeliveryV2() throws IOException, InterruptedException {
        mockReport(BigDecimal.valueOf(5000));

        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.emptySet(),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemV2()))
        );

        assertThat(priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(THRESHOLD_FREE_DELIVERY));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNotReturnFreeDeliveryForYaPlusAndDropship() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        String firstCartId = "firstCartId";
        String secondCartId = "secondCartId";
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId(firstCartId).withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(400))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId(secondCartId)
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                dropship(),
                                                price(BigDecimal.valueOf(500))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(2099)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
    }


    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnNoDeliveryIfLargeDimensionCartV2() throws IOException, InterruptedException {
        mockReportWithLargeDimensionCart();

        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.emptySet(),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemV2()))
        );

        assertThat(priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(), is(nullValue()));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(LARGE_SIZED_CART));
        assertThat(priceLeftForFreeDelivery.getStatus(), equalTo(NO_FREE_DELIVERY));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnFreeDeliveryIfLightCartV2() throws IOException, InterruptedException {
        mockReportWithLightCart();

        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.emptySet(),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemV2()))
        );

        assertThat(priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(THRESHOLD_FREE_DELIVERY));
        assertThat(priceLeftForFreeDelivery.getStatus(), equalTo(ALREADY_FREE));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnNoDeliveryIfLargeDimensionCartWithManyItemsV2() throws IOException, InterruptedException {
        mockReportWithLightCart();

        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.emptySet(),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemsV2(21)))
        );

        assertThat(priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(), is(nullValue()));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(LARGE_SIZED_CART));
        assertThat(priceLeftForFreeDelivery.getStatus(), equalTo(NO_FREE_DELIVERY));
    }

    @Test
    public void shouldIgnoreExpiredOffersForPriceLeftForFreeDeliveryV2() {
        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                MOSCOW_REGION,
                Collections.emptySet(),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemV2()))
        );

        //assertThat(priceLeftForFreeDelivery.getPriceLeftForFreeDelivery(), equalTo(BigDecimal.valueOf(2499)));
        assertThat(priceLeftForFreeDelivery.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(THRESHOLD_FREE_DELIVERY));
    }


    @Test
    public void shouldFailOnPriceLeftForFreeDeliveryRequestWithDuplicateItems() {
        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                        (long) AMURSKI_DISTRICT,
                        Collections.emptySet(),
                        MarketPlatform.BLUE,
                        UsageClientDeviceType.DESKTOP,
                        true,
                        new OrderItemsRequest(Arrays.asList(
                                defaultItemV2(),
                                defaultItemV2()
                        ))
                )
        );

        assertThat(exception.getMessage(), startsWith("Duplicate items in request "));
    }

    @Test
    public void shouldYaPlusThresholdReasonInResponse() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 500);
        PriceLeftForFreeDeliveryResponse priceLeftForFreeDelivery = marketLoyaltyClient.calcPriceLeftForFreeDeliveryV2(
                VORONEZH_REGION_LONG,
                Collections.singleton(YANDEX_PLUS),
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(Collections.singletonList(defaultItemV2())),
                DEFAULT_UID
        );

        assertThat(priceLeftForFreeDelivery.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(priceLeftForFreeDelivery.getReason(), equalTo(YA_PLUS_FREE_DELIVERY));
        assertThat(priceLeftForFreeDelivery.getThreshold(), equalTo(BigDecimal.valueOf(500)));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnWillBeFreeWithYaPlusSubscription() {
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(DEFAULT_REGION, 500);
        final MultiCartWithBundlesDiscountResponse multiCartWithBundlesDiscountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder
                                .builder(orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(1000)
                                        )
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery())
                                        .build())
                                .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                                .build()
                );

        assertEquals(2, multiCartWithBundlesDiscountResponse.getDeliveryDiscountMap().size());
        assertThat(multiCartWithBundlesDiscountResponse, allOf(
                hasProperty("freeDeliveryReason", equalTo(THRESHOLD_FREE_DELIVERY)),
                hasProperty("freeDeliveryStatus", equalTo(WILL_BE_FREE_WITH_MORE_ITEMS))
        ));
        assertThat(
                multiCartWithBundlesDiscountResponse,
                hasProperty("deliveryDiscountMap", allOf(
                        hasEntry(
                                equalTo(THRESHOLD_FREE_DELIVERY),
                                allOf(
                                        hasProperty("threshold", comparesEqualTo(BigDecimal.valueOf(2499))),
                                        hasProperty("priceLeftForFreeDelivery",
                                                comparesEqualTo(BigDecimal.valueOf(1499))),
                                        hasProperty("status", equalTo(WILL_BE_FREE_WITH_MORE_ITEMS))
                                )
                        ),
                        hasEntry(
                                equalTo(YA_PLUS_FREE_DELIVERY),
                                allOf(
                                        hasProperty("threshold", comparesEqualTo(BigDecimal.valueOf(500))),
                                        hasProperty("priceLeftForFreeDelivery", comparesEqualTo(BigDecimal.valueOf(0))),
                                        hasProperty("status", equalTo(WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION))
                                )
                        )
                ))
        );
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnWillBeFreeWithYaPlusSubscription2() {
        regionSettingsUtils.setupWelcomeBonusAndYaPlusThresholdForRegion(DEFAULT_REGION, 500);

        var multiCartWithBundlesDiscountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(1000)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withDeliveries(DeliveryRequestUtils.courierDelivery(b -> b.setRegion(213L)))
                                .build())
                        .build()
        );

        assertEquals(2, multiCartWithBundlesDiscountResponse.getDeliveryDiscountMap().size());
        assertThat(multiCartWithBundlesDiscountResponse, allOf(
                hasProperty("freeDeliveryReason", equalTo(THRESHOLD_FREE_DELIVERY)),
                hasProperty("freeDeliveryStatus", equalTo(NO_FREE_DELIVERY))
        ));
        assertThat(
                multiCartWithBundlesDiscountResponse,
                hasProperty("deliveryDiscountMap", allOf(
                        hasEntry(
                                equalTo(THRESHOLD_FREE_DELIVERY),
                                allOf(
                                        hasProperty("threshold", is(nullValue())),
                                        hasProperty("priceLeftForFreeDelivery", is(nullValue())),
                                        hasProperty("status", equalTo(NO_FREE_DELIVERY))
                                )
                        ),
                        hasEntry(
                                equalTo(YA_PLUS_FREE_DELIVERY),
                                allOf(
                                        hasProperty("threshold", comparesEqualTo(BigDecimal.valueOf(500))),
                                        hasProperty("priceLeftForFreeDelivery", comparesEqualTo(BigDecimal.valueOf(0))),
                                        hasProperty("status", equalTo(WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION))
                                )
                        )
                ))
        );
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNotReturnWillBeFreeWithYaPlusSubscriptionForNoAuth() {
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(DEFAULT_REGION, 500);

        final MultiCartWithBundlesDiscountResponse multiCartWithBundlesDiscountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder
                                .builder(orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                warehouse(MARKET_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(1000)
                                        )
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withDeliveries(DeliveryRequestUtils.courierDelivery())
                                        .build())
                                .withOperationContext(OperationContextFactory.yandexUidOperationContext())
                                .build()
                );

        assertEquals(1, multiCartWithBundlesDiscountResponse.getDeliveryDiscountMap().size());
        assertThat(multiCartWithBundlesDiscountResponse, allOf(
                hasProperty("freeDeliveryReason", equalTo(THRESHOLD_FREE_DELIVERY)),
                hasProperty("freeDeliveryStatus", equalTo(WILL_BE_FREE_WITH_MORE_ITEMS))
        ));
        assertThat(
                multiCartWithBundlesDiscountResponse,
                hasProperty("deliveryDiscountMap", allOf(
                        hasEntry(
                                equalTo(THRESHOLD_FREE_DELIVERY),
                                allOf(
                                        hasProperty("threshold", comparesEqualTo(BigDecimal.valueOf(2499))),
                                        hasProperty("priceLeftForFreeDelivery",
                                                comparesEqualTo(BigDecimal.valueOf(1499))),
                                        hasProperty("status", equalTo(WILL_BE_FREE_WITH_MORE_ITEMS))
                                )
                        )
                ))
        );
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldYaPlusDeliveryResponseForAlreadyFreeStatus() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 500);
        String firstCartId = "firstCartId";
        String secondCartId = "secondCartId";
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId(firstCartId).withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(600))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId(secondCartId)
                                        .withOrderItem(
                                                itemKey(ANOTHER_ITEM_KEY),
                                                dropship(),
                                                price(BigDecimal.valueOf(500))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(0)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(YA_PLUS_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
    }

    @Test
    public void shouldReturnYaPlusFreeDeliveryThresholdIfUserPlusAndHaveCoin() throws Exception {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        OrderItemsRequest itemRequest = new OrderItemsRequest(Arrays.asList(
                defaultItemV2(),
                OrderItemsRequest.Item.builder()
                        .setFeedId(DEFAULT_ITEM_KEY.getFeedId())
                        .setOfferId(DEFAULT_ITEM_KEY.getOfferId())
                        .setBundleId("some bundle")
                        .setCount(1)
                        .build()
        ));
        PriceLeftForFreeDeliveryResponse priceLeft = objectMapper.readValue(
                mockMvc.perform(post("/discount/priceLeftForFreeDelivery/v2")
                                .content(objectMapper.writeValueAsString(itemRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff")
                                .queryParam(LoyaltyTag.REGION_ID, Long.toString(VORONEZH_REGION_LONG))
                                .queryParam("perks", "")
                                .queryParam("platform", "BLUE")
                                .queryParam("clientDeviceType", "APPLICATION")
                                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID)))
                        .andReturn().getResponse().getContentAsString(), PriceLeftForFreeDeliveryResponse.class);

        assertThat(priceLeft.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(699)));
        assertThat(priceLeft.getReason(), equalTo(YA_PLUS_FREE_DELIVERY));
        assertThat(priceLeft.getStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
    }

    @Test
    public void shouldReturnCoinFreeDeliveryThresholdIfUserNotPlusAndHaveCoin() throws Exception {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(500));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        OrderItemsRequest itemRequest = new OrderItemsRequest(Arrays.asList(
                defaultItemV2(),
                OrderItemsRequest.Item.builder()
                        .setFeedId(DEFAULT_ITEM_KEY.getFeedId())
                        .setOfferId(DEFAULT_ITEM_KEY.getOfferId())
                        .setBundleId("some bundle")
                        .setCount(1)
                        .build()
        ));
        PriceLeftForFreeDeliveryResponse priceLeft = objectMapper.readValue(
                mockMvc.perform(post("/discount/priceLeftForFreeDelivery/v2")
                                .content(objectMapper.writeValueAsString(itemRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff")
                                .queryParam(LoyaltyTag.REGION_ID, Long.toString(VORONEZH_REGION_LONG))
                                .queryParam("perks", "")
                                .queryParam("platform", "BLUE")
                                .queryParam("clientDeviceType", "APPLICATION")
                                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID)))
                        .andReturn().getResponse().getContentAsString(), PriceLeftForFreeDeliveryResponse.class);

        assertThat(priceLeft.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(500)));
        assertThat(priceLeft.getReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(priceLeft.getStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
    }

    @Test
    public void shouldReturnCoinFreeDeliveryThresholdIfUserNotPlusAndHaveCoinViaCalc() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 500);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(500))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
        assertEquals(discountResponse.getUnusedCoins().size(), 0);
        discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(700))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(0)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
        assertEquals(discountResponse.getUnusedCoins().size(), 0);
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnYaPlusFreeDeliveryThresholdIfUserYaPlusAndHaveCoin() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(900))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(0)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(YA_PLUS_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnRegionFreeDeliveryThresholdIfUserNotYaPlusAndNotHaveCoin() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(500))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(1999)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
    }


    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnYaFreeDeliveryThresholdIfUserYaPlusAndNotHaveCoin() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        regionSettingsService.reloadCache();
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(500))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(199)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(YA_PLUS_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
    }

    @Test
    public void shouldReturnCoinFreeDeliveryThresholdIfUserYaPlusAndHaveCoinWithBiggerMinTotal() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(500));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(400))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));

        discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(501))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(0)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
    }

    @Test
    public void shouldReturnFreeDeliveryByCoinIfUserNotYaPlusHaveCoin() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        Coin freeDeliveryCoin = createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsService.reloadCache();
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(700))
                                        )
                                        .withDeliveries(courierDelivery(withRegion((long) CHITA_REGION)))
                                        .build()
                        )
                        .withCoins(freeDeliveryCoin.getCoinKey())
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(0)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
    }

    @Test
    public void shouldReturnFreeDeliveryByCoinIfUserNotYaPlusHaveCoinAndPriceMoreCoinThreshold() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        Coin freeDeliveryCoin = createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsService.reloadCache();
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(700))
                                        )
                                        .withDeliveries(courierDelivery(withRegion((long) CHITA_REGION)))
                                        .build()
                        )
                        .withCoins(freeDeliveryCoin.getCoinKey())
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(0)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
    }


    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldCorrectParseNullUid() throws Exception {
        mockReportWithDefaultAndDropshipItem();

        OrderItemsRequest itemsRequest = new OrderItemsRequest(Collections.singletonList(defaultItemV2()));
        PriceLeftForFreeDeliveryResponse priceLeft = objectMapper.readValue(
                mockMvc.perform(post("/discount/priceLeftForFreeDelivery/v2")
                                .content(objectMapper.writeValueAsString(itemsRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff")
                                .queryParam(LoyaltyTag.REGION_ID, Long.toString(MOSCOW_REGION))
                                .queryParam("perks", "")
                                .queryParam("platform", "BLUE")
                                .queryParam("clientDeviceType", "DESKTOP"))
                        .andReturn().getResponse().getContentAsString(), PriceLeftForFreeDeliveryResponse.class);

        assertThat(priceLeft.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(2299)));
        assertThat(priceLeft.getReason(), equalTo(THRESHOLD_FREE_DELIVERY));
    }

    @Test
    public void shouldReturnCoinFreeDeliveryThresholdIfUserYaPlusAndHaveCoinWithAndUseMinOrderTotal() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromoOne = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(500));
        Promo freeDeliveryCoinPromoTwo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(
                BigDecimal.valueOf(600),
                ANOTHER_PROMO_KEY
        );
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromoOne.getId() + "," + freeDeliveryCoinPromoTwo.getId()
        );
        createCoin(freeDeliveryCoinPromoOne, defaultAuth(DEFAULT_UID));
        createCoin(freeDeliveryCoinPromoTwo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(450))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(50)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
        discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(510))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(0)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnRegionFreeDeliveryThresholdIfUserNotYaPlusAndNotHaveCoinButHavePromo() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(
                BigDecimal.valueOf(500));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryPromo.getId()
        );
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(500))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(1999)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
    }

    @Test
    public void shouldNotGetCoinForCartFreeDeliveryCoinForExcludedAlcoholHidsv2ForRegionTier3() throws IOException,
            InterruptedException {
        Promo promo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        createCoin(promo, defaultAuth(DEFAULT_UID));
        mockReport(DEFAULT_ITEM_KEY, ALCOHOL_CATEGORY);
        final CoinsForCart response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                CHITA_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 1))
                )
        );

        assertThat(
                response,
                allOf(
                        hasProperty(
                                "applicableCoins",
                                empty()
                        )
                )
        );
    }


    @Test
    public void shouldReturnCoinFreeDeliveryThresholdIfUserNotYaPlusHaveCoinAndPriceMoreCoinThresholdButHaveAlcohol() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(300)),
                                                categoryId(ALCOHOL_CATEGORY),
                                                itemKey(ANOTHER_ITEM_KEY)
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId("secondCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(500)),
                                                itemKey(DEFAULT_ITEM_KEY)
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(199)));
    }

    @Test
    public void shouldReturnCoinFreeDeliveryThresholdIfUserNotYaPlusHaveCoinAndPriceMoreCoinThresholdButHaveDSBSItem() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(300)),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                atSupplierWarehouse(true),
                                                dropship(true)
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId("secondCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(500)),
                                                itemKey(DEFAULT_ITEM_KEY)
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(199)));
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldNoFreeDeliveryIfUserYaPlusHaveCoinAndPriceMoreYaPlusThresholdButHaveDSBSItem() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(DEFAULT_REGION, 699);
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId")
                                        .withWeight(ZERO)
                                        .withOrderItem(
                                                categoryId(543488),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(2990)),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                atSupplierWarehouse(false),
                                                dropship(true),
                                                warehouse(null),
                                                downloadable(false)
                                        )
                                        .withDeliveries(courierDelivery(withRegion(213L)))
                                        .build()
                        )
                        .withPlatform(MarketPlatform.WHITE)
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(DROPSHIP_ONLY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertNull(discountResponse.getPriceLeftForFreeDelivery());
    }

    @Test
    public void shouldReturnNoFreeDeliveryThresholdIfUserHaveCoinAndOnlyAlcoholAndPharmaItem() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(VORONEZH_REGION_INT, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(300)),
                                                categoryId(ALCOHOL_CHILD_CATEGORY),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                specs(ofSpecs(
                                                        "spec", "medicine",
                                                        "spec", "ethanol"
                                                ))
                                        )
                                        .withDeliveries(freePickupDelivery())
                                        .build(),
                                orderRequestBuilder()
                                        .withCartId("secondCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(300)),
                                                categoryId(PHARMA_CHILD_CATEGORY),
                                                dropship(true),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                specs(ofSpecs(
                                                        "spec", "medicine",
                                                        "spec", "prescription"
                                                ))
                                        )
                                        .withDeliveries(freePickupDelivery())
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertNull(discountResponse.getPriceLeftForFreeDelivery());
    }

    @Test
    public void shouldNotGiveMulticartDiscountForExpressDelivery() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(BigDecimal.valueOf(5000))
                                )
                                .withDeliveries(courierDelivery(
                                        withFeatures(Set.of(DeliveryFeature.EXPRESS)),
                                        withPrice(BigDecimal.valueOf(350))
                                ))
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(BigDecimal.valueOf(300))
                                )
                                .withDeliveries(pickupDelivery())
                                .build(),
                        orderRequestBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(BigDecimal.valueOf(300))
                                )
                                .withDeliveries(courierDelivery())
                                .build()
                ).build()
        );

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE_HALF, MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", contains(deliveryPromo(DEFAULT_DELIVERY_PRICE_HALF, MULTICART_DISCOUNT)))
                        )
                )),
                hasProperty("deliveries", contains(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", empty())
                        )
                ))
        ));
    }

    @Test
    public void shouldNoReturnCoinsIfCoinApplicableNotValid() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        Coin coin = createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsUtils.setupWelcomeBonusAndThresholdsForRegion(DEFAULT_REGION, 699);
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("secondCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(300)),
                                                itemKey(ANOTHER_ITEM_KEY)
                                        )
                                        .withDeliveries(courierDelivery(withRegion(213L)))
                                        .build()
                        )
                        .withCoins(Collections.singletonList(coin.getCoinKey()))
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_THRESHOLD_FREE_DELIVERY));
        assertEquals(discountResponse.getCoins().size(), 1);
        assertEquals(discountResponse.getCoinErrors().size(), 1);
        Long coinId = discountResponse.getCoins().get(0).getId();
        Long coinErrorId = discountResponse.getCoinErrors().get(0).getCoin().getId();
        assertEquals(coinId, coinErrorId);
    }

    @Test
    public void shouldShowFreeDeliveryCoinInCaseNotYaPlusAndHaveYaPlusThresholdConfig() throws IOException, InterruptedException {
        FoundOffer foundOffer = mockReport(BigDecimal.valueOf(111));
        foundOffer.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.YANDEX_MARKET.getCode()));
        foundOffer.setCargoTypes(Collections.singleton(950));
        foundOffer.setSupplierId(10457672L);

        regionSettingsUtils.setupThresholdsForRegion(BigDecimal.valueOf(299), DEFAULT_REGION);

        coinService.create.createCoin(
                createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(299)),
                defaultAuth().build()
        );

        var response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                DEFAULT_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 3))
                )
        );

        assertEquals("Applicable coins map size != 1", 1, response.getApplicableCoins().size());
    }

    @Test
    public void shouldNotShowFreeDeliveryCoinInCaseYaPlusAndHaveYaPlusThresholdConfig() throws IOException, InterruptedException {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        var price = 333;
        FoundOffer foundOffer = mockReport(BigDecimal.valueOf(price));
        foundOffer.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.YANDEX_MARKET.getCode()));
        foundOffer.setCargoTypes(Collections.singleton(950));
        foundOffer.setSupplierId(10457672L);

        regionSettingsUtils.setupThresholdsForRegion(BigDecimal.valueOf(699), DEFAULT_REGION);

        var coin = coinService.create.createCoin(
                createFreeDeliveryCoinPromo(),
                defaultAuth().build()
        );

        var response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                DEFAULT_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 3))
                )
        );

        assertThat(
                response,
                hasProperty(
                        "disabledCoins",
                        hasEntry(equalTo(FREE_DELIVERY_THRESHOLD_RESTRICTION), contains(any(UserCoinResponse.class)))
                )
        );

        var discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("secondCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.valueOf(3)),
                                                price(price),
                                                itemKey(DEFAULT_ITEM_KEY)
                                        )
                                        .withDeliveries(pickupDelivery(withRegion(DEFAULT_REGION), withPrice(BigDecimal.valueOf(99L))))
                                        .build()
                        )
                        .withCoins(Collections.singletonList(coin))
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(COIN_FREE_DELIVERY));
        assertEquals(1, discountResponse.getCoins().size());
        assertEquals(0, discountResponse.getCoinErrors().size());
    }

    @Test
    @Ignore
    public void fullDisabledDiscountRule() throws IOException, InterruptedException {
        FoundOffer foundOffer = mockReport(BigDecimal.valueOf(111));
        foundOffer.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.YANDEX_MARKET.getCode()));
        foundOffer.setCargoTypes(Collections.singleton(950));
        foundOffer.setSupplierId(10457672L);
        Promo promo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        CoinKey coin = coinService.create.createCoin(promo, defaultAuth().build());
        regionSettingsService.saveOrUpdateRegionSettings(RegionSettings.builder()
                .withWelcomeBonusEnabledValue(true)
                .withYandexPlusThresholdEnabled(PropertyStateType.ENABLED)
                .withYandexPlusThresholdValue(BigDecimal.valueOf(299))
                .withThresholdValue(BigDecimal.valueOf(599))
                .withEnabledThreshold()
                .withRegionId(43)
                .build());
        regionSettingsService.reloadCache();
        final CoinsForCart response = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                43,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 3))
                )
        );
        assertEquals(1, response.getApplicableCoins().size());

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("secondCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.valueOf(3)),
                                                price(BigDecimal.valueOf(111)),
                                                itemKey(ANOTHER_ITEM_KEY)
                                        )
                                        .withDeliveries(pickupDelivery(withRegion(43L), withPrice(BigDecimal.valueOf(99L))))
                                        .build()
                        )
                        .withCoins(Collections.singletonList(coin))
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(REGION_WITHOUT_THRESHOLD));
        assertEquals(1, discountResponse.getCoins().size());
        assertEquals(1, discountResponse.getCoinErrors().size());
    }


    @Test
    public void disabledDiscountRule() throws IOException, InterruptedException {
        FoundOffer foundOffer = mockReport(BigDecimal.valueOf(1111));
        foundOffer.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.YANDEX_MARKET.getCode()));
        foundOffer.setCargoTypes(Collections.singleton(950));
        foundOffer.setSupplierId(10457672L);

        Promo promo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(2999));
        configurationService.set(
                ConfigurationService.DELIVERY_UNIFIED_TARIFF_PERSONAL_COIN_ENABLED,
                false
        );
        CoinKey coin = coinService.create.createCoin(promo, defaultAuth().build());

        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        var coinsForCart = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                DEFAULT_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 3))
                )
        );

        assertEquals("Applicable coins map size != 1", 1, coinsForCart.getApplicableCoins().size());

        var discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("secondCartId")
                                        .withOrderItem(
                                                quantity(BigDecimal.valueOf(3)),
                                                price(BigDecimal.valueOf(1111)),
                                                itemKey(ANOTHER_ITEM_KEY)
                                        )
                                        .withDeliveries(pickupDelivery(
                                                withRegion(DEFAULT_REGION),
                                                withPrice(BigDecimal.valueOf(99L)))
                                        )
                                        .build()
                        )
                        .withCoins(Collections.singletonList(coin))
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(REGION_WITHOUT_THRESHOLD));
        assertEquals(1, discountResponse.getCoins().size());
        assertEquals(0, discountResponse.getCoinErrors().size());
    }

    @Test
    public void shouldNotReturnCoinsForDbs() throws IOException, InterruptedException {
        FoundOffer foundOffer = mockReportWithDbs(DEFAULT_ITEM_KEY, ANOTHER_CATEGORY_ID, BigDecimal.valueOf(1111));
        foundOffer.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.YANDEX_MARKET.getCode()));
        foundOffer.setCargoTypes(Collections.singleton(950));
        foundOffer.setSupplierId(10457672L);
        foundOffer.setRgb(Color.WHITE);

        Promo promo = createFreeDeliveryCoinPromoWithNoDbsRule();

        configurationService.set(
                ConfigurationService.DELIVERY_UNIFIED_TARIFF_PERSONAL_COIN_ENABLED,
                false
        );

        coinService.create.createCoin(promo, defaultAuth().build());

        regionSettingsUtils.setupDefaultThresholdsForDefaultRegion();

        var coinsForCart = marketLoyaltyClient.getCoinsForCartV2(
                DEFAULT_UID,
                DEFAULT_REGION,
                false,
                MarketPlatform.BLUE,
                UsageClientDeviceType.DESKTOP,
                new OrderItemsRequest(
                        Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 3))
                )
        );

        assertEquals("Applicable coins map size != 0", 0, coinsForCart.getApplicableCoins().size());
    }

    @Test
    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    public void shouldReturnRegionFreeDeliveryThresholdIfUserNotPlusAndHaveCoin() {
        configurationService.set(
                ConfigurationService.THRESHOLD_EXP_ENABLE_REARR, "market_blue_tariffs_exp=new_min_tariff");
        Promo freeDeliveryCoinPromo = createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal.valueOf(699));
        configurationService.set(
                ConfigurationService.FREE_DELIVERY_PROMO_IDS,
                freeDeliveryCoinPromo.getId()
        );
        createCoin(freeDeliveryCoinPromo, defaultAuth(DEFAULT_UID));
        regionSettingsService.saveOrUpdateRegionSettings(RegionSettings.builder()
                .withWelcomeBonusEnabledValue(true)
                .withYandexPlusThresholdEnabled(PropertyStateType.ENABLED)
                .withYandexPlusThresholdValue(BigDecimal.valueOf(500))
                .withThresholdValue(DEFAULT_THRESHOLD)
                .withEnabledThreshold()
                .withRegionId(VORONEZH_REGION_INT)
                .build());
        regionSettingsService.reloadCache();
        HttpHeaders header = new HttpHeaders();
        header.add("X-Market-Rearrfactors", "market_blue_tariffs_exp=new_min_tariff");
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(

                DiscountRequestBuilder.builder(
                                orderRequestBuilder()
                                        .withCartId("firstCartId").withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(9999))
                                        )
                                        .withDeliveries(courierDelivery(withRegion(VORONEZH_REGION_LONG)))
                                        .build()
                        )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
                , header
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(ZERO));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(ALREADY_FREE));
        assertEquals(discountResponse.getUnusedCoins().size(), 0);
    }

    private static Matcher<DeliveryResponse> emptyDeliveryDiscounts(String s) {
        return allOf(
                hasProperty("id", equalTo(s)),
                hasProperty("promos", is(empty()))
        );
    }

    private static Matcher<IdObject> coinIdObject(Coin coin) {
        return equalTo(new IdObject(coin.getCoinKey().getId()));
    }

    private Coin createCoin(Promo promo, CoinInsertRequest.Builder builder) {
        return coinService.search.getCoin(coinService.create.createCoin(promo, builder.build())).orElseThrow(
                AssertionError::new);
    }

    private Promo createFreeDeliveryCoinPromo() {
        BigDecimal initialBudget = BigDecimal.valueOf(5000);
        return promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery().setBudget(initialBudget)
        );
    }

    private Promo createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal minOrderTotal) {
        Promo activePromo = promoManager.createSmartShoppingPromo(
                defaultFreeDeliveryWithMinOrderTotalRule(minOrderTotal)
        );
        promoUtils.reloadFreeDeliveryPromosCache();
        return activePromo;
    }

    private Promo createFreeDeliveryCoinPromoWithMinOrderTotalRule(BigDecimal minOrderTotal, String promoKey) {
        Promo activePromo = promoManager.createSmartShoppingPromo(
                defaultFreeDeliveryWithMinOrderTotalRule(minOrderTotal).setPromoKey(promoKey)
        );
        promoUtils.reloadFreeDeliveryPromosCache();
        return activePromo;
    }

    private Promo createFreeDeliveryCoinPromoWithNoDbsRule() {
        Promo activePromo = promoManager.createSmartShoppingPromo(
                defaultFreeDeliveryWithNoDbsRule()
        );
        promoUtils.reloadFreeDeliveryPromosCache();
        return activePromo;
    }

    private static Matcher<Object> deliveryPromo(Coin coin, BigDecimal deliveryDiscount) {
        return allOf(
                hasProperty("discount", comparesEqualTo(deliveryDiscount)),
                hasProperty("promoType", equalTo(PromoType.SMART_SHOPPING)),
                hasProperty("promoKey", equalTo(coin.getPromoKey())),
                hasProperty("usedCoin", hasProperty("id", equalTo(coin.getCoinKey().getId())))
        );
    }

    private static Matcher<Object> freeDeliveryAddressPromo(BigDecimal deliveryDiscount) {
        return allOf(
                hasProperty("discount", comparesEqualTo(deliveryDiscount)),
                hasProperty("promoType", equalTo(PromoType.FREE_DELIVERY_ADDRESS))
        );
    }

    private static Matcher<Object> deliveryPromo(BigDecimal deliveryDiscount, PromoType promoType) {
        return allOf(
                hasProperty("discount", comparesEqualTo(deliveryDiscount)),
                hasProperty("promoType", equalTo(promoType)),
                hasProperty("promoKey", is(nullValue())),
                hasProperty("usedCoin", is(nullValue()))
        );
    }

    private static OrderItemsRequest.Item defaultItemV2() {
        return defaultItemsV2(1);
    }

    private static OrderItemsRequest.Item defaultItemsV2(int count) {
        return makeItem(DEFAULT_ITEM_KEY, count);
    }

    private static OrderItemsRequest.Item dropshipItemV2() {
        return OrderItemsRequest.Item.builder()
                .setFeedId(ANOTHER_ITEM_KEY.getFeedId())
                .setOfferId(ANOTHER_ITEM_KEY.getOfferId())
                .setCount(1)
                .build();
    }

    private FoundOffer mockReport(BigDecimal price) throws IOException, InterruptedException {
        FoundOffer fo = new FoundOffer();
        fo.setFeedId(DEFAULT_ITEM_KEY.getFeedId());
        fo.setShopOfferId(DEFAULT_ITEM_KEY.getOfferId());
        fo.setPrice(price);
        fo.setHyperCategoryId(1);
        fo.setWeight(BigDecimal.ONE);

        reportMockUtils.mockReportService(fo);
        return fo;
    }

    private void mockReportWithDefaultAndDropshipItem() throws IOException, InterruptedException {
        FoundOffer fo = new FoundOffer();
        fo.setFeedId(DEFAULT_ITEM_KEY.getFeedId());
        fo.setShopOfferId(DEFAULT_ITEM_KEY.getOfferId());
        fo.setPrice(BigDecimal.valueOf(DEFAULT_ITEM_PRICE));
        fo.setHyperCategoryId(1);
        fo.setWeight(BigDecimal.ONE);

        FoundOffer dropshipFo = new FoundOffer();
        dropshipFo.setFeedId(ANOTHER_ITEM_KEY.getFeedId());
        dropshipFo.setShopOfferId(ANOTHER_ITEM_KEY.getOfferId());
        dropshipFo.setPrice(BigDecimal.valueOf(500));
        dropshipFo.setHyperCategoryId(1);
        dropshipFo.setDeliveryPartnerTypes(Collections.singletonList("SHOP"));
        dropshipFo.setWeight(BigDecimal.ONE);

        reportMockUtils.mockReportService(fo, dropshipFo);
    }

    private void mockReportWithLargeDimensionCart() throws IOException, InterruptedException {
        FoundOffer fo = new FoundOffer();
        fo.setFeedId(DEFAULT_ITEM_KEY.getFeedId());
        fo.setShopOfferId(DEFAULT_ITEM_KEY.getOfferId());
        fo.setPrice(BigDecimal.valueOf(5000));
        fo.setHyperCategoryId(1);
        fo.setWeight(BigDecimal.valueOf(21.0));

        reportMockUtils.mockReportService(fo);
    }

    private void mockReportWithLightCart() throws IOException, InterruptedException {
        FoundOffer fo = new FoundOffer();
        fo.setFeedId(DEFAULT_ITEM_KEY.getFeedId());
        fo.setShopOfferId(DEFAULT_ITEM_KEY.getOfferId());
        fo.setPrice(BigDecimal.valueOf(5000));
        fo.setHyperCategoryId(1);
        fo.setWeight(BigDecimal.ONE);

        reportMockUtils.mockReportService(fo);
    }
}
