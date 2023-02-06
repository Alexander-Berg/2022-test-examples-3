package ru.yandex.market.logistics.nesu.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.altay.unifier.HttpUnifierClient;
import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.trust.client.TrustClient;
import ru.yandex.market.deliverycalculator.indexerclient.DeliveryCalculatorIndexerClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.logistics.admin.serializer.ExcelParser;
import ru.yandex.market.logistics.apikeys.ApiKeysClient;
import ru.yandex.market.logistics.datacamp.client.DataCampClient;
import ru.yandex.market.logistics.delivery.calculator.client.DeliveryCalculatorSearchEngineClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.api.configuration.OpenApiInterceptorConfiguration;
import ru.yandex.market.logistics.nesu.configuration.geobase.GeoBaseConfig;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.configuration.properties.UpdatePartnersHolidaysProperties;
import ru.yandex.market.logistics.nesu.configuration.queue.DbQueueConfiguration;
import ru.yandex.market.logistics.nesu.jobs.producer.CreateShopPickupPointProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.CreateShopPickupPointTariffProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.CreateTrustProductProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.DropoffRegistrationProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.LabelsFileGenerationProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.ModifierUploadTaskProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.PushDbsShopLicenseToLmsProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.PushFfLinkToMbiProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.PushMarketIdToLmsProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.PushPartnerMappingToL4SProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.RegisterOrderCapacityProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.RemoveDropoffShopBannerProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationToShopProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SendReturnOrdersWaitingNotificationProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SetPartnerHolidaysBatchProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SetPartnerHolidaysProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SetupNewShopProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SetupStockSyncStrategyProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SwitchShipmentLogisticPointProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdateBusinessWarehousePartnerApiMethodsProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdatePartnerExternalParamValueProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdateShopPickupPointProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdateShopPickupPointTariffProducer;
import ru.yandex.market.logistics.nesu.repository.FileProcessingTaskRepository;
import ru.yandex.market.logistics.nesu.service.combinator.CombinatorGrpcClient;
import ru.yandex.market.logistics.nesu.service.fileprocessing.FileProcessingTaskService;
import ru.yandex.market.logistics.nesu.service.fileprocessing.FileProcessingTaskServiceImpl;
import ru.yandex.market.logistics.nesu.service.health.StubPingChecker;
import ru.yandex.market.logistics.nesu.service.marketid.MarketIdService;
import ru.yandex.market.logistics.nesu.service.mds.DiscrepancyActService;
import ru.yandex.market.logistics.nesu.service.mds.DiscrepancyActServiceImpl;
import ru.yandex.market.logistics.nesu.service.sender.SenderService;
import ru.yandex.market.logistics.nesu.specification.FileProcessingTaskSpecificationFactory;
import ru.yandex.market.logistics.nesu.tvm.TvmAutoConfiguration;
import ru.yandex.market.logistics.nesu.utils.UuidGenerator;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics4shops.client.api.OutboundApi;
import ru.yandex.market.logistics4shops.client.api.PartnerMappingApi;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.partner.banners.client.api.TemplateBannersApi;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.tms.quartz2.service.TmsMonitoringService;
import ru.yandex.market.tpl.internal.client.TplInternalClient;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
@EnableZonkyEmbeddedPostgres
@Import({
    DbUnitTestConfiguration.class,
    DbQueueConfiguration.class,
    LiquibaseConfiguration.class,
    TvmAutoConfiguration.class,
    RepositoryConfiguration.class,
    WebMvcConfiguration.class,
    OpenApiInterceptorConfiguration.class,
    GeoSearchConfiguration.class,
    DeliveryCalculatorSearchEngineConfiguration.class,
    DeliveryCalculatorIndexerConfiguration.class,
    TrustConfiguration.class,
    LmsConfiguration.class,
    StockStorageConfiguration.class,
    GeoBaseConfig.class,
    ClockConfiguration.class,
    TvmTicketEnrichConfiguration.class,
    JobsConfiguration.class,
})
@MockBean(classes = {
    StubPingChecker.class,
    MdsS3Client.class,
    ResourceLocationFactory.class,
    TvmClient.class,
    BlackboxService.class,
    ApiKeysClient.class,
    LMSClient.class,
    AboAPI.class,
    LomClient.class,
    DataCampClient.class,
    UuidGenerator.class,
    MbiApiClient.class,
    GeoClient.class,
    DeliveryCalculatorSearchEngineClient.class,
    DeliveryCalculatorIndexerClient.class,
    TrustClient.class,
    StockStorageOrderClient.class,
    MdbClient.class,
    TarifficatorClient.class,
    WwClient.class,
    HttpUnifierClient.class,
    TplInternalClient.class,
    TransportManagerClient.class,
    PvzLogisticsClient.class,
    CheckouterAPI.class,
    MarketIdService.class,
    PartnerMappingApi.class,
    OutboundApi.class,
    CombinatorGrpcClient.class,
    TemplateBannersApi.class,
    TmsMonitoringService.class,
})
@SpyBean(classes = {
    SenderService.class,
    ModifierUploadTaskProducer.class,
    CreateTrustProductProducer.class,
    LabelsFileGenerationProducer.class,
    RegisterOrderCapacityProducer.class,
    SendNotificationProducer.class,
    SendNotificationToShopProducer.class,
    SetPartnerHolidaysProducer.class,
    SetupNewShopProducer.class,
    FeatureProperties.class,
    UpdatePartnersHolidaysProperties.class,
    SendReturnOrdersWaitingNotificationProducer.class,
    DropoffRegistrationProducer.class,
    SwitchShipmentLogisticPointProducer.class,
    PushMarketIdToLmsProducer.class,
    PushFfLinkToMbiProducer.class,
    SetupStockSyncStrategyProducer.class,
    SetPartnerHolidaysBatchProducer.class,
    CreateShopPickupPointProducer.class,
    UpdateShopPickupPointProducer.class,
    CreateShopPickupPointTariffProducer.class,
    UpdateShopPickupPointTariffProducer.class,
    UpdateBusinessWarehousePartnerApiMethodsProducer.class,
    PushPartnerMappingToL4SProducer.class,
    JdbcTemplate.class,
    PushDbsShopLicenseToLmsProducer.class,
    RemoveDropoffShopBannerProducer.class,
    UpdatePartnerExternalParamValueProducer.class,
})
@ComponentScan({
    "ru.yandex.market.logistics.nesu.admin",
    "ru.yandex.market.logistics.nesu.api",
    "ru.yandex.market.logistics.nesu.controller",
    "ru.yandex.market.logistics.nesu.converter",
    "ru.yandex.market.logistics.nesu.facade",
    "ru.yandex.market.logistics.nesu.jobs",
    "ru.yandex.market.logistics.nesu.repository",
    "ru.yandex.market.logistics.nesu.service",
    "ru.yandex.market.logistics.nesu.specification",
    "ru.yandex.market.logistics.nesu.waybill",
    "ru.yandex.market.logistics.nesu.configuration.properties",
    "ru.yandex.market.logistics.nesu.validation",
    "ru.yandex.market.logistics.nesu.enricher",
})
public class IntegrationTestConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public DiscrepancyActService discrepancyActTestingService(MdsS3Client mdsS3Client) {
        return new DiscrepancyActServiceImpl(mdsS3Client);
    }

    @Bean
    public FileProcessingTaskService fileProcessingTaskTestingService(
        FileProcessingTaskRepository fileProcessingTaskRepository,
        FileProcessingTaskSpecificationFactory fileProcessingTaskSpecificationFactory,
        MdsS3Client mdsS3Client
    ) {
        return new FileProcessingTaskServiceImpl(
            fileProcessingTaskRepository,
            fileProcessingTaskSpecificationFactory,
            mdsS3Client,
            new ExcelParser(objectMapper)
        );
    }
}
