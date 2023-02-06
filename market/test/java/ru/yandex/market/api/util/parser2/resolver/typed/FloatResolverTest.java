package ru.yandex.market.api.util.parser2.resolver.typed;

import org.junit.Test;

import ru.yandex.market.api.util.parser2.resolver.ResolverTestUtils;

public class FloatResolverTest {

    private FloatResolver resolver = new FloatResolver();

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
        ResolverTestUtils.assertResolveFloat(resolver, "123.45", 123.45f);
    }

    @Test
    public void formatError() {
        ResolverTestUtils.assertExpectedFormatError(resolver, "trash", "float number");
    }
}
