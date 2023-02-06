package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.StartReturnProcessingQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.StartReturnProcessingPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class StartReturnProcessingQueueConsumerIntegrationTest extends IntegrationTestWithDbQueueConsumers {
    public static final Set<String> ORDER_IDS = Set.of("1", "2");

    @Autowired
    StartReturnProcessingQueueConsumer consumer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void startReturnProcessingTaskStartedAndFinished() {
        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(lomClient).startReturnOrdersProcessing(ORDER_IDS);
    }

    private TaskExecutionResult executeTask() {
        StartReturnProcessingPayload payload = new StartReturnProcessingPayload(ORDER_IDS);
        Task<StartReturnProcessingPayload> task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        return transactionTemplate.execute(status -> consumer.execute(task));
    }
}
