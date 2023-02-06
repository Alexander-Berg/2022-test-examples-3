package ru.yandex.market.mbi.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbi.FunctionalTest;

@Timeout(10)
class DbmsLockServiceH2Test extends FunctionalTest implements DbmsLockServiceTestBase {
    DbmsLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new DbmsLockService(jdbcTemplate);
    }

    @Override
    public DbmsLockService lockService() {
        return lockService;
    }

    @Override
    public TransactionTemplate transactionTemplate() {
        return transactionTemplate;
    }
}



