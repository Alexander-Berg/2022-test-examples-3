package ru.yandex.market.abo.cpa.order.limit.count

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.count.CpaOrderCountTotal
import ru.yandex.market.abo.cpa.order.count.CpaOrderCountTotal.Key
import ru.yandex.market.abo.cpa.order.count.CpaOrderCountTotalRepo
import ru.yandex.market.abo.cpa.order.count.CpaOrderCountTotalUpdater.Companion.TRANSFUSE_DAYS_BEFORE
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitPartner
import ru.yandex.market.abo.cpa.order.model.PartnerModel.CROSSDOCK
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS

/**
 * @author komarovns
 */
class CpaOrderCountServiceTest @Autowired constructor(
    private val cpaOrderCountService: CpaOrderCountService,
    private val cpaOrderLimitCountRepo: CpaOrderLimitCountRepo,
    private val cpaOrderCountTotalRepo: CpaOrderCountTotalRepo,
) : EmptyTest() {

    @Test
    fun `total count`() {
        val now = LocalDate.now()
        cpaOrderCountTotalRepo.save(CpaOrderCountTotal(Key(0, DSBS), now.minusDays(TRANSFUSE_DAYS_BEFORE), 2))
        cpaOrderLimitCountRepo.saveAll(listOf(
            CpaOrderLimitCount(0, DSBS.id, now, 1),
            CpaOrderLimitCount(1, DSBS.id, now, 1),
            CpaOrderLimitCount(0, DSBS.id, now - 1, 2),
        ))
        flushAndClear()
        assertEquals(5, cpaOrderCountService.loadTotalCount(CpaOrderLimitPartner(0, DSBS)))
        assertEquals(1, cpaOrderCountService.loadTotalCount(CpaOrderLimitPartner(1, DSBS)))
    }

    @Test
    fun `total count for model`() {
        val now = LocalDate.now()
        cpaOrderLimitCountRepo.saveAll(listOf(
            CpaOrderLimitCount(0, DSBS.id, now, 1),
            CpaOrderLimitCount(0, DSBS.id, now - 1, 2),
            CpaOrderLimitCount(0, DSBS.id, now - 2, 2),
            CpaOrderLimitCount(0, DSBS.id, now + 1, 2),
            CpaOrderLimitCount(1, DSBS.id, now, 1),
            CpaOrderLimitCount(1, CROSSDOCK.id, now, 4)
        ))
        flushAndClear()
        assertEquals(hashMapOf(
            0L to 3L,
            1L to 1L
        ), cpaOrderCountService.loadTotalCount(DSBS, now - 1, now))
    }

    @Test
    fun `avg count`() {
        val now = LocalDate.now()
        cpaOrderLimitCountRepo.saveAll(listOf(
            CpaOrderLimitCount(0, DSBS.id, now, 10),
            CpaOrderLimitCount(0, DSBS.id, now - 1, 2),
            CpaOrderLimitCount(1, DSBS.id, now, 10),
            CpaOrderLimitCount(1, CROSSDOCK.id, now, 100)
        ))
        flushAndClear()
        assertEquals(hashMapOf(
            0L to 6L,
            1L to 10L
        ), cpaOrderCountService.loadAvg(DSBS, 2))
    }

    @Test
    fun `avg count by partner`() {
        val now = LocalDate.now()
        cpaOrderLimitCountRepo.saveAll(listOf(
            CpaOrderLimitCount(0, DSBS.id, now, 10),
            CpaOrderLimitCount(0, DSBS.id, now - 1, 2),
            CpaOrderLimitCount(0, DSBS.id, now - 5, 12),
        ))
        flushAndClear()
        assertEquals(6, cpaOrderCountService.loadAvg(0, DSBS, 2))
    }

    private operator fun LocalDate.plus(days: Int) = this.plusDays(days.toLong())
    private operator fun LocalDate.minus(days: Int) = this + (-days)
}
