package ru.yandex.market.markup3.skuconflicts

import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.ModelId
import ru.yandex.market.markup3.ParameterId
import ru.yandex.market.markup3.SkuId
import ru.yandex.market.markup3.core.TaskId
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.dto.TaskGroupConfig
import ru.yandex.market.markup3.core.executor.TaskGroupExecutor
import ru.yandex.market.markup3.core.executor.TaskGroupExecutorProvider
import ru.yandex.market.markup3.core.services.TaskService
import ru.yandex.market.markup3.mboc.OfferId
import ru.yandex.market.markup3.mboc.offertask.service.CancelledTaskWatcher
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.YangLogSaver
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.SkuParametersConflictHandler
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.ConflictData
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.ConflictParameterResult
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.ConflictResultStatus
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.ConflictSkuParameter
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.SkuParametersConflictInput
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.SkuParametersConflictResultItem
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
import java.util.Date

class SkuConflictsProcessTest : CommonTaskTest() {
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
    private lateinit var handler: SkuParametersConflictHandler

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
        val groupKey = SkuParametersConflictHandler.TASK_GROUP_KEY + "_test"
        taskGroup = taskGroupRegistry.getOrCreateTaskGroup(groupKey) {
            TaskGroup(
                key = groupKey,
                name = groupKey,
                taskType = TaskType.YANG_SKU_PARAMETER_CONFLICT,
                config = TaskGroupConfig(basePoolId = basePool.id)
            )
        }
        taskGroupExecutor = taskGroupExecutorProvider.createTaskGroupExecutor(taskGroup, sleepTimeMs = 0)

        ReflectionTestUtils.setField(handler, "taskGroup", taskGroup)
        ReflectionTestUtils.setField(handler, "cancelledCheckDelay", 0)
        ReflectionTestUtils.setField(tolokaTasksService, "secondsToCancelGap", 0)

        tolokaProfileRepository.insert(TolokaProfileRow(WORKER_ID, STAFF_LOGIN, UID))

        clearInvocations(yangLogStorageService)
    }

    @Test
    fun `test sku conflicts full process`() {
        val taskId = createSimpleTask(listOf(TestConflict(1, 1, 1)))

        var task = getOnlyOneTask()
        task.id shouldBe taskId
        task.processingStatus shouldBe ProcessingStatus.ACTIVE
        task.stage shouldBe TaskService.INIT_STAGE

        // init
        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe SkuParametersConflictHandler.Stages.ACTIVE.toString()

        val pool = getOnlyOnePool()
        pool.source shouldBe TolokaSource.YANG
        pool.active shouldBe true

        var taskInfo = getOnlyOneTaskInfo()
        taskInfo.taskId shouldBe task.id
        taskInfo.tolokaTaskSuiteId shouldNotBe null

        val taskSuite = tolokaClientMock.getTaskSuite(taskInfo.tolokaTaskSuiteId)
        taskSuite shouldNotBe null

        val workerResult = createSimpleResult(listOf(ConflictParameterResult(
            modelId = 1,
            parameterId = 1,
            offerId = "1",
            status = ConflictResultStatus.RESOLVED,
            wrongMapping = false,
            wrongCategory = false,
        )))
        val genAssignmentId = tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, workerResult, WORKER_ID)
        yangResultsDownloader.downloadAllPools()

        taskInfo = getOnlyOneTaskInfo()
        taskInfo.solution shouldNotBe null
        taskInfo.assignmentId shouldNotBe null
        taskInfo.workerId shouldNotBe null

        // TaskReady
        taskGroupExecutor.processSingleEvent()
        task = getOnlyOneTask()
        task.stage shouldBe SkuParametersConflictHandler.Stages.SAVING.toString()

        // SaveBilling
        taskGroupExecutor.processSingleEvent()

        task = getOnlyOneTask()
        task.stage shouldBe SkuParametersConflictHandler.Stages.SAVED.toString()
        task.processingStatus shouldBe ProcessingStatus.DONE

        val captor = argumentCaptor<YangLogStorage.YangLogStoreRequest>()
        verify(yangLogStorageService, times(1)).yangLogStore(captor.capture())

        val request = captor.firstValue
        with(request) {
            categoryId shouldBe 1L
            id shouldBe YangLogSaver.getYangLogStoreTaskId(task.id)
            hitmanId shouldBe YangLogSaver.getExternalTaskId(task.id)
            taskType shouldBe YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT

            val statistics = skuParameterConflictStatisticList[0]
            with(statistics) {
                this.uid shouldBe UID
                this.modelId shouldBe 1L
                this.recheckMappingOffersList shouldContainExactlyInAnyOrder listOf()
                this.parameterConflictsList shouldContainExactlyInAnyOrder listOf(
                    logStoreConflictParameter(1L, YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED),
                )
            }

            with(contractorInfo) {
                this.uid shouldBe UID
                this.poolId shouldBe pool.id.toString()
                this.taskId shouldBe taskInfo.tolokaTaskId
                this.assignmentId shouldBe genAssignmentId
            }
        }
    }

    @Test
    fun `test offers deduplication on send to YangLogStore`() {
        val taskId = createSimpleTask(listOf(
            // model conflicts
            TestConflict(offerId = 1, conflictCardId = 1, parameterId = 1, modelId = 1, mappingSkuId = 2),
            TestConflict(offerId = 1, conflictCardId = 1, parameterId = 2, modelId = 1, mappingSkuId = 2),
            TestConflict(offerId = 2, conflictCardId = 1, parameterId = 3, modelId = 1, mappingSkuId = 2),
            // sku conflict with same model
            TestConflict(offerId = 1, conflictCardId = 2, parameterId = 4, modelId = 1, mappingSkuId = 2),
            TestConflict(offerId = 2, conflictCardId = 2, parameterId = 5, modelId = 1, mappingSkuId = 2),
            TestConflict(offerId = 2, conflictCardId = 2, parameterId = 6, modelId = 1, mappingSkuId = 2),
            // another card
            TestConflict(offerId = 3, conflictCardId = 3, parameterId = 7, modelId = 3, mappingSkuId = 4),
        ))

        // init
        taskGroupExecutor.processSingleEvent()

        val baseConflictResult = ConflictParameterResult(
            modelId = 1, parameterId = 1, offerId = "1",
            status = ConflictResultStatus.REJECTED,
            wrongMapping = true, wrongCategory = false,
        )
        val taskInfo = getOnlyOneTaskInfo()
        val workerResult = createSimpleResult(listOf(
            baseConflictResult.copy(modelId = 1, parameterId = 1, offerId = "1"),
            baseConflictResult.copy(modelId = 1, parameterId = 2, offerId = "1"),
            baseConflictResult.copy(modelId = 1, parameterId = 3, offerId = "2"),

            baseConflictResult.copy(modelId = 2, parameterId = 4, offerId = "1"),
            baseConflictResult.copy(modelId = 2, parameterId = 5, offerId = "2"),
            baseConflictResult.copy(modelId = 2, parameterId = 6, offerId = "2"),

            baseConflictResult.copy(modelId = 3, parameterId = 7, offerId = "3"),
        ))
        tolokaClientMock.setTaskFinished(taskInfo.tolokaTaskSuiteId, workerResult, WORKER_ID)
        yangResultsDownloader.downloadAllPools()

        // TaskReady
        taskGroupExecutor.processSingleEvent()
        // SaveBilling
        taskGroupExecutor.processSingleEvent()

        val task = getOnlyOneTask()
        task.stage shouldBe SkuParametersConflictHandler.Stages.SAVED.toString()
        task.processingStatus shouldBe ProcessingStatus.DONE

        val captor = argumentCaptor<YangLogStorage.YangLogStoreRequest>()
        verify(yangLogStorageService, times(1)).yangLogStore(captor.capture())

        val DECISION_NEED_INFO = YangLogStorage.ParameterConflictDecision.CONFLICT_NEED_INFO
        val request = captor.firstValue
        with(request) {
            categoryId shouldBe 1L
            id shouldBe YangLogSaver.getYangLogStoreTaskId(task.id)
            hitmanId shouldBe YangLogSaver.getExternalTaskId(task.id)
            taskType shouldBe YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT

            skuParameterConflictStatisticList shouldHaveSize 4
            skuParameterConflictStatisticList.forAll { statistics ->
                statistics.uid shouldBe UID

                when (statistics.modelId) {
                    1L -> {
                        statistics.recheckMappingOffersList shouldContainExactlyInAnyOrder listOf()
                        statistics.parameterConflictsList shouldContainExactlyInAnyOrder listOf(
                            logStoreConflictParameter(1L, DECISION_NEED_INFO),
                            logStoreConflictParameter(2L, DECISION_NEED_INFO),
                            logStoreConflictParameter(3L, DECISION_NEED_INFO),
                        )
                    }

                    2L -> {
                        statistics.recheckMappingOffersList shouldContainExactlyInAnyOrder listOf(1L, 2L)
                        statistics.parameterConflictsList shouldContainExactlyInAnyOrder listOf(
                            logStoreConflictParameter(4L, DECISION_NEED_INFO),
                            logStoreConflictParameter(5L, DECISION_NEED_INFO),
                            logStoreConflictParameter(6L, DECISION_NEED_INFO),
                        )
                    }

                    3L -> {
                        statistics.recheckMappingOffersList shouldContainExactlyInAnyOrder listOf()
                        statistics.parameterConflictsList shouldContainExactlyInAnyOrder listOf(
                            logStoreConflictParameter(7L, DECISION_NEED_INFO),
                        )
                    }

                    4L -> {
                        statistics.recheckMappingOffersList shouldContainExactlyInAnyOrder listOf(3L)
                        statistics.parameterConflictsList shouldHaveSize 0
                    }

                    else -> throw AssertionError("Unknown modelId: ${statistics.modelId}")
                }
            }
        }
    }

    private fun logStoreConflictParameter(parameterId: ParameterId, decision: YangLogStorage.ParameterConflictDecision)
        = YangLogStorage.ParameterConflict.newBuilder().apply {
            this.parameterId = parameterId
            this.decision = decision
        }.build()

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

    private data class TestConflict(
        val offerId: OfferId,
        val conflictCardId: SkuId,
        val parameterId: ParameterId,
        val modelId: ModelId? = null,
        val mappingSkuId: SkuId? = null,
    )

    private fun createSimpleTask(conflicts: List<TestConflict>): TaskId {
        val conflictsByCardId = conflicts.groupBy { it.conflictCardId }

        return createTestTask(taskGroup.id, SkuParametersConflictInput(
            categoryId = 1,
            priority = .0,
            conflicts = conflictsByCardId.map { (cardId, conflicts) ->
                ConflictData(
                    modelId = conflicts[0].modelId ?: cardId,
                    conflictCardId = cardId,
                    parameters = conflicts.map {
                        ConflictSkuParameter(
                            offerId = it.offerId.toString(),
                            mappingSkuId = it.mappingSkuId ?: it.conflictCardId,
                            parameterId = it.parameterId,
                            parameterValue = listOf("test"),
                        )
                    },
                )
            },
        ))
    }

    private fun createSimpleResult(results: List<ConflictParameterResult>): Map<String, JsonNode> {
        val output = CommonObjectMapper.valueToTree<JsonNode>(
            SkuParametersConflictResultItem(
                recheckMappingOffers = null,
                results = results,
            )
        )
        return mapOf("output" to output)
    }
}
