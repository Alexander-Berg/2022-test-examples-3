package ru.yandex.market.loyalty.test;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.samePropertyValuesAs;

public class SameCollection {
    public static <T> Matcher<Iterable<? extends T>> sameCollectionInAnyOrder(Collection<T> expected) {
        return containsInAnyOrder(
                expected.stream()
                        .map(Matchers::equalTo)
                        .collect(Collectors.toList())
        );
    }

    public static <T> Matcher<Iterable<? extends T>> sameCollection(Collection<T> expected) {
        return contains(
                expected.stream()
                        .map(Matchers::equalTo)
                        .collect(Collectors.toList())
        );
    }

    public static <T> List<Matcher<? super T>> sameCollectionByPropertyValuesAs(Collection<T> expected,
                                                                                String... ignoredProperties) {
        return expected.stream()
                .map(t -> samePropertyValuesAs(t, ignoredProperties))
                .collect(Collectors.toList());
    }
}
