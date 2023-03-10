package ru.yandex.market.loyalty.core.utils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.regex.Pattern;

/**
 * Will be replaced with the same one in hamcrest-2.0.
 *
 * @author borettim
 * @author sf105
 */
public class StringRegularExpression extends TypeSafeDiagnosingMatcher<String> {

    protected StringRegularExpression(Pattern pattern) {
        this.pattern = pattern;
    }

    private Pattern pattern;

    @Override
    public void describeTo(Description description) {
        description.appendText("a string matching the pattern ").appendValue(pattern);
    }

    @Override
    protected boolean matchesSafely(String actual, Description mismatchDescription) {
        if (!pattern.matcher(actual).matches()) {
            mismatchDescription.appendText("the string was ").appendValue(actual);
            return false;
        }
        return true;
    }

    /**
     * Validate a string with a {@link java.util.regex.Pattern}.
     * <p>
     * <pre>
     * assertThat(&quot;abc&quot;, matchesRegex(Pattern.compile(&quot;&circ;[a-z]$&quot;));
     * </pre>
     *
     * @param pattern the pattern to be used.
     * @return The matcher.
     */
    public static Matcher<String> matchesRegex(Pattern pattern) {
        return new StringRegularExpression(pattern);
    }

    /**
     * Validate a string with a regex.
     * <p>
     * <pre>
     * assertThat(&quot;abc&quot;, matchesRegex(&quot;&circ;[a-z]+$&quot;));
     * </pre>
     *
     * @param regex The regex to be used for the validation.
     * @return The matcher.
     */
    public static Matcher<String> matchesRegex(String regex) {
        return matchesRegex(Pattern.compile(regex));
    }
}
