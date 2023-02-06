package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.offer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "ManagementCheckerTest.before.csv")
class OfferManagementCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("У пользователя нет доступа на управление ни у одного вендора")
    void userCantManageVendors() {
        var requestBody = requestBody(1500);
        var response = check("offerManagementChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Пользователь может управлять двумя вендорами")
    void userManageTwoVendors() {
        var requestBody = requestBody(1501);
        var response = check("offerManagementChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Пользователь может управлять отключенным вендором")
    void userManageOneVendorWithCutoff() {
        var requestBody = requestBody(1502);
        var response = check("offerManagementChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Пользователь не может управлять неоффертным вендором")
    void userManageOnlyNonOfferVendor() {
        var requestBody = requestBody(1503);
        var response = check("offerManagementChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String requestBody(long uid) {
        return String.format("{\"uid\": %s}", uid);
    }
}