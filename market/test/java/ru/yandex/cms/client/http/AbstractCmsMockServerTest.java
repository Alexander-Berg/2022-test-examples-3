package ru.yandex.cms.client.http;

import org.mockserver.client.MockServerClient;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.cms.client.http.autoconfigure.CmsHttpClientConfig;
import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.test.AbstractMockServerTest;

/**
 * Общий наследник для всех тестовых классов. Нужен, чтобы честно инициализировать контекст приложения в тестах.
 * Date: 14.10.2021
 * Project: arcadia-market_jlibrary_cms-client
 *
 * @author alexminakov
 */
@SpringBootTest(classes = {
        CommonBeanAutoconfiguration.class,
        JacksonAutoConfiguration.class,
        CmsHttpClientConfig.class
})
@TestPropertySource(locations = "/applications.properties")
public abstract class AbstractCmsMockServerTest extends AbstractMockServerTest {

    public AbstractCmsMockServerTest(MockServerClient server) {
        super(server);
    }
}
