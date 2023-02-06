package ru.yandex.market.markup3.tasks.blueclassification

import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import ru.yandex.market.markup3.core.services.TaskService.Companion.INIT_STAGE
import ru.yandex.market.markup3.mboc.PriorityUtil
import ru.yandex.market.markup3.mboc.blueclassification.MbocBlueClassificationConstants
import ru.yandex.market.markup3.mboc.blueclassification.generator.BlueClassificationGenerator
import ru.yandex.market.markup3.mboc.blueclassification.processor.BlueClassificationResultProcessor
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.offertask.service.CancelledTaskWatcher
import ru.yandex.market.markup3.mboc.taskOffer
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.YangLogSaver
import ru.yandex.market.markup3.tasks.blueclassification.BlueClassificationHandler.Stages
import ru.yandex.market.markup3.tasks.blueclassification.dto.BlueClassificationOutput
import ru.yandex.market.markup3.tasks.blueclassification.dto.BlueClassificationResult
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
import ru.yandex.market.markup3.yang.services.recover.RecoverTaskService
import ru.yandex.market.mbo.http.YangLogStorage
import ru.yandex.market.mbo.http.YangLogStorageService
import ru.yandex.market.mboc.http.MboCategory.UpdateSupplierOfferCategoryResponse
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.market.mboc.http.SupplierOffer.OfferStatus
import ru.yandex.market.mboc.http.SupplierOffer.OperationStatus
import ru.yandex.toloka.client.v1.pool.Pool
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.util.Date

class BlueClassificationProcessTest : CommonTaskTest() {
    @Autowired
    private lateinit var taskGroupExecutorProvider: TaskGroupExecutorProvider

    @Autowired
    private lateinit var tolokaTasksService: TolokaTasksService

    @Autowired
    private lateinit var tolokaClientMock: TolokaClientMock

    @Autowired
    private lateinit var yangResultsDownloader: TolokaResultsDownloader

    @Autowired
    private lateinit var cancelledTaskWatcher: CancelledTaskWatcher

    @Autowired
    private lateinit var yangLogStorageService: YangLogStorageService

    @Autowired
    private lateinit var tolokaActiveTasksService: TolokaActiveTasksService

    @Autowired
    private lateinit var tolokaRecoverQueueRepository: TolokaRecoverQueueRepository

    @Autowired
    private lateinit var recoverTaskService: RecoverTaskService

    @Autowired
    private lateinit var handler: BlueClassificationHandler

    @Autowired
    private lateinit var generator: BlueClassificationGenerator

    @Autowired
    private lateinit var resultProcessor: BlueClassificationResultProcessor

    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    @Autowired
    private lateinit var mbocMock: MboCategoryServiceMock

    @Autowired
    private lateinit var tolokaProfileRepository: TolokaProfileRepository

    private lateinit var taskGroup: TaskGroup
    private lateinit var taskGroupExecutor: TaskGroupExecutor

    companion object {
        const val UID = 1234L
        const val WORKER_ID = "worker"
        const val STAFF_LOGIN = "staff"
    }

    @Before
    fun setUp() {
        val basePool = createBasePool()
        taskGroup = taskGroupRegistry.getOrCreateTaskGroup(MbocBlueClassificationConstants.GROUP_KEY) {
            TaskGroup(
                key = MbocBlueClassificationConstants.GROUP_KEY,
                name = MbocBlueClassificationConstants.GROUP_KEY,
                taskType = TaskType.BLUE_CLASSIFICATION,
                config = TaskGroupConfig(basePoolId = basePool.id)
            )
        }
        taskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(taskGroup, sleepTimeMs = 0)

        ReflectionTestUtils.setField(handler, "cancelledCheckDelay", 0)
        ReflectionTestUtils.setField(tolokaTasksService, "secondsToCancelGap", 0)

        tolokaProfileRepository.insert(TolokaProfileRow(WORKER_ID, STAFF_LOGIN, UID))

        mbocMock.addTaskOffers(
            taskOffer(
                5,
                2,
                4,
                deadline = 10000,
                priority = 3.0,
                skuType = SupplierOffer.SkuType.TYPE_PARTNER,
                ticketId = 1
            )
        )

        mbocMock.updateSupplierOfferCategoryResponse = defaultUpdCatAnswer(listOf(5))
        mbocMock.updateSupplierOfferCategoryRequests.clear()
        clearInvocations(yangLogStorageService)
    }

    @Test
    fun `test blue classification full process`() {
        generator.generate()

        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe INIT_STAGE

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe Stages.ACTIVE.toString()

        val pool = getOnlyOnePool()
        pool.source shouldBe TolokaSource.YANG
        pool.active shouldBe true

        var taskInfo = getOnlyOneTaskInfo()
        taskInfo.taskId shouldBe task.id
        taskInfo.tolokaTaskSuiteId shouldNotBe null

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite shouldNotBe null

        taskGroupExecutor.processSingleEvent() // Offer status SENT

        val genAssignmentId = tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, getSampleOutput(), WORKER_ID)
        yangResultsDownloader.downloadAllPools()

        taskInfo = getOnlyOneTaskInfo()
        taskInfo.solution shouldNotBe null
        taskInfo.assignmentId shouldNotBe null
        taskInfo.workerId shouldNotBe null

        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe Stages.SAVING.toString()

        taskGroupExecutor.processSingleEvent() // Offer status RECEIVED

        resultProcessor.process()

        val active = offerTaskRepository.findAll()
        active shouldHaveSize 0

        mbocMock.updateSupplierOfferCategoryRequests.also {
            it shouldHaveSize 1
            it.first().resultList[0].offerId shouldBe "5"
            it.clear()
        }

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.stage shouldBe Stages.SAVED.toString()

        taskDbService.findAll()[0].processingStatus shouldBe ProcessingStatus.DONE

        val captor = argumentCaptor<YangLogStorage.YangLogStoreRequest>()
        verify(yangLogStorageService, times(1)).yangLogStore(captor.capture())

        val request = captor.firstValue
        with(request) {
            categoryId shouldBe 4
            id shouldBe YangLogSaver.getYangLogStoreTaskId(task.id)
            hitmanId shouldBe YangLogSaver.getExternalTaskId(task.id)
            taskType shouldBe YangLogStorage.YangTaskType.BLUE_CLASSIFICATION

            val statistics = classificationStatisticList[0]
            with(statistics) {
                uid shouldBe UID
                offerId shouldBe 5
                fixedCategoryId shouldBe 10
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
    fun `test blue classification set and change priority`() {
        val oldDeadline = LocalDate.of(2021, Month.OCTOBER, 19).toEpochDay()
        val oldPriority = PriorityUtil.calculatePriority(oldDeadline, false)
        mbocMock.taskOffersMap[5]?.deadline = oldDeadline
        generator.generate()

        taskGroupExecutor.processSingleEvent()
        taskGroupExecutor.processSingleEvent() // Offer status SENT

        var task = getOnlyOneTask()
        task.stage shouldBe Stages.ACTIVE.toString()

        var taskInfo = getOnlyOneTaskInfo()
        var taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)

        taskInfo.issuingOrderOverride shouldBe oldPriority
        taskSuite.issuingOrderOverride shouldBe oldPriority

        val newDeadline = LocalDate.of(2021, Month.OCTOBER, 15).toEpochDay()
        val newPriority = PriorityUtil.calculatePriority(newDeadline, false)
        mbocMock.taskOffersMap[5]?.deadline = newDeadline

        generator.generate()

        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.stage shouldBe Stages.ACTIVE.toString()

        taskInfo = getOnlyOneTaskInfo()
        taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)

        taskInfo.issuingOrderOverride shouldBe newPriority
        taskSuite.issuingOrderOverride shouldBe newPriority
    }

    @Test
    fun `test blue classification cancel task`() {
        generator.generate()
        offerTaskRepository.findAll() shouldNotHaveSize 0

        taskGroupExecutor.processSingleEvent() // Init
        taskGroupExecutor.processSingleEvent() // Offer status SENT

        var taskInfo = getOnlyOneTaskInfo()
        taskInfo.cancelled shouldBe false
        taskInfo.cancelledAt shouldBe null

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite.overlap shouldNotBe 0

        mbocMock.taskOffersMap.remove(5)

        generator.generate()
        taskGroupExecutor.processSingleEvent() // Cancel

        taskInfo = getOnlyOneTaskInfo()
        taskInfo.cancelled shouldBe true
        taskInfo.cancelledAt shouldNotBe null

        var task = getOnlyOneTask()
        task.stage shouldBe Stages.CANCELLING.toString()

        tolokaActiveTasksService.updateActiveCache()
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent() // WaitCancelled
        taskGroupExecutor.processSingleEvent() // Offer status EXPIRED
        task = getOnlyOneTask()
        task.stage shouldBe Stages.CANCELLED.toString()
        task.processingStatus shouldBe ProcessingStatus.CANCELLED

        cancelledTaskWatcher.watchForCancelled()

        offerTaskRepository.findAll() shouldHaveSize 0

        mbocMock.addTaskOffers(
            taskOffer(
                5,
                2,
                4,
                deadline = 10000,
                priority = 3.0,
                skuType = SupplierOffer.SkuType.TYPE_PARTNER,
                ticketId = 1
            )
        )
        generator.generate()

        taskGroupExecutor.processSingleEvent() // Init
        taskGroupExecutor.processSingleEvent() // Offer status SENT
        val newTaskInfos = tolokaTaskInfoRepository.findAll().associateBy { it.id }
        newTaskInfos[taskInfo.id]!!.cancelled shouldBe true

        val newTaskInfo = newTaskInfos.maxByOrNull { it.key }!!.value!!
        newTaskInfo.cancelled shouldBe false
        val newTaskSuite = tolokaClientMock.getTaskSuite(newTaskInfo.tolokaTaskSuiteId)
        newTaskSuite.overlap shouldNotBe 0
    }

    @Test
    fun `test blue classification do not cancel while cache not updated`() {
        generator.generate()

        taskGroupExecutor.processSingleEvent() // Init
        taskGroupExecutor.processSingleEvent() // Offer status SENT

        var taskInfo = getOnlyOneTaskInfo()

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite.overlap shouldNotBe 0

        mbocMock.taskOffersMap.remove(5)

        generator.generate()
        taskGroupExecutor.processSingleEvent() // Cancel

        taskInfo = getOnlyOneTaskInfo()

        taskInfo.cancelled shouldBe true
        taskInfo.cancelledAt shouldNotBe null

        val updatedTaskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        updatedTaskSuite.overlap shouldBe 0

        taskGroupExecutor.processSingleEvent() // WaitCancelled
        taskGroupExecutor.processSingleEvent() // WaitCancelled
        taskGroupExecutor.processSingleEvent() // WaitCancelled

        val task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE

        tolokaActiveTasksService.updateActiveCache()
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent() // WaitCancelled
        taskGroupExecutor.processSingleEvent() // Offer status EXPIRED

        val updatedTask = getOnlyOneTask()

        updatedTask.processingStatus shouldBe ProcessingStatus.CANCELLED
    }

    @Test
    fun `test blue classification do not cancel if active`() {
        generator.generate()

        taskGroupExecutor.processSingleEvent() // Init
        taskGroupExecutor.processSingleEvent() // Offer status SENT

        var taskInfo = getOnlyOneTaskInfo()

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite.overlap shouldNotBe 0

        mbocMock.taskOffersMap.remove(5)

        generator.generate()
        taskGroupExecutor.processSingleEvent() // Cancel

        var task = getOnlyOneTask()
        task.stage shouldBe Stages.CANCELLING.toString()

        taskInfo = getOnlyOneTaskInfo()

        taskInfo.cancelled shouldBe true
        taskInfo.cancelledAt shouldNotBe null

        val updatedTaskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        updatedTaskSuite.overlap shouldBe 0

        tolokaClientMock.addActiveAssignment(taskSuite.id)

        task = getOnlyOneTask()
        task.stage shouldBe Stages.CANCELLING.toString()

        tolokaActiveTasksService.updateActiveCache()
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent() // WaitCancelled

        task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe Stages.ACTIVE.toString()

        tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, getSampleOutput(), "worker")
        yangResultsDownloader.downloadAllPools()

        taskGroupExecutor.processSingleEvent() // Ready

        task = getOnlyOneTask()
        task.stage shouldBe Stages.SAVING.toString()

        taskGroupExecutor.processSingleEvent() // SaveBilling

        task = getOnlyOneTask()
        task.stage shouldBe Stages.SAVED.toString()

        resultProcessor.process()

        mbocMock.updateSupplierOfferCategoryRequests shouldHaveSize 1
        mbocMock.updateSupplierOfferCategoryRequests[0].resultList[0].offerId shouldBe "5"
    }

    @Test
    fun `test reprocess removes from unique`() {
        val offerId = 5
        mbocMock.updateSupplierOfferCategoryResponse = defaultUpdCatAnswer(listOf(5))
            .toBuilder()
            .apply { resultBuilder.offerStatusesBuilderList.onEach { it.status = OperationStatus.REPROCESS } }
            .build()

        generator.generate()

        offerTaskRepository.findAll()[0].key
        taskGroupExecutor.processSingleEvent() // Init
        taskGroupExecutor.processSingleEvent() // Offer status SENT

        var taskInfo = getOnlyOneTaskInfo()

        tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, getSampleOutput(), "worker")
        yangResultsDownloader.downloadAllPools()

        taskInfo = getOnlyOneTaskInfo()
        taskInfo.solution shouldNotBe null

        taskGroupExecutor.processSingleEvent() // Ready
        taskGroupExecutor.processSingleEvent() // Offer status RECEIVED
        taskGroupExecutor.processSingleEvent() // SaveBilling
        taskGroupExecutor.processSingleEvent() // Offer status PROCESSED

        val task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.DONE

        resultProcessor.process()

        offerTaskRepository.findAll() shouldHaveSize 0

        mbocMock.addTaskOffers(
            taskOffer(
                5,
                2,
                4,
                deadline = 10000,
                priority = 3.0,
                skuType = SupplierOffer.SkuType.TYPE_PARTNER,
                ticketId = 2,
                processingCounter = 1
            )
        )

        generator.generate()
        taskGroupExecutor.processSingleEvent() // Init
        taskGroupExecutor.processSingleEvent() // Offer status SENT

        offerTaskRepository.findAll()[0].offerId shouldBe offerId

        val tasks = taskDbService.findAll().sortedBy { it.created }
        tasks shouldHaveSize 2
        tasks[0].processingStatus shouldBe ProcessingStatus.DONE
        tasks[1].processingStatus shouldBe ProcessingStatus.ACTIVE
    }

    @Test
    fun `test recover task - not found in pool`() {
        generator.generate()

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
        task.stage shouldBe Stages.ACTIVE.toString()
    }

    @Test
    fun `test recover task - recover found in pool`() {
        generator.generate()

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
        task.stage shouldBe Stages.ACTIVE.toString()

        taskInfo = getOnlyOneTaskInfo()
        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskInfo.tolokaTaskId shouldBe taskSuite.tasks[0].id
    }

    @Test
    fun `test different events order`() {

    }

    @Test
    fun `test poll closed downloader`() {

    }

    @Test
    fun `test multiple downloads`() {
        generator.generate()

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

        val results = taskResultService.findResults<BlueClassificationResult>(task.id)
        results.filter { it.data is BlueClassificationResult } shouldHaveSize 1

        //TODO save billing test
    }

    private fun getSampleOutput(): Map<String, JsonNode> {
        val results: List<BlueClassificationOutput> = listOf(
            BlueClassificationOutput(1, "5", UID.toString(), 10, emptyList())
        )
        val output = CommonObjectMapper.valueToTree<JsonNode>(results)
        return mapOf(
            "output" to output
        )
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

    private fun defaultUpdCatAnswer(offers: Collection<Long>): UpdateSupplierOfferCategoryResponse {
        return UpdateSupplierOfferCategoryResponse.newBuilder()
            .setResult(
                SupplierOffer.OperationResult.newBuilder()
                    .addAllOfferStatuses(offers.map {
                        OfferStatus.newBuilder()
                            .setOfferId(it.toString())
                            .setStatus(OperationStatus.SUCCESS)
                            .build()
                    })
                    .setStatus(OperationStatus.SUCCESS)
                    .build()
            )
            .build()
    }
}
