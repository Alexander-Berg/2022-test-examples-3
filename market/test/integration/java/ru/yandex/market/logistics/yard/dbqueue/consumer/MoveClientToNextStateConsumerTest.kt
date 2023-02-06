package ru.yandex.market.logistics.yard.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStateConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStatePayload
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.ZonedDateTime

@Import(MoveClientToNextStateConsumer::class)
class MoveClientToNextStateConsumerTest(
    @Autowired private val processClientQueueConsumer: MoveClientToNextStateConsumer,
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock
) : AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/process-client-queue/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/process-client-queue/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun consume() {
        processClientQueueConsumer.execute(
            Task.builder<MoveClientToNextStatePayload>(queueShard.shardId)
                .withCreatedAt(ZonedDateTime.now(clock))
                .withPayload(MoveClientToNextStatePayload(0, 1)).build()
        )
    }
}

