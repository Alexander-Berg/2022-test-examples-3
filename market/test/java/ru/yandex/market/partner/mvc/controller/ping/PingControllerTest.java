package ru.yandex.market.partner.mvc.controller.ping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.mbi.web.ping.GracefulShutdownPingController;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для {@link GracefulShutdownPingController} в mbi-partner.
 *
 * @author Vladislav Bauer
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PingControllerTest extends FunctionalTest {

    private static final String EVERYTHING_IS_OK = "0;OK";

    @Test
    @Order(1)
    @DisplayName("Проверить что ручка '/ping', отвечает корректно (что компонент жив).")
    void testPing() {
        assertThat(execGetRequest("/ping").getBody(), equalTo(EVERYTHING_IS_OK));
    }

    @Test
    @Order(2)
    @DisplayName("Проверить что ручка '/close', начинает выключение компонента")
    void testClose() {
        // Говорим приложению что оно скоро выключится
        execGetRequest("/close");

        // Проверяем что приложение перешло в режим выключения
        assertThrows(HttpServerErrorException.InternalServerError.class, () -> execGetRequest("/ping"));
    }

    private ResponseEntity<String> execGetRequest(String method) {
        return FunctionalTestHelper.get(baseUrl + method);
    }

}
