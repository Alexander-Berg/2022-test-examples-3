package ru.yandex.market.mbo.synchronizer.export;

import org.apache.commons.lang.NotImplementedException;
import ru.yandex.market.mbo.gwt.models.PopularVendors;
import ru.yandex.market.mbo.popularvendors.PopularVendorsService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dmserebr
 * @date 29.05.18
 */
public class PopularVendorsServiceMock implements PopularVendorsService {
    private Map<Long, PopularVendors> popularVendorsMap;

    PopularVendorsServiceMock() {
        popularVendorsMap = new HashMap<>();
    }

    public PopularVendors get(long categoryId) {
        return popularVendorsMap.get(categoryId);
    };

    public List<PopularVendors> getAll() {
        return new ArrayList<>(popularVendorsMap.values());
    }

    public void save(PopularVendors popularVendors) {
        popularVendorsMap.put(popularVendors.getCategoryId(), popularVendors);
    }

    public void delete(long categoryId) {
        popularVendorsMap.remove(categoryId);
    }

    public List<PopularVendors> getRangeWithCategoryNameSorting(int start, int length, Long vendorId) {
        throw new NotImplementedException();
    }

    public int getCount(Long vendorId) {
        return (int) popularVendorsMap.values().stream()
            .filter(popularVendors -> vendorId == null ||
                popularVendors.getVendorsList().stream()
                    .anyMatch(vendorsByRegions -> vendorsByRegions.getVendorIds().contains(vendorId)))
            .count();
    }

    public Map<Long, String> getCategoryNames(Collection<Long> categoryIds) {
        throw new NotImplementedException();
    }
}
