package ru.yandex.market.sdk.userinfo.matcher;

import java.util.Optional;

import javax.annotation.concurrent.NotThreadSafe;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * @authror dimkarp93
 */
@NotThreadSafe
public class OptionalMatcher<T> extends TypeSafeMatcher<Optional<T>> {
    private final Matcher<T> matcher;
    private final boolean hasToPresented;

    private volatile Boolean presented = null;

    public static <T> OptionalMatcher<T> of(T item) {
        return new OptionalMatcher<>(Matchers.is(item), null != item);
    }

    public static <T> OptionalMatcher<T> existAnd(Matcher<T> matcher) {
        return new OptionalMatcher<>(matcher, true);
    }

    public static <T> OptionalMatcher<T> not() {
        return new OptionalMatcher<>((Matcher<T>) Matchers.notNullValue(), false);
    }

    private OptionalMatcher(Matcher<T> matcher, boolean hasToPresented) {
        this.matcher = matcher;
        this.hasToPresented = hasToPresented;
    }

    @Override
    protected boolean matchesSafely(Optional<T> item) {
        presented = item.isPresent();
        if (!hasToPresented) {
            return !presented;
        }
        return presented && matcher.matches(item.get());
    }

    @Override
    public void describeTo(Description description) {
        if (null == presented) {
            //ни разу не звали матч
            return;
        }
        if (!presented) {
            description.appendText("Not presented");
            return;
        }
        matcher.describeTo(description);
    }
}
