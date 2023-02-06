package ru.yandex.market.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.tms.quartz2.model.Executor;

class CleanOldDataConfigTest extends FunctionalTest {
    @Autowired
    @Qualifier("cleanupActionExecutor")
    private Executor job;

    @Test
    @DbUnitDataSet(
            before = "CleanOldDataConfigTest.cleanupActionExecutorTest.before.csv",
            after = "CleanOldDataConfigTest.cleanupActionExecutorTest.after.csv"
    )
    void cleanupActionExecutorTest() {
        job.doJob(null);
    }
}
