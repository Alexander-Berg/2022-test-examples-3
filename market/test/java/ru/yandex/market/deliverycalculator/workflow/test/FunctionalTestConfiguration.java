package ru.yandex.market.deliverycalculator.workflow.test;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.deliverycalculator.storage.configs.DeliveryCalculatorStorageTestConfig;
import ru.yandex.market.deliverycalculator.storage.repository.MdsFileHistoryRepository;
import ru.yandex.market.deliverycalculator.storage.repository.ProtoBucketEntityRepository;
import ru.yandex.market.deliverycalculator.storage.repository.ProtoOptionGroupEntityRepository;
import ru.yandex.market.deliverycalculator.storage.repository.ProtoServiceGroupRepository;
import ru.yandex.market.deliverycalculator.storage.repository.RegionRepository;
import ru.yandex.market.deliverycalculator.storage.repository.WarehouseRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasCourierRegionalDataRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasPickupRegionalDataRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasPostRegionalDataRepository;
import ru.yandex.market.deliverycalculator.storage.repository.daas.DaasServiceGroupRepository;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorMetaStorageService;
import ru.yandex.market.deliverycalculator.storage.service.DeliveryCalculatorStorageService;
import ru.yandex.market.deliverycalculator.storage.service.EnvironmentService;
import ru.yandex.market.deliverycalculator.storage.service.GenerationService;
import ru.yandex.market.deliverycalculator.storage.service.LogisticsStorageService;
import ru.yandex.market.deliverycalculator.storage.service.ProtoStorageService;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryStorageService;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;
import ru.yandex.market.deliverycalculator.storage.service.impl.HttpTariffInfoProvider;
import ru.yandex.market.deliverycalculator.storage.service.impl.ProtoStorageServiceImpl;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.storage.service.impl.YaDeliveryTariffLoadService;
import ru.yandex.market.deliverycalculator.workflow.RegionCache;
import ru.yandex.market.deliverycalculator.workflow.TariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.WarehouseCache;
import ru.yandex.market.deliverycalculator.workflow.daas.DaasCourierTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.daas.DaasOutletExtractorFactory;
import ru.yandex.market.deliverycalculator.workflow.daas.DaasPickupTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.daas.DaasPostTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.mardocourier.MardoCourierTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.mardopickup.MardoPickupTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.mardopost.MardoPostTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.mardowhitecourier.MardoWhiteCourierTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.mardowhitepickup.MardoWhitePickupTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.protobucketbuilder.ProtoBucketBuilder;
import ru.yandex.market.deliverycalculator.workflow.protobucketbuilder.ProtoBucketBuilderImpl;
import ru.yandex.market.deliverycalculator.workflow.regularcourier.RegularCourierTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.regularpickup.RegularPickupTariffWorkflow;
import ru.yandex.market.deliverycalculator.workflow.service.ActualDeliveryInfoCacheService;
import ru.yandex.market.deliverycalculator.workflow.service.CachedRegionService;
import ru.yandex.market.deliverycalculator.workflow.service.DeliveryServicesService;
import ru.yandex.market.deliverycalculator.workflow.service.RegionService;
import ru.yandex.market.deliverycalculator.workflow.service.SenderSettingsCacheService;
import ru.yandex.market.deliverycalculator.workflow.service.ShopSettingsCacheService;
import ru.yandex.market.deliverycalculator.workflow.service.modifier.DeliveryModifierApplianceService;
import ru.yandex.market.deliverycalculator.workflow.service.modifier.DeliveryModifierAvailabilityService;
import ru.yandex.market.deliverycalculator.workflow.tariffprocessor.CourierTariffProcessor;
import ru.yandex.market.deliverycalculator.workflow.tariffprocessor.OutletTariffProcessor;
import ru.yandex.market.deliverycalculator.workflow.tariffprocessor.PickupTariffProcessor;
import ru.yandex.market.deliverycalculator.workflow.tariffprocessor.PostTariffProcessor;
import ru.yandex.market.deliverycalculator.workflow.util.converter.ProtoDeliveryOptionConverter;
import ru.yandex.market.deliverycalculator.workflow.util.converter.daas.DaasDeliveryOptionGroupConverter;
import ru.yandex.market.deliverycalculator.workflow.util.converter.daas.DaasDeliveryServiceGroupConverter;
import ru.yandex.market.deliverycalculator.workflow.util.converter.daas.DaasPickupPointsConverter;
import ru.yandex.market.deliverycalculator.workflow.util.segmentation2.SegmentationService;

import static org.mockito.Mockito.mock;

@Import(DeliveryCalculatorStorageTestConfig.class)
@Configuration
public class FunctionalTestConfiguration {
    @Bean
    public MdsS3Client mdsS3Client() {
        return mock(MdsS3Client.class);
    }

    @Bean
    public TariffInfoProvider tariffInfoProvider() {
        return mock(HttpTariffInfoProvider.class);
    }

    @Autowired
    private GenerationService generationService;

    @Autowired
    private ProtoServiceGroupRepository protoServiceGroupRepository;

    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    @Autowired
    private MdsFileHistoryRepository mdsFileHistoryRepository;

    @Autowired
    private YaDeliveryStorageService yaDeliveryStorageService;

    @Autowired
    private LogisticsStorageService logisticsStorageService;

    @Autowired
    private DeliveryCalculatorMetaStorageService deliveryCalculatorMetaStorageService;

    @Autowired
    private DaasServiceGroupRepository serviceGroupRepository;

    @Autowired
    private DaasPickupRegionalDataRepository daasPickupRegionalDataRepository;

    @Autowired
    private DaasPostRegionalDataRepository daasPostRegionalDataRepository;

    @Autowired
    private ProtoBucketEntityRepository protoBucketEntityRepository;

    @Autowired
    private ProtoOptionGroupEntityRepository protoOptionGroupEntityRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Bean
    public YaDeliveryTariffLoadService yaDeliveryTariffLoadService() {
        return new YaDeliveryTariffLoadService(mdsS3Client(), tariffInfoProvider(),
                yaDeliveryTariffDbService, mdsFileHistoryRepository);
    }

    @Bean
    public SenderSettingsCacheService senderSettingsCacheService() {
        return new SenderSettingsCacheService();
    }

    @Bean
    public ShopSettingsCacheService shopSettingsCacheService() {
        return new ShopSettingsCacheService();
    }

    @Bean
    public ProtoStorageService protoStorageService() {
        return new ProtoStorageServiceImpl(protoBucketEntityRepository, protoOptionGroupEntityRepository);
    }

    @Bean
    public ProtoBucketBuilder protoBucketBuilder() {
        return new ProtoBucketBuilderImpl(protoStorageService(), protoServiceGroupRepository);
    }

    @Bean
    public CourierTariffProcessor courierTariffProcessor() {
        return new CourierTariffProcessor(new SegmentationService());
    }

    @Bean
    public PickupTariffProcessor pickupTariffProcessor() {
        return new PickupTariffProcessor();
    }

    @Bean
    public RegionCache regionCache(
            RegionRepository repository
    ) {
        return new RegionCache(repository, Mockito.mock(ScheduledExecutorService.class), 9999L);
    }

    @Bean
    public RegionService regionService(
            RegionCache regionCache
    ) {
        return new CachedRegionService(regionCache);
    }

    @Bean
    public DeliveryModifierAvailabilityService deliveryModifierAvailabilityService(
            RegionService regionService
    ) {
        return new DeliveryModifierAvailabilityService(regionService);
    }

    @Bean
    public DeliveryModifierApplianceService modifierApplianceService(
            SenderSettingsCacheService senderSettingsCacheService,
            DeliveryModifierAvailabilityService availabilityService
    ) {
        return new DeliveryModifierApplianceService(senderSettingsCacheService, availabilityService);
    }

    @Bean(name = "mardoWhiteCourierIndexerWorkflow")
    public MardoWhiteCourierTariffWorkflow mardoWhiteCourierIndexerWorkflow() {
        return MardoWhiteCourierTariffWorkflow.createForIndexer(
                yaDeliveryStorageService,
                yaDeliveryTariffDbService,
                deliveryCalculatorMetaStorageService,
                generationService,
                courierTariffProcessor(),
                protoBucketBuilder(),
                yaDeliveryTariffLoadService()
        );
    }

    @Bean(name = "mardoWhiteCourierSearchEngineWorkflow")
    public MardoWhiteCourierTariffWorkflow mardoWhiteCourierSearchEngineWorkflow(
            ShopSettingsCacheService shopSettingsCacheService,
            DeliveryModifierApplianceService modifierApplianceService
    ) {
        return MardoWhiteCourierTariffWorkflow.createForSearchEngine(
                shopSettingsCacheService,
                modifierApplianceService
        );
    }

    @Bean
    public SegmentationService segmentationService() {
        return new SegmentationService();
    }

    @Bean
    public DeliveryServicesService deliveryServicesService() {
        return new DeliveryServicesService(
                serviceGroupRepository,
                new DaasDeliveryServiceGroupConverter());
    }

    @Bean
    OutletTariffProcessor outletTariffProcessor() {
        return new OutletTariffProcessor(
                segmentationService(),
                deliveryServicesService(),
                logisticsStorageService,
                generationService,
                protoStorageService(),
                protoServiceGroupRepository,
                environmentService
        );
    }

    @Bean
    public PostTariffProcessor postTariffProcessor() {
        return new PostTariffProcessor();
    }

    @Bean
    public WarehouseCache warehouseCache(WarehouseRepository warehouseRepository) {
        return new WarehouseCache(warehouseRepository, mock(ScheduledExecutorService.class), 10000);
    }

    @Bean
    public DaasPickupPointsConverter daasPickupPointsConverter() {
        return new DaasPickupPointsConverter();
    }

    @Bean
    public DaasDeliveryOptionGroupConverter daasDeliveryOptionGroupConverter() {
        return new DaasDeliveryOptionGroupConverter(protoDeliveryOptionConverter());
    }

    @Bean
    public DaasOutletExtractorFactory daasOutletExtractorFactory() {
        return new DaasOutletExtractorFactory(logisticsStorageService, daasPickupPointsConverter());
    }

    @Bean
    public ProtoDeliveryOptionConverter protoDeliveryOptionConverter() {
        return new ProtoDeliveryOptionConverter();
    }

    @Bean
    public ActualDeliveryInfoCacheService actualDeliveryInfoCacheService(
            List<TariffWorkflow<?, ?>> tariffWorkflows,
            SenderSettingsCacheService senderSettingsCacheService,
            ShopSettingsCacheService shopSettingsCacheService
    ) {
        return new ActualDeliveryInfoCacheService(
                tariffWorkflows,
                senderSettingsCacheService,
                shopSettingsCacheService
        );
    }

    @Bean(name = "mardoWhitePickupIndexerWorkflow")
    public MardoWhitePickupTariffWorkflow mardoWhitePickupIndexerWorkflow() {
        return MardoWhitePickupTariffWorkflow.createForIndexer(
                yaDeliveryStorageService,
                yaDeliveryTariffDbService,
                deliveryCalculatorMetaStorageService,
                generationService,
                yaDeliveryTariffLoadService(),
                outletTariffProcessor()
        );
    }

    @Bean(name = "mardoWhitePickupSearchEngineWorkflow")
    public MardoWhitePickupTariffWorkflow searchEngineWorkflow(
            ShopSettingsCacheService shopSettingsCacheService,
            DeliveryModifierApplianceService modifierApplianceService
    ) {
        return MardoWhitePickupTariffWorkflow.createForSearchEngine(
                shopSettingsCacheService,
                modifierApplianceService
        );
    }

    @Bean(name = "regularCourierTariffIndexerWorkflow")
    public RegularCourierTariffWorkflow regularCourierTariffIndexerWorkflow(
            DeliveryCalculatorStorageService deliveryCalculatorStorageService
    ) {
        return RegularCourierTariffWorkflow.createForIndexer(
                deliveryCalculatorStorageService,
                generationService,
                protoBucketBuilder()
        );
    }

    @Bean(name = "regularCourierTariffSearchEngineWorkflow")
    public RegularCourierTariffWorkflow regularCourierTariffSearchEngineWorkflow() {
        return RegularCourierTariffWorkflow.createForSearchEngine();
    }

    @Bean(name = "regularPickupTariffIndexerWorkflow")
    public RegularPickupTariffWorkflow regularPickupTariffIndexerWorkflow(
            DeliveryCalculatorStorageService deliveryCalculatorStorageService
    ) {
        return RegularPickupTariffWorkflow.createForIndexer(
                deliveryCalculatorStorageService,
                deliveryCalculatorMetaStorageService,
                generationService,
                protoStorageService()
        );
    }

    @Bean(name = "regularPickupTariffSearchEngineWorkflow")
    public RegularPickupTariffWorkflow regularPickupTariffSearchEngineWorkflow() {
        return RegularPickupTariffWorkflow.createForSearchEngine();
    }

    @Bean(name = "mardoCourierTariffIndexerWorkflow")
    MardoCourierTariffWorkflow mardoCourierTariffIndexerWorkflow() {
        return MardoCourierTariffWorkflow.createForIndexer(
                yaDeliveryStorageService,
                yaDeliveryTariffDbService,
                generationService,
                courierTariffProcessor(),
                protoBucketBuilder(),
                yaDeliveryTariffLoadService()
        );
    }

    @Bean(name = "mardoCourierTariffSearchEngineWorkflow")
    MardoCourierTariffWorkflow mardoCourierTariffSearchEngineWorkflow(WarehouseCache warehouseCache) {
        return MardoCourierTariffWorkflow.createForSearchEngine(warehouseCache);
    }

    @Bean(name = "mardoPickupTariffIndexerWorkflow")
    public MardoPickupTariffWorkflow mardoPickupTariffIndexerWorkflow() {
        return MardoPickupTariffWorkflow.createForIndexer(
                yaDeliveryStorageService,
                yaDeliveryTariffDbService,
                deliveryCalculatorMetaStorageService,
                generationService,
                protoStorageService(),
                pickupTariffProcessor(),
                yaDeliveryTariffLoadService()
        );
    }

    @Bean(name = "mardoPickupTariffSearchEngineWorkflow")
    public MardoPickupTariffWorkflow mardoPickupTariffSearchEngineWorkflow(WarehouseCache warehouseCache) {
        return MardoPickupTariffWorkflow.createForSearchEngine(warehouseCache);
    }

    @Bean(name = "mardoPostTariffIndexerWorkflow")
    public MardoPostTariffWorkflow mardoPostTariffIndexerWorkflow() {
        return MardoPostTariffWorkflow.createForIndexer(
                yaDeliveryStorageService,
                yaDeliveryTariffDbService,
                deliveryCalculatorMetaStorageService,
                generationService,
                protoBucketBuilder(),
                yaDeliveryTariffLoadService(),
                postTariffProcessor()
        );
    }

    @Bean(name = "mardoPostTariffSearchEngineWorkflow")
    public MardoPostTariffWorkflow mardoPostTariffSearchEngineWorkflow(WarehouseCache warehouseCache) {
        return MardoPostTariffWorkflow.createForSearchEngine(warehouseCache);
    }

    @Bean(name = "daasCourierTariffSearchEngineWorkflow")
    public DaasCourierTariffWorkflow daasCourierTariffSearchEngineWorkflow(
            SenderSettingsCacheService senderSettingsCacheService,
            DaasCourierRegionalDataRepository regionalDataRepository,
            DeliveryModifierApplianceService modifierApplianceService,
            RegionService regionService
    ) {
        return DaasCourierTariffWorkflow.createForSearchEngine(
                senderSettingsCacheService,
                regionalDataRepository,
                modifierApplianceService,
                regionService
        );
    }

    @Bean(name = "daasPickupTariffIndexerWorkflow")
    public DaasPickupTariffWorkflow daasPickupTariffIndexerWorkflow() {
        return DaasPickupTariffWorkflow.createForIndexer(
                yaDeliveryStorageService,
                yaDeliveryTariffDbService,
                deliveryCalculatorMetaStorageService,
                generationService,
                outletTariffProcessor(),
                daasDeliveryOptionGroupConverter(),
                yaDeliveryTariffLoadService(),
                daasPickupPointsConverter(),
                deliveryServicesService()
        );
    }

    @Bean(name = "daasPickupTariffSearchEngineWorkflow")
    public DaasPickupTariffWorkflow daasPickupTariffSearchEngineWorkflow(
            DaasPickupPointsConverter daasPickupPointsConverter,
            SenderSettingsCacheService senderSettingsCacheService,
            DaasOutletExtractorFactory daasOutletExtractorFactory,
            DeliveryModifierApplianceService modifierApplianceService,
            RegionService regionService
    ) {
        return DaasPickupTariffWorkflow.createForSearchEngine(
                daasPickupPointsConverter,
                senderSettingsCacheService,
                daasPickupRegionalDataRepository,
                daasOutletExtractorFactory,
                modifierApplianceService,
                regionService
        );
    }

    @Bean(name = "daasPostTariffIndexerWorkflow")
    public DaasPostTariffWorkflow daasPostTariffIndexerWorkflow() {
        return DaasPostTariffWorkflow.createForIndexer(
                yaDeliveryStorageService,
                yaDeliveryTariffDbService,
                deliveryCalculatorMetaStorageService,
                generationService,
                outletTariffProcessor(),
                daasDeliveryOptionGroupConverter(),
                yaDeliveryTariffLoadService(),
                deliveryServicesService(),
                daasPickupPointsConverter()
        );
    }

    @Bean(name = "daasPostTariffSearchEngineWorkflow")
    public DaasPostTariffWorkflow daasPostTariffSearchEngineWorkflow(
            SenderSettingsCacheService senderSettingsCacheService,
            DaasPickupPointsConverter pickupPointsConverter,
            DaasOutletExtractorFactory daasOutletExtractorFactory,
            DeliveryModifierApplianceService modifierApplianceService,
            RegionService regionService
    ) {
        return DaasPostTariffWorkflow.createForSearchEngine(
                senderSettingsCacheService,
                daasPostRegionalDataRepository,
                pickupPointsConverter,
                daasOutletExtractorFactory,
                modifierApplianceService,
                regionService
        );
    }

}
