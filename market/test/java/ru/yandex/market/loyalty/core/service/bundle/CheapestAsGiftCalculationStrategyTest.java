package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
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

public class CheapestAsGiftCalculationStrategyTest extends AbstractBundleStrategyTest {

    @Test
    public void shouldCalcDiscountCartWithOneItem() {
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

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(10002)));
        assertThat(calculation.getItems(), hasSize(1));
        assertThat(calculation.getItems(), hasItem(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        )));
        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(3334)))
        )));
    }

    @Test
    public void shouldCalcDiscountCartWithOneItemWithRepetition() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(8)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(20000)));
        assertThat(calculation.getItems(), hasSize(1));
        assertThat(calculation.getItems(), hasItem(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(8)))
        )));
        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.valueOf(2)));
        assertThat(firstBundle(calculation).getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(2500)))
        )));
    }

    @Test
    public void shouldCalcDiscountCartWithTwoItems() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(60)));
        assertThat(calculation.getItems(), hasSize(2));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
        )));
        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(16)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14)))
        )));
    }

    @Test
    public void shouldCalcDiscountCartWithThreeItems() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);

        assertThat(calculation.getItems(), hasSize(3));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, THIRD_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(1)))
        )));
        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(21)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(19)))
        ), allOf(
                hasProperty("offerId", is(THIRD_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(19)))
        )));
    }

    @Test
    public void shouldCalcDiscountCartWithDifferentVariants() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(300),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(200),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(100),
                        promoKeys(PROMO_KEY),
                        quantity(4)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        Collection<SuccessPromoBundle> calculations = calculateDiscounts(combineResult, cart);

        assertThat(calculations, hasSize(1));
        assertThat(firstOf(calculations).getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(402)));
        assertThat(firstOf(calculations).getPromoBundleDiscounts(), hasItems(allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(1)),
                hasProperty("items", hasItem(allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(3))),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(21)))
                )))
        ), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(THIRD_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(21)))
                ), allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(2))),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(42)))
                )))
        ), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(42)))
                ), allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.valueOf(2))),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(64)))
                )))
        )));
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
                        condition(cheapestAsGift(FeedSskuSet.of(FEED_ID, List.of(FIRST_SSKU, SECOND_SSKU, THIRD_SSKU)))),
                        quantityInBundle(3),
                        primary()
                )
        ));
    }
}
