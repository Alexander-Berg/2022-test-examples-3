package ru.yandex.market.api.partner.cache;

import ru.yandex.market.core.cache.AbstractCacheServicesTest;

/**
 * См. {@link AbstractCacheServicesTest}.
 *
 * @author Vladislav Bauer
 */
public class PartnerApiCacheServicesTest extends AbstractCacheServicesTest {

    private static final String[] ROOT_PACKAGES = {
            "ru.yandex.market.api.partner"
    };


    public PartnerApiCacheServicesTest() {
        super(ROOT_PACKAGES);
    }

}
