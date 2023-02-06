package ru.yandex.market.mbi.partner_stat.mvc.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;

/**
 * Тесты для {@link TestResponseController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class TestResponseControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Хороший ответ из json ручки")
    void testSuccessFormat() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(baseUrl() + "/testResponse/false", String.class);

        JsonTestUtil.assertEquals(responseEntity, getClass(), "TestResponseController.success.json");
    }

    @Test
    @DisplayName("Ответ с ошибкой из json ручки")
    void testFailFormat() {
        final HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl() + "/testResponse/true", String.class)
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        JsonTestUtil.assertResponseErrorMessage(exception, getClass(), "TestResponseController.fail.fieldName.json");
    }

    @Test
    @DisplayName("Хороший ответ из НЕ json ручки")
    void testSuccessExcelFormat() {
        final ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.get(baseUrl() + "/testResponse/false/excel", byte[].class);

        final byte[] expected = {0, 4, 5};
        Assertions.assertArrayEquals(expected, responseEntity.getBody());
    }

    @Test
    @DisplayName("Ответ с ошибкой из НЕ json ручки")
    void testFailExcelFormat() {
        final HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl() + "/testResponse/true/excel", String.class)
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        JsonTestUtil.assertResponseErrorMessage(exception, getClass(), "TestResponseController.fail.json");
    }
}
