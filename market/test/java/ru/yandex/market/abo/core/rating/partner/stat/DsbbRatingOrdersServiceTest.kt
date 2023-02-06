package ru.yandex.market.abo.core.rating.partner.stat

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CANCELLATION_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.core.rating.partner.calculator.PartnerRatingCalculator

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 18.04.2022
 */
class DsbbRatingOrdersServiceTest @Autowired constructor(
    jdbcTemplate: JdbcTemplate,
    private val dsbbRatingOrdersService: DsbbRatingOrdersService,
    private val dsbbRatingCalculator: PartnerRatingCalculator
) : PartnerRatingBaseTest(jdbcTemplate) {

    @BeforeEach
    fun init() {
        initDsbbRatingSourceData(PARTNER_ID, TEST_DATETIME)
    }

    @Test
    fun `get rating bad orders test`() {
        val badOrders = dsbbRatingOrdersService.getRatingBadOrders(TEST_DATE)
        assertEquals(
            getCancelledOrUpdatedOrdersForDsbbCancellationRate(),
            badOrders.filter { it.isShopFailed }.map { it.orderId }.toHashSet()
        )
        assertEquals(
            getLateOrdersForDsbbLateShipRate(),
            badOrders.filter { it.isLateShipped }.map { it.orderId }.toHashSet()
        )
    }

    @Test
    fun `get rating stat test`() {
        val stats = dsbbRatingOrdersService.getRatingStats(TEST_DATE)
        assertEquals(1, stats.size)
        val stat = stats[0].orderStat
        assertEquals(PARTNER_ID, stat.shopId)
        assertEquals(9, stat.lateShippedWeightSum)
        assertEquals(10, stat.useInLateShipWeightSum)
        assertEquals(3, stat.shopFailedWeightSum)
        assertEquals(4, stat.useInCancellationWeightSum)
    }

    @Test
    fun `calculate rating test`() {
        val ratings = dsbbRatingCalculator.calculateRating(TEST_DATETIME)
        assertEquals(1, ratings.size)
        val rating = ratings[0]
        assertEquals(90.0, rating.details.components.first { it.type == LATE_SHIP_RATE }.value)
        assertEquals(75.0, rating.details.components.first { it.type == CANCELLATION_RATE }.value)
    }

    @Test
    fun `get order stats for creation test`() {
        val stats = dsbbRatingOrdersService.getOrderStatsForCreation(TEST_DATE)
        assertEquals(
            getOrdersForDsbbLateShipRate(),
            stats.filter { it.useInLateShipRate }.map { it.orderId }.toHashSet()
        )
        assertEquals(
            getOrdersForDsbbCancellationRate(),
            stats.filter { it.useInCancellationRate }.map { it.orderId }.toHashSet()
        )
    }

    companion object {
        private const val PARTNER_ID = 123L
        private val TEST_DATETIME = LocalDateTime.now()
        private val TEST_DATE = TEST_DATETIME.toLocalDate()
    }
}
