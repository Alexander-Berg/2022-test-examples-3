package ru.yandex.market.crm.lb.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.environment.ApplicationEnvironmentResolver;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.PartitionDaoConfig;
import ru.yandex.market.crm.lb.ReaderConfig;
import ru.yandex.market.mcrm.handshake.HandshakeServiceConfiguration;
import ru.yandex.market.mcrm.lock.LockServiceConfiguration;

/**
 * @author apershukov
 */
@Configuration
@Import({
        TestDataSourceConfig.class,
        LockServiceConfiguration.class,
        HandshakeServiceConfiguration.class,
        PartitionDaoConfig.class
})
public class TestLBReaderConfig {

    @Bean
    public TestPlaceholderConfigurer placeholderConfigurer() {
        return new TestPlaceholderConfigurer();
    }

    @Bean
    public EnvironmentResolver environmentResolver() {
        return new ApplicationEnvironmentResolver();
    }

    @Bean
    public ReaderConfig logbrokerConfiguration(@Value("${logBroker.enabled}") boolean enabled,
                                               @Value("${logBroker.safe.interval.size:1}") int safeIntervalSize,
                                               @Value("${logBroker.workers.pool.size:1}") int workersPoolSize,
                                               @Value("${logBroker.error.delay:60}") int errorDelay) {
        return new ReaderConfig(
                enabled,
                workersPoolSize,
                safeIntervalSize,
                errorDelay,
                4,
                LBInstallation.LOGBROKER
        );
    }
}
