package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "ApplicationTabCheckerTest.before.csv")
class ApplicationTabCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Есть доступ только к вендору")
    void hasOnlyVendorAccess() {
        mockUserBalanceVendors(1500, Collections.emptySet());
        var requestBody = requestBody(1500);
        var response = check("applicationTabChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Есть доступ только к вендору (балансовый)")
    void hasOnlyBalanceVendorAccess() {
        mockUserBalanceVendors(1000, Set.of(15L));
        var requestBody = requestBody(1000);
        var response = check("applicationTabChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Есть доступ и к вендору и к магазину")
    void hasBothVendorAndShopAccess() {
        mockUserBalanceVendors(1501, Collections.emptySet());
        var requestBody = requestBody(1501);
        var response = check("applicationTabChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Есть доступ только к магазину")
    void hasOnlyShopAccess() {
        mockUserBalanceVendors(1502, Collections.emptySet());
        var requestBody = requestBody(1502);
        var response = check("applicationTabChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Нет доступа ни к чему")
    void hasNoAccess() {
        mockUserBalanceVendors(1503, Collections.emptySet());
        var requestBody = requestBody(1503);
        var response = check("applicationTabChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String requestBody(long uid) {
        return "{\"uid\": " + uid + "}";
    }
}