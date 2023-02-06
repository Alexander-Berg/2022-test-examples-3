package ru.yandex.market;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.billing.checkout.logbroker.LogbrokerCheckouterConsumerTestConfig;
import ru.yandex.market.billing.config.SpringApplicationConfig;
import ru.yandex.market.billing.marketing.PartnerMarketingBillingTestConfig;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.config.DaoConfig;
import ru.yandex.market.config.FunctionalTestConfig;
import ru.yandex.market.config.YqlTestConfig;
import ru.yandex.market.config.YtMockConfig;
import ru.yandex.market.yql_test.test_listener.YqlTestListener;

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
                DaoConfig.class,
                YtMockConfig.class,
                YqlTestConfig.class,
                PartnerMarketingBillingTestConfig.class
        }
)
@TestExecutionListeners(value = {
        YqlTestListener.class
})
@ActiveProfiles(profiles = {"functionalTest", "development", "goe-processing"})
@TestPropertySource({
        "classpath:00_application.properties",
        "classpath:functional-test.properties",
        "classpath:00_quartz.properties"
})
@Import(LogbrokerCheckouterConsumerTestConfig.class)
public class FunctionalTest extends JupiterDbUnitTest {
    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.000000ZZZZZ")
            .withZone(ZoneId.systemDefault());
    private static final String BASE_URL = "http://localhost:";
    @Autowired
    protected ObjectMapper objectMapper;
    @LocalServerPort
    private int port;

    protected String baseUrl() {
        return BASE_URL + port;
    }

    protected void assertOk(ResponseEntity<?> response) {
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
