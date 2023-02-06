package ru.yandex.market.abo.core.rating.partner.freeze

import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.rating.operational.PartnerRatingCalculationPeriodInfo
import ru.yandex.market.abo.api.entity.rating.operational.PartnerRatingCalculationPeriodType.LAST_ORDERS
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CANCELLATION_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.RETURN_RATE
import ru.yandex.market.abo.core.rating.partner.PartnerRatingActual
import ru.yandex.market.abo.core.rating.partner.PartnerRatingRepo
import ru.yandex.market.abo.core.rating.partner.details.ComponentDetails
import ru.yandex.market.abo.core.rating.partner.details.PartnerRatingDetails
import ru.yandex.market.abo.core.rating.partner.dynamic.PartnerRatingDynamic
import ru.yandex.market.abo.core.rating.partner.dynamic.PartnerRatingDynamicFrozen
import ru.yandex.market.abo.core.rating.partner.dynamic.PartnerRatingDynamicRepo
import ru.yandex.market.abo.core.rating.partner.dynamic.RatingComponentDiff
import ru.yandex.market.abo.core.rating.partner.dynamic.RatingComponentDiffsWrapper
import ru.yandex.market.abo.core.rating.partner.model.PartnerRatingFrozen
import ru.yandex.market.abo.core.rating.partner.model.PartnerRatingFrozenRepo
import ru.yandex.market.abo.core.rating.partner.report.model.DsbbRatingReportDetails
import ru.yandex.market.abo.core.rating.partner.report.model.PartnerRatingReportPart
import ru.yandex.market.abo.core.rating.partner.report.model.PartnerRatingReportPartRepo
import ru.yandex.market.abo.core.rating.partner.report.model.RatingReportDetails
import ru.yandex.market.abo.core.rating.partner.report.service.PartnerRatingReportPartService
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.util.kotlin.toDate
import ru.yandex.market.core.abo.AboCutoff.LOW_RATING
import ru.yandex.market.mbi.api.client.MbiApiClient

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 09.03.2022
 */
class PartnerRatingFreezeServiceTest @Autowired constructor(
    private val partnerRatingFreezeService: PartnerRatingFreezeService,
    private val partnerRatingFrozenRepo: PartnerRatingFrozenRepo,
    private val mbiApiClient: MbiApiClient,

    private val partnerRatingActualRepo: PartnerRatingRepo.RatingActualRepo,

    private val partnerRatingReportPartService: PartnerRatingReportPartService,
    private val partnerRatingReportPartRepo: PartnerRatingReportPartRepo,

    private val partnerRatingDynamicRepo: PartnerRatingDynamicRepo,

    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @Test
    fun freezeTest() {
        val calcTime = LocalDateTime.now()
        val rating = PartnerRatingActual(
            PARTNER_ID, DSBB, calcTime,
            emptyMap(),
            PartnerRatingDetails(12, listOf(
                ComponentDetails(LATE_SHIP_RATE, 1.0),
                ComponentDetails(CANCELLATION_RATE, 1.0),
                ComponentDetails(RETURN_RATE, 1.0)
            ), PartnerRatingCalculationPeriodInfo(LAST_ORDERS, 50)),
            87.1,
            1
        )
        val reportPart = PartnerRatingReportPart(
            PARTNER_ID, DSBB, CANCELLATION_RATE,
            RatingReportDetails.fromDsbbDetails(DsbbRatingReportDetails.CancellationDetails(
                1111L, LocalDateTime.now().toDate(), "lala").convertToDsbbDetails()
            )
        )
        val previousDynamic = PartnerRatingDynamic(
            calcTime.toLocalDate().minusDays(1), PARTNER_ID, DSBB,
            87.1, 0.0,
            RatingComponentDiffsWrapper(listOf(
                RatingComponentDiff(LATE_SHIP_RATE, 1.0, 0.0),
                RatingComponentDiff(CANCELLATION_RATE, 1.0, 0.0),
                RatingComponentDiff(RETURN_RATE, 1.0, 0.0)
            ))
        )
        val dynamic = PartnerRatingDynamic(
            calcTime.toLocalDate(), PARTNER_ID, DSBB,
            87.1, 0.0,
            RatingComponentDiffsWrapper(listOf(
                RatingComponentDiff(LATE_SHIP_RATE, 1.0, 0.0),
                RatingComponentDiff(CANCELLATION_RATE, 1.0, 0.0),
                RatingComponentDiff(RETURN_RATE, 1.0, 0.0)
            ))
        )

        jdbcTemplate.update(
            """
            insert into cpa_order_stat(order_id, shop_id, user_id, creation_date, processing, rgb)
            values (?, ?, ?, ?, ?, 1)
        """.trimIndent(),
            1111L, PARTNER_ID, -17L, calcTime.minusDays(4), calcTime.minusDays(4)
        )
        jdbcTemplate.update(
            """
            insert into cpa_order_delivery(order_id, by_shipment, delivery_partner_type)
            values (?, ?, 'YANDEX_MARKET')
        """.trimIndent(),
            1111L, calcTime.toLocalDate().minusDays(5)
        )
        jdbcTemplate.update("""
            insert into partner_rating_order_stat(
                order_id, partner_id, partner_model_id, estimated_date,
                use_in_late_ship_rate, use_in_cancellation_rate, is_cancelled,
                prepared_for_next_rating)
            values (?, ?, ?, ?, true, true, false, false)
        """.trimIndent(), 1111L, PARTNER_ID, DSBB.id, calcTime.toLocalDate().minusDays(5))
        partnerRatingActualRepo.save(rating)
        partnerRatingReportPartRepo.saveAll(listOf(reportPart))
        partnerRatingReportPartService.saveReportPartsUpdateFlag(DSBB, calcTime.toLocalDate())
        partnerRatingDynamicRepo.saveAll(listOf(previousDynamic, dynamic))
        partnerRatingDynamicRepo.markNotActualBefore(LocalDate.now().minusDays(1), DSBB)
        flushAndClear()
        flushAndClear()

        partnerRatingFreezeService.freezeRatingIfNecessary(PARTNER_ID)
        flushAndClear()

        assertThat(rating.toFrozen())
            .usingRecursiveComparison()
            .isEqualTo(partnerRatingFreezeService.getRating(PARTNER_ID, DSBB))

        assertThat(reportPart.toFrozen())
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(partnerRatingFreezeService.getReportParts(PARTNER_ID, DSBB)[0])

        partnerRatingFreezeService.getDynamic(PARTNER_ID, DSBB)
            .sortedBy { it.calcDate }
            .forEachIndexed { index, dynamicF ->
                if (index == 0) {
                    assertDynamicEquals(previousDynamic, dynamicF)
                    assertFalse(dynamicF.actual)
                } else {
                    assertDynamicEquals(dynamic, dynamicF)
                    assertTrue(dynamicF.actual)
                }
            }

        val orderStats = partnerRatingFreezeService.getOrderStat(PARTNER_ID)
        assertEquals(1, orderStats.size)
        val orderStat = orderStats[0]
        assertEquals(1111, orderStat.toOrderStat().orderId)
    }

    @Test
    fun `unfreeze rating for shops without cutoff test`() {
        whenever(mbiApiClient.getShopsWithAboCutoff(eq(LOW_RATING), anyOrNull())).thenReturn(emptyList())
        partnerRatingFrozenRepo.save(PartnerRatingFrozen(PARTNER_ID,
            DSBB,
            LocalDateTime.now(),
            emptyMap(),
            null,
            32.0,
            1))
        flushAndClear()
        partnerRatingFreezeService.unfreezeRatingIfCutoffClosed()
        flushAndClear()

        assertTrue(partnerRatingFrozenRepo.findAll().isEmpty())
    }

    private fun assertDynamicEquals(expected: PartnerRatingDynamic, actual: PartnerRatingDynamicFrozen) =
        assertThat(expected)
            .usingRecursiveComparison(
                RecursiveComparisonConfiguration.builder().withIgnoredFields("actual").build()
            )
            .isEqualTo(actual)

    companion object {
        private const val PARTNER_ID = 123L
    }
}
