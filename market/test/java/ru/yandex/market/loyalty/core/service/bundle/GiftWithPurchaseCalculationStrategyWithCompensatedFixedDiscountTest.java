package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest.Cart;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
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
import static ru.yandex.market.loyalty.core.utils.PromoBundleUtils.primary;

public class GiftWithPurchaseCalculationStrategyWithCompensatedFixedDiscountTest extends AbstractBundleStrategyTest {

    private PromoBundleDescription expectedBundle;

    @Before
    public void prepare() {
        expectedBundle = typicalDescription(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );
    }

    @Test
    public void shouldCalculateDiscountForBundledItems() {
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
                ).build(), expectedBundle);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(5000)));
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("feedId", comparesEqualTo(FEED_ID)),
                        hasProperty("offerId", equalTo(GIFT_ITEM_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty(
                                "discount",
                                comparesEqualTo(BigDecimal.valueOf(4999))
                        )
                ), allOf(
                        hasProperty("feedId", comparesEqualTo(FEED_ID)),
                        hasProperty("offerId", equalTo(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty(
                                "discount",
                                comparesEqualTo(BigDecimal.ONE)
                        )
                )))
        ));
    }

    @Test
    public void shouldCalculateDiscountForBundledItemsWithDifferentQuantity() {
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
                ).build(), expectedBundle);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(5000).multiply(BigDecimal.valueOf(5)))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(5))),
                hasProperty("items", hasSize(2)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("feedId", comparesEqualTo(FEED_ID)),
                        hasProperty("offerId", equalTo(GIFT_ITEM_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty(
                                "discount",
                                comparesEqualTo(BigDecimal.valueOf(4999))
                        )
                ), allOf(
                        hasProperty("feedId", comparesEqualTo(FEED_ID)),
                        hasProperty("offerId", equalTo(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty(
                                "discount",
                                comparesEqualTo(BigDecimal.ONE)
                        )
                )))
        ));
    }

    @Test
    public void shouldCalculateDiscountForBundledItemsWithSomePrimaryItems() {
        expectedBundle = descriptionWithAnotherGift(
                changeItems(
                        item(
                                condition(giftWithPurchase(FEED_ID, FIRST_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, SECOND_SSKU)),
                                primary()
                        ),
                        item(
                                condition(giftWithPurchase(FEED_ID, GIFT_ITEM_SSKU))
                        )
                )
        );

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(10000),
                        promoKeys(ANOTHER_PROMO_KEY),
                        quantity(7)
                )
                .withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(7000),
                        promoKeys(ANOTHER_PROMO_KEY),
                        quantity(7)
                )
                .withOrderItem(
                        itemKey(GIFT_ITEM_SSKU),
                        ssku(GIFT_ITEM_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(5000),
                        quantity(5)
                ).build(), expectedBundle);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(
                calculation.getTotalDiscount(),
                comparesEqualTo(BigDecimal.valueOf(5000).multiply(BigDecimal.valueOf(5)))
        );
        assertThat(firstBundle(calculation), allOf(
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(5))),
                hasProperty("items", hasSize(3)),
                hasProperty("items", hasItems(allOf(
                        hasProperty("feedId", comparesEqualTo(FEED_ID)),
                        hasProperty("offerId", equalTo(GIFT_ITEM_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty(
                                "discount",
                                comparesEqualTo(BigDecimal.valueOf(4999))
                        )
                ), allOf(
                        hasProperty("feedId", comparesEqualTo(FEED_ID)),
                        hasProperty("offerId", equalTo(FIRST_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty(
                                "discount",
                                comparesEqualTo(BigDecimal.ONE)
                        )
                ), allOf(
                        hasProperty("feedId", comparesEqualTo(FEED_ID)),
                        hasProperty("offerId", equalTo(SECOND_SSKU)),
                        hasProperty("quantityInBundles", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty(
                                "discount",
                                comparesEqualTo(BigDecimal.ZERO)
                        )
                )))
        ));
    }

}
