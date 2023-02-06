package ru.yandex.market.markup3.mboc.offertask.service

import com.fasterxml.jackson.databind.node.TextNode
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.TaskId
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.ProcessingStatus.ACTIVE
import ru.yandex.market.markup3.core.dto.ProcessingStatus.CANCELLED
import ru.yandex.market.markup3.core.dto.TaskRow
import ru.yandex.market.markup3.core.repositories.TaskDbService
import ru.yandex.market.markup3.mboc.blueclassification.MbocBlueClassificationConstants
import ru.yandex.market.markup3.mboc.bluelogs.MbocBlueLogsConstants
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTask
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskKey
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus.CANCELLING
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus.WAITING_FOR_RESULTS
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import kotlin.properties.Delegates

class CancelledTaskWatcherTest : BaseAppTest() {
    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    private var offerTaskService by Delegates.notNull<OfferTaskService>()

    private var taskDbService by Delegates.notNull<TaskDbService>()

    private var watcher by Delegates.notNull<CancelledTaskWatcher>()

    @Before
    fun setUp() {
        offerTaskService = OfferTaskService(offerTaskRepository, TransactionHelper.MOCK)
        taskDbService = mock()
        watcher = CancelledTaskWatcher(offerTaskService, taskDbService, TransactionHelper.MOCK)
    }

    @Test
    fun `removes only really cancelled tasks`() {
        offerTaskRepository.deleteAll()

        offerTaskRepository.insertBatch(
            listOf(
                offerTask(1, TaskType.BLUE_LOGS, WAITING_FOR_RESULTS, 1, MbocBlueLogsConstants.GROUP_KEY),
                offerTask(2, TaskType.BLUE_CLASSIFICATION, CANCELLING, 2, MbocBlueClassificationConstants.GROUP_KEY),
                offerTask(3, TaskType.YANG_MAPPING_MODERATION, CANCELLING, 3, MbocMappingModerationConstants.YANG_GROUP_KEY),
            )
        )
        doReturn(
            listOf(
                taskRow(1, ACTIVE),
                taskRow(2, CANCELLED),
                taskRow(3, ACTIVE),
            )
        ).`when`(taskDbService).findByIds(eq(listOf(2, 3)))

        watcher.watchForCancelled()

        offerTaskRepository.findAll().map { it.key } shouldContainExactlyInAnyOrder listOf(
            OfferTaskKey(TaskType.BLUE_LOGS, MbocBlueLogsConstants.GROUP_KEY, 1),
            OfferTaskKey(TaskType.YANG_MAPPING_MODERATION, MbocMappingModerationConstants.YANG_GROUP_KEY, 3),
        )
    }

    @After
    fun cleanup() {
        offerTaskRepository.deleteAll()
    }

    private fun offerTask(oId: Long, type: TaskType, status: OfferTaskStatus, taskId: TaskId, groupKey: String) = OfferTask(
        taskType = type,
        groupKey = groupKey,
        offerId = oId,
        status = status,
        taskId = taskId,
        ticketId = 0,
    )

    private fun taskRow(id: TaskId, processingStatus: ProcessingStatus) = TaskRow(
        id = id,
        processingStatus = processingStatus,
        state = TextNode("1"),
        stage = "1",
        taskGroupId = 1,
    )
}
