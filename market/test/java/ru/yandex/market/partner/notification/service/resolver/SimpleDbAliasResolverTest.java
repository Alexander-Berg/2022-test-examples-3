package ru.yandex.market.partner.notification.service.resolver;

import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.notification.AbstractFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleDbAliasResolverTest extends AbstractFunctionalTest {
    @Autowired
    private SimpleDbAliasResolver resolver;

    @Test
    void testGetAlias() {
        Collection<String> resolvedAddresses = resolver.resolveAlias("AbuseStorageAddress");
        Assertions.assertThat(resolvedAddresses.size()).isEqualTo(1);
        Assertions.assertThat(resolvedAddresses.iterator().next()).isEqualTo("shop-abuse@yandex-team.ru");
    }

    @Test
    void testGetWrongAlias() {
        Collection<String> resolvedAddresses = resolver.resolveAlias("InvalidAlias");
        assertTrue(resolvedAddresses.isEmpty());
    }
}
