package ru.yandex.market.loyalty.back.controller.promo.spread;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

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
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.spread.SpreadDiscountPromoDescription;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.spread.SpreadPromoService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.back.controller.discount.DiscountControllerPromocodeWithFiltrationTest.FIRST_SSKU;
import static ru.yandex.market.loyalty.back.controller.discount.DiscountControllerPromocodeWithFiltrationTest.SECOND_SSKU;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.PROMO_APPLICABILITY_POLICY;
import static ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy.BOTH;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.spreadDiscountCountBound;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.spreadDiscountCountPromoKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ssku;
import static ru.yandex.market.loyalty.core.utils.OrderResponseUtils.firstOrderOf;

@TestFor(DiscountController.class)
public class DiscountControllerSpreadCountTest extends MarketLoyaltyBackMockedDbTestBase {
    public static final String PROMO_KEY = "promoKey";
    public static final String PROMO_KEY_WITHOUT_BERUBONUS = "promoKeyWithoutBerubonus";
    public static final long FEED_ID = 1241L;
    public static final String THIRD_SSKU = "THIRD_SSKU";

    @Autowired
    private SpreadPromoService spreadPromoService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoManager promoManager;

    @Before
    public void setUp() throws Exception {
        configurationService.set(
                PROMO_APPLICABILITY_POLICY,
                BOTH
        );
        spreadPromoService
                .createOrUpdateSpreadPromo(SpreadDiscountPromoDescription.builder()
                        .promoSource(LOYALTY_VALUE)
                        .feedId(FEED_ID)
                        .promoKey(PROMO_KEY)
                        .source("SOURCE")
                        .shopPromoId(randomString())
                        .startTime(clock.dateTime())
                        .endTime(clock.dateTime().plusYears(10))
                        .name(PROMO_KEY)
                        .promoType(ReportPromoType.SPREAD_COUNT)
                        .allowPromocode(true)
                        .allowBerubonus(true)
                        .build());

        spreadPromoService
                .createOrUpdateSpreadPromo(SpreadDiscountPromoDescription.builder()
                        .promoSource(LOYALTY_VALUE)
                        .feedId(FEED_ID)
                        .promoKey(PROMO_KEY_WITHOUT_BERUBONUS)
                        .source("SOURCE")
                        .shopPromoId(randomString())
                        .startTime(clock.dateTime())
                        .endTime(clock.dateTime().plusYears(10))
                        .name(PROMO_KEY_WITHOUT_BERUBONUS)
                        .promoType(ReportPromoType.SPREAD_COUNT)
                        .allowPromocode(false)
                        .allowBerubonus(false)
                        .build());
    }

    @Test
    public void shouldNotCalcSpreadDiscountWithoutSpecification() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(3),
                        spreadDiscountCountPromoKey(PROMO_KEY)
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
    public void shouldNotCalcSpreadDiscountForOldPromo() {
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
                        .promoType(ReportPromoType.SPREAD_COUNT)
                        .build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(3),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(15), true)
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
    public void shouldCalcPercentSpreadDiscountForFirstTier() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(199),
                        quantity(3),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(20), true)
                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(20)))
                )))
        )));
    }

    @Test
    public void shouldCalcPercentSpreadDiscountForSecondTier() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(15), true)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(30)))
                )))
        )));
    }

    @Test
    public void shouldCalcAbsoluteSpreadDiscountForFirstTier() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(3),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, false),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(20), false)
                )
                .build();


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.TEN))
                )))
        )));
    }

    @Test
    public void shouldCalcAbsoluteSpreadDiscountForSecondTier() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, false),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(100), false)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
                )))
        )));
    }

    @Test
    public void shouldCalcSpreadAbsoluteDiscountForTwoItems() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, false),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(100), false)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(300),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, false),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(100), false)
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
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(100)))
                        )))
                )
        ));
    }

    @Test
    public void shouldCalcSpreadPercentDiscountForTwoItems() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(20), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(300),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(20), true)
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
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(40)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(60)))
                        )))
                )
        ));
    }

    @Test
    public void shouldCalcSpreadPercentDiscountForTwoItemsWithDifferentTiers() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(3),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(20), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(300),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(20), true)
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
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(20)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(60)))
                        )))
                )
        ));
    }

    @Test
    public void shouldNotCalcSpreadPercentDiscountForSecondItemsWithDifferentTiers() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(9999),
                        quantity(4),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(3, BigDecimal.valueOf(7), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(9999),
                        quantity(3),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(4, BigDecimal.TEN, true),
                        spreadDiscountCountBound(6, BigDecimal.valueOf(12), true)
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
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(700)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", empty())

                )
        ));
    }

    @Test
    public void shouldCalcSpreadPercentDiscountForTwoItemsWithDifferentPromos() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(20), true)
                )
                .withOrderItem(
                        itemKey(FEED_ID, SECOND_SSKU),
                        ssku(SECOND_SSKU),
                        price(300),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY_WITHOUT_BERUBONUS),
                        spreadDiscountCountBound(2, BigDecimal.valueOf(30), true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(40), true)
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
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(40)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(SECOND_SSKU)),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(120)))
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
                        price(200),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(15), true)
                )
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(123))
                        .build());

        OrderWithBundlesResponse firstOrder = firstOrderOf(discountResponse);

        assertThat(firstOrder.getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_SSKU)),
                hasProperty("promos", hasItem(allOf(
                        hasProperty("promoType", is(PromoType.SPREAD_COUNT)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(30)))
                )))
        )));
    }

    @Test
    public void shouldApplySpreadDiscountAndCoinsWithAllowBeruBonus() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setCanBeRestoredFromReserveBudget(true)
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(15), true)
                )
                .build();
        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order).withCoins(coinKey).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);
        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.USED));
    }

    @Test
    public void shouldNotApplySpreadDiscountAndCoinsWithoutAllowBeruBonus() {
        spreadPromoService
                .createOrUpdateSpreadPromo(SpreadDiscountPromoDescription.builder()
                        .promoSource(LOYALTY_VALUE)
                        .feedId(FEED_ID)
                        .promoKey(PROMO_KEY_WITHOUT_BERUBONUS)
                        .source("SOURCE")
                        .shopPromoId(randomString())
                        .startTime(clock.dateTime())
                        .endTime(clock.dateTime().plusYears(10))
                        .name(PROMO_KEY_WITHOUT_BERUBONUS)
                        .promoType(ReportPromoType.SPREAD_COUNT)
                        .allowPromocode(false)
                        .allowBerubonus(false)
                        .build());
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setCanBeRestoredFromReserveBudget(true)
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(FEED_ID, FIRST_SSKU),
                        ssku(FIRST_SSKU),
                        price(200),
                        quantity(5),
                        spreadDiscountCountPromoKey(PROMO_KEY_WITHOUT_BERUBONUS),
                        spreadDiscountCountBound(2, BigDecimal.TEN, true),
                        spreadDiscountCountBound(4, BigDecimal.valueOf(15), true)
                )
                .build();
        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order).withCoins(coinKey).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);
        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.ACTIVE));
    }

}
