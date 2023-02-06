package ru.yandex.market.markup3.mboc.offertask.service

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.mboc.blueclassification.MbocBlueClassificationConstants
import ru.yandex.market.markup3.mboc.bluelogs.MbocBlueLogsConstants
import ru.yandex.market.markup3.mboc.offertask.OfferTaskFilter
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTask
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.testutils.BaseAppTest

class OfferTaskServiceTest : BaseAppTest() {
    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    @Autowired
    private lateinit var offerTaskService: OfferTaskService

    @Test
    fun `iterateOfferTasks works`() {
        val tasks = listOf(
            OfferTask(
                TaskType.BLUE_LOGS,
                groupKey = MbocBlueLogsConstants.GROUP_KEY,
                1,
                OfferTaskStatus.CANCELLING,
                201,
                201
            ),
            OfferTask(
                TaskType.BLUE_LOGS,
                groupKey = MbocBlueLogsConstants.GROUP_KEY,
                2,
                OfferTaskStatus.CANCELLING,
                202,
                202
            ),
            OfferTask(
                TaskType.BLUE_LOGS,
                groupKey = MbocBlueLogsConstants.GROUP_KEY,
                3,
                OfferTaskStatus.CANCELLING,
                203,
                203
            ),
            OfferTask(
                TaskType.BLUE_LOGS,
                groupKey = MbocBlueLogsConstants.GROUP_KEY,
                4,
                OfferTaskStatus.CANCELLING,
                204,
                204
            ),
            OfferTask(
                TaskType.BLUE_CLASSIFICATION,
                groupKey = MbocBlueClassificationConstants.GROUP_KEY,
                2,
                OfferTaskStatus.WAITING_FOR_RESULTS,
                30,
                30
            ),
        )
        offerTaskRepository.insertBatch(tasks)

        val iterations = mutableListOf<List<OfferTask>>()
        offerTaskService.iterateOfferTasks(OfferTaskFilter().statuses(OfferTaskStatus.CANCELLING), 1) {
            iterations.add(it)
        }
        iterations shouldHaveSize 4
        iterations.map { it.size } shouldContainInOrder listOf(1, 1, 1, 1)
        iterations.map { it.first().offerId } shouldContainInOrder listOf(1, 2, 3, 4)
    }
}
