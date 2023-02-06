package ru.yandex.market.markup3.mboc.offertask.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.mboc.blueclassification.MbocBlueClassificationConstants
import ru.yandex.market.markup3.mboc.bluelogs.MbocBlueLogsConstants
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTask
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskError
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskKey
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus.WAITING_FOR_RESULTS
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.testutils.BaseAppTest

class OfferTaskRepositoryTest : BaseAppTest() {
    @Autowired
    private lateinit var repository: OfferTaskRepository

    @Test
    fun `compound key works`() {
        val tasks = listOf(
            OfferTask(
                TaskType.BLUE_LOGS,
                groupKey = MbocBlueLogsConstants.GROUP_KEY,
                1,
                WAITING_FOR_RESULTS,
                1,
                10,
                OfferTaskError("A", "msg")
            ),
            OfferTask(TaskType.BLUE_LOGS, groupKey = MbocBlueLogsConstants.GROUP_KEY, 2, WAITING_FOR_RESULTS, 20, 20),
            OfferTask(
                TaskType.BLUE_CLASSIFICATION,
                groupKey = MbocBlueClassificationConstants.GROUP_KEY,
                1,
                WAITING_FOR_RESULTS,
                30,
                30,
                OfferTaskError("A", "msg")
            ),
            OfferTask(
                TaskType.YANG_MAPPING_MODERATION,
                groupKey = MbocMappingModerationConstants.YANG_GROUP_KEY,
                2,
                WAITING_FOR_RESULTS,
                40,
                40
            )
        )
        repository.insertBatch(tasks)

        tasks.forEach { task ->
            val fromRepo = repository.findByKey(OfferTaskKey(task.taskType, task.groupKey, task.offerId))
            fromRepo shouldNotBe null
            fromRepo!!
            fromRepo.key shouldBe task.key
            fromRepo.status shouldBe task.status
            fromRepo.taskId shouldBe task.taskId
            fromRepo.ticketId shouldBe task.ticketId
            fromRepo.error shouldBe task.error
        }
    }

    @After
    fun cleanup() {
        repository.deleteAll()
    }
}
