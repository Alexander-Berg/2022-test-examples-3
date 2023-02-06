package ru.yandex.market.adv.b2bmonetization;

import java.util.function.BiConsumer;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.adv.b2bmonetization.config.TestConfig;
import ru.yandex.market.adv.config.EmbeddedPostgresAutoconfiguration;
import ru.yandex.market.adv.test.AbstractMockServerTest;
import ru.yandex.market.adv.yt.test.configuration.YtTestConfiguration;
import ru.yandex.market.adv.yt.test.extension.YtExtension;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

@ExtendWith(YtExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                EmbeddedPostgresAutoconfiguration.class,
                SpringApplicationConfig.class,
                JacksonAutoConfiguration.class,
                YtTestConfiguration.class,
                TestConfig.class
        }
)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class)
@TestPropertySource(locations = "/99_functional_application.properties")
public abstract class AbstractMonetizationMockServerTest extends AbstractMockServerTest {

    @Autowired
    private BiConsumer<String, Runnable> ytRunner;

    public AbstractMonetizationMockServerTest(MockServerClient server) {
        super(server);
    }

    protected void run(String newPrefix, Runnable runnable) {
        ytRunner.accept(newPrefix, runnable);
    }
}
