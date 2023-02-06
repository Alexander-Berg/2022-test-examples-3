package ru.yandex.market.core.lock;

import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.lock.DbmsLockService;
import ru.yandex.market.mbi.lock.DbmsLockServiceTestBase;

@Timeout(10)
class DbmsLockServiceTest extends FunctionalTest implements DbmsLockServiceTestBase {
    @Autowired
    DbmsLockService lockService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Override
    public DbmsLockService lockService() {
        return lockService;
    }

    @Override
    public TransactionTemplate transactionTemplate() {
        return transactionTemplate;
    }
}

