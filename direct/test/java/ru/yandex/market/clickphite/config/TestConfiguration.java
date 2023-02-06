package ru.yandex.market.clickphite.config;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.clickphite.config.validation.config.MetricContextGroupValidator;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidator;
import ru.yandex.market.monitoring.ComplicatedMonitoring;

import java.io.File;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 29.11.16
 */
@Configuration
public class TestConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ConfigValidator configValidator() {
        return new ConfigValidator();
    }

    @Bean
    public ConfigurationService configurationService() {
        String configDirPath = new File(new File("../config-cs-clickphite"), "src/configs").getAbsolutePath();
        return createConfigurationService(configDirPath, applicationContext.getBean(ConfigValidator.class));
    }

    public static ConfigurationService createConfigurationService(String configDirPath) {
        return createConfigurationService(configDirPath, new ConfigValidator());
    }

    public static ConfigurationService createConfigurationService(String configDirPath, ConfigValidator configValidator) {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigDir(configDirPath);
        configurationService.setDefaultDatabase("market");
        configurationService.setDashboardGraphiteDataSource("market");
        configurationService.setMonitoring(Mockito.mock(ComplicatedMonitoring.class));
        configurationService.setConfigValidator(configValidator);
        configurationService.setMetricContextGroupValidator(Mockito.mock(MetricContextGroupValidator.class));
        return configurationService;
    }
}
