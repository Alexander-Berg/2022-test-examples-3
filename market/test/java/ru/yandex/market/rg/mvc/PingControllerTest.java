package ru.yandex.market.rg.mvc;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Тест для ручки /ping.
 */
public class PingControllerTest extends FunctionalTest {

    private static final String OK_RESPONSE = "0;OK";

    /**
     * Проверить, что ручка "/ping" отвечает корректно.
     */
    @Test
    public void testPing() {
        final String response = FunctionalTestHelper.get(baseUrl + "/ping").getBody();

        assertThat(response, equalTo(OK_RESPONSE));
    }
}
