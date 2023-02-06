package ru.yandex.market.api.util.parser2.resolver.typed;

import org.junit.Test;

import ru.yandex.market.api.util.parser2.resolver.ResolverTestUtils;

public class DoubleResolverTest {

    private DoubleResolver resolver = new DoubleResolver();

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
        ResolverTestUtils.assertResolveDouble(resolver, "123.45", 123.45);
    }

    @Test
    public void formatError() {
        ResolverTestUtils.assertExpectedFormatError(resolver, "trash", "double number");
    }
}
