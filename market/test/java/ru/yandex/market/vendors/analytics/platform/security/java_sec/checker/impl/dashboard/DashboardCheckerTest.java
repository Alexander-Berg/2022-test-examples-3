package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;
import ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.dashboard.DashboardChecker;
import ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.dashboard.DashboardOwnerChecker;

/**
 * Тесты на проверку прав:
 * <ol>
 * <li> доступа к дашборду {@link DashboardChecker}</li>
 * <li> владения дашбордом {@link DashboardOwnerChecker}</li>
 * </ol>
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "DashboardCheckerTest.before.csv")
public class DashboardCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Есть доступ к своему дашборду")
    void ownDashboard() {
        var requestBody = dashboardCheckRequestBody(1001, 1);
        checkDashboardReader(requestBody, true);
    }

    @Test
    @DisplayName("Есть доступ к чужому дашборду")
    void hasAccessToOtherUserDashboard() {
        var requestBody = dashboardCheckRequestBody(1002, 1);
        checkDashboardReader(requestBody, true);
    }

    @Test
    @DisplayName("Нет доступа к чужому дашборду")
    void noAccessToOtherUserDashboard() {
        var requestBody = dashboardCheckRequestBody(1004, 1);
        checkDashboardReader(requestBody, false);
    }

    @Test
    @DisplayName("Нет доступа к своему дашборду из-за отсутствия категории у партнёров")
    void noAccessToOwnDashboardDueMissingCategory() {
        var requestBody = dashboardCheckRequestBody(1004, 2);
        checkDashboardReader(requestBody, false);
    }

    @Test
    @DisplayName("Нет доступа к чужому дашборду из-за отсутствия категории у партнёров")
    void noAccessToAnotherUserDashboardDueMissingCategory() {
        var requestBody = dashboardCheckRequestBody(1003, 1);
        checkDashboardReader(requestBody, false);
    }

    @Test
    @DisplayName("Нет доступа к дашборду из-за отключения у партнёра")
    void noAccessDuePartnerCutoff() {
        var requestBody = dashboardCheckRequestBody(1005, 3);
        checkDashboardReader(requestBody, false);
    }

    @Test
    @DisplayName("Есть права владельца дашборда")
    void dashboardOwner() {
        var requestBody = dashboardCheckRequestBody(1001, 1);
        checkDashboardOwner(requestBody, true);
    }

    @Test
    @DisplayName("Нет прав владельца дашборда")
    void noDashboardOwner() {
        var requestBody = dashboardCheckRequestBody(1002, 1);
        checkDashboardOwner(requestBody, false);
    }

    private void checkDashboardReader(String requestBody, boolean expectedAccess) {
        checkDashboardRights("dashboardChecker", requestBody, expectedAccess);
    }

    private void checkDashboardOwner(String requestBody, boolean expectedAccess) {
        checkDashboardRights("dashboardOwnerChecker", requestBody, expectedAccess);
    }

    private void checkDashboardRights(String checker, String requestBody, boolean expectedAccess) {
        var response = check(checker, requestBody);
        var expected = responseBody(expectedAccess);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String dashboardCheckRequestBody(long uid, long dashboardId) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"dashboardId\": %s\n"
                        + "}",
                uid,
                dashboardId
        );
    }
}
