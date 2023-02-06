package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.offer;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "NextMonthLackWarningCheckerTest.before.csv")
class NextMonthLackWarningCheckerTest extends AbstractCheckerTest {

    private static final String CHECKER_NAME = "nextMonthLackWarningChecker";

    @Test
    @DbUnitDataSet(before ="OfferClock.2020-04-15.csv")
    void needToWarn() {
        var requestBody = requestBody(1501);
        var response = check(CHECKER_NAME, requestBody);
        var expected = responseBody(true);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(before ="OfferClock.2020-04-14.csv")
    void early() {
        var requestBody = requestBody(1501);
        var response = check(CHECKER_NAME, requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(before ="OfferClock.2020-04-15.csv")
    void notOfferUser() {
        var requestBody = requestBody(1500);
        var response = check(CHECKER_NAME, requestBody);
        var expected = responseBody(false);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String requestBody(long uid) {
        return String.format("{\"uid\": %s}", uid);
    }
}