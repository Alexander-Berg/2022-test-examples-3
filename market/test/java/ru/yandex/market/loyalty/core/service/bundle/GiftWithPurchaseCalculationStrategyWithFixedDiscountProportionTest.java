package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest.Cart;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.changeItems;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.condition;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.directionalMapping;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.giftWithPurchase;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.item;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primaryItem;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.proportion;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.quantityInBundle;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.then;
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.when;

public class GiftWithPurchaseCalculationStrategyWithFixedDiscountProportionTest extends AbstractBundleStrategyTest {

    @Test
    public void shouldCalculateProportionalDiscountByFixedProportion() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        primaryItem(FEED_ID, FIRST_SSKU),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(40)
                        ))
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
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
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(5000))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(3000))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(2000))
                                )
                        )))
        ));
    }

    @Test
    public void shouldCalculateProportionalDiscountWithIllegalFixedProportion() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        primaryItem(FEED_ID, FIRST_SSKU),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(90)
                        ))
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(100)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(100))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.TEN)
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(90))
                                )
                        )))
        ));
    }

    @Test
    public void shouldCalculateProportionalDiscountWithIllegalSummaryProportion() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        primaryItem(FEED_ID, directionalMapping(
                                then(FIRST_SSKU),
                                proportion(60)
                        )),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(60)
                        ))
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
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
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(5000))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(2500))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(2500))
                                )
                        )))
        ));
    }

    @Test
    public void shouldCalculateProportionalDiscountWithDifferentQuantities() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, directionalMapping(
                                        then(FIRST_SSKU),
                                        proportion(60)
                                ))),
                                quantityInBundle(2),
                                primary()
                        ),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(60)
                        ))
                )
        );

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
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(5000))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(1250))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(2500))
                                )
                        )))
        ));
    }

    @Test
    public void shouldCalculateProportionalDiscountWithIrrationalProportions() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        primaryItem(FEED_ID, FIRST_SSKU),
                        item(
                                condition(giftWithPurchase(FEED_ID, directionalMapping(
                                        then(SECOND_SSKU),
                                        proportion(43.1275)
                                ))),
                                quantityInBundle(2),
                                primary()
                        ),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                when(SECOND_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(13.045)
                        ))
                )
        );

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
                        price(7500),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);

        assertThat(proportionOf(5000, 43.8275, 100), comparesEqualTo(BigDecimal.valueOf(2191)));
        assertThat(proportionOf(5000, 43.1275, 200), comparesEqualTo(BigDecimal.valueOf(1078)));
        assertThat(proportionOf(5000, 13.045, 100), comparesEqualTo(BigDecimal.valueOf(652)));

        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(5000))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(3)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(2191 + 1))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(SECOND_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(1078))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(652))
                                )
                        )))
        ));
    }

    @Test
    public void shouldCalculateSimpleProportionalDiscount() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        primaryItem(FEED_ID, FIRST_SSKU),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(40)
                        ))
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(6291),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2570),
                        promoKeys(PROMO_KEY)
                )
                .build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(2570))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(1542))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(1028))
                                )
                        )))
        ));
    }

    @Test
    public void shouldCalculateZeroProportionForPrimaryItem() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        primaryItem(FEED_ID, FIRST_SSKU),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(100)
                        ))
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(3831),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2537)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(2537))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(1))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(2536))
                                )
                        )))
        ));
    }

    @Test
    public void shouldCalculateZeroProportionForSecondaryItem() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        primaryItem(FEED_ID, FIRST_SSKU),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(0)
                        ))
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(3831),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2537)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(2537))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(2537))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(0))
                                )
                        )))
        ));
    }

    @Test
    public void shouldCalculateZeroProportionForSecondaryItemWithEqualsPrices() {
        PromoBundleDescription expected = typicalDescription(
                changeItems(
                        primaryItem(FEED_ID, FIRST_SSKU),
                        giftItem(FEED_ID, directionalMapping(
                                when(FIRST_SSKU),
                                then(GIFT_ITEM_SSKU),
                                proportion(0)
                        ))
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2537),
                        promoKeys(PROMO_KEY)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2537)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(2537))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", is(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(FIRST_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(2536))
                                )
                        ), allOf(
                                hasProperty("feedId", is(FEED_ID)),
                                hasProperty("offerId", is(GIFT_ITEM_SSKU)),
                                hasProperty(
                                        "discount",
                                        comparesEqualTo(BigDecimal.valueOf(1))
                                )
                        )))
        ));
    }
}
