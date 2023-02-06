package ru.yandex.market.loyalty.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.mock.MarketLoyaltyCoreMockConfigurer;
import ru.yandex.market.loyalty.core.service.ThrottlingControlService;
import ru.yandex.market.request.trace.Module;

import java.sql.Connection;
import java.sql.SQLException;

@Configuration
@Import({
        DatasourceConfig.class,
        CoreConfigInternal.class,
        MarketLoyaltyCoreMockConfigurer.ReportConfig.class,
        MarketLoyaltyCoreMockConfigurer.AvatarConfig.class,
        MarketLoyaltyCoreMockConfigurer.UaasClientConfig.class,
        MarketLoyaltyCoreMockConfigurer.MemcacheConfig.class,
        MarketLoyaltyCoreMockConfigurer.GeoExportConfig.class,
        MarketLoyaltyCoreMockConfigurer.BlackboxClientConfig.class,
        MarketLoyaltyCoreMockConfigurer.PersNotifyClientConfig.class,
        MarketLoyaltyCoreMockConfigurer.YabacksMailerConfig.class,
        MarketLoyaltyCoreMockConfigurer.LogBrokerConfig.class,
        MarketLoyaltyCoreMockConfigurer.YdbConfig.class
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
    public MasterTestManager masterTestManager() {
        return new MasterTestManager();
    }

    public static class MasterTestManager implements ConnectionInterceptorListener {
        private volatile boolean masterFail;

        public void setMasterFail(boolean masterFail) {
            this.masterFail = masterFail;
        }

        @Override
        public ConnectionSupplier setupSessionOnGetConnection(
                ConnectionSupplier connectionSupplier
        ) {
            return () -> {
                Connection connection = connectionSupplier.get();

                if (!connection.isReadOnly() && masterFail) {
                    throw new SQLException();
                } else {
                    return connection;
                }
            };
        }
    }
}
