package ru.yandex.market.loyalty.back.controller.promo.bundles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundle;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType.ERROR;
import static ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType.NEW_VERSION;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CartUtils.generateBundleId;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.bundle;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.offerId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.OrderUtil.parseOrderId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.anaplanId;
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
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictReturn;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;

@TestFor(DiscountController.class)
public class DiscountControllerGiftWithPurchasePromoBundlesTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final long VIRTUAL_FEED_ID = 12345;
    private static final long WAREHOUSE_ID = 145;
    private static final String BUNDLE = "some bundle";
    private static final String BUNDLE_PROMO_KEY = "some promo";
    private static final String SHOP_PROMO_ID = "shop promo id";
    private static final String ANOTHER_SHOP_PROMO_ID = "another shop promo id";
    private static final String ANAPLAN_ID = "anaplan id";
    private static final String OTHER_BUNDLE_PROMO_KEY = "some new bundle";
    private static final String EXPIRED_BUNDLE_PROMO_KEY = "some expired bundle";
    private static final String PROMO_ITEM_SSKU = "some promo offer";
    private static final String PROMO_ITEM_OFFER_ID = FEED_ID + "." + PROMO_ITEM_SSKU;
    private static final String GIFT_ITEM_SSKU = "some gift offer";
    private static final String ANOTHER_GIFT_ITEM_SSKU = "another gift offer";
    private static final String GIFT_ITEM_OFFER_ID = FEED_ID + "." + GIFT_ITEM_SSKU;
    private static final String ANOTHER_GIFT_OFFER_ID = FEED_ID + "." + ANOTHER_GIFT_ITEM_SSKU;

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
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU, ANOTHER_GIFT_ITEM_SSKU))
                )
        ));
    }

    @Test
    public void shouldConstructBundleWithFixedDiscount() {
        PromoBundleDescription expectedPromo = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, PROMO_ITEM_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(PROMO_ITEM_SSKU),
                        then(GIFT_ITEM_SSKU),
                        fixedPrice(10)
                ))
        ));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        price(123)
                )
                .withOrderId(null)
                .build();

        String bundleId = generateBundleId(expectedPromo, VIRTUAL_FEED_ID,
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
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("promos", hasItem(
                                allOf(
                                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_ID)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(113)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldReturnRestrictionOfItemsReturning() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                strategy(GIFT_WITH_PURCHASE),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                primaryItem(FEED_ID, PROMO_ITEM_SSKU),
                giftItem(FEED_ID, directionalMapping(
                        when(PROMO_ITEM_SSKU),
                        then(GIFT_ITEM_SSKU),
                        fixedPrice(10)
                )),
                restrictions(
                        restrictReturn()
                )
        ));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                                ssku(PROMO_ITEM_SSKU),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(100000)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                                ssku(GIFT_ITEM_SSKU),
                                price(123)
                        ).build()).build());

        OrderWithBundlesResponse order = firstOrderOf(discountResponse);

        assertThat(order.getBundles(), hasItem(
                hasProperty("restrictReturn", is(true))
        ));
    }

    @Test
    public void shouldConstructBundleWithoutBundleIdInRequest() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        price(123)
                )
                .withOrderId(null)
                .build();

        String bundleId = generateBundleId(promoBundleDescription, VIRTUAL_FEED_ID,
                PROMO_ITEM_OFFER_ID, GIFT_ITEM_OFFER_ID
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), not(empty()));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());
        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));
        assertItemsQuantity(PROMO_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(false))
                )
        ));
    }

    @Test
    public void shouldConstructBundleWithRandomBundleId() {
        String randomKey = UUID.randomUUID().toString();

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        bundle(randomKey),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(1063)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        bundle(randomKey),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(1063)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(2371)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        String bundleId = generateBundleId(promoBundleDescription, VIRTUAL_FEED_ID,
                PROMO_ITEM_OFFER_ID, GIFT_ITEM_OFFER_ID
        );

        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));

        assertThat(firstOrderOf(discountResponse).getBundles(), not(empty()));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), hasItem(
                hasProperty("reason", hasProperty("type", is(NEW_VERSION)))
        ));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("quantity", is(BigDecimal.ONE))

                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("quantity", is(BigDecimal.ONE))

                ),
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", is(BigDecimal.ONE))
                )
        ));
    }

    @Test
    public void shouldDestroyExpiredBundle() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys("not existed promo"),
                        bundle("not existed bundle"),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        bundle("not existed bundle"),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys("not existed promo"),
                        price(100000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), empty());
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), hasItem(
                hasProperty("reason", hasProperty("type", is(ERROR)))
        ));
        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));
        assertItemsQuantity(PROMO_ITEM_OFFER_ID, 2, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))

                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("quantity", is(BigDecimal.ONE))

                )
        ));
    }

    @Test
    public void shouldDestroyBundlesForItemsWithoutPromos() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        bundle(BUNDLE),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        bundle(BUNDLE),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        OrderWithBundlesResponse orderResponse = firstOrderOf(discountResponse);

        assertThat(orderResponse.getBundles(), empty());
        assertThat(orderResponse.getBundlesToDestroy(), not(empty()));
        assertThat(orderResponse.getBundlesToDestroy(), hasItem(allOf(
                hasProperty("bundleId", equalTo(BUNDLE)),
                hasProperty(
                        "reason",
                        hasProperty("type", equalTo(BundleDestroyReason.ReasonType.ERROR))
                )
                )
        ));
    }

    @Test
    public void shouldSplitItemsOnBundleConstruction() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        quantity(2),
                        price(15000)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), not(empty()));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());
        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));
        assertItemsQuantity(PROMO_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 2, firstOrderOf(discountResponse));
    }

    @Test
    public void shouldDestroyNotExistsBundles() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(EXPIRED_BUNDLE_PROMO_KEY),
                        bundle(BUNDLE),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        bundle(BUNDLE),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        OrderWithBundlesResponse orderResponse = firstOrderOf(discountResponse);

        assertThat(orderResponse.getBundles(), empty());
        assertThat(orderResponse.getBundlesToDestroy(), not(empty()));
        assertThat(orderResponse.getBundlesToDestroy(), hasItem(allOf(
                hasProperty("bundleId", equalTo(BUNDLE)),
                hasProperty(
                        "reason",
                        hasProperty("type", equalTo(BundleDestroyReason.ReasonType.ERROR))
                )
                )
        ));
    }

    @Test
    public void shouldDestroyExpiredBundles() {
        PromoBundleDescription expectedDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        PromoBundleUtils.promoKey(EXPIRED_BUNDLE_PROMO_KEY),
                        shopPromoId(SHOP_PROMO_ID),
                        anaplanId(ANAPLAN_ID),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime().minusDays(15)),
                        ends(clock.dateTime().minusDays(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        String expectedBundleId = generateBundleId(expectedDescription, VIRTUAL_FEED_ID, PROMO_ITEM_SSKU,
                GIFT_ITEM_SSKU
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(EXPIRED_BUNDLE_PROMO_KEY),
                        bundle(expectedBundleId),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        bundle(expectedBundleId),
                        price(15000)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        OrderWithBundlesResponse orderResponse = firstOrderOf(discountResponse);

        assertThat(orderResponse.getBundles(), empty());
        assertThat(orderResponse.getBundlesToDestroy(), not(empty()));
        assertThat(orderResponse.getBundlesToDestroy(), hasItem(allOf(
                hasProperty("bundleId", equalTo(expectedBundleId)),
                hasProperty(
                        "reason",
                        hasProperty("type", equalTo(BundleDestroyReason.ReasonType.ERROR))
                )
        )));
    }

    @Test
    public void shouldReplaceBundleWithNewVersion() {
        PromoBundleDescription expiredDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        PromoBundleUtils.promoKey(EXPIRED_BUNDLE_PROMO_KEY),
                        shopPromoId(ANOTHER_SHOP_PROMO_ID),
                        anaplanId(ANAPLAN_ID),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime().minusDays(15)),
                        ends(clock.dateTime().minusDays(10)),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        String expiredBundleId = generateBundleId(expiredDescription, VIRTUAL_FEED_ID, PROMO_ITEM_SSKU,
                GIFT_ITEM_SSKU
        );

        PromoBundleDescription newDescription = bundleService.createPromoBundle(
                bundleDescription(
                        promoSource(LOYALTY_VALUE),
                        feedId(FEED_ID),
                        PromoBundleUtils.promoKey(OTHER_BUNDLE_PROMO_KEY),
                        shopPromoId(ANOTHER_SHOP_PROMO_ID),
                        anaplanId(ANAPLAN_ID),
                        strategy(GIFT_WITH_PURCHASE),
                        starts(clock.dateTime()),
                        ends(clock.dateTime().plusYears(1)),
                        item(
                                condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        String newBundleId = generateBundleId(newDescription, VIRTUAL_FEED_ID,
                PROMO_ITEM_OFFER_ID,
                GIFT_ITEM_OFFER_ID
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(OTHER_BUNDLE_PROMO_KEY),
                        bundle(expiredBundleId),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        bundle(expiredBundleId),
                        price(15000)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        OrderWithBundlesResponse orderResponse = firstOrderOf(discountResponse);

        assertItemsMultiplicityInBundle(orderResponse);
        assertItemsQuantity(PROMO_ITEM_OFFER_ID, 1, orderResponse);
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 1, orderResponse);

        assertThat(orderResponse.getBundles(), not(empty()));
        assertThat(orderResponse.getBundles(), hasItem(
                allOf(
                        hasProperty("bundleId", equalTo(newBundleId))
                )
        ));
        assertThat(orderResponse.getBundlesToDestroy(), not(empty()));
        assertThat(orderResponse.getBundlesToDestroy(), hasItem(allOf(
                hasProperty("bundleId", equalTo(expiredBundleId)),
                hasProperty(
                        "reason",
                        hasProperty("type", equalTo(NEW_VERSION))
                )
        )));
    }

    @Test
    public void shouldCreateBundlesWithVariants() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        quantity(4),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, ANOTHER_GIFT_OFFER_ID),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        quantity(2),
                        price(100000)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        String bundleId = generateBundleId(promoBundleDescription, VIRTUAL_FEED_ID,
                PROMO_ITEM_OFFER_ID, GIFT_ITEM_OFFER_ID
        );

        String anotherBundleId = generateBundleId(promoBundleDescription, VIRTUAL_FEED_ID,
                PROMO_ITEM_OFFER_ID, ANOTHER_GIFT_OFFER_ID
        );

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(2));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());
        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));
        assertItemsQuantity(PROMO_ITEM_OFFER_ID, 4, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 2, firstOrderOf(discountResponse));
        assertItemsQuantity(ANOTHER_GIFT_OFFER_ID, 2, firstOrderOf(discountResponse));

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))

                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))

                ),
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(anotherBundleId)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))

                ),
                allOf(
                        hasProperty("offerId", is(ANOTHER_GIFT_OFFER_ID)),
                        hasProperty("bundleId", is(anotherBundleId)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))

                )
        ));
    }

    @Test
    public void shouldDestroyExpiredBundleWithVariants() {
        PromoBundleDescription expectedBundle = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, PROMO_ITEM_SSKU)),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, ANOTHER_GIFT_ITEM_SSKU))
                )
        ));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        bundle("first variant bundle"),
                        quantity(2),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        bundle("first variant bundle"),
                        quantity(2),
                        price(123)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, PROMO_ITEM_OFFER_ID),
                        ssku(PROMO_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        bundle("second variant bundle"),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, ANOTHER_GIFT_OFFER_ID),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        bundle("second variant bundle"),
                        price(123)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, ANOTHER_GIFT_OFFER_ID),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        price(123)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        String bundleId = generateBundleId(expectedBundle, VIRTUAL_FEED_ID,
                PROMO_ITEM_OFFER_ID, ANOTHER_GIFT_OFFER_ID
        );

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(1));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), hasSize(2));
        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));
        assertItemsQuantity(PROMO_ITEM_OFFER_ID, 3, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 2, firstOrderOf(discountResponse));
        assertItemsQuantity(ANOTHER_GIFT_OFFER_ID, 2, firstOrderOf(discountResponse));

        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))

                ),
                allOf(
                        hasProperty("offerId", is(ANOTHER_GIFT_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))

                ),
                allOf(
                        hasProperty("offerId", is(PROMO_ITEM_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", is(BigDecimal.ONE))

                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", is(BigDecimal.valueOf(2)))

                )
        ));
    }

    @Test
    public void shouldNotChangeAnythingForExistedBundle() {
        PromoBundleDescription expectedPromo = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00009.рврврр", "00065.00026.100126174633",
                                "00065.00026.100126174723", "00065.00026.100126174823", "00065.00026.100126174831",
                                "00065.00026.100126174899", "00065.00026.100126176581", "00065.00026.100126176813",
                                "00065.00026.100126176952", "00065.00026.100126176978"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00023.100126176175"))
                )
        ));

        String expectedBundleId = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126174633"),
                offerId(FEED_ID, "00065.00023.100126176175")
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126174633")),
                        ssku("00065.00026.100126174633"),
                        warehouse(WAREHOUSE_ID),
                        bundle(expectedBundleId),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(1063)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126174633")),
                        ssku("00065.00026.100126174633"),
                        warehouse(WAREHOUSE_ID),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(1063)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00023.100126176175")),
                        ssku("00065.00023.100126176175"),
                        warehouse(WAREHOUSE_ID),
                        bundle(expectedBundleId),
                        price(2371)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));

        assertThat(firstOrderOf(discountResponse).getBundles(), not(empty()));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126174633"))),
                        hasProperty("bundleId", is(expectedBundleId)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("quantity", is(BigDecimal.ONE))

                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00023.100126176175"))),
                        hasProperty("bundleId", is(expectedBundleId)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("quantity", is(BigDecimal.ONE))

                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126174633"))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", is(BigDecimal.ONE))
                )
        ));
    }

    @Test
    public void shouldConstructSpecificVariantsBundle() {
        PromoBundleDescription expectedPromo = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00009.рврврр", "00065.00026.100126174633",
                                "00065.00026.100126174723", "00065.00026.100126174823", "00065.00026.100126174831",
                                "00065.00026.100126174899", "00065.00026.100126176581", "00065.00026.100126176813",
                                "00065.00026.100126176952", "00065.00026.100126176978"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00023.100126176175"))
                )
        ));

        String expectedBundle1 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00009.рврврр"),
                offerId(FEED_ID, "00065.00023.100126176175")
        );

        String expectedBundle2 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126174823"),
                offerId(FEED_ID, "00065.00023.100126176175")
        );

        String expectedBundle3 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126176581"),
                offerId(FEED_ID, "00065.00023.100126176175")
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00023.100126176175")),
                                ssku("00065.00023.100126176175"),
                                bundle(expectedBundle1),
                                warehouse(WAREHOUSE_ID),
                                quantity(2),
                                price(2371)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00023.100126176175")),
                                ssku("00065.00023.100126176175"),
                                bundle(expectedBundle2),
                                warehouse(WAREHOUSE_ID),
                                price(2371)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00009.рврврр")),
                                ssku("00065.00009.рврврр"),
                                bundle(expectedBundle1),
                                warehouse(WAREHOUSE_ID),
                                quantity(2),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(1967)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126176581")),
                                ssku("00065.00026.100126176581"),
                                bundle(expectedBundle3),
                                warehouse(WAREHOUSE_ID),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(6659)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00023.100126176175")),
                                ssku("00065.00023.100126176175"),
                                bundle(expectedBundle3),
                                warehouse(WAREHOUSE_ID),
                                price(6659)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126174823")),
                                ssku("00065.00026.100126174823"),
                                bundle(expectedBundle2),
                                warehouse(WAREHOUSE_ID),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(6659)
                        )
                        .build()).build());

        OrderWithBundlesResponse order = firstOrderOf(discountResponse);
        assertThat(order.getBundles(), hasSize(3));
        assertThat(order.getBundles(), hasItems(
                allOf(
                        hasProperty("bundleId", is(expectedBundle1)),
                        hasProperty("quantity", is(2L))
                ),
                allOf(
                        hasProperty("bundleId", is(expectedBundle3)),
                        hasProperty("quantity", is(1L))
                ),
                allOf(
                        hasProperty("bundleId", is(expectedBundle2)),
                        hasProperty("quantity", is(1L))
                )
        ));
    }

    @Test
    public void shouldConstructBundlesWithVirtualFeeds() {
        PromoBundleDescription bundleDescription = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126174633", "00065.00026.100126174723",
                                "00065.00026.100126174823", "00065.00026.100126174899", "00065.00026.100126176813"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00023.100126176175"))
                )
        ));


        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126174633")),
                        ssku("00065.00026.100126174633"),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00023.100126176175")),
                        ssku("00065.00023.100126176175"),
                        price(15000)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        String bundleId = generateBundleId(bundleDescription, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126174633"),
                offerId(FEED_ID, "00065.00023.100126176175")
        );

        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));

        assertThat(firstOrderOf(discountResponse).getBundles(), not(empty()));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());
        assertThat(firstOrderOf(discountResponse).getItems(), everyItem(
                hasProperty("bundleId", equalTo(bundleId))
        ));
    }

    @Test
    public void shouldConstructSpecificVariantsBundle2() {
        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126178007", "00065.00026.100126178009",
                                "00065.00026.100126178011", "00065.00026.100126179060"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126179112"))
                )
        ));

        bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(OTHER_BUNDLE_PROMO_KEY),
                shopPromoId(randomString()),
                anaplanId(randomString()),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126179060")),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126179112"))
                )
        ));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179112")),
                                ssku("00065.00026.100126179112"),
                                warehouse(WAREHOUSE_ID),
                                price(4742)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179060")),
                                ssku("00065.00026.100126179060"),
                                promoKeys(OTHER_BUNDLE_PROMO_KEY),
                                warehouse(WAREHOUSE_ID),
                                price(244)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126178009")),
                                ssku("00065.00026.100126178009"),
                                warehouse(WAREHOUSE_ID),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(1967)
                        )
                        .build()).build());

        OrderWithBundlesResponse order = firstOrderOf(discountResponse);
        assertThat(order.getBundles(), hasSize(1));
        assertThat(order.getBundles(), hasItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));
        assertThat(order.getItems(), hasSize(3));
        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179060"))),
                        hasProperty("bundleId", nullValue())
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126178009"))),
                        hasProperty("bundleId", notNullValue())
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179112"))),
                        hasProperty("bundleId", notNullValue())
                )
        ));
    }

    @Test
    public void shouldNotChangeExistedBundleVariants() {
        PromoBundleDescription expectedPromo = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "000153.00643", "000153.28597", "000153.36330", "000153" +
                                        ".90151",
                                "000160.822067", "000200.SB02012400", "000251.PSM(G)", "000251.PSM(P)", "000251.PSM(Y)",
                                "000328.6018001122", "000328.6018001123", "000328.6018001815", "000328.6018001816",
                                "000328.6018001817", "000328.6018001818", "000328.6018001819", "000331.НР110",
                                "000331.СМ1338",
                                "000331.СМ2557", "000331.СМ2565", "000331.СМ3069"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "000251.FVGG(С)"))
                )
        ));

        String expectedBundle1 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "000251.PSM(G)"),
                offerId(FEED_ID, "000251.FVGG(С)")
        );

        String expectedBundle2 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "000251.PSM(Y)"),
                offerId(FEED_ID, "000251.FVGG(С)")
        );

        String expectedBundle3 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "000251.PSM(P)"),
                offerId(FEED_ID, "000251.FVGG(С)")
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "000251.PSM(G)")),
                                ssku("000251.PSM(G)"),
                                bundle(expectedBundle1),
                                promoKeys(BUNDLE_PROMO_KEY),
                                warehouse(WAREHOUSE_ID),
                                price(2319)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "000251.PSM(Y)")),
                                ssku("000251.PSM(Y)"),
                                bundle(expectedBundle2),
                                promoKeys(BUNDLE_PROMO_KEY),
                                warehouse(WAREHOUSE_ID),
                                price(2319)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "000251.FVGG(С)")),
                                ssku("000251.FVGG(С)"),
                                bundle(expectedBundle3),
                                warehouse(WAREHOUSE_ID),
                                price(200)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "000251.PSM(P)")),
                                ssku("000251.PSM(P)"),
                                warehouse(WAREHOUSE_ID),
                                bundle(expectedBundle3),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(2239)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "000251.FVGG(С)")),
                                ssku("000251.FVGG(С)"),
                                warehouse(WAREHOUSE_ID),
                                bundle(expectedBundle1),
                                price(200)
                        )
                        .build()).build());

        OrderWithBundlesResponse order = firstOrderOf(discountResponse);

        assertThat(order.getBundles(), hasSize(2));
        assertThat(order.getBundles(), everyItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));
        assertThat(order.getItems(), hasSize(5));
        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "000251.PSM(G)"))),
                        hasProperty("bundleId", is(expectedBundle1))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "000251.FVGG(С)"))),
                        hasProperty("bundleId", is(expectedBundle1))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "000251.FVGG(С)"))),
                        hasProperty("bundleId", is(expectedBundle3))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "000251.PSM(P)"))),
                        hasProperty("bundleId", is(expectedBundle3))
                )
        ));
    }

    //https://st.yandex-team.ru/MARKETDISCOUNT-2018
    @Test
    public void shouldSpendWithSpecificBundle() {
        PromoBundleDescription expectedPromo = bundleService.createPromoBundle(bundleDescription(
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                feedId(FEED_ID),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                strategy(GIFT_WITH_PURCHASE),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126174633",
                                "00065.00026.100126174723",
                                "00065.00026.100126174823",
                                "00065.00026.100126174899",
                                "00065.00026.100126176581",
                                "00065.00026.100126176813"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00023.100126176175"))
                )
        ));

        String expectedBundle = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126174633"),
                offerId(FEED_ID, "00065.00023.100126176175")
        );

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderId("4166607")
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126174633")),
                        ssku("00065.00026.100126174633"),
                        promoKeys(BUNDLE_PROMO_KEY),
                        bundle(expectedBundle),
                        price(1063)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00023.100126176175")),
                        ssku("00065.00023.100126176175"),
                        bundle(expectedBundle),
                        price(2371)
                )
                .withOrderId(null)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        OrderWithBundlesResponse orderResponse = firstOrderOf(discountResponse);

        assertItemsMultiplicityInBundle(orderResponse);
        assertItemsQuantity(offerId(FEED_ID, "00065.00026.100126174633"), 1, orderResponse);
        assertItemsQuantity(offerId(FEED_ID, "00065.00023.100126176175"), 1, orderResponse);

        assertThat(orderResponse.getBundles(), hasSize(1));
        assertThat(orderResponse.getBundles(), hasItem(
                allOf(
                        hasProperty("bundleId", equalTo(expectedBundle)),
                        hasProperty("promoKey", equalTo(BUNDLE_PROMO_KEY)),
                        hasProperty("quantity", comparesEqualTo(1L)),
                        hasProperty("items", hasItems(
                                hasProperty("offerId", equalTo(offerId(FEED_ID, "00065.00026.100126174633"))),
                                hasProperty("offerId", equalTo(offerId(FEED_ID, "00065.00023.100126176175")))
                        ))
                )
        ));
        assertThat(orderResponse.getBundlesToDestroy(), empty());
    }

    @Test
    public void shouldJoinBundles() {
        PromoBundleDescription expectedPromo = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126179429", "00065.100126178097",
                                "00065.push1pAPItest100256661371", "00092.qwe123"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126179861"))
                )
        ));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179429")),
                                ssku("00065.00026.100126179429"),
                                bundle("5C8A1B4E-C002-4969-906D-A1942D22FBD4"),
                                promoKeys(BUNDLE_PROMO_KEY),
                                warehouse(WAREHOUSE_ID),
                                price(4711)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179861")),
                                ssku("00065.00026.100126179861"),
                                bundle("BB1FBA6A-F952-485A-90A0-23515AAAC41E"),
                                warehouse(WAREHOUSE_ID),
                                price(5661)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179861")),
                                ssku("00065.00026.100126179861"),
                                bundle("5C8A1B4E-C002-4969-906D-A1942D22FBD4"),
                                warehouse(WAREHOUSE_ID),
                                price(5661)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179429")),
                                ssku("00065.00026.100126179429"),
                                warehouse(WAREHOUSE_ID),
                                bundle("BB1FBA6A-F952-485A-90A0-23515AAAC41E"),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(2239)
                        )
                        .build()).build());

        OrderWithBundlesResponse order = firstOrderOf(discountResponse);
        String expectedBundleId = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126179429"),
                offerId(FEED_ID, "00065.00026.100126179861")
        );

        assertThat(order.getBundlesToDestroy(), hasSize(2));
        assertThat(order.getBundles(), hasSize(1));
        assertThat(order.getBundles(), hasItem(allOf(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                hasProperty("quantity", comparesEqualTo(2L))
        )));
        assertThat(order.getItems(), hasSize(2));
        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179429"))),
                        hasProperty("bundleId", is(expectedBundleId)),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179861"))),
                        hasProperty("bundleId", is(expectedBundleId)),
                        hasProperty("primaryInBundle", is(false))
                )
        ));
    }

    @Test
    public void shouldRestoreBundleVariantOfSamePromo() {
        PromoBundleDescription expectedPromo = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126178007", "00065.00026.100126178009",
                                "00065.00026.100126178011", "00065.00026.100126179060"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126179112"))
                )
        ));

        String expectedBundle1 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126178007"),
                offerId(FEED_ID, "00065.00026.100126179112")
        );

        String expectedBundle2 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126179060"),
                offerId(FEED_ID, "00065.00026.100126179112")
        );

        String expectedBundle3 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126178009"),
                offerId(FEED_ID, "00065.00026.100126179112")
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126178007")),
                                ssku("00065.00026.100126178007"),
                                bundle(expectedBundle1),
                                promoKeys(BUNDLE_PROMO_KEY),
                                warehouse(WAREHOUSE_ID),
                                price(6174)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179112")),
                                ssku("00065.00026.100126179112"),
                                bundle(expectedBundle2),
                                warehouse(WAREHOUSE_ID),
                                price(4742)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179112")),
                                ssku("00065.00026.100126179112"),
                                bundle(expectedBundle3),
                                warehouse(WAREHOUSE_ID),
                                price(4742)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179060")),
                                ssku("00065.00026.100126179060"),
                                warehouse(WAREHOUSE_ID),
                                bundle(expectedBundle2),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(244)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179112")),
                                ssku("00065.00026.100126179112"),
                                bundle(expectedBundle1),
                                warehouse(WAREHOUSE_ID),
                                price(4742)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126178009")),
                                ssku("00065.00026.100126178009"),
                                warehouse(WAREHOUSE_ID),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(2274),
                                quantity(3)
                        )
                        .build()).build());

        OrderWithBundlesResponse order = firstOrderOf(discountResponse);
        assertThat(order.getBundlesToDestroy(), empty());
        assertThat(order.getBundles(), hasSize(3));
        assertThat(order.getBundles(), everyItem(
                hasProperty("promoKey", is(BUNDLE_PROMO_KEY))
        ));
        assertThat(order.getItems(), hasSize(7));
        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126178007"))),
                        hasProperty("bundleId", is(expectedBundle1)),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179112"))),
                        hasProperty("bundleId", is(expectedBundle1)),
                        hasProperty("primaryInBundle", is(false))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179060"))),
                        hasProperty("bundleId", is(expectedBundle2)),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179112"))),
                        hasProperty("bundleId", is(expectedBundle2)),
                        hasProperty("primaryInBundle", is(false))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126178009"))),
                        hasProperty("bundleId", is(expectedBundle3)),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179112"))),
                        hasProperty("bundleId", is(expectedBundle3)),
                        hasProperty("primaryInBundle", is(false))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126178009"))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
                )
        ));
    }

    @Test
    public void shouldRestoreBundleVariantOfDifferentPromo() {
        PromoBundleDescription expectedPromo = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(SHOP_PROMO_ID),
                anaplanId(ANAPLAN_ID),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126178007",
                                "00065.00026.100126178011", "00065.00026.100126179060"
                        )),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126179112"))
                )
        ));

        PromoBundleDescription anotherPromo = bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                PromoBundleUtils.promoKey(OTHER_BUNDLE_PROMO_KEY),
                shopPromoId(randomString()),
                anaplanId(randomString()),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126178009")),
                        primary()
                ),
                item(
                        condition(giftWithPurchase(FEED_ID, "00065.00026.100126179112"))
                )
        ));

        String expectedBundle1 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126178007"),
                offerId(FEED_ID, "00065.00026.100126179112")
        );

        String expectedBundle2 = generateBundleId(
                expectedPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126179060"),
                offerId(FEED_ID, "00065.00026.100126179112")
        );

        String expectedBundle3 = generateBundleId(
                anotherPromo, VIRTUAL_FEED_ID,
                offerId(FEED_ID, "00065.00026.100126178009"),
                offerId(FEED_ID, "00065.00026.100126179112")
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126178007")),
                                ssku("00065.00026.100126178007"),
                                bundle(expectedBundle1),
                                promoKeys(BUNDLE_PROMO_KEY),
                                warehouse(WAREHOUSE_ID),
                                price(6174)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179112")),
                                ssku("00065.00026.100126179112"),
                                bundle(expectedBundle2),
                                warehouse(WAREHOUSE_ID),
                                price(4742)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179112")),
                                ssku("00065.00026.100126179112"),
                                bundle(expectedBundle3),
                                warehouse(WAREHOUSE_ID),
                                price(4742)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179060")),
                                ssku("00065.00026.100126179060"),
                                warehouse(WAREHOUSE_ID),
                                bundle(expectedBundle2),
                                promoKeys(BUNDLE_PROMO_KEY),
                                price(244)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126179112")),
                                ssku("00065.00026.100126179112"),
                                bundle(expectedBundle1),
                                warehouse(WAREHOUSE_ID),
                                price(4742)
                        )
                        .withOrderItem(
                                itemKey(VIRTUAL_FEED_ID, offerId(FEED_ID, "00065.00026.100126178009")),
                                ssku("00065.00026.100126178009"),
                                warehouse(WAREHOUSE_ID),
                                promoKeys(OTHER_BUNDLE_PROMO_KEY),
                                price(2274),
                                quantity(3)
                        )
                        .build()).build());

        OrderWithBundlesResponse order = firstOrderOf(discountResponse);
        assertThat(order.getBundlesToDestroy(), empty());
        assertThat(order.getBundles(), hasSize(3));
        assertThat(order.getBundles(), hasItems(
                allOf(
                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                        hasProperty("bundleId", is(expectedBundle1))
                ),
                allOf(
                        hasProperty("promoKey", is(BUNDLE_PROMO_KEY)),
                        hasProperty("bundleId", is(expectedBundle2))
                ),
                allOf(
                        hasProperty("promoKey", is(OTHER_BUNDLE_PROMO_KEY)),
                        hasProperty("bundleId", is(expectedBundle3))
                )
        ));
        assertThat(order.getItems(), hasSize(7));
        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126178007"))),
                        hasProperty("bundleId", is(expectedBundle1))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179112"))),
                        hasProperty("bundleId", is(expectedBundle1))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179060"))),
                        hasProperty("bundleId", is(expectedBundle2))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179112"))),
                        hasProperty("bundleId", is(expectedBundle2))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126178009"))),
                        hasProperty("bundleId", is(expectedBundle3))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126179112"))),
                        hasProperty("bundleId", is(expectedBundle3))
                ),
                allOf(
                        hasProperty("offerId", is(offerId(FEED_ID, "00065.00026.100126178009"))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
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
                        ItemKey.withBundle(item.getFeedId(), item.getOfferId(), ItemKey.SINGLE_CART_ID, parseOrderId(response.getOrderId()),
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
}
