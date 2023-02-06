package ru.yandex.market.api.util.parser2.resolver;

import org.junit.Test;

public class NullResolverTest {

    private static final int VALUE = 123;

    private final NullResolver<Integer> resolver = new NullResolver<>(VALUE);

    @Test
    public void handleNull() {
        ResolverTestUtils.assertResolve(resolver, null, VALUE);
    }

    @Test
    public void shouldAvoidEmptyString() {
        ResolverTestUtils.assertIgnore(resolver, "");
    }

    @Test
    public void ignoreNonEmpty() {
        ResolverTestUtils.assertIgnore(resolver, "test");
    }
}
