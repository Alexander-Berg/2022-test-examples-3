package ru.yandex.market.markup3.overlap

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.clearInvocations
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.core.TaskId
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.core.events.CoreEvent
import ru.yandex.market.markup3.core.executor.TaskGroupExecutor
import ru.yandex.market.markup3.core.executor.TaskGroupExecutorProvider
import ru.yandex.market.markup3.core.services.CreateTask
import ru.yandex.market.markup3.core.services.CreateTaskRequest
import ru.yandex.market.markup3.core.services.SendEvent
import ru.yandex.market.markup3.core.services.TaskFacadeProvider
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.moderation.generator.MappingModerationGenerator
import ru.yandex.market.markup3.mboc.moderation.processor.MappingModerationResultProcessor
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.offertask.service.CancelledTaskWatcher
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.mapping_moderation.ModerationTaskType
import ru.yandex.market.markup3.tasks.mapping_moderation.dto.MappingModerationResultItem
import ru.yandex.market.markup3.tasks.mapping_moderation.dto.MappingModerationStatus
import ru.yandex.market.markup3.tasks.mapping_moderation.dto.MappingModerationTaskResult
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.YangMappingModerationHandler
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInput
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInputData
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationInputDataOffer
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationOutput
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationProperties
import ru.yandex.market.markup3.tasks.overlap.YangToYangMappingModerationOverlapHandler
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.users.profile.TolokaProfileRow
import ru.yandex.market.markup3.users.profile.repository.TolokaProfileRepository
import ru.yandex.market.markup3.utils.CommonObjectMapper
import ru.yandex.market.markup3.yang.TolokaClientMock
import ru.yandex.market.markup3.yang.repositories.TolokaRecoverQueueRepository
import ru.yandex.market.markup3.yang.services.TolokaActiveTasksService
import ru.yandex.market.markup3.yang.services.TolokaResultsDownloader
import ru.yandex.market.markup3.yang.services.TolokaTasksService
import ru.yandex.market.markup3.yang.services.recover.RecoverTaskService
import ru.yandex.market.mbo.http.YangLogStorage
import ru.yandex.market.mbo.http.YangLogStorageService
import ru.yandex.toloka.client.v1.pool.Pool
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

/**
 * @author shadoff
 * created on 02.06.2022
 */
class YangToYangMappingModerationProcessTest : CommonTaskTest() {
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
    lateinit var tolokaFacadeProvider: TaskFacadeProvider

    lateinit var taskGroupExecutor: TaskGroupExecutor
    lateinit var childTaskGroupExecutor: TaskGroupExecutor

    lateinit var taskGroup: TaskGroup
    lateinit var childTaskGroup: TaskGroup

    companion object {
        const val UID = 1234L
        const val WORKER_ID = "worker"
        const val STAFF_LOGIN = "staff"
        const val msku = 1234L
        const val offerId = "5"
    }

    @Before
    fun setUp() {
        val basePool = createBasePool()
        childTaskGroup = taskGroupRegistry.getOrCreateTaskGroup(MbocMappingModerationConstants.YANG_GROUP_KEY) {
            TaskGroup(
                key = MbocMappingModerationConstants.YANG_GROUP_KEY,
                name = "yang mm",
                taskType = TaskType.YANG_MAPPING_MODERATION,
                config = TaskGroupConfig(
                    basePoolId = basePool.id,
                    taskTypeProps = YangMappingModerationProperties(
                        mappingModerationType = YangLogStorage.MappingModerationType.DEDUPLICATION,
                    )
                )
            )
        }
        taskGroup = taskGroupRegistry.getOrCreateTaskGroup("overlap_key") {
            TaskGroup(
                key = "overlap_key",
                name = "overlap yang mm",
                taskType = TaskType.YANG_MAPPING_MODERATION_OVERLAP,
                config = TaskGroupConfig(
                    overlapProperties = TaskGroupConfig.OverlapProperties(
                        minOverlap = 2, //TODO params
                        maxOverlap = 3,
                        confidenceProportion = 0.5f,
                        childTaskGroupKey = childTaskGroup.key
                    )
                )
            )
        }
        taskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(taskGroup, sleepTimeMs = 0)
        childTaskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(childTaskGroup, sleepTimeMs = 0)

        ReflectionTestUtils.setField(yangMappingModerationHandler, "cancelledCheckDelay", 0)
        ReflectionTestUtils.setField(tolokaTasksService, "secondsToCancelGap", 0)

        tolokaProfileRepository.insert(TolokaProfileRow(WORKER_ID, STAFF_LOGIN, UID))

        clearInvocations(yangLogStorageService)
    }

    @After
    fun tearDown() {
        mboCategoryService.setDefaultSaveResponse()
        mboCategoryService.taskOffersMap.clear()
    }

    @Test
    fun `test deduplication with overlap full process`() {
        val tolokaFacade = tolokaFacadeProvider.getTaskFacade(YangToYangMappingModerationOverlapHandler::class.java)
        tolokaFacade.createTasks(
            CreateTaskRequest(
                taskGroupId = taskGroup.id,
                tasks = listOf(
                    CreateTask(
                        input = YangMappingModerationInput(
                            data = YangMappingModerationInputData(
                                offers = listOf(
                                    YangMappingModerationInputDataOffer(
                                        id = 3,
                                        offerId = "5",
                                        categoryId = 9,
                                        categoryName = "categoryName"
                                    )
                                ),
                                taskType = ModerationTaskType.MAPPING_MODERATION,
                                taskSubtype = null,
                            ),
                            categoryId = 9,
                        ),
                    )
                )
            )
        )

        var task = getOnlyOneTask(taskGroup.id)
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        taskGroupExecutor.processSingleEvent()

        var firstChildTask = getOnlyOneTask(childTaskGroup.id)
        processChildTaskWithResult(firstChildTask.id, getSampleOutput(), WORKER_ID) //TODO output + worker

        // ChildResultEvent + ChildLifecycleEvent
        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()

        val activeTasks = taskDbService.findAll()
            .filter { it.taskGroupId == childTaskGroup.id && it.processingStatus == ProcessingStatus.ACTIVE }
        activeTasks.size shouldBe 1
        val secondChildTask = activeTasks[0]

        secondChildTask.id shouldNotBe firstChildTask.id

        processChildTaskWithResult(secondChildTask.id, getSampleOutput(), WORKER_ID) //TODO output + worker

        // ChildResultEvent + ChildLifecycleEvent
        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()

        val allResults = taskResultDbService.findAll()
        allResults.size shouldBe 3
        // child results should be consumed by handler
        allResults.asSequence()
            .filter { it.taskGroupId == childTaskGroup.id }
            .forEach { it.consumed shouldNotBe null }
        // parent results marked consumed after external system consumption
        val parentResults = taskResultDbService.findNotConsumedResults(taskGroup.id, 100)
        parentResults shouldHaveSize 1
        parentResults.forEach { it.consumed shouldBe null }
    }

    @Test
    fun `test overlap main cancelled`() {
        val tolokaFacade = tolokaFacadeProvider.getTaskFacade(YangToYangMappingModerationOverlapHandler::class.java)
        tolokaFacade.createTasks(
            CreateTaskRequest(
                taskGroupId = taskGroup.id,
                tasks = listOf(
                    CreateTask(
                        input = YangMappingModerationInput(
                            data = YangMappingModerationInputData(
                                offers = listOf(
                                    YangMappingModerationInputDataOffer(
                                        id = 3,
                                        offerId = "5",
                                        categoryId = 9,
                                        categoryName = "categoryName"
                                    )
                                ),
                                taskType = ModerationTaskType.MAPPING_MODERATION,
                                taskSubtype = null,
                            ),
                            categoryId = 9,
                        ),
                    )
                )
            )
        )

        var task = getOnlyOneTask(taskGroup.id)
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        taskGroupExecutor.processSingleEvent()

        var firstChildTask = getOnlyOneTask(childTaskGroup.id)
        firstChildTask.processingStatus shouldBe ProcessingStatus.ACTIVE
        childTaskGroupExecutor.processSingleEvent()

        taskEventService.sendEvents(SendEvent(task.id, CoreEvent.Cancel, Instant.now()))
        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask(taskGroup.id)
        task.processingStatus shouldBe ProcessingStatus.CANCELLED

        // Cancel
        childTaskGroupExecutor.processSingleEvent()
        firstChildTask = getOnlyOneTask(childTaskGroup.id)
        firstChildTask.stage shouldBe YangMappingModerationHandler.Stages.CANCELLING.toString()

        tolokaActiveTasksService.updateActiveCache()
        yangResultsDownloader.downloadAllPools()
        // WaitCancelled
        childTaskGroupExecutor.processSingleEvent()

        firstChildTask = getOnlyOneTask(childTaskGroup.id)
        firstChildTask.processingStatus shouldBe ProcessingStatus.CANCELLED
    }

    @Test
    fun `should correctly process overlap for accepted status`() {
        baseOverlapTest(
            listOf(MappingModerationStatus.ACCEPTED, MappingModerationStatus.ACCEPTED),
            MappingModerationStatus.ACCEPTED
        )
    }

    @Test
    fun `should correctly process overlap for rejected status`() {
        baseOverlapTest(
            listOf(MappingModerationStatus.REJECTED, MappingModerationStatus.REJECTED),
            MappingModerationStatus.REJECTED
        )
    }

    @Test
    fun `should correctly process overlap for need_info status`() {
        baseOverlapTest(
            listOf(MappingModerationStatus.NEED_INFO, MappingModerationStatus.NEED_INFO),
            MappingModerationStatus.NEED_INFO
        )
    }

    @Test
    fun `should correctly process overlap for need_info and rejected statuses`() {
        baseOverlapTest(
            listOf(MappingModerationStatus.NEED_INFO, MappingModerationStatus.REJECTED),
            MappingModerationStatus.REJECTED
        )
    }

    @Test
    fun `should correctly process overlap for max overlap`() {
        baseOverlapTest(
            listOf(
                MappingModerationStatus.ACCEPTED,
                MappingModerationStatus.REJECTED,
                MappingModerationStatus.ACCEPTED
            ),
            MappingModerationStatus.ACCEPTED
        )
    }

    @Test
    fun `should correctly process overlap for max overlap 2`() {
        baseOverlapTest(
            listOf(
                MappingModerationStatus.ACCEPTED,
                MappingModerationStatus.REJECTED,
                MappingModerationStatus.NEED_INFO
            ),
            MappingModerationStatus.REJECTED
        )
    }

    @Test
    fun `should correctly process multioffers tasks`() {
        val tolokaFacade = tolokaFacadeProvider.getTaskFacade(YangToYangMappingModerationOverlapHandler::class.java)
        tolokaFacade.createTasks(
            CreateTaskRequest(
                taskGroupId = taskGroup.id,
                tasks = listOf(
                    CreateTask(
                        input = YangMappingModerationInput(
                            data = YangMappingModerationInputData(
                                offers = listOf(
                                    YangMappingModerationInputDataOffer(
                                        id = 3,
                                        offerId = "1",
                                        categoryId = 9,
                                        categoryName = "categoryName"
                                    ),
                                    YangMappingModerationInputDataOffer(
                                        id = 4,
                                        offerId = "2",
                                        categoryId = 9,
                                        categoryName = "categoryName"
                                    ),
                                    YangMappingModerationInputDataOffer(
                                        id = 5,
                                        offerId = "3",
                                        categoryId = 9,
                                        categoryName = "categoryName"
                                    )
                                ),
                                taskType = ModerationTaskType.MAPPING_MODERATION,
                                taskSubtype = null,
                            ),
                            categoryId = 9,
                        ),
                    )
                )
            )
        )

        var task = getOnlyOneTask(taskGroup.id)
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        taskGroupExecutor.processSingleEvent()

        val offerIds = listOf("1", "2", "3")
        val mskus = listOf(123L, 234L, 345L)
        val statuses = listOf(
            listOf(
                MappingModerationStatus.ACCEPTED,
                MappingModerationStatus.NEED_INFO,
                MappingModerationStatus.REJECTED
            ),
            listOf(
                MappingModerationStatus.REJECTED,
                MappingModerationStatus.ACCEPTED,
                MappingModerationStatus.NEED_INFO
            ),
            listOf(MappingModerationStatus.ACCEPTED, MappingModerationStatus.NEED_INFO),
        )
        statuses.forEachIndexed { index, statuses ->
            val workerId = index.toString()
            tolokaProfileRepository.insert(
                TolokaProfileRow(workerId, STAFF_LOGIN+workerId, UID+index+1)
            )
            processChildTaskResult(index.toString()) { getCustomOutput(statuses, mskus, offerIds) }
        }

        val allResults = taskResultDbService.findAll()
        allResults.size shouldBe 4
        // child results should be consumed by handler
        allResults.asSequence()
            .filter { it.taskGroupId == childTaskGroup.id }
            .forEach { it.consumed shouldNotBe null }
        // parent results marked consumed after external system consumption
        val parentResults = taskResultDbService.findNotConsumedResults(taskGroup.id, 100)
        parentResults shouldHaveSize 1
        with(parentResults[0]) {
            val parentTaskResult =
                CommonObjectMapper.treeToValue(this.data.value, MappingModerationTaskResult::class.java)
            parentTaskResult.results shouldHaveSize 3
            parentTaskResult.results[0].offerId shouldBe offerIds[0]
            parentTaskResult.results[0].status shouldBe MappingModerationStatus.ACCEPTED
            parentTaskResult.results[0].msku shouldBe mskus[0]
            parentTaskResult.results[1].offerId shouldBe offerIds[1]
            parentTaskResult.results[1].status shouldBe MappingModerationStatus.NEED_INFO
            parentTaskResult.results[1].msku shouldBe mskus[1]
            parentTaskResult.results[2].offerId shouldBe offerIds[2]
            parentTaskResult.results[2].status shouldBe MappingModerationStatus.REJECTED
            parentTaskResult.results[2].msku shouldBe mskus[2]
            listOf("0","2","1") shouldContainAll listOf(parentTaskResult.workerId)
        }
    }

    private fun baseOverlapTest(
        childStatuses: List<MappingModerationStatus>,
        finalStatus: MappingModerationStatus,
    ) {
        val tolokaFacade = tolokaFacadeProvider.getTaskFacade(YangToYangMappingModerationOverlapHandler::class.java)
        tolokaFacade.createTasks(
            CreateTaskRequest(
                taskGroupId = taskGroup.id,
                tasks = listOf(
                    CreateTask(
                        input = YangMappingModerationInput(
                            data = YangMappingModerationInputData(
                                offers = listOf(
                                    YangMappingModerationInputDataOffer(
                                        id = 3,
                                        offerId = offerId,
                                        categoryId = 9,
                                        categoryName = "categoryName"
                                    )
                                ),
                                taskType = ModerationTaskType.MAPPING_MODERATION,
                                taskSubtype = null,
                            ),
                            categoryId = 9,
                        ),
                    )
                )
            )
        )

        var task = getOnlyOneTask(taskGroup.id)
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        taskGroupExecutor.processSingleEvent()

        childStatuses.forEach { processChildTaskResult { getSampleOutput(it) } }

        val allResults = taskResultDbService.findAll()
        allResults.size shouldBe childStatuses.size + 1
        // child results should be consumed by handler
        allResults.asSequence()
            .filter { it.taskGroupId == childTaskGroup.id }
            .forEach { it.consumed shouldNotBe null }
        // parent results marked consumed after external system consumption
        val parentResults = taskResultDbService.findNotConsumedResults(taskGroup.id, 100)
        parentResults shouldHaveSize 1
        parentResults.forEach {
            val parentTaskResult =
                CommonObjectMapper.treeToValue(it.data.value, MappingModerationTaskResult::class.java)
            parentTaskResult.results shouldHaveSize 1
            parentTaskResult.results[0].offerId shouldBe offerId
            parentTaskResult.results[0].status shouldBe finalStatus
            parentTaskResult.results[0].msku shouldBe msku
        }
    }

    private fun processChildTaskResult(workerId: String = WORKER_ID, outputProvider: () -> Map<String, JsonNode>) {
        val activeTasks = taskDbService.findAll()
            .filter { it.taskGroupId == childTaskGroup.id && it.processingStatus == ProcessingStatus.ACTIVE }
        val childTask = activeTasks[0]

        processChildTaskWithResult(childTask.id, outputProvider.invoke(), workerId)

        // ChildResultEvent + ChildLifecycleEvent
        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()
    }

    private fun processChildTaskWithResult(id: TaskId, result: Map<String, JsonNode>, workerId: String) {
        var task = taskDbService.findById(id)
        task.processingStatus shouldBe ProcessingStatus.ACTIVE

        childTaskGroupExecutor.processSingleEvent()
        task = taskDbService.findById(id)
        task.stage shouldBe YangMappingModerationHandler.Stages.ACTIVE.toString()

        val pool = getOnlyOnePool()
        pool.source shouldBe TolokaSource.YANG
        pool.active shouldBe true

        var taskInfo = getOnlyOneTaskInfo(task.id)
        taskInfo.taskId shouldBe task.id
        taskInfo.tolokaTaskSuiteId shouldNotBe null

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite shouldNotBe null

        val genAssignmentId = tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, result, workerId)
        yangResultsDownloader.downloadAllPools()

        taskInfo = getOnlyOneTaskInfo(task.id)
        taskInfo.solution shouldNotBe null
        taskInfo.assignmentId shouldNotBe null
        taskInfo.workerId shouldNotBe null

        childTaskGroupExecutor.processSingleEvent()
        task = taskDbService.findById(id)
        task.stage shouldBe YangMappingModerationHandler.Stages.SAVING.toString()

        childTaskGroupExecutor.processSingleEvent()
        task = taskDbService.findById(id)
        task.processingStatus shouldBe ProcessingStatus.DONE
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

    private fun getCustomOutput(
        statuses: List<MappingModerationStatus>,
        mskus: List<Long>,
        offerIds: List<String>
    ): Map<String, JsonNode> {
        val mapper = jacksonObjectMapper()

        val results = statuses.mapIndexed { index, status ->
            MappingModerationResultItem(
                index.toLong(), mskus[index], status, offerIds[index],
                listOf(), null, null, null
            )
        }
        val output = mapper.convertValue(YangMappingModerationOutput(results), JsonNode::class.java)
        return mapOf(
            "output" to output
        )
    }

    private fun getSampleOutput(
        status: MappingModerationStatus = MappingModerationStatus.ACCEPTED
    ): Map<String, JsonNode> {
        val mapper = jacksonObjectMapper()

        val results: List<MappingModerationResultItem> = listOf(
            MappingModerationResultItem(
                1, msku, status, offerId,
                listOf(), null, null, null
            )
        )
        val output = mapper.convertValue(YangMappingModerationOutput(results), JsonNode::class.java)
        return mapOf(
            "output" to output
        )
    }
}
