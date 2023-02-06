package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.PromoType.GENERIC_BUNDLE;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CartUtils.generateBundleId;
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
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.fixedPrice;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

public class DiscountControllerCoinsWithPromoBundlesTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final long FEED_ID = 123;
    private static final String BUNDLE_PROMO_KEY = "some promo bundle";
    private static final String BUNDLE_PROMO_KEY_FIXED = "some promo bundle with fixed price";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String GIFT_ITEM_SSKU = "some gift offer";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoBundleService bundleService;

    private CoinKey expectedCoin;
    private String expectedPromoBundle;
    private String expectedPromoBundleWithFixedPrice;

    @Before
    public void prepare() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setStartDate(toDate(LocalDate.now()))
                        .setEndDate(toDate(LocalDate.now().plusDays(1)))
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        expectedCoin = coinService.create.createCoin(promo, defaultAuth().build());

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

        PromoBundleDescription bundleDescriptionFixed = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        promoKey(BUNDLE_PROMO_KEY_FIXED),
                        shopPromoId(BUNDLE_PROMO_KEY_FIXED),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(10)),
                        primaryItem(FEED_ID, PROMO_ITEM_SSKU),
                        giftItem(FEED_ID, directionalMapping(
                                when(PROMO_ITEM_SSKU),
                                then(GIFT_ITEM_SSKU),
                                fixedPrice(100)
                        ))
                ));

        expectedPromoBundle = generateBundleId(bundleDescription, FEED_ID, PROMO_ITEM_SSKU, GIFT_ITEM_SSKU);
        expectedPromoBundleWithFixedPrice = generateBundleId(bundleDescriptionFixed,
                FEED_ID, PROMO_ITEM_SSKU, GIFT_ITEM_SSKU
        );
    }

    @Test
    public void shouldApplyPromoBundleWithCoinTogether() {
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
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
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

    @Test
    public void shouldApplyPromoBundleWithFixedDiscountWithCoinTogether() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY_FIXED),
                        price(100000)
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
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundleWithFixedPrice)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                DEFAULT_COIN_FIXED_NOMINAL
                                        ))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundleWithFixedPrice)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.ZERO
                                        ))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14900)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyPromoBundleWithCoinOnPrimaryItemSplitting() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        quantity(2),
                        price(100000)
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
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                ),
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.valueOf(149)
                                        ))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.valueOf(151)
                                        ))
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

    @Test
    public void shouldApplyPromoBundleWithFixedDiscountWithCoinOnPrimaryItemSplitting() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY_FIXED),
                        quantity(2),
                        price(100000)
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
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundleWithFixedPrice)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
                                ),
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.valueOf(149)
                                        ))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.valueOf(151)
                                        ))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundleWithFixedPrice)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.ZERO
                                        ))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14900)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyPromoBundleWithCoinOnSecondaryItemSplitting() {
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
                        quantity(2),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundle)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.ONE))
                                ),
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.valueOf(261)
                                        ))
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
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.valueOf(39)
                                        ))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldApplyPromoBundleWithFixedDiscountWithCoinOnSecondaryItemSplitting() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SSKU),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY_FIXED),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        quantity(2),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withCoins(expectedCoin)
                        .build());

        assertThat(discountResponse.getCoins(), hasItem(
                hasProperty("id", is(expectedCoin.getId()))
        ));
        assertThat(firstOrderOf(discountResponse).getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED))
        ));

        assertThat(firstOrderOf(discountResponse).getItems(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundleWithFixedPrice)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
                                ),
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.valueOf(261)
                                        ))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", is(expectedPromoBundleWithFixedPrice)),
                        hasProperty("promos", hasSize(2)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.ZERO
                                        ))
                                ),
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY_FIXED)),
                                        hasProperty("promoType", is(GENERIC_BUNDLE)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14900)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("usedCoin", hasProperty("id", is(expectedCoin.getId()))),
                                        hasProperty("discount", comparesEqualTo(
                                                BigDecimal.valueOf(39)
                                        ))
                                )
                        ))
                )
        ));
    }
}
