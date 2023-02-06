package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.role;

import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * Тесты на проверку чекера прав на отзыв ролей {@link RevokeRoleChecker}
 */
@DbUnitDataSet(before = "RevokeRoleCheckerTest.before.csv")
class RevokeRoleCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Чекер проверяет, что пользователь является пользователем партнера, у которого отзывает роль.")
    void failCheckAnotherPartner() {
        var requestBody = revokeRoleRequestBody(5L, 1000L);
        var response = check("revokeRoleChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("У пользователя должна быть активная роль.")
    void userHasInactiveRole() {
        var requestBody = revokeRoleRequestBody(7L, 1006L);
        var response = check("revokeRoleChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Успешная валидация для отзыва роли.")
    void roleRevocationIsAllowed() {
        var requestBody = revokeRoleRequestBody(6L, 1009L);
        var response = check("revokeRoleChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Отзываемая роль неактивна.")
    void roleIsNotActive() {
        var requestBody = revokeRoleRequestBody(6L, 1008L);
        var response = check("revokeRoleChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Не передан uid в чекер")
    void noUid() {
        var requestBody = revokeRoleRequestBody(null, 1009L);
        var response = check("revokeRoleChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Не передан roleId в чекер")
    void noRole() {
        var requestBody = revokeRoleRequestBody(1L, null);
        var response = check("revokeRoleChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Отзыв несуществующей роли.")
    void roleDoesNotExist() {
        var requestBody = revokeRoleRequestBody(1L, 10000L);
        var response = check("revokeRoleChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String revokeRoleRequestBody(@Nullable Long uid, @Nullable Long roleId) {
        return String.format(""
                        + "{"
                        + "\"uid\": %s,"
                        + "\"roleId\": %s"
                        + "}",
                uid, roleId);
    }
}
