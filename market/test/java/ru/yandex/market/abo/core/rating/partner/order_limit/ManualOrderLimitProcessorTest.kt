package ru.yandex.market.abo.core.rating.partner.order_limit

import java.time.LocalDateTime
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.core.rating.partner.HIGH_PARTNER_RATING_TOTAL_VALUE
import ru.yandex.market.abo.core.rating.partner.PartnerRatingActual
import ru.yandex.market.abo.core.rating.partner.PartnerRatingRepo
import ru.yandex.market.abo.core.rating.partner.details.ComponentDetails
import ru.yandex.market.abo.core.rating.partner.details.PartnerRatingDetails
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitRepo
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimitReason
import ru.yandex.market.abo.cpa.order.model.PartnerModel

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 23.06.2022
 */
class ManualOrderLimitProcessorTest @Autowired constructor(
    private val manualOrderLimitProcessor: ManualOrderLimitProcessor,
    private val partnerRatingActualRepo: PartnerRatingRepo.RatingActualRepo,
    private val cpaOrderLimitRepo: CpaOrderLimitRepo
) : EmptyTest() {

    @Test
    fun `test limit not removed for partners with low rating`() {
        createLimit(CpaOrderLimitReason.MANUAL)
        createRating(HIGH_PARTNER_RATING_TOTAL_VALUE - 15.0)

        manualOrderLimitProcessor.removeManualLimitsForHighRatingPartners(PartnerModel.DSBB)

        assertEquals(1, cpaOrderLimitRepo.findAllByDeletedFalseAndReasonIn(setOf(CpaOrderLimitReason.MANUAL)).size)
    }

    @Test
    fun `test limit not removed for not manual reason`() {
        createLimit(CpaOrderLimitReason.NEWBIE)
        createRating(HIGH_PARTNER_RATING_TOTAL_VALUE + 1.0)

        manualOrderLimitProcessor.removeManualLimitsForHighRatingPartners(PartnerModel.DSBB)

        assertEquals(1, cpaOrderLimitRepo.findAllByDeletedFalseAndReasonIn(setOf(CpaOrderLimitReason.NEWBIE)).size)
    }

    @Test
    fun `test limit removed for high rating partner`() {
        createLimit(CpaOrderLimitReason.MANUAL)
        createRating(HIGH_PARTNER_RATING_TOTAL_VALUE + 1.0)

        manualOrderLimitProcessor.removeManualLimitsForHighRatingPartners(PartnerModel.DSBB)

        assertTrue(cpaOrderLimitRepo.findAllByDeletedFalseAndReasonIn(setOf(CpaOrderLimitReason.MANUAL)).isEmpty())
    }

    private fun createLimit(reason: CpaOrderLimitReason) {
        cpaOrderLimitRepo.save(
            CpaOrderLimit(
                PARTNER_ID, PartnerModel.DSBB, reason, 123, null, 123152
            ).apply {
                creationTime = Date()
                createdUserId = -1
            }
        )
        flushAndClear()
    }

    private fun createRating(total: Double) {
        partnerRatingActualRepo.save(PartnerRatingActual(
            PARTNER_ID,
            PartnerModel.DSBB,
            LocalDateTime.now(),
            emptyMap(),
            PartnerRatingDetails(12, listOf(ComponentDetails(LATE_SHIP_RATE, 0.9))),
            total,
            0
        ))
        flushAndClear()
    }

    companion object {
        private const val PARTNER_ID = 123124L
    }
}
