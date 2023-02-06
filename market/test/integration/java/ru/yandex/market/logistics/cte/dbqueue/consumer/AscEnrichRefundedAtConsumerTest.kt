package ru.yandex.market.logistics.cte.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi
import ru.yandex.market.checkout.checkouter.returns.Return
import ru.yandex.market.checkout.checkouter.returns.ReturnItem
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.AscEnrichmentPayload
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Date

internal class AscEnrichRefundedAtConsumerTest(
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock,
    @Autowired private val consumer: AscEnrichRefundedAtConsumer,
    @Qualifier("checkouterApi") @Autowired private val checkouterClient: CheckouterAPI
) : IntegrationTest() {

    val checkouterReturnApi: CheckouterReturnApi = Mockito.mock(CheckouterReturnApi::class.java)

    @Test
    @DatabaseSetup(
        value = ["classpath:/dbqueue/consumer/asc-enrich-refunded-at/before.xml"]
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/asc-enrich-refunded-at/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldEnrichRefundedAtField() {
        val returnItem = ReturnItem()
        returnItem.returnReason = "Экран разбит"
        val theReturn = Return()
        theReturn.creationDate = Date.from(Instant.parse("2011-11-11T07:11:11Z"))
        theReturn.items = listOf(returnItem)
        whenever(checkouterClient.returns()).thenReturn(checkouterReturnApi)
        whenever(checkouterReturnApi.getReturn(any(), any())).thenReturn(theReturn)
        val task = Task.builder<AscEnrichmentPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(AscEnrichmentPayload(listOf(1L, 2L, 3L), 1L))
            .build()
        consumer.execute(task)
    }
}

