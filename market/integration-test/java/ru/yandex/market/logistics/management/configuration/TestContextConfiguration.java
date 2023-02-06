package ru.yandex.market.logistics.management.configuration;

import javax.annotation.Nonnull;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.capacity.storage.client.CapacityStorageClient;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.blackbox.BlackBoxClient;
import ru.yandex.market.logistics.management.configuration.properties.BackwardMovementProperties;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.controller.CalendarController;
import ru.yandex.market.logistics.management.controller.PartnerController;
import ru.yandex.market.logistics.management.domain.converter.lgw.LogisticsPointConverter;
import ru.yandex.market.logistics.management.facade.LogisticsPointFacade;
import ru.yandex.market.logistics.management.plugin.tracker.TrackerTracksController;
import ru.yandex.market.logistics.management.queue.producer.BuildWarehouseSegmentsProducer;
import ru.yandex.market.logistics.management.queue.producer.DbsGraphCreationProducer;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.queue.producer.LogisticSegmentValidationProducer;
import ru.yandex.market.logistics.management.queue.producer.PechkinNotificationTaskProducer;
import ru.yandex.market.logistics.management.queue.producer.PickupPointSyncProducer;
import ru.yandex.market.logistics.management.queue.producer.UpdateDbsPartnerCargoTypesProducer;
import ru.yandex.market.logistics.management.repository.LogisticsPointCountRepository;
import ru.yandex.market.logistics.management.repository.YtOutletCountRepository;
import ru.yandex.market.logistics.management.repository.export.dynamic.s3.MdsS3BucketClient;
import ru.yandex.market.logistics.management.repository.yado.YadoDao;
import ru.yandex.market.logistics.management.service.balance.Balance2;
import ru.yandex.market.logistics.management.service.calendar.LocationCalendarsSyncService;
import ru.yandex.market.logistics.management.service.calendar.YaCalendarService;
import ru.yandex.market.logistics.management.service.client.LogisticsPointService;
import ru.yandex.market.logistics.management.service.client.PartnerRelationService;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.PartnerRelationDynamicValidationService;
import ru.yandex.market.logistics.management.service.frontend.FrontPluginsCollector;
import ru.yandex.market.logistics.management.service.graph.LogisticEdgeService;
import ru.yandex.market.logistics.management.service.graph.LogisticSegmentEntityService;
import ru.yandex.market.logistics.management.service.graph.LogisticServiceEntityService;
import ru.yandex.market.logistics.management.service.health.ping.HealthCheckerStub;
import ru.yandex.market.logistics.management.service.notification.juggler.JugglerClient;
import ru.yandex.market.logistics.management.service.point.sync.ImportPartnerPickupPointsService;
import ru.yandex.market.logistics.management.service.yt.YtLogisticsServicesUpdater;
import ru.yandex.market.logistics.management.util.BusinessWarehouseFactory;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.TransactionalUtils;
import ru.yandex.market.logistics.management.util.UuidGenerator;
import ru.yandex.market.logistics.management.util.tvm.TvmInfoExtractor;
import ru.yandex.market.logistics.oebs.client.OebsClient;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.StatelessTvmTicketProvider;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

@TestConfiguration
@EnableZonkyEmbeddedPostgres
@EnableJpaRepositories(basePackages = {
    "ru.yandex.market.logistics.management.repository",
    "ru.yandex.market.logistics.management.capacityRule"
})
@MockBean({
    MdsS3Client.class,
    MdsS3BucketClient.class,
    YadoDao.class,
    YaCalendarService.class,
    HealthCheckerStub.class,
    JugglerClient.class,
    FrontPluginsCollector.class,
    DeliveryClient.class,
    TvmTicketProvider.class,
    GeoClient.class,
    PechkinHttpClient.class,
    TvmClientApi.class,
    TrackerTracksController.class,
    BlackBoxClient.class,
    Balance2.class,
    Yt.class,
    OebsClient.class,
    HttpGeobase.class,
    LogbrokerEventPublisher.class,
    TvmInfoExtractor.class,
    CapacityStorageClient.class,
})
@SpyBean({
    LogisticEdgeService.class,
    LogisticSegmentEntityService.class,
    LocationCalendarsSyncService.class,
    PartnerRelationDynamicValidationService.class,
    PartnerRelationService.class,
    LogisticsPointService.class,
    CalendarController.class,
    RegionService.class,
    PartnerController.class,
    LogisticsPointCountRepository.class,
    YtOutletCountRepository.class,
    TransactionTemplate.class,
    CacheManager.class,
    PechkinNotificationTaskProducer.class,
    LogisticsPointConverter.class,
    ImportPartnerPickupPointsService.class,
    UuidGenerator.class,
    LogbrokerEventTaskProducer.class,
    PickupPointSyncProducer.class,
    LogisticsPointFacade.class,
    YtLogisticsServicesUpdater.class,
    FeatureProperties.class,
    BackwardMovementProperties.class,
    UpdateDbsPartnerCargoTypesProducer.class,
    LogisticSegmentValidationProducer.class,
    DbsGraphCreationProducer.class,
    BuildWarehouseSegmentsProducer.class,
    LogisticServiceEntityService.class,
})
@Import({
    DatasourceConfig.class,
    PostgresDatabaseCleaner.class,
    TestAsyncConfiguration.class,
    TestableClock.class,
    TransactionalUtils.class,
    CacheConfiguration.class,
    TestBalanceConfiguration.class,
    BusinessWarehouseFactory.class,
})
public class TestContextConfiguration {

    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean(TestableClock clock) {
        return new LocalValidatorFactoryBean() {
            @Override
            protected void postProcessConfiguration(@Nonnull javax.validation.Configuration<?> configuration) {
                super.postProcessConfiguration(configuration);
                configuration.clockProvider(() -> clock);
            }
        };
    }

    @Bean(value = {
        "lgwTvmTicketProvider",
        "geosearchTvmTicketProvider",
        "calendarTvmTicketProvider",
        "logistics4ShopsTvmTicketProvider",
        "lomTvmTicketProvider",
        "lrmTvmTicketProvider",
        "stockstorageTvmTicketProvider",
        "nesuTvmTicketProvider",
        "tarifficatorTvmTicketProvider",
        "mdbTvmTicketProvider",
        "tmTvmTicketProvider",
        "balanceTvmTicketProvider",
        "tplTvmTicketProvider",
        "pvzTvmTicketProvider",
        "capacityStorageTvmTicketProvider",
        "carrierTvmTicketProvider",
        "lesTvmTicketProvider",
        "hrmsTvmTicketProvider",
        "mqmTvmTicketProvider"
    })
    public TvmTicketProvider tvmTicketProvider() {
        return Mockito.mock(TvmTicketProvider.class);
    }

    @Bean
    public StatelessTvmTicketProvider statelessTicketProvider() {
        return tvmServiceId -> tvmTicketProvider().provideServiceTicket();
    }

    @Bean
    @ConfigurationProperties("self")
    public ExternalServiceProperties selfProperties() {
        return new ExternalServiceProperties();
    }

    @Bean
    public HttpTemplate lomHttpTemplate() {
        return Mockito.mock(HttpTemplate.class);
    }

    @Bean
    @Qualifier("backup")
    public Yt backupYt() {
        return Mockito.mock(Yt.class);
    }
}
