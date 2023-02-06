package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDefinition;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest.Cart;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.bundle;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.changeItems;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.itemRestrictions;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.maxQuantity;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.minQuantity;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.quantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.restrictions;

public class GiftWithPurchaseBundleConstructionStrategyTest extends AbstractBundleStrategyTest {

    @Test
    public void shouldConstructBundleOnValidCart() {
        PromoBundleDescription expected = typicalDescription();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        someKey(),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        someKey(),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                ).build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasSize(1));
        assertThat(bundles, hasItem(hasProperty(
                "promoKey",
                equalTo(PROMO_KEY)
        )));

        assertThat(items, hasSize(2));
    }

    @Test
    public void shouldNotConstructBundleOnDifferentWarehouse() {
        PromoBundleDescription expected = typicalDescription();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        someKey(),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        someKey(),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(ANOTHER_WAREHOUSE_ID),
                        price(5000)
                ).build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, empty());
        assertThat(items, hasSize(2));
    }

    @Test
    public void shouldConstructBundlesProportionally() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                quantityInBundle(3),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        String expectedBundleId = generateBundleId(expected, FIRST_SSKU, GIFT_ITEM_SSKU);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(10)
                )
                .build(), expected);
        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasSize(1));
        assertThat(bundles, hasItem(allOf(
                hasProperty("promoKey", equalTo(PROMO_KEY)),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        )));

        assertThat(items, hasSize(4));
        assertThat(items, hasItems(
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(9)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(7)))
                )
        ));
    }

    @Test
    public void shouldNotConstructBundlesOnRestrictionViolation() {
        PromoBundleDescription expected = typicalDescription(
                restrictions(
                        minQuantity(3),
                        maxQuantity(10)
                ),
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                quantityInBundle(2),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        someKey(),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                )
                .withOrderItem(
                        someKey(),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(1)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, empty());
        assertThat(items, hasSize(2));
    }

    @Test
    public void shouldConstructBundlesOnRestrictionPass() {
        PromoBundleDescription expected = typicalDescription(
                restrictions(
                        minQuantity(3),
                        maxQuantity(10)
                ),
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                quantityInBundle(2),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        String expectedBundleId = generateBundleId(expected, FIRST_SSKU, GIFT_ITEM_SSKU);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(6)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(5)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasSize(1));
        assertThat(bundles, hasItem(allOf(
                hasProperty("promoKey", equalTo(PROMO_KEY)),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        )));

        assertThat(items, hasSize(3));
        assertThat(items, hasItems(
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(6)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
                )
        ));
    }

    @Test
    public void shouldNotConstructBundlesOnItemRestrictionViolation() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                quantityInBundle(2),
                                primary(),
                                itemRestrictions(
                                        minQuantity(3),
                                        maxQuantity(10)
                                )
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        someKey(),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                )
                .withOrderItem(
                        someKey(),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(1)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, empty());
        assertThat(items, hasSize(2));
    }

    @Test
    public void shouldConstructBundlesOnItemRestrictionPass() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                quantityInBundle(2),
                                primary(),
                                itemRestrictions(
                                        minQuantity(3),
                                        maxQuantity(10)
                                )
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        String offerId1 = randomString();
        String offerId2 = randomString();

        String expectedBundleId = generateBundleId(expected, offerId1, offerId2);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(offerId1),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(4)
                )
                .withOrderItem(
                        itemKey(offerId2),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(2)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasSize(1));
        assertThat(bundles, hasItem(allOf(
                hasProperty("promoKey", equalTo(PROMO_KEY)),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
        )));

        assertThat(items, hasSize(2));
        assertThat(items, everyItem(
                hasProperty(
                        "itemKey",
                        hasProperty("bundleId", equalTo(expectedBundleId))
                )
        ));
    }

    @Test
    public void shouldConstructWithMaxQuantityRestrictions() {
        PromoBundleDescription expected = typicalDescription(
                restrictions(
                        minQuantity(1),
                        maxQuantity(2)
                )
        );

        String expectedBundleId = generateBundleId(expected, FIRST_SSKU, GIFT_ITEM_SSKU);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(10)
                )
                .build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasSize(1));
        assertThat(bundles, hasItem(allOf(
                hasProperty("promoKey", equalTo(PROMO_KEY)),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
        )));

        assertThat(items, hasSize(4));
        assertThat(items, hasItems(
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(8)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(8)))
                )
        ));
    }

    @Test
    public void shouldConstructWithMaxItemQuantityRestrictions() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary(),
                                itemRestrictions(
                                        minQuantity(1),
                                        maxQuantity(5)
                                )
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        String expectedBundleId = generateBundleId(expected, FIRST_SSKU, GIFT_ITEM_SSKU);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(10)
                )
                .build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasItem(allOf(
                hasProperty("promoKey", equalTo(PROMO_KEY)),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(5)))
        )));

        assertThat(items, hasSize(4));
        assertThat(items, hasItems(
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(5)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(5)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(5)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(5)))
                )
        ));
    }

    @Test
    public void shouldConstructDifferentVariantsForSameBundle() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU, ANOTHER_GIFT_ITEM_SSKU))
                        )
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        quantity(2),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .withOrderItem(
                        itemKey(ANOTHER_GIFT_ITEM_SSKU),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasSize(2));
        assertThat(bundles, hasItems(
                allOf(
                        hasProperty("promoKey", equalTo(PROMO_KEY)),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("items", hasItems(
                                hasProperty("offerId", equalTo(FIRST_SSKU)),
                                hasProperty("offerId", equalTo(GIFT_ITEM_SSKU))
                        ))
                ),
                allOf(
                        hasProperty("promoKey", equalTo(PROMO_KEY)),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("items", hasItems(
                                hasProperty("offerId", equalTo(FIRST_SSKU)),
                                hasProperty("offerId", equalTo(ANOTHER_GIFT_ITEM_SSKU))
                        ))
                )
        ));
    }

    @Test
    public void shouldForceBundlesFromRequestInVariants() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU, ANOTHER_GIFT_ITEM_SSKU))
                        )
                )
        );

        String expectedBundleId = generateBundleId(expected, FIRST_SSKU, GIFT_ITEM_SSKU);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        bundle(BUNDLE_ID),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        bundle(BUNDLE_ID),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .withOrderItem(
                        itemKey(ANOTHER_GIFT_ITEM_SSKU),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasSize(1));
        assertThat(bundles, hasItems(
                allOf(
                        hasProperty("promoKey", equalTo(PROMO_KEY)),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("items", hasItems(
                                hasProperty("offerId", equalTo(FIRST_SSKU)),
                                hasProperty("offerId", equalTo(GIFT_ITEM_SSKU))
                        ))
                )
        ));

        assertThat(items, hasSize(3));
        assertThat(items, hasItems(
                hasProperty("itemKey", equalTo(
                        ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(), expectedBundleId)
                )),
                hasProperty("itemKey", equalTo(
                        ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), expectedBundleId)
                )),
                hasProperty("itemKey", equalTo(
                        ItemKey.withBundle(FEED_ID, ANOTHER_GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), null)
                ))
        ));
    }

    @Test
    public void shouldForceBundlesFromRequestInVariants2() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU, ANOTHER_GIFT_ITEM_SSKU))
                        )
                )
        );

        String expectedBundleId = generateBundleId(expected, FIRST_SSKU, ANOTHER_GIFT_ITEM_SSKU);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        bundle(BUNDLE_ID),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .withOrderItem(
                        itemKey(ANOTHER_GIFT_ITEM_SSKU),
                        bundle(BUNDLE_ID),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasSize(1));
        assertThat(bundles, hasItems(
                allOf(
                        hasProperty("promoKey", equalTo(PROMO_KEY)),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("items", hasItems(
                                hasProperty("offerId", equalTo(FIRST_SSKU)),
                                hasProperty("offerId", equalTo(ANOTHER_GIFT_ITEM_SSKU))
                        ))
                )
        ));

        assertThat(items, hasSize(3));
        assertThat(items, hasItems(
                hasProperty("itemKey", equalTo(
                        ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(), expectedBundleId)
                )),
                hasProperty("itemKey", equalTo(
                        ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), null)
                )),
                hasProperty("itemKey", equalTo(
                        ItemKey.withBundle(FEED_ID, ANOTHER_GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(),
                                expectedBundleId)
                ))
        ));
    }

    @Test
    public void shouldSplitItemsByBundle() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU, ANOTHER_GIFT_ITEM_SSKU))
                        )
                )
        );

        String expectedBundleId = generateBundleId(expected, FIRST_SSKU, GIFT_ITEM_SSKU);

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
                        quantity(2)
                )
                .build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);
        Collection<Item> items = itemsOf(combineResult);

        assertThat(bundles, not(empty()));
        assertThat(bundles, hasItem(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
        ));
        assertThat(items, hasSize(3));
        assertThat(items, hasItems(
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(),
                                        expectedBundleId)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
                ),
                allOf(
                        hasProperty("itemKey", equalTo(
                                ItemKey.withBundle(FEED_ID, GIFT_ITEM_SSKU, cart.getId(), cart.getOrderId(), null)
                        )),
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
                )
        ));
    }

    @Test
    public void shouldNotDestroyBundlesOnIdEquals() {
        PromoBundleDescription expected = typicalDescription();

        String offerId1 = randomString();
        String offerId2 = randomString();

        String expectedBundleId = generateBundleId(expected, offerId1, offerId2);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(offerId1),
                        bundle(expectedBundleId),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(offerId2),
                        bundle(expectedBundleId),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .build(), expected);

        assertThat(combineResult.getCarts(), not(empty()));

        Cart cart = firstCart(combineResult);

        assertThat(cart, notNullValue());
        assertThat(combineResult.getBundlesToDestroy().size(), comparesEqualTo(0));
        assertThat(combineResult.getBundles(cart), hasSize(1));
        assertThat(combineResult.getBundles(cart), hasItem(hasProperty(
                "promoKey",
                equalTo(PROMO_KEY)
        )));
    }

    @Test
    public void shouldDestroyBundles() {
        PromoBundleDescription expected = typicalDescription();

        String offerId1 = randomString();
        String offerId2 = randomString();

        String expectedBundleId = generateBundleId(expected, offerId1, offerId2);

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(offerId2),
                        bundle(expectedBundleId),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .build(), expected);

        assertThat(combineResult.getCarts(), not(empty()));

        Cart cart = firstCart(combineResult);

        assertThat(cart, notNullValue());
        assertThat(combineResult.getBundlesToDestroy().size(), greaterThan(0));
    }

    @Test
    public void shouldBuildBundlesWithOnePrimaryItemAndSeveralSecondaryItems() {
        PromoBundleDescription expected = typicalDescription();
        PromoBundleDescription expectedAnother = descriptionWithAnotherGift();

        String offerId1 = randomString();
        String offerId2 = randomString();
        String offerId3 = randomString();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(offerId1),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY, ANOTHER_PROMO_KEY),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(offerId2),
                        ssku(ANOTHER_GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                )
                .withOrderItem(
                        itemKey(offerId3),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(3000)
                )
                .build(), Set.of(expected, expectedAnother));

        assertThat(combineResult.getCarts(), not(empty()));
        assertThat(combineResult.getBundlesToDestroy().size(), equalTo(0));

        Cart cart = firstCart(combineResult);

        assertThat(cart, notNullValue());
    }
}
