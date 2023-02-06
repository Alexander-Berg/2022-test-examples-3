package ru.yandex.market.billing.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.billing.api.config.EmbeddedPostgresConfig;
import ru.yandex.market.billing.api.config.FunctionalTestConfig;
import ru.yandex.market.billing.api.config.SpringApplicationConfig;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

/**
 * Базовый класс для всех тестов
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SpringApplicationConfig.class
)
@SpringJUnitConfig(
        classes = {
                FunctionalTestConfig.class,
                EmbeddedPostgresConfig.class
        }
)
@ActiveProfiles(profiles = {"functionalTest", "development"})
@TestPropertySource({
        "classpath:functional-test.properties"
})
public class FunctionalTest extends JupiterDbUnitTest {
}
