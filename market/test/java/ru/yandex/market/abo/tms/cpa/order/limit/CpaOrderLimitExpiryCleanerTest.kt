package ru.yandex.market.abo.tms.cpa.order.limit

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.count.CpaOrderCountTotal
import ru.yandex.market.abo.cpa.order.count.CpaOrderCountTotal.Key
import ru.yandex.market.abo.cpa.order.count.CpaOrderCountTotalRepo
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderLimitCount
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderLimitCountRepo
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason.MANUAL
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason.NEWBIE
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS
import ru.yandex.market.abo.util.kotlin.toDate

/**
 * @author komarovns
 */
open class CpaOrderLimitExpiryCleanerTest @Autowired constructor(
    private val cpaOrderLimitExpiryCleaner: CpaOrderLimitExpiryCleaner,
    private val cpaOrderLimitService: CpaOrderLimitService,
    private val cpaOrderLimitCountRepo: CpaOrderLimitCountRepo,
    private val cpaOrderCountTotalRepo: CpaOrderCountTotalRepo
) : EmptyTest() {

    @Test
    fun `expired by date`() {
        val now = LocalDate.now()
        limit(0, now.plusDays(1))
        limit(1, now)
        limit(2, now.minusDays(1))
        flushAndClear()

        cpaOrderLimitExpiryCleaner.deleteExpiredLimits()
        flushAndClear()

        assertEquals(listOf(0L), cpaOrderLimitService.findAllActive().map { it.shopId })
    }

    @Test
    fun `expired by count`() {
        limit(1, 21)
        limit(2, 20)
        limit(3, 19)
        limit(4, 19, LocalDate.now().plusDays(2))
        limit(5, DSBS, NEWBIE, 50, null, null)
        (1..5).forEach { count(it.toLong(), 20, LocalDate.now()) }
        totalCount(4, 20)
        totalCount(5, 31)
        flushAndClear()

        cpaOrderLimitExpiryCleaner.deleteExpiredLimits()
        flushAndClear()

        assertEquals(listOf(1L, 4L), cpaOrderLimitService.findAllActive().map { it.shopId })
    }

    private fun limit(shopId: Long, expiryDate: LocalDate) = limit(shopId, DSBS, null, expiryDate, null)

    private fun limit(shopId: Long, expiryCount: Int) = limit(
        shopId, DSBS, expiryCount, null, null
    )

    private fun limit(shopId: Long, expiryCount: Int, expirationCounterDayFrom: LocalDate?) = limit(
        shopId, DSBS, expiryCount, null, expirationCounterDayFrom
    )

    private fun limit(shopId: Long, partnerModel: PartnerModel,
                      expiryCount: Int?, expiryDate: LocalDate?, expirationCounterDayFrom: LocalDate?): Long = limit(
        shopId, partnerModel, MANUAL, expiryCount, expiryDate, expirationCounterDayFrom
    )

    private fun limit(shopId: Long, partnerModel: PartnerModel, reason: CpaOrderLimitReason,
                      expiryCount: Int?, expiryDate: LocalDate?,
                      expirationCounterDayFrom: LocalDate?): Long {
        val limit = CpaOrderLimit(
            shopId, partnerModel, reason, 1, expiryDate?.toDate(), expiryCount
        )
        limit.expirationCounterDayFrom = expirationCounterDayFrom
        return cpaOrderLimitService.addIfNotExistsOrDeleted(limit, 0).id
    }

    private fun count(shopId: Long, count: Long, day: LocalDate) = count(shopId, DSBS, count, day)

    private fun count(shopId: Long, partnerModel: PartnerModel, count: Long, day: LocalDate) {
        val entity = CpaOrderLimitCount(shopId, partnerModel.id, day, count)
        cpaOrderLimitCountRepo.save(entity)
    }

    private fun totalCount(shopId: Long, count: Long) {
        val entity = CpaOrderCountTotal(Key(shopId, DSBS), LocalDate.now().minusDays(90L), count)
        cpaOrderCountTotalRepo.save(entity)
    }
}
