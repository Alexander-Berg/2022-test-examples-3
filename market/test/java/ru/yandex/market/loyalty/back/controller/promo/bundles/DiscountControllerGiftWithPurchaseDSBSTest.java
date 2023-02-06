package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CartUtils.generateBundleId;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.bundle;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;

@TestFor(DiscountController.class)
public class DiscountControllerGiftWithPurchaseDSBSTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 859907;
    private static final String BUNDLE = "some bundle";
    private static final String BUNDLE_PROMO_KEY = "some promo";
    private static final String SHOP_PROMO_ID = "shop promo id";
    private static final String OFFER_1 = "offer 1";
    private static final String OFFER_2 = "offer 2";
    private static final String OFFER_3 = "offer 3";

    @Autowired
    private PromoBundleService bundleService;

    private PromoBundleDescription promoBundleDescription;

    @Before
    public void prepare() {
        promoBundleDescription = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, OFFER_1, OFFER_2),
                giftItem(FEED_ID, directionalMapping(
                        when(OFFER_1),
                        then(OFFER_3)
                ), directionalMapping(
                        when(OFFER_2),
                        then(OFFER_3)
                ))
        ));
    }

    @Test
    public void shouldApplyBundleDiscountOnDsbsOrder() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withPlatform(MarketPlatform.WHITE)
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_1),
                        promoKeys(BUNDLE_PROMO_KEY),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_3),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        price(1000)
                ).build();

        String bundleId = generateBundleId(promoBundleDescription, FEED_ID,
                OFFER_1, OFFER_3
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), everyItem(allOf(
                hasProperty("bundleId", is(bundleId)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.GENERIC_BUNDLE)),
                        hasProperty("discount")
                )))
        )));

        assertThat(firstOrder.getBundles(), hasSize(1));
        assertThat(firstOrder.getBundles(), hasItem(allOf(
                hasProperty("bundleId", is(bundleId)),
                hasProperty("items", hasSize(2))
        )));
    }

    @Test
    public void shouldApplyDifferentBundleDiscountOnDsbsOrder() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withPlatform(MarketPlatform.WHITE)
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_1),
                        promoKeys(BUNDLE_PROMO_KEY),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        quantity(2),
                        price(10000)
                ).withOrderItem(
                        itemKey(FEED_ID, OFFER_2),
                        promoKeys(BUNDLE_PROMO_KEY),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        quantity(2),
                        price(10000)
                ).withOrderItem(
                        itemKey(FEED_ID, OFFER_3),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        quantity(3),
                        price(1000)
                ).build();

        String bundleId1 = generateBundleId(promoBundleDescription, FEED_ID,
                OFFER_1, OFFER_3
        );
        String bundleId2 = generateBundleId(promoBundleDescription, FEED_ID,
                OFFER_2, OFFER_3
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(),
                hasItems(allOf(
                        hasProperty("offerId", is(OFFER_1)),
                        hasProperty("bundleId", is(bundleId1)),
                        hasProperty("quantity", is(BigDecimal.ONE))
                ), allOf(
                        hasProperty("offerId", is(OFFER_1)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("quantity", is(BigDecimal.ONE))
                ), allOf(
                        hasProperty("offerId", is(OFFER_2)),
                        hasProperty("bundleId", is(bundleId2)),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))
                ), allOf(
                        hasProperty("offerId", is(OFFER_3)),
                        hasProperty("bundleId", is(bundleId2)),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))
                ), allOf(
                        hasProperty("offerId", is(OFFER_3)),
                        hasProperty("bundleId", is(bundleId1)),
                        hasProperty("quantity", is(BigDecimal.ONE))
                )));

        assertThat(firstOrder.getBundles(), hasSize(2));
        assertThat(firstOrder.getBundles(), hasItems(allOf(
                hasProperty("bundleId", is(bundleId1)),
                hasProperty("items", hasSize(2)),
                hasProperty("quantity", comparesEqualTo(1L))
        ), allOf(
                hasProperty("bundleId", is(bundleId2)),
                hasProperty("items", hasSize(2)),
                hasProperty("quantity", comparesEqualTo(2L))
        )));
    }

    @Test
    public void shouldBrakeBundleIfNoPromoOnDsbsOrder() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withPlatform(MarketPlatform.WHITE)
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_1),
                        bundle(BUNDLE),
                        promoKeys("another key"),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        price(10000)
                )
                .withOrderItem(
                        itemKey(FEED_ID, OFFER_3),
                        bundle(BUNDLE),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), everyItem(hasProperty("bundleId", nullValue())));

        assertThat(firstOrder.getBundles(), empty());
        assertThat(firstOrder.getBundlesToDestroy(), hasSize(1));
        assertThat(firstOrder.getBundlesToDestroy(), hasItem(allOf(
                hasProperty("bundleId", is(BUNDLE)),
                hasProperty("items", hasSize(2))
        )));
    }
}
