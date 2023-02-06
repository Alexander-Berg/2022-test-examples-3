package ru.yandex.market.api.util.parser2.resolver;

import org.junit.Test;

public class AliasResolverTest {
    private final AliasResolver<Integer> resolver = new AliasResolver<>("aliasName", 123);

    @Test
    public void ignoreNull() {
        ResolverTestUtils.assertIgnore(resolver, null);
    }

    @Test
    public void ignoreEmptyString() {
        ResolverTestUtils.assertIgnore(resolver, "");
    }

    @Test
    public void handleAlias() {
        ResolverTestUtils.assertResolve(resolver, "aliasName", 123);
    }

    @Test
    public void ignoreNotAlias() {
        ResolverTestUtils.assertIgnore(resolver, "someValue");
    }
}
