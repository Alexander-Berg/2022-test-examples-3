package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "BalanceUserCheckerTest.before.csv")
class BalanceUserCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Есть права на пополнение баланса вендора")
    void hasRole() {
        mockUserBalanceVendors(1500, List.of(10L, 20L));
        var requestBody = requestBody(1500, 10);
        var response = check("balanceUserChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Нет прав на пополнение баланса вендора")
    void hasNoRole() {
        mockUserBalanceVendors(2001, Collections.emptyList());
        var requestBody = requestBody(2001, 11);
        var response = check("balanceUserChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Незарегестрированный вендор")
    void nonOfferVendor() {
        var requestBody = requestBody(2001, 12);
        var response = check("balanceUserChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String requestBody(long uid, long vendorId) {
        return String.format("{\"uid\": %s, \"vendorId\": %s}", uid, vendorId);
    }
}