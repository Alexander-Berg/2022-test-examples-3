package ru.yandex.market.markup3.tasks.bluelogs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
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
import ru.yandex.market.markup3.mboc.bluelogs.MbocBlueLogsConstants
import ru.yandex.market.markup3.mboc.bluelogs.generator.BlueLogsGenerator
import ru.yandex.market.markup3.mboc.bluelogs.processor.BlueLogsResultProcessor
import ru.yandex.market.markup3.mboc.category.info.CategoryInfo
import ru.yandex.market.markup3.mboc.category.info.repository.CategoryInfoRepository
import ru.yandex.market.markup3.mboc.category.info.service.CategoryInfoService
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.taskOffer
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.YangLogSaver
import ru.yandex.market.markup3.tasks.bluelogs.dto.BlueLogsState
import ru.yandex.market.markup3.tasks.bluelogs.dto.MappingStatuses
import ru.yandex.market.markup3.tasks.bluelogs.dto.TaskResult
import ru.yandex.market.markup3.tasks.bluelogs.dto.TaskSolution
import ru.yandex.market.markup3.tasks.bluelogs.handler.BlueLogsHandler
import ru.yandex.market.markup3.tasks.bluelogs.handler.BlueLogsHandler.Stages
import ru.yandex.market.markup3.tasks.bluelogs.handler.modifyPriorityForInspection
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.users.profile.TolokaProfileRow
import ru.yandex.market.markup3.users.profile.repository.TolokaProfileRepository
import ru.yandex.market.markup3.utils.CommonObjectMapper
import ru.yandex.market.markup3.yang.TolokaClientMock
import ru.yandex.market.markup3.yang.services.TolokaResultsDownloader
import ru.yandex.market.markup3.yang.services.TolokaTasksService
import ru.yandex.market.mbo.http.YangLogStorage
import ru.yandex.market.mbo.http.YangLogStorageService
import ru.yandex.market.mboc.http.MboCategory.SaveTaskMappingsResponse
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.market.mboc.http.SupplierOffer.OfferStatus
import ru.yandex.market.mboc.http.SupplierOffer.OperationStatus
import ru.yandex.market.mboc.http.SupplierOffer.SkuType.TYPE_PARTNER
import ru.yandex.toloka.client.v1.pool.Pool
import java.math.BigDecimal
import java.util.Date

class BlueLogsProcessTest : CommonTaskTest() {
    @Autowired
    private lateinit var taskGroupExecutorProvider: TaskGroupExecutorProvider

    @Autowired
    private lateinit var tolokaTasksService: TolokaTasksService

    @Autowired
    private lateinit var tolokaClientMock: TolokaClientMock

    @Autowired
    private lateinit var yangResultsDownloader: TolokaResultsDownloader

    @Autowired
    private lateinit var yangLogStorageService: YangLogStorageService

    @Autowired
    private lateinit var handler: BlueLogsHandler

    @Autowired
    private lateinit var generator: BlueLogsGenerator

    @Autowired
    private lateinit var resultProcessor: BlueLogsResultProcessor

    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    @Autowired
    private lateinit var mbocMock: MboCategoryServiceMock

    @Autowired
    private lateinit var tolokaProfileRepository: TolokaProfileRepository

    @Autowired
    private lateinit var categoryInfoService: CategoryInfoService

    @Autowired
    private lateinit var categoryInfoRepository: CategoryInfoRepository

    private lateinit var taskGroup: TaskGroup
    private lateinit var taskGroupExecutor: TaskGroupExecutor

    companion object {
        const val MAIN_UID = 1234L
        const val MAIN_WORKER_ID = "worker"
        const val MAIN_STAFF_LOGIN = "staff"
        const val INSPECTOR_UID = 9999L
        const val INSPECTOR_WORKER_ID = "inspector"
        const val INSPECTOR_STAFF_LOGIN = "inspectorStaff"
    }

    @Before
    fun setUp() {
        val basePool = createBasePool()
        taskGroup = taskGroupRegistry.getOrCreateTaskGroup(MbocBlueLogsConstants.GROUP_KEY) {
            TaskGroup(
                key = MbocBlueLogsConstants.GROUP_KEY,
                name = MbocBlueLogsConstants.GROUP_KEY,
                taskType = TaskType.BLUE_LOGS,
                config = TaskGroupConfig(basePoolId = basePool.id)
            )
        }
        taskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(taskGroup, sleepTimeMs = 0)

        ReflectionTestUtils.setField(handler, "cancelledCheckDelay", 0)
        ReflectionTestUtils.setField(tolokaTasksService, "secondsToCancelGap", 0)

        tolokaProfileRepository.insert(TolokaProfileRow(MAIN_WORKER_ID, MAIN_STAFF_LOGIN, MAIN_UID))
        tolokaProfileRepository.insert(TolokaProfileRow(INSPECTOR_WORKER_ID, INSPECTOR_STAFF_LOGIN, INSPECTOR_UID))

        mbocMock.addTaskOffers(
            taskOffer(5, 2, 4, deadline = 10000, priority = 3.0, skuType = TYPE_PARTNER, ticketId = 1)
        )

        mbocMock.saveTaskMappingsResponse = defaultSaveMappingAnswer(listOf(5))
        mbocMock.saveTaskMappingsRequests.clear()
        clearInvocations(yangLogStorageService)

        configureInspectionNeeded(true)

        categoryInfoRepository.insert(
            CategoryInfo(
                hid = 4,
                parentHid = -1,
                name = "TestCat",
                uniqueName = "TCUniq",
                isNotUsed = false,
                isPublished = true,
                isAcceptGoodContent = true,
                inCategory = null,
                outOfCategory = null,
                isLeaf = false,
            )
        )
        categoryInfoService.invalidateCache()
    }

    @Test
    fun `full process with inspection`() {
        // Gen task and check main init
        generator.generate()
        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe INIT_STAGE

        taskGroupExecutor.processSingleEvent() // Init

        task = getOnlyOneTask()
        task.stage shouldBe Stages.ACTIVE.toString()
        CommonObjectMapper.treeToValue<BlueLogsState>(task.state).also {
            it.categoryName shouldBe "TestCat"
            it.uniqueName shouldBe "TCUniq"
        }

        val pool = getOnlyOnePool()
        pool.source shouldBe TolokaSource.YANG
        pool.active shouldBe true

        var mainTaskInfo = getOnlyOneTaskInfo()
        mainTaskInfo.taskId shouldBe task.id
        mainTaskInfo.tolokaTaskSuiteId shouldNotBe null

        val mainTaskSuite = tolokaClientMock.getTaskSuite(mainTaskInfo.tolokaTaskSuiteId)
        mainTaskSuite shouldNotBe null

        // Finish main
        val mainAssignmentId = tolokaClientMock.setTaskFinished(
            mainTaskInfo.tolokaTaskSuiteId,
            getSampleOutput(MappingStatuses.MAPPED), MAIN_WORKER_ID
        )
        yangResultsDownloader.downloadAllPools()

        mainTaskInfo = getOnlyOneTaskInfo()
        mainTaskInfo.solution shouldNotBe null
        mainTaskInfo.assignmentId shouldNotBe null
        mainTaskInfo.workerId shouldNotBe null

        taskGroupExecutor.processSingleEvent() // TaskReady (main)

        task = getOnlyOneTask()
        task.stage shouldBe Stages.ACTIVE.toString()
        CommonObjectMapper.treeToValue<BlueLogsState>(task.state).also {
            it.categoryName shouldBe "TestCat"
            it.uniqueName shouldBe "TCUniq"
            it.contractorWorkerId shouldBe MAIN_WORKER_ID
            it.outputForInspection shouldNotBe null
        }

        // Check in inspection
        taskGroupExecutor.processSingleEvent() // InspectionNeeded

        task = getOnlyOneTask()
        task.stage shouldBe Stages.INSPECTION.toString()

        var inspectionTaskInfo = tolokaTaskInfoRepository.findByTaskId(task.id).first { it.solution == null }
        inspectionTaskInfo.taskId shouldBe task.id
        inspectionTaskInfo.tolokaTaskSuiteId shouldNotBe null

        val inspectionTaskSuite = tolokaClientMock.getTaskSuite(inspectionTaskInfo.tolokaTaskSuiteId)
        inspectionTaskSuite shouldNotBe null

        // Finish inspection
        val inspectionAssignmentId = tolokaClientMock.setTaskFinished(
            inspectionTaskInfo.tolokaTaskSuiteId,
            getSampleOutput(MappingStatuses.MAPPED), INSPECTOR_WORKER_ID
        )
        yangResultsDownloader.downloadAllPools()

        inspectionTaskInfo = tolokaTaskInfoRepository.findById(inspectionTaskInfo.id)
        inspectionTaskInfo.solution shouldNotBe null
        inspectionTaskInfo.assignmentId shouldNotBe null
        inspectionTaskInfo.workerId shouldNotBe null

        // Check saving
        taskGroupExecutor.processSingleEvent() // TaskReady (inspection)
        task = getOnlyOneTask()
        task.stage shouldBe Stages.SAVING.toString()

        resultProcessor.process()

        val active = offerTaskRepository.findAll()
        active shouldHaveSize 0

        with(mbocMock.saveTaskMappingsRequests) {
            this shouldHaveSize 1
            with(first().mappingList[0]) {
                offerId shouldBe "5"
                categoryId shouldBe 4
                marketSkuId shouldBe 134
                staffLogin shouldBe INSPECTOR_STAFF_LOGIN
            }
            clear()
        }

        // Check billing
        taskGroupExecutor.processSingleEvent() // Save billing
        task = getOnlyOneTask()
        task.stage shouldBe Stages.SAVED.toString()
        task.processingStatus shouldBe ProcessingStatus.DONE

        val captor = argumentCaptor<YangLogStorage.YangLogStoreRequest>()
        verify(yangLogStorageService, times(1)).yangLogStore(captor.capture())

        val request = captor.firstValue
        with(request) {
            categoryId shouldBe 4
            id shouldBe YangLogSaver.getYangLogStoreTaskId(task.id)
            hitmanId shouldBe YangLogSaver.getExternalTaskId(task.id)
            taskType shouldBe YangLogStorage.YangTaskType.BLUE_LOGS

            val statistics = mappingStatisticList[0]
            with(statistics) {
                uid shouldBe MAIN_UID
                offerId shouldBe 5
                offerMappingStatus shouldBe YangLogStorage.MappingStatus.MAPPED
                marketSkuId shouldBe 134
            }

            with(contractorInfo) {
                uid shouldBe MAIN_UID
                poolId shouldBe pool.id.toString()
                taskId shouldBe mainTaskInfo.tolokaTaskId
                assignmentId shouldBe mainAssignmentId
            }
            with(inspectorInfo) {
                uid shouldBe INSPECTOR_UID
                poolId shouldBe pool.id.toString()
                taskId shouldBe inspectionTaskInfo.tolokaTaskId
                assignmentId shouldBe inspectionAssignmentId
            }
        }
    }

    @Test
    fun `Inspector UID in billing if mapping changed`() {
        // Gen task and check main init
        generator.generate()
        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe INIT_STAGE

        taskGroupExecutor.processSingleEvent() // Init

        task = getOnlyOneTask()
        task.stage shouldBe Stages.ACTIVE.toString()
        CommonObjectMapper.treeToValue<BlueLogsState>(task.state).also {
            it.categoryName shouldBe "TestCat"
            it.uniqueName shouldBe "TCUniq"
        }

        val pool = getOnlyOnePool()
        pool.source shouldBe TolokaSource.YANG
        pool.active shouldBe true

        var mainTaskInfo = getOnlyOneTaskInfo()
        mainTaskInfo.taskId shouldBe task.id
        mainTaskInfo.tolokaTaskSuiteId shouldNotBe null

        val mainTaskSuite = tolokaClientMock.getTaskSuite(mainTaskInfo.tolokaTaskSuiteId)
        mainTaskSuite shouldNotBe null

        // Finish main
        val mainAssignmentId = tolokaClientMock.setTaskFinished(
            mainTaskInfo.tolokaTaskSuiteId,
            getSampleOutput(MappingStatuses.MAPPED), MAIN_WORKER_ID
        )
        yangResultsDownloader.downloadAllPools()

        mainTaskInfo = getOnlyOneTaskInfo()
        mainTaskInfo.solution shouldNotBe null
        mainTaskInfo.assignmentId shouldNotBe null
        mainTaskInfo.workerId shouldNotBe null

        taskGroupExecutor.processSingleEvent() // TaskReady (main)

        task = getOnlyOneTask()
        task.stage shouldBe Stages.ACTIVE.toString()
        CommonObjectMapper.treeToValue<BlueLogsState>(task.state).also {
            it.categoryName shouldBe "TestCat"
            it.uniqueName shouldBe "TCUniq"
            it.contractorWorkerId shouldBe MAIN_WORKER_ID
            it.outputForInspection shouldNotBe null
        }

        // Check in inspection
        taskGroupExecutor.processSingleEvent() // InspectionNeeded

        task = getOnlyOneTask()
        task.stage shouldBe Stages.INSPECTION.toString()

        var inspectionTaskInfo = tolokaTaskInfoRepository.findByTaskId(task.id).first { it.solution == null }
        inspectionTaskInfo.taskId shouldBe task.id
        inspectionTaskInfo.tolokaTaskSuiteId shouldNotBe null

        val inspectionTaskSuite = tolokaClientMock.getTaskSuite(inspectionTaskInfo.tolokaTaskSuiteId)
        inspectionTaskSuite shouldNotBe null

        // Finish inspection
        val inspectionAssignmentId = tolokaClientMock.setTaskFinished(
            inspectionTaskInfo.tolokaTaskSuiteId,
            getSampleOutput(MappingStatuses.TRASH), INSPECTOR_WORKER_ID
        )
        yangResultsDownloader.downloadAllPools()

        inspectionTaskInfo = tolokaTaskInfoRepository.findById(inspectionTaskInfo.id)
        inspectionTaskInfo.solution shouldNotBe null
        inspectionTaskInfo.assignmentId shouldNotBe null
        inspectionTaskInfo.workerId shouldNotBe null

        // Check saving
        taskGroupExecutor.processSingleEvent() // TaskReady (inspection)
        task = getOnlyOneTask()
        task.stage shouldBe Stages.SAVING.toString()

        resultProcessor.process()

        val active = offerTaskRepository.findAll()
        active shouldHaveSize 0

        with(mbocMock.saveTaskMappingsRequests) {
            this shouldHaveSize 1
            with(first().mappingList[0]) {
                offerId shouldBe "5"
                categoryId shouldBe 4
                marketSkuId shouldBe 134
                staffLogin shouldBe INSPECTOR_STAFF_LOGIN
            }
            clear()
        }


        // Check billing
        taskGroupExecutor.processSingleEvent() // Save billing
        task = getOnlyOneTask()
        task.stage shouldBe Stages.SAVED.toString()
        task.processingStatus shouldBe ProcessingStatus.DONE

        val captor = argumentCaptor<YangLogStorage.YangLogStoreRequest>()
        verify(yangLogStorageService, times(1)).yangLogStore(captor.capture())

        val request = captor.firstValue
        with(request) {
            categoryId shouldBe 4
            id shouldBe YangLogSaver.getYangLogStoreTaskId(task.id)
            hitmanId shouldBe YangLogSaver.getExternalTaskId(task.id)
            taskType shouldBe YangLogStorage.YangTaskType.BLUE_LOGS

            val statistics = mappingStatisticList[0]
            with(statistics) {
                uid shouldBe INSPECTOR_UID
                offerId shouldBe 5
                offerMappingStatus shouldBe YangLogStorage.MappingStatus.TRASH
                marketSkuId shouldBe 134
            }

            with(contractorInfo) {
                uid shouldBe MAIN_UID
                poolId shouldBe pool.id.toString()
                taskId shouldBe mainTaskInfo.tolokaTaskId
                assignmentId shouldBe mainAssignmentId
            }
            with(inspectorInfo) {
                uid shouldBe INSPECTOR_UID
                poolId shouldBe pool.id.toString()
                taskId shouldBe inspectionTaskInfo.tolokaTaskId
                assignmentId shouldBe inspectionAssignmentId
            }
        }

    }

    @Test
    fun `test priotity was changed before inspection`() {
        val oldDeadline = mbocMock.taskOffersMap[5]!!.deadline
        // Gen task and check main init
        generator.generate()
        var task = getOnlyOneTask()
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe INIT_STAGE

        taskGroupExecutor.processSingleEvent() // Init
        taskGroupExecutor.processSingleEvent() // Send state

        val oldPriority = PriorityUtil.calculatePriority(oldDeadline, false)

        var mainTaskInfo = getOnlyOneTaskInfo()
        mainTaskInfo.issuingOrderOverride shouldBe oldPriority

        var mainTaskSuite = tolokaClientMock.getTaskSuite(mainTaskInfo.tolokaTaskSuiteId)
        mainTaskSuite shouldNotBe null
        mainTaskSuite.issuingOrderOverride shouldBe oldPriority

        val newDeadline = oldDeadline - 500
        mbocMock.taskOffersMap[5]?.deadline = newDeadline
        generator.generate()

        val newPriority = PriorityUtil.calculatePriority(newDeadline, false)

        taskGroupExecutor.processSingleEvent() // Change priority

        mainTaskInfo = getOnlyOneTaskInfo()
        mainTaskInfo.issuingOrderOverride shouldBe newPriority

        mainTaskSuite = tolokaClientMock.getTaskSuite(mainTaskInfo.tolokaTaskSuiteId)
        mainTaskSuite.issuingOrderOverride shouldBe newPriority

        val mainAssignmentId = tolokaClientMock.setTaskFinished(
            mainTaskInfo.tolokaTaskSuiteId,
            getSampleOutput(MappingStatuses.MAPPED), MAIN_WORKER_ID
        )
        yangResultsDownloader.downloadAllPools()

        mainTaskInfo = getOnlyOneTaskInfo()
        mainTaskInfo.solution shouldNotBe null
        mainTaskInfo.assignmentId shouldNotBe null
        mainTaskInfo.workerId shouldNotBe null

        taskGroupExecutor.processSingleEvent() // TaskReady (main)
        taskGroupExecutor.processSingleEvent() // send state
        taskGroupExecutor.processSingleEvent() // InspectionNeeded

        task = getOnlyOneTask()
        task.stage shouldBe Stages.INSPECTION.toString()

        val inspectionPriority = modifyPriorityForInspection(newPriority)

        val inspectionTaskInfo = tolokaTaskInfoRepository.findByTaskId(task.id).first { it.solution == null }
        inspectionTaskInfo.taskId shouldBe task.id
        inspectionTaskInfo.tolokaTaskSuiteId shouldNotBe null
        inspectionTaskInfo.issuingOrderOverride shouldBe inspectionPriority
        inspectionTaskInfo.issuingOrderOverride!! shouldBeGreaterThan newPriority

        val inspectionTaskSuite = tolokaClientMock.getTaskSuite(inspectionTaskInfo.tolokaTaskSuiteId)
        inspectionTaskSuite shouldNotBe null
        inspectionTaskSuite.issuingOrderOverride shouldBe inspectionPriority
        inspectionTaskSuite.issuingOrderOverride shouldBeGreaterThan newPriority
    }

    private fun getSampleOutput(mappingStatus: MappingStatuses): Map<String, JsonNode> {
        val result = TaskSolution(
            task_result = listOf(
                TaskResult(
                    market_sku_id = 134,
                    offer_id = "5",
                    offer_mapping_status = mappingStatus,
                    req_id = "5"
                )
            ),
            contractorWorkerId = MAIN_WORKER_ID,
            req_id = 5
        )
        val output = CommonObjectMapper.valueToTree<JsonNode>(result)
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

    private fun defaultSaveMappingAnswer(offers: Collection<Long>): SaveTaskMappingsResponse {
        return SaveTaskMappingsResponse.newBuilder()
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

    private fun configureInspectionNeeded(inspectionNeeded: Boolean) {
        doReturn(
            YangLogStorage.YangResolveNeedInspectionResponse.newBuilder()
                .apply {
                    needInspection = inspectionNeeded
                }
                .build()
        ).`when`(yangLogStorageService).yangResolveNeedInspection(any())
    }
}

