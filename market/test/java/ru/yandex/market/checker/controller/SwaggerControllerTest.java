package ru.yandex.market.checker.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checker.FunctionalTest;

import static ru.yandex.market.common.test.spring.FunctionalTestHelper.get;

public class SwaggerControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получаем yaml файл без ошибок.")
    void test_swaggerController() {
        ResponseEntity<String> stringResponseEntity = get(baseUrl() + "/swagger");

        assertOk(stringResponseEntity);
    }
}
