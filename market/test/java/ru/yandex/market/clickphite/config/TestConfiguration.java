package ru.yandex.market.clickphite.config;

import java.io.File;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import ru.yandex.market.clickphite.config.storage.ClickphiteConfigCodeToMongoCopier;
import ru.yandex.market.clickphite.config.storage.json.ClickphiteConfigFileJsonLoader;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidator;
import ru.yandex.market.health.configs.clickphite.ClickphiteConfigDao;
import ru.yandex.market.health.configs.clickphite.ContextCreator;
import ru.yandex.market.health.configs.clickphite.defaults.ClickphiteDefaultValueResolver;
import ru.yandex.market.health.configs.clickphite.spring.HealthConfigUtilsClickphiteInternalSpringConfig;
import ru.yandex.market.health.configs.clickphite.validation.query.MetricContextGroupValidator;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 29.11.16
 */
@Configuration
@PropertySource("classpath:/test.properties")
@Import(HealthConfigUtilsClickphiteInternalSpringConfig.class)
public class TestConfiguration {
    private static final MongoClient MONGO_CLIENT = new MongoClient(
        new ServerAddress(
            new MongoServer(new MemoryBackend()).bind()
        )
    );

    @Autowired
    private ClickphiteDefaultValueResolver defaultValueResolver;

    @Autowired
    private ClickphiteConfigDao configDao;

    @Bean
    public ConfigValidator configValidator() {
        return new ConfigValidator();
    }

    @Bean
    public Function<String, ConfigurationService> configurationServiceFactory() {
        return this::configurationService;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ConfigurationService configurationService(String configDirPath) {
        String absoluteConfigDirPath = new File(configDirPath).getAbsolutePath();
        ClickphiteConfigFileJsonLoader configFileJsonLoader = new ClickphiteConfigFileJsonLoader(absoluteConfigDirPath);

        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigDir(absoluteConfigDirPath);
        configurationService.setDefaultDatabase("market");
        configurationService.setDashboardGraphiteDataSource("market");
        configurationService.setMonitoring(Mockito.mock(ComplicatedMonitoring.class));
        configurationService.setConfigValidator(configValidator());
        configurationService.setMetricContextGroupValidator(Mockito.mock(MetricContextGroupValidator.class));
        configurationService.setUseNewConfigLoading(true);
        configurationService.setConfigFileJsonLoader(configFileJsonLoader);
        configurationService.setDefaultValueResolver(defaultValueResolver);
        configurationService.setConfigDao(configDao);
        configurationService.setContextCreator(
            new ContextCreator(
                "",
                "market",
                Collections.emptyList(),
                "",
                "",
                "",
                "",
                "",
                false,
                false,
                null,
                null
            )
        );

        ComplicatedMonitoring monitoring = new ComplicatedMonitoring();
        new ClickphiteConfigCodeToMongoCopier(configFileJsonLoader, configDao, monitoring)
            .copyConfigsFromCodeToMongo();
        assertThat(monitoring.getResult().getStatus()).isEqualTo(MonitoringStatus.OK);

        return configurationService;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MongoTemplate clickphiteConfigsMongoTemplate() {
        return new MongoTemplate(new SimpleMongoDbFactory(MONGO_CLIENT, UUID.randomUUID().toString()));
    }
}
