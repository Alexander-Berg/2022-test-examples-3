package ru.yandex.market.checkout.util.matching;

import java.math.BigDecimal;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * @author mkasumov
 */
public class AsNumberMatcher extends CustomTypeSafeMatcher<String> {

    private final Matcher<Number> delegateMatcher;

    public AsNumberMatcher(Matcher<Number> delegateMatcher) {
        super("a number");
        this.delegateMatcher = delegateMatcher;
    }

    @Override
    protected void describeMismatchSafely(String item, Description mismatchDescription) {
        try {
            delegateMatcher.describeMismatch(parseNumber(item), mismatchDescription);
        } catch (NumberFormatException e) {
            mismatchDescription.appendText("invalid number ").appendValue(item);
        }
    }

    @Override
    protected boolean matchesSafely(String item) {
        try {
            return delegateMatcher.matches(parseNumber(item));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Number parseNumber(String value) throws NumberFormatException {
        return new BigDecimal(value);
    }
}
