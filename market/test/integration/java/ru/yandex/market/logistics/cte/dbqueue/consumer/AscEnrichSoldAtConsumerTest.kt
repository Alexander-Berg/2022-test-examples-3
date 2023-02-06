package ru.yandex.market.logistics.cte.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI
import ru.yandex.market.checkout.checkouter.order.Order
import ru.yandex.market.checkout.checkouter.order.PagedOrders
import ru.yandex.market.checkout.common.rest.Pager
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.AscEnrichmentPayload
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Date

internal class AscEnrichSoldAtConsumerTest(
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock,
    @Autowired private val consumer: AscEnrichSoldAtConsumer,
    @Qualifier("checkouterApi") @Autowired private val checkouterClient: CheckouterAPI
) : IntegrationTest() {

    @Test
    @DatabaseSetup(
        value = ["classpath:/dbqueue/consumer/asc-enrich-sold-at/before.xml"]
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/asc-enrich-sold-at/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldEnrichSoldAtField() {
        val orders = listOf(
            Order().apply {
                id = 111
                creationDate = Date.from(Instant.parse("2011-11-11T07:11:11Z"))
            }, Order().apply {
                id = 333
                creationDate = Date.from(Instant.parse("2011-11-11T07:11:11Z"))
            })
        whenever(checkouterClient.getOrders(any(), any())).thenReturn(PagedOrders(orders, Pager()))
        val task = Task.builder<AscEnrichmentPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(AscEnrichmentPayload(listOf(1L, 2L, 3L), 1L))
            .build()
        consumer.execute(task)
    }
}
