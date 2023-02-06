package ru.yandex.market.api.util.parser2.resolver;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import ru.yandex.market.api.util.parser2.resolver.typed.IntegerResolver;

public class CollectionResolver2Test {
    private final Resolver2<Collection<Integer>, Object> resolver = new CollectionResolver2<>(
        ArrayList::new,
        x -> x.split(","),
        new IntegerResolver()
    );

    @Test
    public void ignoreNull() {
        ResolverTestUtils.assertIgnore(resolver, null);
    }

    @Test
    public void ignoreEmptyString() {
        ResolverTestUtils.assertIgnore(resolver, "");
    }

    @Test
    public void singleValue() {
        ResolverTestUtils.assertResolveCollection(resolver, "1", 1);
    }

    @Test
    public void multipleValues() {
        ResolverTestUtils.assertResolveCollection(resolver, "1,2,3", 1, 2, 3);
    }

    @Test
    public void invalidFormatForOneItem() {
        ResolverTestUtils.assertExpectedFormatError(resolver, "1,trash,3", "integer number");
    }

    @Test
    public void invalidFormatForMultipleItem() {
        ResolverTestUtils.assertExpectedFormatError(resolver, "1,trash,3,4,one more trash,5", "integer number");
    }
}
