package ru.yandex.market.sdk.userinfo.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * @authror dimkarp93
 */
@NotThreadSafe
public class MapMatcher<K, V> extends TypeSafeMatcher<Map<? extends K, ? extends V>> {
    private Collection<Matcher<Map<? extends K, ? extends V>>> matchers;
    private Collection<Matcher<Map<? extends K, ? extends V>>> failed;

    public MapMatcher(Matcher<Map<? extends K, ? extends V>>... matchers) {
        this.matchers = Arrays.asList(matchers);
    }

    @Override
    protected boolean matchesSafely(Map<? extends K, ? extends V> item) {
        failed = new ArrayList<>();
        for (Matcher<Map<? extends K, ? extends V>> m : matchers) {
            if (!m.matches(item)) {
                failed.add(m);
            }
        }
        return failed.isEmpty();
    }

    @Override
    public void describeTo(Description description) {
        if (null == failed) {
            //не было вызвано метода матч
            return;
        }
        for (Matcher<Map<? extends K, ? extends V>> m : failed) {
            description.appendDescriptionOf(m)
                    .appendText("\n");
        }
    }
}
