package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
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
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.anaplanId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.blueSet;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.ends;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;

@TestFor(DiscountController.class)
public class DiscountControllerBlueSetPromoBundlesTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final long WAREHOUSE_ID = 145;
    private static final String BUNDLE_PROMO_KEY = "some promo";
    private static final String ZERO_BUNDLE_PROMO_KEY = "zero promo";
    private static final String SHOP_PROMO_ID = "shop promo id";
    private static final String ANAPLAN_ID = "anaplan id";
    private static final String FIRST_ITEM_SSKU = "first promo offer";
    private static final String SECOND_ITEM_SSKU = "second promo offer";
    private static final String THIRD_ITEM_SSKU = "third promo offer";
    private static final String FOURTH_ITEM_SSKU = "fourth promo offer";
    private static final String PROMO_ITEM_FIRST_OFFER_ID = FEED_ID + "." + FIRST_ITEM_SSKU;
    private static final String PROMO_ITEM_SECOND_OFFER_ID = FEED_ID + "." + SECOND_ITEM_SSKU;
    private static final String PROMO_ITEM_THIRD_OFFER_ID = FEED_ID + "." + THIRD_ITEM_SSKU;
    private static final String PROMO_ITEM_FOURTH_ITEM_SSKU = FEED_ID + "." + FOURTH_ITEM_SSKU;

    @Autowired
    private PromoBundleService bundleService;

    @Before
    public void prepare() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_ITEM_SSKU, 10),
                                proportion(SECOND_ITEM_SSKU, 20)
                        )),
                        primary()
                ),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_ITEM_SSKU, 10),
                                proportion(SECOND_ITEM_SSKU, 20),
                                proportion(FOURTH_ITEM_SSKU, 30)
                        )),
                        primary()
                )
        ));
    }

    @Test
    public void shouldConstructBundle() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_FIRST_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SECOND_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_THIRD_OFFER_ID),
                        ssku(THIRD_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(1));

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_FIRST_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(210)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SECOND_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(420)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_THIRD_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", empty())
                )
        ));
    }

    @Test
    public void shouldConstructDifferentBundle1() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_FIRST_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(1205),
                        quantity(2)
                ).withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SECOND_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(1253),
                        quantity(2)
                ).withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_FOURTH_ITEM_SSKU),
                        ssku(FOURTH_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(2));

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_FIRST_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2))),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(121)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SECOND_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2))),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(251)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_FOURTH_ITEM_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(630)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldConstructDifferentBundle2() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_FIRST_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(1205),
                        quantity(3)
                ).withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SECOND_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(1253),
                        quantity(2)
                ).withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_FOURTH_ITEM_SSKU),
                        ssku(FOURTH_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(2));

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_FIRST_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3))),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(80)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_SECOND_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2))),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(251)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_FOURTH_ITEM_SSKU)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(632)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldNotAddDiscountOnZeroDescription() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(ZERO_BUNDLE_PROMO_KEY),
                shopPromoId(randomString()),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_ITEM_SSKU, 0),
                                proportion(SECOND_ITEM_SSKU, 0)
                        )),
                        primary()
                )
        ));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_FIRST_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(ZERO_BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_SECOND_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(ZERO_BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).withOrderItem(
                        itemKey(FEED_ID, PROMO_ITEM_THIRD_OFFER_ID),
                        ssku(THIRD_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(1));

        assertThat(firstOrderOf(discountResponse).getItems(), everyItem(
                allOf(
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", empty())
                )
        ));
    }
}
