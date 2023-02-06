package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.SendMbiNotificationQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class SendMbiNotificationQueueConsumerIntegrationTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    SendMbiNotificationQueueConsumer consumer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void sendMbiNotificationTaskStartedAndFinished() {
        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(mbiApiClient).sendMessageToSupplier(1L, 1, "1");
    }

    private TaskExecutionResult executeTask() {
        SendMbiNotificationPayload payload = new SendMbiNotificationPayload(1L, 1, "1");
        Task<SendMbiNotificationPayload> task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        return transactionTemplate.execute(status -> consumer.execute(task));
    }
}
