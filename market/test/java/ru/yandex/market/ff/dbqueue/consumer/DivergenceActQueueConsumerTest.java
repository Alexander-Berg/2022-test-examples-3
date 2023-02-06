package ru.yandex.market.ff.dbqueue.consumer;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.service.DivergenceActProcessingService;
import ru.yandex.market.ff.model.dbqueue.DivergenceActPayload;
import ru.yandex.market.ff.service.DbQueueLogService;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class DivergenceActQueueConsumerTest extends IntegrationTestWithDbQueueConsumers {

    private static final String PAYLOAD_STRING = "{\"requestId\":123}";

    @Autowired
    private DivergenceActQueueConsumer divergenceActQueueConsumer;

    @Autowired
    private DbQueueLogService dbQueueLogService;

    @Autowired
    @Qualifier("requiresNewTransactionTemplate")
    private TransactionTemplate transactionTemplate;

    @Test
    public void testDeserializationWorks() {
        DivergenceActPayload payload = divergenceActQueueConsumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        assertions.assertThat(payload).isNotNull();
        assertions.assertThat(payload.getEntityId()).isEqualTo(123);
    }

    @Test
    public void testMaxAttemptsExceeded() {
        DivergenceActPayload payload = divergenceActQueueConsumer.getPayloadTransformer()
                .toObject(PAYLOAD_STRING);
        DivergenceActProcessingService service = mock(DivergenceActProcessingService.class);
        DivergenceActQueueConsumer consumer =
                new DivergenceActQueueConsumer(service, dbQueueLogService, transactionTemplate, null);
        doThrow(new RuntimeException()).when(service).processPayload(any(DivergenceActPayload.class));

        TaskExecutionResult resultToRetry = consumer.execute(new Task<>(
                new QueueShardId("id"), payload, 19, ZonedDateTime.now(), "info", "actor"));
        TaskExecutionResult resultToFinish = consumer.execute(new Task<>(
                new QueueShardId("id"), payload, 20, ZonedDateTime.now(), "info", "actor"));

        assertions.assertThat(resultToRetry.getActionType()).isEqualByComparingTo(TaskExecutionResult.Type.FAIL);
        assertions.assertThat(resultToFinish.getActionType()).isEqualByComparingTo(TaskExecutionResult.Type.REENQUEUE);
    }
}
