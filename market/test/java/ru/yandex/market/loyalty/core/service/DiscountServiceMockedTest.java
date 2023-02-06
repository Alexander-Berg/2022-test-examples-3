package ru.yandex.market.loyalty.core.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryPromoResponse;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.dao.coupon.CouponHistoryDao;
import ru.yandex.market.loyalty.core.model.OperationContext;
import ru.yandex.market.loyalty.core.model.RevertSource;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.coupon.CouponHistoryRecord;
import ru.yandex.market.loyalty.core.model.promo.CoreCouponValueType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.RuleCategory;
import ru.yandex.market.loyalty.core.rule.RuleFactory;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyRegionSettingsCoreMockedDbTest;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.RulePayloads;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.market.loyalty.api.model.PromoType.FREE_DELIVERY_ADDRESS;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.REGION_WITHOUT_THRESHOLD;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.ALREADY_FREE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.NO_FREE_DELIVERY;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.COURIER_DELIVERY_ID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.COURIER_SPECIAL_ADDRESS_DELIVERY_ID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.PICKUP_DELIVERY_ID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.POST_DELIVERY_ID;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * @author dinyat
 * 07/06/2017
 */
public class DiscountServiceMockedTest extends MarketLoyaltyRegionSettingsCoreMockedDbTest {

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponHistoryDao couponHistoryDao;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private RuleFactory ruleFactory;
    @Autowired
    private RegionSettingsService regionSettingsService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private DiscountUtils discountUtils;


    private static Matcher<Iterable<? extends DeliveryPromoResponse>> containsFreeDeliveryAddress() {
        return contains(
                allOf(
                        hasProperty("promoType", equalTo(FREE_DELIVERY_ADDRESS)),
                        hasProperty("discount", comparesEqualTo(DeliveryRequestUtils.DEFAULT_DELIVERY_PRICE))
                )
        );
    }

    @Test
    public void testPromoNotCreatedForAlreadyFreeDelivery() {
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_000)),
                        price(BigDecimal.valueOf(3))
                )
                        .withDeliveries(DeliveryRequestUtils.courierDeliveryToSpecialAddressAlreadyFree()).build()
        ).build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountService.calculateDiscounts(request, applicabilityPolicy, null);

        assertNotNull(discountResponse);
        assertEquals(ALREADY_FREE, discountResponse.getFreeDeliveryStatus());
        assertEquals(FreeDeliveryReason.ALREADY_ZERO_PRICE, discountResponse.getFreeDeliveryReason());
        assertTrue(discountResponse.getOrders().get(0).getDeliveries().get(0).getPromos().isEmpty());
    }

    @Test
    public void testOneFreeDeliveryAddressMaxThresholdSatisfied() {
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_000)),
                        price(BigDecimal.valueOf(3))
                )
                        .withDeliveries(DeliveryRequestUtils.courierDeliveryToSpecialAddress()).build()
        ).build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountService.calculateDiscounts(request, applicabilityPolicy, null);

        assertEquals(NO_FREE_DELIVERY, discountResponse.getFreeDeliveryStatus());
        assertEquals(REGION_WITHOUT_THRESHOLD, discountResponse.getFreeDeliveryReason());
        assertThat(
                discountResponse.getOrders().get(0).getDeliveries().get(0).getPromos(),
                containsFreeDeliveryAddress()
        );
    }

    @Ignore("MARKETDISCOUNT-2782")
    @Test
    public void testOneFreeDeliveryAddressMaxThresholdUnsatisfied() {
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_000)),
                        price(BigDecimal.valueOf(2))
                )
                        .withDeliveries(DeliveryRequestUtils.courierDeliveryToSpecialAddress()).build()
        ).build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountService.calculateDiscounts(request, applicabilityPolicy, null);

        assertOnlyFreeDeliveryAddressPromo(discountResponse);
    }

    @Test
    public void testFreeDeliveryAddressButNotCourier() {
        MultiCartWithBundlesDiscountRequest request = builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.valueOf(1_000)),
                                price(BigDecimal.valueOf(2)))
                        .withDeliveries(DeliveryRequestUtils.postDeliveryToSpecialAddress())
                        .build()
        )
                .build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountService.calculateDiscounts(request, applicabilityPolicy, null);

        assertNotNull(discountResponse);
        assertEquals(NO_FREE_DELIVERY, discountResponse.getFreeDeliveryStatus());
        assertTrue(discountResponse.getOrders().get(0).getDeliveries().get(0).getPromos().isEmpty());
    }

    @Test
    public void testMultipleDeliveryOptionsWithOneFreeDeliveryAddress() {
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem()
                        .withDeliveries(
                                DeliveryRequestUtils.courierDelivery(),
                                DeliveryRequestUtils.courierDeliveryToSpecialAddress(),
                                DeliveryRequestUtils.postDelivery(),
                                DeliveryRequestUtils.pickupDelivery()
                        ).build()
        ).build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountService.calculateDiscounts(request, applicabilityPolicy, null);

        assertEquals(NO_FREE_DELIVERY, discountResponse.getFreeDeliveryStatus());

        assertThat(
                discountResponse.getOrders().get(0).getDeliveries(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("id", equalTo(COURIER_DELIVERY_ID)),
                                hasProperty("promos", is(empty()))
                        ),
                        allOf(
                                hasProperty("id", equalTo(COURIER_SPECIAL_ADDRESS_DELIVERY_ID)),
                                hasProperty("promos", containsFreeDeliveryAddress())
                        ),
                        allOf(
                                hasProperty("id", equalTo(POST_DELIVERY_ID)),
                                hasProperty("promos", is(empty()))
                        ),
                        allOf(
                                hasProperty("id", equalTo(PICKUP_DELIVERY_ID)),
                                hasProperty("promos", is(empty()))
                        )
                )
        );
    }

    @Test
    public void testMulticartMaxThresholdSatisfiedWithFreeDeliveryAddress() {
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_000)),
                        price(BigDecimal.ONE)
                )
                        .withDeliveries(DeliveryRequestUtils.courierDeliveryToSpecialAddress()).build(),
                orderRequestWithBundlesBuilder().withOrderItem(
                        itemKey(ANOTHER_ITEM_KEY),
                        quantity(BigDecimal.valueOf(1_500)),
                        price(BigDecimal.ONE)
                )
                        .withDeliveries(DeliveryRequestUtils.postDelivery()).build()
        ).build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountService.calculateDiscounts(request, applicabilityPolicy, null);

        assertNotNull(discountResponse);
        assertEquals(NO_FREE_DELIVERY, discountResponse.getFreeDeliveryStatus());
        assertEquals(REGION_WITHOUT_THRESHOLD, discountResponse.getFreeDeliveryReason());

        assertThat(
                discountResponse.getOrders().stream()
                        .map(OrderWithBundlesResponse::getDeliveries)
                        .filter(deliveries -> deliveries.stream().anyMatch(d -> !d.getPromos().isEmpty()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()),
                everyItem(hasProperty("promos", containsFreeDeliveryAddress()))
        );
    }

    @Test
    public void testMulticartMaxThresholdUnsatisfied() {
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder()
                        .withCartId("first")
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.valueOf(1_000)),
                                price(BigDecimal.ONE)
                        )
                        .withDeliveries(DeliveryRequestUtils.courierDeliveryToSpecialAddress()).build(),
                orderRequestWithBundlesBuilder()
                        .withCartId("second")
                        .withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                quantity(BigDecimal.valueOf(1_300)),
                                price(BigDecimal.ONE)
                        )
                        .withDeliveries(DeliveryRequestUtils.postDelivery()).build()
        ).build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse =
                discountService.calculateDiscounts(request, applicabilityPolicy, null);

        assertNotNull(discountResponse);
        assertEquals(NO_FREE_DELIVERY, discountResponse.getFreeDeliveryStatus());
        assertEquals(REGION_WITHOUT_THRESHOLD, discountResponse.getFreeDeliveryReason());
        regionSettingsService.reloadCache();
        assertNull(discountResponse.getPriceLeftForFreeDelivery());

        assertThat(discountResponse.getOrders(), hasItems(
                allOf(
                        hasProperty("cartId", is("first")),
                        hasProperty("deliveries", hasItem(
                                hasProperty("promos", containsFreeDeliveryAddress())
                        ))
                ),
                allOf(
                        hasProperty("cartId", is("second")),
                        hasProperty("deliveries", hasItem(
                                hasProperty("promos", empty())
                        ))
                )
        ));
    }

    private static void assertOnlyFreeDeliveryAddressPromo(MultiCartWithBundlesDiscountResponse discountResponse) {
        assertThat(discountResponse.getOrders(), everyItem(
                hasProperty("deliveries", everyItem(
                        hasProperty("promos", containsFreeDeliveryAddress())
                ))
        ));
    }

    @Test
    public void testSingleUseCoupon() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        Coupon coupon = Coupon.builder()
                .setId(1L)
                .setCode("code")
                .setStatus(CouponStatus.ACTIVE)
                .setSourceKey("key")
                .setCreationTime(new Timestamp(clock.millis()))
                .setModificationTime(new Timestamp(clock.millis()))
                .setPromoId(promo.getId())
                .setVersion(1)
                .build();
        ruleFactory.getSinglePromoRule(promo.getRulesContainer(), RuleCategory.COUPON_RULE,
                discountUtils.getRulesPayload()).useCoupon(coupon);

        assertEquals(CouponStatus.USED, coupon.getStatus());
    }

    @Test
    public void testRevertDiscountTwice() {
        BigDecimal couponValue = BigDecimal.valueOf(100);
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setCouponValue(couponValue,
                CoreCouponValueType.FIXED));
        Coupon coupon = createCoupon(promo);
        OrderWithBundlesRequest orderRequest = orderRequestWithBundlesBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY))
                .build();
        MultiCartWithBundlesDiscountRequest discountRequest =
                DiscountRequestWithBundlesBuilder.builder(orderRequest).withCoupon(coupon.getCode()).build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        RulePayloads<?> rulesPayload = DiscountUtils.getRulesPayload(SpendMode.SPEND, applicabilityPolicy);
        MultiCartWithBundlesDiscountResponse discountResponse = discountService.spendDiscount(
                discountRequest,
                applicabilityPolicy,
                null
        );
        assertThat(accountDao.getBalance(promo.getBudgetAccountId()),
                comparesEqualTo(PromoUtils.DEFAULT_BUDGET.subtract(couponValue)));
        assertThat(accountDao.getBalance(promo.getSpendingAccountId()), comparesEqualTo(couponValue));

        discountService.revertDiscount(getDiscountTokens(discountResponse.getOrders().get(0)), null, new Date(),
                RevertSource.HTTP
        );
        List<CouponHistoryRecord> records = couponHistoryDao.getRecords(coupon.getId());
        assertEquals(4, records.size());
        assertThat(accountDao.getBalance(promo.getBudgetAccountId()), comparesEqualTo(PromoUtils.DEFAULT_BUDGET));
        assertThat(accountDao.getBalance(promo.getSpendingAccountId()), comparesEqualTo(BigDecimal.ZERO));

        try {
            discountService.revertDiscount(getDiscountTokens(discountResponse.getOrders().get(0)), null, new Date(),
                    RevertSource.HTTP
            );
            fail();
        } catch (MarketLoyaltyException e) {
            assertEquals(MarketLoyaltyErrorCode.REVERT_TOKEN_NOT_FOUND, e.getMarketLoyaltyErrorCode());
        }
        records = couponHistoryDao.getRecords(coupon.getId());
        assertEquals(4, records.size());
        assertThat(accountDao.getBalance(promo.getBudgetAccountId()), comparesEqualTo(PromoUtils.DEFAULT_BUDGET));
        assertThat(accountDao.getBalance(promo.getSpendingAccountId()), comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    public void shouldNotReturnErrorCoinsIfCartPriceAmountGreaterThanMinimalForApplicationOrder() {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse = discountService.calculateDiscounts(
                createRequestWithFreeDeliveryCoin(
                        UsageClientDeviceType.APPLICATION,
                        BigDecimal.valueOf(250),
                        BigDecimal.TEN
                ),
                applicabilityPolicy,
                null
        );
        assertTrue(discountResponse.getCoinErrors().isEmpty());
    }

    @Test
    public void shouldNotReturnErrorCoinsCartPriceLessThanMinimalForDesktopOrder() {
        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        MultiCartWithBundlesDiscountResponse discountResponse = discountService.calculateDiscounts(
                createRequestWithFreeDeliveryCoin(
                        UsageClientDeviceType.DESKTOP,
                        BigDecimal.valueOf(250),
                        BigDecimal.ONE
                ),
                applicabilityPolicy,
                null
        );
        assertTrue(discountResponse.getCoinErrors().isEmpty());
    }

    private MultiCartWithBundlesDiscountRequest createRequestWithFreeDeliveryCoin(
            UsageClientDeviceType deviceType, BigDecimal quantity, BigDecimal price
    ) {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );
        CoinKey freeDeliveryCoinKey = coinService.create.createCoin(
                smartShoppingPromo,
                defaultAuth(DEFAULT_UID)
                        .build()
        );

        OperationContext operationContext = OperationContextFactory.uidOperationContext();
        operationContext.setClientDeviceType(deviceType);
        return DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder().withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        quantity(quantity),
                        price(price)
                ).build()
        )
                .withOperationContext(operationContext)
                .withCoins(freeDeliveryCoinKey)
                .build();
    }

    private Coupon createCoupon(Promo promo) {
        CouponCreationRequest request = CouponCreationRequest.builder("key", promo.getId()).build();
        Coupon coupon = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());
        Map<String, String> couponWithKey = new HashMap<>();
        couponWithKey.put(coupon.getCode(), coupon.getSourceKey());
        couponService.activateCouponsFromInactive(couponWithKey);
        return coupon;
    }
}
