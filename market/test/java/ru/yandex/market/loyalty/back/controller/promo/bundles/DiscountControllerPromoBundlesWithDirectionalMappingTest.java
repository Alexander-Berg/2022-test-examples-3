package ru.yandex.market.loyalty.back.controller.promo.bundles;

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
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.bundle.PromoBundleService;
import ru.yandex.market.loyalty.core.utils.PromoBundleUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.GIFT_WITH_PURCHASE;
import static ru.yandex.market.loyalty.core.utils.CartUtils.generateBundleId;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
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
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoKey;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.shopPromoId;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.starts;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.strategy;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;

@TestFor(DiscountController.class)
public class DiscountControllerPromoBundlesWithDirectionalMappingTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FEED_ID = 123;
    private static final long VIRTUAL_FEED_ID = 12345;
    private static final String BUNDLE_PROMO_KEY = "some promo";
    private static final String FIRST_ITEM_SSKU = "first promo offer";
    private static final String FIRST_ITEM_SSKU_OFFER_ID = FEED_ID + "." + FIRST_ITEM_SSKU;
    private static final String SECOND_ITEM_SSKU = "second promo offer";
    private static final String SECOND_ITEM_SSKU_OFFER_ID = FEED_ID + "." + SECOND_ITEM_SSKU;
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
                promoKey(BUNDLE_PROMO_KEY),
                shopPromoId(BUNDLE_PROMO_KEY),
                strategy(GIFT_WITH_PURCHASE),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(10)),
                PromoBundleUtils.item(
                        condition(giftWithPurchase(FEED_ID, FIRST_ITEM_SSKU, SECOND_ITEM_SSKU)),
                        primary()
                ),
                PromoBundleUtils.item(
                        condition(giftWithPurchase(
                                FEED_ID,
                                directionalMapping(
                                        when(FIRST_ITEM_SSKU),
                                        then(GIFT_ITEM_SSKU)
                                ),
                                directionalMapping(
                                        when(SECOND_ITEM_SSKU),
                                        then(ANOTHER_GIFT_ITEM_SSKU)
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldConstructBundlesWithValidDirectionalMapping() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, FIRST_ITEM_SSKU_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        price(123)
                ).build();

        String bundleId = generateBundleId(promoBundleDescription, VIRTUAL_FEED_ID,
                FIRST_ITEM_SSKU_OFFER_ID, GIFT_ITEM_OFFER_ID
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(1));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());
        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));
        assertItemsQuantity(FIRST_ITEM_SSKU_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_ITEM_SSKU_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId))
                )
        ));
    }

    @Test
    public void shouldNotConstructBundlesWithInvalidDirectionalMapping() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, FIRST_ITEM_SSKU_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, ANOTHER_GIFT_OFFER_ID),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        price(123)
                ).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), empty());
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());
    }

    @Test
    public void shouldConstructBundlesOnlyForValidDirectionalMapping() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, FIRST_ITEM_SSKU_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        price(123)
                ).withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, ANOTHER_GIFT_OFFER_ID),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        price(123)
                ).build();

        String bundleId = generateBundleId(promoBundleDescription, VIRTUAL_FEED_ID,
                FIRST_ITEM_SSKU_OFFER_ID, GIFT_ITEM_OFFER_ID
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        assertThat(firstOrderOf(discountResponse).getBundles(), hasSize(1));
        assertThat(firstOrderOf(discountResponse).getBundlesToDestroy(), empty());
        assertItemsMultiplicityInBundle(firstOrderOf(discountResponse));
        assertItemsQuantity(FIRST_ITEM_SSKU_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertItemsQuantity(ANOTHER_GIFT_OFFER_ID, 1, firstOrderOf(discountResponse));
        assertThat(firstOrderOf(discountResponse).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_ITEM_SSKU_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_ITEM_OFFER_ID)),
                        hasProperty("bundleId", is(bundleId))
                )
        ));
    }

    @Test
    public void shouldAvoidConstructBundlesWithInvalidDirectionalMapping() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, FIRST_ITEM_SSKU_OFFER_ID),
                        ssku(FIRST_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, SECOND_ITEM_SSKU_OFFER_ID),
                        ssku(SECOND_ITEM_SSKU),
                        promoKeys(BUNDLE_PROMO_KEY),
                        price(100000),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, GIFT_ITEM_OFFER_ID),
                        ssku(GIFT_ITEM_SSKU),
                        price(123)
                )
                .withOrderItem(
                        itemKey(VIRTUAL_FEED_ID, ANOTHER_GIFT_OFFER_ID),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        price(123)
                ).build();

        String firstBundleId = generateBundleId(promoBundleDescription, VIRTUAL_FEED_ID,
                FIRST_ITEM_SSKU_OFFER_ID, GIFT_ITEM_OFFER_ID
        );

        String secondBundleId = generateBundleId(promoBundleDescription, VIRTUAL_FEED_ID,
                SECOND_ITEM_SSKU_OFFER_ID, ANOTHER_GIFT_OFFER_ID
        );

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).build());

        OrderWithBundlesResponse bundlesResponse = firstOrderOf(discountResponse);

        assertThat(bundlesResponse.getBundles(), hasSize(2));
        assertThat(bundlesResponse.getBundlesToDestroy(), empty());

        assertThat(bundlesResponse.getBundles(), hasItems(
                hasProperty("bundleId", is(firstBundleId)),
                hasProperty("bundleId", is(secondBundleId))
        ));


        assertItemsMultiplicityInBundle(bundlesResponse);
        assertItemsQuantity(FIRST_ITEM_SSKU_OFFER_ID, 2, bundlesResponse);
        assertItemsQuantity(SECOND_ITEM_SSKU_OFFER_ID, 2, bundlesResponse);
        assertItemsQuantity(GIFT_ITEM_OFFER_ID, 1, bundlesResponse);
        assertItemsQuantity(ANOTHER_GIFT_OFFER_ID, 1, bundlesResponse);
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
}
