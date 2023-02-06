package ru.yandex.market.crm.lb.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.crm.environment.ApplicationEnvironmentResolver;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.PartitionDaoConfig;
import ru.yandex.market.crm.lb.ReaderConfig;
import ru.yandex.market.jmf.db.test.TestDefaultDataSourceConfiguration;
import ru.yandex.market.jmf.handshake.HandshakeServiceConfiguration;
import ru.yandex.market.jmf.lock.LockServiceConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

/**
 * @author apershukov
 */
@Configuration
@Import({
        TestDefaultDataSourceConfiguration.class,
        LockServiceConfiguration.class,
        HandshakeServiceConfiguration.class,
        PartitionDaoConfig.class
})
@PropertySource(name = "testLbReaderProperties", value = "classpath:lb_reader_test.properties")
public class TestLBReaderConfig extends AbstractModuleConfiguration {

    protected TestLBReaderConfig() {
        super("jmf/lb_reader/test");
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
