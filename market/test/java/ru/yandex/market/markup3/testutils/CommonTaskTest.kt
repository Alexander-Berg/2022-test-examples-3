package ru.yandex.market.markup3.testutils

import io.kotest.assertions.forEachAsClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.After
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.TaskGroupId
import ru.yandex.market.markup3.core.TaskId
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.core.dto.TaskRow
import ru.yandex.market.markup3.core.repositories.TaskDbService
import ru.yandex.market.markup3.core.repositories.TaskEventDbService
import ru.yandex.market.markup3.core.repositories.TaskGroupRepository
import ru.yandex.market.markup3.core.repositories.TaskResultDbService
import ru.yandex.market.markup3.core.services.CreateTask
import ru.yandex.market.markup3.core.services.CreateTaskRequest
import ru.yandex.market.markup3.core.services.CreateTaskResult
import ru.yandex.market.markup3.core.services.TaskEventService
import ru.yandex.market.markup3.core.services.TaskGroupRegistry
import ru.yandex.market.markup3.core.services.TaskResultService
import ru.yandex.market.markup3.core.services.TaskService
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.testutils.handlers.TestHandler
import ru.yandex.market.markup3.testutils.handlers.TestInput
import ru.yandex.market.markup3.yang.dto.TolokaPoolInfo
import ru.yandex.market.markup3.yang.dto.TolokaTaskInfo
import ru.yandex.market.markup3.yang.repositories.TolokaPoolInfoRepository
import ru.yandex.market.markup3.yang.repositories.TolokaTaskInfoRepository
import ru.yandex.market.mbo.storage.StorageKeyValueService

/**
 * Все зависимости под рукой + методы для создания моков.
 */
abstract class CommonTaskTest : BaseAppTest() {
    @Autowired
    lateinit var taskGroupRepository: TaskGroupRepository

    @Autowired
    lateinit var taskDbService: TaskDbService

    @Autowired
    lateinit var taskEventDbService: TaskEventDbService

    @Autowired
    lateinit var taskEventService: TaskEventService

    @Autowired
    lateinit var testHandler: TestHandler

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var taskResultService: TaskResultService

    @Autowired
    lateinit var taskResultDbService: TaskResultDbService

    @Autowired
    lateinit var taskGroupRegistry: TaskGroupRegistry

    @Autowired
    lateinit var tolokaTaskInfoRepository: TolokaTaskInfoRepository

    @Autowired
    lateinit var tolokaPoolInfoRepository: TolokaPoolInfoRepository

    @Autowired
    lateinit var keyValueService: StorageKeyValueService

    @After
    fun commonReset() {
        testHandler.reset()
        keyValueService.invalidateCache()
        taskGroupRegistry.refresh()
    }

    protected fun createTestTask(
        taskGroupId: TaskGroupId, input: Any = TestInput(), parentTaskId: TaskId? = null
    ): TaskId {
        val result = taskService.createTasks(
            CreateTaskRequest(taskGroupId, listOf(CreateTask(input, parentTaskId = parentTaskId)))
        )
        result.results shouldHaveSize 1
        result.results.forEachAsClue {
            it.result shouldBe CreateTaskResult.OK
        }

        return result.results[0].taskId!!
    }

    protected fun getDefaultName(id: TaskGroupId, taskType: String): String {
        return "$taskType #$id"
    }

    protected fun getOnlyOneTaskInfo(): TolokaTaskInfo {
        val taskInfos = tolokaTaskInfoRepository.findAll()
        taskInfos shouldHaveSize 1
        return taskInfos[0]
    }

    protected fun getOnlyOneTaskInfo(taskId: TaskId): TolokaTaskInfo {
        val taskInfos = tolokaTaskInfoRepository.findByTaskId(taskId)
        taskInfos shouldHaveSize 1
        return taskInfos[0]
    }

    protected fun getOnlyOneTask(): TaskRow {
        val tasks = taskDbService.findAll()
        tasks shouldHaveSize 1
        return tasks[0]
    }

    protected fun getOnlyOneTask(taskGroupId: TaskGroupId): TaskRow {
        val tasks = taskDbService.findAll().filter { it.taskGroupId == taskGroupId }
        tasks shouldHaveSize 1
        return tasks[0]
    }

    protected fun getOnlyOnePool(): TolokaPoolInfo {
        val pools = tolokaPoolInfoRepository.findAll()
        pools shouldHaveSize 1
        return pools[0]
    }

    protected fun createTestTaskGroup(config: TaskGroupConfig? = null): TaskGroup {
        return taskGroupRegistry.getOrCreateTaskGroup("test_key") {
            TaskGroup(
                key = "test_key",
                name = "Test task group",
                taskType = TaskType.DICE,
                config = config ?: TaskGroupConfig()
            )
        }
    }
}
