package ru.yandex.autotests.market.stat.matchers.collections;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.market.common.attacher.Attacher;

import java.util.Collection;

/**
 * Created by jkt on 16.07.14.
 */
public class ContainsAllItemsMatcher<T> extends TypeSafeMatcher<Collection<T>> {

    private Collection<T> expectedItems;

    @Factory
    public static <T> ContainsAllItemsMatcher<T> containsAllItems(Collection<T> expectedItems) {
        return new ContainsAllItemsMatcher(expectedItems);
    }

    public ContainsAllItemsMatcher(Collection<T> expectedItems) {
        this.expectedItems = expectedItems;
    }

    @Override
    protected boolean matchesSafely(Collection<T> ts) {
        if (ts.containsAll(expectedItems)) {
            return true;
        }
        attachMissingItems(expectedItems, ts);
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("contains all items " + expectedItems);
    }

    @Override
    protected void describeMismatchSafely(Collection<T> item, Description mismatchDescription) {
        mismatchDescription.appendText("Does not contain all expected items. See attachment.");
    }

    private void attachMissingItems(Collection<T> expectedItems, Collection<T> actualItems) {
        expectedItems.removeAll(actualItems);
        Attacher.attachMissingItems(expectedItems);
    }
}
