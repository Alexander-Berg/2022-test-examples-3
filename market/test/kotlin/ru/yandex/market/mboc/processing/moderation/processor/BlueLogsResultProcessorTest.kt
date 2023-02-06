package ru.yandex.market.mboc.processing.moderation.processor

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.processing.bluelogs.processor.BlueLogsResultProcessor
import ru.yandex.market.mboc.processing.bluelogs.saver.BlueLogsSaver

class BlueLogsResultProcessorTest: AbstractResultProcessorTest() {
    @Autowired
    private lateinit var blueLogsSaver: BlueLogsSaver

    override fun initMockedSaver() {
        taskResultSaver = blueLogsSaver
    }

    override fun initProcessor() {
        processor = BlueLogsResultProcessor(
            keyValueService,
            transactionHelper,
            markup3ApiTaskService,
            blueLogsSaver,
            uniqueKeysConstructorService,
            offerProcessingTaskRepository
        )
    }

    override fun whenPollResultsDoAnswer(): Markup3Api.TaskResultData {
        return Markup3Api.TaskResultData.newBuilder().apply {
            val resultData = Markup3Api.BlueLogsResult.newBuilder().apply {
                solution = Markup3Api.BlueLogsResult.TaskSolution.newBuilder().apply {
                    val finishedOffers = listOf(234, 235, 236).map {
                        Markup3Api.BlueLogsResult.TaskResult.newBuilder().apply {
                            offerId = it.toString()
                        }.build()
                    }
                    addAllTaskResult(finishedOffers)
                }.build()
            }.build()
            blueLogsResult = resultData
        }.build()
    }
}
