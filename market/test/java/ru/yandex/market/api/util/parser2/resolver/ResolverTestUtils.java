package ru.yandex.market.api.util.parser2.resolver;

import java.util.Collection;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.util.parser2.resolver.errors.CompositeResolverError;
import ru.yandex.market.api.util.parser2.resolver.errors.ExpectedFormatResolverError;
import ru.yandex.market.api.util.parser2.resolver.errors.ResolverError;
import ru.yandex.market.api.util.parser2.resolver.errors.TestUnknownResolverError;
import ru.yandex.market.api.util.parser2.resolver.errors.WrongBooleanValueResolverError;
import ru.yandex.market.api.util.parser2.resolver.typed.BooleanResolver;

public class ResolverTestUtils {
    private static final double EPS = 1.0e-8;

    public static <T> void assertExpectedFormatError(Resolver2<T, ? super Void> resolver, String s,
                                                     String expectedFormat) {
        ResolverError resolverError = assertError(resolver, s);
        Assert.assertTrue(resolverError instanceof ExpectedFormatResolverError);
        Assert.assertEquals(expectedFormat, ((ExpectedFormatResolverError) resolverError).getExpectedFormat());
    }

    public static <T> void assertIgnore(Resolver2<T, ? super Void> resolver, String s) {
        Result<Maybe<T>, ResolverError> result = resolver.apply(s, null);
        Assert.assertTrue(result.isOk());
        Assert.assertFalse(result.getValue().hasValue());
    }

    public static <T> void assertResolve(Resolver2<T, ? super Void> resolver, String s, T expectedValue) {
        Assert.assertEquals(expectedValue, assertResolved(resolver, s));
    }

    public static <T> void assertResolveCollection(Resolver2<Collection<T>, ? super Void> resolver, String s, T... expected) {
        Collection<T> actual = assertResolved(resolver, s);
        Assert.assertEquals(String.format("Collections has different sizes. Expected %s items, actual %s items.", expected.length,
            actual.size()),
            expected.length,
            actual.size());
        Iterator<T> actualIterator = actual.iterator();
        int position = 0;
        while (actualIterator.hasNext() && position < expected.length) {
            T actualItem = actualIterator.next();
            T expectedItem = expected[position];
            Assert.assertEquals(String.format("Invalid item at position %s (zero-based), expected = %s, actual = %s",
                position,
                expectedItem.toString(),
                actual.toString()),
                expectedItem,
                actualItem);
            ++position;
        }
    }

    public static void assertResolveDouble(Resolver2<Double, ? super Void> resolver, String s, double expected) {
        double aFloat = assertResolved(resolver, s);
        Assert.assertTrue(Math.abs(expected - aFloat) < EPS);
    }

    public static void assertResolveFloat(Resolver2<Float, ? super Void> resolver, String s, float expected) {
        float actual = assertResolved(resolver, s);
        Assert.assertTrue(Math.abs(expected - actual) < EPS);
    }

    public static void assertUnknownError(Resolver2<Integer, ? super Void> resolver, String s, String... expectedErrors) {
        if (expectedErrors.length == 0) {
            Assert.fail("define error messages");
        }
        ResolverError resolverError = assertError(resolver, s);
        if (expectedErrors.length == 1) {
            Assert.assertTrue(resolverError instanceof TestUnknownResolverError);
            Assert.assertEquals(expectedErrors[0], ((TestUnknownResolverError) resolverError).getSource());
        } else {
            Assert.assertTrue(resolverError instanceof CompositeResolverError);
            CompositeResolverError compositeError = (CompositeResolverError) resolverError;
            Collection<ResolverError> errors = compositeError.getErrors();
            Assert.assertEquals(expectedErrors.length, errors.size());
            for (String expectedError : expectedErrors) {
                Assert.assertTrue(errors.stream().anyMatch(x -> expectedError.equals(((TestUnknownResolverError) x).getSource())));
            }
        }
    }

    public static void assertWrongBooleanValue(BooleanResolver resolver, String trash, String trash1) {
        ResolverError error = assertError(resolver, trash);
        Assert.assertTrue(error instanceof WrongBooleanValueResolverError);
        Assert.assertEquals(trash1, ((WrongBooleanValueResolverError) error).getInvalidValue());
    }

    private static <T> ResolverError assertError(Resolver2<T, ? super Void> resolver, String s) {
        Result<Maybe<T>, ResolverError> result = resolver.apply(s, null);
        Assert.assertFalse(result.isOk());
        return result.getError();
    }

    @NotNull
    private static <R> R assertResolved(Resolver2<R, ? super Void> resolver, String s) {
        Result<Maybe<R>, ResolverError> result = resolver.apply(s, null);
        Assert.assertTrue(result.isOk());
        Assert.assertTrue(result.getValue().hasValue());
        return result.getValue().getValue();
    }
}
