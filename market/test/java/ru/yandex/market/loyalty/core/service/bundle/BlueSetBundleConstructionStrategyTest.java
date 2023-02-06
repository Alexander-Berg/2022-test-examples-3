package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDefinition;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;

import java.math.BigDecimal;
import java.util.Collection;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.internal.util.collections.Iterables.firstOf;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
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

public class BlueSetBundleConstructionStrategyTest extends AbstractBundleStrategyTest {

    @Test
    public void shouldNotConstructBundleForOrderWithNoNecessaryItems() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, empty());
    }

    @Test
    public void shouldConstructBundleForOrderWithNecessaryItems() {
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
                        promoKeys(PROMO_KEY)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(10))))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(20))))
                )))
        ));
    }

    @Test
    public void shouldConstructBundleForOrderWithDifferentItems() {
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
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(FOURTH_SSKU),
                        ssku(FOURTH_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(ANOTHER_SSKU),
                        ssku(ANOTHER_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(10))))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(20))))
                )))
        ));
    }

    @Test
    public void shouldConstructBundleVariantWithTheBiggestItemsCount() {
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
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(FOURTH_SSKU),
                        ssku(FOURTH_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(4)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30))))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(35))))
                ), allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(40))))
                ), allOf(
                        hasProperty("offerId", is(FOURTH_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(45))))
                )))
        ));
    }

    @Test
    public void shouldConstructBundleVariantWithProportionalCapacity() {
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
                        quantity(3)
                )
                .withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3))),
                hasProperty("items", hasSize(3)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(20))))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(25))))
                ), allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30))))
                )))
        ));
    }

    @Test
    public void shouldConstructBundleVariantWithProportionalCapacityOnNotProportionalItemCount() {
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
                        quantity(4)
                )
                .withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2000),
                        promoKeys(PROMO_KEY),
                        quantity(5)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(1));
        assertThat(firstOf(bundles), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3))),
                hasProperty("items", hasSize(3)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(20))))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(25))))
                ), allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30))))
                )))
        ));
    }

    @Test
    public void shouldConstructDifferentBundleVariantsWithSomeCapacity() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(5)
                )
                .withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        promoKeys(PROMO_KEY),
                        quantity(4)
                )
                .withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                )
                .build(), expected);

        Collection<PromoBundleDefinition> bundles = bundlesOf(combineResult);

        assertThat(bundles, hasSize(2));
        assertThat(bundles, hasItems(allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3))),
                hasProperty("items", hasSize(3)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(20))))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(25))))
                ), allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(30))))
                )))
        ), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(10))))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("offerDiscount",
                                hasProperty("proportion", comparesEqualTo(BigDecimal.valueOf(20))))
                )))
        )));
    }

    private PromoBundleDescription createBundle() {
        return bundleService.createPromoBundle(bundleDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                promoKey(PROMO_KEY),
                shopPromoId(randomString()),
                strategy(BLUE_SET),
                starts(clock.dateTime()),
                ends(clock.dateTime().plusYears(1)),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_SSKU, 10),
                                proportion(SECOND_SSKU, 20)
                        )),
                        primary()
                ),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_SSKU, 20),
                                proportion(SECOND_SSKU, 25),
                                proportion(THIRD_SSKU, 30)
                        )),
                        primary()
                ),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_SSKU, 30),
                                proportion(SECOND_SSKU, 35),
                                proportion(THIRD_SSKU, 40),
                                proportion(FOURTH_SSKU, 45)
                        )),
                        primary()
                )
        ));
    }
}
