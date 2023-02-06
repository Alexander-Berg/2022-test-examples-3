package ru.yandex.market.abo.core.rating.partner

import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.yandex.EmptyTestWithTransactionTemplate
import ru.yandex.market.abo.core.rating.partner.calculator.PartnerRatingCalculator
import ru.yandex.market.abo.core.rating.partner.listener.PartnerRatingListenerWrapper
import ru.yandex.market.abo.cpa.order.model.PartnerModel

/**
 * @author imelnikov, neliubin
 * @since 2020
 */
class PartnerRatingManagerTest : EmptyTestWithTransactionTemplate() {
    private val ratingCalcTime = LocalDateTime.now()

    private val partnerRatingService: PartnerRatingService = mock()
    private val newRating: PartnerRating = mock {
        on { partnerId } doReturn SHOP_ID
        on { calcTime } doReturn ratingCalcTime
        on { this.partnerModel } doReturn MODEL
    }
    private val calculator: PartnerRatingCalculator = mock {
        on { model } doReturn MODEL
        on { calculateRating(any()) } doReturn listOf(newRating)
    }
    private val anotherCalculator: PartnerRatingCalculator = mock {
        on { model } doReturn PartnerModel.CROSSDOCK
    }
    private val listenersWrapper: PartnerRatingListenerWrapper = mock()

    private val ratingService = PartnerRatingManager(
        partnerRatingService,
        listOf(calculator, anotherCalculator),
        listenersWrapper,
        transactionTemplate
    )

    @Test
    fun `update rating`() {
        ratingService.updateRatingIfNotUpdatedToday(MODEL)
        verify(calculator).calculateRating(any())
        verify(anotherCalculator, never()).calculateRating(any())
        verify(partnerRatingService).saveNewRating(eq(listOf(newRating)), eq(MODEL), any())
    }

    companion object {
        private const val SHOP_ID = 1L
        private val MODEL = PartnerModel.DSBB
    }
}
