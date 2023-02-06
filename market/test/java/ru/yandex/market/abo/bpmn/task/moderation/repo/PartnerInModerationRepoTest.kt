package ru.yandex.market.abo.bpmn.task.moderation.repo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.bpmn.AbstractFunctionalTest
import ru.yandex.market.abo.bpmn.task.moderation.model.ModerationStatus.INITIALIZED
import ru.yandex.market.abo.bpmn.task.moderation.model.ModerationType.LITE_MODERATION
import ru.yandex.market.abo.bpmn.task.moderation.model.PartnerInModeration
import ru.yandex.market.abo.bpmn.task.moderation.model.PartnerProgramType.DROPSHIP

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 18.03.2022
 */
open class PartnerInModerationRepoTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var partnerInModerationRepo: PartnerInModerationRepo

    @Test
    fun `repo save test`() {
        partnerInModerationRepo.createModeration(PARTNER_ID, DROPSHIP, LITE_MODERATION, SYNOPSIS)

        val moderation = partnerInModerationRepo.getModeration(PARTNER_ID, LITE_MODERATION)
        validateModeration(moderation)
    }

    @Test
    fun `repo delete test`() {
        val moderation = partnerInModerationRepo.createModeration(PARTNER_ID, DROPSHIP, LITE_MODERATION, SYNOPSIS)
        partnerInModerationRepo.deleteModeration(moderation.id)

        assertTrue(partnerInModerationRepo.getModerations(PARTNER_ID).isEmpty())
    }

    private fun validateModeration(moderation: PartnerInModeration?) {
        assertNotNull(moderation)
        assertEquals(PARTNER_ID, moderation!!.partnerId)
        assertEquals(DROPSHIP, moderation.partnerProgramType)
        assertEquals(LITE_MODERATION, moderation.moderationType)
        assertEquals(INITIALIZED, moderation.moderationStatus)
        assertEquals(SYNOPSIS, moderation.synopsis)
    }

    companion object {
        private const val PARTNER_ID = 123L
        private const val SYNOPSIS = "Косяк"
    }
}
