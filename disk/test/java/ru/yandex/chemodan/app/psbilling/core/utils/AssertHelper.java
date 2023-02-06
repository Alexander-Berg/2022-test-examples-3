package ru.yandex.chemodan.app.psbilling.core.utils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Predicate;

import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.misc.test.Assert;

public class AssertHelper {
    public static void assertEquals(BigDecimal actual, double expected) {
        assertEquals(actual, BigDecimal.valueOf(expected));
    }

    public static void assertEquals(BigDecimal actual, BigDecimal expected) {
        String message = String.format("expected %s to be %s", actual, expected);
        if (actual == null && expected != null || actual != null && expected == null) {
            Assert.fail(message);
        }

        Assert.assertEquals(message, 0, expected.compareTo(actual));
    }

    public static void assertTrue(boolean condition) {
        Assert.assertTrue(condition);
    }

    public static void assertEquals(Object actual, Object expected) {
        Assert.equals(expected, actual);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        Assert.equals(actual, expected, message);
    }

    public static void notEquals(Object actual, Object expected) {
        Assert.assertNotEquals(expected, actual);
    }

    // collection
    public static void assertSize(Collection<?> collection, int size) {
        Assert.sizeIs(size, collection);
    }

    public static <T> T assertSingle(Collection<T> collection) {
        assertSize(collection, 1);
        return collection.stream().findFirst().get();
    }

    public static <T> void assertContains(Collection<T> collection, T item) {
        Assert.assertContains(collection, item);
    }

    // exceptions
    public static void assertBadRequest(Assert.Block block) {
        assertThrows(block, A3ExceptionWithStatus.class, e -> e.getHttpStatusCode() == 400);
    }

    public static <T extends Throwable> void assertThrows(Assert.Block block, Class<T> type) {
        assertThrows(block, type, exception -> true);
    }

    public static <T extends Throwable> void assertThrows(Assert.Block block, Class<T> type, Predicate<T> predicate) {
        Assert.assertThrows(block, type, predicate);
    }
}
