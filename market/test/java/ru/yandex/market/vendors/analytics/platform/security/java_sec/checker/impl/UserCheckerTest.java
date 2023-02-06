package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * Тест на чекер прав доступа пользователя в аналитическую платформу {@link UserChecker}.
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "UserCheckerTest.before.csv")
public class UserCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Не передан uid в чекер")
    void noUid() {
        var response = check("analyticsUserChecker", "{}");
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("У пользователя нет доступа")
    void noAccess() {
        var requestBody = userCheckRequestBody(1000);
        var response = check("analyticsUserChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Успешный сценарий авторизации (ANALYTICS_USER)")
    void successCheckAnalyticsUser() {
        var requestBody = userCheckRequestBody(1500);
        var response = check("analyticsUserChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Успешный сценарий авторизации (ADMIN)")
    void successCheckAdminUser() {
        var requestBody = userCheckRequestBody(1501);
        var response = check("analyticsAdminChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("У пользователя нет роли админа")
    void failCheckAdminUser() {
        var requestBody = userCheckRequestBody(1500);
        var response = check("analyticsAdminChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("У магазинного пользователя есть доступ")
    void shopHasAccess() {
        var requestBody = userCheckRequestBody(2001);
        var response = check("analyticsUserChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("У магазинного пользователя нет доступа")
    void shopHasNoAccess() {
        var requestBody = userCheckRequestBody(2002);
        var response = check("analyticsUserChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String userCheckRequestBody(long uid) {
        return String.format(""
                        + "{"
                        + "\"uid\": %s"
                        + "}",
                uid);
    }
}
