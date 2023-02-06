package ru.yandex.market.loyalty.core.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.core.dao.accounting.MetaTransactionDao;
import ru.yandex.market.loyalty.core.dao.accounting.OperationContextDao;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 14.06.17
 */
public class MetaTransactionDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private MetaTransactionDao metaTransactionDao;
    @Autowired
    private OperationContextDao operationContextDao;

    @Test
    public void doubleCommit() {
        long transactionId = metaTransactionDao.createEmptyTransaction();
        assertTrue(metaTransactionDao.commitTransaction(transactionId, new Date()));
        assertFalse(metaTransactionDao.commitTransaction(transactionId, new Date()));
    }

    @Test(expected = MarketLoyaltyException.class)
    public void doubleRevert() {
        long transactionId = metaTransactionDao.createEmptyTransaction();
        metaTransactionDao.revertTransaction(transactionId, new Date());
        metaTransactionDao.revertTransaction(transactionId, new Date());
    }
}
