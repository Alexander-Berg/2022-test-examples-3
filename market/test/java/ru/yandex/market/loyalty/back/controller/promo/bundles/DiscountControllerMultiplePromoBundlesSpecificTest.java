package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType.NEW_VERSION;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.bundle;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;

@TestFor(DiscountController.class)
public class DiscountControllerMultiplePromoBundlesSpecificTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 599992;
    private static final long VIRTUAL_FEED_ID = 475690;
    private static final long WAREHOUSE_ID = 145;
    private static final String BUNDLE_1 = "56f93d888da8567c4f93a707a2f4f20b";
    private static final String BUNDLE_2 = "8dbf9c6a177ced78012e0a867cb37988";
    private static final String BUNDLE_3 = "f87ae0b27154613504823752c27f50d7";
    private static final String BUNDLE_PROMO_KEY_1 = "FeDrbXlS5_uWpc-hjSc72g";
    private static final String BUNDLE_PROMO_KEY_2 = "Cb2uB5nm_itYbN_cGrGqng";
    private static final String SSKU_1 = "YNDX-0004S";
    private static final String SSKU_2 = "1084TRUE";
    private static final String SSKU_3 = "YNDX-0004B";
    private static final String SSKU_4 = "GG0919";
    private static final String SSKU_5 = "YNDX-00010";

    @Autowired
    private PromoBundleService bundleService;

    @Before
    public void prepare() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(BUNDLE_PROMO_KEY_1),
                shopPromoId(randomString()),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, SSKU_1, SSKU_3),
                giftItem(
                        FEED_ID,
                        directionalMapping(
                                when(SSKU_1),
                                then(SSKU_5)
                        ),
                        directionalMapping(
                                when(SSKU_3),
                                then(SSKU_5)
                        )
                )
        ));

        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(BUNDLE_PROMO_KEY_2),
                shopPromoId(randomString()),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, SSKU_4),
                giftItem(
                        FEED_ID,
                        directionalMapping(
                                when(SSKU_4),
                                then(SSKU_2)
                        )
                )
        ));
    }

    @Test
    public void shouldConstructDifferentBundles() {
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, SSKU_1)),
                                        ssku(SSKU_1),
                                        bundle(BUNDLE_1),
                                        warehouse(WAREHOUSE_ID),
                                        promoKeys(BUNDLE_PROMO_KEY_1),
                                        price(4490)
                                )
                                .withOrderItem(
                                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, SSKU_5)),
                                        ssku(SSKU_5),
                                        bundle(BUNDLE_1),
                                        warehouse(WAREHOUSE_ID),
                                        price(1390)
                                )
                                .withOrderItem(
                                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, SSKU_4)),
                                        ssku(SSKU_4),
                                        bundle(BUNDLE_2),
                                        warehouse(WAREHOUSE_ID),
                                        promoKeys(BUNDLE_PROMO_KEY_2),
                                        price(8590)
                                )
                                .withOrderItem(
                                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, SSKU_2)),
                                        ssku(SSKU_2),
                                        bundle(BUNDLE_2),
                                        warehouse(WAREHOUSE_ID),
                                        price(1390)
                                )
                                .withOrderItem(
                                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, SSKU_3)),
                                        ssku(SSKU_3),
                                        bundle(BUNDLE_3),
                                        warehouse(WAREHOUSE_ID),
                                        promoKeys(BUNDLE_PROMO_KEY_1),
                                        price(4490)
                                )
                                .withOrderItem(
                                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, SSKU_5)),
                                        ssku(SSKU_5),
                                        bundle(BUNDLE_3),
                                        warehouse(WAREHOUSE_ID),
                                        price(1390)
                                ).build()
                ).build());

        assertThat(discountResponse.getOrders(), everyItem(allOf(
                hasProperty("bundles", hasSize(3)),
                hasProperty("bundles", hasItems(
                        hasProperty("items", hasItems(
                                hasProperty("offerId", is(offerId(FEED_ID, SSKU_1))),
                                hasProperty("offerId", is(offerId(FEED_ID, SSKU_5)))
                        )),
                        hasProperty("items", hasItems(
                                hasProperty("offerId", is(offerId(FEED_ID, SSKU_4))),
                                hasProperty("offerId", is(offerId(FEED_ID, SSKU_2)))
                        )),
                        hasProperty("items", hasItems(
                                hasProperty("offerId", is(offerId(FEED_ID, SSKU_3))),
                                hasProperty("offerId", is(offerId(FEED_ID, SSKU_5)))
                        ))
                )),
                hasProperty("bundlesToDestroy", hasSize(3)),
                hasProperty("bundlesToDestroy", hasItems(
                        allOf(
                                hasProperty("bundleId", is(BUNDLE_1)),
                                hasProperty("reason", hasProperty("type", is(NEW_VERSION)))
                        ),
                        allOf(
                                hasProperty("bundleId", is(BUNDLE_2)),
                                hasProperty("reason", hasProperty("type", is(NEW_VERSION)))
                        ),
                        allOf(
                                hasProperty("bundleId", is(BUNDLE_3)),
                                hasProperty("reason", hasProperty("type", is(NEW_VERSION)))
                        )
                ))
        )));
    }

    private static String offerId(long feedId, String ssku) {
        return feedId + "." + ssku;
    }
}
