package ru.yandex.market.api.util.parser2.resolver.typed;

import org.junit.Test;

import ru.yandex.market.api.util.parser2.resolver.ResolverTestUtils;

public class BooleanResolverTest {

    private final BooleanResolver resolver = new BooleanResolver();

    @Test
    public void ignoreNull() {
        ResolverTestUtils.assertIgnore(resolver, null);
    }

    @Test
    public void ignoreEmptyString() {
        ResolverTestUtils.assertIgnore(resolver, "");
    }

    @Test
    public void handleTrue() {
        String[] trueValues = new String[]{"1", "T", "TRUE", "Y", "YES"};
        for (String s : trueValues) {
            ResolverTestUtils.assertResolve(resolver, s, true);
        }
    }

    @Test
    public void handleFalse() {
        String[] falseValues = new String[]{"0", "F", "FALSE", "N", "NO"};
        for (String s : falseValues) {
            ResolverTestUtils.assertResolve(resolver, s, false);
        }
    }

    @Test
    public void invalidValue() {
        ResolverTestUtils.assertWrongBooleanValue(resolver, "trash", "trash");
    }
}
