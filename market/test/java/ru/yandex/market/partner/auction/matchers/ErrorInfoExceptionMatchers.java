package ru.yandex.market.partner.auction.matchers;

import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import ru.yandex.common.framework.core.SimpleErrorInfo;
import ru.yandex.market.core.error.ErrorInfoException;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author vbudnev
 */
public class ErrorInfoExceptionMatchers {

    @Factory
    public static Matcher<ErrorInfoException> hasErrorCode(final Integer expectedHttpCode) {
        return new FeatureMatcher<ErrorInfoException, Integer>(
                equalTo(expectedHttpCode),
                "httpCode",
                "httpCode"
        ) {
            @Override
            protected Integer featureValueOf(final ErrorInfoException actual) {
                return actual.getErrorInfo().getStatusCode();
            }
        };
    }

    @Factory
    public static Matcher<ErrorInfoException> hasErrorMessage(final String expectedErrorMessage) {
        return new FeatureMatcher<ErrorInfoException, String>(
                equalTo(expectedErrorMessage),
                "errorMessage",
                "errorMessage"
        ) {
            @Override
            protected String featureValueOf(final ErrorInfoException exception) {
                //todod woohoo classcast exception? =)
                return ((SimpleErrorInfo) exception.getErrorInfo()).getMessageCode();
            }

        };
    }
}
