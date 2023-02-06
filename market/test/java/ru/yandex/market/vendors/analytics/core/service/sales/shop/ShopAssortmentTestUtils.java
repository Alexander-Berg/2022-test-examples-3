package ru.yandex.market.vendors.analytics.core.service.sales.shop;

import java.util.Set;

import ru.yandex.market.vendors.analytics.core.model.common.MoneyCountPair;
import ru.yandex.market.vendors.analytics.core.model.sales.common.CategoryPriceSegmentsFilter;
import ru.yandex.market.vendors.analytics.core.model.sales.shops.price.RawShopRegionSales;

/**
 * @author antipov93.
 */
public class ShopAssortmentTestUtils {

    private ShopAssortmentTestUtils() {
    }

    public static RawShopRegionSales createRawSales(
            String shopDomain, long regionId, String date, long money, long count
    ) {
        return RawShopRegionSales.builder()
                .shopDomain(shopDomain)
                .regionId(regionId)
                .date(date)
                .sales(MoneyCountPair.builder().money(money).count(count).build())
                .build();
    }

    public static CategoryPriceSegmentsFilter createCategoryPriceSegmentsFilter(
            long hid,
            Set<Integer> segments
    ) {
        return CategoryPriceSegmentsFilter.builder()
                .categoryId(hid)
                .priceSegments(segments)
                .build();
    }
}
