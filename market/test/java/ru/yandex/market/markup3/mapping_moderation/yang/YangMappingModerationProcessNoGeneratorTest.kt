package ru.yandex.market.markup3.mapping_moderation.yang

import com.nhaarman.mockitokotlin2.clearInvocations
import io.kotest.assertions.forEachAsClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.core.executor.TaskGroupExecutor
import ru.yandex.market.markup3.core.executor.TaskGroupExecutorProvider
import ru.yandex.market.markup3.core.services.CreateTask
import ru.yandex.market.markup3.core.services.CreateTaskRequest
import ru.yandex.market.markup3.core.services.CreateTaskResult
import ru.yandex.market.markup3.core.services.TaskService.Companion.INIT_STAGE
import ru.yandex.market.markup3.mboc.category.info.CategoryInfo
import ru.yandex.market.markup3.mboc.category.info.repository.CategoryInfoRepository
import ru.yandex.market.markup3.mboc.category.info.service.CategoryInfoService
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.moderation.generator.MappingModerationGenerator
import ru.yandex.market.markup3.mboc.moderation.processor.MappingModerationResultProcessor
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.offertask.service.CancelledTaskWatcher
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.mapping_moderation.ModerationTaskType
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.YangMappingModerationHandler
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInput
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInputData
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInputDataOffer
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.users.profile.TolokaProfileRow
import ru.yandex.market.markup3.users.profile.repository.TolokaProfileRepository
import ru.yandex.market.markup3.yang.TolokaClientMock
import ru.yandex.market.markup3.yang.repositories.TolokaRecoverQueueRepository
import ru.yandex.market.markup3.yang.services.TolokaActiveTasksService
import ru.yandex.market.markup3.yang.services.TolokaResultsDownloader
import ru.yandex.market.markup3.yang.services.TolokaTasksService
import ru.yandex.market.markup3.yang.services.YangTraitsAndSkillsService
import ru.yandex.market.markup3.yang.services.recover.RecoverTaskService
import ru.yandex.market.mbo.http.YangLogStorage
import ru.yandex.market.mbo.http.YangLogStorageService
import ru.yandex.toloka.client.v1.pool.Pool
import java.math.BigDecimal
import java.util.Date

/**
 * @author shadoff
 * created on 7/27/21
 */
class YangMappingModerationProcessNoGeneratorTest : CommonTaskTest() {
    @Autowired
    lateinit var taskGroupExecutorProvider: TaskGroupExecutorProvider

    @Autowired
    lateinit var tolokaTasksService: TolokaTasksService

    @Autowired
    lateinit var tolokaClientMock: TolokaClientMock

    @Autowired
    lateinit var yangResultsDownloader: TolokaResultsDownloader

    @Autowired
    lateinit var yangLogStorageService: YangLogStorageService

    @Autowired
    lateinit var tolokaActiveTasksService: TolokaActiveTasksService

    @Autowired
    lateinit var tolokaRecoverQueueRepository: TolokaRecoverQueueRepository

    @Autowired
    lateinit var recoverTaskService: RecoverTaskService

    @Autowired
    lateinit var cancelledTaskWatcher: CancelledTaskWatcher

    @Autowired
    lateinit var yangMappingModerationHandler: YangMappingModerationHandler

    @Autowired
    lateinit var mappingModerationGenerator: MappingModerationGenerator

    @Autowired
    lateinit var resultProcessor: MappingModerationResultProcessor

    @Autowired
    lateinit var offerTaskRepository: OfferTaskRepository

    @Autowired
    lateinit var mboCategoryService: MboCategoryServiceMock

    @Autowired
    lateinit var tolokaProfileRepository: TolokaProfileRepository

    @Autowired
    private lateinit var yangTraitsAndSkillsService: YangTraitsAndSkillsService

    @Autowired
    private lateinit var categoryInfoRepository: CategoryInfoRepository

    @Autowired
    private lateinit var categoryInfoService: CategoryInfoService

    lateinit var taskGroupExecutor: TaskGroupExecutor

    lateinit var taskGroup: TaskGroup

    companion object {
        const val UID = 1234L
        const val WORKER_ID = "worker"
        const val STAFF_LOGIN = "staff"
    }

    @Before
    fun setUp() {
        val basePool = createBasePool()
        taskGroup = taskGroupRegistry.getOrCreateTaskGroup(MbocMappingModerationConstants.YANG_GROUP_KEY) {
            TaskGroup(
                key = MbocMappingModerationConstants.YANG_GROUP_KEY,
                name = "yang mm",
                taskType = TaskType.YANG_MAPPING_MODERATION,
                config = TaskGroupConfig(
                    basePoolId = basePool.id,
                )
            )
        }
        taskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(taskGroup, sleepTimeMs = 0)

        ReflectionTestUtils.setField(yangMappingModerationHandler, "cancelledCheckDelay", 0)
        ReflectionTestUtils.setField(tolokaTasksService, "secondsToCancelGap", 0)

        tolokaProfileRepository.insert(TolokaProfileRow(WORKER_ID, STAFF_LOGIN, UID))

        clearInvocations(yangLogStorageService)

        categoryInfoRepository.insert(
            CategoryInfo(
                hid = 4L, parentHid = 1L, name = "qwe", uniqueName = null,
                isNotUsed = false, isPublished = true, isAcceptGoodContent = true, isLeaf = true,
                inCategory = null, outOfCategory = null,
            )
        )
        categoryInfoService.invalidateCache()
    }

    @After
    fun tearDown() {
        mboCategoryService.setDefaultSaveResponse()
        mboCategoryService.taskOffersMap.clear()
    }


    @Test
    fun `test mapping moderation group trait`() {
        val input = YangMappingModerationInput(
            categoryId = -1,
            categoryGroups = listOf(1L),
            data = YangMappingModerationInputData(
                taskType = ModerationTaskType.MAPPING_MODERATION,
                taskSubtype = null,
                offers = listOf(
                    YangMappingModerationInputDataOffer(
                        id = 1,
                        offerId = "1",
                        categoryId = 11L,
                        categoryName = "qwe",
                    )
                )
            ),
        )

        val result = taskService.createTasks(
            CreateTaskRequest(taskGroup.id, listOf(CreateTask(input)))
        )
        result.results shouldHaveSize 1
        result.results.forEachAsClue {
            it.result shouldBe CreateTaskResult.OK
        }

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe INIT_STAGE

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.ACTIVE.toString()

        val pool = getOnlyOnePool()
        pool.source shouldBe TolokaSource.YANG
        pool.active shouldBe true

        var taskInfo = getOnlyOneTaskInfo()
        taskInfo.taskId shouldBe task.id
        taskInfo.tolokaTaskSuiteId shouldNotBe null

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite shouldNotBe null

        val groupTrait = yangTraitsAndSkillsService.getTraitForGroup(1L,
            YangLogStorage.YangTaskType.MAPPING_MODERATION, taskGroup)
        taskSuite.traitsAnyOf shouldContainExactlyInAnyOrder listOf(groupTrait)
    }

    @Test
    fun `test mapping moderation group trait rollback`() {
        val input = YangMappingModerationInput(
            categoryId = 4,
            categoryGroups = listOf(),
            data = YangMappingModerationInputData(
                taskType = ModerationTaskType.MAPPING_MODERATION,
                taskSubtype = null,
                offers = listOf(
                    YangMappingModerationInputDataOffer(
                        id = 1,
                        offerId = "1",
                        categoryId = 11L,
                        categoryName = "qwe",
                    )
                )
            ),
        )

        val result = taskService.createTasks(
            CreateTaskRequest(taskGroup.id, listOf(CreateTask(input)))
        )
        result.results shouldHaveSize 1
        result.results.forEachAsClue {
            it.result shouldBe CreateTaskResult.OK
        }

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe INIT_STAGE

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.ACTIVE.toString()

        val pool = getOnlyOnePool()
        pool.source shouldBe TolokaSource.YANG
        pool.active shouldBe true

        var taskInfo = getOnlyOneTaskInfo()
        taskInfo.taskId shouldBe task.id
        taskInfo.tolokaTaskSuiteId shouldNotBe null

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite shouldNotBe null

        val traits = yangTraitsAndSkillsService.getTraitsForCategory(4L,
            YangLogStorage.YangTaskType.MAPPING_MODERATION, taskGroup, false)!!
        taskSuite.traitsAnyOf shouldContainExactlyInAnyOrder traits
    }

    private fun createBasePool(): Pool = tolokaClientMock.createPool(
        Pool(
            "prj",
            "pr_name",
            true,
            Date(),
            BigDecimal.ONE,
            1,
            true,
            null
        )
    ).result
}
