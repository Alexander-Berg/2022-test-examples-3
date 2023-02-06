package ru.yandex.market.adv.content.manager;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.quartz.JobExecutionContext;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.cms.client.mock.CmsMockClientConfig;
import ru.yandex.market.adv.config.EmbeddedPostgresAutoconfiguration;
import ru.yandex.market.adv.test.AbstractMockServerTest;
import ru.yandex.market.adv.yt.test.configuration.YtTestConfiguration;
import ru.yandex.market.adv.yt.test.extension.YtExtension;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

/**
 * Общий наследник для всех тестовых классов в которых требуется поднятие {@link MockServerClient}.
 * Date: 14.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@ExtendWith(YtExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                EmbeddedPostgresAutoconfiguration.class,
                SpringApplicationConfig.class,
                JacksonAutoConfiguration.class,
                YtTestConfiguration.class,
                CmsMockClientConfig.class
        }
)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class)
@TestPropertySource(locations = "/99_functional_application.properties")
public abstract class AbstractContentManagerMockServerTest extends AbstractMockServerTest {

    protected AbstractContentManagerMockServerTest(MockServerClient server) {
        super(server);
    }

    protected JobExecutionContext mockContext() {
        return Mockito.mock(JobExecutionContext.class);
    }
}
