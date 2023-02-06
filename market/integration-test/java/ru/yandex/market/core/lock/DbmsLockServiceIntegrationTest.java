package ru.yandex.market.core.lock;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.config.DevIntegrationTest;
import ru.yandex.market.mbi.lock.DbmsLockService;
import ru.yandex.market.mbi.lock.DbmsLockServiceTestBase;

@Deprecated(forRemoval = true, since = "можно дропнуть после переезда с оракла на пг")
class DbmsLockServiceIntegrationTest extends DevIntegrationTest implements DbmsLockServiceTestBase {
    DbmsLockService lockService;

    @Autowired
    TransactionTemplate transactionTemplate;

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
