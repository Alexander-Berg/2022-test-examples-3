package ru.yandex.market.mboc.processing.moderation.saver

import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.common.services.offers.mapping.SaveTaskMappingsService
import ru.yandex.market.mboc.processing.ProcessingStrategyType
import ru.yandex.market.mboc.processing.TaskId
import ru.yandex.market.mboc.processing.TaskResultId
import ru.yandex.market.mboc.processing.bluelogs.saver.BlueLogsSaver
import ru.yandex.market.mboc.processing.task.OfferProcessingTask

class BlueLogsSaverTest : AbstractSaverTest<Markup3Api.BlueLogsResult>(
    ProcessingStrategyType.BLUE_LOGS
) {
    @Autowired
    @Qualifier("succeedSaveTaskMapping")
    private lateinit var succeedSaveTaskMappingsService: SaveTaskMappingsService
    @Autowired
    @Qualifier("failedSaveTaskMapping")
    private lateinit var failedSaveTaskMappingsService: SaveTaskMappingsService

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
    ) : List<Markup3Api.TasksResultPollResponse.TaskResult> {
        return offerProcessingTasks
            .zip(taskResultIds)
            .map {
                Markup3Api.TasksResultPollResponse.TaskResult.newBuilder().apply {
                    taskid = it.first.taskId
                    taskResultId = it.second
                    result = Markup3Api.TaskResultData.newBuilder().apply {
                        setBlueLogsResult(Markup3Api.BlueLogsResult.newBuilder().apply {
                            solution = Markup3Api.BlueLogsResult.TaskSolution.newBuilder().apply {
                                Markup3Api.BlueLogsResult.TaskResult.newBuilder().apply {
                                    offerId = it.first.offerId.toString()
                                }.build().let { addTaskResult(it) }
                            }.build()
                            staffLogin = "vasya"
                            categoryId = 1
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
    ): Markup3Api.TasksResultPollResponse.TaskResult {
        return Markup3Api.TasksResultPollResponse.TaskResult.newBuilder().apply {
            this.taskid = taskId
            this.taskResultId = taskResultId
            result = Markup3Api.TaskResultData.newBuilder().apply {
                setBlueLogsResult(Markup3Api.BlueLogsResult.newBuilder().apply {
                    staffLogin = "vasya"
                    categoryId = 1
                    solution = Markup3Api.BlueLogsResult.TaskSolution.newBuilder().apply {
                        offerProcessingTasks.map {
                            Markup3Api.BlueLogsResult.TaskResult.newBuilder().apply {
                                offerId = it.offerId.toString()
                            }.build().let { addTaskResult(it) }
                        }
                    }.build()
                }.build())
            }.build()
        }.build()
    }

    override fun generateSucceedSaver() {
        saver = BlueLogsSaver(
            offerProcessingTaskRepository,
            succeedSaveTaskMappingsService,
            transactionHelper,
        )
    }

    override fun generateFailedSaver() {
        saver = BlueLogsSaver(
            offerProcessingTaskRepository,
            failedSaveTaskMappingsService,
            transactionHelper,
        )
    }
}
