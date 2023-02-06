package ru.yandex.market.logistics.yard.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStateConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStatePayload
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.ZonedDateTime

@Import(RefreshClientStateConsumer::class)
class RefreshClientStateConsumerTest(
    @Autowired val refreshClientStateConsumer: RefreshClientStateConsumer,
    @Autowired val dbqueueShard: QueueShard,
    @Autowired val clock: Clock
) : AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/refresh-client-state/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/consumer/refresh-client-state/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun consume() {
        refreshClientStateConsumer.execute(
            Task.builder<RefreshClientStatePayload>(dbqueueShard.shardId)
                .withCreatedAt(ZonedDateTime.now(clock))
                .withPayload(RefreshClientStatePayload(0)).build()
        )
    }
}

