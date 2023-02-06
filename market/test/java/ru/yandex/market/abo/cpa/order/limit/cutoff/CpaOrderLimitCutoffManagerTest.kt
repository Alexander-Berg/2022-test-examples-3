package ru.yandex.market.abo.cpa.order.limit.cutoff

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.cpa.order.limit.BaseCpaOrderLimitTest
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitPartner
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderLimitCountRepo
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitRepo

/**
 * @author komarovns
 */
class CpaOrderLimitCutoffManagerTest @Autowired constructor(
    private val cpaOrderLimitCutoffManager: CpaOrderLimitCutoffManager,
    cpaOrderLimitRepo: CpaOrderLimitRepo,
    cpaOrderLimitCountRepo: CpaOrderLimitCountRepo,
    cpaOrderLimitActiveCutoffRepo: CpaOrderLimitActiveCutoffRepo
) : BaseCpaOrderLimitTest(cpaOrderLimitRepo, cpaOrderLimitCountRepo, cpaOrderLimitActiveCutoffRepo) {

    @ParameterizedTest
    @MethodSource("close cutoff method source")
    fun `close cutoff`(closed: Boolean, count: Long?, limit: Int?) {
        initCutoff(PARTNER_ID, PARTNER_MODEL)
        count?.let { initCount(PARTNER_ID, PARTNER_MODEL, it) }
        limit?.let { initLimit(PARTNER_ID, PARTNER_MODEL, limit) }

        flushAndClear()
        cpaOrderLimitCutoffManager.closeCutoff(PARTNER)
        flushAndClear()

        assertNotEquals(closed, cpaOrderLimitActiveCutoffRepo.existsById(CpaOrderLimitActiveCutoff.Key(PARTNER_ID, PARTNER_MODEL)))
    }

    @ParameterizedTest
    @MethodSource("open cutoff method source")
    fun `open cutoff`(opened: Boolean, count: Long?, limit: Int?) {
        count?.let { initCount(PARTNER_ID, PARTNER_MODEL, it) }
        limit?.let { initLimit(PARTNER_ID, PARTNER_MODEL, limit) }

        flushAndClear()
        cpaOrderLimitCutoffManager.openCutoff(PARTNER)
        flushAndClear()

        assertEquals(opened, cpaOrderLimitActiveCutoffRepo.existsById(CpaOrderLimitActiveCutoff.Key(PARTNER_ID, PARTNER_MODEL)))
    }

    companion object {
        private const val PARTNER_ID = 774L
        private val PARTNER_MODEL = DSBS
        private val PARTNER = CpaOrderLimitPartner(PARTNER_ID, PARTNER_MODEL)

        @JvmStatic
        fun `close cutoff method source`(): Iterable<Arguments> = listOf(
            Arguments.of(true, 2L, 10),
            Arguments.of(false, 12L, 10),
            Arguments.of(true, null, 10),
            Arguments.of(true, 100L, null),
            Arguments.of(true, null, null)
        )

        @JvmStatic
        fun `open cutoff method source`(): Iterable<Arguments> = listOf(
            Arguments.of(true, 10L, 10),
            Arguments.of(false, 9L, 10),
            Arguments.of(true, null, 0),
            Arguments.of(false, 100L, null),
            Arguments.of(false, null, null),
        )
    }
}
