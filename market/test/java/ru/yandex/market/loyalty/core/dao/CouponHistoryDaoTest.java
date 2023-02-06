package ru.yandex.market.loyalty.core.dao;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.OperationContextDao;
import ru.yandex.market.loyalty.core.dao.coupon.CouponDao;
import ru.yandex.market.loyalty.core.dao.coupon.CouponHistoryDao;
import ru.yandex.market.loyalty.core.model.OperationContext;
import ru.yandex.market.loyalty.core.model.accounting.MetaTransaction;
import ru.yandex.market.loyalty.core.model.coupon.CouponHistoryRecord;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.dao.CouponDaoTest.coupon;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 22.05.17
 */
public class CouponHistoryDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long UID = 1L;

    @Autowired
    private IdentityDao identityDao;
    @Autowired
    private CouponDao couponDao;
    @Autowired
    private OperationContextDao operationContextDao;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CouponHistoryDao couponHistoryDao;
    @Autowired
    private MetaTransactionDao metaTransactionDao;


    private Long identityId = -1L;
    private Promo promo;

    @Before
    public void init() {
        Optional.ofNullable(identityDao.createIfNecessaryUserIdentity(new Uid(UID))).ifPresent(id -> identityId = id);
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void addRecordWithoutCoupon() {
        couponHistoryDao.addRecord(new CouponHistoryRecord(0L, 1L, DiscountHistoryRecordType.CREATION,
                CouponStatus.INACTIVE, "source1", new Date(), null), (Long) null);
    }

    @Test
    public void addRecord() {
        long couponId = couponDao.tryInsertAndGetCoupon(coupon(identityId, "key1", "code1", promo.getId())).getId();
        long couponId2 = couponDao.tryInsertAndGetCoupon(coupon(identityId, "key2", "code2", promo.getId())).getId();

        long contextId = operationContextDao.save(OperationContext.empty());

        List<CouponHistoryRecord> expectedRecords = Arrays.asList(
                new CouponHistoryRecord(0L, couponId, DiscountHistoryRecordType.CREATION,
                        CouponStatus.INACTIVE, "source1", new Date(), contextId),
                new CouponHistoryRecord(0L, couponId, DiscountHistoryRecordType.ACTIVATION,
                        CouponStatus.ACTIVE, "source3", new Date(), contextId),
                new CouponHistoryRecord(0L, couponId, DiscountHistoryRecordType.USAGE,
                        CouponStatus.USED, "source4", new Date(), contextId),
                new CouponHistoryRecord(0L, couponId2, DiscountHistoryRecordType.REVERSION,
                        CouponStatus.ACTIVE, "source5", new Date(), contextId)
        );
        couponHistoryDao.addRecord(expectedRecords.get(0), (Long) null);
        couponHistoryDao.addRecords(expectedRecords.subList(1, expectedRecords.size())
                .stream()
                .map(r -> Pair.of(r, r.getRecordType() == DiscountHistoryRecordType.USAGE ?
                        ImmutableSet.of(metaTransactionDao.createEmptyTransaction()) : Collections.<Long>emptySet()))
                .collect(Collectors.toList()));
        List<CouponHistoryRecord> actualRecords = couponHistoryDao.getRecords(couponId);
        for (int i = 0; i < expectedRecords.size() - 1; i++) {
            assertThat(expectedRecords.get(i), samePropertyValuesAs(actualRecords.get(actualRecords.size() - i - 1),
                    "id", "creationTime", "modificationTime", "transactionId"));
        }
    }

    @Test
    public void getRecordByToken() {
        long couponId = couponDao.tryInsertAndGetCoupon(coupon(identityId, "key1", "code1", promo.getId())).getId();

        String revertToken = "revert";
        long contextId = operationContextDao.save(OperationContext.empty());
        long transactionId = metaTransactionDao.createTransaction(revertToken, null, null);
        CouponHistoryRecord record = new CouponHistoryRecord(0L, couponId, DiscountHistoryRecordType.CREATION,
                CouponStatus.INACTIVE, "source1", new Date(), contextId);
        couponHistoryDao.addRecord(record, transactionId);
        CouponHistoryRecord actualRecord = couponHistoryDao.getRecord(revertToken);
        assertThat(record, samePropertyValuesAs(actualRecord, "id", "creationTime", "transactionId"));
    }

    @Test
    public void testGetEmptyLastRevertedRecords() {
        List<MetaTransaction> lastRevertedRecords = metaTransactionDao.getTransactionsToRevert(0L);

        assertEquals(0, lastRevertedRecords.size());
    }

    @Test
    public void testGetEmptyLastUsageRecords() {
        List<MetaTransaction> lastUsageRecords = metaTransactionDao.getTransactionsToCommit(0L);

        assertEquals(0, lastUsageRecords.size());
    }

}
