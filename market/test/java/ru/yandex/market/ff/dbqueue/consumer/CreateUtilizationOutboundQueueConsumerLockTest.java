package ru.yandex.market.ff.dbqueue.consumer;

import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.model.dbqueue.CreateUtilizationOutboundsPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class CreateUtilizationOutboundQueueConsumerLockTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private CreateUtilizationOutboundsQueueConsumer createUtilizationOutboundsQueueConsumer;

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/create-utilization-outbounds/before-with-lock.xml")
    @ExpectedDatabase(
            value = "classpath:db-queue/consumer/create-utilization-outbounds/before-with-lock.xml",
            assertionMode = NON_STRICT)
    public void testExceptionWhenLockAlreadyTaken() {
        createUtilizationOutboundsQueueConsumer.execute(new Task<>(
                new QueueShardId("shard"),
                new CreateUtilizationOutboundsPayload(2),
                0,
                ZonedDateTime.now(),
                "trace",
                "actor"
        ));
    }
}
