package ru.yandex.market.core.notification.resolver.impl;

import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.context.impl.ShopNotificationContext;
import ru.yandex.market.core.notification.context.impl.UidableNotificationContext;
import ru.yandex.market.core.notification.exception.AliasResolvingException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Интеграционные тесты для {@link SupplierContactResolver}.
 *
 * @author avetokhin 07/03/18.
 */
@DbUnitDataSet(before = "SupplierContactResolverTest.before.csv")
class SupplierContactResolverTest extends FunctionalTest {

    @Autowired
    private SupplierContactResolver resolver;

    @Test
    void invalidContext() {
        Assertions.assertThrows(
                AliasResolvingException.class,
                () -> resolver.resolveAddresses("SupplierAdmins", new UidableNotificationContext(100500))
        );
    }

    @Test
    void resolveSupplierAdmins() {
        final Collection<String> addresses = resolver.resolveAddresses("SupplierAdmins",
                new ShopNotificationContext(1L));
        assertThat(addresses, notNullValue());
        assertThat(addresses, hasSize(2));
        assertThat(addresses, containsInAnyOrder("coolGuy@yandex.ru", "business@yandex.ru"));
    }

    @Test
    void resolveSupplierAll() {
        final Collection<String> addresses = resolver.resolveAddresses("SupplierAll", new ShopNotificationContext(1L));
        assertThat(addresses, containsInAnyOrder("coolGuy@yandex.ru", "yamhistory1@yandex.ru",
                "supplier1return@yandex.ru", "business@yandex.ru"));
    }
}
