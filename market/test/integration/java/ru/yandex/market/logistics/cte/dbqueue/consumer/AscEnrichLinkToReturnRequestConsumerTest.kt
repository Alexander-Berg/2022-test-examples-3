package ru.yandex.market.logistics.cte.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.AscEnrichmentPayload
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import ru.yandex.startrek.client.Session
import java.time.Clock
import java.time.ZonedDateTime

class AscEnrichLinkToReturnRequestConsumerTest(
        @Autowired private val ascEnrichLinkToReturnRequestConsumer: AscEnrichLinkToReturnRequestConsumer,
        @Autowired private val queueShard: QueueShard,
        @Autowired private val clock: Clock,
        @Autowired private val session: Session
) : IntegrationTest() {

    @BeforeEach
    fun init() {
        Mockito.reset(session)
    }

    @Test
    @DatabaseSetup(
            value = ["classpath:/dbqueue/consumer/asc-enrich-link-to-return-request-consumer/before.xml"]
    )
    @ExpectedDatabase(
            value = "classpath:dbqueue/consumer/asc-enrich-link-to-return-request-consumer/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldSuccessfullyEnrichLinkToReturnRequest() {

        val task = Task.builder<AscEnrichmentPayload>(queueShard.shardId)
                .withCreatedAt(ZonedDateTime.now(clock))
                .withPayload(AscEnrichmentPayload(listOf(1L, 2L, 3L), 1L))
                .build()
        ascEnrichLinkToReturnRequestConsumer.execute(task)
    }
}
