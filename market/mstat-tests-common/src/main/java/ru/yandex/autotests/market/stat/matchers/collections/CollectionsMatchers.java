package ru.yandex.autotests.market.stat.matchers.collections;

import org.hamcrest.Matcher;

import java.util.Collection;

/**
 * Created by jkt on 29.01.15.
 */
public class CollectionsMatchers {

    public static <T> Matcher<Collection<T>> containsAllItems(Collection<T> expectedItems) {
        return ContainsAllItemsMatcher.containsAllItems(expectedItems);
    }
}
