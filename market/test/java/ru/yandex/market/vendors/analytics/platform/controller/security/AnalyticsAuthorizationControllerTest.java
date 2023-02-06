package ru.yandex.market.vendors.analytics.platform.controller.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * Простые тесты на базовые случаи {@link AnalyticsAuthorizationController}, не привязанные к конкретным чекерам.
 *
 * @author antipov93.
 */
public class AnalyticsAuthorizationControllerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Передан неизвестный чекер")
    void unknownChecker() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> check("unknownChecker", "{}")
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        String expected = "" +
                "{  \n" +
                "   \"code\":\"BAD_REQUEST\",\n" +
                "   \"message\":\"Can not create bean with name: unknownChecker\"\n" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }
}
