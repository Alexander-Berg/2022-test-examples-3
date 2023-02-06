package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.offer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "CurrentMonthLackSumCheckerTest.before.csv")
class CurrentMonthLackSumCheckerTest extends AbstractCheckerTest {

    private static final String CHECKER_NAME = "currentMonthLackSumChecker";

    @Test
    @DisplayName("Нужно показать")
    void needToShow() {
        mockBalance(11, 10);
        var requestBody = requestBody(1);
        var response = check(CHECKER_NAME, requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("У вендора не оплачен последний месяц")
    void lastMonthNotBilled() {
        var requestBody = requestBody(2);
        var response = check(CHECKER_NAME, requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("У вендора не отключались категории")
    void hasNoCutoffCategories() {
        var requestBody = requestBody(3);
        var response = check(CHECKER_NAME, requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Вендор пополнил баланс в этом месяце")
    void replenishBalance() {
        mockBalance(14, 101);
        var requestBody = requestBody(4);
        var response = check(CHECKER_NAME, requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DisplayName("Вендор подключил новые категории в этом месяце")
    void addNewCategories() {
        var requestBody = requestBody(5);
        var response = check(CHECKER_NAME, requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String requestBody(long vendorId) {
        return String.format("{\"vendorId\": %s}", vendorId);
    }
}