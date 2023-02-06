package ru.yandex.market.mboc.processing.moderation.processor

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.processing.blueclassification.processor.BlueClassificationResultProcessor
import ru.yandex.market.mboc.processing.blueclassification.saver.BlueClassificationSaver

class BlueClassificationResultProcessorTest: AbstractResultProcessorTest() {
    @Autowired
    private lateinit var blueClassificationSaver: BlueClassificationSaver

    override fun initMockedSaver() {
        taskResultSaver = blueClassificationSaver
    }

    override fun initProcessor() {
        processor = BlueClassificationResultProcessor(
            keyValueService,
            transactionHelper,
            markup3ApiTaskService,
            blueClassificationSaver,
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
