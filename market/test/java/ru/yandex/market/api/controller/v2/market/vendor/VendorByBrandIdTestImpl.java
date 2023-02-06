package ru.yandex.market.api.controller.v2.market.vendor;

import io.netty.util.concurrent.Future;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.yandex.market.api.internal.market.vendor.VendorApiClient;
import ru.yandex.market.api.internal.market.vendor.VendorByBrandIdCache;
import ru.yandex.market.api.internal.market.vendor.domain.VendorItem;

/**
 * @author dimkarp93
 */
@Component
@Profile("test")
public class VendorByBrandIdTestImpl implements VendorByBrandIdCache {
    private final VendorApiClient vendorApiClient;

    public VendorByBrandIdTestImpl(VendorApiClient vendorApiClient) {
        this.vendorApiClient = vendorApiClient;
    }

    @Override
    public Future<VendorItem> get(long brandId, long uid) {
        return vendorApiClient.getVendorByBrandId(brandId, uid);
    }
}
