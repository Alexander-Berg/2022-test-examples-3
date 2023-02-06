package ru.yandex.market.logistics.iris.configuration;

import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.lang.NonNull;

import ru.yandex.market.common.mds.s3.spring.configuration.MdsS3BasicConfiguration;
import ru.yandex.market.logistics.iris.configuration.protobuf.ProtobufMappersConfiguration;
import ru.yandex.market.logistics.iris.configuration.queue.DbQueueConfiguration;
import ru.yandex.market.logistics.iris.configuration.solomon.PusherToSolomonClientConfiguration;
import ru.yandex.market.logistics.iris.repository.ItemChangeRepository;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;

@Import(value = {
        ApiConfiguration.class,
        TestDataSourceConfiguration.class,
        LiquibaseConfiguration.class,
        DbUnitTestConfiguration.class,
        SecurityConfiguration.class,
        PingCheckersConfiguration.class,
        ReferenceIndexerTestConfiguration.class,
        DbQueueConfiguration.class,
        DbQueueTestConfiguration.class,
        TrustworthyValuesPickerConfiguration.class,
        ProtobufMappersConfiguration.class,
        TvmMockConfiguration.class,
        LmsClientConfiguration.class,
        MdsS3BasicConfiguration.class,
        MboClientMockConfiguration.class,
        DateTimeConfiguration.class,
        PusherToSolomonClientConfiguration.class,
        EventHandlesConfiguration.class,
        AsyncConfiguration.class,
        LogbrokerCheckerConfiguration.class,
        LogbrokerPropertiesConfiguration.class
})
@Configuration
@EntityScan("ru.yandex.market.logistics.iris.entity")
@EnableJpaRepositories("ru.yandex.market.logistics.iris.repository")
@ComponentScan(value = {
        "ru.yandex.market.logistics.iris.entity",
        "ru.yandex.market.logistics.iris.service",
        "ru.yandex.market.logistics.iris.controller",
        "ru.yandex.market.logistics.iris.repository",
        "ru.yandex.market.logistics.iris.jobs",
        "ru.yandex.market.logistics.iris.picker",
})
public class IntegrationTestConfiguration {

    // enable spy on jpa
    @Bean
    public BeanPostProcessor itemChangeRepositoryPostProcessor() {
        return new ProxiedMockPostProcessor(ItemChangeRepository.class);
    }

    static class ProxiedMockPostProcessor implements BeanPostProcessor {
        private final Class<?> mockedClass;

        public ProxiedMockPostProcessor(Class<?> mockedClass) {
            this.mockedClass = mockedClass;
        }

        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName)
                throws BeansException {
            if (mockedClass.isInstance(bean)) {
                return Mockito.mock(mockedClass, AdditionalAnswers.delegatesTo(bean));
            }
            return bean;
        }
    }

}
