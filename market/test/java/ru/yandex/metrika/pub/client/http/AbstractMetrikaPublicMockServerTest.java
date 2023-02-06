package ru.yandex.metrika.pub.client.http;

import org.mockserver.client.MockServerClient;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.test.AbstractMockServerTest;
import ru.yandex.metrika.pub.client.http.autoconfigure.MetrikaPublicHttpClientConfig;

/**
 * Общий наследник для всех тестовых классов. Нужен, чтобы честно инициализировать контекст приложения в тестах.
 * Date: 03.03.2022
 * Project: arcadia-market_adv-shop_metrika-public-client
 *
 * @author eogoreltseva
 */
@SpringBootTest(classes = {
        CommonBeanAutoconfiguration.class,
        JacksonAutoConfiguration.class,
        MetrikaPublicHttpClientConfig.class
})
@TestPropertySource(locations = "/applications.properties")
public class AbstractMetrikaPublicMockServerTest extends AbstractMockServerTest {

    public AbstractMetrikaPublicMockServerTest(MockServerClient server) {
        super(server);
    }
}
