package ru.yandex.market.logistics.dbqueue.consumer;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.dbqueue.base.AbstractContextualTest;
import ru.yandex.market.logistics.dbqueue.base.BaseDbQueueConsumer;
import ru.yandex.market.logistics.dbqueue.impl.DbQueueTaskType;
import ru.yandex.market.logistics.dbqueue.impl.TestPayload;
import ru.yandex.market.logistics.dbqueue.registry.DbQueueConfigRegistry;
import ru.yandex.market.logistics.dbqueue.service.DbQueueLogService;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

@DbUnitConfiguration(databaseConnection = "dbqueueDatabaseConnection")
class ConsumerTest extends AbstractContextualTest {

    @Autowired
    private DbQueueConfigRegistry dbqueueConfigRegistry;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DbQueueLogService queueLogService;

    @Test
    @DatabaseSetup("classpath:fixtures/dbqueue/empty.xml")
    @ExpectedDatabase(value = "classpath:fixtures/dbqueue/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection")
    void dbqueueConsumerRequestSuccess() {
        TestPayload payload = new TestPayload(Arrays.asList(2L, 1L));
        Task<TestPayload> task = Task.<TestPayload>builder(
                new QueueShardId("test")).withPayload(payload).build();
        new BaseDbQueueConsumer<TestPayload>(dbqueueConfigRegistry, objectMapper,
                DbQueueTaskType.TEST_QUOTA, queueLogService) {
            @Override
            protected void executeInner(Task<TestPayload> task) {
            }
        }.execute(task);
    }
}
