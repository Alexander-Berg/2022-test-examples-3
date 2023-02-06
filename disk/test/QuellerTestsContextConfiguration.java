package ru.yandex.chemodan.app.queller.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.queller.bazinga.QuellerBazingaContextConfiguration;
import ru.yandex.chemodan.app.queller.celery.QuellerCeleryContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.util.ZkUtils;
import ru.yandex.chemodan.zk.configuration.ZkEmbeddedConfiguration;
import ru.yandex.commune.zk2.ZkConfiguration;
import ru.yandex.commune.zk2.ZkMediaDevelopmentUtils;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.inside.admin.conductor.ConductorContextConfiguration;
import ru.yandex.misc.env.EnvironmentType;

/**
 * @author dbrylev
 */
@Configuration
@Import({
        QuellerCeleryContextConfiguration.class,
        ChemodanInitContextConfiguration.class,
        ConductorContextConfiguration.class,
        QuellerBazingaContextConfiguration.class,
        TestsBaseContextConfiguration.class,
        ZkEmbeddedConfiguration.class,
})
public class QuellerTestsContextConfiguration {

    @Bean
    public ZkConfiguration zkConfiguration() {
        return new ZkConfiguration(ZkMediaDevelopmentUtils.hostsNoEnvCheck());
    }

    @Bean
    public ZkPath zkRoot() {
        return ZkUtils.rootPath("queller", EnvironmentType.TESTS);
    }
}
