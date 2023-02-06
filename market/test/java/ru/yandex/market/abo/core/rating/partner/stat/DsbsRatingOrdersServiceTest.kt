package ru.yandex.market.abo.core.rating.partner.stat

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.abo.api.entity.rating.operational.PartnerRatingCalculationPeriodType.LAST_DAYS

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 21.04.2022
 */
class DsbsRatingOrdersServiceTest @Autowired constructor(
    jdbcTemplate: JdbcTemplate,
    private val dsbsRatingOrdersService: DsbsRatingOrdersService
) : PartnerRatingBaseTest(jdbcTemplate) {

    @Test
    fun `get rating bad orders test`() {
        initDsbsRatingSourceData(PARTNER_ID, TEST_DATETIME)
        val badOrders = dsbsRatingOrdersService.getRatingBadOrders(TEST_DATE)
        assertEquals(
            getCancelledOrUpdatedOrdersForDsbsCancellationRate(),
            badOrders.filter { it.isCancelByShop || it.isItemsUpdatedCancelByShop }.map { it.orderId }.toHashSet()
        )
        assertEquals(
            getLateOrdersForDsbsLateDeliveryRate(),
            badOrders.filter { it.isLateDelivery }.map { it.orderId }.toHashSet()
        )
    }

    @Test
    fun `get rating stat fro last orders test`() {
        initDsbsRatingSourceData(PARTNER_ID, TEST_DATETIME)
        val stats = dsbsRatingOrdersService.getRatingStats(TEST_DATE)
        assertEquals(1, stats.size)
        val stat = stats[0].orderStat
        assertEquals(PARTNER_ID, stat.shopId)
        assertEquals(40.0, stat.lateDeliveryRate)
        assertEquals(30.0, stat.cancellationRate)
    }

    @Test
    fun `get rating stat for last days test`() {
        initDsbsRatingSourceData(PARTNER_ID, TEST_DATETIME, LAST_DAYS)
        val stats = dsbsRatingOrdersService.getRatingStats(TEST_DATE)
        assertEquals(1, stats.size)
        val stat = stats[0].orderStat
        assertEquals(PARTNER_ID, stat.shopId)
        assertEquals(25.0, stat.lateDeliveryRate)
        assertEquals(25.0, stat.cancellationRate)
    }

    @Test
    fun `get order stats for creation test`() {
        initDsbsRatingSourceData(PARTNER_ID, TEST_DATETIME)
        val stats = dsbsRatingOrdersService.getOrderStatsForCreation(TEST_DATE)
        assertEquals(
            getOrdersForDsbsCancellationRate(),
            stats.filter { it.useInCancellationRate }.map { it.orderId }.toHashSet()
        )
        assertEquals(
            getOrdersForDsbsLateDeliveryRate(),
            stats.filter { it.useInLateShipRate }.map { it.orderId }.toHashSet()
        )
    }

    companion object {
        private const val PARTNER_ID = 123L
        private val TEST_DATETIME = LocalDateTime.now()
        private val TEST_DATE = TEST_DATETIME.toLocalDate()
    }
}
