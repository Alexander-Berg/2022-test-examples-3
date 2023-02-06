package ru.yandex.market.loyalty.admin.archivation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.identity.Uuid;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.TransactionDao;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.coin.CoinHistoryDao;
import ru.yandex.market.loyalty.core.dao.coupon.CouponHistoryDao;
import ru.yandex.market.loyalty.core.model.RevertSource;
import ru.yandex.market.loyalty.core.model.accounting.TransactionEntry;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinNoAuth;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coupon.CouponCode;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.dao.coin.CoinDao.DISCOUNT_TABLE;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SHOP_PROMO_ID;

/**
 * @author artemmz
 */
public abstract class CleanupServiceTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String ACTIVATION_TOKEN = "foobar";

    @Autowired
    PromoManager promoManager;
    @Autowired
    CoinService coinService;
    @Autowired
    DiscountService discountService;
    @Autowired
    DiscountUtils discountUtils;
    @Autowired
    CoinDao coinDao;
    @Autowired
    TransactionDao transactionDao;
    @Autowired
    MetaTransactionDao metaTransactionDao;
    @Autowired
    CoinHistoryDao coinHistoryDao;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    CouponService couponService;
    @Autowired
    CouponHistoryDao couponHistoryDao;

    List<CoinKey> createCoins(long coinCount, int authNoAuthSplitFactor) {
        return createAndUseCoins(createSmartShoppingPromo(), coinCount, 0, authNoAuthSplitFactor);
    }

    /**
     * @return only non reverted coins
     */
    List<CoinKey> createAndUseAndRevertCoins(long coinCount, long useCoinCnt, int authNoAuthSplitFactor, long revertCnt) {
        Map<CoinKey, OrderWithBundlesResponse> coinWithDiscountResp =
                doCreateAndUseCoins(createSmartShoppingPromo(), coinCount, useCoinCnt, authNoAuthSplitFactor);

        Set<CoinKey> reverted = new HashSet<>();
        coinWithDiscountResp.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .limit(revertCnt)
                .forEach(e -> {
                    discountService.revertDiscount(getDiscountTokens(e.getValue()), null, new Date(), RevertSource.HTTP);
                    reverted.add(e.getKey());
                });

        reverted.forEach(coinWithDiscountResp::remove);
        return new ArrayList<>(coinWithDiscountResp.keySet());
    }

    List<CoinKey> createAndUseCoins(Promo promo, long coinCount, long useCoinCnt, int authNoAuthSplitFactor) {
        return new ArrayList<>(doCreateAndUseCoins(promo, coinCount, useCoinCnt, authNoAuthSplitFactor).keySet());
    }

    private Map<CoinKey, OrderWithBundlesResponse> doCreateAndUseCoins(Promo promo,
                                                                       long coinCount,
                                                                       long useCoinCnt,
                                                                       int authNoAuthSplitFactor) {
        List<CoinKey> createdCoins = Stream.iterate(0, i -> i + 1)
                .limit(coinCount)
                .map(i -> {
                    if (i % authNoAuthSplitFactor == 0) {
                        return coinService.create.createCoin(promo, defaultAuth(i).build());
                    } else {
                        CoinKey coin = coinService.create.createCoin(promo, defaultNoAuth(ACTIVATION_TOKEN).build());
                        return coinService.lifecycle.bindCoinsToUser(i, coin, false).stream().findFirst().map(Coin::getCoinKey).orElseThrow();
                    }
                })
                .collect(Collectors.toList());

        AtomicLong useCounter = new AtomicLong();
        Map<CoinKey, OrderWithBundlesResponse> result = new HashMap<>();
        createdCoins.forEach(coinKey -> {
            if (useCounter.incrementAndGet() > useCoinCnt) {
                result.put(coinKey, null);
                return;
            }

            Long uid = coinService.search.getCoin(coinKey).map(Coin::getUid).orElseThrow();
            var order = OrderRequestUtils.orderRequestWithBundlesBuilder()
                    .withOrderId(String.valueOf(coinKey.getId()))
                    .withOrderItem()
                    .build();

            var orderWithDiscount = discountService.spendDiscount(
                    DiscountRequestWithBundlesBuilder.builder(order)
                            .withCoins(coinKey)
                            .withOperationContext(OperationContextFactory.withUidBuilder(uid).buildOperationContext())
                            .build(),
                    configurationService.currentPromoApplicabilityPolicy(),
                    null
            ).getOrders().get(0);

            result.put(coinKey, orderWithDiscount);
        });
        return result;
    }

    Promo createSmartShoppingPromo() {
        return promoManager.createPromocodePromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .setClid(12345L)
                        .setShopPromoId(SHOP_PROMO_ID)
                        .setCode("_PROMOCODE_")
                        .setBudget(BigDecimal.valueOf(100_500))
                        .setEmissionBudget(BigDecimal.valueOf(100_500))
        );
    }

    List<Coupon> useCoupons(long couponCount) {
        Promo couponPromo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        return Stream.iterate(0, i -> i + 1)
                .limit(couponCount)
                .map(i -> {
                    CouponCreationRequest couponRequest = CouponCreationRequest.builder(String.valueOf(i),
                                    couponPromo.getPromoId().getId())
                            .identity(new Uuid(String.valueOf(i)))
                            .build();
                    return couponService.createOrGetCoupon(couponRequest, discountUtils.getRulesPayload());
                })
                .map(coupon -> couponService.activateCoupon(CouponCode.of(coupon.getCode()), "activate_me!"))
                .peek(coupon -> {
                    var order = OrderRequestUtils.orderRequestWithBundlesBuilder()
                            .withOrderId(String.valueOf(RandomUtils.nextInt(0, 10)))
                            .withOrderItem()
                            .build();

                    discountService.spendDiscount(
                            DiscountRequestWithBundlesBuilder.builder(order)
                                    .withCoupon(coupon.getCode())
                                    .withOperationContext(OperationContextFactory.withUidBuilder(RandomUtils.nextInt(0, 10)).buildOperationContext())
                                    .build(),
                            configurationService.currentPromoApplicabilityPolicy(),
                            null
                    );
                }).collect(Collectors.toList());
    }

    void checkCreatedCouponData(List<Coupon> coupons) {
        List<Coupon> couponsFromDb = couponService.getCouponByCode(coupons.stream()
                .map(Coupon::getCode)
                .map(CouponCode::of)
                .collect(Collectors.toSet()));

        assertEquals(coupons.size(), couponsFromDb.size());

        Set<Long> couponHistoryRecordIds = getCouponHistoryRecordsIdsByIds(coupons);
        assertEquals(coupons.isEmpty(), couponHistoryRecordIds.isEmpty());

        Map<Long, Long> couponHistoryXRecordsIdsWithTransactions =
                getCouponHistoryXRecordsIdsWithTransactions(couponHistoryRecordIds);
        assertEquals(coupons.isEmpty(), couponHistoryXRecordsIdsWithTransactions.isEmpty());
    }

    void checkCreatedCoinData(List<CoinKey> coinKeys) {
        checkCreatedCoinData(coinKeys, coinKeys.size(), 0);
    }

    void checkCreatedCoinData(List<CoinKey> coinKeys, long coinCnt, long useCoinCnt) {
        assertFalse(findNoAuthCoins(coinKeys).isEmpty());
        assertEquals(useCoinCnt == 0, findUsedCoins().isEmpty());
        assertTrue(findExpiredCoins().isEmpty());

        Set<Long> historyRecordsIds = getHistoryRecordsIds(coinKeys);
        assertFalse(historyRecordsIds.isEmpty());

        var historyXRecordsIdsWithTransactions = getHistoryXRecordsIdsWithTransactions(historyRecordsIds);
        assertFalse(historyXRecordsIdsWithTransactions.isEmpty());
        assertEquals(coinCnt * 2 + useCoinCnt * 2, getTransactions(coinKeys).size());
    }

    List<CoinNoAuth> findNoAuthCoins(List<CoinKey> coinKeys) {
        return coinDao.getCoinsNoAuth(new HashSet<>(coinKeys));
    }

    List<Coin.Builder> findExpiredCoins() {
        return coinDao.getCoins(DISCOUNT_TABLE.status.eqTo(CoreCoinStatus.EXPIRED), false);
    }

    List<Coin.Builder> findUsedCoins() {
        return coinDao.getCoins(DISCOUNT_TABLE.status.eqTo(CoreCoinStatus.USED), false);
    }

    List<Long> getTransactions(List<CoinKey> coinKeys, List<Coupon> coupons) {
        Set<Long> couponHistoryRecordsIds = getCouponHistoryRecordsIdsByIds(coupons);
        Collection<Long> couponTransactionIds =
                getCouponHistoryXRecordsIdsWithTransactions(couponHistoryRecordsIds).values();

        return Stream.concat(
                        getTransactions(coinKeys).stream(),
                        transactionDao.getTransactionRows(new HashSet<>(couponTransactionIds)).stream()
                                .map(TransactionEntry::getTransactionId)
                )
                .collect(Collectors.toList());
    }

    List<Long> getTransactions(List<CoinKey> coinKeys) {
        Set<Long> historyRecordsIds = getHistoryRecordsIds(coinKeys);
        Collection<Long> transactionIds = getHistoryXRecordsIdsWithTransactions(historyRecordsIds).values();
        return transactionDao.getTransactionRows(new HashSet<>(transactionIds)).stream()
                .map(TransactionEntry::getTransactionId)
                .collect(Collectors.toList());
    }

    Set<Long> getHistoryRecordsIds(Collection<CoinKey> coinKeys) {
        return getHistoryRecordsIdsByIds(Lists.transform(new ArrayList<>(coinKeys), CoinKey::getId));
    }

    Set<Long> getHistoryRecordsIdsByIds(Collection<Long> coinIds) {
        return coinHistoryDao.getHistoryRecordsIds(coinIds);
    }

    Set<Long> getCouponHistoryRecordsIdsByIds(Collection<Coupon> couponIds) {
        return couponHistoryDao.getHistoryRecordsIds(couponIds.stream().map(Coupon::getId).collect(Collectors.toSet()));
    }

    Map<Long, Long> getHistoryXRecordsIdsWithTransactions(Collection<Long> coinHistoryIds) {
        return coinHistoryDao.getHistoryXRecordsIdsWithTransactions(coinHistoryIds);
    }

    Map<Long, Long> getCouponHistoryXRecordsIdsWithTransactions(Collection<Long> couponHistoryIds) {
        return couponHistoryDao.getHistoryXRecordsIdsWithTransactions(couponHistoryIds);
    }
}
