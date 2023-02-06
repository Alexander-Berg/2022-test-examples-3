package ru.yandex.market.api.util.parser2;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.util.parser2.resolver.typed.IntegerResolver;

public class ParserTestUtils {

    public static <T> void assertNotAllowedValues(Result<T, ValidationError> result,
                                                  String... expectedAllowedOptions) {
        assertNotAllowedValues(result, x -> x, expectedAllowedOptions);
    }

    public static <T> void assertNotAllowedValues(Result<T, ValidationError> result,
                                                  T... expectedAllowedOptions) {
        assertNotAllowedValues(result, x -> x.toString().toUpperCase(), expectedAllowedOptions);
    }

    public static <T, R> void assertNotAllowedValues(Result<R, ValidationError> result,
                                                     Function<T, String> toString,
                                                     T... expectedAllowedOptions) {
        Assert.assertFalse(result.isOk());
        ValidationError error = result.getError();
        Assert.assertNotNull(error);
        for (T e : expectedAllowedOptions) {
            Assert.assertTrue(String.format("cant find %s", e.toString()),
                error.getMessage().contains(toString.apply(e)));
        }
    }

    public static <T> void assertError(String errorMsg, Result<T, ValidationError> result) {
        ValidationError error = assertError(result);
        Assert.assertTrue(error.getMessage().contains(errorMsg));
    }

    public static <T> void assertParsed(T expectedValue, Result<T, ValidationError> result) {
        T actual = assertParsed(result);
        Assert.assertEquals(expectedValue, actual);
    }

    public static <T> void assertParsed(T[] expectedValues, Result<Collection<T>, ValidationError> result) {
        Collection<T> actual = assertParsed(result);
        Assert.assertEquals(String.format("incorrect number of items. Expected items = [%s], actual items = [%s]",
            Arrays.stream(expectedValues).map(x -> x.toString()).collect(Collectors.joining(",")),
            actual.stream().map(x -> x.toString()).collect(Collectors.joining(","))),
            expectedValues.length,
            actual.size());
        for (T expectedValue : expectedValues) {
            Assert.assertTrue(String.format("cant find value %s", expectedValue),
                actual.contains(expectedValue));
        }
    }

    @NotNull
    private static <T> T assertParsed(Result<T, ValidationError> result) {
        Assert.assertTrue(result.isOk());
        return result.getValue();
    }

    @NotNull
    private static <T> ValidationError assertError(Result<T, ValidationError> result) {
        Assert.assertTrue(result.hasError());
        return result.getError();
    }

    public static ParserBuilder2<Integer, Void> integerParser() {
        return new ParserBuilder2<Integer, Void>(null)
                .addResolver(new IntegerResolver());
    }
}
