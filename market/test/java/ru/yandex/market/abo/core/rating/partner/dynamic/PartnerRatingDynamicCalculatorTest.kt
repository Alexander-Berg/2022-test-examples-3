package ru.yandex.market.abo.core.rating.partner.dynamic

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CANCELLATION_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.RETURN_RATE
import ru.yandex.market.abo.core.rating.partner.PartnerRatingActual
import ru.yandex.market.abo.core.rating.partner.PartnerRatingRepo
import ru.yandex.market.abo.core.rating.partner.details.ComponentDetails
import ru.yandex.market.abo.core.rating.partner.details.PartnerRatingDetails
import ru.yandex.market.abo.core.rating.partner.settings.PartnerModelSettingsService
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 21.10.2021
 */
class PartnerRatingDynamicCalculatorTest {
    private val dynamicService: PartnerRatingDynamicService = mock()
    private val actualRatingRepo: PartnerRatingRepo.RatingActualRepo = mock()
    private val settingsService: PartnerModelSettingsService = mock()

    private val partnerRatingDynamicCalculator = PartnerRatingDynamicCalculator(
        dynamicService, actualRatingRepo, settingsService
    )

    @BeforeEach
    fun init() {
        whenever(dynamicService.getCalcDateDynamicsByPartner(CALC_TIME.toLocalDate().minusDays(1), DSBB)).thenReturn(
            mapOf(
                PARTNER_ID to buildDynamic(
                    CALC_TIME.toLocalDate().minusDays(1),
                    RATING_VALUE - RATING_VALUE_DIFF,
                    COMPONENT_VALUE - COMPONENT_VALUE_DIFF)
            ))
        whenever(settingsService.getShowInPiOrdersCount(DSBB)).thenReturn(10)
        whenever(actualRatingRepo.findAllByPartnerModel(DSBB)).thenReturn(listOf(
            PartnerRatingActual(
                PARTNER_ID, DSBB,
                CALC_TIME, emptyMap(), PartnerRatingDetails(100, listOf(
                ComponentDetails(LATE_SHIP_RATE, COMPONENT_VALUE),
                ComponentDetails(RETURN_RATE, COMPONENT_VALUE),
                ComponentDetails(CANCELLATION_RATE, COMPONENT_VALUE)
            )), RATING_VALUE, 2
            )
        ))
    }

    @Test
    fun `dynamic calculation test`() {
        partnerRatingDynamicCalculator.calculateDynamic(CALC_TIME.toLocalDate(), DSBB)

        val captor = argumentCaptor<List<PartnerRatingDynamic>>()
        verify(dynamicService, atLeastOnce()).saveNewDynamic(eq(CALC_TIME.toLocalDate()), eq(DSBB), captor.capture())

        val expectedDynamic = buildDynamic(CALC_TIME.toLocalDate(), RATING_VALUE, COMPONENT_VALUE)
        val savedDynamic = captor.firstValue[0]
        assertEquals(expectedDynamic.key, savedDynamic.key)
        assertEquals(expectedDynamic.total, savedDynamic.total)
        assertEquals(expectedDynamic.totalDiff, savedDynamic.totalDiff)
        assertEquals(expectedDynamic.componentDiffs, savedDynamic.componentDiffs)
    }

    private fun buildDynamic(calcDate: LocalDate, ratingValue: Double, componentValue: Double) = PartnerRatingDynamic(
        calcDate, PARTNER_ID, DSBB,
        ratingValue,
        RATING_VALUE_DIFF,
        RatingComponentDiffsWrapper(listOf(
            RatingComponentDiff(LATE_SHIP_RATE, componentValue, COMPONENT_VALUE_DIFF),
            RatingComponentDiff(RETURN_RATE, componentValue, COMPONENT_VALUE_DIFF),
            RatingComponentDiff(CANCELLATION_RATE, componentValue, COMPONENT_VALUE_DIFF),
        ))
    )

    companion object {
        private val CALC_TIME = LocalDateTime.of(2021, 10, 21, 11, 15)

        private const val PARTNER_ID = 123L

        private const val RATING_VALUE: Double = 89.0
        private const val COMPONENT_VALUE: Double = 1.0

        private const val RATING_VALUE_DIFF: Double = 3.0
        private const val COMPONENT_VALUE_DIFF: Double = 0.03
    }
}
