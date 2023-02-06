package ru.yandex.market.pricelabs.api.api;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.AbstractApiSpringConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicApiTest extends AbstractApiSpringConfiguration {

    @Autowired
    private PublicApi publicApiBean;
    private PublicApiInterfaces publicApi;

    @BeforeEach
    void init() {
        publicApi = MockMvcProxy.buildProxy(PublicApiInterfaces.class, publicApiBean);
    }

    @Test
    void testValidApi() {
        assertNotNull(getValidator().getConstraintsForClass(PublicApi.class));
    }

    // Тест проверяет только то, что контексты сконфигурированы корректно
    @Test
    void testVersion() {
        assertTrue(Objects.requireNonNull(publicApi.versionGet().getBody()).startsWith("r"));
    }

}
