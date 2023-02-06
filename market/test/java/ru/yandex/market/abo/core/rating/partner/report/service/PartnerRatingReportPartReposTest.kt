package ru.yandex.market.abo.core.rating.partner.report.service

import java.time.LocalDateTime
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.common.util.date.DateUtil
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CANCELLATION_RATE
import ru.yandex.market.abo.core.rating.partner.report.model.FulfillmentRatingReportDetails
import ru.yandex.market.abo.core.rating.partner.report.model.PartnerRatingReportPart
import ru.yandex.market.abo.core.rating.partner.report.model.PartnerRatingReportPartFrozen
import ru.yandex.market.abo.core.rating.partner.report.model.PartnerRatingReportPartFrozenRepo
import ru.yandex.market.abo.core.rating.partner.report.model.PartnerRatingReportPartRepo
import ru.yandex.market.abo.core.rating.partner.report.model.RatingReportDetails
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 10.12.2020
 */
open class PartnerRatingReportPartReposTest @Autowired constructor(
    private val reportPartRepo: PartnerRatingReportPartRepo,
    private val reportPartFrozenRepo: PartnerRatingReportPartFrozenRepo
) : EmptyTest() {

    @Test
    fun serializationTest() {
        val ratingReportPart = PartnerRatingReportPart(
            partnerId = PARTNER_ID,
            partnerModel =  PartnerModel.FULFILLMENT,
            type = RatingMetric.FF_LATE_SHIP_RATE,
            details = buildRatingReportDetails()
        )
        val ratingReportPartFrozen = PartnerRatingReportPartFrozen(
            partnerId = PARTNER_ID,
            partnerModel = DSBB,
            type = CANCELLATION_RATE,
            details = buildRatingReportDetails()
        )
        reportPartRepo.save(ratingReportPart)
        reportPartFrozenRepo.save(ratingReportPartFrozen)
        flushAndClear()
        Assertions.assertThat(reportPartRepo.findAll())
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(listOf(ratingReportPart))

        Assertions.assertThat(reportPartFrozenRepo.findAll())
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(listOf(ratingReportPartFrozen))
    }

    private fun buildRatingReportDetails(): RatingReportDetails {
        val fulfillmentDetails = FulfillmentRatingReportDetails().apply {
            lateShipDetails = FulfillmentRatingReportDetails.LateShipDetails(
                12435L,
                DateUtil.asDate(LocalDateTime.now().minusDays(5)),
                DateUtil.asDate(LocalDateTime.now().minusDays(3)),
                12,
                "Поставка несвоевременно приехала"
            )
        }
        return RatingReportDetails(fulfillmentDetails = fulfillmentDetails)
    }

    companion object {
        private const val PARTNER_ID = 123L
    }
}
