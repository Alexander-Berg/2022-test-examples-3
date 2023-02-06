package ru.yandex.market.abo.core.rating.partner.generation

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 17.06.2021
 */
class PartnerRatingGenerationServiceTest @Autowired constructor(
    private val partnerRatingGenerationService: PartnerRatingGenerationService,
    private val partnerRatingGenerationRepo: PartnerRatingGenerationRepo
) : EmptyTest() {

    @Test
    fun `create new generation test`() {
        val calcTime = LocalDateTime.now()
        partnerRatingGenerationService.createNewGeneration(DSBB, calcTime)
        flushAndClear()

        val savedGenerations = partnerRatingGenerationRepo.findAll()
        assertEquals(1, savedGenerations.size)
        assertEquals(DSBB, savedGenerations[0].partnerModel)
        assertEquals(calcTime, savedGenerations[0].calcTime)
        assertFalse(savedGenerations[0].actual)
    }

    @Test
    fun `save new generation as active test`() {
        val generation = partnerRatingGenerationService.createNewGeneration(DSBB, LocalDateTime.now())
        flushAndClear()

        partnerRatingGenerationService.saveGenerationAsActual(generation)
        flushAndClear()

        val savedGeneration = partnerRatingGenerationRepo.findById(generation.id).orElseThrow()
        assertTrue(savedGeneration.actual)

        val newGeneration = partnerRatingGenerationService.createNewGeneration(DSBB, LocalDateTime.now())
        flushAndClear()

        partnerRatingGenerationService.saveGenerationAsActual(newGeneration)
        flushAndClear()

        val savedGenerations = partnerRatingGenerationRepo.findAll().sortedBy { it.id }
        assertEquals(2, savedGenerations.size)
        assertFalse(savedGenerations[0].actual)
        assertTrue(savedGenerations[1].actual)
    }
}
