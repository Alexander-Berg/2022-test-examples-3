package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.ValidateCommonRequestQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;

public class ValidateRegistryQueueConsumerForUpdatableReturnTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private ValidateCommonRequestQueueConsumer consumer;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/validate-updating-return-request/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/validate-updating-return-request/after.xml",
            assertionMode = NON_STRICT)
    void shouldSuccessfullyEnrich() {
        TaskExecutionResult taskExecutionResult = executeTask(2L);
        assertThat(taskExecutionResult.getActionType()).isEqualTo(TaskExecutionResult.Type.FINISH);
    }

    private TaskExecutionResult executeTask(long requestId) {
        var payload = new ValidateRequestPayload(requestId);
        var task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        return transactionTemplate.execute(status -> consumer.execute(task));
    }
}
