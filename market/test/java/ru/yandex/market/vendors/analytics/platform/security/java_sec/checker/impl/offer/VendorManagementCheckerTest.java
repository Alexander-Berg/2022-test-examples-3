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
class VendorManagementCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Пользователь не может управлять вендором (неактивная роль)")
    void inactiveRole() {
        var requestBody = requestBody(1501, 10);
        var response = check("vendorManagementChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Пользователь может управлять вендором")
    void ok() {
        var requestBody = requestBody(1501, 12);
        var response = check("vendorManagementChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Пользователь может управлять отключенным вендором")
    void manageVendorWithCutoff() {
        var requestBody = requestBody(1502, 10);
        var response = check("vendorManagementChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Пользователь не может управлять неофертным вендором")
    void nonOfferVendor() {
        var requestBody = requestBody(1503, 15);
        var response = check("vendorManagementChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String requestBody(long uid, long vendorId) {
        return String.format("{\"uid\": %s, \"vendorId\": %s}", uid, vendorId);
    }
}