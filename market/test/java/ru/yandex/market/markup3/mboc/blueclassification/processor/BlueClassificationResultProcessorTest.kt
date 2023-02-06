package ru.yandex.market.markup3.mboc.blueclassification.processor

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3ApiService
import ru.yandex.market.markup3.core.TaskId
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskRow
import ru.yandex.market.markup3.core.repositories.TaskDbService
import ru.yandex.market.markup3.core.services.NewTaskResult
import ru.yandex.market.markup3.core.services.TaskGroupRegistry
import ru.yandex.market.markup3.core.services.TaskResultService
import ru.yandex.market.markup3.grpc.services.api.converters.PollResultsConverterHelper
import ru.yandex.market.markup3.grpc.services.api.converters.TaskGroupTypeAndKey
import ru.yandex.market.markup3.mboc.blueclassification.MbocBlueClassificationConstants
import ru.yandex.market.markup3.mboc.blueclassification.saver.BlueClassificationSaver
import ru.yandex.market.markup3.mboc.blueclassification.saver.BlueClassificationSaver.ResultToSave
import ru.yandex.market.markup3.mboc.blueclassification.saver.BlueClassificationSaver.SaveResultsResponse
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTask
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus
import ru.yandex.market.markup3.mboc.offertask.service.OfferTaskService
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.blueclassification.dto.BlueClassificationOutput
import ru.yandex.market.markup3.tasks.blueclassification.dto.BlueClassificationResult
import ru.yandex.market.markup3.tasks.blueclassification.dto.Comment
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.markup3.utils.CommonObjectMapper
import ru.yandex.market.mbo.storage.StorageKeyValueService
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import kotlin.properties.Delegates

class BlueClassificationResultProcessorTest : BaseAppTest() {
    @Autowired
    private lateinit var taskGroupRegistry: TaskGroupRegistry

    @Autowired
    private lateinit var keyValueService: StorageKeyValueService

    @Autowired
    private lateinit var taskDbService: TaskDbService

    @Autowired
    private lateinit var taskResultService: TaskResultService

    @Autowired
    private lateinit var markup3ApiService: Markup3ApiService

    @Autowired
    private lateinit var offerTaskService: OfferTaskService

    private lateinit var taskGroup: TaskGroup

    private var blueClassificationSaver by Delegates.notNull<BlueClassificationSaver>()

    private var processor by Delegates.notNull<BlueClassificationResultProcessor>()

    companion object {
        const val STAFF_LOGIN = "staff"
    }

    @Before
    fun setup() {

        taskGroup = taskGroupRegistry.getOrCreateTaskGroup(MbocBlueClassificationConstants.GROUP_KEY) {
            TaskGroup(
                key = MbocBlueClassificationConstants.GROUP_KEY,
                name = MbocBlueClassificationConstants.GROUP_KEY,
                taskType = TaskType.BLUE_CLASSIFICATION,
            )
        }
        blueClassificationSaver = mock()
        processor = BlueClassificationResultProcessor(
            keyValueService, TransactionHelper.MOCK, markup3ApiService, offerTaskService, blueClassificationSaver
        )
    }

    @Test
    fun `Test final results`() {
        val tasks = taskDbService.insertTasks(generateTasks(3))
        val task1 = tasks[0]
        val task2 = tasks[1]
        val task3 = tasks[2]

        val baseOfferTask = OfferTask(
            TaskType.BLUE_CLASSIFICATION, "blue_classification",
            0, OfferTaskStatus.WAITING_FOR_RESULTS, 0, 0, null
        )
        offerTaskService.createOfferTasks(
            listOf(
                baseOfferTask.copy(taskId = task1.id, offerId = 1),
                baseOfferTask.copy(taskId = task1.id, offerId = 2),
                baseOfferTask.copy(taskId = task2.id, offerId = 3),
                baseOfferTask.copy(taskId = task2.id, offerId = 4),
                baseOfferTask.copy(taskId = task3.id, offerId = 5),
            )
        )

        doReturn(SaveResultsResponse(saved = setOf(), failed = mapOf())).`when`(blueClassificationSaver)
            .saveResults(any())

        val results = listOf(
            // Test case: one task with two results, and one task with single result
            newResult(
                task1.id,
                BlueClassificationOutput(1, "1", "1", 1, emptyList()),
                BlueClassificationOutput(2, "2", "1", 2, emptyList()),
            ),
            newResult(
                task2.id,
                BlueClassificationOutput(3, "3", "1", 3, emptyList()),
                BlueClassificationOutput(4, "4", "1", null, listOf(Comment("test", listOf("item")))),
            ),
            newResult(
                task3.id,
                BlueClassificationOutput(5, "5", "2", 1, emptyList()),
            ),
        )
        taskResultService.storeResults(taskGroup.id, results)
        val taskResults = results.flatMap { taskResultService.findResults<BlueClassificationResult>(it.taskId) }
        val converted = PollResultsConverterHelper.convert(
            taskResults, TaskGroupTypeAndKey(taskGroup.taskType, taskGroup.key)
        ).resultsList.associate { it.taskid to it.result.blueClassificationResult }

        processor.process()

        val saveRqCaptor = argumentCaptor<Collection<ResultToSave>>()
        verify(blueClassificationSaver, times(1)).saveResults(saveRqCaptor.capture())

        val saveRq = saveRqCaptor.lastValue
        saveRq shouldHaveSize 3
        saveRq.first { it.taskId == task1.id }.also {
            it shouldBe ResultToSave(it.resultId, task1.id, converted[task1.id]!!)
        }
        saveRq.first { it.taskId == task2.id }.also {
            it shouldBe ResultToSave(it.resultId, task2.id, converted[task2.id]!!)
        }
        saveRq.first { it.taskId == task3.id }.also {
            it shouldBe ResultToSave(it.resultId, task3.id, converted[task3.id]!!)
        }
    }

    @After
    fun cleanup() {
        keyValueService.invalidateCache()
    }

    private fun newResult(taskId: TaskId, vararg output: BlueClassificationOutput) =
        NewTaskResult(taskId, BlueClassificationResult(STAFF_LOGIN, output.asList()))

    private fun generateTasks(count: Int) = generateSequence(0) { i -> (i + 1).takeIf { it < count } }.map {
        TaskRow(
            taskGroupId = taskGroup.id,
            stage = "stage",
            state = CommonObjectMapper.valueToTree("q")
        )
    }.toList()

}
