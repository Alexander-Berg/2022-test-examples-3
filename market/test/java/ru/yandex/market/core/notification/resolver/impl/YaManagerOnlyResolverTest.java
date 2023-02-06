package ru.yandex.market.core.notification.resolver.impl;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.context.impl.ShopNotificationContext;
import ru.yandex.market.core.notification.context.impl.UidableNotificationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit тесты для {@link YaManagerOnlyResolver}.
 *
 * @author avetokhin 22/12/17.
 */
@DbUnitDataSet(before = "YaManagerOnlyResolverTest.before.csv")
public class YaManagerOnlyResolverTest extends FunctionalTest {

    private static final long SHOP_ID_NOT_FOUND = 11;
    private static final long SHOP_ID_FOUND = 12;
    private static final long SHOP_ID_BLACK = 13;
    private static final long SHOP_ID_NULL_EMAIL = 14;
    private static final long SUPPLIER_ID_FOUND = 15;
    private static final String PASSPORT_EMAIL = "passport.email@mail.ru";
    private static final String SUPPLIER_PASSPORT_EMAIL = "supplier.passport.email@mail.ru";
    private static final String ALIAS = "alias";

    @Autowired
    @Qualifier("testYaManagerOnlyResolver")
    private YaManagerOnlyResolver resolver;

    /**
     * Передан неподдерживаемый контекст.
     */
    @Test
    public void testResolveInvalidContext() {
        final Collection<String> addresses = resolver.resolveAddresses(ALIAS,
                new UidableNotificationContext(100));

        assertThat(addresses, notNullValue());
        assertThat(addresses, hasSize(0));
    }

    /**
     * Передан ID магазина без менеджера.
     */
    @Test
    public void testNotFound() {
        checkMissed(SHOP_ID_NOT_FOUND);
    }

    /**
     * Позитивный случай, найден менеджер, найден емэйл.
     */
    @Test
    public void testFound() {
        final Collection<String> addresses = resolver.resolveAddresses(ALIAS,
                new ShopNotificationContext(SHOP_ID_FOUND));

        assertThat(addresses, notNullValue());
        assertThat(addresses, hasSize(1));
        assertThat(addresses.iterator().next(), equalTo(PASSPORT_EMAIL));
    }

    /**
     * Позитивный случай, найден менеджер поставщика, найден емэйл.
     */
    @Test
    public void testSupplierFound() {
        final Collection<String> addresses = resolver.resolveAddresses(ALIAS,
                new ShopNotificationContext(SUPPLIER_ID_FOUND));

        assertThat(addresses, notNullValue());
        assertThat(addresses, hasSize(1));
        assertThat(addresses.iterator().next(), equalTo(SUPPLIER_PASSPORT_EMAIL));
    }

    /**
     * Найден менеджер, найден емэйл, но он в черном списке.
     */
    @Test
    public void testBlack() {
        checkMissed(SHOP_ID_BLACK);
    }

    /**
     * Найден менеджер, но у него пустой паспортный емэйл.
     */
    @Test
    public void testNullEmail() {
        checkMissed(SHOP_ID_NULL_EMAIL);
    }

    private void checkMissed(final long shopIdNotFound) {
        final Collection<String> addresses = resolver.resolveAddresses(ALIAS,
                new ShopNotificationContext(shopIdNotFound));

        assertThat(addresses, notNullValue());
        assertThat(addresses, empty());
    }

}
