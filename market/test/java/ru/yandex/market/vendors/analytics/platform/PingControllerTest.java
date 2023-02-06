package ru.yandex.market.vendors.analytics.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;

class PingControllerTest extends FunctionalTest {

    /**
     * Тест проверяет, что приложение analytics-platform стартует и отвечает на пинг стандартным способом
     */
    @Test
    void ping() {
        String actualResponse = FunctionalTestHelper.get(baseUrl() + "/ping", String.class).getBody();
        var expectedResponse = "0;OK";
        Assertions.assertEquals(
                expectedResponse,
                actualResponse
        );
    }
}
