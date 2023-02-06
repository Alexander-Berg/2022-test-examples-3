package ru.yandex.autotests.innerpochta.imap.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * User: lanwen
 * Date: 15.04.14
 * Time: 0:31
 * <p>
 * Костылик для хамкреста - обычный not не имеет mismatchDescription
 */

public class IsNotExtended<T> extends TypeSafeMatcher<T> {
    private final Matcher<T> matcher;

    public IsNotExtended(Matcher<T> matcher) {
        this.matcher = matcher;
    }

    /**
     * Creates a matcher that wraps an existing matcher, but inverts the logic by which
     * it will match.
     * <p/>
     * For example:
     * <pre>assertThat(cheese, is(not(equalTo(smelly))))</pre>
     *
     * @param matcher the matcher whose sense should be inverted
     */
    @Factory
    public static <T> Matcher<T> not(Matcher<T> matcher) {
        return new IsNotExtended<T>(matcher);
    }

    @Override
    protected boolean matchesSafely(Object arg) {
        return !matcher.matches(arg);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("not ").appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(Object obj, Description mismatchDescription) {
        matcher.describeMismatch(obj, mismatchDescription);
    }
}

