package ru.yandex.market.abo.logbroker.order.limit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.export.order.limit.OrderLimitSnapshot
import ru.yandex.market.abo.core.export.order.limit.OrderLimitSnapshotService
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.order.limit.OrderLimits.OrderLimitInfo
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import ru.yandex.market.abo.util.logbroker.LogbrokerEventPublishService
import java.util.Date

/**
 * @author zilzilok
 */
class OrderLimitsProcessorTest @Autowired constructor(
    private val orderLimitSnapshotService: OrderLimitSnapshotService,
    private val orderLimitSnapshotBatchUpdater: PgBatchUpdater<OrderLimitSnapshot>,
    private val cpaOrderLimitService: CpaOrderLimitService
) : EmptyTest() {
    private val orderLimitEventPublishService:
        LogbrokerEventPublishService<OrderLimitInfo, OrderLimitLogbrokerEvent> = mock()
    private val orderLimitsProcessor: OrderLimitsProcessor = OrderLimitsProcessor(
        orderLimitEventPublishService, orderLimitSnapshotService, cpaOrderLimitService
    )

    @Test
    fun `new update`() {
        val orderLimit = defaultCpaOrderLimit()
        cpaOrderLimitService.addIfNotExistsOrDeleted(orderLimit, -1)

        orderLimitsProcessor.export()

        val activeLimits = orderLimitSnapshotService.findAll()
        assertEquals(1, activeLimits.size)
        assertTrue(activeLimits.contains(orderLimitsProcessor.convertToSnapshot(orderLimit)))
        verify(orderLimitEventPublishService).publishEvents(any())
    }

    @Test
    fun `old update`() {
        val orderLimit = defaultCpaOrderLimit()
        cpaOrderLimitService.addIfNotExistsOrDeleted(orderLimit, -1)
        orderLimitSnapshotBatchUpdater.insertWithoutUpdate(listOf(orderLimitsProcessor.convertToSnapshot(orderLimit)))

        orderLimitsProcessor.export()

        verify(orderLimitEventPublishService, never()).publishEvents(any())
    }

    @Test
    fun `diff update`() {
        val orderLimit = defaultCpaOrderLimit()
        orderLimitSnapshotBatchUpdater.insertWithoutUpdate(listOf(orderLimitsProcessor.convertToSnapshot(orderLimit)))
        val updateLimit = orderLimit.apply {
            deleted = true
            deletedUserId = -1
            deletionTime = Date()
        }
        cpaOrderLimitService.addIfNotExistsOrDeleted(updateLimit, -1)

        orderLimitsProcessor.export()

        val activeLimits = orderLimitSnapshotService.findAll()
        assertTrue(activeLimits.isEmpty())
        verify(orderLimitEventPublishService).publishEvents(any())
    }

    private fun defaultCpaOrderLimit() = CpaOrderLimit(PARTNER_ID, PARTNER_MODEL, REASON, ORDER_LIMIT, null, EXPIRY_CNT)
        .apply { creationTime = Date() }

    companion object {
        private const val PARTNER_ID = 1L
        private val PARTNER_MODEL = PartnerModel.DSBB
        private val REASON = CpaOrderLimitReason.NEWBIE
        private const val ORDER_LIMIT = 40
        private const val EXPIRY_CNT = 100
    }
}
