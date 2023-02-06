package ru.yandex.market.notification.simple.service.registry;

import org.junit.Test;

import ru.yandex.market.notification.common.Registry;
import ru.yandex.market.notification.common.TypedRegistry;
import ru.yandex.market.notification.simple.service.registry.type.NotificationAddressTypeRegistryImpl;
import ru.yandex.market.notification.simple.service.registry.type.NotificationContentTypeRegistryImpl;
import ru.yandex.market.notification.simple.service.registry.type.NotificationDestinationTypeRegistryImpl;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для классов пакета {@link ru.yandex.market.notification.simple.service.registry.type}.
 *
 * @author Vladislav Bauer
 */
public class NotificationRegistriesTest {

    /**
     * Основные тесты для проверки классов написаны для их базовых реализаций.
     * Здесь проверяем только контракты, которые реализуют соответствующие реестры.
     */
    @Test
    public void testRegistries() {
        assertThat(new NotificationAddressResolverRegistryImpl(emptySet()), instanceOf(Registry.class));
        assertThat(new NotificationDataProviderRegistryImpl(emptySet()), instanceOf(Registry.class));
        assertThat(new NotificationFacadeRegistryImpl(emptyMap()), instanceOf(TypedRegistry.class));

        assertThat(new NotificationAddressTypeRegistryImpl(emptyMap()), instanceOf(TypedRegistry.class));
        assertThat(new NotificationContentTypeRegistryImpl(emptyMap()), instanceOf(TypedRegistry.class));
        assertThat(new NotificationDestinationTypeRegistryImpl(emptyMap()), instanceOf(TypedRegistry.class));
    }

}
