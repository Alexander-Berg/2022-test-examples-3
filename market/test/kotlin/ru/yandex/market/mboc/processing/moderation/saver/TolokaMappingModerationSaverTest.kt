package ru.yandex.market.mboc.processing.moderation.saver

import com.google.protobuf.Int64Value
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.markup3.api.Markup3Api.TasksResultPollResponse
import ru.yandex.market.markup3.api.Markup3Api.TolokaMappingModerationResult
import ru.yandex.market.mboc.common.services.offers.mapping.SaveMappingModerationService
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.SaverMode
import ru.yandex.market.mboc.processing.TaskId
import ru.yandex.market.mboc.processing.TaskResultId
import ru.yandex.market.mboc.processing.TaskResultMbocSaveStatus
import ru.yandex.market.mboc.processing.task.OfferProcessingTask

class TolokaMappingModerationSaverTest : AbstractSaverTest<TolokaMappingModerationResult>(
    ProcessingStrategyType.TOLOKA_MAPPING_MODERATION
) {
    @Autowired
    @Qualifier("succeedSave")
    private lateinit var succeedSaveMappingModerationService: SaveMappingModerationService
    @Autowired
    @Qualifier("failedSave")
    private lateinit var failedSaveMappingModerationService: SaveMappingModerationService

    @Test
    fun `delete only succeed to save`() {
        generateFailedSaver()
        val processingTasks = generateActiveOfferProcessingTasks(20, 13L)
        val saveResult =
            saver.saveResults(
                listOf(buildOneResultsToSaveList(processingTasks, 666L, 13L)),
                SaverMode.SAVE_ALL
            )

        val expectedTasks = getExpectedSavedOffers(processingTasks)

        offerProcessingTaskRepository.findNonConsumed() shouldContainExactlyInAnyOrder expectedTasks
        expectedTasks shouldNotHaveSize processingTasks.size
        saveResult[TaskResultMbocSaveStatus.SEMI_FAILED]!! shouldHaveSize 1
    }

    @Test
    fun `when there is no msku id then throw exception while saving`() {
        generateSucceedSaver()
        val processingTasks = generateActiveOfferProcessingTasks(20, 13L)
        val saveResponse =
            saver.saveResults(buildResultsToSaveList(
                processingTasks,
                List(20, { it.toLong() }),
                mskuId = null),
                SaverMode.SAVE_ALL
            )
        val expectedTasks = processingTasks

        offerProcessingTaskRepository.findAll() shouldContainExactlyInAnyOrder expectedTasks
        saveResponse[TaskResultMbocSaveStatus.FAILED]!! shouldHaveSize expectedTasks.size
        saveResponse[TaskResultMbocSaveStatus.FAILED]!!
            .map{ it.message!! }
            .forEach { it shouldContain "no msku in MappingModerationResultItem" }
    }

    private fun buildSaveRequest(
        offerProcessingTasks: List<OfferProcessingTask>
    ): MboCategory.SaveMappingsModerationRequest =
        MboCategory.SaveMappingsModerationRequest.newBuilder().apply {
            val a = offerProcessingTasks.map { processingTask ->
                SupplierOffer.MappingModerationTaskResult.newBuilder().apply {
                    offerId = processingTask.offerId.toString()
                    taskId = processingTask.taskId
                    fromToloka = true
                }.build()
            }
            addAllResults(a)
        }.build()

    private fun getExpectedSavedOffers(
        processingTasks: List<OfferProcessingTask>
    ): List<OfferProcessingTask> {
        val saveResponse = failedSaveMappingModerationService.saveMappingsModeration(buildSaveRequest(processingTasks))
        val savedOffers = saveResponse.result.offerStatusesList.asSequence()
            .filter {
                it.status != SupplierOffer.OperationStatus.ERROR
            }.map {
                it.offerId.toLong()
            }.toSet()
        return processingTasks.filterNot { savedOffers.contains(it.offerId) }
    }

    override fun buildResultsToSaveList(
        offerProcessingTasks: List<OfferProcessingTask>,
        taskResultIds: List<TaskResultId>,
        cancelledOffers: List<Long>,
        mskuId: Long?
    ) : List<TasksResultPollResponse.TaskResult> {
        return offerProcessingTasks
            .zip(taskResultIds)
            .map {
                TasksResultPollResponse.TaskResult.newBuilder().apply {
                    taskid = it.first.taskId
                    taskResultId = it.second
                    result = Markup3Api.TaskResultData.newBuilder().apply {
                        setTolokaMappingModerationResult(TolokaMappingModerationResult.newBuilder().apply {
                            TolokaMappingModerationResult.MappingModerationResultItem.newBuilder().apply {
                                offerId = it.first.offerId.toString()
                                mskuId?.let { this.msku = Int64Value.of(mskuId) }
                            }.build().let { addFinishedOffers(it) }
                            addAllCancelledOffers(cancelledOffers)
                        }.build())
                    }.build()
                }.build()
            }.toList()
    }

    override fun buildOneResultsToSaveList(
        offerProcessingTasks: List<OfferProcessingTask>,
        taskResultId: TaskResultId,
        taskId: TaskId,
        cancelledOffers: List<Long>,
        mskuId: Long?
    ): TasksResultPollResponse.TaskResult {
        return TasksResultPollResponse.TaskResult.newBuilder().apply {
            this.taskid = taskId
            this.taskResultId = taskResultId
            result = Markup3Api.TaskResultData.newBuilder().apply {
                setTolokaMappingModerationResult(TolokaMappingModerationResult.newBuilder().apply {
                    offerProcessingTasks.map {
                        TolokaMappingModerationResult.MappingModerationResultItem.newBuilder().apply {
                            offerId = it.offerId.toString()
                            mskuId?.let{ this.msku = Int64Value.of(mskuId) }
                        }.build().let { addFinishedOffers(it) }
                    }
                    addAllCancelledOffers(cancelledOffers)
                }.build())
            }.build()
        }.build()
    }

    override fun generateSucceedSaver() {
        saver = TolokaMappingModerationSaver(
            offerProcessingTaskRepository,
            succeedSaveMappingModerationService,
            transactionHelper,
        )
    }

    override fun generateFailedSaver() {
        saver = TolokaMappingModerationSaver(
            offerProcessingTaskRepository,
            failedSaveMappingModerationService,
            transactionHelper,
        )
    }
}
