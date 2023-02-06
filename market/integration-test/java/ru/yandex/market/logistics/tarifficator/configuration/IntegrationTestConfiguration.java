package ru.yandex.market.logistics.tarifficator.configuration;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.tarifficator.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.tarifficator.configuration.queue.DbQueueConfiguration;
import ru.yandex.market.logistics.tarifficator.jobs.producer.ActivatingPriceListProducer;
import ru.yandex.market.logistics.tarifficator.jobs.producer.ProcessUploadedPriceListProducer;
import ru.yandex.market.logistics.tarifficator.jobs.producer.ProcessUploadedWithdrawPriceListFileProducer;
import ru.yandex.market.logistics.tarifficator.service.export.DeliveryCalculatorDatasetGenerator;
import ru.yandex.market.logistics.tarifficator.service.health.checker.StubPingChecker;
import ru.yandex.market.logistics.tarifficator.service.mds.MdsFileService;
import ru.yandex.market.logistics.tarifficator.service.revision.RevisionItemService;
import ru.yandex.market.logistics.tarifficator.service.shop.changelog.ChangelogMessagePublisher;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.yt.ytclient.proxy.YtClient;

@Configuration
@EnableZonkyEmbeddedPostgres
@Import({
    DbUnitConfiguration.class,
    DbUnitTestConfiguration.class,
    DbQueueConfiguration.class,
    LiquibaseConfiguration.class,
    RepositoryConfiguration.class,
    SecurityConfiguration.class,
    ApiLoggingConfiguration.class,
    GeoBaseConfiguration.class,
    AsyncConfiguration.class,
    JacksonConfiguration.class,
    TvmTicketEnrichConfiguration.class,
    ExcelParserConfiguration.class,
    MbiMdsS3Configuration.class
})
@MockBean({
    YtClient.class,
    LMSClient.class,
    StubPingChecker.class,
    MdsS3Client.class,
    TvmClient.class,
    GeoClient.class,
    ResourceLocationFactory.class,
    TvmClientApi.class,
    Yt.class,
    Cypress.class,
    YtTables.class,
    YtOperations.class,
    ChangelogMessagePublisher.class
})
@SpyBean({
    MdsFileService.class,
    RevisionItemService.class,
    ActivatingPriceListProducer.class,
    ProcessUploadedPriceListProducer.class,
    DeliveryCalculatorDatasetGenerator.class,
    MappingJackson2XmlHttpMessageConverter.class,
    ProcessUploadedWithdrawPriceListFileProducer.class,
    FeatureProperties.class,
})
@ComponentScan({
    "ru.yandex.market.logistics.tarifficator.admin",
    "ru.yandex.market.logistics.tarifficator.service",
    "ru.yandex.market.logistics.tarifficator.controller",
    "ru.yandex.market.logistics.tarifficator.repository",
    "ru.yandex.market.logistics.tarifficator.converter",
    "ru.yandex.market.logistics.tarifficator.jobs",
    "ru.yandex.market.logistics.tarifficator.facade",
    "ru.yandex.market.logistics.tarifficator.specification",
    "ru.yandex.market.logistics.tarifficator.configuration.properties",
})
public class IntegrationTestConfiguration {

    @Bean
    public Clock clock() {
        return new TestableClock();
    }

    @Bean
    public ExecutorService tmsExecutorService() {
        return Executors.newSingleThreadExecutor();
    }
}
