package ru.yandex.market.logistics.dbqueue.producer;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.dbqueue.base.AbstractContextualTest;
import ru.yandex.market.logistics.dbqueue.base.BaseDbqueueProducer;
import ru.yandex.market.logistics.dbqueue.impl.DbQueueTaskType;
import ru.yandex.market.logistics.dbqueue.impl.TestPayload;
import ru.yandex.market.logistics.dbqueue.registry.DbQueueConfigRegistry;
import ru.yandex.market.logistics.dbqueue.service.DbQueueLogService;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.config.QueueShard;


@DbUnitConfiguration(databaseConnection = "dbqueueDatabaseConnection")
public class ProducerTest extends AbstractContextualTest {

    private static final String PAYLOAD_STRING = "{\"bookingIds\":[2,1]}";

    @Autowired
    private DbQueueConfigRegistry queueRegister;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DbQueueLogService dbQueueLogService;

    @Autowired
    private QueueShard queueShard;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/dbqueue/empty.xml", connection = "dbqueueDatabaseConnection")
    public void serialize() {
        TestPayload payload = new TestPayload(Arrays.asList(2L, 1L));

        String payloadString = new BaseDbqueueProducer<TestPayload>(queueShard, queueRegister, objectMapper,
                DbQueueTaskType.TEST_QUOTA, dbQueueLogService) {
        }.getPayloadTransformer().fromObject(payload);
        assertions().assertThat(payloadString).isEqualTo(PAYLOAD_STRING);
    }


    @Test
    @DatabaseSetup(value = "classpath:fixtures/dbqueue/producer/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/dbqueue/producer/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection")
    public void enqueue() {
        TestPayload payload = new TestPayload(Arrays.asList(2L, 1L));
        new BaseDbqueueProducer<TestPayload>(queueShard, queueRegister, objectMapper,
                DbQueueTaskType.TEST_QUOTA, dbQueueLogService) {
        }.enqueue(EnqueueParams.create(payload));
    }
}


