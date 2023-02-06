package ru.yandex.market.mbo.mdm.noscan;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.market.mbo.mdm.common.config.CommonSskuFunctionsConfig;
import ru.yandex.market.mbo.mdm.common.config.LogbrokerMdmConfig;
import ru.yandex.market.mbo.mdm.common.config.LogbrokerParameters;
import ru.yandex.market.mbo.mdm.common.config.MdmDatacampLogbrokerConfig;
import ru.yandex.market.mbo.mdm.common.config.MdmMultiStorageConfig;
import ru.yandex.market.mbo.mdm.common.config.MdmParamConfig;
import ru.yandex.market.mbo.mdm.common.config.MdmQueryContextConfig;
import ru.yandex.market.mbo.mdm.common.config.MdmQueuesConfig;
import ru.yandex.market.mbo.mdm.common.config.MdmRepositoryConfig;
import ru.yandex.market.mbo.mdm.common.config.MdmServicesConfig;
import ru.yandex.market.mbo.mdm.common.datacamp.DatacampOffersFiltrator;
import ru.yandex.market.mbo.mdm.common.datacamp.DatacampOffersImporterImpl;
import ru.yandex.market.mbo.mdm.common.datacamp.SupplierSilverSskuTransformationServiceImpl;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerServiceImpl;
import ru.yandex.market.mbo.mdm.common.infrastructure.logbroker.DataCampToMdmLogbrokerMessageHandler;
import ru.yandex.market.mbo.mdm.common.infrastructure.logbroker.MdmLogbrokerDatacampMessageListener;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.config.logbroker.LogbrokerBaseConfig;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@Import({
    LogbrokerMdmConfig.class,
    LogbrokerBaseConfig.class,
    MdmDatacampLogbrokerConfig.class,
    MdmQueuesConfig.class,
    MdmParamConfig.class,
    CommonSskuFunctionsConfig.class,
    MdmServicesConfig.class,
    MdmQueryContextConfig.class,
    MdmMultiStorageConfig.class
})
@Configuration
public class DataCampToMdmLogbrokerTestConfig {
    @Value("${market.mdm.datacamp-to-mdm-mappings-int-test-topic}")
    private String mappingIntTestDatacampTopic;
    @Value("${market.mdm.datacamp-to-mdm-mappings-int-test-consumer}")
    private String mappingIntTestDatacampConsumer;
    @Value("${market.mdm.datacamp-to-mdm-int-test-topic}")
    private String intTestDatacampTopic;
    @Value("${market.mdm.datacamp-to-mdm-int-test-consumer}")
    private String intTestDatacampConsumer;

    private final LogbrokerMdmConfig logbrokerMdmConfig;
    private final LogbrokerBaseConfig logbrokerBaseConfig;
    private final MdmDatacampLogbrokerConfig mdmDatacampLogbrokerConfig;
    private final MdmQueuesConfig mdmQueuesConfig;
    private final MdmParamConfig mdmParamConfig;
    private final MdmServicesConfig mdmServicesConfig;
    private final CommonSskuFunctionsConfig commonSskuFunctionsConfig;
    private final MdmRepositoryConfig mdmRepositoryConfig;
    private final MdmQueryContextConfig mdmQueryContextConfig;
    private final StorageKeyValueService keyValueService;
    private final MdmMultiStorageConfig mdmMultiStorageConfig;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DataCampToMdmLogbrokerTestConfig(LogbrokerMdmConfig logbrokerMdmConfig,
                                            LogbrokerBaseConfig logbrokerBaseConfig,
                                            MdmDatacampLogbrokerConfig mdmDatacampLogbrokerConfig,
                                            MdmQueuesConfig mdmQueuesConfig,
                                            MdmParamConfig mdmParamConfig,
                                            MdmServicesConfig mdmServicesConfig,
                                            CommonSskuFunctionsConfig commonSskuFunctionsConfig,
                                            MdmRepositoryConfig mdmRepositoryConfig,
                                            MdmQueryContextConfig mdmQueryContextConfig,
                                            MdmMultiStorageConfig mdmMultiStorageConfig) {
        this.logbrokerMdmConfig = logbrokerMdmConfig;
        this.logbrokerBaseConfig = logbrokerBaseConfig;
        this.mdmDatacampLogbrokerConfig = mdmDatacampLogbrokerConfig;
        this.mdmQueuesConfig = mdmQueuesConfig;
        this.mdmParamConfig = mdmParamConfig;
        this.mdmServicesConfig = mdmServicesConfig;
        this.commonSskuFunctionsConfig = commonSskuFunctionsConfig;
        this.mdmRepositoryConfig = mdmRepositoryConfig;
        this.mdmQueryContextConfig = mdmQueryContextConfig;
        this.keyValueService = new StorageKeyValueServiceMock();
        this.mdmMultiStorageConfig = mdmMultiStorageConfig;
    }

    @Bean(name = "testingDataCampToMdmMappingsProducer", destroyMethod = "close")
    public MdmLogbrokerService testingDataCampToMdmMappingsProducer() {
        return new MdmLogbrokerServiceImpl(
            mappingIntTestDatacampTopic,
            logbrokerBaseConfig.getModuleName(),
            logbrokerBaseConfig.getEnvironmentType(),
            logbrokerMdmConfig.logbrokerWriteClusterWithMdmCredentials(),
            logbrokerBaseConfig.poolConfig(),
            logbrokerBaseConfig.retryTemplate()
        );
    }

    @Bean(destroyMethod = "stop")
    public MdmLogbrokerDatacampMessageListener testingMdmLogbrokerDatacampToMdmMappingsListener() {
        return new MdmLogbrokerDatacampMessageListener(
            testingMdmLogbrokerDatacampStreamMappingsConsumer(),
            testingDataCampToMdmLogbrokerMappingsHandler(),
            mdmQueryContextConfig.queryContextComment(),
            keyValueService);
    }

    @Bean
    public DataCampToMdmLogbrokerMessageHandler testingDataCampToMdmLogbrokerMappingsHandler() {
        keyValueService.putValue(MdmProperties.IMPORT_MAPPINGS_FROM_DATACAMP_ENABLED, true);
        DatacampOffersImporterImpl datacampOffersImporter = new DatacampOffersImporterImpl(
            keyValueService,
            mdmQueuesConfig.mdmQueuesManager(),
            commonSskuFunctionsConfig.mdmSskuGroupManager(),
            mdmServicesConfig.mappingsUpdateService(),
            mdmServicesConfig.serviceOfferMigrationService(),
            new DatacampOffersFiltrator(
                mdmRepositoryConfig.beruId(),
                commonSskuFunctionsConfig.mdmSskuGroupManager(),
                keyValueService,
                mdmRepositoryConfig.mdmSupplierRepository(),
                mdmRepositoryConfig.sskuExistenceRepository()
            ),
            new SupplierSilverSskuTransformationServiceImpl(
                mdmMultiStorageConfig.silverSskuRepository(),
                mdmRepositoryConfig.sskuExistenceRepository(),
                keyValueService));
        return new DataCampToMdmLogbrokerMessageHandler(
            keyValueService,
            mdmDatacampLogbrokerConfig.commonSskuFromDatacampConverter(),
            datacampOffersImporter,
            mdmQueuesConfig.mdmQueuesManager()
        );
    }

    @Lazy
    @Bean(destroyMethod = "stopConsume")
    public StreamConsumer testingMdmLogbrokerDatacampStreamMappingsConsumer() {
        var config = new LogbrokerParameters(mappingIntTestDatacampTopic, mappingIntTestDatacampConsumer)
            .createStreamConfig(logbrokerMdmConfig.mdmCredentialsSupplier()).build();
        return logbrokerMdmConfig.logbrokerReadInstallation().createStreamConsumer(config);
    }

    @Bean(name = "testingDataCampToMdmProducer", destroyMethod = "close")
    public MdmLogbrokerService testingDataCampToMdmProducer() {
        return new MdmLogbrokerServiceImpl(
            intTestDatacampTopic,
            logbrokerBaseConfig.getModuleName(),
            logbrokerBaseConfig.getEnvironmentType(),
            logbrokerMdmConfig.logbrokerWriteClusterWithMdmCredentials(),
            logbrokerBaseConfig.poolConfig(),
            logbrokerBaseConfig.retryTemplate()
        );
    }

    @Bean(destroyMethod = "stop")
    public MdmLogbrokerDatacampMessageListener testingMdmLogbrokerDatacampToMdmListener() {
        return new MdmLogbrokerDatacampMessageListener(
            testingMdmLogbrokerDatacampStreamConsumer(),
            testingDataCampToMdmLogbrokerMessageHandler(),
            mdmQueryContextConfig.queryContextComment(),
            keyValueService);
    }

    @Bean
    public DataCampToMdmLogbrokerMessageHandler testingDataCampToMdmLogbrokerMessageHandler() {
        return new DataCampToMdmLogbrokerMessageHandler(
            keyValueService,
            mdmDatacampLogbrokerConfig.commonSskuFromDatacampConverter(),
            mdmDatacampLogbrokerConfig.datacampOffersImporter(),
            mdmQueuesConfig.mdmQueuesManager()
        );
    }

    @Lazy
    @Bean(destroyMethod = "stopConsume")
    public StreamConsumer testingMdmLogbrokerDatacampStreamConsumer() {
        var config = new LogbrokerParameters(intTestDatacampTopic, intTestDatacampConsumer)
            .createStreamConfig(logbrokerMdmConfig.mdmCredentialsSupplier()).build();
        return logbrokerMdmConfig.logbrokerReadInstallation().createStreamConsumer(config);
    }
}
