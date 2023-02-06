package ru.yandex.mail.things.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

public class WithWaitFor <T> extends TypeSafeMatcher<T> {
    public static final long DEFAULT_INTERVAL = SECONDS.toMillis(1);
    public static final long DEFAULT_TIMEOUT = SECONDS.toMillis(30);

    private Matcher<? super T> matcher;

    private long timeoutInMilliseconds;
    private long intervalInMilliseconds;

    public WithWaitFor(Matcher<? super T> matcher,
                       long timeoutInMilliseconds,
                       long intervalInMilliseconds) {
        this.matcher = matcher;
        this.timeoutInMilliseconds = timeoutInMilliseconds;
        this.intervalInMilliseconds = intervalInMilliseconds;
    }

    @Override
    protected boolean matchesSafely(T item) {
        long start = System.currentTimeMillis();
        long end = start + timeoutInMilliseconds;
        while (System.currentTimeMillis() < end) {
            if (matcher.matches(item)) {
                return true;
            }
            try {
                Thread.sleep(intervalInMilliseconds);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return matcher.matches(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("После ожидания [").appendValue(formatDurationWords(timeoutInMilliseconds, true, true))
                .appendText("]: ")
                .appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(T item, Description mismatchDescription) {
        matcher.describeMismatch(item, mismatchDescription);
    }

    @Factory
    public static <T> Matcher<? super T> withWaitFor(Matcher<? super T> matcher) {
        return withWaitFor(matcher, DEFAULT_TIMEOUT, DEFAULT_INTERVAL);
    }


    @Factory
    public static <T> Matcher<? super T> withWaitFor(Matcher<? super T> matcher, long timeoutInMilliseconds) {
        return withWaitFor(matcher, timeoutInMilliseconds, DEFAULT_INTERVAL);
    }


    @Factory
    public static <T> Matcher<? super T> withWaitFor(Matcher<? super T> matcher,
                                                     long timeoutInMilliseconds,
                                                     long intervalInMilliseconds) {
        return new WithWaitFor<T>(matcher, timeoutInMilliseconds, intervalInMilliseconds);
    }

}
