package ru.yandex.market.mbi.partner_stat;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.mbi.partner_stat.config.FunctionalTestConfig;

/**
 * Базовый класс для функциональных тестов.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {MbiPartnerStat.class}
)
@SpringJUnitConfig(
        classes = FunctionalTestConfig.class
)
@ActiveProfiles(profiles = {"functionalTest", "development"})
@TestPropertySource({"classpath:functional-test.properties"})
@ParametersAreNonnullByDefault
public abstract class FunctionalTest extends JupiterDbUnitTest {

    private static final String BASE_URL = "http://localhost:";

    @LocalServerPort
    private int port;

    protected String baseUrl() {
        return BASE_URL + port;
    }
}
