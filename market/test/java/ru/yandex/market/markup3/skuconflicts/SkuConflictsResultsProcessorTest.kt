package ru.yandex.market.markup3.skuconflicts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.ModelId
import ru.yandex.market.markup3.ParameterId
import ru.yandex.market.markup3.SkuId
import ru.yandex.market.markup3.core.TaskId
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.repositories.TaskDbService
import ru.yandex.market.markup3.core.services.TaskGroupRegistry
import ru.yandex.market.markup3.core.services.TaskResult
import ru.yandex.market.markup3.core.services.TaskResultService
import ru.yandex.market.markup3.mboc.OfferId
import ru.yandex.market.markup3.mboc.category.CategoryId
import ru.yandex.market.markup3.mocks.MboMappingsServiceMock
import ru.yandex.market.markup3.remote.ModelStorageService
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsAssignmentRepository
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsAssignmentRow
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsAssignmentStatus
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsResultLogRepository
import ru.yandex.market.markup3.skuconflicts.repository.SkuConflictsResultLogResultState
import ru.yandex.market.markup3.tasks.TaskType
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.SkuParametersConflictHandler
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.ConflictParameterResult
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.ConflictResultStatus
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.SkuParametersConflictResultItem
import ru.yandex.market.markup3.tasks.sku_parameters_conflict.yang.dto.SkuParametersConflictTaskResult
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.mbo.http.ModelStorage
import ru.yandex.market.mboc.http.MboMappings
import ru.yandex.market.mboc.http.MboMappings.SendToRecheckMappingResponse
import kotlin.properties.Delegates

class SkuConflictsResultsProcessorTest : BaseAppTest() {
    @Autowired
    lateinit var taskDbService: TaskDbService

    @Autowired
    private lateinit var taskGroupRegistry: TaskGroupRegistry

    @Autowired
    private lateinit var skuConflictsAssignmentRepository: SkuConflictsAssignmentRepository

    @Autowired
    private lateinit var skuConflictsResultLogRepository: SkuConflictsResultLogRepository

    private var mboMappingsService by Delegates.notNull<MboMappingsServiceMock>()
    private var taskResultServiceMock by Delegates.notNull<TaskResultService>()
    private var processor by Delegates.notNull<SkuConflictsResultsProcessor>()
    private var modelStorageService by Delegates.notNull<ModelStorageService>()

    private lateinit var taskGroup: TaskGroup

    @Before
    fun setup() {
        mboMappingsService = mock()
        taskResultServiceMock = mock()
        modelStorageService = mock()

        taskGroup = taskGroupRegistry.getOrCreateTaskGroup(SkuParametersConflictHandler.TASK_GROUP_KEY) {
            TaskGroup(
                key = SkuParametersConflictHandler.TASK_GROUP_KEY,
                name = "Конфликты в параметрах sku",
                taskType = TaskType.YANG_SKU_PARAMETER_CONFLICT,
            )
        }

        processor = SkuConflictsResultsProcessor(
            skuConflictsAssignmentRepository,
            taskResultServiceMock,
            transactionTemplate,
            taskGroupRegistry,
            skuConflictsResultLogRepository,
            mboMappingsService,
            modelStorageService
        )
    }

    @Test
    fun `Consumes all valid results`() {
        val results = mutableListOf<TaskResult<SkuParametersConflictTaskResult>>()
        for (i in 1L..4L) {
            val assignment = assignment(i, i, SkuConflictsAssignmentStatus.IN_YANG)
            skuConflictsAssignmentRepository.insert(assignment)
            results.add(taskResult(i, 1, listOf(
                conflictResultFromAssignment(assignment, ConflictResultStatus.RESOLVED)
            )))
        }

        doReturn(results, listOf<TaskResult<SkuParametersConflictTaskResult>>())
            .`when`(taskResultServiceMock)
            .pollResults<SkuParametersConflictTaskResult>(
                taskGroupId = eq(taskGroup.id), limit = any()
            )

        processor.processResults()

        verify(taskResultServiceMock, times(1))
            .consumeResults(eq(taskGroup.id), eq(listOf(1, 2, 3, 4)))

        skuConflictsAssignmentRepository.findAll() shouldHaveSize 0

        val logs = skuConflictsResultLogRepository.findAll()
        logs shouldHaveSize 4
        logs.forAll {
            it.resultState shouldBe SkuConflictsResultLogResultState.PROCESSED_YANG.toString()
        }
    }

    @Test
    fun `Deletes only ready assignments`() {
        val results = listOf(
            taskResult(1, 1, listOf()),
            taskResult(2, 1, listOf()),
            taskResult(100, 1, listOf()),
        )

        skuConflictsAssignmentRepository.insertBatch(
            assignment(1, 1, SkuConflictsAssignmentStatus.IN_YANG),
            // invalid statuses should be consumed, but with warn logging
            assignment(1, 2, SkuConflictsAssignmentStatus.NEW),
            assignment(2, 3, SkuConflictsAssignmentStatus.IN_YANG),
            assignment(3, 4, SkuConflictsAssignmentStatus.IN_YANG),
            assignment(null, 5, SkuConflictsAssignmentStatus.NEW),
        )

        doReturn(results, listOf<TaskResult<SkuParametersConflictTaskResult>>())
            .`when`(taskResultServiceMock)
            .pollResults<SkuParametersConflictTaskResult>(
                taskGroupId = eq(taskGroup.id), limit = any()
            )

        processor.processResults()

        verify(taskResultServiceMock, times(1))
            .consumeResults(eq(taskGroup.id), eq(listOf(1, 2, 100)))

        skuConflictsAssignmentRepository.findAll() shouldHaveSize 2

        val allLogs = skuConflictsResultLogRepository.findAll()

        allLogs shouldHaveSize 4

        allLogs.forEach {
            when (it.markupTaskId) {
                1L -> {
                    when (it.parameterId) {
                        1L -> it.resultState shouldBe SkuConflictsResultLogResultState.PROCESSED_YANG.toString()

                        2L -> {
                            it.resultState.split(",") shouldContainExactlyInAnyOrder listOf(
                                SkuConflictsResultLogResultState.PROCESSED_YANG.toString(),
                                SkuConflictsResultLogResultState.WRONG_STATUS.toString(),
                            )
                        }

                        else -> throw AssertionError("Unexpected parameterId ${it.parameterId} for task ${it.markupTaskId}")
                    }
                }

                2L -> {
                    it.parameterId shouldBe 3
                    it.resultState shouldBe SkuConflictsResultLogResultState.PROCESSED_YANG.toString()
                }

                100L -> {
                    it.resultState.split(",") shouldContainExactlyInAnyOrder listOf(
                        SkuConflictsResultLogResultState.PROCESSED_YANG.toString(),
                        SkuConflictsResultLogResultState.ASSIGNMENT_NOT_FOUND.toString(),
                    )
                }

                else -> throw AssertionError("Unexpected task_id ${it.markupTaskId}")
            }
        }
    }

    @Test
    fun `Send to recheck mapping in mbo-category`() {
        val assignments = listOf(
            assignment(1, 1, SkuConflictsAssignmentStatus.IN_YANG, 1, 1),
            assignment(1, 2, SkuConflictsAssignmentStatus.IN_YANG, 2, 1),
            assignment(2, 3, SkuConflictsAssignmentStatus.IN_YANG, 3, 2),
            // dup offer with wrong mapping
            assignment(1, 4, SkuConflictsAssignmentStatus.IN_YANG, 3, 2),
            assignment(2, 5, SkuConflictsAssignmentStatus.IN_YANG, 3, 2),

            assignment(null, 6, SkuConflictsAssignmentStatus.NEW, 4, 3),
        )

        val results = listOf(
            taskResult(1, 1, listOf(
                conflictResultFromAssignment(assignments[0], ConflictResultStatus.REJECTED).copy(wrongMapping = true),
                conflictResultFromAssignment(assignments[1], ConflictResultStatus.REJECTED).copy(wrongMapping = true),
                conflictResultFromAssignment(assignments[3], ConflictResultStatus.REJECTED).copy(wrongMapping = true),
            )),
            taskResult(2, 2, listOf(
                conflictResultFromAssignment(assignments[2], ConflictResultStatus.REJECTED).copy(wrongMapping = true),
                conflictResultFromAssignment(assignments[4], ConflictResultStatus.REJECTED).copy(wrongMapping = true),
            )),
        )

        doReturn(results, listOf<TaskResult<SkuParametersConflictTaskResult>>())
            .`when`(taskResultServiceMock)
            .pollResults<SkuParametersConflictTaskResult>(
                taskGroupId = eq(taskGroup.id), limit = any()
            )

        doReturn(recheckResult(listOf(
            recheckAnswerOk(1, 1),
            recheckAnswerError(2, 1, "have no approved mapping"),
            recheckAnswerError(3, 2, "The current approved mapping does not match the passed mapping id. "
                + "Approved mapping id = 100, mapping id = 1"),
        )))
            .`when`(mboMappingsService)
            .sendToRecheck(request = any())

        skuConflictsAssignmentRepository.insertBatch(assignments)

        processor.processResults()

        verify(taskResultServiceMock, times(1))
            .consumeResults(eq(taskGroup.id), eq(listOf(1, 2)))

        verify(mboMappingsService, times(1))
            .sendToRecheck(check { request ->
                request.recheckMappingSources shouldBe MboMappings.SendToRecheckMappingRequest.RecheckMappingSource.OPERATOR_CONFLICT
                request.offerAndMappingIdsList.map { Pair(it.offerId, it.mappingId) } shouldContainExactlyInAnyOrder listOf(
                    Pair(1L, 1L),
                    Pair(2L, 1L),
                    Pair(3L, 2L),
                )
            })

        skuConflictsAssignmentRepository.findAll() shouldHaveSize 1

        val allLogs = skuConflictsResultLogRepository.findAll()

        allLogs shouldHaveSize 5

        allLogs.forAll {
            it.parameterId!! shouldBeLessThan 6
            it.resultState.split(",") shouldContainExactlyInAnyOrder listOf(
                SkuConflictsResultLogResultState.PROCESSED_YANG.toString(),
                SkuConflictsResultLogResultState.TO_RECHECK_MAPPING.toString(),
            )
        }
    }

    @Test
    fun `Mark for reassign conflicts with wrong category`() {
        val assignments = listOf(
            assignment(1, 1, SkuConflictsAssignmentStatus.IN_YANG, 1, 1),
            assignment(1, 2, SkuConflictsAssignmentStatus.IN_YANG, 2, 2),
            assignment(2, 3, SkuConflictsAssignmentStatus.IN_YANG, 3, 3),
            assignment(2, 4, SkuConflictsAssignmentStatus.IN_YANG, 3, 3),
            assignment(2, 5, SkuConflictsAssignmentStatus.IN_YANG, 4, 4),
        )

        val results = listOf(
            taskResult(1, 1, listOf(
                conflictResultFromAssignment(assignments[0], ConflictResultStatus.UNPROCESSED).copy(wrongCategory = true),
                conflictResultFromAssignment(assignments[1], ConflictResultStatus.UNPROCESSED).copy(wrongCategory = true),
            )),
            taskResult(2, 2, listOf(
                conflictResultFromAssignment(assignments[2], ConflictResultStatus.UNPROCESSED).copy(wrongCategory = true),
                conflictResultFromAssignment(assignments[3], ConflictResultStatus.UNPROCESSED).copy(wrongCategory = true),
                conflictResultFromAssignment(assignments[4], ConflictResultStatus.RESOLVED),
            )),
        )

        skuConflictsAssignmentRepository.insertBatch(assignments)

        doReturn(results, listOf<TaskResult<SkuParametersConflictTaskResult>>())
            .`when`(taskResultServiceMock)
            .pollResults<SkuParametersConflictTaskResult>(
                taskGroupId = eq(taskGroup.id), limit = any()
            )

        doReturn(listOf(
            ModelStorage.Model.newBuilder().apply { id = 1; categoryId = 11 }.build(),
            ModelStorage.Model.newBuilder().apply { id = 3; categoryId = 13 }.build(),
            // conflictCard 2 is missing, it's correct
        ))
            .`when`(modelStorageService)
            .findModels(eq(
                assignments
                    .filter { it.parameterId != 5L }
                    .mapTo(mutableSetOf()) { it.conflictCardId })
            )

        processor.processResults()

        verify(taskResultServiceMock, times(1))
            .consumeResults(eq(taskGroup.id), eq(listOf(1, 2)))

        val assignmentsAfter = skuConflictsAssignmentRepository.findAll()

        assignmentsAfter shouldHaveSize 4
        assignmentsAfter.forEach {
            when (it.parameterId) {
                1L -> {
                    it.status shouldBe SkuConflictsAssignmentStatus.NEW
                    it.categoryId shouldBe 11L
                }

                3L, 4L -> {
                    it.status shouldBe SkuConflictsAssignmentStatus.NEW
                    it.categoryId shouldBe 13L
                }

                2L -> {
                    it.status shouldBe SkuConflictsAssignmentStatus.FAILED
                    it.categoryId shouldBe 1L
                    it.lastError shouldNotBe null
                }

                else -> throw AssertionError("Unexpected parameter_id ${it.parameterId}")
            }
        }

        val allLogs = skuConflictsResultLogRepository.findAll()

        allLogs shouldHaveSize 5
        allLogs.forEach {
            if (it.parameterId == 5L) {
                it.resultState shouldBe SkuConflictsResultLogResultState.PROCESSED_YANG.toString()
            } else {
                it.parameterId shouldBeOneOf listOf(1L, 2L, 3L, 4L)
                it.resultState.split(",") shouldContainExactlyInAnyOrder listOf(
                    SkuConflictsResultLogResultState.PROCESSED_YANG.toString(),
                    SkuConflictsResultLogResultState.WRONG_CATEGORY.toString(),
                )
            }
        }
    }

    private fun assignment(
        markupTaskId: TaskId?,
        parameterId: ParameterId,
        status: SkuConflictsAssignmentStatus,
        offerId: OfferId = 1,
        conflictCardId: ModelId = 1,
    ) = SkuConflictsAssignmentRow(
        status = status,
        offerId = offerId,
        categoryId = 1,
        conflictCardId = conflictCardId,
        mappingSkuId = conflictCardId,
        parentModelId = conflictCardId+100,
        parameterId = parameterId,
        parameterValue = listOf(),
        markupTaskId = markupTaskId,
    )

    private fun conflictResultFromAssignment(
        assignment: SkuConflictsAssignmentRow,
        status: ConflictResultStatus,
    ) = ConflictParameterResult(
        modelId = assignment.conflictCardId,
        parameterId = assignment.parameterId,
        offerId = assignment.offerId.toString(),
        status = status,
        wrongMapping = false,
        wrongCategory = false,
    )

    private fun taskResult(id: TaskId, categoryId: CategoryId, results: List<ConflictParameterResult>) = TaskResult(
        taskId = id,
        externalKey = null,
        taskResultId = id,
        data = SkuParametersConflictTaskResult(
            staffLogin = "test",
            categoryId = categoryId,
            result = SkuParametersConflictResultItem(
                recheckMappingOffers = null,
                results = results,
            ),
        ),
    )

    private fun recheckResult(answers: List<MboMappings.Answer>) = SendToRecheckMappingResponse.newBuilder().apply {
        addAllAnswers(answers)
    }.build()

    private fun recheckAnswerOk(aOfferId: OfferId, aMappingId: SkuId) = MboMappings.Answer.newBuilder().apply {
        offerAndMappingIds = MboMappings.OfferAndMappingIds.newBuilder().apply {
            offerId = aOfferId
            mappingId = aMappingId
        }.build()

        result = MboMappings.Answer.Result.newBuilder().apply {
            status = MboMappings.Answer.Result.Status.OK
        }.build()
    }.build()

    private fun recheckAnswerError(aOfferId: OfferId, aMappingId: SkuId, msg: String?) = MboMappings.Answer.newBuilder().apply {
        offerAndMappingIds = MboMappings.OfferAndMappingIds.newBuilder().apply {
            offerId = aOfferId
            mappingId = aMappingId
        }.build()

        result = MboMappings.Answer.Result.newBuilder().apply {
            status = MboMappings.Answer.Result.Status.ERROR
            message = msg
        }.build()
    }.build()
}
