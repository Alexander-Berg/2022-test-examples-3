package ru.yandex.market.delivery.mdbapp.util;

import java.util.function.Predicate;

import org.mockito.ArgumentMatcher;

public final class MdbArgumentMatcher {

    private MdbArgumentMatcher() {
    }

    public static <T> ArgumentMatcher<T> matcher(Predicate<T> matchesPredicate) {
        return matchesPredicate::test;
    }
}
