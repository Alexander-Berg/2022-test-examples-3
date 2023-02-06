package ru.yandex.market.analytics.platform.admin.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.analytics.platform.admin.FunctionalTest;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

/**
 * @author fbokovikov
 */
public class PingControllerTest extends FunctionalTest {

    private static final String PING_RESPONSE = "0;OK";

    @Test
    void ping() {
        var response = FunctionalTestHelper.get(baseUrl() + "/ping").getBody();
        Assertions.assertEquals(
                PING_RESPONSE,
                response
        );
    }
}
