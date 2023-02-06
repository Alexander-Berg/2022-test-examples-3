package ru.yandex.market.markup3.mapping_moderation.yang

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotHaveSize
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.core.executor.TaskGroupExecutor
import ru.yandex.market.markup3.core.executor.TaskGroupExecutorProvider
import ru.yandex.market.markup3.core.services.TaskService.Companion.INIT_STAGE
import ru.yandex.market.markup3.mboc.OfferId
import ru.yandex.market.markup3.mboc.category.info.CategoryInfo
import ru.yandex.market.markup3.mboc.category.info.repository.CategoryInfoRepository
import ru.yandex.market.markup3.mboc.category.info.service.CategoryInfoService
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants
import ru.yandex.market.markup3.mboc.moderation.generator.AbstractMappingModerationGenerator.Companion.convertMbocPriority
import ru.yandex.market.markup3.mboc.moderation.generator.MappingModerationGenerator
import ru.yandex.market.markup3.mboc.moderation.processor.MappingModerationResultProcessor
import ru.yandex.market.markup3.mboc.offertask.OfferTaskFilter
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.offertask.service.CancelledTaskWatcher
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.YangLogSaver
import ru.yandex.market.markup3.tasks.mapping_moderation.dto.MappingModerationResultItem
import ru.yandex.market.markup3.tasks.mapping_moderation.dto.MappingModerationStatus
import ru.yandex.market.markup3.tasks.mapping_moderation.dto.MappingModerationTaskResult
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.YangMappingModerationHandler
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationOutput
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationProperties
import ru.yandex.market.markup3.tasks.mapping_moderation.yang.dto.YangMappingModerationState
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.users.profile.TolokaProfileRow
import ru.yandex.market.markup3.users.profile.repository.TolokaProfileRepository
import ru.yandex.market.markup3.utils.CommonObjectMapper
import ru.yandex.market.markup3.yang.TolokaClientMock
import ru.yandex.market.markup3.yang.dto.TolokaRecoverStatus
import ru.yandex.market.markup3.yang.repositories.TolokaRecoverQueueRepository
import ru.yandex.market.markup3.yang.services.TolokaActiveTasksService
import ru.yandex.market.markup3.yang.services.TolokaResultsDownloader
import ru.yandex.market.markup3.yang.services.TolokaTasksService
import ru.yandex.market.markup3.yang.services.YangTraitsAndSkillsService
import ru.yandex.market.markup3.yang.services.recover.RecoverTaskService
import ru.yandex.market.mbo.http.YangLogStorage
import ru.yandex.market.mbo.http.YangLogStorageService
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.toloka.client.v1.pool.Pool
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

/**
 * @author shadoff
 * created on 7/27/21
 */
class YangMappingModerationProcessTest : CommonTaskTest() {
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
                    taskTypeProps = YangMappingModerationProperties(
                        mappingModerationType = YangLogStorage.MappingModerationType.DEDUPLICATION,
                    )
                )
            )
        }
        taskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(taskGroup, sleepTimeMs = 0)

        ReflectionTestUtils.setField(yangMappingModerationHandler, "cancelledCheckDelay", 0)
        ReflectionTestUtils.setField(tolokaTasksService, "secondsToCancelGap", 0)

        tolokaProfileRepository.insert(TolokaProfileRow(WORKER_ID, STAFF_LOGIN, UID))

        mboCategoryService.taskOffersMap.clear()
        mboCategoryService.taskOffersMap.putAll(defaultTaskOffersAnswer())

        mboCategoryService.savedMappingModerationIds.clear()
        mboCategoryService.setDefaultSaveResponse()
        clearInvocations(yangLogStorageService)

        categoryInfoRepository.insert(CategoryInfo(
            hid = 4L, parentHid = 1L, name = "qwe", uniqueName = null,
            isNotUsed = false, isPublished = true, isAcceptGoodContent = true, isLeaf = true,
            inCategory = null, outOfCategory = null,
        ))
        categoryInfoService.invalidateCache()
    }

    @After
    fun tearDown() {
        mboCategoryService.setDefaultSaveResponse()
        mboCategoryService.taskOffersMap.clear()
    }

    @Test
    fun `test mapping moderation full process`() {
        offerTaskRepository.findAll() should beEmpty()

        mappingModerationGenerator.generate()

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
            YangLogStorage.YangTaskType.MAPPING_MODERATION_DEDUPLICATION, taskGroup, false)!!
        taskSuite.traitsAnyOf shouldContainExactlyInAnyOrder traits

        val genAssignmentId = tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, getSampleOutput(), WORKER_ID)
        yangResultsDownloader.downloadAllPools()

        taskInfo = getOnlyOneTaskInfo()
        taskInfo.solution shouldNotBe null
        taskInfo.assignmentId shouldNotBe null
        taskInfo.workerId shouldNotBe null


        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.SAVING.toString()

        val results = taskResultService.findResults<MappingModerationTaskResult>(task.id)
        results shouldHaveSize 1
        results[0].data.cancelledOffers shouldContainExactly listOf(10L)

        resultProcessor.process()

        // TODO: Офферы не удаляются за один раз
        val active = offerTaskRepository.findAll()
        active shouldHaveSize 0

        mboCategoryService.savedMappingModerationIds shouldHaveSize 1
        mboCategoryService.savedMappingModerationIds[0] shouldBe "5"

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.SAVED.toString()

        taskDbService.findAll()[0].processingStatus shouldBe ProcessingStatus.DONE

        val captor = argumentCaptor<YangLogStorage.YangLogStoreRequest>()
        verify(yangLogStorageService, times(1)).yangLogStore(captor.capture())

        val request = captor.firstValue
        with(request) {
            categoryId shouldBe 4
            id shouldBe YangLogSaver.getYangLogStoreTaskId(task.id)
            hitmanId shouldBe YangLogSaver.getExternalTaskId(task.id)
            taskType shouldBe YangLogStorage.YangTaskType.MAPPING_MODERATION

            val statistics = mappingModerationStatisticList[0]
            with(statistics) {
                uid shouldBe UID
                offerId shouldBe 5
                mappingModerationType shouldBe YangLogStorage.MappingModerationType.DEDUPLICATION
                mappingModerationStatus shouldBe YangLogStorage.MappingModerationStatus.ACCEPTED
            }

            with(contractorInfo) {
                uid shouldBe UID
                poolId shouldBe pool.id.toString()
                taskId shouldBe taskInfo.tolokaTaskId
                assignmentId shouldBe genAssignmentId
            }
        }
    }

    @Test
    fun `test mapping moderation set and change priority`() {
        val oldPriority = 1000.0
        mboCategoryService.taskOffersMap[5]?.priority = oldPriority
        mappingModerationGenerator.generate()

        taskGroupExecutor.processSingleEvent()

        var task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.ACTIVE.toString()

        var taskInfo = getOnlyOneTaskInfo()
        var taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)

        val oldPriorityConverted = convertMbocPriority(oldPriority).toDouble()
        taskInfo.issuingOrderOverride shouldBe oldPriorityConverted
        taskSuite.issuingOrderOverride shouldBe oldPriorityConverted

        val newPriority = 2000.0
        mboCategoryService.taskOffersMap[5]?.priority = newPriority

        mappingModerationGenerator.generate()
        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.ACTIVE.toString()

        taskInfo = getOnlyOneTaskInfo()
        taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)

        val newPriorityConverted = convertMbocPriority(newPriority).toDouble()
        taskInfo.issuingOrderOverride shouldBe newPriorityConverted
        taskSuite.issuingOrderOverride shouldBe newPriorityConverted
    }

    @Test
    fun `test mapping moderation cancel task`() {
        mappingModerationGenerator.generate()
        offerTaskRepository.findAll() shouldNotHaveSize 0

        taskGroupExecutor.processSingleEvent()

        var taskInfo = getOnlyOneTaskInfo()
        taskInfo.cancelled shouldBe false
        taskInfo.cancelledAt shouldBe null

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite.overlap shouldNotBe 0

        mboCategoryService.taskOffersMap.remove(5)

        mappingModerationGenerator.generate()
        taskGroupExecutor.processSingleEvent()

        taskInfo = getOnlyOneTaskInfo()
        taskInfo.cancelled shouldBe true
        taskInfo.cancelledAt shouldNotBe null

        var task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.CANCELLING.toString()

        tolokaActiveTasksService.updateActiveCache()
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.CANCELLED.toString()
        task.processingStatus shouldBe ProcessingStatus.CANCELLED

        cancelledTaskWatcher.watchForCancelled()

        offerTaskRepository.findAll() shouldHaveSize 0

        mboCategoryService.taskOffersMap.putAll(defaultTaskOffersAnswer())
        mappingModerationGenerator.generate()

        taskGroupExecutor.processSingleEvent()
        val newTaskInfos = tolokaTaskInfoRepository.findAll().associateBy { it.id }
        newTaskInfos[taskInfo.id]!!.cancelled shouldBe true

        val newTaskInfo = newTaskInfos.maxByOrNull { it.key }!!.value!!
        newTaskInfo.cancelled shouldBe false
        val newTaskSuite = tolokaClientMock.getTaskSuite(newTaskInfo.tolokaTaskSuiteId)
        newTaskSuite.overlap shouldNotBe 0
    }

    @Test
    fun `test mapping moderation do not cancel while cache not updated`() {
        mappingModerationGenerator.generate()

        taskGroupExecutor.processSingleEvent()

        var taskInfo = getOnlyOneTaskInfo()

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite.overlap shouldNotBe 0

        mboCategoryService.taskOffersMap.remove(5)

        mappingModerationGenerator.generate()
        taskGroupExecutor.processSingleEvent()

        taskInfo = getOnlyOneTaskInfo()

        taskInfo.cancelled shouldBe true
        taskInfo.cancelledAt shouldNotBe null

        val updatedTaskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        updatedTaskSuite.overlap shouldBe 0

        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()

        val task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE

        tolokaActiveTasksService.updateActiveCache()
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent()

        val updatedTask = getOnlyOneTask()

        updatedTask.processingStatus shouldBe ProcessingStatus.CANCELLED
    }

    @Test
    fun `test mapping moderation do not cancel if active`() {
        mappingModerationGenerator.generate()

        taskGroupExecutor.processSingleEvent()

        var taskInfo = getOnlyOneTaskInfo()

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite.overlap shouldNotBe 0

        mboCategoryService.taskOffersMap.remove(5)

        mappingModerationGenerator.generate()
        taskGroupExecutor.processSingleEvent()

        var task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.CANCELLING.toString()

        taskInfo = getOnlyOneTaskInfo()

        taskInfo.cancelled shouldBe true
        taskInfo.cancelledAt shouldNotBe null

        val updatedTaskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        updatedTaskSuite.overlap shouldBe 0

        tolokaClientMock.addActiveAssignment(taskSuite.id)

        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.CANCELLING.toString()

        tolokaActiveTasksService.updateActiveCache()
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe YangMappingModerationHandler.Stages.ACTIVE.toString()

        tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, getSampleOutput(), "worker")
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.SAVING.toString()

        resultProcessor.process()

        mboCategoryService.savedMappingModerationIds shouldHaveSize 1
        mboCategoryService.savedMappingModerationIds[0] shouldBe "5"
    }

    @Test
    @Ignore("TODO Fix in MBOASSORT-1882")
    fun `test reprocess removes from unique`() {
        val offerId = 5
        mboCategoryService.saveResponse = MboCategory.SaveMappingModerationResponse.newBuilder()
            .setResult(
                SupplierOffer.OperationResult.newBuilder()
                    .setStatus(SupplierOffer.OperationStatus.SUCCESS)
                    .addOfferStatuses(
                        SupplierOffer.OfferStatus.newBuilder()
                            .setOfferId(offerId.toString())
                            .setStatus(SupplierOffer.OperationStatus.REPROCESS)
                            .build()
                    )
            ).build()

        mappingModerationGenerator.generate()

        offerTaskRepository.findAll().map { it.offerId } shouldContainExactly listOf(5L)
        taskGroupExecutor.processSingleEvent()

        var taskInfo = getOnlyOneTaskInfo()

        tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, getSampleOutput(), "worker")
        yangResultsDownloader.downloadAllPools()

        taskInfo = getOnlyOneTaskInfo()
        taskInfo.solution shouldNotBe null

        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()

        val task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.DONE

        resultProcessor.process()

        offerTaskRepository.findAll() shouldHaveSize 0

        mboCategoryService.taskOffersMap.putAll(taskOffersAnswer(2))

        mappingModerationGenerator.generate()
        taskGroupExecutor.processSingleEvent()

        offerTaskRepository.findAll()[0].offerId shouldBe offerId

        val tasks = taskDbService.findAll().sortedBy { it.created }
        tasks shouldHaveSize 2
        tasks[0].processingStatus shouldBe ProcessingStatus.DONE
        tasks[1].processingStatus shouldBe ProcessingStatus.ACTIVE
    }

    @Test
    fun `test recover task - not found in pool`() {
        mappingModerationGenerator.generate()

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe INIT_STAGE

        tolokaClientMock.setShouldThrowOnce(true)
        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe INIT_STAGE

        val pool = getOnlyOnePool()

        var initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 1
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe INIT_STAGE

        val taskInfo = getOnlyOneTaskInfo()
        taskInfo.tolokaTaskId shouldBe null

        var recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 1
        var recoverRow = recoverQueue[0]
        recoverRow.poolId shouldBe pool.id
        recoverRow.taskId shouldBe task.id
        recoverRow.created shouldBe taskInfo.created
        recoverRow.tolokaRecoverStatus shouldBe TolokaRecoverStatus.NEW

        recoverTaskService.recoverAll()

        recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 1
        recoverRow = recoverQueue[0]
        recoverRow.poolId shouldBe pool.id
        recoverRow.taskId shouldBe task.id
        recoverRow.tolokaRecoverStatus shouldBe TolokaRecoverStatus.NOT_RECOVERED

        initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 2
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )
        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.ACTIVE.toString()
    }

    @Test
    fun `test recover task - recover found in pool`() {
        mappingModerationGenerator.generate()

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe INIT_STAGE

        tolokaClientMock.setShouldThrowOnce(true)
        tolokaClientMock.setShouldCreateOnThrowOnce(true)

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe INIT_STAGE

        val pool = getOnlyOnePool()

        var initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 1
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe INIT_STAGE

        var taskInfo = getOnlyOneTaskInfo()
        taskInfo.tolokaTaskId shouldBe null

        var recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 1
        var recoverRow = recoverQueue[0]
        recoverRow.poolId shouldBe pool.id
        recoverRow.created shouldBe taskInfo.created
        recoverRow.taskId shouldBe task.id
        recoverRow.tolokaRecoverStatus shouldBe TolokaRecoverStatus.NEW

        recoverTaskService.recoverAll()

        recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 1
        recoverRow = recoverQueue[0]
        recoverRow.poolId shouldBe pool.id
        recoverRow.taskId shouldBe task.id
        recoverRow.tolokaRecoverStatus shouldBe TolokaRecoverStatus.RECOVERED

        initEvent = taskEventDbService.findAll()[0]
        initEvent.taskId shouldBe task.id
        initEvent.retries shouldBe 2
        taskEventDbService.update(
            initEvent.copy(
                nextRun = Instant.now()
            )
        )
        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.ACTIVE.toString()

        taskInfo = getOnlyOneTaskInfo()
        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskInfo.tolokaTaskId shouldBe taskSuite.tasks[0].id
    }

    @Test
    fun whenYangReturnZeroResultsThenOk() {
        mappingModerationGenerator.generate()

        //Init
        taskGroupExecutor.processSingleEvent()
        //Staging
        var task = getOnlyOneTask()

        var taskInfo = getOnlyOneTaskInfo()
        var offersInDb = offerTaskRepository.find(
            OfferTaskFilter()
                .taskTypes(TaskType.YANG_MAPPING_MODERATION)
                .tasks(listOf(task.id))
        )
        offersInDb.size shouldBe 2
        tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, getEmptyOutput(), "worker")
        yangResultsDownloader.downloadAllPools()


        taskGroupExecutor.processSingleEvent()
        //Saving
        task = getOnlyOneTask()
        task.stage shouldBe YangMappingModerationHandler.Stages.SAVING.toString()
        taskInfo = getOnlyOneTaskInfo()

        val taskState = CommonObjectMapper.convertValue(task.state, YangMappingModerationState::class.java)
        taskState.sentOfferIds shouldContainExactlyInAnyOrder offersInDb.map { it.offerId }

        val output = taskInfo.solution?.let { parseResults(it) } ?: error("solution empty")
        output.results.size shouldBe 0

        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()

        resultProcessor.process()

        offersInDb = offerTaskRepository.find(
            OfferTaskFilter()
                .taskTypes(TaskType.YANG_MAPPING_MODERATION)
                .tasks(listOf(task.id))
        )
        offersInDb.size shouldBe 0

        val results = taskResultService.findResults<MappingModerationTaskResult>(task.id)
        results shouldHaveSize 1

        val active = offerTaskRepository.findAll()
        active shouldHaveSize 0
    }

    @Test
    fun whenOldModelYangModerationStateJsonThenOk() {
        val json = "{\"tolokaTaskId\": 123 }"
        val newState = CommonObjectMapper.readValue(json, YangMappingModerationState::class.java)
        newState shouldNotBe null
    }

    @Test
    fun `test different events order`() {

    }

    @Test
    fun `test poll closed downloader`() {

    }

    @Test
    fun `test multiple downloads`() {
        mappingModerationGenerator.generate()

        val task = getOnlyOneTask()

        taskGroupExecutor.processSingleEvent()
        var taskInfo = getOnlyOneTaskInfo()
        taskInfo.taskId shouldBe task.id
        taskInfo.tolokaTaskSuiteId shouldNotBe null

        tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, getSampleOutput(), "worker")
        yangResultsDownloader.downloadAllPools()

        taskInfo = getOnlyOneTaskInfo()
        taskInfo.solution shouldNotBe null
        taskInfo.assignmentId shouldNotBe null
        taskInfo.workerId shouldNotBe null

        yangResultsDownloader.downloadAllPools()
        yangResultsDownloader.downloadAllPools()
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent()

        val results = taskResultService.findResults<MappingModerationTaskResult>(task.id)
        results shouldHaveSize 1

        //TODO save billing test
    }

    @Test
    fun `test to psku flag`() {
        mboCategoryService.taskOffersMap.putAll(
            mapOf(
                5L to MboCategory.GetTaskOffersResponse.TaskOffer.newBuilder().apply {
                    offerId = 5
                    businessId = 2
                    categoryId = 4
                    suggestSkuType = SupplierOffer.SkuType.TYPE_PARTNER
                    processingCounter = 5
                    ticketId = 1
                    deadline = 0
                    critical = false
                    priority = 3.0
                },
                6L to MboCategory.GetTaskOffersResponse.TaskOffer.newBuilder().apply {
                    offerId = 6
                    businessId = 2
                    categoryId = 4
                    suggestSkuType = SupplierOffer.SkuType.TYPE_MARKET
                    processingCounter = 5
                    ticketId = 1
                    deadline = 0
                    critical = false
                    priority = 3.0
                }
            )
        )

        mappingModerationGenerator.generate()

        val tasks = taskDbService.findAll()
        tasks shouldHaveSize 2
        mboCategoryService.taskOffersMap.clear()
    }

    private fun parseResults(solution: JsonNode): YangMappingModerationOutput {
        val output = solution.get("output")
        return CommonObjectMapper.treeToValue(output)
    }

    private fun getSampleOutput(): Map<String, JsonNode> {
        val mapper = jacksonObjectMapper()

        val results: List<MappingModerationResultItem> = listOf(
            MappingModerationResultItem(
                1, 1234, MappingModerationStatus.ACCEPTED,
                "5",
                listOf(), null, null, null
            ),

            MappingModerationResultItem(
                1, null, MappingModerationStatus.UNDEFINED,
                "10",
                null, null, null, null
            ),
        )
        val output = mapper.convertValue(YangMappingModerationOutput(results), JsonNode::class.java)
        return mapOf(
            "output" to output
        )
    }

    private fun getEmptyOutput(): Map<String, JsonNode> {
        val mapper = jacksonObjectMapper()

        val results: List<MappingModerationResultItem> = emptyList()
        val output = mapper.convertValue(YangMappingModerationOutput(results), JsonNode::class.java)
        return mapOf("output" to output)
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

    private fun defaultTaskOffersAnswer() = taskOffersAnswer(1)

    private fun taskOffersAnswer(processingTicketId: Long): Map<OfferId, MboCategory.GetTaskOffersResponse.TaskOffer.Builder> {
        return mapOf(
            5L to MboCategory.GetTaskOffersResponse.TaskOffer.newBuilder().apply {
                offerId = 5
                businessId = 2
                categoryId = 4
                suggestSkuType = SupplierOffer.SkuType.TYPE_MARKET
                processingCounter = 5
                ticketId = processingTicketId
                deadline = 0
                critical = false
                priority = 3.0
                targetSkuId = 12
            },

            10L to MboCategory.GetTaskOffersResponse.TaskOffer.newBuilder().apply {
                offerId = 10
                businessId = 2
                categoryId = 4
                suggestSkuType = SupplierOffer.SkuType.TYPE_MARKET
                processingCounter = 5
                ticketId = processingTicketId
                deadline = 0
                critical = false
                priority = 3.0
                targetSkuId = 13
            },
        )
    }
}
