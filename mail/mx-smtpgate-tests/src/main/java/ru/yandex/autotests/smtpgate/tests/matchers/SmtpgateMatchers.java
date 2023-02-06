package ru.yandex.autotests.smtpgate.tests.matchers;

import com.jayway.restassured.response.Response;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 * User: alex89
 * Date: 04.02.2016
 */
public class SmtpgateMatchers {
    public static Matcher<Response> hasStatusCode(int expectedCode) {
        return new FeatureMatcher<Response, Integer>(equalTo(expectedCode),
                "status code should be", "actual") {
            @Override
            protected Integer featureValueOf(Response response) {
                return response.getStatusCode();
            }
        };
    }

    public static Matcher<Response> hasResponseBody(Matcher bodyMatcher) {
        return new FeatureMatcher<Response, String>(bodyMatcher,
                "body should be", "actual") {
            @Override
            protected String featureValueOf(Response response) {
                return response.asString();
            }
        };
    }
}
