package ru.yandex.metrika.internal.client.http;

import org.mockserver.client.MockServerClient;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.test.AbstractMockServerTest;
import ru.yandex.metrika.internal.client.http.autoconfigure.MetrikaInternalHttpClientConfig;

/**
 * Общий наследник для всех тестовых классов. Нужен, чтобы честно инициализировать контекст приложения в тестах.
 * Date: 03.03.2022
 * Project: arcadia-market_adv-shop_metrika-internal-client
 *
 * @author eogoreltseva
 */
@SpringBootTest(classes = {
        CommonBeanAutoconfiguration.class,
        JacksonAutoConfiguration.class,
        MetrikaInternalHttpClientConfig.class
})
@TestPropertySource(locations = "/applications.properties")
public class AbstractMetrikaInternalMockServerTest extends AbstractMockServerTest {

    public AbstractMetrikaInternalMockServerTest(MockServerClient server) {
        super(server);
    }
}
