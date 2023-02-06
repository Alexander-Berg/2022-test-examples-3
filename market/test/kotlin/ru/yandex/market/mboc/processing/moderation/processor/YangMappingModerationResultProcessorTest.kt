package ru.yandex.market.mboc.processing.moderation.processor

import com.google.protobuf.Int64Value
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3Api
import ru.yandex.market.mboc.processing.moderation.saver.YangMappingModerationSaver

class YangMappingModerationResultProcessorTest: AbstractResultProcessorTest() {
    @Autowired
    private lateinit var yangMappingModerationSaver: YangMappingModerationSaver

    override fun initMockedSaver() {
        taskResultSaver = yangMappingModerationSaver
    }

    override fun initProcessor() {
        processor = YangMappingModerationResultProcessor(
            keyValueService,
            transactionHelper,
            markup3ApiTaskService,
            yangMappingModerationSaver,
            uniqueKeysConstructorService,
            offerProcessingTaskRepository
        )
    }

    override fun whenPollResultsDoAnswer(): Markup3Api.TaskResultData {
        return Markup3Api.TaskResultData.newBuilder().apply {
            val resultData = Markup3Api.YangMappingModerationResult.newBuilder().apply {
                val finishedOffers = listOf(234, 235, 236).map {
                    Markup3Api.YangMappingModerationResult.MappingModerationResultItem.newBuilder()
                        .apply {
                            offerId = it.toString()
                            msku = Int64Value.of(777L)
                        }.build()
                }
                addAllResults(finishedOffers)
            }.build()
            yangMappingModerationResult = resultData
        }.build()
    }
}
