package ru.yandex.chemodan.app.psbilling.worker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.psbilling.core.PsBillingCoreTestConfig;
import ru.yandex.chemodan.app.psbilling.worker.config.PsBillingWorkerCoreConfiguration;
import ru.yandex.chemodan.util.ZkUtils;
import ru.yandex.chemodan.zk.configuration.ImportZkEmbeddedConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

@Configuration
@Import({
        PsBillingWorkerCoreConfiguration.class
})
@ImportEmbeddedPg
@ImportZkEmbeddedConfiguration
public class PsBillingWorkerTestConfig extends PsBillingCoreTestConfig {
    @Bean
    public ZkPath zkRoot() {
        return ZkUtils.rootPath("ps-billing-worker", EnvironmentType.TESTS);
    }

    @Override
    protected AppName appName() {
        return new SimpleAppName("disk", "ps-billing-worker");
    }
}
