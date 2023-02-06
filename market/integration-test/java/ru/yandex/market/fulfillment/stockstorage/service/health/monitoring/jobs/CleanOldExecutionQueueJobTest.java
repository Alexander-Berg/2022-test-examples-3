package ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;


public class CleanOldExecutionQueueJobTest extends AbstractContextualTest {

    @Autowired
    private CleanOldExecutionQueueJob cleanOldExecutionQueueJob;

    @Test
    @DatabaseSetup(value = {"classpath:database/expected/clean_old_execution_queue/setup.xml",
            "classpath:database/expected/clean_old_execution_queue/system_property.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/clean_old_execution_queue/expected.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testCleaningOldTasks() {
        cleanOldExecutionQueueJob.trigger();
    }
}
