package ru.yandex.market.mbi.orderservice.tms.service.command

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.commandInvocationTest
import ru.yandex.market.mbi.orderservice.common.dsl.OrderTransitionGraph
import ru.yandex.market.mbi.orderservice.common.model.yt.CreatedAtIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSummaryExtended
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSummaryExtendedKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CreatedAtIndexRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderSummaryExtendedRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Instant

@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        CreatedAtIndex::class,
        OrderSummaryExtended::class
    ]
)
internal class FillOrderSummaryWithHistoricalEventsCommandTest : FunctionalTest() {

    @Autowired
    lateinit var ytOrderRepository: YtOrderRepository

    @Autowired
    lateinit var orderSummaryExtendedRepository: OrderSummaryExtendedRepository

    @Autowired
    lateinit var orderTransitionGraph: OrderTransitionGraph

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var createdAtIndexRepository: CreatedAtIndexRepository

    @BeforeEach
    internal fun setUp() {
        val orders = this::class.loadTestEntities<OrderEntity>("entities/summary-orders.json")
        val ordersLines = this::class.loadTestEntities<OrderLineEntity>("entities/summary-order-lines.json")
        orderEntityRepository.insertRows(orders)
        orderLineEntityRepository.insertRows(ordersLines)
        createdAtIndexRepository.insertRows(
            orders.map {
                CreatedAtIndex(
                    tableKey = CreatedAtIndex.Key(
                        createdAt = it.createdAt!!,
                        partnerId = it.key.partnerId,
                        orderId = it.key.orderId
                    )
                )
            }
        )
        orderSummaryExtendedRepository.insertRow(
            OrderSummaryExtended(
                OrderSummaryExtendedKey(
                    543900,
                    Instant.parse("2021-12-10T11:00:00Z")
                ),
                100500, 100500, 100500, 100500
            )
        )
        orderSummaryExtendedRepository.insertRow(
            OrderSummaryExtended(
                OrderSummaryExtendedKey(
                    543900,
                    Instant.parse("2021-12-09T11:00:00Z")
                ),
                100500, 100500, 100500, 100500
            )
        )
    }

    @Test
    fun commandTest() {
        val command =
            FillOrderSummaryWithHistoricalEventsCommand(
                ytOrderRepository,
                orderSummaryExtendedRepository,
                orderTransitionGraph
            )

        val fromTimestamp = Instant.parse("2021-12-09T00:00:30Z")
        val toTimestamp = Instant.parse("2021-12-12T23:00:30Z")
        commandInvocationTest(
            command,
            """fill-order-summary $fromTimestamp $toTimestamp 30"""
        )
        val summary = orderSummaryExtendedRepository.selectAll()
        assertThat(summary).extracting(
            { it.key.partnerId },
            { it.key.timestamp },
            OrderSummaryExtended::createdCount,
            OrderSummaryExtended::createdGmv,
            OrderSummaryExtended::deliveredCount,
            OrderSummaryExtended::deliveredGmv
        ).containsExactlyInAnyOrder(
            tuple(543900L, Instant.parse("2021-12-10T11:00:00Z"), 2L, 232200L, 1L, 21200L),
            tuple(543900L, Instant.parse("2021-12-11T11:00:00Z"), 1L, 739000L, 0L, 0L),
            tuple(543901L, Instant.parse("2021-12-10T11:00:00Z"), 1L, 21200L, 1L, 21200L)
        )
    }
}
