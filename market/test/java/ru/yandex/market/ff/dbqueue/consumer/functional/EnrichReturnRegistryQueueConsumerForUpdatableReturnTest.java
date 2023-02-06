package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.EnrichReturnRegistryQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.EnrichReturnRegistryPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;

public class EnrichReturnRegistryQueueConsumerForUpdatableReturnTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private EnrichReturnRegistryQueueConsumer consumer;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("classpath:service/returns/before-enrich-updatable.xml")
    @ExpectedDatabase(
            value = "classpath:service/returns/after-enrich-updatable.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldSuccessfullyEnrich() {
        TaskExecutionResult taskExecutionResult = executeTask(2L);
        assertThat(taskExecutionResult.getActionType()).isEqualTo(TaskExecutionResult.Type.FINISH);
    }

    private TaskExecutionResult executeTask(long requestId) {
        var payload = new EnrichReturnRegistryPayload(requestId);
        var task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        return transactionTemplate.execute(status -> consumer.execute(task));
    }
}
