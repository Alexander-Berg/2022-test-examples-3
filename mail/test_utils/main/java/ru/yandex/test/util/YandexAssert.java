package ru.yandex.test.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Assert;

import ru.yandex.collection.IntPair;

public final class YandexAssert {
    private static final String STRING = "String '";
    private static final String BUT_WAS = " but was ";
    private static final String EXPECTED_EMPTY = " expected to be empty";
    private static final String SIZE_EXPECTED = " size expected to be ";
    private static final String SIZE_SAME = " size expected to be same as ";
    private static final String WASNT_FOUND = "' wasn't found in '";
    private static final String WHILE_CHECKING_ELEMENT =
        "While checking element # ";
    private static final String OF = " of ";
    private static final String AGAINST = " against ";

    private YandexAssert() {
    }

    public static void check(final Checker expected, final String actual) {
        String checkResult = expected.check(actual);
        if (checkResult != null) {
            System.out.println("Actual " + actual);
            throw new AssertionError(checkResult);
        }
    }

    public static List<Checker> checkersFor(final String... expected) {
        List<Checker> checkers = new ArrayList<>(expected.length);
        for (String expectedValue: expected) {
            checkers.add(new StringChecker(expectedValue));
        }
        return checkers;
    }

    public static List<Checker> checkersFor(final List<String> expected) {
        int size = expected.size();
        List<Checker> checkers = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            checkers.add(new StringChecker(expected.get(i)));
        }
        return checkers;
    }

    public static void check(
        final List<? extends Checker> expected,
        final List<String> actual)
    {
        assertSize(expected, actual);
        for (int i = 0; i < expected.size(); ++i) {
            try {
                check(expected.get(i), actual.get(i));
            } catch (Throwable t) {
                t.addSuppressed(
                    new Exception(
                        WHILE_CHECKING_ELEMENT + i + OF + expected
                        + AGAINST + actual));
                throw t;
            }
        }
    }

    public static void assertEquals(
        final List<?> expected,
        final List<?> actual)
    {
        assertSize(expected, actual);
        for (int i = 0; i < expected.size(); ++i) {
            try {
                Assert.assertEquals(expected.get(i), actual.get(i));
            } catch (Throwable t) {
                t.addSuppressed(
                    new Exception(
                        WHILE_CHECKING_ELEMENT + i + OF + expected
                        + AGAINST + actual));
                throw t;
            }
        }
    }

    public static void assertNotEquals(
        final Object expected,
        final Object actual)
    {
        if (Objects.equals(expected, actual)) {
            throw new AssertionError(
                '\'' + expected.toString() + "' and '"
                + actual + "' expected to be not equal");
        }
    }

    public static void assertContains(
        final String expected,
        final String actual)
    {
        if (!actual.contains(expected)) {
            throw new AssertionError(
                STRING + expected + WASNT_FOUND + actual + '\'');
        }
    }

    public static void assertContains(
        final Object expected,
        final Collection<?> actual)
    {
        if (!actual.contains(expected)) {
            throw new AssertionError(
                "Element '" + expected + WASNT_FOUND + actual + '\'');
        }
    }

    public static void assertNotContains(
        final String notExpected,
        final String actual)
    {
        if (actual.contains(notExpected)) {
            throw new AssertionError(
                "Forbidden string '" + notExpected + "' was found in '"
                    + actual + '\'');
        }
    }

    public static void assertStartsWith(
        final String expected,
        final String actual)
    {
        if (!actual.startsWith(expected)) {
            throw new AssertionError(
                STRING + actual + "' expected to be started with '"
                + expected + '\'');
        }
    }

    public static void assertEndsWith(
        final String expected,
        final String actual)
    {
        if (!actual.endsWith(expected)) {
            throw new AssertionError(
                STRING + actual + "' expected to be ended with '"
                + expected + '\'');
        }
    }

    public static void assertEmpty(final String actual) {
        if (!actual.isEmpty()) {
            throw new AssertionError('\'' + actual + '\'' + EXPECTED_EMPTY);
        }
    }

    public static void assertEmpty(final Collection<?> actual) {
        if (!actual.isEmpty()) {
            throw new AssertionError(actual + EXPECTED_EMPTY);
        }
    }

    public static void assertEmpty(final Map<?, ?> actual) {
        if (!actual.isEmpty()) {
            throw new AssertionError(actual + EXPECTED_EMPTY);
        }
    }

    public static void assertSize(
        final int expected,
        final Collection<?> actual)
    {
        int size = actual.size();
        if (size != expected) {
            throw new AssertionError(
                actual + SIZE_EXPECTED + expected + BUT_WAS + size);
        }
    }

    public static void assertSize(final int expected, final Map<?, ?> actual) {
        int size = actual.size();
        if (size != expected) {
            throw new AssertionError(
                actual + SIZE_EXPECTED + expected + BUT_WAS + size);
        }
    }

    public static IntPair<Map.Entry<Object, Object>> firstDiff(
        final Collection<?> first,
        final Collection<?> second)
    {
        Iterator<?> iter1 = first.iterator();
        Iterator<?> iter2 = second.iterator();
        int i = 0;
        while (iter1.hasNext() && iter2.hasNext()) {
            Object o1 = iter1.next();
            Object o2 = iter2.next();
            if (!Objects.equals(o1, o2)) {
                return new IntPair<>(
                    i,
                    new AbstractMap.SimpleImmutableEntry<>(o1, o2));
            }
            ++i;
        }
        return new IntPair<>(i, null);
    }

    public static void assertSize(
        final Collection<?> expected,
        final Collection<?> actual)
    {
        int size = actual.size();
        int expsize = expected.size();
        if (size != expsize) {
            StringBuilder sb = new StringBuilder(actual.toString());
            sb.append(SIZE_SAME);
            sb.append(expected);
            sb.append(' ');
            sb.append('(');
            sb.append(expsize);
            sb.append(')');
            sb.append(BUT_WAS);
            sb.append(size);
            IntPair<Map.Entry<Object, Object>> diff =
                firstDiff(expected, actual);
            Map.Entry<Object, Object> values = diff.second();
            if (values != null) {
                sb.append(". First difference is at index ");
                sb.append(diff.first());
                sb.append(':');
                sb.append(' ');
                sb.append(values.getKey());
                sb.append(" != ");
                sb.append(values.getValue());
            }
            throw new AssertionError(new String(sb));
        }
    }

    public static void assertSize(
        final Map<?, ?> expected,
        final Map<?, ?> actual)
    {
        assertSize(expected.entrySet(), actual.entrySet());
    }

    public static void assertInstanceOf(
        final Class<?> expected,
        final Object instance)
    {
        if (!expected.isInstance(instance)) {
            StringBuilder sb = new StringBuilder();
            sb.append(instance);
            sb.append(" expected to be instance of ");
            sb.append(expected);
            if (instance != null) {
                sb.append(BUT_WAS);
                sb.append(instance.getClass());
            }
            String message = new String(sb);
            if (instance instanceof Throwable) {
                throw new AssertionError(message, (Throwable) instance);
            } else {
                throw new AssertionError(message);
            }
        }
    }

    public static <T> void assertLess(
        final T max,
        final Comparable<T> actual)
    {
        if (actual.compareTo(max) >= 0) {
            throw new AssertionError(
                '\'' + actual.toString() + "' expected to be less than '"
                + max + '\'');
        }
    }

    public static <T> void assertNotLess(
        final T min,
        final Comparable<T> actual)
    {
        if (actual.compareTo(min) < 0) {
            throw new AssertionError(
                '\'' + actual.toString()
                + "' expected to be greater or equal to '"
                + min + '\'');
        }
    }

    public static <T> void assertGreater(
        final T min,
        final Comparable<T> actual)
    {
        if (actual.compareTo(min) <= 0) {
            throw new AssertionError(
                '\'' + actual.toString() + "' expected to be greater than '"
                + min + '\'');
        }
    }

    public static <T> void assertNotGreater(
        final T max,
        final Comparable<T> actual)
    {
        if (actual.compareTo(max) > 0) {
            throw new AssertionError(
                '\'' + actual.toString()
                + "' expected to be not greater than '"
                + max + '\'');
        }
    }
}

