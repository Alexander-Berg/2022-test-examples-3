package ru.yandex.market.deepmind.common.mocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;

/**
 * @author s-ermakov
 */
public class GlobalVendorsCachingServiceMock implements GlobalVendorsCachingService {
    private final Map<Long, CachedGlobalVendor> vendorMap = new HashMap<>();

    public void addVendor(CachedGlobalVendor globalVendor) {
        vendorMap.put(globalVendor.getId(), globalVendor);
    }

    @Override
    public Optional<CachedGlobalVendor> getVendor(long vendorId) {
        return Optional.ofNullable(vendorMap.get(vendorId));
    }

    @Override
    public Map<Long, Optional<CachedGlobalVendor>> getVendorMap(Collection<Long> vendorIds) {
        return vendorIds.stream()
            .distinct()
            .collect(Collectors.toMap(Function.identity(), o -> Optional.ofNullable(vendorMap.get(o)), (a, b) -> a));
    }
}
