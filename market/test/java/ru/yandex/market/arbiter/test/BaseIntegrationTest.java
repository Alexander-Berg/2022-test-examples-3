package ru.yandex.market.arbiter.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.arbiter.Arbiter;
import ru.yandex.market.arbiter.test.config.TestApplicationConfig;

/**
 * @author moskovkin@yandex-team.ru
 * @since 14.05.2020
 *
 * Test startup server on random port and configure ApiClient to communicate with this server.
 * Empty Embedded Postgres database is used for application. Clear database by hand before each test
 * because data produced by webapp not rolled back after @Transactional test finish.
 */
@SpringBootTest(classes = TestApplicationConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@PropertySource("classpath:test.properties")
public class BaseIntegrationTest {
    // We can use @LocalServerPort only on bean level. If context not fully initialized port is unknown.
    @LocalServerPort
    protected int localServerPort;

    @Autowired
    protected ru.yandex.market.arbiter.api.ApiClient arbiterApiClient;

    @Autowired
    protected ru.yandex.businesschat.provider.api.ApiClient businesschatApiClient;

    @Autowired
    protected TestDataService testDataService;

    @BeforeEach
    private void setup() {
        arbiterApiClient.setBasePath("http://localhost:" + localServerPort + Arbiter.API_BASE_PATH);
        businesschatApiClient.setBasePath("http://localhost:" + localServerPort + Arbiter.API_BASE_PATH);
        testDataService.cleanDatabase();
    }
}
