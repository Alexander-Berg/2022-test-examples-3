package ru.yandex.market.loyalty.core.service.bundle;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest;

import java.util.List;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.junit.Assert.assertTrue;
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
 * https://st.yandex-team.ru/MARKETCHECKOUT-20833
 */
public class CheapestAsGiftCalculationStrategyWithQuantityGiftTest extends AbstractBundleStrategyTest {

    private static final String TEA_1 = "tea 1";
    private static final String TEA_2 = "tea 2";
    private static final String TEA_3 = "tea 3";
    private static final String TEA_4 = "tea 4";
    private static final String TEA_5 = "tea 5";
    private static final String TEA_6 = "tea 6";
    private static final String TEA_7 = "tea 7";

    private PromoBundleDescription expected;

    @Before
    public void prepare() {
        expected = createBundle();
    }

    @Test
    public void shouldSearchGiftInEachBundleForThreeOffersInCartV1() {
        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(7)
                ).withOrderItem(
                        itemKey(TEA_3),
                        ssku(TEA_3),
                        warehouse(WAREHOUSE_ID),
                        price(58),
                        promoKeys(PROMO_KEY),
                        quantity(4)
                ).build(), expected);

        DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        theGiftExistsAndOneGiftInBundle(calculateDiscount(combineResult, cart));
    }

    @Test
    public void shouldSearchGiftInEachBundleForThreeOffersInCartV2() {
        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(7)
                ).withOrderItem(
                        itemKey(TEA_3),
                        ssku(TEA_3),
                        warehouse(WAREHOUSE_ID),
                        price(58),
                        promoKeys(PROMO_KEY),
                        quantity(1)
                ).build(), expected);

        DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        theGiftExistsAndOneGiftInBundle(calculateDiscount(combineResult, cart));
    }

    @Test
    public void shouldSearchGiftInEachBundleForThreeOffersInCartV3() {
        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(TEA_1),
                        ssku(TEA_1),
                        warehouse(WAREHOUSE_ID),
                        price(64),
                        promoKeys(PROMO_KEY),
                        quantity(10)
                ).withOrderItem(
                        itemKey(TEA_2),
                        ssku(TEA_2),
                        warehouse(WAREHOUSE_ID),
                        price(59),
                        promoKeys(PROMO_KEY),
                        quantity(1)
                ).withOrderItem(
                        itemKey(TEA_3),
                        ssku(TEA_3),
                        warehouse(WAREHOUSE_ID),
                        price(58),
                        promoKeys(PROMO_KEY),
                        quantity(1)
                ).build(), expected);

        DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        theGiftExistsAndOneGiftInBundle(calculateDiscount(combineResult, cart));
    }

    @Test
    public void shouldSearchGiftInEachBundleForThreeOffersWithPeerQuantityEqualQuantityInBundleInCart() {
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
                        quantity(3)
                ).withOrderItem(
                        itemKey(TEA_3),
                        ssku(TEA_3),
                        warehouse(WAREHOUSE_ID),
                        price(58),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).build(), expected);

        DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        theGiftExistsAndOneGiftInBundle(calculateDiscount(combineResult, cart));
    }

    @Test
    public void shouldSearchGiftInEachBundleForThreeOffersWithPeerQuantityEqualOneInCart() {
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
                        price(58),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        theGiftExistsAndOneGiftInBundle(calculateDiscount(combineResult, cart));
    }

    @Test
    public void shouldSearchGiftInEachBundleForMixedOffersInCart() {
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

        DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        theGiftExistsAndOneGiftInBundle(calculateDiscount(combineResult, cart));
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
                                cheapestAsGift(
                                        FeedSskuSet.of(FEED_ID, List.of(TEA_1, TEA_2, TEA_3, TEA_4, TEA_5, TEA_6,
                                                TEA_7)))),
                        quantityInBundle(3),
                        primary()
                )
        ));
    }
}
