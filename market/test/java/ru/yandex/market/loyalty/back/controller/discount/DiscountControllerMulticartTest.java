package ru.yandex.market.loyalty.back.controller.discount;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.MultiorderDao;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.order.entity.MultiorderEntry;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.budgeting.BudgetModeService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.ItemPromoCalculation;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.ANOTHER_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_MULTI_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.SUPPLIER_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.loyaltyProgramPartner;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_VALUE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(DiscountController.class)
public class DiscountControllerMulticartTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    private BudgetModeService budgetModeService;
    @Autowired
    private MultiorderDao multiorderDao;

    @Test
    public void shouldSaveMultiorderEntries() {
        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(DEFAULT_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(2800),
                                                loyaltyProgramPartner(false)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(ANOTHER_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(3000),
                                                loyaltyProgramPartner(false)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                        .build())
                        .withMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build()
        );

        List<MultiorderEntry> entries = multiorderDao.findAll();

        assertThat(entries, containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID))
                ),
                allOf(
                        hasProperty("orderId", equalTo(ANOTHER_ORDER_ID)),
                        hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID))
                )
        ));
    }

    @Test
    public void shouldSpendCoinForMultiCart() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoins(coinKey).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(discountResponse.getOrders().stream().map(ItemPromoCalculation::calculateTotalDiscount).reduce(BigDecimal.ZERO, BigDecimal::add), comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)
        );

        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.USED));
    }

    @Test
    public void shouldSpendCouponForMultiCart() {
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );
        Coupon coupon =
                couponService.createOrGetCoupon(CouponCreationRequest.builder("test", promo.getId()).forceActivation(true).build(),
                discountUtils.getRulesPayload());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoupon(coupon.getCode()).build()
        );

        coupon = couponService.getCouponById(coupon.getId());

        assertThat(discountResponse.getOrders().stream().map(ItemPromoCalculation::calculateTotalDiscount).reduce(BigDecimal.ZERO, BigDecimal::add), comparesEqualTo(DEFAULT_COUPON_VALUE));

        assertThat(
                promoService.getPromo(promo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COUPON_VALUE)
        );

        assertThat(coupon.getStatus(), equalTo(CouponStatus.USED));
    }

    @Test
    public void shouldCalcCoinForMultiCart() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoins(coinKey).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        BigDecimal halfCoinNominal = DEFAULT_COIN_FIXED_NOMINAL.divide(BigDecimal.valueOf(2), RoundingMode.UNNECESSARY);

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                hasProperty("items", contains(
                        allOf(
                                hasProperty("offerId", equalTo(DEFAULT_ITEM_KEY.getOfferId())),
                                hasProperty("feedId", equalTo(DEFAULT_ITEM_KEY.getFeedId())),
                                hasProperty("promos", contains(
                                        allOf(
                                                hasProperty("usedCoin",
                                                        equalTo(new IdObject(coin.getCoinKey().getId()))),
                                                hasProperty("discount", comparesEqualTo(halfCoinNominal))
                                        )
                                ))
                        )
                )),
                hasProperty("items", contains(
                        allOf(
                                hasProperty("offerId", equalTo(ANOTHER_ITEM_KEY.getOfferId())),
                                hasProperty("feedId", equalTo(ANOTHER_ITEM_KEY.getFeedId())),
                                hasProperty("promos", contains(
                                        allOf(
                                                hasProperty("usedCoin",
                                                        equalTo(new IdObject(coin.getCoinKey().getId()))),
                                                hasProperty("discount", comparesEqualTo(halfCoinNominal))
                                        )
                                ))
                        )
                ))
        ));
    }

    @Test
    public void shouldRevertCoinMulticartCorrectly() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoins(coinKey).build()
        );

        Set<String> tokens = discountResponse.getOrders()
                .stream()
                .flatMap(o -> o.getItems().stream())
                .flatMap(i -> i.getPromos().stream())
                .map(ItemPromoResponse::getDiscountToken).collect(Collectors.toSet());

        assertThat(tokens, hasSize(2));

        marketLoyaltyClient.revertDiscount(tokens);

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(BigDecimal.ZERO)
        );

        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.ACTIVE));
    }

    @Test
    public void shouldAsyncRevertCoinCorrectly() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(10)).setBudgetMode(BudgetMode.ASYNC)
        );
        budgetModeService.reloadCache();
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey).build()
        );
        deferredMetaTransactionService.consumeBatchOfTransactions(1);

        Set<String> tokens = discountResponse.getOrders()
                .stream()
                .flatMap(o -> o.getItems().stream())
                .flatMap(i -> i.getPromos().stream())
                .map(ItemPromoResponse::getDiscountToken).collect(Collectors.toSet());

        assertThat(tokens, hasSize(1));

        marketLoyaltyClient.revertDiscount(tokens);

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);
        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(coin.getNominal())
        );
        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.ACTIVE));

        deferredMetaTransactionService.consumeBatchOfTransactions(1);
        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(BigDecimal.ZERO)
        );
    }

    @Test
    public void shouldRevertCouponMulticartCorrectly() {
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );
        Coupon coupon =
                couponService.createOrGetCoupon(CouponCreationRequest.builder("test", promo.getId()).forceActivation(true).build(),
                discountUtils.getRulesPayload());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoupon(coupon.getCode()).build()
        );

        List<String> tokens = getRevertTokens(discountResponse);

        assertThat(tokens, hasSize(2));

        marketLoyaltyClient.revertDiscount(ImmutableSet.copyOf(tokens));

        coupon = couponService.getCouponById(coupon.getId());

        assertThat(
                promoService.getPromo(promo.getId()).getSpentBudget(),
                comparesEqualTo(BigDecimal.ZERO)
        );

        assertThat(coupon.getStatus(), equalTo(CouponStatus.ACTIVE));
    }

    @Test
    public void shouldRevertSingleCoinMulticartPartially() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoins(coinKey).build()
        );

        List<String> tokens = getRevertTokens(discountResponse);

        assertThat(tokens, hasSize(2));

        Coin coin;

        // revert first order
        marketLoyaltyClient.revertDiscount(ImmutableSet.of(tokens.get(0)));

        coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL.divide(BigDecimal.valueOf(2), RoundingMode.UNNECESSARY))
        );

        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.USED));

        // revert second order
        marketLoyaltyClient.revertDiscount(ImmutableSet.of(tokens.get(1)));

        coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(BigDecimal.ZERO)
        );

        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.ACTIVE));
    }

    @Test
    public void shouldRevertSeveralCoinMulticartPartially() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        CoinKey coinKey1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoins(coinKey1, coinKey2).build()
        );

        List<String> tokens = getRevertTokens(discountResponse);

        assertThat(tokens, hasSize(4));

        Coin coin1, coin2;

        // revert first order
        marketLoyaltyClient.revertDiscount(ImmutableSet.of(tokens.get(0), tokens.get(1)));

        coin1 = coinService.search.getCoin(coinKey1).orElseThrow(AssertionError::new);
        coin2 = coinService.search.getCoin(coinKey1).orElseThrow(AssertionError::new);

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)
        );

        assertThat(coin1.getStatus(), equalTo(CoreCoinStatus.USED));
        assertThat(coin2.getStatus(), equalTo(CoreCoinStatus.USED));

        // revert second order
        marketLoyaltyClient.revertDiscount(ImmutableSet.of(tokens.get(2), tokens.get(3)));

        coin1 = coinService.search.getCoin(coinKey1).orElseThrow(AssertionError::new);
        coin2 = coinService.search.getCoin(coinKey2).orElseThrow(AssertionError::new);

        assertThat(
                promoService.getPromo(smartShoppingPromo.getId()).getSpentBudget(),
                comparesEqualTo(BigDecimal.ZERO)
        );

        assertThat(coin1.getStatus(), equalTo(CoreCoinStatus.ACTIVE));
        assertThat(coin2.getStatus(), equalTo(CoreCoinStatus.ACTIVE));
    }


    @Test
    public void shouldRevertCouponForMultiCart() {
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );
        Coupon coupon =
                couponService.createOrGetCoupon(CouponCreationRequest.builder("test", promo.getId()).forceActivation(true).build(),
                discountUtils.getRulesPayload());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoupon(coupon.getCode()).build()
        );

        coupon = couponService.getCouponById(coupon.getId());

        assertThat(discountResponse.getOrders().stream().map(ItemPromoCalculation::calculateTotalDiscount).reduce(BigDecimal.ZERO, BigDecimal::add), comparesEqualTo(DEFAULT_COUPON_VALUE));

        assertThat(
                promoService.getPromo(promo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COUPON_VALUE)
        );

        assertThat(coupon.getStatus(), equalTo(CouponStatus.USED));

        List<String> tokens = getRevertTokens(discountResponse);

        // revert first order
        marketLoyaltyClient.revertDiscount(ImmutableSet.of(tokens.get(0)));

        coupon = couponService.getCouponById(coupon.getId());

        assertThat(
                promoService.getPromo(promo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COUPON_VALUE.divide(BigDecimal.valueOf(2), RoundingMode.UNNECESSARY))
        );

        assertThat(coupon.getStatus(), equalTo(CouponStatus.USED));

        // revert second order
        marketLoyaltyClient.revertDiscount(ImmutableSet.of(tokens.get(1)));

        coupon = couponService.getCouponById(coupon.getId());

        assertThat(
                promoService.getPromo(promo.getId()).getSpentBudget(),
                comparesEqualTo(BigDecimal.ZERO)
        );

        assertThat(coupon.getStatus(), equalTo(CouponStatus.ACTIVE));
    }


    @Test
    public void should422IfGreenPlatformUsed() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithDeliveriesRequest order1 = orderRequestBuilder().withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithDeliveriesRequest order2 = orderRequestBuilder().withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        MarketLoyaltyException ex = assertThrows(MarketLoyaltyException.class, () -> marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder.builder(order1, order2).withCoins(coinKey).withPlatform(MarketPlatform.GREEN).build()
        ));

        assertEquals(HttpClientErrorException.UnprocessableEntity.class, ex.getCause().getClass());
    }

    private static List<String> getRevertTokens(MultiCartDiscountResponse discountResponse) {
        return discountResponse.getOrders()
                .stream()
                .flatMap(o -> o.getItems().stream())
                .flatMap(i -> i.getPromos().stream())
                .map(ItemPromoResponse::getDiscountToken)
                .collect(Collectors.toList());
    }
}
