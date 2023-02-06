package ru.yandex.market.adv.data.sync;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.config.EmbeddedPostgresAutoconfiguration;
import ru.yandex.market.adv.config.YtDynamicClientAutoconfiguration;
import ru.yandex.market.adv.config.YtStaticClientAutoconfiguration;
import ru.yandex.market.adv.data.sync.config.DataSyncAutoconfiguration;
import ru.yandex.market.adv.data.sync.config.DataSyncConfigTest;
import ru.yandex.market.adv.yt.test.configuration.YtTestConfiguration;
import ru.yandex.market.adv.yt.test.extension.YtExtension;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

@Slf4j
@ExtendWith({YtExtension.class, SpringExtension.class})
@TestExecutionListeners({DbUnitTestExecutionListener.class, DependencyInjectionTestExecutionListener.class})
@SpringBootTest(
        classes = {
                DataSyncConfigTest.class,
                CommonBeanAutoconfiguration.class,
                JacksonAutoConfiguration.class,
                YtStaticClientAutoconfiguration.class,
                YtDynamicClientAutoconfiguration.class,
                DataSyncAutoconfiguration.class,
                YtTestConfiguration.class,
                EmbeddedPostgresAutoconfiguration.class,
        })
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class)
@TestPropertySource(locations = {"/application.properties"})
public class AbstractTableSynchronizerTest {

    @Value("${yt.static.proxy}")
    protected String testCluster;
}
