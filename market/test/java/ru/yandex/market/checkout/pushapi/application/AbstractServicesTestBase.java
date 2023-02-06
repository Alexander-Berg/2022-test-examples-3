package ru.yandex.market.checkout.pushapi.application;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.checkout.pushapi.config.EmbeddedPostgresConfig;
import ru.yandex.market.checkout.pushapi.config.TestContextConfig;
import ru.yandex.market.checkout.pushapi.config.TestMockConfig;
import ru.yandex.market.checkout.pushapi.helpers.CheckouterMockConfigurer;

@ActiveProfiles("test")
@ContextConfiguration(classes = AbstractServicesTestBase.TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractServicesTestBase {

    @Autowired
    private List<WireMockServer> mockServers;

    @Autowired
    private CheckouterMockConfigurer checkouterMockConfigurer;

    @AfterEach
    public void tearDownBase() throws Exception {
        mockServers.forEach(WireMockServer::resetAll);
    }

    @BeforeEach
    public void setUpBase() throws Exception {
        checkouterMockConfigurer.setDefaultResponse();
    }

    @ComponentScan("ru.yandex.market.checkout.pushapi.config")
    @Import({TestContextConfig.class, TestMockConfig.class, EmbeddedPostgresConfig.class})
    @Configuration
    public static class TestConfig {

    }
}
