package ru.yandex.market.mbo.synchronizer.export;

import ru.yandex.market.mbo.gwt.models.PopularVendors;

/**
 * @author dmserebr
 * @date 29.05.18
 */
public class PopularVendorsBuilder {
    private PopularVendors popularVendors;
    private PopularVendors.VendorsByRegions currentVendorsByRegions;

    public PopularVendorsBuilder() {
        popularVendors = new PopularVendors();
    }

    public PopularVendorsBuilder categoryId(long categoryId) {
        popularVendors.setCategoryId(categoryId);
        return this;
    }

    public PopularVendorsBuilder startList() {
        currentVendorsByRegions = new PopularVendors.VendorsByRegions();
        return this;
    }

    public PopularVendorsBuilder vendor(long vendorId) {
        currentVendorsByRegions.getVendorIds().add(vendorId);
        return this;
    }

    public PopularVendorsBuilder region(long regionId) {
        currentVendorsByRegions.getRegionIds().add(regionId);
        return this;
    }

    public PopularVendorsBuilder blue(boolean isBlue) {
        currentVendorsByRegions.setBlue(isBlue);
        return this;
    }

    public PopularVendorsBuilder endList() {
        popularVendors.addVendorsByRegions(currentVendorsByRegions);
        currentVendorsByRegions = null;
        return this;
    }

    public PopularVendors build() {
        return popularVendors;
    }
}
