package ru.yandex.market.crm.campaign.test;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.campaign.placeholders.ConfigPropertiesProvider;
import ru.yandex.market.crm.campaign.services.ServicesConfig;
import ru.yandex.market.crm.environment.ApplicationEnvironmentResolver;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.platform.config.PlatformConfiguration;
import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;

/**
 * @author apershukov
 */
@Configuration
@Import({
        TestYtConfig.class,
        TestNetConfig.class,
        ServicesConfig.class,
        TestSupplierConf.class,
        TestMasterReadOnlyDataSourceConfiguration.class,
        PlatformConfiguration.class
})
public class ServiceTestConfig {

    @Bean
    public EnvironmentResolver environmentResolver() {
        return new ApplicationEnvironmentResolver();
    }

    @Bean
    public ConfigPropertiesProvider configPropertiesProvider() {
        return new ConfigPropertiesProvider(new Properties());
    }

    @Bean
    public ChangelogProvider campaingChangelogProvider() {
        return ()->"/sql/lilucrm-campaign-changelog.xml";
    }
}
