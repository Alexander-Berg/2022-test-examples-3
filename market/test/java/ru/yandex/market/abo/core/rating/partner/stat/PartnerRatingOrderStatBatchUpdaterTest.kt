package ru.yandex.market.abo.core.rating.partner.stat

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 17.05.2022
 */
class PartnerRatingOrderStatBatchUpdaterTest @Autowired constructor(
    private val partnerRatingOrderStatBatchUpdater: PgBatchUpdater<PartnerRatingOrderStat>,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @Test
    fun `batch update test`() {
        val orderStat = PartnerRatingOrderStat(ORDER_ID, PARTNER_ID, DSBB, ESTIMATED_DATE, true, true, false, true)
        partnerRatingOrderStatBatchUpdater.insertWithoutUpdate(listOf(orderStat))
        flushAndClear()

        assertThat(listOf(orderStat))
            .usingRecursiveComparison()
            .isEqualTo(
                jdbcTemplate.query(
                    "SELECT * FROM partner_rating_order_stat WHERE partner_id = ?",
                    PARTNER_RATING_ORDER_STAT_MAPPER,
                    PARTNER_ID
                )
            )
    }

    companion object {
        private const val PARTNER_ID = 123L
        private const val ORDER_ID = 1111L
        private val ESTIMATED_DATE = LocalDateTime.now()
    }
}
