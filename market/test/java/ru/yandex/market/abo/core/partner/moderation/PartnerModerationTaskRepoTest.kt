package ru.yandex.market.abo.core.partner.moderation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.partner.moderation.ModerationTaskStatus.IN_PROGRESS
import ru.yandex.market.abo.api.entity.partner.moderation.ModerationTaskStatus.SUCCESSFULLY_FINISHED
import ru.yandex.market.abo.api.entity.partner.moderation.ModerationType.FBS_LITE

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 24.03.2022
 */
class PartnerModerationTaskRepoTest @Autowired constructor(
    private val repo: PartnerModerationTaskRepo
): EmptyTest() {
    @Test
    fun `serialization test`() {
        val moderationTask = PartnerModerationTask(PARTNER_ID, FBS_LITE, "")
        repo.save(moderationTask)
        flushAndClear()

        assertThat(repo.findActiveByPartnerIdAndType(PARTNER_ID, FBS_LITE))
            .usingRecursiveComparison()
            .isEqualTo(moderationTask)
    }

    @Test
    fun `update status test`() {
        val moderationTask = PartnerModerationTask(PARTNER_ID, FBS_LITE, "")
        repo.save(moderationTask)
        flushAndClear()
        val verdictComment = "Все еще плохо"
        repo.saveTaskVerdict(moderationTask.id, SUCCESSFULLY_FINISHED, verdictComment)
        flushAndClear()

        val savedTask = repo.findByIdOrNull(moderationTask.id)
        assertNotNull(savedTask)
        assertEquals(SUCCESSFULLY_FINISHED, savedTask!!.status)
        assertEquals(verdictComment, savedTask.verdictComment)
    }

    @Test
    fun `update process id test`() {
        val moderationTask = PartnerModerationTask(PARTNER_ID, FBS_LITE, "")
        repo.save(moderationTask)
        flushAndClear()
        repo.saveRelatedProcess(moderationTask.id, 15L, IN_PROGRESS)
        flushAndClear()

        val savedTask = repo.findByIdOrNull(moderationTask.id)
        assertEquals(15L, savedTask?.relatedProcessId)
        assertEquals(IN_PROGRESS, savedTask?.status)
    }

    companion object {
        private const val PARTNER_ID = 123L
    }
}
