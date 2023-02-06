package ru.yandex.market.loyalty.core.service.bundle.strategy.concrete;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.service.bundle.AbstractBundleStrategyTest;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest;

import java.math.BigDecimal;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy.BLUE_SET;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
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

public class BlueSetBundleCalculationStrategyCalibrationTest extends AbstractBundleStrategyTest {

    @Test
    public void shouldKeepPredefinedProportions1() {
        PromoBundleDescription expected = bundleService.createPromoBundle(bundleDescription(
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
                                proportion(FIRST_SSKU, 20),
                                proportion(SECOND_SSKU, 30)
                        )),
                        primary()
                )
        ));

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1572),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(563),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);

        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(484)));
        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(315)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(169)))
        )));
    }

    @Test
    public void shouldKeepPredefinedProportions2() {
        PromoBundleDescription expected = bundleService.createPromoBundle(bundleDescription(
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
                                proportion(SECOND_SSKU, 15)
                        )),
                        primary()
                )
        ));

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1300),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(6290),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);

        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(1074)));
        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(130)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(944)))
        )));
    }

    @Test
    public void shouldKeepPredefinedProportions3() {
        PromoBundleDescription expected = bundleService.createPromoBundle(bundleDescription(
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
                                proportion(FIRST_SSKU, 30),
                                proportion(SECOND_SSKU, 25),
                                proportion(THIRD_SSKU, 3)
                        )),
                        primary()
                )
        ));

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(4990),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(4491),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(59990),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);

        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(4420)));
        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1497)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1123)))
        ), allOf(
                hasProperty("offerId", is(THIRD_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(1800)))
        )));
    }

}
