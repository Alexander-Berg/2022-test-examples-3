package ru.yandex.market.loyalty.back.controller.promo.spread;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.spread.SpreadDiscountPromoDescription;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.spread.SpreadPromoService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.back.controller.discount.DiscountControllerPromocodeWithFiltrationTest.FIRST_SSKU;
import static ru.yandex.market.loyalty.back.controller.discount.DiscountControllerPromocodeWithFiltrationTest.FOURTH_SSKU;
import static ru.yandex.market.loyalty.back.controller.discount.DiscountControllerPromocodeWithFiltrationTest.SECOND_SSKU;
import static ru.yandex.market.loyalty.back.controller.discount.DiscountControllerPromocodeWithFiltrationTest.THIRD_SSKU;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.spreadDiscountReceiptBound;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.spreadDiscountReceiptPromoKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_BUDGET;

@TestFor(DiscountController.class)

public class DiscountControllerSpreadReceiptTest extends MarketLoyaltyBackMockedDbTestBase {
    public static final String PROMO_KEY = "promoKey";
    public static final long FEED_ID = 1241L;

    @Autowired
    private SpreadPromoService spreadPromoService;

    @Autowired
    private PromoService promoService;

    @Before
    public void setUp() {
        spreadPromoService
                .createOrUpdateSpreadPromo(SpreadDiscountPromoDescription.builder()
                        .promoSource(LOYALTY_VALUE)
                        .feedId(FEED_ID)
                        .promoKey(PROMO_KEY)
                        .source("SOURCE")
                        .shopPromoId("SHOP_PROMO_ID")
                        .startTime(clock.dateTime())
                        .endTime(clock.dateTime().plusYears(10))
                        .name(PROMO_KEY)
                        .promoType(ReportPromoType.SPREAD_RECEIPT)
                        .budgetLimit(DEFAULT_BUDGET)
                        .build());
    }

    @Test
    public void shouldNotCalcSpreadReceiptDiscountWithoutSpecification() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        spreadDiscountReceiptPromoKey(PROMO_KEY)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", empty()))
        ));
    }


    @Test
    public void shouldNotCalcSpreadReceiptDiscountForOldPromo() {
        spreadPromoService
                .createOrUpdateSpreadPromo(SpreadDiscountPromoDescription.builder()
                        .promoSource(LOYALTY_VALUE)
                        .feedId(FEED_ID)
                        .promoKey(PROMO_KEY)
                        .source("SOURCE")
                        .shopPromoId(PROMO_KEY)
                        .startTime(clock.dateTime().minus(3, ChronoUnit.DAYS))
                        .endTime(clock.dateTime().minus(1, ChronoUnit.DAYS))
                        .name(PROMO_KEY)
                        .promoType(ReportPromoType.SPREAD_RECEIPT)
                        .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(3),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(100), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(200), BigDecimal.valueOf(15), true)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", empty()))
        ));
    }

    @Test
    public void shouldCalcPercentSpreadReceiptDiscountForFirstTier() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(1),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(300),
                        quantity(2),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(20)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(30)))
                        )))
                )
        ));
    }


    @Test
    public void shouldCalcPercentSpreadReceiptDiscountForSecondTier() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(1000),
                        quantity(1),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(500),
                        quantity(2),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(150)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(75)))
                        )))
                )
        ));
    }


    @Test
    public void shouldCalcAbsoluteSpreadReceiptDiscountForFirstTier() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(1),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.valueOf(100), false),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(200), false)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(300),
                        quantity(2),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.valueOf(100), false),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(200), false)
                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(24)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(38)))
                        )))
                )
        ));
    }

    @Test
    public void shouldCalcAbsoluteSpreadReceiptDiscountForSecondTier() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(500),
                        quantity(1),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.valueOf(100), false),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(200), false)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(1000),
                        quantity(2),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.valueOf(100), false),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(200), false)
                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(40)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(80)))
                        )))
                )
        ));
    }


    @Test
    public void shouldSpendSpreadDiscount() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(1000),
                        quantity(1),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(500),
                        quantity(2),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .build();

        assertThat(promoService.getPromoByPromoKey(PROMO_KEY).getCurrentBudget(), comparesEqualTo(DEFAULT_BUDGET));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(150)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_RECEIPT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(75)))
                        )))
                )
        ));

        assertThat(
                promoService.getPromoByPromoKey(PROMO_KEY).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(BigDecimal.valueOf(225)))
        );
    }

    @Test
    public void shouldSpendSpreadDiscountWithMultiOrder() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(1000),
                        quantity(1),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(500),
                        quantity(2),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .build();

        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, THIRD_SSKU),
                        ssku(THIRD_SSKU),
                        price(2000),
                        quantity(1),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, FOURTH_SSKU),
                        ssku(FOURTH_SSKU),
                        price(100),
                        quantity(2),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .build();

        assertThat(promoService.getPromoByPromoKey(PROMO_KEY).getCurrentBudget(), comparesEqualTo(DEFAULT_BUDGET));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(List.of(order, order1))
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        assertThat(discountResponse.getOrders(), hasSize(2));

        assertThat(
                promoService.getPromoByPromoKey(PROMO_KEY).getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(BigDecimal.valueOf(540)))
        );
    }

    @Test
    public void shouldNotSpendSpreadDiscountWithoutEnoughBudget() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(100000),
                        quantity(1),
                        spreadDiscountReceiptPromoKey(PROMO_KEY),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(500), BigDecimal.TEN, true),
                        spreadDiscountReceiptBound(BigDecimal.valueOf(1000), BigDecimal.valueOf(15), true)
                )
                .build();

        assertThat(promoService.getPromoByPromoKey(PROMO_KEY).getCurrentBudget(), comparesEqualTo(DEFAULT_BUDGET));

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(FIRST_SSKU)),
                        hasProperty("promos", empty())
                )
        ));

        assertThat(promoService.getPromoByPromoKey(PROMO_KEY).getCurrentBudget(), comparesEqualTo(DEFAULT_BUDGET));
    }
}
