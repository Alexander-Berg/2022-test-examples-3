package ru.yandex.market.abo.core.rating.operational;

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.EmptyTestWithTransactionTemplate
import ru.yandex.market.abo.core.rating.partner.generation.PartnerRatingGeneration
import ru.yandex.market.abo.core.rating.partner.generation.PartnerRatingGenerationService
import ru.yandex.market.abo.core.rating.partner.settings.PartnerModelSettingsService
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS

/**
 * @author artemmz, neliubin
 * @date 31/07/19.
 */
class OperationalRatingServiceTest : EmptyTestWithTransactionTemplate() {
    private val operationalRatingRepo: OperationalRatingRepo = mock()
    private val partnerRatingGenerationService: PartnerRatingGenerationService = mock()
    private val partnerModelSettingsService: PartnerModelSettingsService = mock()
    private val actualRating: OperationalRating = mock {
        on { shopId } doReturn SHOP_ID
    }
    private val operationalRatingService = OperationalRatingService(
        operationalRatingRepo,
        partnerRatingGenerationService,
        transactionTemplate,
        partnerModelSettingsService
    )

    @BeforeEach
    fun init() {
        whenever(operationalRatingRepo.findAllByPartnerModelAndActualTrue(any())).thenReturn(listOf(actualRating))

        val generation = PartnerRatingGeneration(DSBB, LocalDateTime.now())
        whenever(partnerRatingGenerationService.createNewGeneration(any(), any())).thenReturn(generation)
    }

    @Test
    fun `save new rating test`() {
        val newRating = mockRating(DSBB)

        operationalRatingService.saveNewRating(listOf(newRating))

        verify(operationalRatingRepo).markAllNotActual()
        verify(newRating).calcTime = any()
    }

    @Test
    fun `save new and delete old test`() {
        val newRating = mockRating(DSBB)
        whenever(actualRating.shopId).thenReturn(SHOP_ID + 1)

        operationalRatingService.saveNewRating(listOf(newRating))

        verify(operationalRatingRepo).markAllNotActual()
        verify(operationalRatingRepo).saveAll(listOf(newRating))
        verify(newRating).calcTime = any()
    }

    @Test
    fun `rating collapse test`() {
        val dsbsActualRating = mockRating(DSBS)
        val dsbbActualRating = mockRating(DSBB)

        val ratings = operationalRatingService.collapseAllShopRatings(listOf(dsbsActualRating, dsbbActualRating))

        assertEquals(ratings.size, 1)
        assertEquals(ratings[0].partnerModel, DSBB)
    }

    private fun mockRating(model: PartnerModel): OperationalRating {
        return mock {
            on { shopId } doReturn SHOP_ID
            on { partnerModel } doReturn model
            on { orderCount } doReturn ORDER_COUNT
        }
    }

    companion object {
        private const val SHOP_ID = 123L
        private const val ORDER_COUNT = 1000
    }
}
