package ru.yandex.market.loyalty.back.controller.promo.external;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundle;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.loyalty.api.model.PromoType.MARKET_COUPON;
import static ru.yandex.market.loyalty.api.model.PromoType.SMART_SHOPPING;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.utils.CartUtils.generateBundleId;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.discount;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.cheapestAsGift;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.quantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.withQuantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

@TestFor(DiscountController.class)
public class DiscountControllerExternalPromoCompatibilityTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final String COUPON_CODE = "SOME COUPON CODE";
    private static final BigDecimal INITIAL_CURRENT_BUDGET = BigDecimal.valueOf(700);
    private static final String BUNDLE = "some bundle";
    private static final String CHEAPEST_AS_GIFT = "cheapest";
    private static final String EXTERNAL_PROMO = "price drop as you shop";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String PROMO_ITEM_OFFER_ID = FEED_ID + "." + PROMO_ITEM_SSKU;
    private static final String GIFT_ITEM_SSKU = "some gift offer";
    private static final String GIFT_ITEM_OFFER_ID = FEED_ID + "." + GIFT_ITEM_SSKU;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoBundleService bundleService;

    private PromoBundleDescription genericBundlePromoDescription;

    @Before
    public void prepare() {
        genericBundlePromoDescription = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE),
                shopPromoId(BUNDLE),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                PromoBundleUtils.item(
                        condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                        primary()
                ),
                PromoBundleUtils.item(
                        condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                )
        ));

        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(CHEAPEST_AS_GIFT),
                shopPromoId(CHEAPEST_AS_GIFT),
                strategy(PromoBundleStrategy.CHEAPEST_AS_GIFT),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                withQuantityInBundle(2),
                PromoBundleUtils.item(
                        condition(cheapestAsGift(FeedSskuSet.of(FEED_ID, List.of(PROMO_ITEM_SSKU, GIFT_ITEM_SSKU)))),
                        primary(),
                        quantityInBundle(2)
                )
        ));
    }

    @Test
    public void shouldUseExternalPromo() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 400),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 200),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), empty());

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(400)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldUseGenericBundlePromoWithExternalDiscount() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 200),
                        price(1000)
                )
                .withOrderId(null)
                .build();

        String bundleId = generateBundleId(genericBundlePromoDescription, FEED_ID,
                PROMO_ITEM_OFFER_ID, GIFT_ITEM_OFFER_ID
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), not(empty()));
        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));

        assertItemsQuantity(PROMO_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(799)))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldUseCheapestAsGiftPromoWithExternalDiscount() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(CHEAPEST_AS_GIFT),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        promoKeys(CHEAPEST_AS_GIFT),
                        discount(EXTERNAL_PROMO, 200),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), not(empty()));
        assertItemsQuantity(PROMO_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(CHEAPEST_AS_GIFT)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(926)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(CHEAPEST_AS_GIFT)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(74)))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldUseCoinWithExternalDiscount() {
        CoinKey expectedCoin = createCoin();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 200),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 200),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(firstOrderOf(discountResponse).getBundles(), empty());
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoType", is(SMART_SHOPPING)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(278)))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoType", is(SMART_SHOPPING)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(22)))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldUseCoinWithMinimalCostWithExternalDiscount() {
        CoinKey expectedCoin = createCoinWithMinimalTotalCost(10500);

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 500),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 500),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(firstOrderOf(discountResponse).getBundles(), empty());
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoType", is(SMART_SHOPPING)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(285)))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(500)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoType", is(SMART_SHOPPING)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(15)))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(500)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldNotUseCoinWithMinimalCostViolation() {
        CoinKey expectedCoin = createCoinWithMinimalTotalCost(11001);

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 500),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 500),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(firstOrderOf(discountResponse).getBundles(), empty());
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(500)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(500)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldUseExternalPromoWithCoupon() {
        createCouponPromo();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 200),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        discount(EXTERNAL_PROMO, 200),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .withCoupon(COUPON_CODE)
                        .build());

        assertThat(firstOrderOf(discountResponse).getBundles(), empty());
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoType", is(MARKET_COUPON)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(278)))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoType", is(MARKET_COUPON)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(22)))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(EXTERNAL_PROMO)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
                                )
                        ))
                )
        ));
    }

    private static void assertItemsQuantity(
            String offerId,
            long itemQuantity,
            OrderWithBundlesResponse response
    ) {
        assertThat(response.getItems().stream()
                .filter(item -> item.getOfferId().equals(offerId))
                .map(BundledOrderItemResponse::getQuantity)
                .mapToLong(BigDecimal::longValue)
                .sum(), comparesEqualTo(itemQuantity));
    }

    private static void assertItemsMultiplicityInBundle(
            OrderWithBundlesResponse response
    ) {

        Map<ItemKey, BundledOrderItemResponse> bundledOrderItems = response.getItems().stream()
                .collect(toMap(item ->
                        ItemKey.withBundle(item.getFeedId(), item.getOfferId(), ItemKey.SINGLE_CART_ID, null,
                                item.getBundleId()
                        ), Function.identity()));

        for (OrderBundle bundle : response.getBundles()) {
            bundle.getItems().forEach(bundleItem -> {
                BundledOrderItemResponse bundledOrderItem = bundledOrderItems.get(ItemKey.withBundle(
                        bundleItem.getFeedId(),
                        bundleItem.getOfferId(),
                        ItemKey.SINGLE_CART_ID,
                        null,
                        bundle.getBundleId()
                ));

                assertNotNull(bundledOrderItem);
                assertNotNull(bundleItem);
                assertEquals(
                        0,
                        bundledOrderItem.getQuantity().longValue() % bundleItem.getCountInBundle()
                );
            });
        }
    }

    private CoinKey createCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        return coinService.create.createCoin(promo, defaultAuth().build());
    }

    private CoinKey createCoinWithMinimalTotalCost(Number minimalCost) {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(minimalCost.floatValue())
                )
        );

        return coinService.create.createCoin(promo, defaultAuth().build());
    }

    private void createCouponPromo() {
        promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setPlatform(CoreMarketPlatform.BLUE)
                        .setCouponCode(COUPON_CODE)
                        .setBudget(INITIAL_CURRENT_BUDGET)
        );
    }
}
