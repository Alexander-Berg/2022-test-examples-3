package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.offer;

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
@DbUnitDataSet(before = "AnalyticsOfferUserCheckerTest.before.csv")
class AnalyticsOfferUserCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Есть доступ на чтение к офертному вендору")
    void readonlyAccess() {
        var requestBody = requestBody(1501);
        var response = check("analyticsOfferUserChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Есть доступ на чтение к офертному вендору")
    void allAccess() {
        var requestBody = requestBody(1502);
        var response = check("analyticsOfferUserChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Есть доступ на чтение к офертному вендору")
    void noAccess() {
        mockUserBalanceVendors(1503, Collections.emptySet());
        var requestBody = requestBody(1503);
        var response = check("analyticsOfferUserChecker", requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Есть доступ на чтение к офертному вендору")
    void hasBalanceAccess() {
        mockUserBalanceVendors(1504, Set.of(10L));
        var requestBody = requestBody(1504);
        var response = check("analyticsOfferUserChecker", requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String requestBody(long uid) {
        return "{\"uid\": " + uid + "}";
    }
}