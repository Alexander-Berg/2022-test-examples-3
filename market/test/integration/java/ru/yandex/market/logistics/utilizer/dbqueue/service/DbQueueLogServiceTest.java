package ru.yandex.market.logistics.utilizer.dbqueue.service;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.DbqueueContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.service.DbQueueLogService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;


public class DbQueueLogServiceTest extends DbqueueContextualTest {

    @Autowired
    private DbQueueLogService dbQueueLogService;

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/max-number-clean-properties.xml"),
            @DatabaseSetup(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/before-clean.xml",
                    connection = "dbqueueDatabaseConnection")
    })
    @ExpectedDatabase(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/after-clean.xml",
            assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    public void whenMaxNumberDeclaredOnlyCorrectRowsWereDeleted() {
        dbQueueLogService.cleanOldRows();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/max-number-no-clean-properties.xml"),
            @DatabaseSetup(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/before-no-clean.xml",
                    connection = "dbqueueDatabaseConnection")
    })
    @ExpectedDatabase(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/before-no-clean.xml",
            assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    public void whenMaxNumberDeclaredThenNothingWasDeletedIfShouldNot() {
        dbQueueLogService.cleanOldRows();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/time-limit-clean-properties.xml"),
            @DatabaseSetup(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/before-clean.xml",
                    connection = "dbqueueDatabaseConnection")
    })
    @ExpectedDatabase(value = "classpath:fixtures/tms/clean-old-rows-in-queue-log/after-clean.xml",
            assertionMode = NON_STRICT, connection = "dbqueueDatabaseConnection")
    public void whenTimeLimitDeclaredThenOnlyCorrectRowsWereDeleted() {

        dbQueueLogService.cleanOldRows();
    }
}
