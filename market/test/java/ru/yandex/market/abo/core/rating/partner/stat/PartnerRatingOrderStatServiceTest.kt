package ru.yandex.market.abo.core.rating.partner.stat

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.abo.core.rating.partner.PartnerRatingService
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 05.07.2022
 */
class PartnerRatingOrderStatServiceTest @Autowired constructor(
    jdbcTemplate: JdbcTemplate,
    private val partnerRatingOrderStatService: PartnerRatingOrderStatService,
    private val partnerRatingService: PartnerRatingService,
) : PartnerRatingBaseTest(jdbcTemplate) {

    @BeforeEach
    fun init() {
        initDsbbRatingSourceData(DSBB_PARTNER_ID, TEST_DATETIME)
        initDsbsRatingSourceData(DSBS_PARTNER_ID, TEST_DATETIME)
    }

    @Test
    fun `test order stats preparing`() {
        partnerRatingOrderStatService.createOrderStat(TEST_DATE, DSBB)
        partnerRatingService.updateLastRatingCalculationDate(DSBB, TEST_DATE)
        partnerRatingOrderStatService.updateOrderStatForUsage(TEST_DATE, DSBB)
        val stats = partnerRatingOrderStatService.getStats(DSBB_PARTNER_ID)
        assertEquals(
            getOrdersForDsbbCancellationRate(),
            stats.filter { it.useInCancellationRate }.map { it.orderId }.toHashSet()
        )
        assertEquals(
            getOrdersForDsbbLateShipRate(),
            stats.filter { it.useInLateShipRate }.map { it.orderId }.toHashSet()
        )
    }

    @Test
    fun `test stats update affects only one model`() {
        partnerRatingOrderStatService.createOrderStat(TEST_DATE, DSBB)
        partnerRatingService.updateLastRatingCalculationDate(DSBB, TEST_DATE)

        partnerRatingOrderStatService.createOrderStat(TEST_DATE, DSBS)
        partnerRatingService.updateLastRatingCalculationDate(DSBS, TEST_DATE)

        partnerRatingOrderStatService.updateOrderStatForUsage(TEST_DATE, DSBB)

        assertFalse(partnerRatingOrderStatService.getStats(DSBB_PARTNER_ID).isEmpty())
        assertTrue(partnerRatingOrderStatService.getStats(DSBS_PARTNER_ID).isEmpty())
    }

    companion object {
        private const val DSBB_PARTNER_ID = 124L
        private const val DSBS_PARTNER_ID = 125L
        private val TEST_DATETIME = LocalDateTime.now()
        private val TEST_DATE = TEST_DATETIME.toLocalDate()
    }
}
