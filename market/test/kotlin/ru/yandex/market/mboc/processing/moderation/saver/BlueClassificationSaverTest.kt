package ru.yandex.market.mboc.processing.moderation.saver

import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.markup3.api.Markup3Api.BlueClassificationResult
import ru.yandex.market.markup3.api.Markup3Api.TaskResultData
import ru.yandex.market.markup3.api.Markup3Api.TasksResultPollResponse
import ru.yandex.market.mboc.common.services.offers.UpdateSupplierOfferCategoryService
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.TaskId
import ru.yandex.market.mboc.processing.TaskResultId
import ru.yandex.market.mboc.processing.blueclassification.saver.BlueClassificationSaver
import ru.yandex.market.mboc.processing.task.OfferProcessingTask

class BlueClassificationSaverTest : AbstractSaverTest<BlueClassificationResult>(
    ProcessingStrategyType.BLUE_CLASSIFICATION
) {
    @Autowired
    @Qualifier("succeedUpdateSupplierOfferCategoryService")
    private lateinit var succeedUpdateSupplierOfferCategoryService: UpdateSupplierOfferCategoryService

    @Autowired
    @Qualifier("failedUpdateSupplierOfferCategoryService")
    private lateinit var failedUpdateSupplierOfferCategoryService: UpdateSupplierOfferCategoryService

    @Before
    override fun setUp() {
        cancelledOffersDeleted = false
        super.setUp()
    }

    override fun buildResultsToSaveList(
        offerProcessingTasks: List<OfferProcessingTask>,
        taskResultIds: List<TaskResultId>,
        cancelledOffers: List<Long>,
        mskuId: Long?
    ): List<TasksResultPollResponse.TaskResult> {
        return offerProcessingTasks
            .zip(taskResultIds)
            .map {
                TasksResultPollResponse.TaskResult.newBuilder().apply {
                    taskid = it.first.taskId
                    taskResultId = it.second
                    result = TaskResultData.newBuilder().apply {
                        blueClassificationResult = BlueClassificationResult.newBuilder().apply {
                            BlueClassificationResult.BlueClassificationResultItem.newBuilder().apply {
                                offerId = it.first.offerId.toString()
                            }.build().let { addOutput(it) }
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
    ): TasksResultPollResponse.TaskResult {
        return TasksResultPollResponse.TaskResult.newBuilder().apply {
            this.taskid = taskId
            this.taskResultId = taskResultId
            result = TaskResultData.newBuilder().apply {
                blueClassificationResult = BlueClassificationResult.newBuilder().apply {
                    offerProcessingTasks.forEach {
                        BlueClassificationResult.BlueClassificationResultItem.newBuilder().apply {
                            offerId = it.offerId.toString()
                        }.build().let { addOutput(it) }
                    }
                }.build()
            }.build()
        }.build()
    }

    override fun generateSucceedSaver() {
        saver = BlueClassificationSaver(
            offerProcessingTaskRepository,
            succeedUpdateSupplierOfferCategoryService,
            transactionHelper,
        )
    }

    override fun generateFailedSaver() {
        saver = BlueClassificationSaver(
            offerProcessingTaskRepository,
            failedUpdateSupplierOfferCategoryService,
            transactionHelper,
        )
    }
}

