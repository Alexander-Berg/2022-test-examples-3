package ru.yandex.market.adv.shop.integration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.adv.config.EmbeddedPostgresAutoconfiguration;
import ru.yandex.market.adv.shop.integration.configuration.ShopIntegrationTestConfiguration;
import ru.yandex.market.adv.test.AbstractTest;
import ru.yandex.market.adv.yt.test.configuration.YtTestConfiguration;
import ru.yandex.market.adv.yt.test.extension.YtExtension;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

/**
 * Общий наследник для всех тестовых классов. Нужен, чтобы честно инициализировать контекст приложения в тестах.
 * Для БД поднимается embedded PostgreSQL.
 *
 * @author eogoreltseva
 */
@ExtendWith(YtExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                EmbeddedPostgresAutoconfiguration.class,
                SpringApplicationConfig.class,
                JacksonAutoConfiguration.class,
                YtTestConfiguration.class,
                ShopIntegrationTestConfiguration.class
        }
)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class)
@TestPropertySource(locations = "/99_functional_application.properties")
public abstract class AbstractShopIntegrationTest extends AbstractTest {

    @Autowired
    private ShopIntegrationTestRunner shopIntegrationTestRunner;

    protected JobExecutionContext mockContext() {
        return Mockito.mock(JobExecutionContext.class);
    }

    protected void run(String newPrefix, Runnable runnable) {
        shopIntegrationTestRunner.run(newPrefix, runnable);
    }

    protected void run(String newPrefix, long clickTableInterval, Runnable runnable) {
        shopIntegrationTestRunner.run(newPrefix, clickTableInterval, runnable);
    }
}
