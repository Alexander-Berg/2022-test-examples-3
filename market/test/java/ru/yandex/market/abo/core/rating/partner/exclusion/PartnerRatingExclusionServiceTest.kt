package ru.yandex.market.abo.core.rating.partner.exclusion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.core.rating.partner.exclusion.model.PartnerRatingExclusion
import ru.yandex.market.abo.core.rating.partner.exclusion.model.RatingExclusionType.BY_REQUEST
import ru.yandex.market.abo.cpa.order.model.PartnerModel.FULFILLMENT
import ru.yandex.market.abo.util.entity.DeletableEntityService
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 16.06.2020
 */
class PartnerRatingExclusionServiceTest @Autowired constructor(
    private val service: PartnerRatingExclusionService,
    private val repo: PartnerRatingExclusionRepo
) : DeletableEntityServiceTest<PartnerRatingExclusion, Long>() {

    @Test
    fun `save with conflict test`() {
        service.save(newEntity(), 1L)
        flushAndClear()

        service.save(newEntity(), 1L)
        flushAndClear()

        val savedExclusions = repo.findAll()
        assertEquals(1, savedExclusions.size)
        assertEquals(REQUEST_ID, savedExclusions[0].requestId)
        assertEquals(PARTNER_ID, savedExclusions[0].partnerId)
        assertEquals(FULFILLMENT, savedExclusions[0].partnerModel)
        assertEquals(BY_REQUEST, savedExclusions[0].exclusionType)
    }

    @Test
    fun `mark deleted test test`() {
        service.save(newEntity(), 1L)
        flushAndClear()

        val entityForDelete = newEntity()
        entityForDelete.deleted = true
        service.save(entityForDelete, 1L)
        flushAndClear()

        val savedExclusions = repo.findAll()
        assertEquals(1, savedExclusions.size)
        assertEquals(REQUEST_ID, savedExclusions[0].requestId)
        assertEquals(PARTNER_ID, savedExclusions[0].partnerId)
        assertEquals(FULFILLMENT, savedExclusions[0].partnerModel)
        assertEquals(BY_REQUEST, savedExclusions[0].exclusionType)
        assertTrue(savedExclusions[0].deleted)
    }

    override fun service(): DeletableEntityService<PartnerRatingExclusion, Long> = service

    override fun extractId(entity: PartnerRatingExclusion): Long = entity.id

    override fun newEntity(): PartnerRatingExclusion = PartnerRatingExclusion().apply {
        partnerId = PARTNER_ID
        exclusionType = BY_REQUEST
        partnerModel = FULFILLMENT
        requestId = REQUEST_ID
        comment = COMMENT
        deleted = false
    }

    override fun example() = PartnerRatingExclusion()

    companion object {
        private const val REQUEST_ID = 123456L
        private const val PARTNER_ID = 123L
        private const val COMMENT = "Магазин-плохиш"

    }
}
