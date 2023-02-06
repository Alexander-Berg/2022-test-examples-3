package ru.yandex.chemodan.app.orchestrator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.chemodan.util.ZkUtils;
import ru.yandex.chemodan.zk.configuration.ImportZkEmbeddedConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author yashunsky
 */
@Configuration
@Import({
        OrchestratorCoreContextConfiguration.class,
})
@ImportZkEmbeddedConfiguration
public class OrchestratorCoreTestConfig extends ChemodanTestBaseContextConfiguration {
    @Bean
    public ZkPath zkRoot() {
        return ZkUtils.rootPath("orchestrator", EnvironmentType.TESTS);
    }

    @Override
    protected AppName appName() {
        return new SimpleAppName("disk", "orchestrator-core");
    }
}
