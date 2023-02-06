package ru.yandex.market.mbi.feed.processor.config

import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.transaction.PlatformTransactionManager
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import ru.yandex.market.mbi.feed.processor.environment.EnvironmentService
import ru.yandex.market.mbi.feed.processor.environment.UnitedEnvironmentService
import ru.yandex.market.mbi.feed.processor.mds.MbiMdsClient
import ru.yandex.market.mbi.feed.processor.parsing.result.FeedParsingResultEventNotification
import ru.yandex.market.mbi.feed.processor.parsing.update.event.DataCampFeedUpdateLogbrokerEvent
import ru.yandex.market.mbi.feed.processor.samovar.proxy.SamovarResultDataProxyEvent
import ru.yandex.market.mbi.feed.processor.samovar.request.event.SamovarFeedDownloadLogbrokerEvent
import ru.yandex.market.mbi.feed.processor.yt.reader.factory.YtTableReaderFactory
import ru.yandex.market.mbi.open.api.client.api.FeedApi
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Конфигурация функциональных тестов.
 */
@TestConfiguration
internal class FunctionalTestConfig {

    @Bean
    @Primary
    fun transactionManager(txManager: PlatformTransactionManager) = txManager

    @Bean
    fun clock(): Clock = Clock.fixed(Instant.parse("2018-06-19T10:01:30.00Z"), ZoneId.of("Asia/Vladivostok"))

    @Bean
    fun ytFeedParsingClientProxySource(): YtClientProxySource {
        return mock { }
    }

    @Bean
    fun ytFeedParsingClientProxy(): YtClientProxy {
        return mock { }
    }

    @Bean
    fun ytSamovarFeedDownloadClientProxySource(): YtClientProxySource {
        return mock { }
    }

    @Bean
    fun ytSamovarFeedDownloadClientProxy(): YtClientProxy {
        return mock { }
    }

    @Bean
    fun ytDatacampParsingHistoryClientProxySource(): YtClientProxySource {
        return mock { }
    }

    @Bean
    fun ytDatacampParsingHistoryClientProxy(): YtClientProxy {
        return mock { }
    }

    @Bean
    fun dataCampMarketFeedUpdateLogbrokerService(): LogbrokerEventPublisher<DataCampFeedUpdateLogbrokerEvent> {
        return mock { }
    }

    @Bean
    fun dataCampFeedUpdateLogbrokerService(): LogbrokerEventPublisher<DataCampFeedUpdateLogbrokerEvent> {
        return mock { }
    }

    @Bean
    fun samovarFeedDownloadRequestLogbrokerService(): LogbrokerEventPublisher<SamovarFeedDownloadLogbrokerEvent> {
        return mock { }
    }

    @Bean
    fun samovarResultDataProxyLogbrokerService(): LogbrokerEventPublisher<SamovarResultDataProxyEvent> {
        return mock { }
    }

    @Bean
    fun mbiFeedApiClient(): FeedApi {
        return mock { }
    }

    @Bean
    fun mbiFeedUpdateNotificationLogbrokerService(): LogbrokerEventPublisher<FeedParsingResultEventNotification> {
        return mock { }
    }

    @Bean
    fun ytTableReaderFactory(): YtTableReaderFactory {
        return mock { }
    }

    @Bean
    fun mbiMdsClient(): MbiMdsClient {
        return mock { }
    }

    @Bean
    fun mdsS3Client(): MdsS3Client {
        return mock { }
    }

    @Bean
    fun unitedEnvironmentService(
        environmentService: EnvironmentService
    ): UnitedEnvironmentService {
        return UnitedEnvironmentService(environmentService, useCache = false)
    }
}
