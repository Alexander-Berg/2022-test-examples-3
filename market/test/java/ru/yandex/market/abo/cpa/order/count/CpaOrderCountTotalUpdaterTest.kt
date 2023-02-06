package ru.yandex.market.abo.cpa.order.count

import java.time.LocalDate
import java.time.Month.OCTOBER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderLimitCountRepo
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 28.10.2021
 */
class CpaOrderCountTotalUpdaterTest @Autowired constructor(
    private val cpaOrderCountTotalUpdater: CpaOrderCountTotalUpdater,
    private val cpaOrderCountTotalRepo: CpaOrderCountTotalRepo,
    private val cpaOrderLimitCountRepo: CpaOrderLimitCountRepo,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @BeforeEach
    fun init() {
        jdbcTemplate.update("""
            INSERT INTO cpa_order_limit_count(partner_id, partner_model, day, count) 
            VALUES ($PARTNER_ID, ${DSBB.id}, '${UPDATE_DATE}', 1) 
        """.trimIndent())

        jdbcTemplate.update("""
            INSERT INTO cpa_order_count_total(partner_id, partner_model, last_order_day, count) 
            VALUES ($PARTNER_ID, ${DSBB.id}, '${UPDATE_DATE.minusDays(1)}', 1) 
        """.trimIndent())
        flushAndClear()
    }

    @Test
    fun `update order count test`() {
        cpaOrderCountTotalUpdater.updateCount(UPDATE_DATE)
        assertEquals(2, cpaOrderCountTotalRepo.findByIdOrNull(CpaOrderCountTotal.Key(PARTNER_ID, DSBB))!!.count)
    }

    @Test
    fun `clean order count test`() {
        cpaOrderCountTotalUpdater.cleanLimitCountTable(UPDATE_DATE)
        assertTrue(cpaOrderLimitCountRepo.findAll().isEmpty())
    }

    companion object {
        private const val PARTNER_ID = 123L
        private val UPDATE_DATE = LocalDate.of(2021, OCTOBER, 28)
    }
}
