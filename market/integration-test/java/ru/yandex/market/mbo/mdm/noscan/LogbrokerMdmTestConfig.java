package ru.yandex.market.mbo.mdm.noscan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.common.logbroker.LogbrokerProducerService;
import ru.yandex.market.mbo.common.logbroker.LogbrokerProducerServiceImpl;
import ru.yandex.market.mbo.mdm.common.config.LogbrokerMdmConfig;
import ru.yandex.market.mboc.common.config.LazyWrapper;
import ru.yandex.market.mboc.common.config.logbroker.LogbrokerBaseConfig;

@Lazy
@TestConfiguration
@Import({
    LogbrokerMdmConfig.class
})
public class LogbrokerMdmTestConfig {

    private final LogbrokerBaseConfig logbrokerBaseConfig;
    private final LogbrokerMdmConfig logbrokerMdmConfig;
    @Value("${market.mdm.iris-to-mdm-topic-int-test}")
    private String irisToMdmTopic;
    @Value("${market.mdm.iris-records-consumer-path-int-test}")
    private String irisRecordsConsumerPath;

    public LogbrokerMdmTestConfig(LogbrokerBaseConfig logbrokerBaseConfig,
                                  LogbrokerMdmConfig logbrokerMdmConfig) {
        this.logbrokerBaseConfig = logbrokerBaseConfig;
        this.logbrokerMdmConfig = logbrokerMdmConfig;
    }

    /**
     * Запись в топик ${market.mdm.iris-to-mdm-topic}.
     *
     * @see LogbrokerMdmTestConfig#irisToMdmTopic
     */
    @Lazy
    @LazyWrapper
    @Bean(destroyMethod = "tearDown")
    public LogbrokerProducerService<MdmIrisPayload.Item> irisToMdmProducer() {
        return new LogbrokerProducerServiceImpl<>(
            logbrokerBaseConfig.lbkxClientFactory(),
            irisToMdmProducerConfig()
        );
    }

    /**
     * Запись батчами в топик ${market.mdm.iris-to-mdm-topic}.
     *
     * @see LogbrokerMdmTestConfig#irisToMdmTopic
     */
    @Lazy
    @LazyWrapper
    @Bean(destroyMethod = "tearDown")
    public LogbrokerProducerService<MdmIrisPayload.ItemBatch> irisToMdmBatchProducer() {
        return new LogbrokerProducerServiceImpl<>(
            logbrokerBaseConfig.lbkxClientFactory(),
            irisToMdmProducerConfig()
        );
    }

    @LazyWrapper
    @Bean
    public StreamConsumerConfig irisToMdmConsumerConfig() {
        return logbrokerBaseConfig.createConsumerConfig(
            logbrokerMdmConfig.mdmCredentialsSupplier(),
            irisToMdmTopic,
            irisRecordsConsumerPath,
            logbrokerBaseConfig.lbkxExecutorService()
        );
    }

    @LazyWrapper
    @Bean
    public AsyncProducerConfig irisToMdmProducerConfig() {
        return logbrokerBaseConfig.createProducerConfig(
            logbrokerMdmConfig.mdmCredentialsSupplier(),
            irisToMdmTopic
        );
    }
}
