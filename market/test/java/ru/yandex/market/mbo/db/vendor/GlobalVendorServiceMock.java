package ru.yandex.market.mbo.db.vendor;

import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class GlobalVendorServiceMock extends GlobalVendorService {

    private final Collection<GlobalVendor> globalVendors;

    public GlobalVendorServiceMock() {
        this(new ArrayList<>());
    }

    public GlobalVendorServiceMock(GlobalVendor... globalVendors) {
        this(Arrays.asList(globalVendors));
    }

    public GlobalVendorServiceMock(Collection<GlobalVendor> globalVendors) {
        this.globalVendors = new ArrayList<>(globalVendors);
    }

    @Override
    public List<GlobalVendor> getCachedGlobalVendorsByIds(Collection<Long> ids) {
        return globalVendors.stream()
                .filter(v -> ids.contains(v.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<GlobalVendor> loadVendors(Collection<Long> ids) {
        return globalVendors.stream()
                .filter(gv -> ids.contains(gv.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public GlobalVendor loadVendor(long vendorId) {
        return globalVendors.stream()
                .filter(gv -> gv.getId() == vendorId)
                .findFirst()
                .orElse(null);
    }

    public void addVendor(GlobalVendor vendor) {
        globalVendors.add(vendor);
    }
}
