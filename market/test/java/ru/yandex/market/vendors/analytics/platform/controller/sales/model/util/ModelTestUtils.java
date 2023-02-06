package ru.yandex.market.vendors.analytics.platform.controller.sales.model.util;

import ru.yandex.market.vendors.analytics.core.model.common.MoneyCountPair;
import ru.yandex.market.vendors.analytics.core.model.sales.brand.RegionBrandSales;
import ru.yandex.market.vendors.analytics.core.model.sales.common.RegionTotalSales;

/**
 * @author fbokovikov
 */
public final class ModelTestUtils {

    private ModelTestUtils() {
    }

    public static RegionBrandSales regionBrandSales(long regionId, long brandId, long count, long money) {
        var moneyCount = MoneyCountPair.builder()
                .count(count)
                .money(money)
                .build();

        return RegionBrandSales.builder()
                .sales(moneyCount)
                .regionId(regionId)
                .brandId(brandId)
                .build();
    }

    public static RegionTotalSales regionTotalSales(long regionId,  long count, long money) {
        var moneyCount = MoneyCountPair.builder()
                .count(count)
                .money(money)
                .build();
        return RegionTotalSales.builder()
                .regionId(regionId)
                .sales(moneyCount)
                .build();
    }
}
