package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * Тесты на проверку прав:
 * <ol>
 * <li> доступа к категории {@link CategoryUserChecker}</li>
 * </ol>
 *
 * @author sergeymironov.
 */
@DbUnitDataSet(before = "CategoryUserCheckerTest.before.csv")
class CategoryUserCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Есть доступ к категории")
    void accessCategoryUser() {
        var requestBody = categoryUserCheckRequestBody(1001, 91491);
        checkCategoryUser("categoryUserChecker", requestBody, true);
    }

    @Test
    @DisplayName("Нет доступа к категории")
    void noAccessCategoryUser() {
        var requestBody = categoryUserCheckRequestBody(1005, 91491);
        checkCategoryUser("categoryUserChecker", requestBody, false);
    }

    private void checkCategoryUser(String checker, String requestBody, boolean expectedAccess) {
        var response = check(checker, requestBody);
        var expected = responseBody(expectedAccess);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String categoryUserCheckRequestBody(long uid, long hid) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"hid\": %s\n"
                        + "}",
                uid,
                hid
        );
    }
}