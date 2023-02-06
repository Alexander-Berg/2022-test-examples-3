package ru.yandex.market.abo.tms.cpa.order.limit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.abo.cpa.order.limit.BaseCpaOrderLimitTest
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitPartner
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderLimitCountRepo
import ru.yandex.market.abo.cpa.order.limit.cutoff.CpaOrderLimitActiveCutoffRepo
import ru.yandex.market.abo.cpa.order.limit.cutoff.CpaOrderLimitCutoffManager
import ru.yandex.market.abo.cpa.order.model.PartnerModel.CROSSDOCK
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitRepo

/**
 * @author komarovns
 */
open class CpaOrderLimitCutoffUpdaterTest @Autowired constructor(
    jdbcTemplate: JdbcTemplate,
    cpaOrderLimitRepo: CpaOrderLimitRepo,
    cpaOrderLimitCountRepo: CpaOrderLimitCountRepo,
    cpaOrderLimitActiveCutoffRepo: CpaOrderLimitActiveCutoffRepo
) : BaseCpaOrderLimitTest(cpaOrderLimitRepo, cpaOrderLimitCountRepo, cpaOrderLimitActiveCutoffRepo) {

    private val cpaOrderLimitCutoffManager: CpaOrderLimitCutoffManager = mock()
    private val cpaOrderLimitCutoffUpdater: CpaOrderLimitCutoffUpdater = CpaOrderLimitCutoffUpdater(
        jdbcTemplate, cpaOrderLimitCutoffManager
    )

    @Test
    fun `load partners for cutoff close`() {
        initLimit(1, DSBS, 10)
        initLimit(2, DSBS, 10)
        initLimit(2, CROSSDOCK, 10, deleted = true)
        initLimit(4, DSBS, 10)

        initCount(1, DSBS, 2)
        initCount(2, DSBS, 100)
        initCount(2, CROSSDOCK, 100)
        initCount(3, CROSSDOCK, 10)
        initCount(4, DSBS, 2)

        initCutoff(1, DSBS)
        initCutoff(2, DSBS)
        initCutoff(2, CROSSDOCK)
        initCutoff(3, CROSSDOCK)

        flushAndClear()

        val partnerCaptor = argumentCaptor<CpaOrderLimitPartner>()
        cpaOrderLimitCutoffUpdater.closeCutoffs()
        verify(cpaOrderLimitCutoffManager, atLeastOnce()).closeCutoff(partnerCaptor.capture())
        assertThat(partnerCaptor.allValues).containsExactlyInAnyOrder(
            CpaOrderLimitPartner(1, DSBS),
            CpaOrderLimitPartner(2, CROSSDOCK),
            CpaOrderLimitPartner(3, CROSSDOCK)
        )
    }

    @Test
    fun `load partners for cutoff open`() {
        initLimit(1, DSBS, 10)
        initLimit(1, CROSSDOCK, 0)
        initLimit(2, DSBS, 0)
        initLimit(2, CROSSDOCK, 10)
        initLimit(3, DSBS, 10, deleted = true)
        initLimit(3, CROSSDOCK, 10)

        initCount(1, DSBS, 10)
        initCount(1, CROSSDOCK, 10)
        initCount(2, CROSSDOCK, 9)
        initCount(3, DSBS, 100)
        initCount(3, CROSSDOCK, 100)

        initCutoff(3, CROSSDOCK)

        flushAndClear()

        val partnerCaptor = argumentCaptor<CpaOrderLimitPartner>()
        cpaOrderLimitCutoffUpdater.openCutoffs()
        verify(cpaOrderLimitCutoffManager, atLeastOnce()).openCutoff(partnerCaptor.capture())
        assertThat(partnerCaptor.allValues).containsExactlyInAnyOrder(
            CpaOrderLimitPartner(1, DSBS),
            CpaOrderLimitPartner(1, CROSSDOCK),
            CpaOrderLimitPartner(2, DSBS)
        )
    }
}
