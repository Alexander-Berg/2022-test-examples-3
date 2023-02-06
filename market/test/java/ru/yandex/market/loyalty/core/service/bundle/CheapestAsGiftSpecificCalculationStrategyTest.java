package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest.Cart;

import java.math.BigDecimal;
import java.util.List;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
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


/**
 * https://st.yandex-team.ru/MARKETCHECKOUT-12591
 */
public class CheapestAsGiftSpecificCalculationStrategyTest extends AbstractBundleStrategyTest {

    private static final String TEA_1 = "tea 1";
    private static final String TEA_2 = "tea 2";
    private static final String TEA_3 = "tea 3";
    private static final String TEA_4 = "tea 4";
    private static final String TEA_5 = "tea 5";
    private static final String TEA_6 = "tea 6";
    private static final String TEA_7 = "tea 7";

    @Test
    public void shouldCalcDiscountForThreeOffersInCart() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_3),
                        ssku(TEA_3),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(59)));
        assertThat(calculation.getItemDiscount().getDiscounts(), allOf(
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_1, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(21)) //43
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_2, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(19)) //40
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_3, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(19)) //40
                )
        ));
    }

    @Test
    public void shouldCalcDiscountForMixedOffersInCart() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_4),
                        ssku(TEA_4),
                        warehouse(WAREHOUSE_ID),
                        price(40),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_3),
                        ssku(TEA_3),
                        warehouse(WAREHOUSE_ID),
                        price(50),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_5),
                        ssku(TEA_5),
                        warehouse(WAREHOUSE_ID),
                        price(54),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_6),
                        ssku(TEA_6),
                        warehouse(WAREHOUSE_ID),
                        price(54),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(56),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_7),
                        ssku(TEA_7),
                        warehouse(WAREHOUSE_ID),
                        price(67),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(78),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(94)));
        assertThat(calculation.getItemDiscount().getDiscounts(), allOf(
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_4, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(11)) //29
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_3, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(14)) //36
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_5, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(16)) //38
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_6, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(16)) //38
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_2, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(17)) //39
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_7, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(20)) //48
                )
        ));
    }

    @Test
    public void shouldCalcDiscountForObeOfferWithProportionalQuantity() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(6)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(132)));
        assertThat(calculation.getItemDiscount().getDiscounts(), allOf(
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_1, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(22)) //42
                )
        ));
    }

    @Test
    public void shouldCalcDiscountForTwoOffersWithOneVariant() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(60)));
        assertThat(calculation.getItemDiscount().getDiscounts(), allOf(
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_1, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(16)) //48
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_2, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(14)) //45
                )
        ));
    }

    @Test
    public void shouldCalcDiscountForTwoOffersWithTwoVariants() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(6)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(120)));
        assertThat(calculation.getItemDiscount().getDiscounts(), allOf(
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_2, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(20)) //39
                )
        ));
    }

    @Test
    public void shouldCalcDiscountForTwoOffersWithThreeVariants() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(7)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(119)));
        assertThat(calculation.getItemDiscount().getDiscounts(), allOf(
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_2, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(17)) // 42
                )
        ));
    }

    @Test
    public void shouldCalcDiscountForTwoOffersWithThreeVariantsNoProportion() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(7)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(177)));
        assertThat(calculation.getItemDiscount().getDiscounts(), allOf(
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_1, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(22)) // 42
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_2, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(19)) // 40
                )
        ));
    }

    @Test
    public void shouldCalcDiscountForTwoOffersWithThreeVariantsNoProportion2() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(7)
                ).build(), expected);

        Cart cart = firstCart(combineResult);
        SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(179)));
        assertThat(calculation.getItemDiscount().getDiscounts(), allOf(
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_1, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(20)) //44
                ),
                hasEntry(
                        is(ItemKey.withBundle(FEED_ID, TEA_2, cart.getId(), cart.getOrderId(), null)),
                        comparesEqualTo(BigDecimal.valueOf(17)) //42
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
                        condition(
                                cheapestAsGift(FeedSskuSet.of(FEED_ID, List.of(TEA_1, TEA_2, TEA_3, TEA_4, TEA_5, TEA_6, TEA_7)))),
                        quantityInBundle(3),
                        primary()
                )
        ));
    }
}
