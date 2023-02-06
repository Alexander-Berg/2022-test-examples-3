package ru.yandex.market.markup3.api

import io.kotest.matchers.collections.shouldContainInOrder
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.TaskGroupId
import ru.yandex.market.markup3.core.dto.Related
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskResultRow
import ru.yandex.market.markup3.core.dto.TaskRow
import ru.yandex.market.markup3.core.repositories.TaskDbService
import ru.yandex.market.markup3.core.repositories.TaskResultDbService
import ru.yandex.market.markup3.core.services.TaskGroupRegistry
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.bluelogs.dto.BlueLogsResult
import ru.yandex.market.markup3.tasks.bluelogs.dto.MappingStatuses
import ru.yandex.market.markup3.tasks.bluelogs.dto.TaskResult
import ru.yandex.market.markup3.tasks.bluelogs.dto.TaskSolution
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.markup3.utils.CommonObjectMapper

class Markup3ApiServiceImplTest : BaseAppTest() {

    @Autowired
    private lateinit var taskGroupRegistry: TaskGroupRegistry
    @Autowired
    private lateinit var taskDbService: TaskDbService
    @Autowired
    private lateinit var markup3ApiServiceImpl: Markup3ApiServiceImpl
    @Autowired
    private lateinit var taskResultDbService: TaskResultDbService

    private val groupKey = "abc"
    private val taskType = Markup3Api.TaskType.BLUE_LOGS

    @Before
    fun setUp() {
        taskGroupRegistry.getOrCreateTaskGroup(groupKey) {
            TaskGroup(
                key = groupKey,
                name = "test",
                taskType = TaskType.BLUE_LOGS,
            )
        }
    }

    @Test
    fun `poll least read results should increase counters`() {
        val expectedResults = mutableListOf<TaskResultRow>()
        val taskGroupId = taskGroupRegistry.getTaskGroupOrThrow(TaskType.BLUE_LOGS, groupKey).id
        expectedResults.addAll(generateTasksAndResults(1, 3, taskGroupId))
        val lastResults = generateTasksAndResults(4, 6, taskGroupId, 10)
        expectedResults.addAll(generateTasksAndResults(7, 9, taskGroupId, 2))
        expectedResults.addAll(lastResults)

        val response = markup3ApiServiceImpl.pollOrderedResults(
            Markup3Api.TasksResultPollRequest.newBuilder().apply {
                setTaskTypeIdentity(
                    Markup3Api.TaskTypeIdentity.newBuilder().apply {
                        setGroupKey(this@Markup3ApiServiceImplTest.groupKey)
                        setType(taskType)
                    }.build()
                )
                setCount(10)
            }.build()
        )

        response.resultsList.map{ it.taskResultId } shouldContainInOrder expectedResults.map { it.id }
        taskResultDbService.findLeastPolledNotConsumedResults(taskGroupId, 10).map {
            it.id to it.pollingCounter
        } shouldContainInOrder expectedResults.map { it.id to ((it.pollingCounter ?: 0) + 1)}
    }

    private fun generateTasksAndResults(from:Int, to: Int, taskGroupId: TaskGroupId, readAttempts: Long? = null
    ): List<TaskResultRow> {
        val taskSolution =
            TaskSolution(listOf(TaskResult(offer_id = "1", req_id = "123", offer_mapping_status = MappingStatuses.MAPPED)))
        return (from..to).map {
            TaskRow(taskGroupId = taskGroupId,  stage = "stage", state = CommonObjectMapper.valueToTree("q"))
        }.let {
            taskDbService.insertTasks(it)
        }.map {
            TaskResultRow(
                taskGroupId = taskGroupId,
                taskId = it.id,
                pollingCounter = readAttempts,
                data = Related.Value(
                    CommonObjectMapper.valueToTree(
                        BlueLogsResult("testLogin", 1, taskSolution)
                    )
                )
            )
        }.let {
            taskResultDbService.insertResults(it)
        }
    }
}
