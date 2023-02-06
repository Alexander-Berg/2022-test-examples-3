package ru.yandex.market.crm.operatorwindow.util;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;

public abstract class AssertHelpers {

    public static <T> void assertListMatches(
            String prefix,
            AssertMatcher<T> matcher,
            List<T> expected,
            List<T> actual
    ) {
        Assertions.assertEquals(expected.size(), actual.size(), prefix + ".size()");
        Iterator<T> expectedIter = expected.iterator();
        Iterator<T> actualIter = actual.iterator();
        for (int i = 0; expectedIter.hasNext(); i++) {
            T expectedVal = expectedIter.next();
            T actualVal = actualIter.next();
            matcher.matches(prefix + "[" + i + "]", expectedVal, actualVal);
        }
    }

}
