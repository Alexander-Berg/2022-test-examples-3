package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.CartUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.PromoType.GENERIC_BUNDLE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.rule.RuleType.DONT_USE_WITH_BUNDLES_FILTER_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_APPLICABILITY_POLICY;
import static ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy.BOTH;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictBerubonus;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictPromocode;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

public class DiscountControllerCoinsWithPromoBundlesBothPolicyApplicabilityTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final String BUNDLE_PROMO_KEY = "some promo bundle";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String GIFT_ITEM_SSKU = "some gift offer";
    private static final String SOME_SSKU = "some offer";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private PromoBundleService bundleService;

    @Before
    public void prepare() {
        configurationService.set(
                PROMO_APPLICABILITY_POLICY,
                BOTH
        );
    }

    @Test
    public void shouldNotApplyCoinWhenRestrictedOnBundle() {
        CoinKey expectedCoin = createCoin();
        String expectedPromoBundle = generateBundleIdWithRestrictions();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        price(15000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SOME_SSKU),
                        ssku(SOME_SSKU),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(discountResponse.getUnusedCoins(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14999)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SOME_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldNotApplyCoinWhenRestrictedOnCoin() {
        CoinKey expectedCoin = createCoinWithRestrictions();
        String expectedPromoBundle = generateBundle();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        price(15000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SOME_SSKU),
                        ssku(SOME_SSKU),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(discountResponse.getUnusedCoins(), empty());
        assertThat(discountResponse.getCoinErrors(), empty());
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", contains(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14999)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SOME_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyCoinWhenNoRestrictions() {
        CoinKey expectedCoin = createCoin();
        String expectedBundle = generateBundle();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        price(15000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SOME_SSKU),
                        ssku(SOME_SSKU),
                        price(15000)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(discountResponse.getUnusedCoins(), is(empty()));
        assertThat(discountResponse.getCoinErrors(), is(empty()));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedBundle)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                ),
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14999)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(SOME_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItem(
                                hasProperty(
                                        "usedCoin",
                                        hasProperty("id", is(expectedCoin.getId()))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyCoinForItemsWhichNotInBundleWhenRestrictedOnBundle() {
        CoinKey expectedCoin = createCoin();
        String expectedPromoBundle = generateBundleIdWithRestrictions();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(discountResponse.getUnusedCoins(), is(empty()));
        assertThat(discountResponse.getCoinErrors(), is(empty()));

        final OrderWithBundlesResponse orderResponse = firstOrderOf(discountResponse);
        assertThat(orderResponse.getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(orderResponse.getItems(), hasSize(3));
        assertThat(orderResponse.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", contains(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(nullValue())),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", contains(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14999)))
                                )
                        ))
                )
        ));
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

    private CoinKey createCoinWithRestrictions() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .addCoinRule(RuleContainer.builder(DONT_USE_WITH_BUNDLES_FILTER_RULE))
        );

        return coinService.create.createCoin(promo, defaultAuth().build());
    }

    private String generateBundleIdWithRestrictions() {
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(BUNDLE_PROMO_KEY),
                        shopPromoId(BUNDLE_PROMO_KEY),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(10)),
                        restrictions(
                                restrictBerubonus(),
                                restrictPromocode()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                ));

        return CartUtils.generateBundleId(bundleDescription, FEED_ID, PROMO_ITEM_SSKU, GIFT_ITEM_SSKU);
    }

    private String generateBundle() {
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(BUNDLE_PROMO_KEY),
                        shopPromoId(BUNDLE_PROMO_KEY),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                ));

        return CartUtils.generateBundleId(bundleDescription, FEED_ID, PROMO_ITEM_SSKU, GIFT_ITEM_SSKU);
    }
}
