package ru.yandex.market.mboc.processing

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import ru.yandex.market.mboc.common.config.repo.RepositoryConfig
import ru.yandex.market.mboc.config.MbocMappingModerationMocksConfig
import ru.yandex.market.mboc.processing.assignment.OfferProcessingAssignmentConfig
import ru.yandex.market.mboc.processing.blueclassification.saver.BlueClassificationSaver
import ru.yandex.market.mboc.processing.blueclassification.saver.RecheckClassificationSaver
import ru.yandex.market.mboc.processing.bluelogs.saver.BlueLogsSaver
import ru.yandex.market.mboc.processing.config.MarkupResultProcessorConfig
import ru.yandex.market.mboc.processing.moderation.saver.TolokaMappingModerationSaver
import ru.yandex.market.mboc.processing.moderation.saver.YangMappingModerationSaver
import ru.yandex.market.mboc.processing.task.OfferProcessingTaskConfig

@TestConfiguration
@Import(
    OfferProcessingAssignmentConfig::class,
    OfferProcessingTaskConfig::class,
    MarkupResultProcessorConfig::class,
    MbocMappingModerationMocksConfig::class,
    RepositoryConfig::class
)
open class TestOfferProcessingConfig {
    @Bean
    open fun markup3ApiService(): Markup3ApiService {
        return Mockito.mock(Markup3ApiService::class.java)
    }

    @Bean
    open fun tolokaMappingModerationSaver(): TolokaMappingModerationSaver {
        return Mockito.mock(TolokaMappingModerationSaver::class.java)
    }

    @Bean
    open fun yangMappingModerationSaver(): YangMappingModerationSaver {
        return Mockito.mock(YangMappingModerationSaver::class.java)
    }

    @Bean
    open fun blueLogsSaver(): BlueLogsSaver {
        return Mockito.mock(BlueLogsSaver::class.java)
    }

    @Bean
    open fun blueClassificationSaver(): BlueClassificationSaver {
        return Mockito.mock(BlueClassificationSaver::class.java)
    }

    @Bean
    open fun recheckClassificationSaver(): RecheckClassificationSaver {
        return Mockito.mock(RecheckClassificationSaver::class.java)
    }
}
