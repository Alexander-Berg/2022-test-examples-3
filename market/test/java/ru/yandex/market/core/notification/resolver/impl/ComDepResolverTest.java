package ru.yandex.market.core.notification.resolver.impl;

import java.util.Collection;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.Matchers.contains;

public class ComDepResolverTest extends FunctionalTest {
    @Autowired
    private ComDepResolver comDepResolver;

    @Test
    public void testResolveAddresses() {
        Collection<String> emails = comDepResolver.resolveAddresses(null, null);
        MatcherAssert.assertThat(emails, contains("pupkin@yandex.ru"));
    }
}
