package ru.yandex.market.mbi.api.cache;

import ru.yandex.market.core.cache.AbstractCacheServicesTest;

/**
 * См. {@link AbstractCacheServicesTest}.
 *
 * @author Vladislav Bauer
 */
public class ApiCacheServicesTest extends AbstractCacheServicesTest {

    private static final String[] ROOT_PACKAGES = {
            "ru.yandex.market.mbi.api"
    };


    public ApiCacheServicesTest() {
        super(ROOT_PACKAGES);
    }

}
