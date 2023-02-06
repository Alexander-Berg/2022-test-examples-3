package ru.yandex.market;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static ru.yandex.market.common.test.spring.FunctionalTestHelper.get;

/**
 * Тест для ручки /ping.
 */

public class PingTest extends FunctionalTest {


    @Test
    public void serverPing() {
        ResponseEntity<String> response = get(baseUrl() + "/ping");
        assertOk(response);
    }
}
