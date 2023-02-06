package ru.yandex.market.mboc.processing.moderation.processor

import com.google.protobuf.Int64Value
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.processing.moderation.saver.TolokaMappingModerationSaver

class TolokaMappingModerationResultProcessorTest: AbstractResultProcessorTest() {
    @Autowired
    private lateinit var tolokaMappingModerationSaver: TolokaMappingModerationSaver

    override fun initMockedSaver() {
        taskResultSaver = tolokaMappingModerationSaver
    }

    override fun initProcessor() {
        processor = TolokaMappingModerationResultProcessor(
            keyValueService,
            transactionHelper,
            markup3ApiTaskService,
            tolokaMappingModerationSaver,
            uniqueKeysConstructorService,
            offerProcessingTaskRepository
        )
    }

    override fun whenPollResultsDoAnswer(): Markup3Api.TaskResultData {
        return Markup3Api.TaskResultData.newBuilder().apply {
            val resultData = Markup3Api.TolokaMappingModerationResult.newBuilder().apply {
                val finishedOffers = listOf(234, 235, 236).map {
                    Markup3Api.TolokaMappingModerationResult.MappingModerationResultItem.newBuilder()
                        .apply {
                            offerId = it.toString()
                            msku = Int64Value.of(777L)
                        }.build()
                }
                addAllFinishedOffers(finishedOffers)
            }.build()
            tolokaMappingModerationResult = resultData
        }.build()
    }
}
