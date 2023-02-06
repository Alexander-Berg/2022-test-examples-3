package ru.yandex.market.mboc.processing.moderation.processor

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.processing.blueclassification.processor.RecheckClassificationResultProcessor
import ru.yandex.market.mboc.processing.blueclassification.saver.RecheckClassificationSaver

class RecheckClassificationResultProcessorTest : AbstractResultProcessorTest() {
    @Autowired
    private lateinit var recheckClassificationSaver: RecheckClassificationSaver

    override fun initMockedSaver() {
        taskResultSaver = recheckClassificationSaver
    }

    override fun initProcessor() {
        processor = RecheckClassificationResultProcessor(
            keyValueService,
            transactionHelper,
            markup3ApiTaskService,
            recheckClassificationSaver,
            uniqueKeysConstructorService,
            offerProcessingTaskRepository
        )
    }

    override fun whenPollResultsDoAnswer(): Markup3Api.TaskResultData {
        return Markup3Api.TaskResultData.newBuilder().apply {
            val resultData = Markup3Api.BlueClassificationResult.newBuilder().apply {
                val finishedOffers = listOf(234, 235, 236).map {
                    Markup3Api.BlueClassificationResult.BlueClassificationResultItem.newBuilder().apply {
                        offerId = it.toString()
                    }.build()
                }
                addAllOutput(finishedOffers)
            }.build()
            blueClassificationResult = resultData
        }.build()
    }
}
