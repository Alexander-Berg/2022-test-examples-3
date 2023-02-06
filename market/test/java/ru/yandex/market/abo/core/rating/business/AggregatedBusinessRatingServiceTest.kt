package ru.yandex.market.abo.core.rating.business

import java.util.Date
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.model.PartnerModel.CROSSDOCK
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 19.07.2021
 */
class AggregatedBusinessRatingServiceTest @Autowired constructor(
    private val aggregatedBusinessRatingService: AggregatedBusinessRatingService,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @BeforeEach
    fun init() {
        jdbcTemplate.update("""
            insert into ext_organization_info(datasource_id, business_id)
            values ($PARTNER_ID, $BUSINESS_ID),
                   ($ANOTHER_PARTNER_ID, $BUSINESS_ID)
        """.trimIndent())
        flushAndClear()

        jdbcTemplate.update("""
            insert into operational_rating(calc_time, shop_id, order_count, late_ship_rate,
                cancellation_rate, return_rate, total, actual, partner_model, generation_id)
            values (?, $PARTNER_ID, 100, 0.0, 0.0, 0.0, $PARTNER_TOTAL_RATING, true, ?, ?)
        """.trimIndent(), Date(), DSBB.name, 1)
        flushAndClear()

        jdbcTemplate.update("""
            insert into partner_rating(calc_time, partner_id, total, actual, partner_model, generation_id)
            values (?, $ANOTHER_PARTNER_ID, $ANOTHER_PARTNER_TOTAL_RATING, true, ?, ?)
        """.trimIndent(), Date(), CROSSDOCK.name, 2)

        jdbcTemplate.update("""
            insert into partner_rating_actual(calc_time, partner_id, total, partner_model, generation_id)
            values (?, $ANOTHER_PARTNER_ID, $ANOTHER_PARTNER_TOTAL_RATING, ?, ?)
        """.trimIndent(), Date(), CROSSDOCK.name, 2)
        flushAndClear()
    }

    @Test
    fun `business rating aggregation test`() {
        val businessRating = aggregatedBusinessRatingService.getRating(BUSINESS_ID)
        val rangeStats = businessRating.ratingRangeStats.sortedBy { it.ratingScore }

        assertEquals(2, rangeStats.size)

        assertEquals(1, rangeStats[0].partnerRatingsCount)
        assertEquals(setOf(PARTNER_ID), rangeStats[0].partnerIds)
        assertEquals(1, rangeStats[0].ratingScore)

        assertEquals(1, rangeStats[1].partnerRatingsCount)
        assertEquals(setOf(ANOTHER_PARTNER_ID), rangeStats[1].partnerIds)
        assertEquals(5, rangeStats[1].ratingScore)
    }

    companion object {
        private const val PARTNER_ID = 123L
        private const val ANOTHER_PARTNER_ID = 128L

        private const val PARTNER_TOTAL_RATING = 35.7
        private const val ANOTHER_PARTNER_TOTAL_RATING = 97.4

        private const val BUSINESS_ID = 124235L
    }
}
