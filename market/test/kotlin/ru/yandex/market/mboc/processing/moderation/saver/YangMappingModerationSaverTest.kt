package ru.yandex.market.mboc.processing.moderation.saver

import com.google.protobuf.Int64Value
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingType
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferTarget
import ru.yandex.market.mboc.common.offers.model.Offer
import ru.yandex.market.mboc.common.offers.model.OfferProcessingAssignment
import ru.yandex.market.mboc.common.services.offers.mapping.SaveMappingModerationService
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.SupplierOffer
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.SaverMode
import ru.yandex.market.mboc.processing.TaskId
import ru.yandex.market.mboc.processing.TaskResultId
import ru.yandex.market.mboc.processing.TaskResultMbocSaveStatus
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentRepository
import ru.yandex.market.mboc.processing.task.OfferProcessingTask

class YangMappingModerationSaverTest : AbstractSaverTest<Markup3Api.YangMappingModerationResult>(
    ProcessingStrategyType.YANG_MAPPING_MODERATION
) {
    @Autowired
    @Qualifier("succeedSave")
    private lateinit var succeedSaveMappingModerationService: SaveMappingModerationService

    @Autowired
    @Qualifier("failedSave")
    private lateinit var failedSaveMappingModerationService: SaveMappingModerationService

    @Autowired
    private lateinit var offerProcessingAssignmentRepository: OfferProcessingAssignmentRepository

    @Before
    override fun setUp() {
        offerProcessingAssignmentRepository.deleteAll()
        super.setUp()
    }

    @Test
    override fun `don't delete failed to save`() {
        generateOfferProcessingAssignments(20)
        super.`don't delete failed to save`()
    }

    @Test
    override fun `save one result when response has succeed status`() {
        generateOfferProcessingAssignments(20)
        super.`save one result when response has succeed status`()
    }

    @Test
    override fun `save several results when response has succeed status`() {
        generateOfferProcessingAssignments(20)
        super.`save several results when response has succeed status`()
    }

    @Test
    override fun `should save all polled task results`() {
        generateOfferProcessingAssignments(20)
        super.`should save all polled task results`()
    }

    @Test
    override fun `when empty shouldnot fail`() {
        generateOfferProcessingAssignments(20)
        super.`when empty shouldnot fail`()
    }

    @Test
    override fun `should save all results in save_all mode`() {
        generateOfferProcessingAssignments(20)
        super.`should save all results in save_all mode`()
    }

    @Test
    fun `delete only succeed to save`() {
        generateFailedSaver()
        generateOfferProcessingAssignments(20, 13)
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
        generateOfferProcessingAssignments(20, 13)
        val saveResponse =
            saver.saveResults(
                buildResultsToSaveList(
                    processingTasks,
                    List(20) { it.toLong() },
                    mskuId = null
                ),
                SaverMode.SAVE_ALL
            )

        offerProcessingTaskRepository.findAll() shouldContainExactlyInAnyOrder processingTasks
        saveResponse[TaskResultMbocSaveStatus.FAILED]!! shouldHaveSize processingTasks.size
        saveResponse[TaskResultMbocSaveStatus.FAILED]!!
            .map { it.message!! }
            .forEach { it shouldContain "no msku in MappingModerationResultItem" }
    }

    @Test
    fun `when there is no offer assignments id then results is saved`() {
        // do not generate assignments
        // generateOfferProcessingAssignments(20)
        super.`save one result when response has succeed status`()
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
    ): List<Markup3Api.TasksResultPollResponse.TaskResult> {
        return offerProcessingTasks
            .zip(taskResultIds)
            .map {
                Markup3Api.TasksResultPollResponse.TaskResult.newBuilder().apply {
                    taskid = it.first.taskId
                    taskResultId = it.second
                    result = Markup3Api.TaskResultData.newBuilder().apply {
                        yangMappingModerationResult = Markup3Api.YangMappingModerationResult.newBuilder().apply {
                            Markup3Api.YangMappingModerationResult.MappingModerationResultItem.newBuilder().apply {
                                offerId = it.first.offerId.toString()
                                mskuId?.let { this.msku = Int64Value.of(mskuId) }
                            }.build().let { addResults(it) }
                            addAllCancelledOffers(cancelledOffers)
                            staffLogin = "vasya"
                            workerId = "11"
                        }.build()
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
    ): Markup3Api.TasksResultPollResponse.TaskResult {
        return Markup3Api.TasksResultPollResponse.TaskResult.newBuilder().apply {
            this.taskid = taskId
            this.taskResultId = taskResultId
            result = Markup3Api.TaskResultData.newBuilder().apply {
                yangMappingModerationResult = Markup3Api.YangMappingModerationResult.newBuilder().apply {
                    offerProcessingTasks.map {
                        Markup3Api.YangMappingModerationResult.MappingModerationResultItem.newBuilder().apply {
                            offerId = it.offerId.toString()
                            mskuId?.let { this.msku = Int64Value.of(mskuId) }
                            status = Markup3Api.YangMappingModerationResult.MappingModerationStatus.ACCEPTED
                        }.build().let { addResults(it) }
                    }
                    addAllCancelledOffers(cancelledOffers)
                    staffLogin = "vasya"
                    workerId = "11"
                }.build()
            }.build()
        }.build()
    }

    override fun generateSucceedSaver() {
        saver = YangMappingModerationSaver(
            offerProcessingTaskRepository,
            offerProcessingAssignmentRepository,
            succeedSaveMappingModerationService,
            transactionHelper,
            ProcessingStrategyType.YANG_MAPPING_MODERATION
        )
    }

    override fun generateFailedSaver() {
        saver = YangMappingModerationSaver(
            offerProcessingTaskRepository,
            offerProcessingAssignmentRepository,
            failedSaveMappingModerationService,
            transactionHelper,
            ProcessingStrategyType.YANG_MAPPING_MODERATION
        )
    }

    private fun generateOfferProcessingAssignments(
        number: Int,
        @Suppress("UNUSED_PARAMETER") taskId: Long = 2L
    ): List<OfferProcessingAssignment> {
        return (1..number).map {
            OfferProcessingAssignment.builder().apply {
                offerId(it.toLong())
                targetSkuId(it * 100L)
                skuType(Offer.SkuType.MARKET)
                target(OfferTarget.YANG)
                type(OfferProcessingType.IN_MODERATION)
                processingTicketId(number)
            }.build()
        }.toList().also {
            offerProcessingAssignmentRepository.insertBatch(it)
        }
    }
}

