package ru.yandex.market.mbi.orderservice.tms.service.command

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.commandInvocationTest
import ru.yandex.market.mbi.orderservice.common.model.yt.CreatedAtIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CreatedAtIndexRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Instant

@CleanupTables(
    [
        OrderEntity::class,
        CreatedAtIndex::class
    ]
)
internal class CalculateCreatedAtIndexCommandTest : FunctionalTest() {

    @Autowired
    lateinit var ytOrderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var createdAtIndexRepository: CreatedAtIndexRepository

    @BeforeEach
    internal fun setUp() {
        val orders = this::class.loadTestEntities<OrderEntity>("entities/summary-orders.json")
        orderEntityRepository.insertRows(orders)
    }

    @Test
    fun commandTest() {
        val command =
            CalculateCreatedAtIndexCommand(
                ytOrderRepository,
                createdAtIndexRepository
            )

        val fromTimestamp = Instant.parse("2021-12-09T00:00:00Z")
        val toTimestamp = Instant.parse("2021-12-17T00:00:00Z")

        commandInvocationTest(
            command,
            """calculate-created-at $fromTimestamp $toTimestamp 0 2"""
        )
        val indices = createdAtIndexRepository.selectAll()
        assertThat(indices).extracting(
            { it.tableKey.partnerId },
            { it.tableKey.orderId },
            { it.tableKey.createdAt },
        ).containsExactlyInAnyOrder(
            tuple(543900L, 81545129L, Instant.parse("2021-12-10T10:03:44Z")),
            tuple(543900L, 81545127L, Instant.parse("2021-12-10T11:01:44Z")),
            tuple(543900L, 81545130L, Instant.parse("2021-12-10T11:04:44Z")),
            tuple(543901L, 81545131L, Instant.parse("2021-12-10T11:05:44Z")),
            tuple(543900L, 81545128L, Instant.parse("2021-12-11T11:02:30Z")),
            tuple(543900L, 81545124L, Instant.parse("2021-12-09T11:01:44Z")),
            tuple(543900L, 81545125L, Instant.parse("2021-12-10T11:01:44Z")),
            tuple(543900L, 81545126L, Instant.parse("2021-12-10T11:01:44Z"))
        )
    }
}
