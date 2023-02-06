package ru.yandex.market.vendors.analytics.platform.security;

import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.controller.billing.BalanceFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.security.AnalyticsAuthorizationController;
import ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.JavaSecChecker;

/**
 * Базовый класс для тестов {@link JavaSecChecker} и {@link AnalyticsAuthorizationController}.
 *
 * @author antipov93.
 */
public abstract class AbstractCheckerTest extends BalanceFunctionalTest {

    protected void assertAccess(String checker, String request, boolean expectedAccess) {
        var response = check(checker, request);
        var expected = responseBody(expectedAccess);
        JsonTestUtil.assertEquals(expected, response);
    }

    protected String check(String checker, String jsonBody) {
        var url = checkUrl(checker);
        return FunctionalTestHelper.postForJson(url, jsonBody);
    }

    public static String responseBody(boolean hasAccess) {
        return String.format(""
                        + "{\n"
                        + "    \"hasAccess\":%b\n"
                        + "}",
                hasAccess
        );
    }

    private String checkUrl(String checker) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("check?checker={checker}&params=")
                .buildAndExpand(checker)
                .toUriString();
    }
}
