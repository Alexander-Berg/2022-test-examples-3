package ru.yandex.market.fulfillment.stockstorage.service.jobs;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;

public class ExecutionQueueStateUpdateJobTest extends AbstractContextualTest {

    @Autowired
    private ExecutionQueueStateUpdateJob executionQueueStateUpdateJob;

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_state_update/setup_for_update.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue_state_update/after_update.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void successfulUpdate() {
        executionQueueStateUpdateJob.trigger();
    }

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_state_update/setup_for_insert.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue_state_update/after_insert.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void successfulInsert() {
        executionQueueStateUpdateJob.trigger();
    }
}
