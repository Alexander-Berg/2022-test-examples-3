package ru.yandex.market.loyalty.admin.config;

import java.util.List;

import javax.sql.DataSource;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.mockito.MockSettings;
import org.mockito.listeners.InvocationListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.market.loyalty.core.config.ComponentDescriptor;
import ru.yandex.market.loyalty.core.config.CoreConfigExternal;
import ru.yandex.market.loyalty.core.config.CoreConfigInternal;
import ru.yandex.market.loyalty.core.config.CoreTestConfig;
import ru.yandex.market.loyalty.core.config.DatasourceType;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.mock.MarketLoyaltyCoreMockConfigurer;
import ru.yandex.market.loyalty.core.service.budgeting.CommonDeferredTransactionHooks;
import ru.yandex.market.loyalty.core.service.budgeting.DeferredTransactionHooks;
import ru.yandex.market.loyalty.core.service.ThrottlingControlService;
import ru.yandex.market.loyalty.db.config.EnvironmentOrEmbeddedPostgresDb;
import ru.yandex.market.loyalty.db.config.LiquibaseChangelog;
import ru.yandex.market.request.trace.Module;

import static org.mockito.Mockito.withSettings;

/**
 * Конфиг для интеграционных тестов
 * Выглядит как AdminTestConfig + CoreTestConfig, но вместо моков настоящие внешние зависимости
 *
 * @see AdminTestConfig
 * @see CoreTestConfig
 */
@Configuration
@Import({
        AdminConfigInternal.class,
        EnvironmentOrEmbeddedPostgresDb.class,
        CoreConfigExternal.ReportConfig.class,
        CoreConfigExternal.RecommendationsConfig.class,
        CoreConfigExternal.AvatarConfig.class,
        CoreConfigExternal.AntifraudConfig.class,
        MarketLoyaltyCoreMockConfigurer.UaasClientConfig.class,
        CoreConfigExternal.MemcacheConfig.class,
        CoreConfigExternal.GeoExportConfig.class,
        CoreConfigExternal.JugglerConfig.class,
        CoreConfigExternal.BusinessRulesEngineConfig.class,
        CoreConfigExternal.StaffConfig.class,
        MarketLoyaltyCoreMockConfigurer.BlackboxClientConfig.class,
        MarketLoyaltyCoreMockConfigurer.PersNotifyClientConfig.class,
        MarketLoyaltyCoreMockConfigurer.YabacksMailerConfig.class,
        MarketLoyaltyCoreMockConfigurer.CheckouterClientConfig.class,
        AdminConfigExternal.YtConfig.class,
        AdminConfigExternal.YtJdbcConfig.class,
        AdminConfigExternal.MdsClientConfig.class,
        AdminConfigExternal.LogBrokerConfig.class,
        AdminConfigExternal.SolomonPusherConfig.class,
        MarketLoyaltyAdminMockConfigurer.GeoSearchApiConfig.class,
        MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.class,
        MarketLoyaltyAdminMockConfigurer.BlackboxClientConfig.class,
        MarketLoyaltyAdminMockConfigurer.MbiClientConfig.class,
        MarketLoyaltyCoreMockConfigurer.ClockConfig.class,
        MarketLoyaltyCoreMockConfigurer.TvmConfiguration.class,
        MarketLoyaltyCoreMockConfigurer.TrustConfig.class,
        CoreConfigExternal.YdbConfig.class,
        CoreConfigExternal.LaasConfig.class,
        CoreConfigExternal.BankConfig.class,
        CoreConfigInternal.BankConfig.class,
        AdminConfigExternal.BankConfig.class,
        AdminConfigInternal.BankConfig.class
})
@PropertySource({
        "classpath:/test-application.properties",
        "classpath:/test.properties",
        "classpath:/it.properties"
})
public class ITConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ThrottlingControlService throttlingControlService() {
        return ThrottlingControlService.alwaysSuccess();
    }

    @Bean
    public ClockForTests clock() {
        return new ClockForTests();
    }

    @Bean
    public ComponentDescriptor componentDescriptor() {
        return new ComponentDescriptor(
                "localhost",
                8080,
                Module.MARKET_LOYALTY_ADMIN
        );
    }

    @Bean
    public DeferredTransactionHooks deferredTransactionHooks() {
        return new CommonDeferredTransactionHooks();
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase() {
            @Override
            public void afterPropertiesSet() throws LiquibaseException {
                DatasourceType.READ_WRITE.within(super::afterPropertiesSet);
            }
        };
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(LiquibaseChangelog.ONLY_SCHEMA.getChangelog());
        return liquibase;
    }

    @Bean
    public MockSettings mockSettings(@Autowired(required = false) List<InvocationListener> listeners) {
        MockSettings mockSettings = withSettings();
        if (listeners != null && !listeners.isEmpty()) {
            mockSettings = mockSettings.invocationListeners(
                    listeners.toArray(new InvocationListener[0])
            );
        }
        return mockSettings;
    }
}
