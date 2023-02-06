package ru.yandex.market.adv.b2bmonetization.client.http;

import java.util.List;
import java.util.Map;

import org.mockserver.client.MockServerClient;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.adv.b2bmonetization.client.http.autoconfigure.MonetizationHttpClientConfig;
import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.test.AbstractMockServerTest;

/**
 * Общий наследник для всех тестовых классов.
 * Инициализируте контекст приложения в тестах.
 */
@SpringBootTest(classes = {
        CommonBeanAutoconfiguration.class,
        JacksonAutoConfiguration.class,
        MonetizationHttpClientConfig.class
})
@TestPropertySource(locations = "/applications.properties")
public class AbstractMonetizationMockServerTest extends AbstractMockServerTest {

    public AbstractMonetizationMockServerTest(MockServerClient server) {
        super(server);
    }

    protected void initMock(String method,
                            String path,
                            String requestFile,
                            String responseFile,
                            Map<String, List<String>> parameters) {
        mockServerPath(
                method,
                path,
                requestFile == null ? null : "json/" + requestFile + ".json",
                parameters,
                200,
                "json/" + responseFile + ".json"
        );
    }
}
