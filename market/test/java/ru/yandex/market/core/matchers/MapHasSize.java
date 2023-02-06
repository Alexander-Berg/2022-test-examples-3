package ru.yandex.market.core.matchers;

import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

/**
 * @author vbudnev
 */
public class MapHasSize extends TypeSafeMatcher<Map<?, ?>> {
    private int expectedSize;

    public MapHasSize(int expectedSize) {
        this.expectedSize = expectedSize;
    }

    @Factory
    public static MapHasSize mapIsEmpty() {
        return new MapHasSize(0);
    }

    @Factory
    public static MapHasSize mapHasSize(int expectedSize) {
        return new MapHasSize(expectedSize);
    }

    @Override
    protected boolean matchesSafely(Map<?, ?> mapToCompare) {
        return mapToCompare.size() == expectedSize;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Map size expected to be: ").appendValue(expectedSize);
    }

    @Override
    protected void describeMismatchSafely(Map<?, ?> item, Description mismatchDescription) {
        mismatchDescription.appendText("was ").appendValue(item.size());
    }

}
