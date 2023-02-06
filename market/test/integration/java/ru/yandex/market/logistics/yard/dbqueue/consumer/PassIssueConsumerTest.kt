package ru.yandex.market.logistics.yard.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.issue_pass.PassIssueConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.issue_pass.PassIssuePayload
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.ZonedDateTime

@Import(PassIssueConsumer::class)
class PassIssueConsumerTest(
    @Autowired private val passIssueConsumer: PassIssueConsumer,
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock,
) : AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/pass-issue/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/pass-issue/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun consume() {


        passIssueConsumer.execute(
            Task.builder<PassIssuePayload>(queueShard.shardId)
                .withCreatedAt(ZonedDateTime.now(clock))
                .withPayload(PassIssuePayload(1, null, 10)).build()
        )
    }
}

