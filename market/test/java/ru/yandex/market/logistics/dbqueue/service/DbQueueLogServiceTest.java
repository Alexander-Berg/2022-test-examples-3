package ru.yandex.market.logistics.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.dbqueue.base.AbstractContextualTest;

@DbUnitConfiguration(databaseConnection = "dbqueueDatabaseConnection")
public class DbQueueLogServiceTest extends AbstractContextualTest {

    @Autowired
    public DbQueueLogService logService;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/dbqueue/offset-delete/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/dbqueue/offset-delete/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void deleteOldRowsOffset() {
        logService.cleanOldRows();
    }


    @Test
    @DatabaseSetup(value = "classpath:fixtures/dbqueue/time-delete/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/dbqueue/time-delete/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void deleteOldRowsTime() {
        logService.cleanOldRows();
    }


}
