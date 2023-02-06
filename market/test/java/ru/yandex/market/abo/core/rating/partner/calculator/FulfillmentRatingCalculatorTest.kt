package ru.yandex.market.abo.core.rating.partner.calculator;

import java.time.LocalDateTime
import java.util.EnumSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric
import ru.yandex.market.abo.core.rating.operational.RatingMetricRanges
import ru.yandex.market.abo.core.rating.operational.RatingMetricRangesMatrix
import ru.yandex.market.abo.core.rating.operational.RatingMetricService
import ru.yandex.market.abo.core.rating.partner.PartnerRatingPart
import ru.yandex.market.abo.core.rating.partner.source.FulfillmentsRatingSourceProvider
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderCountService
import ru.yandex.market.abo.cpa.order.model.PartnerModel.FULFILLMENT

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 15.10.2020
 */
class FulfillmentRatingCalculatorTest {

    private val sourceProvider: FulfillmentsRatingSourceProvider = mock()
    private val ratingMetricService: RatingMetricService = mock()
    private val cpaOrderCountService: CpaOrderCountService = mock()

    private val fulfillmentRatingCalculator = FulfillmentRatingCalculator(
        FULFILLMENT,
        ratingMetricService,
        cpaOrderCountService,
        EnumSet.of(RatingMetric.FF_PLANFACT_RATE, RatingMetric.FF_RETURN_RATE, RatingMetric.FF_LATE_SHIP_RATE),
        sourceProvider
    )

    @BeforeEach
    fun init() {
        val ratingParts = listOf(
            buildRatingPart(RatingMetric.FF_PLANFACT_RATE, FF_PLANFACT_RATE),
            buildRatingPart(RatingMetric.FF_LATE_SHIP_RATE, FF_LATE_SHIP_RATE),
            buildRatingPart(RatingMetric.FF_RETURN_RATE, FF_RETURN_RATE)
        )

        whenever(sourceProvider.provide(any())).thenReturn(ratingParts)
        val ratingMatrix = ratingMatrix()
        whenever(ratingMetricService.ratingMatrix).thenReturn(ratingMatrix)
    }

    @Test
    fun `test rating calculation`() {
        val ratings = fulfillmentRatingCalculator.calculateRating(LocalDateTime.now())

        assertEquals(1, ratings.size)
        assertEquals(EXPECTED_TOTAL_RATING, ratings[0].total)
        assertEquals(FF_LATE_SHIP_RATE, ratings[0].ratesMap[RatingMetric.FF_LATE_SHIP_RATE])
        assertEquals(FF_PLANFACT_RATE, ratings[0].ratesMap[RatingMetric.FF_PLANFACT_RATE])
        //MARKETASSESSOR-10476: because of temporary returns ignore
        assertEquals(0.0, ratings[0].ratesMap[RatingMetric.FF_RETURN_RATE])
    }

    private fun ratingMatrix(): RatingMetricRangesMatrix {
        val metricRangesList = listOf(
            initMetricRanges(RatingMetric.FF_PLANFACT_RATE, 15.0, 10.0, 7.0, 5.0),
            initMetricRanges(RatingMetric.FF_LATE_SHIP_RATE, 25.0, 20.0, 15.0, 10.0),
            initMetricRanges(RatingMetric.FF_RETURN_RATE, 60.0, 40.0, 30.0, 20.0),
            initMetricRanges(RatingMetric.TOTAL, 40.0, 60.0, 80.0, 95.0)
        )

        return RatingMetricRangesMatrix(metricRangesList)
    }

    private fun initMetricRanges(metric: RatingMetric,
                                 star1Bound: Double, star2Bound: Double,
                                 star3Bound: Double, star4Bound: Double
    ): RatingMetricRanges {
        val metricRanges = RatingMetricRanges()
        metricRanges.metric = metric

        metricRanges.star1bound = star1Bound
        metricRanges.star2bound = star2Bound
        metricRanges.star3bound = star3Bound
        metricRanges.star4bound = star4Bound
        metricRanges.star5bound = if (metric != RatingMetric.TOTAL) 0.0 else 100.0

        metricRanges.isInverted = metric != RatingMetric.TOTAL

        return metricRanges
    }

    private fun buildRatingPart(metric: RatingMetric, value: Double): PartnerRatingPart {
        val part = PartnerRatingPart()

        part.partnerId = PARTNER_ID
        part.type = metric
        part.value = value

        return part
    }

    companion object {
        private const val PARTNER_ID = 123L

        private const val FF_PLANFACT_RATE = 8.0
        private const val FF_LATE_SHIP_RATE = 5.0
        private const val FF_RETURN_RATE = 0.5

        //MARKETASSESSOR-10476: because of temporary returns ignore
//        private const val EXPECTED_TOTAL_RATING = 69.875
        private const val EXPECTED_TOTAL_RATING = 70.0
    }
}
