package ru.yandex.market.abo.core.rating.partner.report.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.core.pilot.PilotPartnerRepo
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author zilzilok
 */
class PartnerRatingReportPartRepoTest @Autowired constructor(
    private val partnerRatingReportPartRepo: PartnerRatingReportPartRepo,
    private val pilotPartnerRepo: PilotPartnerRepo
) : EmptyTest() {

    @Test
    fun `test repo`() {
        partnerRatingReportPartRepo.save(
            PartnerRatingReportPart(PARTNER_ID, DSBB, LATE_SHIP_RATE, RatingReportDetails())
        )

        val parts = partnerRatingReportPartRepo.findAllByPartnerModelAndActualTrue(DSBB)
        assertEquals(1, parts.size)

        partnerRatingReportPartRepo.deleteAllByPartnerModelAndActualFalse(DSBB)
    }

    companion object {
        private const val PARTNER_ID = 1L
    }
}
