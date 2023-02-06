package ru.yandex.market.loyalty.core.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.loyalty.core.dao.coupon.CodeDao;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CodeDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private CodeDao codeDao;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void shouldResolveConflictWithoutTransactionAbort() {
        transactionTemplate.execute(status -> {
            assertTrue(codeDao.tryInsert("first"));
            assertFalse(codeDao.tryInsert("first"));
            assertTrue(codeDao.tryInsert("second"));
            return null;
        });
    }
}
