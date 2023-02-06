package ru.yandex.market.mvc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.security.FunctionalTestHelper;
import ru.yandex.market.security.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "user_info.before.csv")
public class UserInfoControllerTest extends FunctionalTest {

    @Autowired
    private String baseUrl;

    @Test
    public void testExistingUser() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/userInfo/staffLogin?passportUid=123000");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("test-username", response.getBody());
    }

    @Test
    public void testNotExistingUser() {
        assertThrows(HttpClientErrorException.NotFound.class,
                () -> FunctionalTestHelper.get(baseUrl + "/userInfo/staffLogin?passportUid=-1"));
    }
}
