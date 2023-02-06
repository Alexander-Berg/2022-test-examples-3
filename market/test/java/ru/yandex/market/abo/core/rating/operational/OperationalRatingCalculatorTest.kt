package ru.yandex.market.abo.core.rating.operational

import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.abo.cpa.order.model.OrderOperation
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.cpa.order.service.OrderOperationService

/**
 * @author artemmz
 * @date 31/07/19.
 */
class OperationalRatingCalculatorTest {
    private val operationalRatingService: OperationalRatingService = mock()

    private val orderOperationService: OrderOperationService = mock {
        on { getOperationStat(any<LocalDate>(), any()) } doReturn listOf(initDropshipOrderOp())
    }

    private val ratingMetricService: RatingMetricService = mock {
        on { ratingMatrix } doReturn RatingMetricRangesMatrixTest.rangesMatrix()
    }

    private val operationalRatingCalculator = OperationalRatingCalculator(
        operationalRatingService, orderOperationService, ratingMetricService
    )

    @Test
    fun testCalcRating() {
        operationalRatingCalculator.recalculateRating()
        val savedRatingCaptor = argumentCaptor<List<OperationalRating>>()
        verify(operationalRatingService).saveNewRating(savedRatingCaptor.capture())
        val savedRating = savedRatingCaptor.firstValue
        // assertEquals(2, savedRating.size)
        // assertThat(savedRating)
        //     .extracting<PartnerModel> { it.partnerModel }
        //     .containsExactlyInAnyOrder(DSBS, DSBB)
        // savedRating.forEach { checkRating(it) }
        assertEquals(1, savedRating.size)
        assertThat(savedRating)
            .extracting<PartnerModel> { it.partnerModel }
            .containsExactlyInAnyOrder(DSBB)
        savedRating.forEach { checkRating(it) }
    }

    private fun checkRating(rating: OperationalRating) {
        assertEquals(SHOP_ID, rating.shopId)
        assertEquals(ORDER_COUNT, rating.orderCount)
        assertEquals(100 - SHIPPED_PERCENT, rating.lateShipRate)
        assertEquals(SHOP_FAILED_PERCENT, rating.cancellationRate)
        assertEquals(RETURNS_PERCENT, rating.returnRate)
        assertEquals(56.83, rating.total)
        assertFalse(rating.actual)
    }

    private fun initDropshipOrderOp() = OrderOperation().apply {
        count = ORDER_COUNT
        shopId = SHOP_ID
        processedCount = ORDER_COUNT
        shippedPercent = SHIPPED_PERCENT
        shopFailPercent = SHOP_FAILED_PERCENT
        returnBadQualityPercent = RETURNS_PERCENT
        partnerModel = DSBB
    }

    companion object {
        private const val ORDER_COUNT = 77
        private const val SHOP_ID = 111L
        private const val SHIPPED_PERCENT = 80.0
        private const val SHOP_FAILED_PERCENT = 10.0
        private const val RETURNS_PERCENT = 1.0
    }
}
