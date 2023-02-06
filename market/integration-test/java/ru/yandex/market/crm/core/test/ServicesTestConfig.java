package ru.yandex.market.crm.core.test;

import java.util.Collections;
import java.util.Properties;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.core.services.CoreServicesConfig;
import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.core.test.utils.CoreTestUtilsConfig;
import ru.yandex.market.crm.external.blackbox.YandexBlackboxClient;
import ru.yandex.market.crm.templates.TemplateService;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;
import ru.yandex.market.mcrm.http.internal.TvmServiceMockImpl;
import ru.yandex.market.mcrm.http.tvm.TvmService;
import ru.yandex.market.mcrm.utils.PropertiesProvider;

@Configuration
@Import({
        LocalYtConfig.class,
        TestNetConfig.class,
        TestMasterReadOnlyDataSourceConfiguration.class,
        JacksonConfig.class,
        TestEnvironmentResolver.class,
        CoreTestUtilsConfig.class,
        CoreTestSuppliersConfig.class,
        CoreServicesConfig.class
})
public class ServicesTestConfig {

    @Bean
    public PropertiesProvider propertyProvider(ConfigurableBeanFactory beanFactory) {
        return new PropertiesProvider(beanFactory);
    }

    @Bean
    public YandexBlackboxClient yandexBlackboxClient() {
        return Mockito.mock(YandexBlackboxClient.class);
    }

    @Bean
    public TvmService tvmService() {
        return new TvmServiceMockImpl();
    }

    @Bean
    public TemplateService templateService(@Value("${templator.groovy.cache.size.max}") int maxCacheSize) {
        return new TemplateService(
                new Properties(),
                Collections.emptyMap(),
                maxCacheSize
        );
    }
}
