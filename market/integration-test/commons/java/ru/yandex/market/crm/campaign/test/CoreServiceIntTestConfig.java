package ru.yandex.market.crm.campaign.test;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import ru.yandex.market.crm.campaign.placeholders.ConfigPropertiesProvider;
import ru.yandex.market.crm.campaign.services.ServicesConfig;
import ru.yandex.market.crm.campaign.test.loggers.TestLoggersConfig;
import ru.yandex.market.crm.campaign.test.tms.MockTmsConfig;
import ru.yandex.market.crm.campaign.test.utils.TestUtilsConfig;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.yt.YtTokenSupplier;
import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;
import ru.yandex.market.mcrm.http.internal.TvmServiceMockImpl;
import ru.yandex.market.mcrm.http.tvm.TvmService;
import ru.yandex.market.mcrm.utils.PropertiesProvider;

/**
 * @author apershukov
 */
@Configuration
@Import({
        TestNetConfig.class,
        ServicesConfig.class,
        TestSupplierConf.class,
        TestMasterReadOnlyDataSourceConfiguration.class,
        TestEnvironmentResolver.class,
        TestUtilsConfig.class,
        TestLoggersConfig.class,
        MockTmsConfig.class
})
@ComponentScan("ru.yandex.market.crm.campaign.yt.utils")
public class CoreServiceIntTestConfig {

    @Bean
    public ConfigPropertiesProvider configPropertiesProvider(TestPropertyPlaceholderConfigurer configurer) {
        return new ConfigPropertiesProvider(configurer.getProperties());
    }

    @Bean
    public PropertiesProvider propertyProvider(ConfigurableBeanFactory beanFactory) {
        return new PropertiesProvider(beanFactory);
    }

    @Bean
    public TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new TestPropertyPlaceholderConfigurer();
    }

    @Bean
    public TvmService tvmService() {
        return new TvmServiceMockImpl();
    }

    @Bean
    public ChangelogProvider coreIntTestChangelogProvider() {
        return () -> "/sql/changelog.xml";
    }

    @Bean
    public YtTokenSupplier ytTokenSupplier() {
        return new YtTokenSupplier("", "test-token", "");
    }

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderStub();
    }
}
