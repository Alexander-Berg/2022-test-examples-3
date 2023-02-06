package ru.yandex.market.logistics.yard.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsPayload
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.ZonedDateTime

@Import(UnparsedLogbrokerEventsConsumer::class)
class UnparsedLogbrokerEventsConsumerTest(
    @Autowired private val consumer: UnparsedLogbrokerEventsConsumer,
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock,
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/unprocessed_events/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/unprocessed_events/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun consume() {

        val line =
            FileContentUtils.getFileContent("classpath:fixtures/dbqueue/consumer/unprocessed_events/payload.json")

        consumer.execute(
            Task.builder<UnparsedLogbrokerEventsPayload>(queueShard.shardId)
                .withCreatedAt(ZonedDateTime.now(clock))
                .withPayload(UnparsedLogbrokerEventsPayload(entity = "client_event", line = line)).build()
        )
    }
}

