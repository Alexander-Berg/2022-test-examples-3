package ru.yandex.autotests.innerpochta.imap.matchers;

import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;

import ru.yandex.qatools.matchers.decorators.MatcherDecoratorsBuilder;

import static ru.yandex.qatools.matchers.decorators.MatcherDecoratorsBuilder.should;
import static ru.yandex.qatools.matchers.decorators.TimeoutWaiter.timeoutHasExpired;

public class WaitMatcher {
    public static <T> MatcherDecoratorsBuilder<? super T> withWaitFor(Matcher<? super T> matcher, long time, TimeUnit unit) {
        return should(matcher)
                .whileWaitingUntil(timeoutHasExpired(unit.toMillis(time)));
    }
}
