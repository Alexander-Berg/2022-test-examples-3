package ru.yandex.market.partner.contract;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.partner.contract.config.FunctionalTestConfig;
import ru.yandex.market.partner.contract.config.SpringApplicationConfig;

/**
 * Базовый класс для всех тестов
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SpringApplicationConfig.class
)
@SpringJUnitConfig(
        classes = {
                FunctionalTestConfig.class
        }
)
@ActiveProfiles(profiles = {"functionalTest", "development"})
@TestPropertySource({
        "classpath:functional-test.properties"
})
public class FunctionalTest extends JupiterDbUnitTest {
}
