package ru.yandex.market.loyalty.core.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.api.model.identity.Uuid;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.OperationContextDao;
import ru.yandex.market.loyalty.core.dao.coupon.CouponDao;
import ru.yandex.market.loyalty.core.dao.coupon.CouponHistoryDao;
import ru.yandex.market.loyalty.core.model.OperationContext;
import ru.yandex.market.loyalty.core.model.RevertSource;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.coupon.CouponHistoryRecord;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.market.loyalty.core.service.coupon.CouponCode.of;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * Created by maratik.
 */
public class CouponServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final Logger logger = LogManager.getLogger(CouponServiceTest.class);

    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponDao couponDao;
    @Autowired
    private CouponHistoryDao couponHistoryDao;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private OperationContextDao operationContextDao;
    @Autowired
    private MetaTransactionDao metaTransactionDao;
    @Autowired
    private DiscountUtils discountUtils;

    private Promo promo;

    @Before
    public void init() {
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.valueOf(10_000)));
    }

    @Test(expected = MarketLoyaltyException.class)
    public void canNotCreateCouponIfPromoNotActive() {
        Promo promo = promoManager.createCouponPromo(
                PromoUtils.Coupon.defaultSingleUse()
                        .setEmissionBudget(BigDecimal.valueOf(10_000))
                        .setStartEmissionDate(new Date(0L))
                        .setEndEmissionDate(new Date(0L))
        );
        createOrGetCoupon(promo.getId());
    }

    @Test
    public void testOptimisticLock() {
        Coupon couponOld1 = createOrGetCoupon(promo.getId());
        couponOld1.setStatus(CouponStatus.ACTIVE);
        Coupon couponOld = couponService.activateCouponsFromInactive(Collections.singletonMap(couponOld1.getCode(),
                "key1")).get(0);
        Coupon coupon = couponService.getCouponByCode(of(couponOld.getCode())).orElseThrow(() -> new AssertionError(
                "not found"));
        assertNotNull(coupon.getActivationTime());

        coupon.setStatus(CouponStatus.USED);

        ExecutorService pool = Executors.newFixedThreadPool(2);

        AtomicBoolean firstUpdatePassed = new AtomicBoolean(false);
        AtomicBoolean secondTransactionFailed = new AtomicBoolean(false);
        AtomicBoolean secondTransactionStarted = new AtomicBoolean(false);
        AtomicBoolean firstTransactionSucceeded = new AtomicBoolean(false);

        pool.submit((Runnable) () -> transactionTemplate.execute(status -> {
            try {
                Coupon localCoupon =
                        couponService.getCouponByCode(of(coupon.getCode())).orElseThrow(() -> new AssertionError("not" +
                                " found"));
                OperationContext operationContext = OperationContextFactory.uidOperationContext();
                operationContextDao.save(operationContext);
                long transactionId = metaTransactionDao.createEmptyTransaction();

                firstTransactionSucceeded.set(
                        couponService.useCoupon(
                                localCoupon, promo, ImmutableSet.of(transactionId), String.valueOf(123L),
                                operationContext, discountUtils.getRulesPayload()
                        ).getStatus() == CouponStatus.USED
                );
            } finally {
                firstUpdatePassed.set(true);
            }
            while (!secondTransactionStarted.get()) {
                Thread.yield();
                //wait
            }
            return null;
        }));
        pool.submit(() -> {
            while (!firstUpdatePassed.get()) {
                Thread.yield();
                // wait
            }
            try {
                transactionTemplate.execute(status -> {
                    secondTransactionStarted.set(true);
                    try {
                        Coupon localCoupon =
                                couponService.getCouponByCode(of(couponOld.getCode())).orElseThrow(() -> new AssertionError("not found"));
                        OperationContext operationContext = OperationContextFactory.uidOperationContext();
                        operationContextDao.save(operationContext);
                        long transactionId = metaTransactionDao.createEmptyTransaction();
                        boolean result = couponService.useCoupon(localCoupon, promo,
                                ImmutableSet.of(transactionId), String.valueOf(123L), operationContext,
                                discountUtils.getRulesPayload()
                        ).getStatus() != CouponStatus.USED;
                        secondTransactionFailed.set(result);
                        return result;
                    } catch (MarketLoyaltyException e) {
                        secondTransactionFailed.set(true);
                        return true;
                    }
                });
            } catch (Exception e) {
                logger.debug("Ignored error {}", e::toString);
            }
        });

        pool.shutdown();
        long maxWait = 5000L;
        long curWait = 0L;
        while (true) {
            try {
                long waitTime = 100L;
                if (pool.awaitTermination(waitTime, TimeUnit.MILLISECONDS)) {
                    break;
                }
                curWait += waitTime;
                if (curWait > maxWait) {
                    pool.shutdownNow();
                    fail("Too long wait");
                }
            } catch (InterruptedException e) {
                logger.debug("Ignored error {}", e::toString);
            }
        }

        assertTrue(firstTransactionSucceeded.get());
        assertTrue(secondTransactionFailed.get());
    }

    @Test
    public void testCreateOrGetCouponWithoutForceActivationCouponAlreadyExistsAndActive() {
        testCreateOrGetForceActivation(true, false);
    }

    @Test
    public void testCreateOrGetCouponWithForceActivationCouponAlreadyExists() {
        testCreateOrGetForceActivation(false, true);
    }

    @Test
    public void testCreateOrGetCouponWithForceActivation() {
        testCreateOrGetForceActivation(true, true);
    }

    @Test
    public void testCreateOrGetCouponWithoutForceActivation() {
        testCreateOrGetForceActivation(false, false);
    }

    private void testCreateOrGetForceActivation(boolean forceOnCreate, boolean forceOnGet) {
        CouponCreationRequest request = CouponCreationRequest.builder("1", promo.getId())
                .identity(new Uuid("1"))
                .forceActivation(forceOnCreate)
                .build();
        Coupon couponMain = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());
        if (forceOnCreate) {
            assertEquals(CouponStatus.ACTIVE, couponMain.getStatus());
        }

        for (Identity.Type type : Identity.Type.values()) {
            request = CouponCreationRequest.builder("1", promo.getId())
                    .identity(type.buildIdentity("1"))
                    .forceActivation(forceOnGet)
                    .build();
            Coupon coupon = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());
            assertEquals(couponMain.getCode(), coupon.getCode());
            if (forceOnCreate || forceOnGet) {
                assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
            } else {
                assertEquals(CouponStatus.INACTIVE, coupon.getStatus());
            }
        }

        for (int i = 2; i < 10; ++i) {
            request = CouponCreationRequest.builder(Integer.toString(i), promo.getId())
                    .identity(new Uuid("1"))
                    .forceActivation(forceOnCreate)
                    .build();
            Coupon coupon = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());
            assertNotEquals(couponMain.getCode(), coupon.getCode());
            if (forceOnCreate) {
                assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
            }
        }
    }


    @Test
    public void testCreateCouponLeavesHistory() {
        Coupon couponMain = createOrGetCoupon(promo.getId());
        List<CouponHistoryRecord> records = couponHistoryDao.getRecords(couponMain.getId());
        assertEquals(1, records.size());
        assertEquals(couponMain.getId(), records.get(0).getCouponId());
        assertEquals(couponMain.getStatus(), records.get(0).getStatus());
        assertEquals(DiscountHistoryRecordType.CREATION, records.get(0).getRecordType());
    }

    @Test
    public void testCouponActivationWithLowerCase() {
        CouponCreationRequest request = CouponCreationRequest.builder("some-key", promo.getId())
                .identity(new Uuid("1"))
                .build();
        String lowercase = "lowercase";
        Coupon coupon = Coupon.fromRequest(lowercase, request)
                .setCreatedFor(1L)
                .build();
        couponService.createOrGetCoupon(promo, coupon, discountUtils.getRulesPayload());
        couponService.activateCouponsFromInactive(
                Collections.singletonMap(lowercase, "source1")
        );

        assertEquals(CouponStatus.ACTIVE,
                couponDao.getCouponByCode(lowercase).orElseThrow(CouponServiceTest::couponNotExists).getStatus());
    }

    @Test
    public void testActivateCouponLeavesHistory() {
        Coupon couponMain = createOrGetCoupon(promo.getId());
        Coupon coupon = couponService.activateCouponsFromInactive(
                Collections.singletonMap(String.valueOf(couponMain.getCode()), "source1")
        ).get(0);

        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
        assertNotNull(couponService.getCouponByCode(of(coupon.getCode())).orElseThrow(() -> new AssertionError("not " +
                "found")).getActivationTime());

        List<CouponHistoryRecord> records = couponHistoryDao.getRecords(couponMain.getId());
        assertEquals(2, records.size());
        assertEquals(couponMain.getId(), records.get(0).getCouponId());
        assertEquals(CouponStatus.ACTIVE, records.get(0).getStatus());
        assertEquals(DiscountHistoryRecordType.ACTIVATION, records.get(0).getRecordType());
    }

    @Test
    public void createCouponWithEmissionLeavesTransactionIdInHistory() {
        Coupon couponMain = createOrGetCoupon(promo.getId());
        List<CouponHistoryRecord> records = couponHistoryDao.getRecords(couponMain.getId());
        assertEquals(1, records.size());
        assertEquals(DiscountHistoryRecordType.CREATION, records.get(0).getRecordType());
        assertFalse(couponHistoryDao.getTransactions(records.get(0).getId()).isEmpty());
    }

    @Test
    public void doubleActivationOfCoupon() {
        Coupon couponMain = createOrGetCoupon(promo.getId());
        couponService.activateCouponsFromInactive(
                Collections.singletonMap(String.valueOf(couponMain.getCode()), "source1")
        );
        List<CouponHistoryRecord> records = couponHistoryDao.getRecords(couponMain.getId());
        assertEquals(2, records.size());
        assertEquals(DiscountHistoryRecordType.ACTIVATION, records.get(0).getRecordType());

        couponService.activateCouponsFromInactive(
                Collections.singletonMap(String.valueOf(couponMain.getCode()), "source1")
        );
        records = couponHistoryDao.getRecords(couponMain.getId());
        assertEquals(2, records.size());
        assertEquals(DiscountHistoryRecordType.ACTIVATION, records.get(0).getRecordType());
    }

    @Test
    public void emptyRequest() {
        couponService.activateCouponsFromInactive(Collections.emptyMap());
    }

    @Test
    public void notFound() {
        MarketLoyaltyException notFound = assertThrows(MarketLoyaltyException.class, () ->
                couponService.activateCouponsFromInactive(Collections.singletonMap("notExists", ""))
        );
        assertEquals(MarketLoyaltyErrorCode.COUPON_NOT_EXISTS, notFound.getMarketLoyaltyErrorCode());

        notFound = assertThrows(MarketLoyaltyException.class, () ->
                couponService.activateCoupon(of("notExists"), "")
        );
        assertEquals(MarketLoyaltyErrorCode.COUPON_NOT_EXISTS, notFound.getMarketLoyaltyErrorCode());
    }

    @Test
    public void revertCouponAction() {
        Coupon coupon = createOrGetCoupon(promo.getId());
        coupon = couponService.activateCouponsFromInactive(Collections.singletonMap(String.valueOf(coupon.getCode()),
                "source1")).get(0);
        coupon = couponService.getCouponByCode(of(coupon.getCode())).orElseThrow(() -> new AssertionError("not found"));
        assertNotNull(coupon.getActivationTime());


        String revertToken = "revert-token=1";
        OperationContext context1 = operationContextDao.get(
                operationContextDao.save(OperationContextFactory.uidOperationContext())
        );
        long transactionId = metaTransactionDao.createTransaction(revertToken, null, null);
        couponService.useCoupon(coupon, promo, ImmutableSet.of(transactionId), String.valueOf(123L), context1,
                discountUtils.getRulesPayload());
        Coupon couponStored =
                couponDao.getCouponByCode(coupon.getCode()).orElseThrow(CouponServiceTest::couponNotExists);
        assertEquals(CouponStatus.USED, couponStored.getStatus());

        OperationContext context2 = operationContextDao.get(
                operationContextDao.save(OperationContextFactory.yandexUidOperationContext())
        );

        CouponHistoryRecord couponHistoryRecord = couponHistoryDao.getRecordsByTransaction(transactionId).get(0);
        couponService.revertCouponAction(RevertSource.HTTP,
                ImmutableSet.of(metaTransactionDao.createEmptyTransaction()), context2.getId(), couponHistoryRecord
        );
        couponStored = couponDao.getCouponByCode(coupon.getCode()).orElseThrow(CouponServiceTest::couponNotExists);
        assertEquals(CouponStatus.ACTIVE, couponStored.getStatus());

        List<CouponHistoryRecord> records = couponHistoryDao.getRecords(coupon.getId());
        assertEquals(4, records.size());
        assertEquals(DiscountHistoryRecordType.USAGE, records.get(1).getRecordType());
        assertEquals(DiscountHistoryRecordType.REVERSION, records.get(0).getRecordType());
    }

    @Test
    public void activateAfterRevert() {
        Coupon coupon = createOrGetCoupon(promo.getId());
        coupon = couponService.activateCouponsFromInactive(Collections.singletonMap(String.valueOf(coupon.getCode()),
                "source1")).get(0);
        coupon = couponService.getCouponByCode(of(coupon.getCode())).orElseThrow(() -> new AssertionError("not found"));

        String revertToken = "revert-token=1";
        OperationContext context1 = operationContextDao.get(
                operationContextDao.save(OperationContextFactory.uidOperationContext())
        );
        long transactionId = metaTransactionDao.createTransaction(revertToken, null, null);
        couponService.useCoupon(coupon, promo, ImmutableSet.of(transactionId), String.valueOf(123L), context1,
                discountUtils.getRulesPayload());

        OperationContext context2 = operationContextDao.get(
                operationContextDao.save(OperationContextFactory.yandexUidOperationContext())
        );
        CouponHistoryRecord couponHistoryRecord = couponHistoryDao.getRecordsByTransaction(transactionId).get(0);
        couponService.revertCouponAction(RevertSource.HTTP,
                ImmutableSet.of(metaTransactionDao.createEmptyTransaction()), context2.getId(), couponHistoryRecord
        );
        List<CouponHistoryRecord> records = couponHistoryDao.getRecords(coupon.getId());
        assertEquals(4, records.size());

        couponService.activateCouponsFromInactive(Collections.singletonMap(String.valueOf(coupon.getCode()), "source1"
        ));
        records = couponHistoryDao.getRecords(coupon.getId());
        assertEquals(4, records.size());
    }

    @Test
    public void shouldEmitCouponPieceAccountMatter() {
        Promo promo = promoManager
                .createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                        .setEmissionBudget(BigDecimal.valueOf(100)));
        CouponCreationRequest request = CouponCreationRequest.builder("coupon", promo.getId())
                .identity(new Uid(1L))
                .forceActivation(true)
                .build();

        Coupon coupon = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());

        promo = promoService.getPromo(promo.getId());
        assertThat(coupon, hasProperty("sourceKey", equalTo("coupon")));
        assertThat(promo.getCurrentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(99)));
    }

    private Coupon createOrGetCoupon(Long promoId) {
        CouponCreationRequest request = CouponCreationRequest.builder("1", promoId)
                .identity(new Uuid("1"))
                .build();
        return couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());
    }

    public static AssertionError couponNotExists() {
        return new AssertionError("Coupon does not exist");
    }
}
