package ru.yandex.market.fulfillment.stockstorage.service.queue;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.ExecutionQueueItem;
import ru.yandex.market.fulfillment.stockstorage.service.execution.ExecutionQueueType;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.WarehouseAwareExecutionQueuePayload;

public class ExecutionQueueServiceTest extends AbstractContextualTest {

    @Autowired
    private ExecutionQueueService executionQueueService;

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_service/push_without_config.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue_service/push_without_config.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pushWithoutConfig() {
        executionQueueService.push(createItem());
    }

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_service/push_without_queue_state.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue_service/push_without_queue_state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pushWithoutQueueState() {
        executionQueueService.push(createItem());
    }

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_service/" +
            "push_with_null_for_number_of_tasks_in_queue_to_add_minute_config.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue_service/" +
            "push_with_null_for_number_of_tasks_in_queue_to_add_minute_config.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pushWithNullForNumberOfTasksInQueueToAddMinuteConfig() {
        executionQueueService.push(createItem());
    }

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_service/" +
            "push_with_adding_nothing.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue_service/" +
            "push_with_adding_nothing.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pushWithAddingNothing() {
        executionQueueService.push(createItem());
    }

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_service/" +
            "push_with_applying_minutes_to_add_config.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/execution_queue_service/" +
            "push_with_applying_minutes_to_add_config.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void pushWithApplyingMinutesToAddConfig() {
        executionQueueService.push(createItem());
    }

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_service/pull_without_config.xml"})
    public void pullWithoutConfig() {
        assertions().assertThat(executionQueueService.pull(ExecutionQueueType.FULL_SYNC_STOCK)).isNotEmpty();
    }

    @Test
    @DatabaseSetup(value = {"classpath:database/states/execution_queue_service/pull_with_config.xml"})
    public void pullWithConfig() {
        assertions().assertThat(executionQueueService.pull(ExecutionQueueType.FULL_SYNC_STOCK)).isNotEmpty();
    }

    private Collection<ExecutionQueueItem<?>> createItem() {
        return Collections.singleton(
                ExecutionQueueItem.of(
                        LocalDateTime.of(2021, 10, 21, 10, 0),
                        LocalDateTime.of(2071, 10, 21, 10, 0),
                        ExecutionQueueType.FULL_SYNC_STOCK,
                        new WarehouseAwareExecutionQueuePayload(10L, 25L, 15, false, 172)
                )
        );
    }
}
