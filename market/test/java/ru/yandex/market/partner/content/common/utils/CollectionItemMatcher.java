package ru.yandex.market.partner.content.common.utils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

public class CollectionItemMatcher<T extends Collection<R>, R> implements ArgumentMatcher<T> {
    public static <R> Set<R> setOf(R oneItem) {
        return setOf(Sets.newHashSet(oneItem));
    }

    public static <R> Set<R> setOf(Collection<R> items) {
        return ArgumentMatchers.argThat(new CollectionItemMatcher<Set<R>, R>(Sets.newHashSet(items)));
    }

    public static <R> List<R> listOf(R oneItem) {
        return listOf(Lists.newArrayList(oneItem));
    }

    public static <R> List<R> listOf(Collection<R> items) {
        return ArgumentMatchers.argThat(new CollectionItemMatcher<List<R>, R>(Lists.newArrayList(items)));
    }

    private final T expected;

    private CollectionItemMatcher(T expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(T actual) {
        return actual.size() == expected.size() && containsAllItems(actual);
    }

    private boolean containsAllItems(T actual) {
        for (R o : actual) {
            if (!expected.contains(o)) {
                return false;
            }
        }
        return true;
    }
}
