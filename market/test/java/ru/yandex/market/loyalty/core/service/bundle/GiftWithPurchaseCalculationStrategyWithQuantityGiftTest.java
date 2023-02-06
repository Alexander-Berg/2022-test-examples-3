package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;

public class GiftWithPurchaseCalculationStrategyWithQuantityGiftTest extends AbstractBundleStrategyTest {

    private PromoBundleDescription expected;

    @Before
    public void prepare() {
        expected = typicalDescription();
    }

    @Test
    public void shouldSearchGiftInEachBundleForBundledItems() {
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
                )
                .build(), expected);

        assertThat(combineResult.getCarts(), not(empty()));

        DiscountCalculationRequest.Cart cart = firstCart(combineResult);

        assertThat(combineResult.getBundles(cart), not(empty()));

        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);

        theGiftExistsAndOneGiftInBundle(calculation);
        assertThat(calculation.getPromoBundleDiscounts().size(), comparesEqualTo(1));
    }

    @Test
    public void shouldSearchGiftInEachBundleForBundledItemsWithDifferentQuantity() {
        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(PROMO_KEY),
                        quantity(7)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(5)
                )
                .build(), expected);

        assertThat(combineResult.getCarts(), not(empty()));

        DiscountCalculationRequest.Cart cart = firstCart(combineResult);

        assertThat(combineResult.getBundles(cart), not(empty()));

        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);

        theGiftExistsAndOneGiftInBundle(calculation);
        assertThat(calculation.getPromoBundleDiscounts().size(), comparesEqualTo(1));
    }

    private void theGiftExistsAndOneGiftInBundle(SuccessPromoBundle calculation) {
        assertTrue(
                calculation.getPromoBundleDiscounts().stream()
                        .allMatch(bundle -> bundle.getItems().stream()
                                .anyMatch(promoBundleItemDiscount -> !promoBundleItemDiscount.isPrimaryInBundle()))
        );
        assertTrue(
                calculation.getPromoBundleDiscounts().stream()
                        .allMatch(bundle -> bundle.getItems().stream()
                                .filter(promoBundleItemDiscount -> !promoBundleItemDiscount.isPrimaryInBundle())
                                .count() == 1L)
        );
    }
}
