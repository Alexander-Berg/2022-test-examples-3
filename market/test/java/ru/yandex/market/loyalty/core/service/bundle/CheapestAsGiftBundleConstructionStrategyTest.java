package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDefinition;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest.Cart;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.internal.util.collections.Iterables.firstOf;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.CHEAPEST_AS_GIFT;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.bundleDescription;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.changeItems;
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

public class CheapestAsGiftBundleConstructionStrategyTest extends AbstractBundleStrategyTest {

    @Test
    public void shouldConstructBundlesWithOneItem() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(1)),
                hasProperty("items", hasItem(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(3)))
                )))
        ));
    }

    @Test
    public void shouldConstructBundlesWithDifferentItems() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY),
                        quantity(1)
                ).build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(2)))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE))
                )))
        ));
    }

    @Test
    public void shouldConstructBundlesWithTwoItemsAndRemain() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(2)))
                )))
        ));
    }

    @Test
    public void shouldNotConstructBundlesOnDifferentWarehouse() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(randomString()),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(randomString()),
                        ssku(SECOND_SSKU),
                        warehouse(ANOTHER_WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(0));
    }

    @Test
    public void shouldNotConstructVariantsOnInsufficientQuantity() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(randomString()),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(0));
    }

    @Test
    public void shouldConstructMultipleBundles() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3))),
                hasProperty("items", hasSize(1)),
                hasProperty("items", hasItem(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(3)))
                )))
        ));
    }

    @Test
    public void shouldConstructMultipleBundlesWithoutSplitting() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                )
                .build(), expected);

        Collection<Item> items = itemsOf(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(items), hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(10))));
    }

    @Test
    public void shouldConstructDifferentVariants() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                )
                .withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY),
                        quantity(6)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(2));
        assertThat(bundles, hasItems(allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(1)),
                hasProperty("items", hasItem(hasProperty("offerId", is(FIRST_SSKU))))
        ), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2))),
                hasProperty("items", hasSize(1)),
                hasProperty("items", hasItem(hasProperty("offerId", is(SECOND_SSKU))))
        )));
    }

    @Test
    public void shouldConstructMixedVariants() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY),
                        quantity(5)
                )
                .withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(3));
        assertThat(bundles, hasItems(allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(1)),
                hasProperty("items", hasItem(hasProperty("offerId", is(FIRST_SSKU))))
        ), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(2)))
                        ),
                        allOf(
                                hasProperty("offerId", is(SECOND_SSKU)),
                                hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(1)))
                        )
                ))
        ), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3))),
                hasProperty("items", hasSize(1)),
                hasProperty("items", hasItem(hasProperty("offerId", is(SECOND_SSKU))))
        )));
    }

    @Test
    public void shouldNotSplitItemsByBundle() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                )
                .build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, hasSize(1));

        assertThat(items, hasSize(2));
        assertThat(items, hasItems(
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
                )
        ));
    }

    @Test
    public void shouldNotSplitCartWithThreeItemsByBundle() {
        PromoBundleDescription expected = typicalDescription(
                strategy(CHEAPEST_AS_GIFT),
                withQuantityInBundle(3),
                changeItems(
                        item(
                                condition(cheapestAsGift(
                                        FeedSskuSet.of(FEED_ID, List.of(FIRST_SSKU, GIFT_ITEM_SSKU, THIRD_SSKU)))),
                                quantityInBundle(3),
                                primary()
                        )
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(1)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY),
                        quantity(1)
                )
                .withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(4000),
                        promoKeys(PROMO_KEY),
                        quantity(1)
                )
                .build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, hasSize(1));

        assertThat(items, hasSize(3));
        assertThat(items, hasItems(
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, THIRD_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
                )
        ));
    }

    private PromoBundleDescription createBundle() {
        return bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(PROMO_KEY),
                shopPromoId(randomString()),
                strategy(CHEAPEST_AS_GIFT),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                withQuantityInBundle(3),
                item(
                        condition(cheapestAsGift(FeedSskuSet.of(FEED_ID, List.of(FIRST_SSKU, SECOND_SSKU)))),
                        quantityInBundle(3),
                        primary()
                )
        ));
    }
}
