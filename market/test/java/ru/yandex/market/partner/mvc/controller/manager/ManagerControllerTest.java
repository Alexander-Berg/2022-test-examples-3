package ru.yandex.market.partner.mvc.controller.manager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тест для {@link ManagerController}.
 */
@DbUnitDataSet(before = "ManagerControllerTest.before.csv")
class ManagerControllerTest extends FunctionalTest {

    @Test
    void getManagerInfoByUid() {
        final ResponseEntity<String> stringResponseEntity = FunctionalTestHelper.get(
                String.format("%s/manager/%d", baseUrl, 9876));

        final String expected = "{" +
                "  \"uid\": 9876," +
                "  \"fullName\": \"Брюс Уэйн\"," +
                "  \"email\": \"batman@wayne.dc\"," +
                "  \"phone\": \"9009\"" +
                "}";

        Assertions.assertEquals(HttpStatus.OK, stringResponseEntity.getStatusCode());

        JsonTestUtil.assertEquals(stringResponseEntity, expected);
    }

    @Test
    void getManagerInfoByUidNotFound() {
        final HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(String.format("%s/manager/%d", baseUrl, 1001)));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}