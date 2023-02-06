package ru.yandex.market.api.util.parser2.resolver.typed;

import org.junit.Test;

import ru.yandex.market.api.util.parser2.resolver.ResolverTestUtils;

public class IntegerResolverTest {

    private IntegerResolver resolver = new IntegerResolver();

    @Test
    public void ignoreNullValue() {
        ResolverTestUtils.assertIgnore(resolver, null);
    }

    @Test
    public void ignoreEmptyString() {
        ResolverTestUtils.assertIgnore(resolver, "");
    }

    @Test
    public void handleInteger() {
        ResolverTestUtils.assertResolve(resolver, "123", 123);
    }

    @Test
    public void formatError() {
        ResolverTestUtils.assertExpectedFormatError(resolver, "trash", "integer number");
    }
}
