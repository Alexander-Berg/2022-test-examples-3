package ru.yandex.market.logistics.yard.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils
import ru.yandex.market.logistics.yard_v2.dbqueue.handle_yard_client_state_change.HandleYardClientStateChangeConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.handle_yard_client_state_change.HandleYardClientStateChangePayload
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsPayload
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClientStateHistoryEntity
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Import(UnparsedLogbrokerEventsConsumer::class)
class HandleYardClientStateChangeConsumerTest(
    @Autowired private val consumer: HandleYardClientStateChangeConsumer,
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock,
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/handle-client-state-change/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/handle-client-state-change/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun consume() {

        consumer.execute(
            Task.builder<HandleYardClientStateChangePayload>(queueShard.shardId)
                .withCreatedAt(ZonedDateTime.now(clock))
                .withPayload(
                    HandleYardClientStateChangePayload(
                        0,
                        YardClientStateHistoryEntity(1, 0, 1000, LocalDateTime.of(2022, 2, 2, 2, 2)),
                        YardClientStateHistoryEntity(2, 0, 1001, LocalDateTime.of(2022, 2, 2, 2, 3))
                    )
                )
                .build()
        )
    }
}

