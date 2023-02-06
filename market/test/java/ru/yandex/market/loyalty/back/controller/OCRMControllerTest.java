package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.CouponValueType;
import ru.yandex.market.loyalty.api.model.coin.CRMUserCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.OrdersStatusUpdatedRequest;
import ru.yandex.market.loyalty.api.model.coin.OrdersUpdatedCoinsForFront;
import ru.yandex.market.loyalty.api.model.coin.UserCoinHistoryResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.ocrm.CashbackAccrualStatus;
import ru.yandex.market.loyalty.api.model.ocrm.CashbackRefundStatus;
import ru.yandex.market.loyalty.api.model.ocrm.CoinCashbackFilter;
import ru.yandex.market.loyalty.api.model.ocrm.Coupon;
import ru.yandex.market.loyalty.api.model.ocrm.EntityType;
import ru.yandex.market.loyalty.api.model.ocrm.OcrmUserCoinsWithCashbackResponse;
import ru.yandex.market.loyalty.api.model.ocrm.OrderCoins;
import ru.yandex.market.loyalty.api.model.ocrm.ReferralInfoRequest;
import ru.yandex.market.loyalty.api.model.ocrm.ReferralInfoResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.back.util.CashbackUtils;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.coin.CoinTestDataDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserReferralPromocodeDao;
import ru.yandex.market.loyalty.core.model.OperationContext;
import ru.yandex.market.loyalty.core.model.RevertSource;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinHistoryTraceRecord;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletNewTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.model.ydb.UserReferralPromocode;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoStatusWithBudgetCacheService;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredMetaTransactionService;
import ru.yandex.market.loyalty.core.service.coin.CoinHistoryTraceDao;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.ReferralProgramService;
import ru.yandex.market.loyalty.core.service.promocode.UserPromocodeInfo;
import ru.yandex.market.loyalty.core.test.CheckouterMockUtils;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType.BINDING;
import static ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType.CREATION;
import static ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType.EXPIRING;
import static ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType.REVERSION;
import static ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType.REVOCATION;
import static ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType.USAGE;
import static ru.yandex.market.loyalty.api.model.coin.CoinHistoryReason.CREATION_ORDER;
import static ru.yandex.market.loyalty.api.model.coin.CoinHistoryReason.ORDER_CANCEL;
import static ru.yandex.market.loyalty.back.controller.CoinsControllerCoinsForMultiOrderTest.DEFAULT_MULTI_ORDER;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate.EFFECTIVELY_PROCESSING;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate.PROCESSING_PREPAID;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.CANCELLED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.CONFIRMATION_ERROR;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.CONFIRMED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.IN_QUEUE;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.PENDING;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.PROCESSED;
import static ru.yandex.market.loyalty.core.service.discount.DiscountChangeSource.POST_DISCOUNT_REVERT;
import static ru.yandex.market.loyalty.core.test.CheckouterMockUtils.getOrderInStatus;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.ANOTHER_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(OCRMController.class)
public class OCRMControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final long FIRST_MULTI_ORDER_PART_ID = 100L;
    private static final long SECOND_MULTI_ORDER_PART_ID = 101L;
    private static final String PROMOCODE = "PROMOCODE";
    private static final String DEFAULT_ACCRUAL_PROMO_KEY = "accrual_promo";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private CheckouterMockUtils checkouterMockUtils;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private CoinTestDataDao coinTestDataDao;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private DeferredMetaTransactionService deferredMetaTransactionService;
    @Autowired
    private PromoStatusWithBudgetCacheService promoStatusWithBudgetCacheService;
    @Autowired
    private ReferralProgramService referralProgramService;
    @Autowired
    private UserReferralPromocodeDao userReferralPromocodeDao;
    @Autowired
    private CashbackUtils cashbackUtils;
    @Autowired
    private CoinHistoryTraceDao coinHistoryTraceDao;

    @Test
    public void shouldReturnCoinsForOrderWithStatusIfEventWasProcessed() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinService.create.createCoin(promo, defaultAuth().build());

        List<CRMUserCoinResponse> coins = marketLoyaltyClient.getCoins(DEFAULT_UID);

        assertThat(coins, hasSize(1));
    }

    @Test
    public void shouldReturnArchivedCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinTestDataDao.createArchivedCoin(promo, 1L, "archived-coin-1", false, DEFAULT_UID);

        List<CRMUserCoinResponse> coins = marketLoyaltyClient.getCoins(DEFAULT_UID);

        assertThat(coins, hasSize(1));
    }

    @Test
    public void shouldReturnIssuedAndUsedCoinsForOrder() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, orderRestriction(EFFECTIVELY_PROCESSING));

        CoinKey usedCoin = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(DEFAULT_ORDER_ID)
                .setUid(DEFAULT_UID)
                .addItem(CheckouterUtils.defaultOrderItem()
                        .build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder()
                                .withOrderId(Long.toString(DEFAULT_ORDER_ID))
                                .withOrderItem()
                                .build()
                )
                .withCoins(usedCoin)
                .build()
        );

        OrdersUpdatedCoinsForFront issuedCoins = marketLoyaltyClient.sendOrderStatusUpdatedEvent(
                CheckouterUtils.defaultStatusUpdatedRequest(DEFAULT_ORDER_ID));

        OrderCoins coins = marketLoyaltyClient.getIssuedAndUsedCoinsForOrder(DEFAULT_ORDER_ID);

        assertThat(
                coins,
                allOf(
                        hasProperty(
                                "issuedCoins",
                                contains(
                                        allOf(
                                                hasProperty(
                                                        "id", equalTo(issuedCoins.getNewCoins()
                                                                .get(0)
                                                                .getId())),
                                                hasProperty("coinPromocode", equalTo(false)),
                                                hasProperty("historyRecords", hasSize(1))
                                        )
                                )
                        ),
                        hasProperty(
                                "usedCoins",
                                contains(
                                        allOf(
                                                hasProperty("id", equalTo(usedCoin.getId())),
                                                hasProperty("coinPromocode", equalTo(false)),
                                                hasProperty("historyRecords", hasSize(2))
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldReturnIssuedAndUsedCoinsForSmartshoppingPromocodeForOrder() {
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE));
        Order order = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(DEFAULT_ORDER_ID)
                .setUid(DEFAULT_UID)
                .addItem(CheckouterUtils.defaultOrderItem()
                        .build())
                .build();
        checkouterMockUtils.mockCheckoutGetOrdersResponse(order);

        PromocodeActivationResponse response = marketLoyaltyClient.activatePromocodes(
                PromocodeActivationRequest.builder()
                        .userId(DEFAULT_UID)
                        .codes(Set.of(PROMOCODE))
                        .build());

        assertThat(response.getActivationResults(), hasSize(1));
        assertThat(response.getActivationResults()
                .get(0)
                .getCode(), is(PROMOCODE));
        assertThat(response.getActivationResults()
                .get(0)
                .getResultCode(), is(PromocodeActivationResultCode.SUCCESS));

        UserPromocodeInfo promocodeInfo = promocodeService.retrievePromocodeInfo(DEFAULT_UID, PROMOCODE)
                .stream()
                .findFirst()
                .orElse(null);

        assertThat(promocodeInfo, notNullValue());
        assertThat(promocodeInfo.getCoinKey(), notNullValue());

        Coin activeCoin = Optional.ofNullable(coinService.search.getCoin(promocodeInfo.getCoinKey()))
                .map(Optional::get)
                .orElse(null);

        assertThat(activeCoin, notNullValue());

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder()
                                .withOrderId(Long.toString(DEFAULT_ORDER_ID))
                                .withOrderItem()
                                .build()
                )
                .withCoins(activeCoin.getCoinKey())
                .build()
        );

        OrderCoins coins = marketLoyaltyClient.getIssuedAndUsedCoinsForOrder(DEFAULT_ORDER_ID);

        assertThat(
                coins,
                allOf(
                        hasProperty(
                                "usedCoins",
                                contains(
                                        allOf(
                                                hasProperty(
                                                        "id", equalTo(activeCoin.getCoinKey()
                                                                .getId())),
                                                hasProperty("coinPromocode", equalTo(true)),
                                                hasProperty("historyRecords", hasSize(2))
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldReturnHistoryWithOrderIds() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        deferredMetaTransactionService.consumeBatchOfTransactions(1);
        CoinKey usedCoin = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder()
                                .withOrderId(Long.toString(DEFAULT_ORDER_ID))
                                .withOrderItem()
                                .build(),
                        orderRequestBuilder()
                                .withOrderId(Long.toString(ANOTHER_ORDER_ID))
                                .withOrderItem()
                                .build()
                )
                .withCoins(usedCoin)
                .build()
        );

        OrderCoins coins = marketLoyaltyClient.getIssuedAndUsedCoinsForOrder(DEFAULT_ORDER_ID);
        assertThat(
                coins,
                allOf(
                        hasProperty("issuedCoins", is(empty())),
                        hasProperty(
                                "usedCoins",
                                contains(
                                        allOf(
                                                hasProperty("id", equalTo(usedCoin.getId())),
                                                hasProperty("historyRecords", containsInAnyOrder(
                                                        hasProperty("orderIds", is(empty())),
                                                        hasProperty(
                                                                "orderIds",
                                                                containsInAnyOrder(equalTo(DEFAULT_ORDER_ID),
                                                                        equalTo(ANOTHER_ORDER_ID))
                                                        )
                                                )),
                                                hasProperty("coinPromocode", equalTo(false))
                                        )
                                )
                        )
                )
        );
    }

    @Test
    public void shouldGetCoinHistory() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
        );
        // CREATION
        Coin coin = coinService.search.getCoin(coinService.create.createCoin(promo, ANOTHER_ORDER_ID, null,
                        defaultNoAuth()
                                .setReason(CoreCoinCreationReason.ORDER)
                                .build()
                ))
                .orElseThrow(AssertionError::new);
        // BINDING
        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, coin.getActivationToken());
        // USAGE
        MultiCartDiscountResponse usage = marketLoyaltyClient.spendDiscount(
                DiscountRequestBuilder
                        .builder(orderRequestBuilder().withOrderItem()
                                .build())
                        .withCoins(coin.getCoinKey())
                        .build()
        );
        // REVERTING
        marketLoyaltyClient.revertDiscount(getDiscountTokens(usage.getOrders()
                .get(0)));
        // REVOKING
        discountService.revertDiscount(
                ANOTHER_ORDER_ID, null, OperationContext.empty(), Date.from(clock.instant()), RevertSource.HTTP,
                t -> true
        );

        List<CoinHistoryTraceRecord> coinHistoryTraceDaoRecords =
                coinHistoryTraceDao.getRecordsByDiscountChangeSource(POST_DISCOUNT_REVERT);
        assertThat(coinHistoryTraceDaoRecords, hasSize(1));

        UserCoinHistoryResponse coinHistory = marketLoyaltyClient.getCoinHistory(coin.getCoinKey()
                .getId());
        assertThat(coinHistory, hasProperty(
                "records",
                containsInAnyOrder(
                        allOf(
                                hasProperty("recordType", equalTo(CREATION)),
                                hasProperty("orderIds", contains(equalTo(ANOTHER_ORDER_ID))),
                                hasProperty("reason", equalTo(CREATION_ORDER))
                        ),
                        allOf(
                                hasProperty("recordType", equalTo(BINDING)),
                                hasProperty("orderIds", is(empty()))
                        ),
                        allOf(
                                hasProperty("recordType", equalTo(USAGE)),
                                hasProperty("orderIds",
                                        contains(equalTo(Long.valueOf(OrderRequestUtils.DEFAULT_ORDER_ID))))
                        ),
                        hasProperty("recordType", equalTo(REVERSION)),
                        allOf(
                                hasProperty("recordType", equalTo(REVOCATION)),
                                hasProperty("reason", equalTo(ORDER_CANCEL))
                        )
                )
        ));
    }

    @Test
    public void shouldGetArchivedCoinHistory() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinTestDataDao.createArchivedCoin(promo, 1L, "archived-coin-1", false, DEFAULT_UID);

        UserCoinHistoryResponse coinHistory = marketLoyaltyClient.getCoinHistory(1L);
        assertThat(coinHistory, hasProperty(
                "records",
                containsInAnyOrder(
                        allOf(
                                hasProperty("recordType", equalTo(CREATION)),
                                hasProperty("orderIds", is(empty()))
                        ),
                        allOf(
                                hasProperty("recordType", equalTo(EXPIRING)),
                                hasProperty("orderIds", is(empty()))
                        )
                )
        ));
    }

    @Test
    public void shouldReturnEmptyResponseOnNotFoundOrder() {
        OrderCoins coins = marketLoyaltyClient.getIssuedAndUsedCoinsForOrder(DEFAULT_ORDER_ID);
        assertThat(coins.getIssuedCoins(), is(empty()));
        assertThat(coins.getUsedCoins(), is(empty()));
    }

    @Test
    public void shouldReturnEmptyResponseOnNotFoundUser() {
        List<CRMUserCoinResponse> coins = marketLoyaltyClient.getCoins(DEFAULT_UID);
        assertThat(coins, is(empty()));
    }

    @Test
    public void shouldReturnSameMultiOrderCoinsForEachPartOfMultiOrderOnOcrmRequest() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(PROCESSING_PREPAID)
        );

        setupCheckouterEventWithGivenStatus(OrderStatus.PROCESSING);
        sendOrderStatusUpdatedEventReturningCoins();

        List<Long> multiOrderCoinIds = getCreatedCoinForDefaultMultiOrder();

        OrderCoins firstMultiOrderPartCoins = marketLoyaltyClient.getIssuedAndUsedCoinsForOrder(
                FIRST_MULTI_ORDER_PART_ID
        );
        OrderCoins secondMultiOrderPartCoins = marketLoyaltyClient.getIssuedAndUsedCoinsForOrder(
                SECOND_MULTI_ORDER_PART_ID
        );

        assertThat(
                multiOrderCoinIds,
                hasSize(1)
        );
        assertThat(
                firstMultiOrderPartCoins.getIssuedCoins(),
                hasSize(1)
        );
        assertThat(
                secondMultiOrderPartCoins.getIssuedCoins(),
                hasSize(1)
        );
        assertThat(
                firstMultiOrderPartCoins.getIssuedCoins()
                        .get(0)
                        .getId(),
                equalTo(multiOrderCoinIds.get(0))
        );
        assertThat(
                secondMultiOrderPartCoins.getIssuedCoins()
                        .get(0)
                        .getId(),
                equalTo(multiOrderCoinIds.get(0))
        );
    }

    @Test
    public void shouldReturnUsedCouponOnOcrmOrderRequest() {
        final Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );
        final ru.yandex.market.loyalty.core.model.coupon.Coupon coupon = couponService.createOrGetCoupon(
                CouponCreationRequest.builder("test", promo.getId())
                        .forceActivation(true)
                        .build(), discountUtils.getRulesPayload()
        );

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder().withOrderId(DEFAULT_ORDER_ID)
                                .withOrderItem()
                                .build()
                )
                .withCoupon(coupon.getCode())
                .build()
        );

        Coupon usedCoupon =
                marketLoyaltyClient
                        .getIssuedAndUsedCoinsForOrder(DEFAULT_ORDER_ID)
                        .getUsedCoupon();

        assertThat(
                usedCoupon,
                allOf(
                        hasProperty("couponCode", equalTo(coupon.getCode())),
                        hasProperty("couponNominal", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("couponValueType", equalTo(CouponValueType.FIXED)),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("couponRestrictions", allOf(
                                hasProperty("minOrderTotal", is(nullValue())),
                                hasProperty("maxOrderTotal", is(nullValue())),
                                hasProperty("oneUsagePerUser", equalTo(false)),
                                hasProperty("usageClientDeviceTypes", is(nullValue())),
                                hasProperty("description", equalTo(promo.getDescription()))
                        ))
                )
        );
    }

    @Test
    public void shouldReturnUsedCoupons() {
        final Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
        );
        final ru.yandex.market.loyalty.core.model.coupon.Coupon coupon = couponService.createOrGetCoupon(
                CouponCreationRequest.builder("test", promo.getId())
                        .forceActivation(true)
                        .build(), discountUtils.getRulesPayload()
        );

        marketLoyaltyClient.spendDiscount(DiscountRequestBuilder
                .builder(
                        orderRequestBuilder()
                                .withOrderId(DEFAULT_ORDER_ID)
                                .withOrderItem()
                                .build()
                )
                .withOperationContext(OperationContextFactory.uidOperationContextDto(DEFAULT_UID))
                .withCoupon(coupon.getCode())
                .build()
        );

        List<Coupon> usedCoupon =
                marketLoyaltyClient
                        .getUsedCouponsForUser(DEFAULT_UID)
                        .getCoupons();

        assertThat(
                usedCoupon,
                contains(
                        allOf(
                                hasProperty("couponCode", equalTo(coupon.getCode())),
                                hasProperty("couponNominal", comparesEqualTo(BigDecimal.valueOf(300))),
                                hasProperty("couponValueType", equalTo(CouponValueType.FIXED)),
                                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(300))),
                                hasProperty("couponRestrictions", allOf(
                                        hasProperty("minOrderTotal", is(nullValue())),
                                        hasProperty("maxOrderTotal", is(nullValue())),
                                        hasProperty("oneUsagePerUser", equalTo(false)),
                                        hasProperty("usageClientDeviceTypes", is(nullValue())),
                                        hasProperty("description", equalTo(promo.getDescription()))
                                ))
                        )
                )
        );
    }

    @Test
    public void shouldReturnCoinHistoryWithAllPartsOfMultiOrder() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo,
                orderRestriction(PROCESSING_PREPAID)
        );

        setupCheckouterEventWithGivenStatus(OrderStatus.PROCESSING);
        sendOrderStatusUpdatedEventReturningCoins();

        List<Long> multiOrderCoinIds = getCreatedCoinForDefaultMultiOrder();
        Coin coinForMultiOrder = coinService.search.getCoin(new CoinKey(multiOrderCoinIds.get(0)))
                .orElseThrow();

        UserCoinHistoryResponse coinHistory = marketLoyaltyClient.getCoinHistory(
                coinForMultiOrder.getCoinKey()
                        .getId());

        assertThat(
                coinHistory.getRecords(),
                allOf(
                        hasSize(1),
                        everyItem
                                (hasProperty(
                                        "orderIds",
                                        containsInAnyOrder(FIRST_MULTI_ORDER_PART_ID, SECOND_MULTI_ORDER_PART_ID)
                                ))
                )
        );
    }

    @Test
    public void allCashbackEnumsMustHaveLinksToCore() {
        for (YandexWalletTransactionStatus value : YandexWalletTransactionStatus.values()) {
            assertThat(CashbackAccrualStatus.findByCode(value.getCode()), notNullValue());
        }
        for (YandexWalletRefundTransactionStatus value : YandexWalletRefundTransactionStatus.values()) {
            assertThat(CashbackRefundStatus.findByCode(value.getCode()), notNullValue());
        }
    }

    @Test
    public void shouldReturnUserCashbackAndCoins() {
        final long uid = 100L;
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        coinService.create.createCoin(promo, defaultAuth(uid).build());

        yandexWalletTransactionDao.enqueueTransactions(
                null, "cashback", createWalletTopUps(0, 4, uid), YandexWalletTransactionPriority.HIGH);

        List<OcrmUserCoinsWithCashbackResponse> coinsV2 = marketLoyaltyClient.getCoinsV2(uid, 0, 10, null);
        assertThat(coinsV2, hasSize(5));
    }

    @Test
    public void shouldReturnFilteredContent() {
        final long uid = 100L;
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed()
                .setName("Promo1"));
        //Title купона = Скидка 300 ₽
        coinService.create.createCoin(promo, defaultAuth(uid).build());

        yandexWalletTransactionDao.enqueueTransactions(
                null, "cashback", createWalletTopUps(0, 4, uid),
                null, null, promo.getId(), YandexWalletTransactionPriority.HIGH
        );
        promo = promoManager.createSmartShoppingPromo(defaultFixed()
                .setName("Promo2"));
        yandexWalletTransactionDao.enqueueTransactions(
                null, "cashback", createWalletTopUps(4, 4, uid),
                null, null, promo.getId(), YandexWalletTransactionPriority.HIGH
        );

        List<OcrmUserCoinsWithCashbackResponse> coinsV2 = marketLoyaltyClient.getCoinsV2(uid, 0, 10,
                CoinCashbackFilter.of(null, EntityType.CASHBACK));
        assertThat(coinsV2, hasSize(8));

        coinsV2 = marketLoyaltyClient.getCoinsV2(uid, 0, 10,
                CoinCashbackFilter.of(null, EntityType.COIN));
        assertThat(coinsV2, hasSize(1));

        coinsV2 = marketLoyaltyClient.getCoinsV2(uid, 0, 10,
                CoinCashbackFilter.of("", EntityType.CASHBACK));
        assertThat(coinsV2, hasSize(8));

        coinsV2 = marketLoyaltyClient.getCoinsV2(uid, 0, 10,
                CoinCashbackFilter.of("", EntityType.COIN));
        assertThat(coinsV2, hasSize(1));

        coinsV2 = marketLoyaltyClient.getCoinsV2(uid, 0, 10,
                CoinCashbackFilter.of("promo", null));
        assertThat(coinsV2, hasSize(8));

        coinsV2 = marketLoyaltyClient.getCoinsV2(uid, 0, 10,
                CoinCashbackFilter.of("promo1", null));
        assertThat(coinsV2, hasSize(4));

        coinsV2 = marketLoyaltyClient.getCoinsV2(uid, 0, 10,
                CoinCashbackFilter.of("promo2", null));
        assertThat(coinsV2, hasSize(4));
    }

    @Test
    public void shouldReturnStatisticWithoutAccruals() throws Exception {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_MAX_REFERRER_REWARD, 5000);
        createPromosForReferralProgram();

        ReferralInfoRequest referralInfoRequest = new ReferralInfoRequest(0L, clock.instant(), clock.instant().plus(1
                , ChronoUnit.DAYS));
        ReferralInfoResponse referralInfoResponse = sendReferralRequest(referralInfoRequest);
        assertThat(referralInfoResponse, allOf(
                hasProperty("maxRefererReward", equalTo(5000L)),
                hasProperty("userGotForReferrals", equalTo(0L)),
                hasProperty("userReferralPromocodes", hasSize(0))
        ));
    }

    @Test
    public void shouldReturnStatisticWithAccruals() throws Exception {
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_MAX_REFERRER_REWARD, 5000);
        createPromosForReferralProgram();
        long uid = 22L;
        createUserAccruals(uid);
        for (int i = 0; i < 5; i++) {
            userReferralPromocodeDao.insertNewEntry(UserReferralPromocode.builder()
                    .setPromocode(UUID.randomUUID().toString())
                    .setUid(uid)
                    .setAssignTime(clock.instant())
                    .setExpireTime(clock.instant().plus(1, ChronoUnit.DAYS))
                    .build()
            );
        }

        ReferralInfoRequest referralInfoRequest = new ReferralInfoRequest(uid, clock.instant(), clock.instant().plus(1
                , ChronoUnit.DAYS));
        ReferralInfoResponse referralInfoResponse = sendReferralRequest(referralInfoRequest);
        assertThat(referralInfoResponse, allOf(
                hasProperty("maxRefererReward", equalTo(5000L)),
                hasProperty("userGotForReferrals", equalTo(1600L)),
                hasProperty("userReferralPromocodes", hasSize(5))
        ));
    }

    @Test
    public void shouldThrowErrorWithNoConfig() throws Exception {
        ReferralInfoRequest referralInfoRequest = new ReferralInfoRequest(0L, clock.instant(), clock.instant().plus(1
                , ChronoUnit.DAYS));
        MockHttpServletRequestBuilder requestBuilder = get("/ocrm/referral/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(referralInfoRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    private void createUserAccruals(long uid) {
        Promo.PromoStatusWithBudget promoStatusWithBudget =
                referralProgramService.loadReferralPromo(configurationService.getReferralProgramAccrualPromoKey())
                        .orElseThrow();
        cashbackUtils.createUserAccruals(uid,
                promoStatusWithBudget.getPromoId(),
                Pair.of(300L, CONFIRMED),
                Pair.of(300L, CONFIRMED),
                Pair.of(500L, CONFIRMED),
                Pair.of(500L, CONFIRMED),
                Pair.of(300L, PENDING),
                Pair.of(300L, IN_QUEUE),
                Pair.of(300L, PROCESSED),
                Pair.of(300L, CANCELLED),
                Pair.of(300L, CONFIRMATION_ERROR)
        );
    }

    private void createPromosForReferralProgram() {
        Promo accrualPromo = promoManager.createAccrualPromo(PromoUtils.WalletAccrual.defaultModelAccrual()
                .setName(DEFAULT_ACCRUAL_PROMO_KEY)
                .setPromoKey(DEFAULT_ACCRUAL_PROMO_KEY)
                .setStartDate(java.sql.Date.from(clock.instant()))
                .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        Promo couponPromo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultInfiniteUse()
                        .setCouponCode("REFERRAL_CODE")
                        .setBudget(BigDecimal.valueOf(1000))
                        .setEmissionBudget(BigDecimal.valueOf(1000))
                        .setBudgetMode(BudgetMode.SYNC)
                        .setEndDate(java.sql.Date.from(clock.instant().plus(7, ChronoUnit.DAYS)))
        );
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_ACCRUAL, accrualPromo.getPromoKey());
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_PROMOCODE, couponPromo.getPromoKey());
        promoStatusWithBudgetCacheService.reloadCache();
    }

    private ReferralInfoResponse sendReferralRequest(ReferralInfoRequest request) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/ocrm/referral/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
        MockHttpServletResponse response = mockMvc.perform(requestBuilder)
                .andReturn()
                .getResponse();
        ReferralInfoResponse referralInfoResponse = objectMapper.readValue(response.getContentAsString(),
                ReferralInfoResponse.class);
        return referralInfoResponse;
    }

    private List<YandexWalletNewTransaction> createWalletTopUps(int fromId, int size, long uid) {
        return LongStream.range(fromId, fromId + size)
                .mapToObj(i -> new YandexWalletNewTransaction(
                        uid,
                        BigDecimal.valueOf(100),
                        Long.toString(i),
                        "{\"is_employee\": \"true\",\"product_id\": \"product\"}",
                        "product",
                        null
                ))
                .collect(Collectors.toList());
    }

    @NotNull
    private List<Long> getCreatedCoinForDefaultMultiOrder() {
        return coinService.search.getAllCoinsByMultiOrderId(
                        DEFAULT_MULTI_ORDER)
                .get(CREATION)
                .stream()
                .map(Coin::getCoinKey)
                .map(CoinKey::getId)
                .collect(Collectors.toList());
    }

    private void setupCheckouterEventWithGivenStatus(OrderStatus status) {
        checkouterMockUtils.mockCheckouterPagedEvents(
                getOrderInStatus(status)
                        .setUid(DEFAULT_UID)
                        .setOrdersCount(2)
                        .setOrderId(FIRST_MULTI_ORDER_PART_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER)
                        .build(),
                getOrderInStatus(status)
                        .setUid(DEFAULT_UID)
                        .setOrdersCount(2)
                        .setOrderId(SECOND_MULTI_ORDER_PART_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER)
                        .build()
        );
    }

    private OrdersUpdatedCoinsForFront sendOrderStatusUpdatedEventReturningCoins() {
        return marketLoyaltyClient.sendOrderStatusUpdatedEventAndGetCoins(
                new OrdersStatusUpdatedRequest(
                        Arrays.asList(
                                FIRST_MULTI_ORDER_PART_ID,
                                SECOND_MULTI_ORDER_PART_ID
                        ),
                        DEFAULT_UID, null, false
                )
        );
    }
}
