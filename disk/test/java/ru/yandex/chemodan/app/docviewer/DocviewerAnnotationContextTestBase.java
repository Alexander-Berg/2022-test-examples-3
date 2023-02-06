package ru.yandex.chemodan.app.docviewer;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.config.DocviewerInitContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.util.ZkUtils;
import ru.yandex.chemodan.zk.configuration.ZkEmbeddedConfiguration;
import ru.yandex.chemodan.zk.registries.tvm.ZkTvm2ContextConfiguration;
import ru.yandex.commune.alive2.location.Location;
import ru.yandex.commune.alive2.location.LocationType;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author akirakozov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = {DocviewerInitContextConfiguration.class,
                ChemodanInitContextConfiguration.class,
                ZkTvm2ContextConfiguration.class,
                ZkEmbeddedConfiguration.class,
                DocviewerAnnotationContextTestBase.Context.class
        },
        loader=DocviewerAnnotationTestContextLoader.class)
public class DocviewerAnnotationContextTestBase {
    @Autowired
    protected Tvm2 tvm2;

    @Before
    public void initTvm() {
        tvm2.refresh();
    }

    @Configuration
    public static class Context {

        @Bean
        public AppName appName() {
            return new SimpleAppName("docviewer", "web");
        }

        @Bean
        public Location myLocation() {
            return new Location("host", Option.empty(), Option.empty(), Cf.list(), LocationType.YP);
        }

        @Bean
        public ZkPath zkRoot() {
            return ZkUtils.rootPath("docviewer", EnvironmentType.TESTS);
        }
    }
}
