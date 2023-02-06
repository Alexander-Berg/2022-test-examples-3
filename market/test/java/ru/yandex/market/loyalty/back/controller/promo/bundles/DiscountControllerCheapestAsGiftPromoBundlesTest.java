package ru.yandex.market.loyalty.back.controller.promo.bundles;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.utils.OrderRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.BUNDLE_DO_NOT_SUPPORT_MULTI_ORDER;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.offerId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.anaplanId;
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
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(DiscountController.class)
public class DiscountControllerCheapestAsGiftPromoBundlesTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final long SECOND_FEED_ID = 234;
    private static final long THIRD_FEED_ID = 456;
    private static final long VIRTUAL_FEED_ID = 12345;
    private static final long WAREHOUSE_ID = 145;
    private static final String BUNDLE_PROMO_KEY = "some promo";
    private static final String SHOP_PROMO_ID = "shop promo id";
    private static final String ANAPLAN_ID = "anaplan id";
    private static final String FIRST_ITEM_SSKU = "first promo offer";
    private static final String SECOND_ITEM_SSKU = "second promo offer";
    private static final String THIRD_ITEM_SSKU = "third promo offer";
    private static final String PROMO_ITEM_FIRST_OFFER_ID = FEED_ID + "." + FIRST_ITEM_SSKU;
    private static final String PROMO_ITEM_SECOND_OFFER_ID = FEED_ID + "." + SECOND_ITEM_SSKU;
    private static final String PROMO_ITEM_THIRD_OFFER_ID = FEED_ID + "." + THIRD_ITEM_SSKU;

    private static final long ZERO_FEED_ID = 0;
    private static final long MULTI_FEED_ID = 537372;
    private static final String MULTI_BUNDLE_PROMO_KEY = "BLoalNLYH4WucdcBfcrlPg";
    private static final String MULTI_SHOP_ID = "#17534";
    private static final List<String> MULTI_OFFER_ID = List.of("000128.8076809576079", "000109.С28", "000128.85293",
            "000128.8076809576000", "000319.504012", "000041.5363", "000041.144098", "000128.8076809576048",
            "000128.8076809575997", "000041.144001");
    private static final long MULTI_WAREHOUSE_ID = 147;
    private static final List<Long> MULTI_PRICE = List.of(130L, 20L, 82L, 112L, 52L, 374L, 393L, 118L, 113L, 136L);
    private static final int QUANTITY_IN_BUNDLE = 3;

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
                strategy(CHEAPEST_AS_GIFT),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                withQuantityInBundle(QUANTITY_IN_BUNDLE),
                item(
                        condition(
                                cheapestAsGift(
                                        FeedSskuSet.of(FEED_ID, List.of(FIRST_ITEM_SSKU, SECOND_ITEM_SSKU,
                                                THIRD_ITEM_SSKU)))),
                        quantityInBundle(3),
                        primary()
                )
        ));
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(ZERO_FEED_ID),
                promoKey(MULTI_BUNDLE_PROMO_KEY),
                shopPromoId(MULTI_SHOP_ID),
                anaplanId(MULTI_SHOP_ID),
                strategy(CHEAPEST_AS_GIFT),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                withQuantityInBundle(5)
        ));
    }

    @Test
    public void shouldConstructBundleWithSameQuantityAndPrice() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_FIRST_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_SECOND_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(2100)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_THIRD_OFFER_ID),
                        ssku(THIRD_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
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
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(700)))
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
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(700)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_THIRD_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(700)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldMultiOrderException() {
        OrderWithBundlesRequest first = createOrderRequestWithBundlesBuilder(0).build();
        OrderWithBundlesRequest second = createOrderRequestWithBundlesBuilder(5).build();

        MarketLoyaltyException marketLoyaltyException = assertThrows(
                MarketLoyaltyException.class,
                () -> marketLoyaltyClient.calculateDiscount(builder(first, second).build())
        );

        assertEquals(BUNDLE_DO_NOT_SUPPORT_MULTI_ORDER.name(), marketLoyaltyException.getModel().getCode());
    }

    private OrderRequestWithBundlesBuilder createOrderRequestWithBundlesBuilder(int start) {
        OrderRequestWithBundlesBuilder orderRequestWithBundlesBuilder = orderRequestWithBundlesBuilder();
        Stream.iterate(start, n -> n + 1).limit(5).forEach(index ->
            orderRequestWithBundlesBuilder
                    .withOrderItem(
                            //offerId и ssku должны быть разные?
                            //в тестах разные, а на стрельбах одинаковые
                            //в текущем тесте делаю как на стрельбах
                            itemKey(MULTI_FEED_ID, MULTI_OFFER_ID.get(index)),
                            ssku(MULTI_OFFER_ID.get(index)),
                            promoKeys(MULTI_BUNDLE_PROMO_KEY),
                            warehouse(MULTI_WAREHOUSE_ID),
                            price(MULTI_PRICE.get(index))
                    )
        );
        return orderRequestWithBundlesBuilder;
    }

    @Test
    public void shouldConstructBundleWithProportionalQuantity() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_FIRST_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(3),
                        price(2100)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_SECOND_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(3),
                        price(2100)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_THIRD_OFFER_ID),
                        ssku(THIRD_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(3),
                        price(2100)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(3));
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
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(700)))
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
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(700)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_THIRD_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(700)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldConstructBundleWithDifferentPrices() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_FIRST_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(3),
                        price(101)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_SECOND_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(3),
                        price(202)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_THIRD_OFFER_ID),
                        ssku(THIRD_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(3),
                        price(303)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(3));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_FIRST_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(33))),
                                        hasProperty("additionalInfo",
                                                hasProperty("quantityInBundle", is(QUANTITY_IN_BUNDLE)))
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
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(68))),
                                        hasProperty("additionalInfo",
                                                hasProperty("quantityInBundle", is(QUANTITY_IN_BUNDLE)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_THIRD_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(101))),
                                        hasProperty("additionalInfo",
                                                hasProperty("quantityInBundle", is(QUANTITY_IN_BUNDLE)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldConstructBundleWithNotProportionalQuantity() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_FIRST_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(3),
                        price(101)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_SECOND_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(4),
                        price(202)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_THIRD_OFFER_ID),
                        ssku(THIRD_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        quantity(5),
                        price(303)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(4));
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
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(32)))
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
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(62)))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(PROMO_ITEM_THIRD_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(93)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldConstructBundleWithDifferentFeedId() {
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
                                        FeedSskuSet.of(FEED_ID, List.of(FIRST_ITEM_SSKU)),
                                        FeedSskuSet.of(SECOND_FEED_ID, List.of(SECOND_ITEM_SSKU)),
                                        FeedSskuSet.of(THIRD_FEED_ID, List.of(THIRD_ITEM_SSKU))
                                )
                        ),
                        quantityInBundle(3),
                        primary()
                )
        ));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, FIRST_ITEM_SSKU)),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(101)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(SECOND_FEED_ID, SECOND_ITEM_SSKU)),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(202)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(THIRD_FEED_ID, THIRD_ITEM_SSKU)),
                        ssku(THIRD_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        warehouse(WAREHOUSE_ID),
                        price(303)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(1));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, FIRST_ITEM_SSKU))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(offerId(SECOND_FEED_ID, SECOND_ITEM_SSKU))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
                                )
                        ))
                ), allOf(
                        hasProperty("offerId", is(offerId(THIRD_FEED_ID, THIRD_ITEM_SSKU))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
                                )
                        ))
                )
        ));
    }
}
