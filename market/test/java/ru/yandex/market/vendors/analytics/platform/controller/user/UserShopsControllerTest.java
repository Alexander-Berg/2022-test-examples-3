package ru.yandex.market.vendors.analytics.platform.controller.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Functional tests for {@link UserController#getShops(long)}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "UserShopsControllerTest.before.csv")
public class UserShopsControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получение магазинов пользователя")
    void getUserShops() {
        var response = getUserShops(1001);
        var expected = "[1]";
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    void getUserShopsNotFound() {
        var response = getUserShops(3001);
        var expected = "[]";
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Получение магазинов пользователя")
    void getUserLevels() {
        var response = getUserLevels(1001);
        var expected = "{\"shopLevel\":\"FROM_0_5_TO_1\",\"vendorLevel\":null}";
        JsonTestUtil.assertEquals(expected, response);
    }


    private String getUserShops(long userId) {
        var shopsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/users/{userId}/shops")
                .buildAndExpand(userId)
                .toUriString();
        return FunctionalTestHelper.get(shopsUrl).getBody();
    }

    private String getUserLevels(long userId) {
        var shopsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/users/{userId}/levels")
                .buildAndExpand(userId)
                .toUriString();
        return FunctionalTestHelper.get(shopsUrl).getBody();
    }
}
