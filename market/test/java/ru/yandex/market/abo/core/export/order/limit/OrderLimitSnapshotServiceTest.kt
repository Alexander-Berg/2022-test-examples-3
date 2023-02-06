package ru.yandex.market.abo.core.export.order.limit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.export.order.limit.OrderLimitSnapshot.Key
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason.NEWBIE
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import java.util.Date

/**
 * @author zilzilok
 */
class OrderLimitSnapshotServiceTest @Autowired constructor(
    private val orderLimitSnapshotService: OrderLimitSnapshotService,
    private val orderLimitSnapshotBatchUpdater: PgBatchUpdater<OrderLimitSnapshot>
) : EmptyTest() {

    @Test
    fun `update snapshot`() {
        val delete = OrderLimitSnapshot(Key(1L, DSBB), 40, NEWBIE, null, 100, Date())
        val save = OrderLimitSnapshot(Key(2L, DSBB), 40, NEWBIE, null, 100, Date())

        orderLimitSnapshotBatchUpdater.insertWithoutUpdate(listOf(delete))
        orderLimitSnapshotService.updateSnapshot(mapOf(false to listOf(delete), true to listOf(save)))
        flushAndClear()

        val activeLimits = orderLimitSnapshotService.findAll()
        assertEquals(1, activeLimits.size)
        assertTrue(activeLimits.contains(save))
    }
}
