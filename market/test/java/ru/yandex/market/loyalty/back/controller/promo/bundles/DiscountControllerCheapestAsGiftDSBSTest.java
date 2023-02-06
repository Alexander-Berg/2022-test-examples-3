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
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.List;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.cheapestAsGift;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.quantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.withQuantityInBundle;

@TestFor(DiscountController.class)
public class DiscountControllerCheapestAsGiftDSBSTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 859907;
    private static final String BUNDLE_PROMO_KEY = "some promo";
    private static final String SHOP_PROMO_ID = "shop promo id";
    private static final String OFFER_1 = "offer 1";
    private static final String OFFER_2 = "offer 2";
    private static final String OFFER_3 = "offer 3";

    @Autowired
    private PromoBundleService bundleService;

    @Before
    public void prepare() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                strategy(CHEAPEST_AS_GIFT),
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
                ).withOrderItem(
                        itemKey(FEED_ID, OFFER_2),
                        promoKeys(BUNDLE_PROMO_KEY),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        price(2000)
                ).withOrderItem(
                        itemKey(FEED_ID, OFFER_3),
                        promoKeys(BUNDLE_PROMO_KEY),
                        msku(null),
                        ssku(null),
                        warehouse(null),
                        price(1000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order)
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), everyItem(allOf(
                hasProperty("primaryInBundle", nullValue()),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.CHEAPEST_AS_GIFT)),
                        hasProperty("discount")
                )))
        )));

        assertThat(firstOrder.getBundles(), hasSize(1));
        assertThat(firstOrder.getBundles(), hasItem(allOf(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                hasProperty("items", hasSize(3))
        )));
    }
}
