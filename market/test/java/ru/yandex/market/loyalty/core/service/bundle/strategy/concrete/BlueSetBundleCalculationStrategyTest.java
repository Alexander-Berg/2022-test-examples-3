package ru.yandex.market.loyalty.core.service.bundle.strategy.concrete;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.bundle.AbstractBundleStrategyTest;
import ru.yandex.market.loyalty.core.service.bundle.calculation.SuccessPromoBundle;
import ru.yandex.market.loyalty.core.service.bundle.construction.PromoBundleCombineResult;
import ru.yandex.market.loyalty.core.service.discount.DiscountCalculationRequest;

import java.math.BigDecimal;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

public class BlueSetBundleCalculationStrategyTest extends AbstractBundleStrategyTest {

    @Test
    public void shouldCalcDiscountForCartWithOneProportionalBundle() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(FOURTH_SSKU),
                        ssku(FOURTH_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(1));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(700)));
        assertThat(calculation.getItems(), hasSize(4));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, THIRD_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FOURTH_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
        )));

        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(150)))
        ), allOf(
                hasProperty("offerId", is(THIRD_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
        ), allOf(
                hasProperty("offerId", is(FOURTH_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(250)))
        )));
    }

    @Test
    public void shouldCalcDiscountForCartWithOneProportionalBundleWithSomeQuantity() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(1));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(1350)));
        assertThat(calculation.getItems(), hasSize(3));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, THIRD_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        )));

        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.valueOf(3)));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(150)))
        ), allOf(
                hasProperty("offerId", is(THIRD_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(200)))
        )));
    }

    @Test
    public void shouldCalcDiscountForCartWithNotProportionalBundleWithSomeQuantity() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(1));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(500)));
        assertThat(calculation.getItems(), hasSize(2));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
        )));

        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.valueOf(2)));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(66)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(151)))
        )));
    }

    @Test
    public void shouldCalcDiscountForCartWithNotProportionalBundleWithSomeQuantity2() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(1));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(500)));
        assertThat(calculation.getItems(), hasSize(2));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        )));

        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.valueOf(2)));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
        )));
    }

    @Test
    public void shouldCalcDiscountForCartWithItemsWithNotProportionalPrices() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2222),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(3333),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(2555),
                        promoKeys(PROMO_KEY),
                        quantity(2)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(1));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(2468)));
        assertThat(calculation.getItems(), hasSize(3));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, THIRD_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2)))
        )));

        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.valueOf(2)));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(148)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(501)))
        ), allOf(
                hasProperty("offerId", is(THIRD_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(511)))
        )));
    }

    @Test
    public void shouldCalcDiscountForCartWithTwoDifferentBundles() {
        PromoBundleDescription expected = createBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(133),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(234),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(435),
                        promoKeys(PROMO_KEY),
                        quantity(3)
                ).withOrderItem(
                        itemKey(FOURTH_SSKU),
                        ssku(FOURTH_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(154),
                        promoKeys(PROMO_KEY),
                        quantity(1)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(2));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(448)));
        assertThat(calculation.getItems(), hasSize(4));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, SECOND_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, THIRD_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(3)))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FOURTH_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
        )));

        assertThat(calculation.getPromoBundleDiscounts(), hasItems(
                allOf(
                        hasProperty("quantity", comparesEqualTo(BigDecimal.ONE)),
                        hasProperty("items", hasItems(
                                allOf(
                                        hasProperty("offerId", is(FIRST_SSKU)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(13)))
                                ), allOf(
                                        hasProperty("offerId", is(SECOND_SSKU)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(35)))
                                ), allOf(
                                        hasProperty("offerId", is(THIRD_SSKU)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(88)))
                                ), allOf(
                                        hasProperty("offerId", is(FOURTH_SSKU)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(40)))
                                )
                        ))
                ),
                allOf(
                        hasProperty("quantity", comparesEqualTo(BigDecimal.valueOf(2))),
                        hasProperty("items", hasItems(
                                allOf(
                                        hasProperty("offerId", is(FIRST_SSKU)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(13)))
                                ), allOf(
                                        hasProperty("offerId", is(SECOND_SSKU)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(35)))
                                ), allOf(
                                        hasProperty("offerId", is(THIRD_SSKU)),
                                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(88)))
                                )
                        ))
                )
        ));
    }

    @Test
    public void shouldSkipItemsWithZeroProportion() {
        PromoBundleDescription expected = createZeroBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(133),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(234),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(435),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(1));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(101)));
        assertThat(calculation.getItems(), hasSize(2));
        assertThat(calculation.getItems(), hasItems(allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, FIRST_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
        ), allOf(
                hasProperty("itemKey", is(ItemKey.withBundle(FEED_ID, THIRD_SSKU, cart.getId(), cart.getOrderId(),
                        null))),
                hasProperty("quantity", comparesEqualTo(BigDecimal.ONE))
        )));

        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(14)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.ZERO))
        ), allOf(
                hasProperty("offerId", is(THIRD_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(87)))
        )));
    }

    @Test
    public void shouldCalcZeroDiscountsForEmptyBundle() {
        PromoBundleDescription expected = createZeroBundle();

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(133),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(234),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(1));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.ZERO));
        assertThat(calculation.getItems(), empty());

        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.ZERO))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.ZERO))
        )));
    }

    @Test
    public void shouldCalcWithUpRoundingDiscount() {
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
                                proportion(FIRST_SSKU, 5),
                                proportion(SECOND_SSKU, 10),
                                proportion(THIRD_SSKU, 15)
                        )),
                        primary()
                )
        ));

        PromoBundleCombineResult combineResult = recombineCart(orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(4786),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(3926),
                        promoKeys(PROMO_KEY)
                ).withOrderItem(
                        itemKey(THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        warehouse(WAREHOUSE_ID),
                        price(4014),
                        promoKeys(PROMO_KEY)
                ).build(), expected);

        final DiscountCalculationRequest.Cart cart = firstCart(combineResult);
        final SuccessPromoBundle calculation = calculateDiscount(combineResult, cart);
        assertThat(calculation.getPromoBundleDiscounts(), hasSize(1));
        assertThat(calculation.getTotalDiscount(), comparesEqualTo(BigDecimal.valueOf(1236)));

        assertThat(firstBundle(calculation).getQuantity(), comparesEqualTo(BigDecimal.ONE));
        assertThat(firstBundle(calculation).getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(240)))
        ), allOf(
                hasProperty("offerId", is(SECOND_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(393)))
        ), allOf(
                hasProperty("offerId", is(THIRD_SSKU)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(603)))
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
                                proportion(SECOND_SSKU, 15),
                                proportion(THIRD_SSKU, 20),
                                proportion(FOURTH_SSKU, 25)
                        )),
                        primary()
                ),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_SSKU, 10),
                                proportion(SECOND_SSKU, 15),
                                proportion(THIRD_SSKU, 20)
                        )),
                        primary()
                ),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_SSKU, 10),
                                proportion(SECOND_SSKU, 15)
                        )),
                        primary()
                )
        ));
    }

    private PromoBundleDescription createZeroBundle() {
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
                                proportion(SECOND_SSKU, 0),
                                proportion(THIRD_SSKU, 20)
                        )),
                        primary()
                ),
                item(
                        condition(blueSet(
                                FEED_ID,
                                proportion(FIRST_SSKU, 0),
                                proportion(SECOND_SSKU, 0)
                        )),
                        primary()
                )
        ));
    }
}
