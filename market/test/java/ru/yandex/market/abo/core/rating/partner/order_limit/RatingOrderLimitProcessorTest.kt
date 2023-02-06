package ru.yandex.market.abo.core.rating.partner.order_limit

import java.time.LocalDate
import java.util.stream.Stream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.core.cutoff.CutoffManager
import ru.yandex.market.abo.core.exception.ExceptionalShopReason.DONT_CREATE_RATING_ORDER_LIMIT
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.core.rating.operational.OperationalRatingOrderLimitNotifier
import ru.yandex.market.abo.core.rating.partner.PartnerRatingActual
import ru.yandex.market.abo.core.rating.partner.PartnerRatingService
import ru.yandex.market.abo.core.rating.partner.dynamic.PartnerRatingDynamic
import ru.yandex.market.abo.core.rating.partner.dynamic.PartnerRatingDynamicService
import ru.yandex.market.abo.core.rating.partner.order_limit.PartnerRatingLimitType.ORDERS_LIMIT
import ru.yandex.market.abo.cpa.MbiApiService
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderCountService
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.core.abo.AboCutoff.LOW_RATING
import kotlin.math.ceil

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 15.07.2022
 */
class RatingOrderLimitProcessorTest {

    private val partnerRatingService: PartnerRatingService = mock()
    private val partnerRatingDynamicService: PartnerRatingDynamicService = mock()
    private val orderLimitService: CpaOrderLimitService = mock()
    private val exceptionalShopsService: ExceptionalShopsService = mock()
    private val cpaOrderCountService: CpaOrderCountService = mock()
    private val operationalRatingOrderLimitNotifier: OperationalRatingOrderLimitNotifier = mock()
    private val ratingLimitRangeService: PartnerRatingLimitRangeService = mock {
        on { getModelRanges(any()) } doReturn listOf(
            buildLimitRange(1, 0.0, MEDIUM_RATING_LOWER_BOUND, PartnerRatingLimitType.SWITCH_OFF, 0.0),
            buildLimitRange(2, MEDIUM_RATING_LOWER_BOUND, HIGH_RATING_LOWER_BOUND, ORDERS_LIMIT, ORDERS_PART_FOR_LIMIT),
            buildLimitRange(3, HIGH_RATING_LOWER_BOUND, 100.0, PartnerRatingLimitType.NO_LIMIT, 1.0)
        )
    }
    private val cutoffManager: CutoffManager = mock()
    private val mbiApiService: MbiApiService = mock()

    private val ratingOrderLimitProcessor: RatingOrderLimitProcessor = RatingOrderLimitProcessor(
        partnerRatingService, partnerRatingDynamicService, orderLimitService,
        exceptionalShopsService, operationalRatingOrderLimitNotifier, ratingLimitRangeService,
        cutoffManager, cpaOrderCountService, mbiApiService
    )

    private val partnerRating: PartnerRatingActual = mock {
        on { partnerId } doReturn PARTNER_ID
        on { partnerModel } doReturn DSBB
        on { ordersCount } doReturn ORDERS_LOWER_BOUND_FOR_RATING_LIMIT + 3
    }
    private val orderLimit: CpaOrderLimit = mock {
        on { shopId } doReturn PARTNER_ID
    }

    @BeforeEach
    fun init() {
        whenever(partnerRatingService.getActualRatings(any())).thenReturn(listOf(partnerRating))
        whenever(exceptionalShopsService.loadShops(DONT_CREATE_RATING_ORDER_LIMIT)).thenReturn(emptySet())
        whenever(mbiApiService.getShopsWithAboCutoff(LOW_RATING)).thenReturn(emptySet())
        whenever(orderLimitService.findNonRatingLimits()).thenReturn(emptyList())
        whenever(orderLimitService.findRatingLimits()).thenReturn(emptyList())
        whenever(orderLimitService.updateLimitWithOperationalRatingReason(eq(PARTNER_ID), eq(DSBB), any()))
            .thenReturn(orderLimit)
        whenever(orderLimitService.markOperationalRatingLimitDeleted(PARTNER_ID, DSBB)).thenReturn(orderLimit)
    }

    @Test
    fun `do not create limit if shop in exceptions test`() {
        whenever(exceptionalShopsService.loadShops(DONT_CREATE_RATING_ORDER_LIMIT)).thenReturn(setOf(PARTNER_ID))
        ratingOrderLimitProcessor.updateRatingLimits(LocalDate.now(), DSBB)
        verifyLimitNotUpdated()
    }

    @Test
    fun `do not create limit if shop has rating cutoff test`() {
        whenever(mbiApiService.getShopsWithAboCutoff(LOW_RATING)).thenReturn(setOf(PARTNER_ID))
        ratingOrderLimitProcessor.updateRatingLimits(LocalDate.now(), DSBB)
        verifyLimitNotUpdated()
    }

    @ParameterizedTest
    @CsvSource("DSBB, false", "DSBS, true")
    fun `do not create limit if exists non rating limit test`(existsLimitModel: PartnerModel, processLimit: Boolean) {
        mockRatingDependingOnPrevious(MEDIUM_RATING_LOWER_BOUND, 1.0)
        val nonRatingLimit = mockOrderLimit(existsLimitModel)
        whenever(orderLimitService.findNonRatingLimits()).thenReturn(listOf(nonRatingLimit))
        ratingOrderLimitProcessor.updateRatingLimits(LocalDate.now(), DSBB)
        verify(orderLimitService, times(if (processLimit) 1 else 0))
            .updateLimitWithOperationalRatingReason(eq(PARTNER_ID), eq(DSBB), any())
    }

    @Test
    fun `do not update limit if rating range not changed and limit exists test`() {
        mockRatingDependingOnPrevious(MEDIUM_RATING_LOWER_BOUND + 10, 0.0)
        mockCurrentRatingLimit(LOW_RATING_ORDER_LIMIT + 20)
        ratingOrderLimitProcessor.updateRatingLimits(LocalDate.now(), DSBB)
        verifyLimitNotUpdated()
    }

    @Test
    fun `do not update limit if limit exists and rating above low test`() {
        mockRatingDependingOnPrevious(
            previousRatingValue = MEDIUM_RATING_LOWER_BOUND - 1,
            ratingsDiff = MEDIUM_RATING_LOWER_BOUND + 1
        )
        mockCurrentRatingLimit(LOW_RATING_ORDER_LIMIT)
        ratingOrderLimitProcessor.updateRatingLimits(LocalDate.now(), DSBB)
        verifyLimitNotUpdated()
    }

    @Test
    fun `do not update limit for sequential high ranges test`() {
        mockRatingDependingOnPrevious(HIGH_RATING_LOWER_BOUND + 1, 0.0)
        ratingOrderLimitProcessor.updateRatingLimits(LocalDate.now(), DSBB)
        verifyLimitNotUpdated()
    }

    @ParameterizedTest(name = "limitResolveTest_{index}")
    @MethodSource("limitResolveTestArguments")
    fun `limit resolve test`(ratingValue: Double, daysAvgOrdersCount: Long, expectedOrdersLimit: Int) {
        whenever(partnerRating.total).thenReturn(ratingValue)
        mockPreviousRating(ratingValue - 5)
        whenever(cpaOrderCountService.loadAvg(DSBB, DAYS_PERIOD_FOR_AVG_ORDERS_CALCULATION))
                .thenReturn(mapOf(PARTNER_ID to  daysAvgOrdersCount))
        whenever(orderLimit.deleted).thenReturn(false)
        ratingOrderLimitProcessor.updateRatingLimits(LocalDate.now(), DSBB)
        verify(orderLimitService).updateLimitWithOperationalRatingReason(
                eq(PARTNER_ID), eq(DSBB), eq(expectedOrdersLimit)
        )
        verify(operationalRatingOrderLimitNotifier).notify(
                eq(PARTNER_ID), eq(DSBB), eq(orderLimit), any(), any()
        )
    }

    @Test
    fun `delete limit if rating high test`() {
        mockPreviousRating(HIGH_RATING_LOWER_BOUND - 10)
        whenever(partnerRating.total).thenReturn(HIGH_RATING_LOWER_BOUND + 1.0)
        whenever(orderLimit.deleted).thenReturn(true)
        ratingOrderLimitProcessor.updateRatingLimits(LocalDate.now(), DSBB)
        verify(orderLimitService).markOperationalRatingLimitDeleted(PARTNER_ID, DSBB)
        verify(orderLimitService, never()).updateLimitWithOperationalRatingReason(eq(PARTNER_ID), eq(DSBB), any())
    }

    private fun buildLimitRange(id: Int, lowerBound: Double, upperBound: Double,
                                limitType: PartnerRatingLimitType, ordersPartForLimit: Double
    ) = PartnerRatingLimitRange(
        id, DSBB, lowerBound, upperBound, limitType, ordersPartForLimit,
        if (limitType == PartnerRatingLimitType.SWITCH_OFF) 0 else LOW_RATING_ORDER_LIMIT
    )

    private fun verifyLimitNotUpdated() {
        verifyNoMoreInteractions(operationalRatingOrderLimitNotifier)
        verify(orderLimitService, never()).updateLimitWithOperationalRatingReason(eq(PARTNER_ID), eq(DSBB), any())
        verify(orderLimitService, never()).markOperationalRatingLimitDeleted(PARTNER_ID, DSBB)
    }

    private fun mockCurrentRatingLimit(ordersCount: Int) {
        val currentLimit = mockOrderLimit(DSBB)
        whenever(currentLimit.orderLimit).thenReturn(ordersCount)
        whenever(orderLimitService.findRatingLimits()).thenReturn(listOf(currentLimit))
    }

    private fun mockOrderLimit(partnerModel: PartnerModel): CpaOrderLimit {
        return mock {
            on { shopId } doReturn PARTNER_ID
            on { partnerModelId } doReturn partnerModel.id
            on { getPartnerModel() } doReturn partnerModel
        }
    }

    private fun mockRatingDependingOnPrevious(previousRatingValue: Double, ratingsDiff: Double) {
        mockPreviousRating(previousRatingValue)
        whenever(partnerRating.total).thenReturn(previousRatingValue + ratingsDiff)
    }

    private fun mockPreviousRating(ratingValue: Double) {
        val dynamic: PartnerRatingDynamic = mock {
            on { partnerId } doReturn PARTNER_ID
            on { total } doReturn ratingValue
        }
        whenever(partnerRatingDynamicService.getCalcDateDynamicsByPartner(any(), eq(DSBB)))
            .thenReturn(mapOf(PARTNER_ID to dynamic))
    }

    companion object {
        private const val PARTNER_ID = 123L

        private const val MEDIUM_RATING_LOWER_BOUND = 40.0
        private const val HIGH_RATING_LOWER_BOUND = 95.0
        private const val ORDERS_PART_FOR_LIMIT = 0.5

        private const val DEFAULT_DAYS_AVG_OFFERS_COUNT = 5L
        private const val LIMIT_LOWER_BOUND = 1

        @JvmStatic
        fun limitResolveTestArguments(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    MEDIUM_RATING_LOWER_BOUND + 1,
                    DEFAULT_DAYS_AVG_OFFERS_COUNT,
                    ceil(DEFAULT_DAYS_AVG_OFFERS_COUNT * 0.5).toInt()
                ),
                Arguments.of(
                    MEDIUM_RATING_LOWER_BOUND + 1,
                    (LIMIT_LOWER_BOUND / ORDERS_PART_FOR_LIMIT).toInt() + 10,
                    (LIMIT_LOWER_BOUND + (ORDERS_PART_FOR_LIMIT * 10)).toInt()
                )
            )
        }
    }
}
