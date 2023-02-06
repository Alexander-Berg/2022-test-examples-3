package ru.yandex.market.mbo.mdm.noscan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.logbroker.config.LogbrokerListenerConfig;
import ru.yandex.market.logbroker.consumer.TransactionalLogbrokerListenerFactory;
import ru.yandex.market.mbo.mdm.common.config.LogbrokerMdmConfig;
import ru.yandex.market.mbo.mdm.common.config.LogbrokerParameters;
import ru.yandex.market.mbo.mdm.common.config.MdmQueuesConfig;
import ru.yandex.market.mbo.mdm.common.config.MdmRepositoryConfig;
import ru.yandex.market.mbo.mdm.common.config.MdmServicesConfig;
import ru.yandex.market.mbo.mdm.common.infrastructure.LogbrokerConsumerSpawner;
import ru.yandex.market.mbo.mdm.common.infrastructure.MbiPartnersLogbrokerDataProcessor;
import ru.yandex.market.mbo.mdm.common.infrastructure.MbiPartnersLogbrokerDataProcessorImpl;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerServiceImpl;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.config.logbroker.LogbrokerBaseConfig;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@Import({
    LogbrokerMdmConfig.class,
    LogbrokerBaseConfig.class,
    LogbrokerListenerConfig.class,
    MdmRepositoryConfig.class,
    MdmServicesConfig.class,
    MdmQueuesConfig.class
})
@Configuration
public class MdmMbiLogbrokerTestConfig {
    @Value("${market.mdm.mbi-to-mdm-partners-int-test-topic}")
    private String mbiPartnersTopic;
    @Value("${market.mdm.mbi-to-mdm-partners-int-test-consumer}")
    private String mbiPartnersConsumer;

    private final LogbrokerMdmConfig logbrokerMdmConfig;
    private final LogbrokerBaseConfig logbrokerBaseConfig;
    private final LogbrokerListenerConfig logbrokerListenerConfig;
    private final MdmRepositoryConfig mdmRepositoryConfig;
    private final MdmServicesConfig mdmServicesConfig;
    private final MdmQueuesConfig mdmQueuesConfig;

    public MdmMbiLogbrokerTestConfig(LogbrokerMdmConfig logbrokerMdmConfig,
                                     LogbrokerBaseConfig logbrokerBaseConfig,
                                     LogbrokerListenerConfig logbrokerListenerConfig,
                                     MdmRepositoryConfig mdmRepositoryConfig,
                                     MdmServicesConfig mdmServicesConfig,
                                     MdmQueuesConfig mdmQueuesConfig) {
        this.logbrokerMdmConfig = logbrokerMdmConfig;
        this.logbrokerBaseConfig = logbrokerBaseConfig;
        this.logbrokerListenerConfig = logbrokerListenerConfig;
        this.mdmRepositoryConfig = mdmRepositoryConfig;
        this.mdmServicesConfig = mdmServicesConfig;
        this.mdmQueuesConfig = mdmQueuesConfig;
    }

    @Bean(name = "testingMbiToMdmPartnersProducer", destroyMethod = "close")
    public MdmLogbrokerService testingMbiToMdmPartnersProducer() {
        return new MdmLogbrokerServiceImpl(
            mbiPartnersTopic,
            logbrokerBaseConfig.getModuleName(),
            logbrokerBaseConfig.getEnvironmentType(),
            logbrokerMdmConfig.logbrokerWriteClusterWithMdmCredentials(),
            logbrokerBaseConfig.poolConfig(),
            logbrokerBaseConfig.retryTemplate()
        );
    }

    @Bean(initMethod = "init", destroyMethod = "stop")
    public LogbrokerConsumerSpawner mbiToMdmConsumerSpawner() {
        var config = new LogbrokerParameters(mbiPartnersTopic, mbiPartnersConsumer)
            .createStreamConfig(logbrokerMdmConfig.mdmCredentialsSupplier()).build();
        return new LogbrokerConsumerSpawner(logbrokerMdmConfig.logbrokerReadInstallation(),
            config, testingMbiToMdmPartnersListener(), new ComplexMonitoring(), new StorageKeyValueServiceMock());
    }

    @Bean
    public StreamListener testingMbiToMdmPartnersListener() {
        TransactionalLogbrokerListenerFactory listenerFactory =
            logbrokerListenerConfig.transactionalLogbrokerListenerFactory(
                logbrokerMdmConfig.logbrokerMonitorExceptionsService()
            );
        return listenerFactory.createListener(testingMbiPartnersLogbrokerDataProcessor());
    }

    @Bean
    public MbiPartnersLogbrokerDataProcessor testingMbiPartnersLogbrokerDataProcessor() {
        StorageKeyValueService keyValueService = new StorageKeyValueServiceMock();
        keyValueService.putValue(MdmProperties.READ_PARTNERS_FROM_MBI_LB, true);
        return new MbiPartnersLogbrokerDataProcessorImpl(
            keyValueService,
            mdmRepositoryConfig.mdmSupplierRepository(),
            mdmServicesConfig.mdmBusinessStageSwitcher(),
            mdmQueuesConfig.mdmQueuesManager());
    }
}
