package ru.yandex.market.notification.simple.service.common;

import ru.yandex.market.notification.common.TypedRegistry;

/**
 * Unit-тесты для {@link TypedRegistryImpl}.
 *
 * @author Vladislav Bauer
 */
public class TypedRegistryImplTest extends AbstractTypedRegistryTest<TypedRegistry<String, String>> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected TypedRegistry<String, String> createRegistry() {
        return new TypedRegistryImpl<>(createData());
    }

}
